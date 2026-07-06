#!/usr/bin/env python3
"""
audit.py — Qdash Code & Database Health Audit Tool
Entry point / CLI controller.

Usage:
    python audit.py
    python audit.py --full               # force re-scan all files
    python audit.py --report-only        # regenerate index.json only
    python audit.py --project-root <path>

Outputs:
    - reports/scan_<timestamp>.json      (full report, never overwritten)
    - reports/index.json                 (rolling scan history)
    - Prints summary to stdout
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

# ─── Path setup ─────────────────────────────────────────────────────────────
# audit.py lives at tools/qdash-audit/audit.py
# project root is two levels up
_AUDIT_ROOT    = Path(__file__).resolve().parent
_PROJECT_ROOT  = _AUDIT_ROOT.parent.parent

# Add audit root to sys.path so relative imports work
sys.path.insert(0, str(_AUDIT_ROOT))

from use_cases import (
    RunFullScanUseCase,
    GetScanHistoryUseCase,
    GetCurrentIssuesUseCase,
    GetDatabaseHealthUseCase,
)
from models import SEVERITY_ORDER, SEVERITY_COLORS


# ─── Terminal colours (ANSI, Windows 10+ compatible) ─────────────────────────
def _ansi(code: str) -> str:
    return f"\033[{code}m"


RESET  = _ansi("0")
BOLD   = _ansi("1")
RED    = _ansi("91")
ORANGE = _ansi("93")
YELLOW = _ansi("33")
BLUE   = _ansi("94")
GRAY   = _ansi("37")
GREEN  = _ansi("92")
CYAN   = _ansi("96")

SEV_COLOR = {
    "CRITICAL": RED,
    "HIGH":     ORANGE,
    "MEDIUM":   YELLOW,
    "LOW":      BLUE,
    "INFO":     GRAY,
}

def _enable_ansi_on_windows() -> None:
    """Enable VT100 escape codes and UTF-8 output on Windows 10+ cmd/powershell."""
    if sys.platform == "win32":
        try:
            import ctypes
            kernel32 = ctypes.windll.kernel32
            kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
        except Exception:
            pass
        # Force UTF-8 output so box-drawing chars work
        try:
            import io
            sys.stdout = io.TextIOWrapper(
                sys.stdout.buffer, encoding='utf-8', errors='replace'
            )
        except Exception:
            pass


# ─── CLI ─────────────────────────────────────────────────────────────────────
def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Qdash Code & Database Health Audit Tool",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument(
        "--full", action="store_true",
        help="Force re-scan all files (ignore hash cache)"
    )
    p.add_argument(
        "--report-only", action="store_true",
        help="Regenerate index.json from existing reports without scanning"
    )
    p.add_argument(
        "--project-root", default=str(_PROJECT_ROOT),
        help=f"Path to the Qdash repo root (default: {_PROJECT_ROOT})"
    )
    p.add_argument(
        "--audit-root", default=str(_AUDIT_ROOT),
        help=f"Path to tools/qdash-audit/ (default: {_AUDIT_ROOT})"
    )
    p.add_argument(
        "--json", action="store_true",
        help="Print the scan report JSON to stdout (machine-readable)"
    )
    p.add_argument(
        "--history", action="store_true",
        help="Print scan history summary and exit"
    )
    return p.parse_args()


# ─── Print helpers ────────────────────────────────────────────────────────────
def _print_header(project_root: str) -> None:
    print(f"\n{BOLD}{CYAN}╔══════════════════════════════════════════════╗{RESET}")
    print(f"{BOLD}{CYAN}║   Qdash Code & DB Health Audit Tool v1.0    ║{RESET}")
    print(f"{BOLD}{CYAN}╚══════════════════════════════════════════════╝{RESET}")
    print(f"  Project: {BOLD}{project_root}{RESET}\n")


def _print_severity_bar(issues_by_severity: dict[str, int]) -> None:
    total = sum(issues_by_severity.values())
    print(f"  {'Issues by severity':25s}", end="")
    for sev in SEVERITY_ORDER:
        count = issues_by_severity.get(sev, 0)
        color = SEV_COLOR.get(sev, "")
        if count:
            print(f"  {color}{sev}:{RESET} {BOLD}{count}{RESET}", end="")
    print(f"\n  {'Total issues':25s}  {BOLD}{total}{RESET}")


def _print_report_summary(report) -> None:
    print(f"\n{BOLD}Scan complete{RESET}")
    print(f"  Files scanned:  {report.total_files_scanned}")
    print(f"  Duration:       {report.duration_seconds}s")
    print(f"  DB schema ver:  {report.schema_version}")
    _print_severity_bar(report.issues_by_severity)

    new  = len(report.new_issue_ids)
    res  = len(report.resolved_issue_ids)
    if new:
        print(f"\n  {RED}▲ {new} new issue(s) introduced{RESET}")
    if res:
        print(f"  {GREEN}▼ {res} issue(s) resolved{RESET}")

    if report.total_issues == 0:
        print(f"\n  {GREEN}{BOLD}✓ No issues found!{RESET}")
        return

    print(f"\n{BOLD}Issue list{RESET} (grouped by severity):\n")
    grouped: dict[str, list] = {sev: [] for sev in SEVERITY_ORDER}
    for issue in report.issues:
        grouped.setdefault(issue.severity, []).append(issue)

    for sev in SEVERITY_ORDER:
        issues = grouped.get(sev, [])
        if not issues:
            continue
        color = SEV_COLOR.get(sev, "")
        print(f"  {color}{BOLD}── {sev} ({len(issues)}) ──{RESET}")
        for iss in issues:
            loc = f":{iss.line_start}" if iss.line_start else ""
            print(f"    [{iss.rule_id}] {iss.title}")
            print(f"           {GRAY}{iss.file_path}{loc}{RESET}")
            if iss.user_symptom:
                print(f"           ↳ {iss.user_symptom}")
        print()


def _print_history(history) -> None:
    if not history:
        print("  No scans in history yet.")
        return
    print(f"\n{BOLD}Scan History{RESET} ({len(history)} scans)\n")
    print(f"  {'Timestamp':26s} {'Files':>6s} {'Issues':>7s} {'New':>5s} {'Resolved':>9s} {'Duration':>9s}")
    print("  " + "-" * 70)
    for s in history:
        dt  = s.started_at[:19].replace("T", " ")
        new = f"+{s.new_issues}" if s.new_issues else "  0"
        res = f"-{s.resolved_issues}" if s.resolved_issues else "  0"
        print(f"  {dt:26s} {s.total_files_scanned:6d} {s.total_issues:7d} "
              f"{new:>5s} {res:>9s} {s.duration_seconds:>8.1f}s")



def _write_dashboard_data(audit_root: str, report) -> None:
    """Write dashboard/data.js with embedded scan data for offline file:// access."""
    history = GetScanHistoryUseCase(audit_root).run()
    history_dicts = [s.to_dict() for s in history]
    report_dict   = report.to_dict() if hasattr(report, "to_dict") else report

    data_js_path = Path(audit_root) / "dashboard" / "data.js"
    data_js_path.parent.mkdir(parents=True, exist_ok=True)

    payload = json.dumps(
        {"current": report_dict, "history": history_dicts},
        ensure_ascii=False, indent=2,
    )
    data_js_path.write_text(
        "// Auto-generated by audit.py\nwindow.AUDIT_DATA = " + payload + ";\n",
        encoding="utf-8",
    )
    print("  [+] Dashboard data written to dashboard/data.js")

