"""
rules/rtl_rules.py — RTL/Arabic UI regression checks.

RTL-001: Hardcoded LayoutDirection.Ltr in @Composable
RTL-002: Hardcoded string literals in @Composable instead of stringResource
RTL-003: Text() with non-empty string literal (detailed per-call report)
"""
from __future__ import annotations

import re
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from scanner.file_walker import FileContext, ProjectContext
    from scanner.kotlin_parser import KotlinFileMeta

from rules.base import Rule, make_issue
from models import Issue

# Characters that hint the string is NOT a UI label (path, format, tag etc.)
_TECHNICAL_HINTS = re.compile(r'[/\\._@%\d]{3,}|^\s*$|^[A-Z_]{4,}$')


# ── RTL-001 ─────────────────────────────────────────────────────────────────
class RtlHardcodedLtrRule(Rule):
    """Hardcoded LayoutDirection.Ltr breaks RTL/Arabic layout."""
    id       = "RTL-001"
    title    = "Hardcoded LayoutDirection.Ltr in Composable"
    severity = "HIGH"
    category = "RTL"

    _LTR_RE = re.compile(r"layoutDirection\s*=\s*LayoutDirection\.Ltr", re.MULTILINE)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_composable:
            return []

        issues = []
        for m in self._LTR_RE.finditer(file_ctx.content):
            ln = file_ctx.content[:m.start()].count("\n") + 1
            snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
            issues.append(make_issue(
                rule=self,
                file_path=file_ctx.rel_path,
                line_start=ln,
                line_end=ln,
                description=(
                    "`LayoutDirection.Ltr` is hardcoded in a Composable. "
                    "This overrides the user's locale-based layout direction, "
                    "breaking RTL layout for Arabic-speaking users. "
                    "Remove the hardcoded direction and rely on `LocalLayoutDirection.current` "
                    "or let the system handle it based on the device locale."
                ),
                user_symptom="يعطل تخطيط الشاشة للمستخدمين العرب — ستظهر العناصر بترتيب معكوس أو خاطئ",
                code_snippet=snippet,
            ))
        return issues


# ── RTL-002 ─────────────────────────────────────────────────────────────────
class RtlHardcodedStringInNewScreenRule(Rule):
    """
    Composable screen files that contain raw string literals in Text()
    calls instead of stringResource(R.string.xxx).
    This rule targets screen files specifically (not component files).
    """
    id       = "RTL-002"
    title    = "Hardcoded UI string in Composable screen"
    severity = "MEDIUM"
    category = "RTL"

    _SCREEN_RE   = re.compile(r"Screen\.kt$", re.IGNORECASE)
    _TEXT_STR_RE = re.compile(r'Text\s*\(\s*(?:text\s*=\s*)?"([^"\\]{3,})"', re.MULTILINE)
    _STRING_RES  = re.compile(r"stringResource\s*\(", re.MULTILINE)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_composable:
            return []
        if not self._SCREEN_RE.search(file_ctx.rel_path):
            return []

        issues = []
        for m in self._TEXT_STR_RE.finditer(file_ctx.content):
            raw_str = m.group(1)
            if _TECHNICAL_HINTS.search(raw_str):
                continue   # skip technical strings (paths, tags)
            ln = file_ctx.content[:m.start()].count("\n") + 1
            snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
            issues.append(make_issue(
                rule=self,
                file_path=file_ctx.rel_path,
                line_start=ln,
                line_end=ln,
                description=(
                    f'Hardcoded string `"{raw_str}"` in Text(). '
                    "Use `stringResource(R.string.xxx)` instead so that Arabic translations "
                    "are picked up automatically, supporting the app's Arabic-first design."
                ),
                user_symptom="قد لا تظهر الترجمات العربية بشكل صحيح — النص سيبقى بالغة الأصلية بغض النظر عن لغة المستخدم",
                code_snippet=snippet,
            ))
        return issues


# ── RTL-003 ─────────────────────────────────────────────────────────────────
class RtlRawTextLiteralRule(Rule):
    """
    Any Text("literal") call in any Composable file is a potential
    localization miss. Severity LOW — acts as a reminder/INFO.
    """
    id       = "RTL-003"
    title    = "Text() with raw string literal (localization miss)"
    severity = "LOW"
    category = "RTL"

    _TEXT_STR_RE = re.compile(r'Text\s*\(\s*(?:text\s*=\s*)?"([^"\\]{2,})"', re.MULTILINE)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_composable:
            return []

        issues = []
        seen_lines: set[int] = set()
        for m in self._TEXT_STR_RE.finditer(file_ctx.content):
            raw_str = m.group(1)
            if _TECHNICAL_HINTS.search(raw_str):
                continue
            ln = file_ctx.content[:m.start()].count("\n") + 1
            if ln in seen_lines:
                continue
            seen_lines.add(ln)
            snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
            issues.append(make_issue(
                rule=self,
                file_path=file_ctx.rel_path,
                line_start=ln,
                line_end=ln,
                description=(
                    f'Text("{raw_str}") — raw string literal. '
                    "Consider moving to strings.xml and using stringResource() for full i18n/RTL support."
                ),
                user_symptom="النص لن يظهر مترجماً في إصدارات اللغة العربية — مجرد تنبيه للمراجعة",
                code_snippet=snippet,
            ))
        return issues


RTL_RULES: list[Rule] = [
    RtlHardcodedLtrRule(),
    # RtlHardcodedStringInNewScreenRule(),  # RTL-002: Ignored by design (Arabic-only app)
    # RtlRawTextLiteralRule(),             # RTL-003: Ignored by design (Arabic-only app)
]
