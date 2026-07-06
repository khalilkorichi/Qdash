"""
models.py — Core data models for the Qdash Audit Tool.
All dataclasses serialise/deserialise cleanly to/from JSON.
"""
from __future__ import annotations

import uuid
from dataclasses import dataclass, field, asdict
from typing import Optional
from datetime import datetime, timezone


# ---------------------------------------------------------------------------
# Severity constants (strictly non-overlapping, ordered)
# ---------------------------------------------------------------------------
SEVERITY_CRITICAL = "CRITICAL"
SEVERITY_HIGH     = "HIGH"
SEVERITY_MEDIUM   = "MEDIUM"
SEVERITY_LOW      = "LOW"
SEVERITY_INFO     = "INFO"

SEVERITY_ORDER = [SEVERITY_CRITICAL, SEVERITY_HIGH, SEVERITY_MEDIUM, SEVERITY_LOW, SEVERITY_INFO]

SEVERITY_COLORS = {
    SEVERITY_CRITICAL: "#ef4444",
    SEVERITY_HIGH:     "#f97316",
    SEVERITY_MEDIUM:   "#eab308",
    SEVERITY_LOW:      "#3b82f6",
    SEVERITY_INFO:     "#6b7280",
}


# ---------------------------------------------------------------------------
# Issue
# ---------------------------------------------------------------------------
@dataclass
class Issue:
    """A single detected problem in the codebase."""
    rule_id:        str
    severity:       str
    category:       str
    file_path:      str
    line_start:     int
    line_end:       int
    title:          str
    description:    str
    user_symptom:   str          # Arabic phrase — expected app-level symptom
    affected_files: list[str]    = field(default_factory=list)
    code_snippet:   str          = ""
    id:             str          = field(default_factory=lambda: str(uuid.uuid4()))
    detected_at:    str          = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    def to_dict(self) -> dict:
        return asdict(self)

    @staticmethod
    def from_dict(d: dict) -> "Issue":
        return Issue(**d)


# ---------------------------------------------------------------------------
# Entity / migration metadata (from Room schema parser)
# ---------------------------------------------------------------------------
@dataclass
class ColumnInfo:
    name:        str
    type:        str
    not_null:    bool  = False
    primary_key: bool  = False

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass
class EntityInfo:
    name:          str
    table_name:    str
    columns:       list[ColumnInfo]  = field(default_factory=list)
    indices:       list[str]         = field(default_factory=list)   # index column names
    foreign_keys:  list[str]         = field(default_factory=list)   # referenced table names
    has_primary_key: bool            = True
    is_flagged:    bool              = False
    flag_reasons:  list[str]         = field(default_factory=list)

    def to_dict(self) -> dict:
        d = asdict(self)
        return d


@dataclass
class DatabaseHealth:
    schema_version:      int
    entity_count:        int
    entities:            list[EntityInfo]        = field(default_factory=list)
    migration_versions:  list[list[int]]         = field(default_factory=list)  # [[from,to], ...]
    flagged_entities:    list[str]               = field(default_factory=list)
    missing_migrations:  list[str]               = field(default_factory=list)  # "vX→vY"
    has_destructive_migration: bool              = False
    schema_files_found:  list[str]               = field(default_factory=list)

    def to_dict(self) -> dict:
        d = asdict(self)
        return d


# ---------------------------------------------------------------------------
# Scan report
# ---------------------------------------------------------------------------
@dataclass
class ScanReport:
    """Complete result of one full scan run."""
    scan_id:             str
    started_at:          str
    completed_at:        str
    duration_seconds:    float
    project_root:        str
    total_files_scanned: int
    total_issues:        int
    issues_by_severity:  dict[str, int]
    issues:              list[Issue]      = field(default_factory=list)
    new_issue_ids:       list[str]        = field(default_factory=list)
    resolved_issue_ids:  list[str]        = field(default_factory=list)
    db_health:           Optional[DatabaseHealth] = None
    schema_version:      int              = 0

    def to_dict(self) -> dict:
        d = asdict(self)
        return d

    @staticmethod
    def from_dict(d: dict) -> "ScanReport":
        issues = [Issue.from_dict(i) for i in d.pop("issues", [])]
        db_h_raw = d.pop("db_health", None)
        db_health = None
        if db_h_raw:
            entities = [EntityInfo(**e) for e in db_h_raw.pop("entities", [])]
            db_health = DatabaseHealth(entities=entities, **db_h_raw)
        return ScanReport(issues=issues, db_health=db_health, **d)


# ---------------------------------------------------------------------------
# Scan index entry (lightweight summary for the history panel)
# ---------------------------------------------------------------------------
@dataclass
class ScanSummary:
    scan_id:            str
    started_at:         str
    duration_seconds:   float
    total_files_scanned: int
    total_issues:       int
    issues_by_severity: dict[str, int]
    report_file:        str
    new_issues:         int = 0
    resolved_issues:    int = 0

    def to_dict(self) -> dict:
        return asdict(self)

    @staticmethod
    def from_dict(d: dict) -> "ScanSummary":
        return ScanSummary(**d)

    @staticmethod
    def from_report(report: ScanReport, report_file: str) -> "ScanSummary":
        return ScanSummary(
            scan_id=report.scan_id,
            started_at=report.started_at,
            duration_seconds=report.duration_seconds,
            total_files_scanned=report.total_files_scanned,
            total_issues=report.total_issues,
            issues_by_severity=report.issues_by_severity,
            report_file=report_file,
            new_issues=len(report.new_issue_ids),
            resolved_issues=len(report.resolved_issue_ids),
        )
