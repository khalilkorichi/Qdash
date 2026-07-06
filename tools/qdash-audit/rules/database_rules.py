"""
rules/database_rules.py — Room database health checks.

DB-001: Entity has no @PrimaryKey
DB-002: fallbackToDestructiveMigration detected in Room builder
DB-003: Schema version bump with no corresponding migration
DB-004: Large entity with no @Index
DB-005: Possible foreign key without @ForeignKey annotation
DB-006: Room schema JSON missing for latest declared version
"""
from __future__ import annotations

import re
from pathlib import Path
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from scanner.file_walker import FileContext, ProjectContext
    from scanner.kotlin_parser import KotlinFileMeta

from rules.base import Rule, make_issue
from models import Issue


# ── DB-001 ──────────────────────────────────────────────────────────────────
class DbNoPrimaryKeyRule(Rule):
    """Entity class has no @PrimaryKey annotation."""
    id       = "DB-001"
    title    = "Room entity missing @PrimaryKey"
    severity = "CRITICAL"
    category = "Database"

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_entity:
            return []
        if kotlin_meta.has_primary_key:
            return []
        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=file_ctx.line_count,
            description=(
                "A Room @Entity was found in this file with no @PrimaryKey annotation. "
                "Room requires every entity to have exactly one primary key. "
                "Without it, Room will fail to compile, or if using autoGenerate it may "
                "silently produce duplicate rows."
            ),
            user_symptom="خطر فقدان البيانات أو تعارضها في قاعدة البيانات — قد يتعطل التطبيق عند الفتح",
        )]


# ── DB-002 ──────────────────────────────────────────────────────────────────
class DbDestructiveMigrationRule(Rule):
    """fallbackToDestructiveMigration() detected."""
    id       = "DB-002"
    title    = "Destructive migration fallback enabled"
    severity = "CRITICAL"
    category = "Database"

    _PATTERN = re.compile(r"fallbackToDestructiveMigration\s*\(")

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        issues = []
        for m in self._PATTERN.finditer(file_ctx.content):
            ln = file_ctx.content[:m.start()].count("\n") + 1
            snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
            issues.append(make_issue(
                rule=self,
                file_path=file_ctx.rel_path,
                line_start=ln,
                line_end=ln,
                description=(
                    "`fallbackToDestructiveMigration()` tells Room to wipe all user data if "
                    "a migration path is missing. This is never safe in production. "
                    "Remove this call and implement proper migrations for every version bump."
                ),
                user_symptom="خطر مسح جميع بيانات المستخدم (معاملات، حسابات، مدخرات) عند ترقية التطبيق",
                code_snippet=snippet,
            ))
        return issues


# ── DB-003 ──────────────────────────────────────────────────────────────────
class DbMissingMigrationRule(Rule):
    """
    Checks that every consecutive schema version pair has a migration.
    This rule operates at the ProjectContext level — it reads the pre-computed
    db_health.missing_migrations produced by the Room schema parser.
    """
    id       = "DB-003"
    title    = "Missing Room migration for schema version bump"
    severity = "HIGH"
    category = "Database"

    # This rule runs once per project, not per file.
    # We tag it to the AppDatabase.kt file.
    _DB_FILE_PATTERN = re.compile(r"AppDatabase\.kt$")

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not self._DB_FILE_PATTERN.search(file_ctx.rel_path):
            return []

        # missing_migrations is populated by room_schema_parser
        missing = getattr(project_ctx, "_db_health_missing_migrations", [])
        if not missing:
            return []

        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=1,
            description=(
                f"The following schema version transitions have no migration defined: "
                f"{', '.join(missing)}. "
                "Room will crash at runtime when a user upgrades from the older version. "
                "Add the missing Migration objects to Migrations.kt and register them in AppContainerImpl."
            ),
            user_symptom="قد يسبب تعطل التطبيق عند ترقية قاعدة البيانات لدى المستخدمين القادمين من إصدار قديم",
            affected_files=["app/src/main/java/com/qdash/core/data/Migrations.kt"],
        )]


