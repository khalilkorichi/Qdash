"""
scanner/kotlin_parser.py — Lightweight regex-based Kotlin metadata extractor.

Extracts structural and semantic metadata from .kt/.kts files using regex
patterns, without requiring a full Kotlin compiler or AST library.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Optional

from scanner.file_walker import FileContext


# ---------------------------------------------------------------------------
# Compiled regex patterns
# ---------------------------------------------------------------------------

_RE_PACKAGE      = re.compile(r"^\s*package\s+([\w.]+)", re.MULTILINE)
_RE_IMPORT       = re.compile(r"^\s*import\s+([\w.*]+)", re.MULTILINE)
_RE_CLASS        = re.compile(r"^\s*(?:(?:data|sealed|abstract|open|inner)\s+)*class\s+(\w+)", re.MULTILINE)
_RE_OBJECT       = re.compile(r"^\s*(?:companion\s+)?object\s*(\w*)", re.MULTILINE)
_RE_FUN          = re.compile(r"^\s*(suspend\s+)?fun\s+(\w+)\s*\(", re.MULTILINE)
_RE_ANNOTATION   = re.compile(r"@(\w+)", re.MULTILINE)

# Specific annotations
_RE_COMPOSABLE   = re.compile(r"@Composable", re.MULTILINE)
_RE_ENTITY       = re.compile(r"@Entity", re.MULTILINE)
_RE_DAO          = re.compile(r"@Dao", re.MULTILINE)
_RE_DATABASE     = re.compile(r"@Database", re.MULTILINE)
_RE_HILT_VM      = re.compile(r"@HiltViewModel", re.MULTILINE)
_RE_MODULE       = re.compile(r"@Module", re.MULTILINE)

# Threading / blocking patterns
_RE_RUNBLOCKING  = re.compile(r"\brunBlocking\s*[({]", re.MULTILINE)
_RE_MAIN_DISP    = re.compile(r"Dispatchers\.Main", re.MULTILINE)
_RE_IO_DISP      = re.compile(r"Dispatchers\.IO", re.MULTILINE)

# Room patterns
_RE_DB_VERSION   = re.compile(r"@Database\s*\([^)]*version\s*=\s*(\d+)", re.DOTALL)
_RE_DESTRUCTIVE  = re.compile(r"fallbackToDestructiveMigration\s*\(", re.MULTILINE)

# Architecture layer patterns
_RE_IMPORT_DATA  = re.compile(r"^\s*import\s+[\w.]*\.data\.(?!local\.dao\.|local\.entities\.)(\w+)", re.MULTILINE)
_RE_DIRECT_DAO   = re.compile(r"\bdatabase\.\w+Dao\(\)", re.MULTILINE)
_RE_DIRECT_DB_IO = re.compile(r"\b(?:database\.\w+Dao\(\)|File\s*\(|FileOutputStream|FileInputStream|BufferedWriter|PrintWriter)\b", re.MULTILINE)

# RTL patterns
_RE_LTR_FORCE    = re.compile(r"layoutDirection\s*=\s*LayoutDirection\.Ltr", re.MULTILINE)
_RE_HARDCODED_STR= re.compile(r'Text\s*\(\s*"([^"]{3,})"', re.MULTILINE)
_RE_STRING_RES   = re.compile(r"stringResource\s*\(", re.MULTILINE)

# Suspend / coroutine
_RE_SUSPEND      = re.compile(r"\bsuspend\s+fun\b", re.MULTILINE)
_RE_FLOW         = re.compile(r"\bFlow\s*<", re.MULTILINE)

# Primary key
_RE_PK           = re.compile(r"@PrimaryKey", re.MULTILINE)

# StateFlow / LiveData references
_RE_STATE_FLOW   = re.compile(r"\bMutableStateFlow\s*<([\w<>, ?]+)>", re.MULTILINE)
_RE_LIVE_DATA    = re.compile(r"\bMutableLiveData\s*<([\w<>, ?]+)>", re.MULTILINE)


# ---------------------------------------------------------------------------
# Output dataclass
# ---------------------------------------------------------------------------
@dataclass
class KotlinFileMeta:
    """Parsed metadata from a single Kotlin file."""
    rel_path:              str
    package:               str                 = ""
    imports:               list[str]           = field(default_factory=list)
    class_names:           list[str]           = field(default_factory=list)
    function_names:        list[str]           = field(default_factory=list)
    suspend_function_names: list[str]          = field(default_factory=list)

    # Annotations detected (booleans)
    has_composable:        bool = False
    has_entity:            bool = False
    has_dao:               bool = False
    has_database:          bool = False
    has_hilt_viewmodel:    bool = False
    has_module:            bool = False

    # Patterns detected
    has_run_blocking:      bool = False
    has_main_dispatcher:   bool = False
    has_io_dispatcher:     bool = False
    has_destructive_migration: bool = False
    has_ltr_force:         bool = False
    has_direct_dao_call:   bool = False
    has_suspend_funs:      bool = False
    has_flow:              bool = False
    has_primary_key:       bool = False

    # Line-level detail maps: {line_number: matched_text}
    run_blocking_lines:    dict[int, str]      = field(default_factory=dict)
    main_dispatcher_lines: dict[int, str]      = field(default_factory=dict)
    ltr_force_lines:       dict[int, str]      = field(default_factory=dict)
    hardcoded_str_lines:   dict[int, str]      = field(default_factory=dict)
    direct_dao_lines:      dict[int, str]      = field(default_factory=dict)
    direct_db_io_lines:    dict[int, str]      = field(default_factory=dict)
    state_flow_types:      list[str]           = field(default_factory=list)

    # Room-specific
    db_version:            Optional[int]       = None
    non_suspend_dao_funs:  list[tuple[int,str]] = field(default_factory=list)  # [(line, name)]

    # Layer
    layer:                 str                 = ""  # "presentation", "data", "domain", "core"


def _find_line_number(content: str, match_start: int) -> int:
    """Return 1-based line number for a character offset in content."""
    return content[:match_start].count("\n") + 1


def _line_matches(content: str, pattern: re.Pattern) -> dict[int, str]:
    """Return {line_no: matched_text} for all matches of pattern."""
    result: dict[int, str] = {}
    for m in pattern.finditer(content):
        ln = _find_line_number(content, m.start())
        result[ln] = m.group(0)
    return result


def _detect_layer(rel_path: str) -> str:
    p = rel_path.replace("\\", "/")
    if "/presentation/" in p:
        return "presentation"
    if "/data/" in p:
        return "data"
    if "/domain/" in p:
        return "domain"
    if "/core/" in p:
        return "core"
    if "/ui/" in p:
        return "ui"
    return "unknown"


def parse_kotlin_file(fc: FileContext) -> KotlinFileMeta:
    """
    Extract all relevant metadata from a Kotlin FileContext.
    Returns a KotlinFileMeta object populated via regex analysis.
    """
    content = fc.content
    meta = KotlinFileMeta(rel_path=fc.rel_path)

    meta.layer = _detect_layer(fc.rel_path)

    # Package
    pkg_m = _RE_PACKAGE.search(content)
    meta.package = pkg_m.group(1) if pkg_m else ""

    # Imports
    meta.imports = _RE_IMPORT.findall(content)

    # Class names
    meta.class_names = _RE_CLASS.findall(content)

    # Functions
    all_funs = _RE_FUN.findall(content)
    meta.suspend_function_names = [name for suspend, name in all_funs if suspend.strip()]
    meta.function_names = [name for _, name in all_funs]
    meta.has_suspend_funs = bool(meta.suspend_function_names)

    # Annotation flags
    meta.has_composable  = bool(_RE_COMPOSABLE.search(content))
    meta.has_entity      = bool(_RE_ENTITY.search(content))
    meta.has_dao         = bool(_RE_DAO.search(content))
    meta.has_database    = bool(_RE_DATABASE.search(content))
    meta.has_hilt_viewmodel = bool(_RE_HILT_VM.search(content))
    meta.has_module      = bool(_RE_MODULE.search(content))
    meta.has_flow        = bool(_RE_FLOW.search(content))
    meta.has_primary_key = bool(_RE_PK.search(content))
    meta.has_destructive_migration = bool(_RE_DESTRUCTIVE.search(content))
    meta.has_ltr_force   = bool(_RE_LTR_FORCE.search(content))
    meta.has_run_blocking = bool(_RE_RUNBLOCKING.search(content))
    meta.has_main_dispatcher = bool(_RE_MAIN_DISP.search(content))
    meta.has_io_dispatcher = bool(_RE_IO_DISP.search(content))
    meta.has_direct_dao_call = bool(_RE_DIRECT_DAO.search(content))

    # Line-level detail maps
    meta.run_blocking_lines    = _line_matches(content, _RE_RUNBLOCKING)
    meta.main_dispatcher_lines = _line_matches(content, _RE_MAIN_DISP)
    meta.ltr_force_lines       = _line_matches(content, _RE_LTR_FORCE)
    meta.hardcoded_str_lines   = _line_matches(content, _RE_HARDCODED_STR)
    meta.direct_dao_lines      = _line_matches(content, _RE_DIRECT_DAO)
    meta.direct_db_io_lines    = _line_matches(content, _RE_DIRECT_DB_IO)

    # StateFlow/LiveData type tracking (for DUP-001)
    meta.state_flow_types = _RE_STATE_FLOW.findall(content) + _RE_LIVE_DATA.findall(content)

    # Room DB version
    db_ver_m = _RE_DB_VERSION.search(content)
    if db_ver_m:
        try:
            meta.db_version = int(db_ver_m.group(1))
        except ValueError:
            pass

    # Non-suspend DAO functions (THR-001)
    if meta.has_dao:
        meta.non_suspend_dao_funs = _find_non_suspend_dao_functions(fc.lines)

    return meta


def _find_non_suspend_dao_functions(lines: list[str]) -> list[tuple[int, str]]:
    """
    Find DAO functions that are NOT suspend and do NOT return Flow<...>.
    These would be synchronous blocking DB calls.
    """
    results: list[tuple[int, str]] = []
    for i, line in enumerate(lines, start=1):
        stripped = line.strip()
        # Skip comments
        if stripped.startswith("//") or stripped.startswith("*"):
            continue
        # Match plain `fun` declaration (not suspend, not abstract interface boilerplate override)
        m = re.match(r"^\s*(?!suspend\s)fun\s+(\w+)\s*\(", line)
        if m:
            fun_name = m.group(1)
            # If the return type includes Flow or Unit we skip
            # We look ahead a bit for the return type on same line
            if "Flow<" in line or ": Flow" in line:
                continue
            # @Query annotations are the real DAO danger; include all
            results.append((i, fun_name))
    return results


def parse_all_kotlin(files: list[FileContext]) -> list[KotlinFileMeta]:
    return [parse_kotlin_file(fc) for fc in files]
