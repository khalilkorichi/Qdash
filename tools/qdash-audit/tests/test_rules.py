"""
tests/test_rules.py — Unit tests for all audit rules using fixture .kt files.

Each test creates a minimal FileContext from a fixture file and verifies
the expected rule fires (or does not fire).
"""
import os
import sys
import unittest
from pathlib import Path

# ── Path setup ─────────────────────────────────────────────────────────────
TESTS_DIR  = Path(__file__).resolve().parent
AUDIT_ROOT = TESTS_DIR.parent
sys.path.insert(0, str(AUDIT_ROOT))

FIXTURES_DIR = TESTS_DIR / "fixtures"


def _make_fc(filename: str, rel_prefix: str = "app/src/main/java/com/qdash"):
    """Create a minimal FileContext from a fixture file."""
    from scanner.file_walker import FileContext
    path = FIXTURES_DIR / filename
    if not path.exists():
        raise FileNotFoundError(f"Fixture not found: {path}")
    lines = path.read_text(encoding="utf-8").splitlines()
    ext   = path.suffix.lower()
    return FileContext(
        path=str(path),
        rel_path=f"{rel_prefix}/{filename}",
        extension=ext,
        size_bytes=path.stat().st_size,
        sha256="test",
        is_changed=True,
        is_new=True,
        lines=lines,
        line_count=len(lines),
    )


def _make_project_ctx():
    """Create a minimal ProjectContext for testing."""
    from scanner.file_walker import ProjectContext
    ctx = ProjectContext(project_root=str(AUDIT_ROOT.parent.parent))
    ctx._db_health_missing_migrations = []
    ctx._db_schema_versions = [22]
    ctx._dup001_registry = {}
    ctx._dup002_registry = {}
    return ctx


# ── Architecture Rules ─────────────────────────────────────────────────────
class TestArchRules(unittest.TestCase):

    def test_arch003_viewmodel_direct_dao_fires(self):
        """ARCH-003 must fire on bad_viewmodel_direct_db.kt"""
        from rules.architecture_rules import ArchViewModelDirectDbRule
        from scanner.kotlin_parser import parse_kotlin_file
        fc  = _make_fc("bad_viewmodel_direct_db.kt",
                        "app/src/main/java/com/qdash/presentation/transactions")
        # Rule checks rel_path ends in ViewModel.kt
        fc.rel_path = "app/src/main/java/com/qdash/presentation/transactions/TransactionsViewModel.kt"
        meta = parse_kotlin_file(fc)
        ctx  = _make_project_ctx()
        rule = ArchViewModelDirectDbRule()
        issues = rule.check(fc, ctx, meta)
        self.assertGreater(len(issues), 0, "ARCH-003 should fire on direct DAO access in ViewModel")
        self.assertEqual(issues[0].rule_id, "ARCH-003")

    def test_arch003_does_not_fire_on_good_viewmodel(self):
        """ARCH-003 must NOT fire on good_viewmodel.kt"""
        from rules.architecture_rules import ArchViewModelDirectDbRule
        from scanner.kotlin_parser import parse_kotlin_file
        fc   = _make_fc("good_viewmodel.kt",
                        "app/src/main/java/com/qdash/presentation/transactions")
        meta = parse_kotlin_file(fc)
        ctx  = _make_project_ctx()
        rule = ArchViewModelDirectDbRule()
        issues = rule.check(fc, ctx, meta)
        self.assertEqual(len(issues), 0, "ARCH-003 should NOT fire on clean ViewModel")

    def test_arch004_large_composable(self):
        """ARCH-004 fires when a composable screen exceeds line threshold."""
        from rules.architecture_rules import ArchGiantComposableRule
        from scanner.file_walker import FileContext
        lines = ["@Composable\nfun FakeScreen() {}".splitlines()[0]] * 450
        fc = FileContext(
            path="/fake/FakeScreen.kt",
            rel_path="app/src/main/java/com/qdash/presentation/FakeScreen.kt",
            extension=".kt", size_bytes=1000, sha256="x",
            is_changed=True, is_new=True, lines=lines, line_count=450,
        )
        from scanner.kotlin_parser import parse_kotlin_file
        meta = parse_kotlin_file(fc)
        # Manually flag has_composable for this synthetic file
        meta.has_composable = True
        rule = ArchGiantComposableRule()
        issues = rule.check(fc, _make_project_ctx(), meta)
        self.assertGreater(len(issues), 0, "ARCH-004 should fire for 450-line composable screen")


