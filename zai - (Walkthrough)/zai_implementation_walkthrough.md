# Project Implementation Walkthrough — FinTrack DZ (Z AI Coordination Memory)

> **Authoritative coordination memory for Z AI, Antigravity, and any future coding assistant.**
> Repository state always wins over chat context (Operating Rule #10). Every claim below is verified against live files unless explicitly marked **Unverified** or **Needs confirmation**.

---

## 1. Objective

- **Current task:** Establish the persistent Z AI walkthrough/work-plan file with a verified baseline, then await a concrete implementation task from the user. No application code changes in this step.
- **Why it matters:** Provides durable, traceable, evidence-based memory so future sessions can resume work without confusion and without re-auditing the entire project.
- **User problem solved:** Eliminates drift between assistants (Z AI vs Codex vs Antigravity) and prevents fabricated/assumed progress.
- **Constraints to preserve:**
  - Inspect the real repository before documenting or claiming progress.
  - Never trust version numbers / architecture from memory — verify in code.
  - Mark uncertain items explicitly.
  - Do not perform feature implementation before this baseline is ready and approved.

---

## 2. Current Baseline

### 2.1 Repository identity
- **Root:** `C:\Users\Khalil\antigravity\FinTrack-DZ-2026-05-29-30f3a`
- **Git branch:** `main` (default branch). Git user: `Khalil`.
- **Gradle root project name:** `Kdach` (see `settings.gradle.kts`, `rootProject.name = "Kdach"`).
- **Modules:** single module `:app` only (`include(":app")`).

### 2.2 App / build configuration (verified from `app/build.gradle.kts`)
| Field | Value |
| --- | --- |
| `namespace` | `com.example` |
| `applicationId` | `com.aistudio.fintrackdz.agkdlm` |
| `compileSdk` | 36 |
| `targetSdk` | 36 |
| `minSdk` | 24 |
| **`versionCode`** | **12** |
| **`versionName`** | **"1.0.0.12"** |
| `BUILD_TIMESTAMP` | `System.currentTimeMillis()` at build |
| `UPDATE_IDENTITY` | `112L` |
| Compose / BuildConfig | enabled |
| Secrets plugin | reads `.env` + `.env.example` |
| Signing configs | `release` (env-driven: `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) + `debugConfig` (`debug.keystore`) |

> ⚠️ **Conflict noted (Operating Rule #10 applied — repository wins):** The pre-existing `Codex  - (Walkthrough)/implementation_walkthrough.md` claims `versionCode = 9 / "1.0.0.09"`. The live repository says **12 / "1.0.0.12"**. That older Codex doc is stale on this point. The numbers above are the source of truth.

### 2.3 Manifest (verified from `app/src/main/AndroidManifest.xml`)
- Permissions: `POST_NOTIFICATIONS`, `INTERNET`, `REQUEST_INSTALL_PACKAGES`, `WRITE_EXTERNAL_STORAGE` (maxSdk 28).
- `android:name=".FinTrackApp"`, `android:supportsRtl="true"`, theme `@style/Theme.Kdach`.
- Single launcher activity: `.MainActivity`.
- `FileProvider` with authority `${applicationId}.provider`, paths `@xml/file_paths`.

### 2.4 Architecture (verified — layered packages under `app/src/main/java/com/example/`)
- `core/` — `data/` (migrations, seeder), `di/` (`AppContainer`, `AppContainerImpl`), `preferences/`, `ui/` (shared components), `utils/`.
- `data/` — `backup/`, `categorization/`, `local/` (Room DB + DAOs/entities), `repository/` (12 impls), `update/`.
- `domain/` — `model/`, `repository/` (contracts), `usecase/`.
- `presentation/` — Compose screens + ViewModels + `navigation/` (`NavRoutes`, `NavGraph`) + `app/` shell. Feature packages include: `accounts, ai, analytics, app, backup, budgetgoals, categories, components, debt, export, home, navigation, notifications, onboarding, plans, salary, savings, search, settings, simulator, subscriptions, templates, transactions, transfer, update`.
- `ui/` — `designsystem/`, `theme/`.
- **DI pattern:** manual (no Hilt/Dagger). `AppContainerImpl` wires all repositories + use cases as `by lazy` singletons.

### 2.5 Room database (verified from `AppDatabase.kt` + `Migrations.kt` + `AppContainerImpl.kt`)
- DB name: `kdach_database`. **Schema version = 14**, `exportSchema = true`.
- **19 entities:** Transaction, Account, Category, IncomeSource, SavingGoal, Subscription, BudgetGoal, CategoryRule, UserCategoryMapping, SavingsContribution, Debt, DebtPayment, Transfer, Notification, FinancialPlan, DailyFinancialAggregate, TransactionTemplate, AiChatMessage, PostalProfile.
- **Migrations present (verified):** `MIGRATION_3_4` … `MIGRATION_13_14` (continuous 3→14), aggregated as `ALL_MIGRATIONS`.
- **`fallbackToDestructiveMigration()` is enabled** in `AppContainerImpl` → any future schema change not covered by a migration **will wipe user data**. High-risk area.

### 2.6 Navigation (verified from `NavRoutes.kt` + `NavGraph.kt`)
- `Screen` sealed class defines routes; `mainBottomNavScreens = [Home, Analytics, Accounts, Settings]`.
- `NavGraph.kt` registers (verified, non-exhaustive) composables for: Onboarding, Home, Transactions, Accounts, Analytics, Settings, Savings, Subscriptions, Debts, Transfer, Export, BudgetGoals, Salary, AddBudgetGoal, Notifications, AiChat, Search, Categories, FinancialPlans, Templates, CreateTemplate, Backup, IncomeHistory, Updates, DocumentSimulatorEntry, DocumentSimulator, PostalProfiles.
- `Screen.AddTransaction` route carries query args `type / transactionId / date / draft`.
- `CreateEditPostalProfile` and `BudgetGoalDetails` / `EditTemplate` carry path params.

### 2.7 External integrations (verified)
- **Update system:** `MainActivity` checks `update.json` (GitHub raw CDN) 3s after cold start; compares `versionName` + `versionCode`; writes a notification via `notificationRepository`. A global in-app bottom **update bar** with 3 phases (alert → background download progress → instant install) was added in commit `6e5282c`.
- **AI ("قداشّ"):** `AiRepositoryImpl` loads `GEMINI_API_KEY` (system property / env / BuildConfig) and calls Gemini `generateContent` via OkHttp.
- **Backup:** `BackupManager` exports/imports the Room DB + related files as ZIP.

### 2.8 In-app update release checklist (from `.agents/AGENTS.md`)
- Bumping a version requires, atomically: bump `versionCode`/`versionName`/`UPDATE_IDENTITY` → `.\gradlew assembleDebug` → replace `.build-outputs/New folder (2)/app-debug.apk` → update **every** field of `update.json` (incl. exact `apkSize` + lowercase `apkSha256`) → `git add` all → push. `update.json` is the single source of truth for update detection.

### 2.9 Current repository working-tree state (verified via `git status --short`)
- **Modified (tracked):**
  - `app/.../presentation/analytics/AnalyticsComponents.kt` (large: +724/-646 net across the two files)
  - `app/.../presentation/analytics/AnalyticsScreen.kt`
- **Untracked:**
  - `.agents/`
  - `Codex  - (Walkthrough)/`
  - `scratch/inspect_bytes.py`
- **Implication:** There is **active, uncommitted work on the Analytics screens**. This must not be disturbed or silently reverted.

### 2.10 Known in-progress fix flagged by the Codex walkthrough (Needs confirmation)
- The `Codex  - (Walkthrough)/change_handoff.md` lists an **active fix** for the *Add Transaction amount input*: display Western digits per settings and allow freer cursor movement / deletion / selection while preserving the custom numpad.
- **Verification against live code (AddTransactionComponents.kt / AddTransactionScreen.kt):**
  - `AmountDisplayCard` uses `BasicTextField` with `FormulaThousandsSeparatorTransformation`.
  - `FormulaThousandsSeparatorTransformation` (line ~1195) **already** uses a Western formatter: `DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))`. ✅ matches the intended fix.
  - However `AddTransactionScreen.kt:299` and `:329` still use a **locale-default** `DecimalFormat("#,###")` (no fixed locale) → potential Arabic/Indic-digit leak under Arabic locale.
  - Whether `readOnly = true` is still set on the amount field is **Needs confirmation** (grep for `readOnly = true` in the amount field returned no direct hit in the inspected region — must be confirmed by reading the full `AmountDisplayCard` before any claim).
- **Conclusion:** That Codex-tracked fix appears **partially landed**. It is **not** Z AI's task unless explicitly assigned; it is flagged here for traceability.

---

## 3. Scope of Work

### 3.1 In scope (this step)
- Create the `zai - (Walkthrough)/` folder and `zai_implementation_walkthrough.md`.
- Populate an evidence-based baseline, execution framework, validation strategy, risks, and handoff notes.
- Surface the conflict with the stale Codex doc and the uncommitted Analytics work.
- Await the user's concrete implementation task before any code change.

### 3.2 Out of scope (this step)
- Any Android feature implementation, UI change, schema change, financial-logic change, dependency upgrade, or git cleanup of untracked files.

### 3.3 Must not break
- Existing build config, Room schema/migrations, navigation routes, Arabic/RTL behavior, update/backup/AI/financial data flows, and the **uncommitted Analytics work** in the working tree.

---

## 4. Evidence-Based Audit

### 4.1 Files inspected (this session)
- `settings.gradle.kts`, `build.gradle.kts` (root), `app/build.gradle.kts`, `gradle/libs.versions.toml`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/FinTrackApp.kt`, `MainActivity.kt`
- `app/src/main/java/com/example/core/di/AppContainerImpl.kt`
- `app/src/main/java/com/example/data/local/AppDatabase.kt`
- `app/src/main/java/com/example/core/data/Migrations.kt` (grep: migration definitions)
- `app/src/main/java/com/example/presentation/navigation/NavRoutes.kt`, `NavGraph.kt` (grep: composable registrations)
- `app/src/main/java/com/example/domain/model/DomainModels.kt`
- `app/src/main/java/com/example/presentation/analytics/AnalyticsScreen.kt`
- `app/src/main/java/com/example/presentation/transactions/AddTransactionComponents.kt`, `AddTransactionScreen.kt` (grep: amount formatter / readOnly)
- `update.json`, `.agents/AGENTS.md`, `.env.example`, `metadata.json`
- `Codex  - (Walkthrough)/implementation_walkthrough.md`, `change_handoff.md`, `work_log.md`
- `git status --short`, `git diff --stat`

