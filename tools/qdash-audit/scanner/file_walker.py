"""
scanner/file_walker.py — Recursive file system walker with SHA-256 change detection.

Walks the Qdash project root, hashes each relevant source file, and compares
against the previous run's hash index to identify new/changed/deleted files.
"""
from __future__ import annotations

import hashlib
import json
import os
from dataclasses import dataclass, field
from pathlib import Path
from datetime import datetime, timezone
from typing import Optional

# File extensions we care about
SCAN_EXTENSIONS = {".kt", ".kts", ".xml", ".json"}

# Directories to always skip
SKIP_DIRS = {
    ".git", "build", ".gradle", ".idea", "node_modules",
    "__pycache__", ".agents", "releases", "scratch", "assets",
    ".build-outputs", "audit_data", "reports", "tests",
}

# Specific directories/files inside the project that aren't source code
SKIP_NAMES = {"gradlew", "gradlew.bat"}


@dataclass
class FileContext:
    """All metadata and content for a single scanned file."""
    path:          str          # absolute path
    rel_path:      str          # relative to project root
    extension:     str
    size_bytes:    int
    sha256:        str
    is_changed:    bool         # True if hash differs from previous run
    is_new:        bool         # True if not seen in previous run
    lines:         list[str]    = field(default_factory=list)  # raw content lines (stripped)
    line_count:    int          = 0

    @property
    def content(self) -> str:
        return "\n".join(self.lines)

    def is_kotlin(self) -> bool:
        return self.extension in (".kt", ".kts")

    def is_xml(self) -> bool:
        return self.extension == ".xml"

    def is_json(self) -> bool:
        return self.extension == ".json"


@dataclass
class ProjectContext:
    """Aggregate view of all scanned files, used by the rule engine."""
    project_root:     str
    files:            list[FileContext]       = field(default_factory=list)
    kotlin_files:     list[FileContext]       = field(default_factory=list)
    xml_files:        list[FileContext]       = field(default_factory=list)
    json_files:       list[FileContext]       = field(default_factory=list)
    deleted_rel_paths: list[str]             = field(default_factory=list)
    scanned_at:       str                    = field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat()
    )

    def by_rel_path(self, rel: str) -> Optional[FileContext]:
        for f in self.files:
            if f.rel_path == rel or f.rel_path.replace("\\", "/") == rel.replace("\\", "/"):
                return f
        return None

    def kotlin_in_dir(self, subdir: str) -> list[FileContext]:
        """Return all Kotlin files whose rel_path contains subdir."""
        subdir = subdir.replace("\\", "/")
        return [f for f in self.kotlin_files if subdir in f.rel_path.replace("\\", "/")]


def _sha256_file(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def _load_hash_index(index_path: Path) -> dict[str, str]:
    """Returns {rel_path: sha256} from the persisted index."""
    if index_path.exists():
        try:
            with open(index_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return {}
    return {}


def _save_hash_index(index_path: Path, index: dict[str, str]) -> None:
    index_path.parent.mkdir(parents=True, exist_ok=True)
    with open(index_path, "w", encoding="utf-8") as f:
        json.dump(index, f, indent=2, ensure_ascii=False)


def walk_project(
    project_root: str,
    audit_data_dir: str,
    force_full: bool = False,
) -> ProjectContext:
    """
    Walk the project, compute hashes, detect changes.

    Args:
        project_root:   Absolute path to the Qdash repo root.
        audit_data_dir: Where the hash index is stored (tool's own data dir).
        force_full:     If True, treat every file as changed (full re-scan).

    Returns:
        ProjectContext with all discovered FileContext objects.
    """
    root = Path(project_root).resolve()
    index_path = Path(audit_data_dir) / "file_index.json"
    old_index: dict[str, str] = {} if force_full else _load_hash_index(index_path)
    new_index: dict[str, str] = {}

    ctx = ProjectContext(project_root=str(root))

    for dirpath, dirnames, filenames in os.walk(root):
        # Prune skip directories in-place (modifies traversal)
        dirnames[:] = [
            d for d in dirnames
            if d not in SKIP_DIRS and not d.startswith(".")
        ]

        for fname in sorted(filenames):
            if fname in SKIP_NAMES:
                continue

            full_path = Path(dirpath) / fname
            ext = full_path.suffix.lower()

            if ext not in SCAN_EXTENSIONS:
                continue

            # Skip schema JSONs that live in the tool itself
            rel = str(full_path.relative_to(root)).replace("\\", "/")
            if rel.startswith("tools/"):
                continue

            try:
                sha = _sha256_file(str(full_path))
            except (PermissionError, OSError):
                continue

            new_index[rel] = sha
            is_new = rel not in old_index
            is_changed = is_new or (old_index.get(rel) != sha) or force_full

            try:
                with open(full_path, "r", encoding="utf-8", errors="replace") as fh:
                    raw_lines = fh.readlines()
            except (PermissionError, OSError):
                raw_lines = []

            lines = [ln.rstrip("\r\n") for ln in raw_lines]
            size = full_path.stat().st_size

            fc = FileContext(
                path=str(full_path),
                rel_path=rel,
                extension=ext,
                size_bytes=size,
                sha256=sha,
                is_changed=is_changed,
                is_new=is_new,
                lines=lines,
                line_count=len(lines),
            )

            ctx.files.append(fc)
            if fc.is_kotlin():
                ctx.kotlin_files.append(fc)
            elif fc.is_xml():
                ctx.xml_files.append(fc)
            elif fc.is_json():
                ctx.json_files.append(fc)

    # Detect deleted files
    ctx.deleted_rel_paths = [p for p in old_index if p not in new_index]

    # Persist updated index
    _save_hash_index(index_path, new_index)

    return ctx
