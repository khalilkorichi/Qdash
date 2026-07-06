"""
rules/base.py — Base Rule class and Issue factory helpers.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from scanner.file_walker import FileContext, ProjectContext
    from scanner.kotlin_parser import KotlinFileMeta

from models import Issue, SEVERITY_CRITICAL, SEVERITY_HIGH, SEVERITY_MEDIUM, SEVERITY_LOW, SEVERITY_INFO


class Rule(ABC):
    """
    Abstract base for all audit rules.

    Subclasses implement `check()` and return a (possibly empty) list of Issues.
    Rules are stateless and deterministic — identical input always produces
    identical output.
    """
    id:       str
    title:    str
    severity: str
    category: str

    @abstractmethod
    def check(
        self,
        file_ctx: "FileContext",
        project_ctx: "ProjectContext",
        kotlin_meta: "KotlinFileMeta | None" = None,
    ) -> list[Issue]:
        ...


def make_issue(
    rule: Rule,
    file_path: str,
    line_start: int,
    line_end: int,
    description: str,
    user_symptom: str,
    affected_files: list[str] | None = None,
    code_snippet: str = "",
) -> Issue:
    """Convenience factory — creates an Issue from a Rule and context."""
    return Issue(
        rule_id=rule.id,
        severity=rule.severity,
        category=rule.category,
        file_path=file_path,
        line_start=line_start,
        line_end=line_end,
        title=rule.title,
        description=description,
        user_symptom=user_symptom,
        affected_files=affected_files or [],
        code_snippet=code_snippet,
    )
