"""
use_cases.py — Orchestration layer for scan operations.

RunFullScanUseCase:       Walk + parse + rule-check + diff + persist
GetScanHistoryUseCase:    Read index.json → sorted history
GetCurrentIssuesUseCase:  Latest scan report
GetDatabaseHealthUseCase: Parse Room schema + Migrations.kt
"""
from __future__ import annotations

import json
import os
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

from models import (
    Issue, ScanReport, ScanSummary, DatabaseHealth,
    SEVERITY_ORDER
)
from scanner.file_walker import walk_project, ProjectContext
from scanner.kotlin_parser import parse_kotlin_file, KotlinFileMeta
from scanner.room_schema_parser import build_database_health
from rules import ALL_RULES


# ---------------------------------------------------------------------------
# Storage helpers
# ---------------------------------------------------------------------------

def _reports_dir(audit_root: str) -> Path:
    p = Path(audit_root) / "reports"
    p.mkdir(parents=True, exist_ok=True)
    return p


def _audit_data_dir(audit_root: str) -> Path:
    p = Path(audit_root) / "audit_data"
    p.mkdir(parents=True, exist_ok=True)
    return p


def _index_path(audit_root: str) -> Path:
    return _reports_dir(audit_root) / "index.json"


def _load_index(audit_root: str) -> list[ScanSummary]:
    path = _index_path(audit_root)
    if not path.exists():
        return []
    try:
        with open(path, "r", encoding="utf-8") as f:
            raw = json.load(f)
        return [ScanSummary.from_dict(s) for s in raw]
    except Exception:
        return []


def _save_index(audit_root: str, summaries: list[ScanSummary]) -> None:
    path = _index_path(audit_root)
    with open(path, "w", encoding="utf-8") as f:
        json.dump([s.to_dict() for s in summaries], f, indent=2, ensure_ascii=False)


def _save_report(audit_root: str, report: ScanReport, filename: str) -> None:
    path = _reports_dir(audit_root) / filename
    with open(path, "w", encoding="utf-8") as f:
        json.dump(report.to_dict(), f, indent=2, ensure_ascii=False)


def _load_report(audit_root: str, filename: str) -> Optional[ScanReport]:
    path = _reports_dir(audit_root) / filename
    if not path.exists():
        return None
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return ScanReport.from_dict(data)
    except Exception:
        return None


# ---------------------------------------------------------------------------
# Run Full Scan Use Case
# ---------------------------------------------------------------------------