### 4.2 Confirmed findings
- Single-module Android app, Gradle Kotlin DSL, manual DI, Room v14, Compose Navigation, layered architecture. (Details in §2.)
- Stale version claim in the Codex walkthrough (9 / 1.0.0.09) contradicted by live repo (12 / 1.0.0.12).
- Uncommitted, in-flight Analytics work exists and must be preserved.

### 4.3 Unverified / Needs confirmation
- Full build (`assembleDebug`) and test pass — **not run** in this baseline step. Needs confirmation before any change that requires build validation.
- Runtime/device behavior — Needs confirmation.
- Whether `readOnly = true` is still present on the Add Transaction amount `BasicTextField` — Needs confirmation (read full `AmountDisplayCard`).
- Whether `fallbackToDestructiveMigration()` is intentional for production — Needs confirmation before any DB work.
- Release signing env vars (`KEYSTORE_PATH`, etc.) — Unverified; no release build attempted.
- Ownership/purpose of `scratch/inspect_bytes.py` — Needs confirmation; left untouched.

---

## 5. Execution Plan

> Phases A–B are the documentation-only work of this step. Phases C–E are gated on the user's concrete task and will be filled in when scope is confirmed.

| Phase | Goal | Files to Create/Modify | Implementation Approach | Risks | Validation Method | Status |
| --- | --- | --- | --- | --- | --- | --- |
| **A — Repository Audit** | Establish verified baseline from actual files | None (read-only) | Inspect Gradle, manifest, entry points, DI, DB, nav, repos, working tree | Missing feature-specific depth until a concrete task arrives | Compare every claim to inspected files | ✅ Completed |
| **B — Walkthrough Baseline** | Create persistent Z AI memory file | `zai - (Walkthrough)/zai_implementation_walkthrough.md` | Write required sections with verified facts + uncertainties marked | Over-documenting unverified behavior | File exists with all 11 required sections | ✅ Completed |
| **C — Implementation** | (Gated) Implement the user's concrete task | TBD by task | Read this doc first → audit task files → minimal scoped edits → update doc after each step | Financial/data/nav/RTL regressions; colliding with uncommitted Analytics work | Build + targeted tests + manual QA | ⏸ Not started (awaiting scope) |
| **D — Verification** | (Gated) Validate build/runtime/UI/data | TBD | Run `assembleDebug`, relevant tests, manual run | Untested edge cases | Document what was + wasn't tested | ⏸ Not started |
| **E — Handoff Readiness** | Keep this file current for Antigravity/Codex | This file + changed code | Update log + matrix + handoff after each meaningful step | Doc drift | Re-verify before signaling done | ⏸ Not started |

