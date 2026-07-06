"""
rules/size_rules.py — Giant file/class detection.

SIZE-001: ViewModel file exceeds LINE_THRESHOLD lines
SIZE-002: Composable screen file exceeds LINE_THRESHOLD lines
"""
from __future__ import annotations

import re
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from scanner.file_walker import FileContext, ProjectContext
    from scanner.kotlin_parser import KotlinFileMeta

from rules.base import Rule, make_issue
from models import Issue


# ── SIZE-001 ─────────────────────────────────────────────────────────────────
class SizeGiantViewModelRule(Rule):
    """ViewModel files exceeding the line threshold signal god-class ViewModels."""
    id        = "SIZE-001"
    title     = "ViewModel file too large"
    severity  = "MEDIUM"
    category  = "Size"
    THRESHOLD = 500

    _VM_SUFFIX = re.compile(r"ViewModel\.kt$", re.IGNORECASE)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not self._VM_SUFFIX.search(file_ctx.rel_path):
            return []
        if file_ctx.line_count <= self.THRESHOLD:
            return []
        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=file_ctx.line_count,
            description=(
                f"ViewModel has {file_ctx.line_count} lines (threshold: {self.THRESHOLD}). "
                "Excessively large ViewModels typically indicate multiple responsibilities "
                "(god class anti-pattern), making the code hard to test, maintain, and extend. "
                "Consider splitting into feature-scoped ViewModels or extracting use-case logic "
                "into dedicated UseCase classes."
            ),
            user_symptom="لا يوجد تأثير مباشر على المستخدم — لكنه يزيد من خطر الأخطاء عند تعديل الكود في المستقبل",
        )]


# ── SIZE-002 ─────────────────────────────────────────────────────────────────
class SizeGiantScreenRule(Rule):
    """Composable Screen files exceeding the line threshold."""
    id        = "SIZE-002"
    title     = "Composable screen file too large"
    severity  = "LOW"
    category  = "Size"
    THRESHOLD = 400

    _SCREEN_SUFFIX = re.compile(r"Screen\.kt$", re.IGNORECASE)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_composable:
            return []
        if not self._SCREEN_SUFFIX.search(file_ctx.rel_path):
            return []
        if file_ctx.line_count <= self.THRESHOLD:
            return []
        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=file_ctx.line_count,
            description=(
                f"Composable screen file has {file_ctx.line_count} lines (threshold: {self.THRESHOLD}). "
                "Large screen files increase recomposition scope, slow down Android Studio previews, "
                "and make it harder to find UI bugs. "
                "Extract sub-components into separate composable functions or sub-files."
            ),
            user_symptom="قد يؤدي إلى بطء في تحديث الشاشة عند إعادة التركيب (recomposition)",
        )]


SIZE_RULES: list[Rule] = [
    SizeGiantViewModelRule(),
    SizeGiantScreenRule(),
]