class RunFullScanUseCase:
    """
    Orchestrates the complete scan pipeline:
      1. Walk all files (hash-diff for incremental detection)
      2. Parse Kotlin metadata for each relevant file
      3. Inject database health metadata into ProjectContext
      4. Run all rules against each file
      5. Diff against previous scan (new / resolved issues)
      6. Persist report and update index

    Args:
        project_root:  Absolute path to the Qdash repository root
        audit_root:    Absolute path to tools/qdash-audit/
        force_full:    Force re-scan of all files (ignore hash cache)

    Returns:
        (ScanReport, report_filename)
    """

    def __init__(self, project_root: str, audit_root: str, force_full: bool = False):
        self.project_root = project_root
        self.audit_root   = audit_root
        self.force_full   = force_full

    def run(self) -> tuple[ScanReport, str]:
        started_at  = datetime.now(timezone.utc)
        start_ts    = time.monotonic()
        scan_id     = str(uuid.uuid4())

        # ── 1. Walk files ──────────────────────────────────────────────────
        print("  [1/5] Walking project files…")
        audit_data = str(_audit_data_dir(self.audit_root))
        project_ctx = walk_project(self.project_root, audit_data, self.force_full)

        # ── 2. Parse Kotlin metadata ────────────────────────────────────────
        print(f"  [2/5] Parsing {len(project_ctx.kotlin_files)} Kotlin files…")
        kotlin_meta_map: dict[str, KotlinFileMeta] = {}
        for fc in project_ctx.kotlin_files:
            kotlin_meta_map[fc.rel_path] = parse_kotlin_file(fc)

        # ── 3. Build DB health + inject into project_ctx ───────────────────
        print("  [3/5] Analysing Room schema…")
        db_health = build_database_health(self.project_root)
        # Inject helpers for project-level DB rules
        project_ctx._db_health_missing_migrations = db_health.missing_migrations  # type: ignore[attr-defined]
        project_ctx._db_schema_versions = [                                         # type: ignore[attr-defined]
            int(Path(f).stem) for f in db_health.schema_files_found
            if Path(f).stem.isdigit()
        ]
        project_ctx._dup001_registry = {}   # type: ignore[attr-defined]
        project_ctx._dup002_registry = {}   # type: ignore[attr-defined]

        # ── 4. Run rules ───────────────────────────────────────────────────
        print(f"  [4/5] Running {len(ALL_RULES)} rules…")
        all_issues: list[Issue] = []
        for fc in project_ctx.files:
            kotlin_meta = kotlin_meta_map.get(fc.rel_path)
            for rule in ALL_RULES:
                try:
                    issues = rule.check(fc, project_ctx, kotlin_meta)
                    all_issues.extend(issues)
                except Exception as exc:
                    print(f"      ⚠ Rule {rule.id} failed on {fc.rel_path}: {exc}")

        # ── 5. Diff against previous scan ─────────────────────────────────
        print("  [5/5] Diffing against previous scan…")
        history = _load_index(self.audit_root)
        prev_issue_ids: set[str] = set()
        if history:
            prev_report = _load_report(self.audit_root, history[-1].report_file)
            if prev_report:
                prev_issue_ids = {
                    f"{i.rule_id}|{i.file_path}|{i.line_start}"
                    for i in prev_report.issues
                }

        current_keys = {
            f"{i.rule_id}|{i.file_path}|{i.line_start}"
            for i in all_issues
        }
        new_issue_ids      = [i.id for i in all_issues
                              if f"{i.rule_id}|{i.file_path}|{i.line_start}" not in prev_issue_ids]
        resolved_issue_ids = list(prev_issue_ids - current_keys)

        # ── Build report ──────────────────────────────────────────────────
        completed_at   = datetime.now(timezone.utc)
        duration       = time.monotonic() - start_ts
        by_severity    = {sev: 0 for sev in SEVERITY_ORDER}
        for issue in all_issues:
            by_severity[issue.severity] = by_severity.get(issue.severity, 0) + 1

        report = ScanReport(
            scan_id=scan_id,
            started_at=started_at.isoformat(),
            completed_at=completed_at.isoformat(),
            duration_seconds=round(duration, 2),
            project_root=self.project_root,
            total_files_scanned=len(project_ctx.files),
            total_issues=len(all_issues),
            issues_by_severity=by_severity,
            issues=all_issues,
            new_issue_ids=new_issue_ids,
            resolved_issue_ids=resolved_issue_ids,
            db_health=db_health,
            schema_version=db_health.schema_version,
        )

        # ── Persist ───────────────────────────────────────────────────────
        ts_str        = started_at.strftime("%Y%m%d_%H%M%S")
        report_file   = f"scan_{ts_str}_{scan_id[:8]}.json"
        _save_report(self.audit_root, report, report_file)

        summary   = ScanSummary.from_report(report, report_file)
        history.append(summary)
        _save_index(self.audit_root, history)

        return report, report_file


# ---------------------------------------------------------------------------
# Read-only use cases
# ---------------------------------------------------------------------------

class GetScanHistoryUseCase:
    def __init__(self, audit_root: str):
        self.audit_root = audit_root

    def run(self) -> list[ScanSummary]:
        return sorted(
            _load_index(self.audit_root),
            key=lambda s: s.started_at,
            reverse=True,
        )


class GetCurrentIssuesUseCase:
    def __init__(self, audit_root: str):
        self.audit_root = audit_root

    def run(self) -> Optional[ScanReport]:
        history = _load_index(self.audit_root)
        if not history:
            return None
        # Sort by date; most recent first
        latest = max(history, key=lambda s: s.started_at)
        return _load_report(self.audit_root, latest.report_file)


class GetDatabaseHealthUseCase:
    def __init__(self, project_root: str):
        self.project_root = project_root

    def run(self) -> DatabaseHealth:
        return build_database_health(self.project_root)