---

## 6. Detailed Walkthrough Log

| Step | Date/Time | Action Performed | Files Changed | Reason | Result | Follow-up |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 2026-06-20 | Read project context, metadata.json, settings/build gradle, manifest, entry points, DI, DB, nav routes, domain models, analytics screen, update.json, AGENTS.md, Codex walkthrough set. | None | Establish verified baseline before creating Z AI memory. | Baseline captured; found version conflict (repo=12 vs Codex doc=9) and uncommitted Analytics work. | Resolve conflict by trusting repo; preserve Analytics work. |
| 2 | 2026-06-20 | Verified migrations (3→14, `ALL_MIGRATIONS`), NavGraph composable registrations, git working-tree status, and Add Transaction amount formatter usage. | None | Evidence-based confirmation of DB migrations, nav surface, and in-flight changes. | Migrations + nav surface confirmed; Analytics files dirty; amount field uses Western formatter in the VisualTransformation but locale-default `DecimalFormat` remains in `AddTransactionScreen.kt:299/329`. | Confirm `readOnly` state of amount field before touching it. |
| 3 | 2026-06-20 | Created `zai - (Walkthrough)/` folder and `zai_implementation_walkthrough.md` with all 11 required sections. | `zai - (Walkthrough)/zai_implementation_walkthrough.md` | Required project memory before any implementation. | Baseline documentation ready. | Present path/structure/summary to user; await concrete task. |

