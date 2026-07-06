"""
rules/orphan_rules.py — Unused / orphaned file detection.

ORP-001: Kotlin file under presentation/ not referenced in NavGraph.kt or NavRoutes.kt
ORP-002: Repository implementation in data/ without matching interface in domain/
"""
from __future__ import annotations

import re
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from scanner.file_walker import FileContext, ProjectContext
    from scanner.kotlin_parser import KotlinFileMeta

from rules.base import Rule, make_issue
from models import Issue


# ── ORP-001 ─────────────────────────────────────────────────────────────────
class OrphanedScreenRule(Rule):
    """
    Any *Screen.kt under presentation/ that is not mentioned anywhere in
    NavGraph.kt or NavRoutes.kt may be orphaned (never reachable).
    """
    id       = "ORP-001"
    title    = "Composable screen not referenced in navigation graph"
    severity = "LOW"
    category = "Orphan"

    _SCREEN_SUFFIX   = re.compile(r"(?P<name>\w+Screen)\.kt$")
    _PRESENTATION_RE = re.compile(r"/presentation/", re.IGNORECASE)
    _NAV_FILES       = {"NavGraph.kt", "NavRoutes.kt"}

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        rel = file_ctx.rel_path.replace("\\", "/")
        if not self._PRESENTATION_RE.search(rel):
            return []

        m = self._SCREEN_SUFFIX.search(file_ctx.rel_path)
        if not m:
            return []

        screen_name = m.group("name")

        # Search NavGraph.kt and NavRoutes.kt for this screen name
        nav_content = ""
        for nav_file_name in self._NAV_FILES:
            for f in project_ctx.kotlin_files:
                if f.rel_path.endswith(nav_file_name):
                    nav_content += f.content

        if not nav_content:
            return []   # Can't determine — NavGraph not found

        if screen_name in nav_content:
            return []   # Screen is referenced

        # Also skip non-composable screen files (e.g. plain data classes named XxxScreen)
        if not kotlin_meta or not kotlin_meta.has_composable:
            return []

        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=1,
            description=(
                f"`{screen_name}` is not referenced in NavGraph.kt or NavRoutes.kt. "
                "This screen may be orphaned — never reachable from navigation — "
                "resulting in dead code that won't be tested or maintained. "
                "Either add a nav route or delete the file."
            ),
            user_symptom="المستخدم لا يمكنه الوصول إلى هذه الشاشة — كود ميت لن يُختبر أبداً",
            affected_files=["app/src/main/java/com/qdash/presentation/navigation/NavGraph.kt"],
        )]


# ── ORP-002 ─────────────────────────────────────────────────────────────────
class OrphanedRepositoryImplRule(Rule):
    """
    Repository implementation classes in data/repository/ should each have
    a matching interface in domain/repository/.
    """
    id       = "ORP-002"
    title    = "Repository implementation without domain interface"
    severity = "INFO"
    category = "Orphan"

    _IMPL_SUFFIX     = re.compile(r"(?P<name>\w+)RepositoryImpl\.kt$")
    _DATA_REPO_RE    = re.compile(r"/data/repository/", re.IGNORECASE)
    _DOMAIN_REPO_RE  = re.compile(r"/domain/repository/", re.IGNORECASE)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        rel = file_ctx.rel_path.replace("\\", "/")
        if not self._DATA_REPO_RE.search(rel):
            return []

        m = self._IMPL_SUFFIX.search(file_ctx.rel_path)
        if not m:
            return []

        base_name = m.group("name")
        expected_iface = f"{base_name}Repository"

        # Check if interface exists in domain/repository/
        for f in project_ctx.kotlin_files:
            if self._DOMAIN_REPO_RE.search(f.rel_path.replace("\\", "/")):
                if expected_iface in f.content or f.rel_path.endswith(f"{expected_iface}.kt"):
                    return []

        return [make_issue(
            rule=self,
            file_path=file_ctx.rel_path,
            line_start=1,
            line_end=1,
            description=(
                f"`{base_name}RepositoryImpl` has no matching `{expected_iface}` interface "
                "in `domain/repository/`. "
                "Clean Architecture requires the domain layer to define the contract; "
                "the data layer provides the implementation. "
                "Without the interface, the presentation/domain layers depend on the concrete "
                "implementation, tying them to the data layer."
            ),
            user_symptom="لا يوجد تأثير مباشر على المستخدم — يُضعف البنية المعمارية ويُعيق الاختبارات",
        )]


ORPHAN_RULES: list[Rule] = [
    OrphanedScreenRule(),
    OrphanedRepositoryImplRule(),
]
