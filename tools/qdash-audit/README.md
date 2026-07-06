# Qdash Code & Database Health Audit Tool

A standalone local analysis tool that scans the Qdash repository for architecture violations, Room schema issues, threading bugs, RTL regressions, and more — with a beautiful Arabic-first HTML dashboard.

---

## Prerequisites

- **Python 3.9+** — check with `python --version`
- No additional packages required (uses Python stdlib only)

---

## Quick Start

### Run the audit
```powershell
# From the project root:
python tools/qdash-audit/audit.py

# Force a full re-scan of every file (ignore hash cache):
python tools/qdash-audit/audit.py --full

# Or double-click the launcher (Windows):
tools\qdash-audit\audit.bat
```

### View the dashboard
Open `tools/qdash-audit/dashboard/index.html` directly in Chrome, Edge, or Firefox.

> **Note:** Because the dashboard reads local JSON files via `fetch()`, you may need to either:
> - Use the **Live Server** VS Code extension (right-click → "Open with Live Server"), or  
> - Serve locally: `python -m http.server 8080 --directory tools/qdash-audit` then open `http://localhost:8080/dashboard/index.html`
>
> Chrome may block local file fetch — Edge usually works fine with `file://` URLs.

---

## What Gets Scanned

The tool recursively walks the entire Qdash repository, skipping:
- `.git/`, `build/`, `.gradle/`, `releases/`, `scratch/`, `tools/`

Scanned file types: `.kt`, `.kts`, `.xml`, `.json` (Room schemas)

---

## Rules (22 total)

### Architecture (4 rules)
| ID | Severity | Description |
|----|----------|-------------|
| ARCH-001 | HIGH | UI layer imports data layer directly (bypassing domain) |
| ARCH-002 | HIGH | Business logic / DAO calls inside `@Composable` functions |
| ARCH-003 | HIGH | ViewModel calls DAO or file I/O directly (bypassing repository) |
| ARCH-004 | MEDIUM | `@Composable` screen file exceeds 400 lines |

### Database (6 rules)
| ID | Severity | Description |
|----|----------|-------------|
| DB-001 | CRITICAL | Room entity missing `@PrimaryKey` |
| DB-002 | CRITICAL | `fallbackToDestructiveMigration()` detected |
| DB-003 | HIGH | Schema version bump with no corresponding migration |
| DB-004 | MEDIUM | Large entity (>5 fields) with no `@Index` |
| DB-005 | MEDIUM | Possible missing `@ForeignKey` on related entity |
| DB-006 | INFO | Room schema export file missing for current version |

### Threading (3 rules)
| ID | Severity | Description |
|----|----------|-------------|
| THR-001 | CRITICAL | Non-`suspend` DAO function (synchronous DB call) |
| THR-002 | HIGH | `runBlocking` used in UI context (ViewModel/Activity/Composable) |
| THR-003 | HIGH | `Dispatchers.Main` used inside Repository or DAO |

### RTL / Arabic (3 rules)
| ID | Severity | Description |
|----|----------|-------------|
| RTL-001 | HIGH | Hardcoded `LayoutDirection.Ltr` in `@Composable` |
| RTL-002 | MEDIUM | Hardcoded string literals in screen Composable instead of `stringResource()` |
| RTL-003 | LOW | `Text()` with raw string literal (localization reminder) |

### Duplication (2 rules)
| ID | Severity | Description |
|----|----------|-------------|
| DUP-001 | MEDIUM | Same domain type held as `StateFlow` in multiple ViewModels |
| DUP-002 | LOW | Identical field declared in 3+ domain model files |

### Size (2 rules)
| ID | Severity | Description |
|----|----------|-------------|
| SIZE-001 | MEDIUM | ViewModel file exceeds 500 lines |
| SIZE-002 | LOW | Composable screen file exceeds 400 lines |

### Orphans (2 rules)
| ID | Severity | Description |
|----|----------|-------------|
| ORP-001 | LOW | Screen Composable not referenced in `NavGraph.kt` or `NavRoutes.kt` |
| ORP-002 | INFO | Repository implementation without matching domain interface |