# ── Database Rules ─────────────────────────────────────────────────────────
class TestDatabaseRules(unittest.TestCase):

    def test_db001_no_primary_key_fires(self):
        """DB-001 must fire on entity with no @PrimaryKey."""
        from rules.database_rules import DbNoPrimaryKeyRule
        from scanner.kotlin_parser import parse_kotlin_file
        fc   = _make_fc("bad_entity_no_pk.kt",
                        "app/src/main/java/com/qdash/data/local/entities")
        # Ensure rel_path ends in .kt in data/local/entities
        fc.rel_path = "app/src/main/java/com/qdash/data/local/entities/BadRecordEntity.kt"
        meta = parse_kotlin_file(fc)
        # Verify parser correctly detected entity without PK
        self.assertTrue(meta.has_entity, "Parser should detect @Entity annotation")
        self.assertFalse(meta.has_primary_key, "Parser should NOT detect @PrimaryKey in this fixture")
        ctx  = _make_project_ctx()
        rule = DbNoPrimaryKeyRule()
        issues = rule.check(fc, ctx, meta)
        self.assertGreater(len(issues), 0, "DB-001 should fire on entity without @PrimaryKey")
        self.assertEqual(issues[0].severity, "CRITICAL")

    def test_db002_destructive_migration_fires(self):
        """DB-002 must fire when fallbackToDestructiveMigration is present."""
        from rules.database_rules import DbDestructiveMigrationRule
        from scanner.file_walker import FileContext
        code = "Room.databaseBuilder(...).fallbackToDestructiveMigration().build()"
        fc = FileContext(
            path="/fake/AppContainerImpl.kt",
            rel_path="app/src/main/java/com/qdash/core/di/AppContainerImpl.kt",
            extension=".kt", size_bytes=100, sha256="x",
            is_changed=True, is_new=True,
            lines=code.splitlines(), line_count=1,
        )
        rule = DbDestructiveMigrationRule()
        issues = rule.check(fc, _make_project_ctx())
        self.assertGreater(len(issues), 0, "DB-002 should fire on fallbackToDestructiveMigration")
        self.assertEqual(issues[0].severity, "CRITICAL")

    def test_db002_no_false_positive_on_clean_code(self):
        """DB-002 must NOT fire on clean Room builder."""
        from rules.database_rules import DbDestructiveMigrationRule
        from scanner.file_walker import FileContext
        code = "Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS).build()"
        fc = FileContext(
            path="/fake/AppContainerImpl.kt",
            rel_path="app/src/main/java/com/qdash/core/di/AppContainerImpl.kt",
            extension=".kt", size_bytes=100, sha256="x",
            is_changed=True, is_new=True,
            lines=code.splitlines(), line_count=1,
        )
        rule = DbDestructiveMigrationRule()
        issues = rule.check(fc, _make_project_ctx())
        self.assertEqual(len(issues), 0)


# ── Threading Rules ────────────────────────────────────────────────────────
class TestThreadingRules(unittest.TestCase):

    def test_thr001_sync_dao_fires(self):
        """THR-001 must fire on non-suspend DAO functions."""
        from rules.threading_rules import ThrSyncDaoFunctionRule
        from scanner.kotlin_parser import parse_kotlin_file
        fc   = _make_fc("bad_dao_sync.kt",
                        "app/src/main/java/com/qdash/data/local/dao")
        meta = parse_kotlin_file(fc)
        ctx  = _make_project_ctx()
        rule = ThrSyncDaoFunctionRule()
        issues = rule.check(fc, ctx, meta)
        self.assertGreater(len(issues), 0, "THR-001 should fire on sync DAO functions")
        self.assertEqual(issues[0].rule_id, "THR-001")

    def test_thr002_runblocking_in_viewmodel_fires(self):
        """THR-002 must fire when runBlocking is used in ViewModel."""
        from rules.threading_rules import ThrRunBlockingInUiRule
        from scanner.file_walker import FileContext
        code = """
class TestViewModel : ViewModel() {
    fun doWork() {
        runBlocking { someRepo.load() }
    }
}"""
        fc = FileContext(
            path="/fake/TestViewModel.kt",
            rel_path="app/src/main/java/com/qdash/presentation/TestViewModel.kt",
            extension=".kt", size_bytes=len(code), sha256="x",
            is_changed=True, is_new=True,
            lines=code.splitlines(), line_count=len(code.splitlines()),
        )
        rule = ThrRunBlockingInUiRule()
        issues = rule.check(fc, _make_project_ctx())
        self.assertGreater(len(issues), 0, "THR-002 should fire on runBlocking in ViewModel")


