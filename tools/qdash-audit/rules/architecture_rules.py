"""
rules/architecture_rules.py — Architecture layer violation checks.

ARCH-001: UI layer imports data layer directly (bypassing domain)
ARCH-002: Business logic / DAO calls inside @Composable functions
ARCH-003: ViewModel performing direct DB I/O (bypassing repository)
ARCH-004: @Composable function file exceeds complexity threshold
"""
from __future__ import annotations

import re
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from scanner.file_walker import FileContext, ProjectContext
    from scanner.kotlin_parser import KotlinFileMeta

from rules.base import Rule, make_issue
from models import Issue


# ── ARCH-001 ────────────────────────────────────────────────────────────────
class ArchDirectDataImportRule(Rule):
    """
    Presentation layer (.kt files under presentation/) must not import
    from the data layer directly (e.g. com.qdash.data.local.*, com.qdash.data.repository.*).
    All access should go through the domain layer interfaces.
    """
    id       = "ARCH-001"
    title    = "UI layer imports data layer directly"
    severity = "HIGH"
    category = "Architecture"

    # Allowed data imports (design-system, config constants etc.)
    _ALLOWED_DATA_PREFIXES = (
        "com.qdash.data.update",   # update checker accessed from AppShell
    )
    _BAD_PATTERN = re.compile(
        r"import\s+(com\.qdash\.data\.(?:local|repository|backup|categorization|worker|notification)\.\w+)"
    )

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        issues = []
        if not kotlin_meta or kotlin_meta.layer != "presentation":
            return issues

        content = file_ctx.content
        for m in self._BAD_PATTERN.finditer(content):
            import_str = m.group(1)
            if any(import_str.startswith(p) for p in self._ALLOWED_DATA_PREFIXES):
                continue
            line_no = content[:m.start()].count("\n") + 1
            snippet = file_ctx.lines[line_no - 1] if line_no <= len(file_ctx.lines) else ""
            issues.append(make_issue(
                rule=self,
                file_path=file_ctx.rel_path,
                line_start=line_no,
                line_end=line_no,
                description=(
                    f"File imports `{import_str}` directly from the data layer. "
                    "Presentation should only depend on domain interfaces "
                    "(com.qdash.domain.repository.* or use-cases), never on concrete data implementations."
                ),
                user_symptom="قد يسبب صعوبة في الاختبار وتراكم الأخطاء المعمارية بمرور الوقت",
                code_snippet=snippet,
            ))
        return issues


# ── ARCH-002 ────────────────────────────────────────────────────────────────
class ArchBusinessLogicInComposableRule(Rule):
    """
    @Composable functions must not contain:
    - Direct DAO calls
    - runBlocking calls
    - Heavy computation (detected via direct DB/file I/O patterns)
    """
    id       = "ARCH-002"
    title    = "Business logic inside @Composable function"
    severity = "HIGH"
    category = "Architecture"

    _DAO_CALL   = re.compile(r"\bdatabase\.\w+Dao\(\)\.\w+\(")
    _RUN_BLOCK  = re.compile(r"\brunBlocking\s*[({]")
    _REPO_CALL  = re.compile(r"\brepository\.\w+\s*\(")   # direct repo call (not via VM)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        issues = []
        if not kotlin_meta or not kotlin_meta.has_composable:
            return issues

        content = file_ctx.content
        bad_patterns = [
            (self._DAO_CALL,  "Direct DAO call inside @Composable"),
            (self._RUN_BLOCK, "runBlocking inside @Composable"),
        ]
        for pattern, label in bad_patterns:
            for m in pattern.finditer(content):
                ln = content[:m.start()].count("\n") + 1
                snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
                issues.append(make_issue(
                    rule=self,
                    file_path=file_ctx.rel_path,
                    line_start=ln,
                    line_end=ln,
                    description=(
                        f"{label} detected. Composables should only read UI state from "
                        "ViewModels (StateFlow/State). Move business logic to ViewModel or use-case."
                    ),
                    user_symptom="قد يسبب تجمد الواجهة عند تحميل أو تحديث الشاشة",
                    code_snippet=snippet,
                ))
        return issues


# ── ARCH-003 ────────────────────────────────────────────────────────────────
class ArchViewModelDirectDbRule(Rule):
    """
    ViewModel files must not call DAOs or perform file I/O directly.
    All persistence must go through a Repository.
    """
    id       = "ARCH-003"
    title    = "ViewModel calls DAO or file I/O directly"
    severity = "HIGH"
    category = "Architecture"

    _VIEWMODEL_SUFFIX = re.compile(r"ViewModel\.kt$", re.IGNORECASE)
    _DIRECT_DAO = re.compile(r"\bdatabase\.\w+Dao\(\)")
    _FILE_IO    = re.compile(r"\b(?:FileOutputStream|FileInputStream|BufferedWriter|PrintWriter|File\s*\()\b")

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        issues = []
        if not self._VIEWMODEL_SUFFIX.search(file_ctx.rel_path):
            return issues

        content = file_ctx.content
        for pattern, label in [
            (self._DIRECT_DAO, "Direct DAO access in ViewModel"),
            (self._FILE_IO,    "Direct file I/O in ViewModel"),
        ]:
            for m in pattern.finditer(content):
                ln = content[:m.start()].count("\n") + 1
                snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
                issues.append(make_issue(
                    rule=self,
                    file_path=file_ctx.rel_path,
                    line_start=ln,
                    line_end=ln,
                    description=(
                        f"{label} — `{m.group(0).strip()}`. "
                        "ViewModels must only call Repository methods, never DAO or File APIs directly. "
                        "This breaks Clean Architecture and makes the ViewModel untestable."
                    ),
                    user_symptom="قد يسبب تجمد واجهة المستخدم (ANR) أو فقدان البيانات عند إجراء عمليات قاعدة البيانات",
                    code_snippet=snippet,
                ))
        return issues


# ── ARCH-004 ────────────────────────────────────────────────────────────────
class ArchGiantComposableRule(Rule):
    """
    Composable screen files exceeding LINE_THRESHOLD lines indicate
    excessive complexity that hampers readability and recomposition performance.
    """
    id        = "ARCH-004"
    title     = "@Composable screen file too large"
    severity  = "MEDIUM"
    category  = "Architecture"
    LINE_THRESHOLD = 400   # configurable

    _SCREEN_PATTERN = re.compile(r"Screen\.kt$", re.IGNORECASE)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_composable:
            return []
        if not self._SCREEN_PATTERN.search(file_ctx.rel_path):
            return []
        if file_ctx.line_count <= self.LINE_THRESHOLD:
            return []
        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=file_ctx.line_count,
            description=(
                f"File has {file_ctx.line_count} lines (threshold: {self.LINE_THRESHOLD}). "
                "Large Composable screen files hurt readability, increase recomposition scope, "
                "and slow down Android Studio preview rendering. "
                "Consider splitting into smaller sub-composable functions or separate files."
            ),
            user_symptom="قد يؤدي إلى بطء في تحديث الشاشة وصعوبة في صيانة الكود",
        )]


ARCHITECTURE_RULES: list[Rule] = [
    ArchDirectDataImportRule(),
    ArchBusinessLogicInComposableRule(),
    ArchViewModelDirectDbRule(),
    ArchGiantComposableRule(),
]