# ─── Main ─────────────────────────────────────────────────────────────────────
def main() -> int:
    _enable_ansi_on_windows()
    args = parse_args()

    project_root = str(Path(args.project_root).resolve())
    audit_root   = str(Path(args.audit_root).resolve())

    _print_header(project_root)

    # ── History mode ─────────────────────────────────────────────────────────
    if args.history:
        history = GetScanHistoryUseCase(audit_root).run()
        _print_history(history)
        return 0

    # ── Report-only mode ─────────────────────────────────────────────────────
    if args.report_only:
        print("  report-only mode: regenerating index.json…")
        report = GetCurrentIssuesUseCase(audit_root).run()
        if report:
            _print_report_summary(report)
        else:
            print("  No reports found. Run a full scan first.")
        return 0

    # ── Full scan ─────────────────────────────────────────────────────────────
    print(f"  Mode: {'FULL (forced)' if args.full else 'incremental'}\n")

    use_case = RunFullScanUseCase(
        project_root=project_root,
        audit_root=audit_root,
        force_full=args.full,
    )
    report, report_file = use_case.run()

    if args.json:
        print(json.dumps(report.to_dict(), indent=2, ensure_ascii=False))
        return 0

    _print_report_summary(report)

    reports_path   = Path(audit_root) / "reports" / report_file
    dashboard_path = Path(audit_root) / "dashboard" / "index.html"

    # Write embedded data.js so dashboard works via file://
    _write_dashboard_data(audit_root, report)

    print(f"\n  {BOLD}Report saved:{RESET} {reports_path}")
    print(f"  {BOLD}Dashboard:   {RESET} {dashboard_path}")
    print(f"\n  {GREEN}Open dashboard in browser (works offline):{RESET}")
    print(f"  {CYAN}{dashboard_path}{RESET}\n")

    # Exit 0 = ran successfully (having issues is normal, not an error)
    return 0


if __name__ == "__main__":
    sys.exit(main())