---

## 7. File Change Matrix

| File Path | Status | Purpose | Linked Phase | Notes |
| --- | --- | --- | --- | --- |
| `zai - (Walkthrough)/zai_implementation_walkthrough.md` | Created | Main Z AI execution plan, audit, validation, risk, handoff record | B | Living document; update after each meaningful step. |
| `app/.../presentation/analytics/AnalyticsComponents.kt` | Pre-existing **modified (uncommitted)** | Analytics UI components | — | **Not touched by Z AI.** Active work by another session/author. |
| `app/.../presentation/analytics/AnalyticsScreen.kt` | Pre-existing **modified (uncommitted)** | Analytics screen | — | **Not touched by Z AI.** Active work. |
| `Codex  - (Walkthrough)/implementation_walkthrough.md` | Pre-existing (stale on version) | Codex's memory | — | Trust repo over this doc where they disagree. |
| `scratch/inspect_bytes.py` | Pre-existing untracked | Unknown | — | Not modified; ownership Needs confirmation. |

---

## 8. Validation Checklist

- **Build validation**
  - [ ] Run `.\gradlew.bat app:assembleDebug` when an implementation change requires build validation (not run in this baseline step).
  - [ ] Run targeted unit tests when logic changes (`app/src/test`).
- **Runtime validation**
  - [ ] Launch on emulator/device after UI/nav/runtime changes.
  - [ ] Confirm onboarding vs home start destination.
