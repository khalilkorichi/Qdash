"""
scanner/room_schema_parser.py — Room exported JSON schema parser.

Reads Room schema JSON files from app/schemas/ and Migrations.kt to:
  - Enumerate entities, their columns, indices, and foreign keys.
  - Determine the current schema version.
  - Verify every version gap has a corresponding migration.
  - Detect missing or destructive migrations.
"""
from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Optional

from models import ColumnInfo, EntityInfo, DatabaseHealth


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _find_schema_dirs(project_root: str) -> list[Path]:
    """Find all Room schema export directories under app/schemas/."""
    schemas_root = Path(project_root) / "app" / "schemas"
    if not schemas_root.exists():
        return []
    return [d for d in schemas_root.iterdir() if d.is_dir()]


def _parse_schema_file(schema_path: Path) -> Optional[dict]:
    """Parse a single Room schema JSON file. Returns raw dict or None."""
    try:
        with open(schema_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None


def _extract_entities_from_schema(schema_data: dict) -> list[EntityInfo]:
    """Build EntityInfo list from a parsed Room schema JSON."""
    entities: list[EntityInfo] = []
    db_section = schema_data.get("database", {})
    raw_entities = db_section.get("entities", [])

    for e in raw_entities:
        columns: list[ColumnInfo] = []
        pk_names: set[str] = set()

        # Primary key columns
        pk_section = e.get("primaryKey", {})
        pk_cols = pk_section.get("columnNames", [])
        pk_names.update(pk_cols)

        for col in e.get("fields", []):
            col_name = col.get("columnName", col.get("name", ""))
            columns.append(ColumnInfo(
                name=col_name,
                type=col.get("affinity", col.get("type", "")),
                not_null=col.get("notNull", False),
                primary_key=(col_name in pk_names),
            ))

        # Indices
        index_cols: list[str] = []
        for idx in e.get("indices", []):
            for col_name in idx.get("columnNames", idx.get("columns", [])):
                index_cols.append(col_name)

        # Foreign keys
        fk_tables: list[str] = []
        for fk in e.get("foreignKeys", []):
            fk_tables.append(fk.get("table", ""))

        entities.append(EntityInfo(
            name=e.get("entityClass", e.get("tableName", "Unknown")).split(".")[-1],
            table_name=e.get("tableName", ""),
            columns=columns,
            indices=index_cols,
            foreign_keys=fk_tables,
            has_primary_key=bool(pk_names),
        ))

    return entities


def _extract_migration_ranges_from_kt(migrations_kt_path: Path) -> list[tuple[int, int]]:
    """
    Parse Migrations.kt to extract all (fromVersion, toVersion) pairs.
    Looks for patterns like: object MIGRATION_X_Y : Migration(X, Y)
    or: Migration(X, Y) { ... }
    """
    if not migrations_kt_path.exists():
        return []

    try:
        content = migrations_kt_path.read_text(encoding="utf-8", errors="replace")
    except Exception:
        return []

    ranges: list[tuple[int, int]] = []
    # Pattern: Migration(from, to)
    for m in re.finditer(r"Migration\s*\(\s*(\d+)\s*,\s*(\d+)\s*\)", content):
        ranges.append((int(m.group(1)), int(m.group(2))))

    return sorted(set(ranges))


def _find_migrations_kt(project_root: str) -> Optional[Path]:
    """Locate the Migrations.kt file anywhere under the project source tree."""
    src_root = Path(project_root) / "app" / "src" / "main" / "java"
    for root, dirs, files in os.walk(src_root):
        for fname in files:
            if fname == "Migrations.kt":
                return Path(root) / fname
    return None


def _check_migration_coverage(
    schema_versions: list[int],
    migration_ranges: list[tuple[int, int]],
) -> list[str]:
    """
    Given sorted schema version numbers, check that every consecutive pair
    v(n) -> v(n+1) is covered by at least one migration range.
    Returns list of missing "vX→vY" strings.
    """
    missing: list[str] = []
    sorted_versions = sorted(schema_versions)
    for i in range(len(sorted_versions) - 1):
        fr = sorted_versions[i]
        to = sorted_versions[i + 1]
        covered = any(r_from == fr and r_to == to for r_from, r_to in migration_ranges)
        if not covered:
            missing.append(f"v{fr}→v{to}")
    return missing


def _check_destructive_migration(project_root: str) -> bool:
    """Scan all Kotlin files for fallbackToDestructiveMigration usage."""
    src_root = Path(project_root) / "app" / "src" / "main" / "java"
    pattern = re.compile(r"fallbackToDestructiveMigration\s*\(")
    for root, dirs, files in os.walk(src_root):
        for fname in files:
            if fname.endswith(".kt"):
                try:
                    content = (Path(root) / fname).read_text(encoding="utf-8", errors="replace")
                    if pattern.search(content):
                        return True
                except Exception:
                    pass
    return False


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------

def build_database_health(project_root: str) -> DatabaseHealth:
    """
    Parse Room schema JSONs + Migrations.kt to produce a DatabaseHealth report.
    Uses the latest (highest-numbered) schema JSON as the authoritative view.
    """
    schema_dirs = _find_schema_dirs(project_root)
    all_schema_files: list[tuple[int, Path]] = []

    for schema_dir in schema_dirs:
        for json_file in schema_dir.glob("*.json"):
            try:
                ver = int(json_file.stem)
                all_schema_files.append((ver, json_file))
            except ValueError:
                pass

    all_schema_files.sort(key=lambda x: x[0])
    schema_versions = [v for v, _ in all_schema_files]
    schema_file_names = [str(p) for _, p in all_schema_files]

    # Parse the latest schema for entity details
    entities: list[EntityInfo] = []
    current_version = 0
    if all_schema_files:
        latest_ver, latest_path = all_schema_files[-1]
        current_version = latest_ver
        schema_data = _parse_schema_file(latest_path)
        if schema_data:
            entities = _extract_entities_from_schema(schema_data)

    # Parse migrations
    migrations_kt = _find_migrations_kt(project_root)
    migration_ranges = _extract_migration_ranges_from_kt(migrations_kt) if migrations_kt else []

    # Check migration coverage
    missing_migrations = _check_migration_coverage(schema_versions, migration_ranges)

    # Flag problematic entities
    flagged_entities: list[str] = []
    for entity in entities:
        if not entity.has_primary_key:
            entity.is_flagged = True
            entity.flag_reasons.append("No @PrimaryKey")
            flagged_entities.append(entity.name)
        elif not entity.indices and len(entity.columns) > 5:
            entity.is_flagged = True
            entity.flag_reasons.append("No indices on >5-column entity")
            # Don't add to flagged list for this (medium severity, not critical)

    # Check destructive migration
    has_destructive = _check_destructive_migration(project_root)

    return DatabaseHealth(
        schema_version=current_version,
        entity_count=len(entities),
        entities=entities,
        migration_versions=[[fr, to] for fr, to in migration_ranges],
        flagged_entities=flagged_entities,
        missing_migrations=missing_migrations,
        has_destructive_migration=has_destructive,
        schema_files_found=schema_file_names,
    )
