# Implementation Walkthrough: Room Database Security Hardening

To protect sensitive user data from catastrophic loss, we have implemented a strict Room database migration strategy and removed any destructive migration fallbacks.

## Changes Made

1. **Removed Destructive Fallback**
   - Location: [AppContainerImpl.kt](file:///c:/Users/Khalil/antigravity/FinTrack-DZ-2026-05-29-30f3a/app/src/main/java/com/qdash/core/di/AppContainerImpl.kt)
   - Completely deleted the reflective block that invoked destructive migration fallbacks in `BuildConfig.DEBUG` builds.
   - Now, any mismatch between the database schema and the active code entities will trigger a crash instead of silently wiping all user data.

2. **Schema Export Configuration**
   - Verified that `exportSchema = true` is set on the `@Database` annotation in [AppDatabase.kt](file:///c:/Users/Khalil/antigravity/FinTrack-DZ-2026-05-29-30f3a/app/src/main/java/com/qdash/data/local/AppDatabase.kt).
   - Verified that schema schemas are exported to the project folder `${projectDir}/schemas` via KSP in [app/build.gradle.kts](file:///c:/Users/Khalil/antigravity/FinTrack-DZ-2026-05-29-30f3a/app/build.gradle.kts) and tracked in version control.

---

## Strict Migration Policy

### Rule: No Destructive Migrations
Destructive migrations are **permanently disabled**. Any schema changes must be handled using structured migration objects.

### Procedure for Schema Updates
Whenever you change the database schema (e.g., adding/modifying tables or columns):
1. **Increment Database Version**: Increase the `version` number in the `@Database` annotation of `AppDatabase.kt`.
2. **Define a Migration Object**: Define a new `Migration(oldVersion, newVersion)` object inside [Migrations.kt](file:///c:/Users/Khalil/antigravity/FinTrack-DZ-2026-05-29-30f3a/app/src/main/java/com/qdash/core/data/Migrations.kt).
3. **Write SQL Changes**: Implement the schema changes using standard SQLite commands (e.g., `ALTER TABLE`, `CREATE TABLE`) within the `migrate(db: SupportSQLiteDatabase)` function.
4. **Register the Migration**: Append your new migration object to the `ALL_MIGRATIONS` array at the bottom of `Migrations.kt`.
5. **Verify the Migration**:
   - Run `./gradlew assembleDebug` to trigger KSP and generate the updated schema JSON file.
   - Run tests to verify the schema updates.

---

# Implementation Walkthrough: Enhanced Algerian Category Tree & Smart Categorization Engine

We have enhanced the financial transaction categorization system specifically for Algerian users with local dialect terms (Darja), French, and local financial habits.

## 1. Modular Architecture & Data Layer
- **`AlgerianKeywordDictionary.kt`**: Extracted isolated dictionary mapping local Algerian terms (e.g. `فليكسي`, `موبيليس`, `خضرة`, `مارشي`, `حافلة`, `تاكسي`, `فاست فود`, `محاجب`, `دروغري`, `كوسميتيك`, `مصروف عائلي`, `الذهبية`, `CCP`) to category and subcategory names.
- **`DatabaseSeeder.kt`**: Extended `prepopulateSystemDefaults` to seed Algerian-tailored subcategories (`خضار وفواكه`, `أكل سريع`, `خردوات وعتاد`, `خدمات محلية`, `كوسميتيك وعناية`, `مصروف عائلي وكفالة`) safely with `insertCategoryIgnoreConflict` without overwriting custom user categories.

## 2. Decoupled Categorization Engine & Async Execution
- **`RuleBasedCategorizationEngine.kt`**: Refactored to execute multi-stage evaluation strictly on `Dispatchers.Default`:
  1. `HISTORY`: User's historical persistent mappings (`user_category_mappings` table).
  2. `KEYWORD`: High-confidence match via `AlgerianKeywordDictionary`.
  3. `RULE`: Dynamic active database category rules (`category_rules` table).
  4. `AI`: Local AI fallback if enabled.
- **`GetSmartCategorySuggestionUseCase.kt`**: Created dedicated use case under `domain/usecase/transaction/` for debounced non-blocking suggestions.
- **`LearnCategoryMappingUseCase.kt`**: Saves/increments confidence count for user-accepted category suggestions.

## 3. Real-Time Smart Suggestion UI
- **`SmartCategorySuggestionRow.kt`**: Created isolated, `@Stable` Composable placed directly below transaction note/title input (`OutlinedTextField`).
- **High vs. Soft Confidence Chips**: Displays clear visual badges for high-confidence auto-applies vs soft suggestions with accept/dismiss actions in Material 3 RTL layout.
- **ViewModel Debouncing**: Handled via `AddTransactionViewModel` and `TransactionsViewModel` with 200ms input debouncing.

---

# Implementation Walkthrough: Expanded Categories, Subcategories & SVG Icon Architecture

We have modularized category seeding and implemented native SVG vector icon resource mappings for Algerian financial tracking.

## 1. Standalone Category Seeder
- **`AlgerianCategoriesSeeder.kt`**: Extracted a dedicated seeder under `data/local/seeder/` executing on `Dispatchers.IO`. Seeds root and subcategories tailored to Algerian habits (Marché / Khodra, Flexy / 4G Box, Fast Food / Pizzeria, Kosmetik / Droguerie, Transport / Taxi / Tramway, Kaffala / Family, Bricolage / Hardware, Cabinet & Pharmacie).
- **`DatabaseSeeder.kt`**: Delegated category prepopulation to `AlgerianCategoriesSeeder`.

## 2. SVG Vector Icon Assets & Mapping Registry
- **Vector Drawables**: Added custom XML vector drawables in `res/drawable/` (`ic_cat_marche.xml`, `ic_cat_flexy.xml`, `ic_cat_transport.xml`, `ic_cat_health.xml`, `ic_cat_fastfood.xml`, `ic_cat_bricolage.xml`, `ic_cat_education.xml`, `ic_cat_family.xml`, `ic_cat_default.xml`).
- **Default Fallback SVG Icon (`ic_cat_default.xml`)**: Designed a dedicated vector icon as the default fallback for user-created custom categories without a specific icon assignment.
- **`CategoryIconRegistry.kt`**: Created an isolated token registry mapping icon keys to vector resource IDs, setting `ic_cat_default` as the automatic fallback for unmapped keys.
- **`IconMapper.kt`**: Updated to resolve both standard Compose `ImageVector` icons and custom SVG vector resource tokens.

## 3. Hierarchy Domain & UI Decomposition
- **`GetCategoriesWithSubcategoriesUseCase.kt`**: Added use case grouping subcategories under parent category models.
- **`SubcategoryChipGrid.kt`**: Created modular Composable displaying subcategories as selectable chips.
- **`CategoryIconPickerBottomSheet.kt`**: Created isolated LazyGrid modal bottom sheet for picking category vector icons.

---

# Implementation Walkthrough: Real-Time Account Balance Live Preview Fix (Edit Mode)

We fixed the Live Preview balance calculation in Edit Mode so that it restores the original transaction's impact before calculating the new preview balance.

## 1. Edit Mode Balance Restoration Logic
- **`AddTransactionViewModel.kt`**: Added `initEditMode(...)` and `calculatePreviewBalances(...)` to track original transaction parameters (`originalAmount`, `originalType`, `originalAccountId`, `originalToAccountId`).
- **`AddTransactionOperations.kt`**: Refactored `calculateExpectedBalances` to compute `baseBalance` by restoring the original transaction:
  - If editing an expense on the original account: `baseBalance = currentBalance + originalAmount`.
  - If editing an income on the original account: `baseBalance = currentBalance - originalAmount`.
  - If changing account: Original account balance is restored, and the new account balance takes the full impact.
- **`AddTransactionScreen.kt`**: Integrated `addTxViewModel` to feed accurate live balance previews.

## 2. Verification & Automated Tests
- **`AddTransactionViewModelTest.kt`**: Added unit tests covering:
  - Editing an expense on the same account (verifying `320 DZD` balance with `680 DZD` original expense edited to `1000 DZD` yields `0 DZD` preview balance).
  - Editing an expense to the same amount (`320 DZD -> 320 DZD`).
  - Editing an expense with account change (restoring old account, impacting new account).
  - Non-edit mode (new transaction direct impact).