# ── DB-004 ──────────────────────────────────────────────────────────────────
class DbNoIndexRule(Rule):
    """Large entity (>5 fields) with no @Index — may cause slow queries."""
    id       = "DB-004"
    title    = "Room entity has no index on any column"
    severity = "MEDIUM"
    category = "Database"

    _FIELD_THRESHOLD = 5
    _INDEX_PATTERN   = re.compile(r"@Index|Index\s*\(")
    _FIELD_PATTERN   = re.compile(r"(?:val|var)\s+\w+\s*:")

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_entity:
            return []

        content = file_ctx.content
        field_count = len(self._FIELD_PATTERN.findall(content))
        has_index   = bool(self._INDEX_PATTERN.search(content))

        if has_index or field_count <= self._FIELD_THRESHOLD:
            return []

        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=file_ctx.line_count,
            description=(
                f"Entity has approximately {field_count} fields but no @Index annotations. "
                "Queries filtering or sorting by non-primary-key columns (e.g. accountId, date, type) "
                "will perform full table scans, degrading performance as data grows. "
                "Add @Index on frequently queried foreign-key or filter columns."
            ),
            user_symptom="قد يسبب بطء في تحميل قوائم المعاملات والحسابات مع نمو البيانات",
        )]


# ── DB-005 ──────────────────────────────────────────────────────────────────
class DbMissingForeignKeyRule(Rule):
    """
    Detects fields named *Id or *_id with no @ForeignKey in the entity.
    This is a heuristic — not all such fields are foreign keys, but it's
    a useful prompt for review.
    """
    id       = "DB-005"
    title    = "Possible missing @ForeignKey on related entity"
    severity = "MEDIUM"
    category = "Database"

    _FK_FIELD    = re.compile(r"(?:val|var)\s+(\w*[Ii]d)\s*:", re.MULTILINE)
    _HAS_FK      = re.compile(r"@ForeignKey|ForeignKey\s*\(")
    _HAS_ENTITY  = re.compile(r"@Entity")

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_entity:
            return []

        content = file_ctx.content
        if self._HAS_FK.search(content):
            return []   # Already has foreign keys declared

        if "qdash-audit: suppress DB-005" in content:
            return []   # Intentionally suppressed — polymorphic or managed-in-code FK

        fk_candidates = [
            m.group(1) for m in self._FK_FIELD.finditer(content)
            if m.group(1).lower() not in ("id",)  # skip own PK named 'id'
        ]
        if not fk_candidates:
            return []

        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=file_ctx.line_count,
            description=(
                f"Entity contains potential foreign-key fields: `{', '.join(fk_candidates)}`, "
                "but declares no @ForeignKey constraints. "
                "Without @ForeignKey, Room will not enforce referential integrity. "
                "Deleting a parent row will leave orphaned child rows (e.g. payments without a debt). "
                "Review and add @ForeignKey(onDelete=CASCADE) where appropriate."
            ),
            user_symptom="قد يسبب ظهور بيانات يتيمة أو معاملات مرتبطة بحسابات محذوفة في الشاشات",
        )]


# ── DB-006 ──────────────────────────────────────────────────────────────────
class DbSchemaMissingRule(Rule):
    """Schema JSON for the declared @Database version is not exported."""
    id       = "DB-006"
    title    = "Room schema export file missing for current version"
    severity = "INFO"
    category = "Database"

    _DB_FILE_PATTERN = re.compile(r"AppDatabase\.kt$")

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_database:
            return []
        if not self._DB_FILE_PATTERN.search(file_ctx.rel_path):
            return []

        declared_version = kotlin_meta.db_version
        if not declared_version:
            return []

        found_versions = getattr(project_ctx, "_db_schema_versions", [])
        if declared_version in found_versions:
            return []

        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=1,
            description=(
                f"@Database declares version={declared_version} but no matching schema JSON "
                f"was found under app/schemas/. "
                "Ensure `exportSchema = true` is set and the schema file is committed to source control. "
                "Schema files are required for the audit tool's migration coverage checks."
            ),
            user_symptom="لا يوجد تأثير مباشر على المستخدم، لكن يُعيق فحص التحديثات المستقبلية",
        )]


DATABASE_RULES: list[Rule] = [
    DbNoPrimaryKeyRule(),
    DbDestructiveMigrationRule(),
    DbMissingMigrationRule(),
    DbNoIndexRule(),
    DbMissingForeignKeyRule(),
    DbSchemaMissingRule(),
]