# ── RTL Rules ──────────────────────────────────────────────────────────────
class TestRtlRules(unittest.TestCase):

    def test_rtl001_ltr_force_fires(self):
        """RTL-001 must fire on hardcoded LayoutDirection.Ltr."""
        from rules.rtl_rules import RtlHardcodedLtrRule
        from scanner.kotlin_parser import parse_kotlin_file
        fc   = _make_fc("bad_rtl.kt",
                        "app/src/main/java/com/qdash/presentation/transactions")
        meta = parse_kotlin_file(fc)
        ctx  = _make_project_ctx()
        rule = RtlHardcodedLtrRule()
        issues = rule.check(fc, ctx, meta)
        self.assertGreater(len(issues), 0, "RTL-001 should fire on LayoutDirection.Ltr")
        self.assertEqual(issues[0].severity, "HIGH")

    def test_rtl002_hardcoded_string_fires(self):
        """RTL-002 must fire on hardcoded strings in screen composable."""
        from rules.rtl_rules import RtlHardcodedStringInNewScreenRule
        from scanner.kotlin_parser import parse_kotlin_file
        fc   = _make_fc("bad_rtl.kt",
                        "app/src/main/java/com/qdash/presentation/transactions/BadTransactionScreen.kt")
        # rename rel_path to match Screen.kt pattern
        fc.rel_path = "app/src/main/java/com/qdash/presentation/transactions/BadTransactionScreen.kt"
        meta = parse_kotlin_file(fc)
        ctx  = _make_project_ctx()
        rule = RtlHardcodedStringInNewScreenRule()
        issues = rule.check(fc, ctx, meta)
        self.assertGreater(len(issues), 0, "RTL-002 should fire on hardcoded strings in Screen")


# ── Size Rules ─────────────────────────────────────────────────────────────
class TestSizeRules(unittest.TestCase):

    def test_size001_large_viewmodel_fires(self):
        """SIZE-001 must fire for ViewModel > 500 lines."""
        from rules.size_rules import SizeGiantViewModelRule
        from scanner.file_walker import FileContext
        lines = ["// line"] * 550
        fc = FileContext(
            path="/fake/BigViewModel.kt",
            rel_path="app/src/main/java/com/qdash/presentation/BigViewModel.kt",
            extension=".kt", size_bytes=5500, sha256="x",
            is_changed=True, is_new=True, lines=lines, line_count=550,
        )
        rule = SizeGiantViewModelRule()
        issues = rule.check(fc, _make_project_ctx())
        self.assertGreater(len(issues), 0, "SIZE-001 should fire for 550-line ViewModel")

    def test_size001_normal_viewmodel_ok(self):
        """SIZE-001 must NOT fire for ViewModel ≤ 500 lines."""
        from rules.size_rules import SizeGiantViewModelRule
        from scanner.file_walker import FileContext
        lines = ["// line"] * 200
        fc = FileContext(
            path="/fake/SmallViewModel.kt",
            rel_path="app/src/main/java/com/qdash/presentation/SmallViewModel.kt",
            extension=".kt", size_bytes=2000, sha256="x",
            is_changed=True, is_new=True, lines=lines, line_count=200,
        )
        rule = SizeGiantViewModelRule()
        issues = rule.check(fc, _make_project_ctx())
        self.assertEqual(len(issues), 0)


# ── Walker tests ───────────────────────────────────────────────────────────
class TestWalker(unittest.TestCase):

    def test_fixtures_are_discovered(self):
        """File walker must discover .kt fixture files."""
        from scanner.file_walker import walk_project
        import tempfile, shutil, json
        with tempfile.TemporaryDirectory() as tmp:
            audit_tmp = Path(tmp) / "audit_data"
            audit_tmp.mkdir()
            ctx = walk_project(str(FIXTURES_DIR), str(audit_tmp), force_full=True)
            kt_files = [f.rel_path for f in ctx.kotlin_files]
            self.assertTrue(any("bad_viewmodel" in p for p in kt_files),
                            "bad_viewmodel_direct_db.kt should be discovered")

    def test_incremental_scan_detects_unchanged(self):
        """Second walk of unchanged files should have is_changed=False."""
        from scanner.file_walker import walk_project
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            audit_tmp = Path(tmp) / "audit_data"
            audit_tmp.mkdir()
            # First walk — all new
            ctx1 = walk_project(str(FIXTURES_DIR), str(audit_tmp), force_full=False)
            # Second walk — nothing changed
            ctx2 = walk_project(str(FIXTURES_DIR), str(audit_tmp), force_full=False)
            changed = [f for f in ctx2.files if f.is_changed]
            self.assertEqual(len(changed), 0, "No files should be marked changed on second walk")


if __name__ == "__main__":
    unittest.main(verbosity=2)