- **UI/UX validation**
  - [ ] RTL layout stays correct; Arabic strings render properly.
- **Data integrity validation**
  - [ ] Treat Room migrations, backup/import/export, and financial math as high-risk.
  - [ ] Confirm no unintended destructive migration / data loss.
- **Regression checks**
  - [ ] Transactions/accounts/categories stay consistent after financial-logic changes.
  - [ ] Update flow, AI key loading, backup flow unchanged unless explicitly targeted.
  - [ ] Uncommitted Analytics work still compiles/behaves after any nearby change.
- **Edge cases**
  - [x] Documentation file + section structure verified for this baseline step.
- **Remaining manual QA**
  - [ ] Needs confirmation after any future app behavior change.

---

## 9. Risks and Rollback Notes

- **Key risks**
  - Room v14 + `fallbackToDestructiveMigration()`: an uncovered schema bump wipes user data.
  - Financial-logic edits ripple across balances, budgets, debts, transfers, savings, exports.
  - Navigation edits can break deep routes (edit/detail flows with path params).
  - AI/update features depend on network + external config (`update.json`, `GEMINI_API_KEY`).
  - Backup/import/export touches real user data.
- **Fragile areas**
  - `AppContainerImpl` (central DI + DB setup), `NavRoutes`/`NavGraph` (routing), repository/use-case layer (domain behavior), `PreferencesManager` (startup/theme/language/dashboard).
  - **Uncommitted Analytics work** — editing nearby shared components could collide.
- **Rollback strategy**
  - Keep patches minimal and file-scoped; revert only files touched by the specific task.
  - For DB changes: add an explicit migration + document data-preservation; never rely on destructive fallback.
  - For UI changes: preserve route names + ViewModel contracts unless planned.
- **Safe fallback plan**
  - `git status --short` → identify the task's files → revert just those.
  - Restore this walkthrough to the prior log state if a step is abandoned.
  - Never delete/rewrite unrelated untracked files (`scratch/`, other walkthrough folders).

---

## 10. Pending Items

- **Await concrete implementation task** from the user before changing application code.
- Verify full `assembleDebug` + test status when implementation begins.
- Confirm whether `fallbackToDestructiveMigration()` is acceptable for production before any DB work.
- Confirm `readOnly` state of the Add Transaction amount field if that area is touched.
- Confirm ownership/purpose of `scratch/inspect_bytes.py` if cleanup is requested.
- Confirm release signing env vars only if a release build is needed.
- Decide coordination policy with the other in-flight Analytics work (commit first vs leave as-is).

---

## 11. Handoff Notes for Future AI

- **Finished**
  - Repository audit completed; Z AI walkthrough file created with all 11 required sections.
  - Confirmed baseline: Kotlin/Compose single-module app, manual DI, Room v14 (migrations 3→14), Compose Navigation, Arabic/RTL, versionCode 12 / "1.0.0.12" / UPDATE_IDENTITY 112.
- **In progress**
  - No Z AI application implementation in progress.
  - **Another session has uncommitted work** on `AnalyticsComponents.kt` + `AnalyticsScreen.kt` — do not disturb.
- **Files that matter most before continuing**
  - This file: `zai - (Walkthrough)/zai_implementation_walkthrough.md`
  - `app/src/main/java/com/example/core/di/AppContainerImpl.kt`
  - `app/src/main/java/com/example/data/local/AppDatabase.kt` + `core/data/Migrations.kt`
  - `app/src/main/java/com/example/presentation/navigation/NavRoutes.kt` + `NavGraph.kt`
  - `app/build.gradle.kts`, `update.json`, `.agents/AGENTS.md`
- **Safe assumptions**
  - Single-module Android app; layered architecture; manual DI via `AppContainer`.
  - Trust the repository over the older Codex walkthrough where they disagree (e.g., version numbers).
- **Check first before continuing**
  - Re-read this file.
  - Run `git status --short` to see current local changes (esp. the Analytics files).
  - Re-audit task-specific files before editing.
  - Update this file (log + matrix + handoff) after every meaningful step.
