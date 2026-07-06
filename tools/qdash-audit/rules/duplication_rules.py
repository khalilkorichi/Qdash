"""
rules/duplication_rules.py — Duplicate source-of-truth detection.

DUP-001: Same domain entity type held as StateFlow in multiple ViewModels
DUP-002: Identical field name+type declared in >2 domain model files
"""
from __future__ import annotations

import re
from collections import defaultdict
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from scanner.file_walker import FileContext, ProjectContext
    from scanner.kotlin_parser import KotlinFileMeta

from rules.base import Rule, make_issue
from models import Issue


# ── DUP-001 ─────────────────────────────────────────────────────────────────
class DupMultipleViewModelStateRule(Rule):
    """
    If the same entity/domain type (e.g. List<TransactionEntity>) appears
    as a MutableStateFlow in more than one ViewModel, there may be two
    independent sources of truth that can drift out of sync.

    This rule works at the project level: it accumulates metadata across all
    ViewModel files and only fires on the SECOND+ ViewModel that holds the
    same state type.
    """
    id       = "DUP-001"
    title    = "Duplicate StateFlow type across multiple ViewModels"
    severity = "MEDIUM"
    category = "Duplication"

    _VM_SUFFIX   = re.compile(r"ViewModel\.kt$", re.IGNORECASE)
    _STATE_FLOW  = re.compile(r"MutableStateFlow\s*<([^>]+)>", re.MULTILINE)
    _LIVE_DATA   = re.compile(r"MutableLiveData\s*<([^>]+)>", re.MULTILINE)

    # Clean up generic noise: List<Foo> → Foo
    _INNER_TYPE  = re.compile(r"(?:List|Flow|State|Result)\s*<\s*(\w+)\s*>")

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        # DUP-001 is a project-level rule.
        # We emit issues from a shared registry attached to project_ctx.
        if not self._VM_SUFFIX.search(file_ctx.rel_path):
            return []

        registry: dict[str, list[str]] = getattr(
            project_ctx, "_dup001_registry", {}
        )

        content = file_ctx.content
        issues = []

        for pattern in (self._STATE_FLOW, self._LIVE_DATA):
            for m in pattern.finditer(content):
                raw_type = m.group(1).strip()
                # Extract the innermost meaningful type
                inner_m = self._INNER_TYPE.search(raw_type)
                key_type = inner_m.group(1) if inner_m else raw_type
                if not key_type or len(key_type) < 3:
                    continue

                if key_type not in registry:
                    registry[key_type] = [file_ctx.rel_path]
                elif file_ctx.rel_path not in registry[key_type]:
                    registry[key_type].append(file_ctx.rel_path)
                    ln = file_ctx.content[:m.start()].count("\n") + 1
                    snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
                    issues.append(make_issue(
                        rule=self,
                        file_path=file_ctx.rel_path,
                        line_start=ln,
                        line_end=ln,
                        description=(
                            f"`{key_type}` is held as a StateFlow/LiveData in multiple ViewModels: "
                            f"{', '.join(registry[key_type])}. "
                            "Multiple owners of the same state can cause inconsistent UI across screens. "
                            "Consider using a shared ViewModel, a singleton repository stream, or "
                            "a shared StateFlow in a shared ViewModel."
                        ),
                        user_symptom="قد يسبب تناقضاً في البيانات بين الشاشات — تغيير في شاشة لا ينعكس على شاشة أخرى",
                        affected_files=list(registry[key_type]),
                        code_snippet=snippet,
                    ))

        project_ctx._dup001_registry = registry
        return issues


# ── DUP-002 ─────────────────────────────────────────────────────────────────
class DupIdenticalFieldInModelsRule(Rule):
    """
    If the exact same `val fieldName: Type` appears in more than 2
    domain model files, it may indicate a copy-paste duplication that
    should instead be a shared base class or value object.
    """
    id       = "DUP-002"
    title    = "Identical field declared in multiple domain model files"
    severity = "LOW"
    category = "Duplication"

    _DOMAIN_SUFFIX = re.compile(r"/domain/model/", re.IGNORECASE)
    _FIELD_RE      = re.compile(
        r"(?:val|var)\s+(\w+)\s*:\s*([\w<>, ?]+)", re.MULTILINE
    )
    _THRESHOLD     = 3   # flag if seen in >= 3 model files

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not self._DOMAIN_SUFFIX.search(file_ctx.rel_path.replace("\\", "/")):
            return []

        registry: dict[str, list[str]] = getattr(
            project_ctx, "_dup002_registry", {}
        )

        issues = []
        for m in self._FIELD_RE.finditer(file_ctx.content):
            key = f"{m.group(1)}:{m.group(2).strip()}"
            if key not in registry:
                registry[key] = []
            if file_ctx.rel_path not in registry[key]:
                registry[key].append(file_ctx.rel_path)

            if len(registry[key]) == self._THRESHOLD:
                ln = file_ctx.content[:m.start()].count("\n") + 1
                snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
                issues.append(make_issue(
                    rule=self,
                    file_path=file_ctx.rel_path,
                    line_start=ln,
                    line_end=ln,
                    description=(
                        f"Field `{m.group(1)}: {m.group(2).strip()}` is declared identically in "
                        f"{self._THRESHOLD}+ domain model files: {', '.join(registry[key])}. "
                        "Consider extracting this into a shared base class, interface, or value object."
                    ),
                    user_symptom="لا يوجد تأثير مباشر على المستخدم — تحسين جودة الكود للصيانة المستقبلية",
                    affected_files=list(registry[key]),
                    code_snippet=snippet,
                ))

        project_ctx._dup002_registry = registry
        return issues


DUPLICATION_RULES: list[Rule] = [
    DupMultipleViewModelStateRule(),
    DupIdenticalFieldInModelsRule(),
]