---

## Severity Definitions

| Level | Meaning |
|-------|---------|
| **CRITICAL** | Crash or data-loss risk — fix immediately |
| **HIGH** | ANR/lag/major architecture violation with real user impact |
| **MEDIUM** | Best-practice violation, no immediate crash |
| **LOW** | Style/minor improvement |
| **INFO** | Suggestion only — no action required |

---

## Output Files

All output is written to `tools/qdash-audit/`:
```
reports/
  index.json                  ← rolling scan history (append-only)
  scan_20260706_102300_abc.json ← full scan report (never overwritten)
audit_data/
  file_index.json             ← SHA-256 hash index for change detection
```

---

## Running Tests

```powershell
cd tools/qdash-audit
python -m unittest tests/test_rules.py -v
```

---

## Adding a New Rule

1. Create or open the relevant rule file in `rules/` (e.g. `architecture_rules.py`)
2. Subclass `Rule` from `rules/base.py`:
   ```python
   class MyNewRule(Rule):
       id       = "ARCH-005"
       title    = "My rule title"
       severity = "MEDIUM"
       category = "Architecture"

       def check(self, file_ctx, project_ctx, kotlin_meta=None):
           issues = []
           # ... your detection logic ...
           # Use make_issue() to create Issue objects
           return issues
   ```
3. Add to the list at the bottom of the rule file: `MY_RULES = [..., MyNewRule()]`
4. Import and extend `ALL_RULES` in `rules/__init__.py`
5. Add a fixture file in `tests/fixtures/` and a test case in `tests/test_rules.py`

### Arabic symptom phrase guide

Each rule's `user_symptom` should be a concise Arabic sentence starting with "قد يسبب":
- Crash/data loss: `"خطر فقدان البيانات — قد يتعطل التطبيق"`
- ANR/freeze: `"قد يسبب تجمد الواجهة (ANR) عند ..."`
- Architecture: `"قد يسبب صعوبة في الاختبار وتراكم الأخطاء"`
- Style only: `"لا يوجد تأثير مباشر — تنبيه للصيانة المستقبلية"`

---

## CLI Reference

```
python audit.py [options]

Options:
  --full            Force re-scan all files (ignore hash cache)
  --report-only     Regenerate index.json without scanning
  --project-root    Path to Qdash repo root (auto-detected by default)
  --audit-root      Path to tools/qdash-audit/ (auto-detected)
  --json            Print full report JSON to stdout
  --history         Print scan history and exit
  -h, --help        Show this help message
```

**Exit codes:**
- `0` — No issues found
- `1` — Issues found (none CRITICAL)
- `2` — At least one CRITICAL issue found

---

## Architecture

```
tools/qdash-audit/
├── audit.py          ← CLI entry point
├── audit.bat         ← Windows launcher
├── models.py         ← Issue, ScanReport, DatabaseHealth dataclasses
├── use_cases.py      ← RunFullScanUseCase + read-only use cases
├── scanner/
│   ├── file_walker.py        ← recursive walk + SHA-256 change detection
│   ├── kotlin_parser.py      ← regex-based Kotlin metadata extractor
│   └── room_schema_parser.py ← Room JSON schema parser
├── rules/
│   ├── base.py               ← Rule ABC + make_issue() factory
│   ├── architecture_rules.py
│   ├── database_rules.py
│   ├── threading_rules.py
│   ├── rtl_rules.py
│   ├── duplication_rules.py
│   ├── size_rules.py
│   └── orphan_rules.py
├── reports/          ← append-only JSON scan reports
├── audit_data/       ← SHA-256 hash index (tool's own state)
├── dashboard/
│   ├── index.html    ← single-file Arabic dashboard
│   ├── styles.css    ← dark-mode glassmorphism CSS
│   └── app.js        ← pure vanilla JS state + rendering
└── tests/
    ├── fixtures/     ← sample .kt files (positive + negative cases)
    └── test_rules.py ← unit tests for all rules
```
