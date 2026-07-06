"""
rules/threading_rules.py — Threading and coroutine safety checks.

THR-001: Non-suspend DAO function (synchronous DB call)
THR-002: runBlocking in UI context (Composable/Activity/ViewModel)
THR-003: Dispatchers.Main used inside Repository or DAO
"""
from __future__ import annotations

import re
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from scanner.file_walker import FileContext, ProjectContext
    from scanner.kotlin_parser import KotlinFileMeta

from rules.base import Rule, make_issue
from models import Issue


# ── THR-001 ─────────────────────────────────────────────────────────────────
class ThrSyncDaoFunctionRule(Rule):
    """
    @Dao interface functions that are NOT suspend and do NOT return Flow<>
    are synchronous — they block the calling thread. If called from any
    thread (including IO), they bypass Room's coroutine dispatcher safety.
    """
    id       = "THR-001"
    title    = "Non-suspend DAO function (synchronous DB call)"
    severity = "CRITICAL"
    category = "Threading"

    # Match: fun xxx(...) — no 'suspend' prefix, no Flow return
    _FUN_RE  = re.compile(r"^(\s*)(?!.*suspend\s+fun)fun\s+(\w+)\s*\(", re.MULTILINE)
    _FLOW_RE = re.compile(r"\bFlow\s*<")

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not kotlin_meta or not kotlin_meta.has_dao:
            return []

        issues = []
        for ln_no, name in kotlin_meta.non_suspend_dao_funs:
            snippet = file_ctx.lines[ln_no - 1] if ln_no <= len(file_ctx.lines) else ""
            # Double-check: skip if same line has Flow
            if self._FLOW_RE.search(snippet):
                continue
            issues.append(make_issue(
                rule=self,
                file_path=file_ctx.rel_path,
                line_start=ln_no,
                line_end=ln_no,
                description=(
                    f"DAO function `{name}` is not declared `suspend` and does not return `Flow<>`. "
                    "This makes it a synchronous, blocking database call. "
                    "Room's `allowMainThreadQueries()` is not recommended in production. "
                    "Declare this function as `suspend fun` to ensure it runs on a coroutine dispatcher."
                ),
                user_symptom="قد يسبب تجمد الواجهة (ANR) عند أي استعلام من قاعدة البيانات على الخيط الرئيسي",
                code_snippet=snippet,
            ))
        return issues


# ── THR-002 ─────────────────────────────────────────────────────────────────
class ThrRunBlockingInUiRule(Rule):
    """
    runBlocking inside UI-context files (Composables, Activities, ViewModels)
    is extremely dangerous — it blocks the calling thread, which is often
    the Main thread in these contexts.
    """
    id       = "THR-002"
    title    = "runBlocking used in UI context"
    severity = "HIGH"
    category = "Threading"

    _UI_SUFFIXES = re.compile(
        r"(?:Screen|Activity|Fragment|ViewModel|Composable)\.kt$", re.IGNORECASE
    )
    _RUNBLOCKING = re.compile(r"\brunBlocking\s*[({]", re.MULTILINE)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not self._UI_SUFFIXES.search(file_ctx.rel_path):
            return []

        issues = []
        for m in self._RUNBLOCKING.finditer(file_ctx.content):
            ln = file_ctx.content[:m.start()].count("\n") + 1
            snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
            issues.append(make_issue(
                rule=self,
                file_path=file_ctx.rel_path,
                line_start=ln,
                line_end=ln,
                description=(
                    "`runBlocking` in a UI-context file blocks the current thread until the "
                    "coroutine completes. In an Activity or ViewModel this will almost certainly "
                    "block the Main thread, causing ANR after 5 seconds. "
                    "Replace with `viewModelScope.launch` or `lifecycleScope.launch`."
                ),
                user_symptom="قد يسبب تجمد كامل للتطبيق وظهور رسالة 'التطبيق لا يستجيب' (ANR)",
                code_snippet=snippet,
            ))
        return issues


# ── THR-003 ─────────────────────────────────────────────────────────────────
class ThrMainDispatcherInRepoRule(Rule):
    """
    Repository and DAO implementations must never use Dispatchers.Main.
    They should use Dispatchers.IO (or the injected coroutine dispatcher).
    """
    id       = "THR-003"
    title    = "Dispatchers.Main used inside Repository or DAO"
    severity = "HIGH"
    category = "Threading"

    _REPO_DAO_SUFFIX = re.compile(
        r"(?:RepositoryImpl|Dao|DaoImpl)\.kt$", re.IGNORECASE
    )
    _MAIN_DISP = re.compile(r"\bDispatchers\.Main\b", re.MULTILINE)

    def check(self, file_ctx, project_ctx, kotlin_meta=None):
        if not self._REPO_DAO_SUFFIX.search(file_ctx.rel_path):
            return []

        issues = []
        for m in self._MAIN_DISP.finditer(file_ctx.content):
            ln = file_ctx.content[:m.start()].count("\n") + 1
            snippet = file_ctx.lines[ln - 1] if ln <= len(file_ctx.lines) else ""
            issues.append(make_issue(
                rule=self,
                file_path=file_ctx.rel_path,
                line_start=ln,
                line_end=ln,
                description=(
                    "`Dispatchers.Main` used in a Repository or DAO. "
                    "Data-layer code must run on `Dispatchers.IO` to avoid blocking the UI thread. "
                    "Move UI-thread dispatching to the ViewModel or presentation layer."
                ),
                user_symptom="قد يسبب تجمد الواجهة عند إجراء عمليات قاعدة البيانات أو الشبكة",
                code_snippet=snippet,
            ))
        return issues


THREADING_RULES: list[Rule] = [
    ThrSyncDaoFunctionRule(),
    ThrRunBlockingInUiRule(),
    ThrMainDispatcherInRepoRule(),
]
