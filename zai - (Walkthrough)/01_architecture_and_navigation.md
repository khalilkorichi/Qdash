# تحليل البنية الهيكلية والتنقل — تطبيق FinTrack DZ (قداشّ)

> **تاريخ التحليل:** 2026-06-22  
> **الإصدار:** v1.0.0.12 (versionCode 12)  
> **المصدر:** فحص مباشر لملفات المشروع

---

## 1. البنية التقنية العامة

### 1.1 تقنية المشروع

| العنصر | التفاصيل |
|---|---|
| **المنصة** | Android (API 24 – API 36) |
| **اللغة** | Kotlin 2.2.10 |
| **البنية** | Clean Architecture — طبقات `core / data / domain / presentation / ui` |
| **واجهة المستخدم** | Jetpack Compose + Material 3 |
| **قاعدة البيانات** | Room v14 (19 كيانًا، اسم `kdach_database`) |
| **التنقل** | Navigation Compose مع `NavHost` مركزي |
| **حقن التبعية** | يدوي عبر `AppContainer` / `AppContainerImpl` |
| **الشبكة** | Retrofit + OkHttp + Moshi |
| **الإعدادات** | DataStore Preferences |
| **الأيقونات** | Material Icons (Core + Extended) |
| **التوقيع** | debug (keystore) + release (env vars) |
| **Build System** | Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`) |

### 1.2 هيكل الحزم

```
app/src/main/java/com/example/
├── FinTrackApp.kt              ← Application class
├── MainActivity.kt             ← Single launcher activity
│
├── core/
│   ├── data/                   ← Migrations.kt, DatabaseSeeder.kt
│   ├── di/                     ← AppContainer.kt, AppContainerImpl.kt
│   ├── preferences/            ← PreferencesManager (DataStore)
│   ├── ui/components/          ← Shared Compose components (12 file)
│   └── utils/                  ← FormatterUtils, FileUtils, SystemNotificationHelper
│
├── data/
│   ├── backup/                 ← BackupManager (ZIP export/import)
│   ├── categorization/         ← RuleBasedCategorizationEngine
│   ├── local/
│   │   ├── dao/                ← 18 DAO interface
│   │   └── entities/           ← 19 Room entity
│   ├── repository/             ← 12 Repository implementation
│   └── update/                 ← UpdateRepositoryImpl (GitHub CDN)
│
├── domain/
│   ├── model/                  ← Domain models + mappers (Entity↔Domain)
│   ├── repository/             ← Repository contracts (interfaces)
│   └── usecase/                ← Use cases (budget, savings, debt, transfer, export, categorization, templates)
│
├── presentation/
│   ├── ViewModelFactory.kt     ← Centralized VM factory
│   ├── navigation/             ← NavRoutes.kt, NavGraph.kt
│   ├── app/                    ← AppShell, FinTrackAppComposable, BottomNavBar
│   ├── accounts/               ← AccountsScreen, AccountsViewModel
│   ├── ai/                     ← AiChatScreen, AiChatViewModel, FloatingAiBubble, MiniChatOverlay
│   ├── analytics/              ← AnalyticsScreen, AnalyticsViewModel, AnalyticsComponents, DashboardComponents
│   ├── backup/                 ← BackupScreen, BackupViewModel
│   ├── budgetgoals/            ← BudgetGoalsScreen, AddBudgetGoalScreen, BudgetGoalDetailsScreen
│   ├── categories/             ← CategoriesScreen, CategoriesViewModel
│   ├── components/             ← RadialMenu FAB
│   ├── debt/                   ← DebtsScreen, DebtViewModel
│   ├── export/                 ← ExportScreen, ExportViewModel
│   ├── home/                   ← HomeScreen, HomeViewModel, HomeComponents
│   ├── notifications/          ← NotificationsScreen, NotificationsViewModel
│   ├── onboarding/             ← OnboardingScreen, OnboardingViewModel
│   ├── plans/                  ← FinancialPlansScreen, FinancialPlansViewModel
│   ├── salary/                 ← SalaryScreen, SalaryViewModel
│   ├── savings/                ← SavingsScreen, SavingsViewModel
│   ├── search/                 ← SearchScreen, SearchViewModel
│   ├── settings/               ← SettingsScreen, SettingsViewModel
│   ├── simulator/              ← DocumentSimulator*, PostalProfiles*, CreateEditPostalProfile*
│   ├── subscriptions/          ← SubscriptionsScreen, SubscriptionsViewModel
│   ├── templates/              ← TemplatesScreen, CreateEditTemplateScreen
│   ├── transactions/           ← TransactionsScreen, AddTransactionScreen, IncomeHistoryScreen
│   ├── transfer/               ← TransferScreen, TransferViewModel
│   └── update/                 ← UpdatesScreen, UpdatesViewModel, UpdateBottomBar
│
└── ui/
    ├── designsystem/
    │   ├── components/         ← AppCard, AppButton, AppInput, AppDialog, AppEmptyState, AppLoadingState, AppSectionHeader, AppBottomSheet
    │   └── tokens/            ← ColorTokens, ShapeTokens, SpacingTokens, MotionTokens
    └── theme/
        ├── Color.kt            ← Semantic color aliases
        ├── Theme.kt            ← KdachTheme (Light + Dark)
        └── Type.kt             ← IBM Plex Sans Arabic typography
```

### 1.3 كيانات قاعدة البيانات (19 كيانًا — Room v14)

| # | الكيان | الوظيفة |
|---|---|---|
| 1 | `TransactionEntity` | المعاملات المالية (دخل/مصروف/تحويل) |
| 2 | `AccountEntity` | الحسابات البنكية والبريدية |
| 3 | `CategoryEntity` | فئات المصاريف والدخل |
| 4 | `IncomeSourceEntity` | مصادر الدخل (راتب/عمل حر/هدايا/إيجار) |
| 5 | `SavingGoalEntity` | أهداف الادخار |
| 6 | `SubscriptionEntity` | الاشتراكات المتكررة |
| 7 | `BudgetGoalEntity` | أهداف الميزانية |
| 8 | `CategoryRuleEntity` | قواعد التصنيف التلقائي |
| 9 | `UserCategoryMappingEntity` | تعيينات الفئات يدوية |
| 10 | `SavingsContributionEntity` | مساهمات الادخار |
| 11 | `DebtEntity` | الديون والالتزامات |
| 12 | `DebtPaymentEntity` | دفعات الديون |
| 13 | `TransferEntity` | عمليات التحويل |
| 14 | `NotificationEntity` | الإشعارات داخل التطبيق |
| 15 | `FinancialPlanEntity` | الخطط المالية |
| 16 | `DailyFinancialAggregateEntity` | الإجماليات اليومية المالية |
| 17 | `TransactionTemplateEntity` | قوالب المعاملات المتكررة |
| 18 | `AiChatMessageEntity` | سجل محادثات المساعد الذكي |
| 19 | `PostalProfileEntity` | ملفات الحسابات البريدية |

> ⚠️ `fallbackToDestructiveMigration()` مفعّل — أي تغيير غير مغطّى بهجرة سيحذف بيانات المستخدم.

---

## 2. نظام التنقل (Navigation Architecture)

### 2.1 بنية التوجيه

التطبيق يستخدم **`NavHost` مركزي** مع `Screen` sealed class يعرّف جميع المسارات:

```
AppShell (يملك NavController)
  └── NavHost (startDestination = Onboarding أو Home)
       ├── Onboarding.route          → OnboardingScreen
       ├── Home.route                → HomeScreen
       ├── add_transaction/{params}  → AddTransactionScreen
       ├── transactions              → TransactionsScreen
       ├── accounts                  → AccountsScreen
       ├── analytics                 → AnalyticsScreen
       ├── settings                  → SettingsScreen
       ├── savings                   → SavingsScreen
       ├── subscriptions             → SubscriptionsScreen
       ├── debts                     → DebtsScreen
       ├── transfer                  → TransferScreen
       ├── export                    → ExportScreen
       ├── budget_goals              → BudgetGoalsScreen
       ├── add_budget_goal           → AddBudgetGoalScreen
       ├── budget_goal_details/{id}  → BudgetGoalDetailsScreen
       ├── salary                    → SalaryScreen
       ├── notifications             → NotificationsScreen
       ├── ai_chat                   → AiChatScreen
       ├── search                    → SearchScreen
       ├── categories                → CategoriesScreen
       ├── financial_plans           → FinancialPlansScreen
       ├── templates                 → TemplatesScreen
       ├── create_template           → CreateEditTemplateScreen
       ├── edit_template/{id}        → CreateEditTemplateScreen
       ├── backup                    → BackupScreen
       ├── income_history            → IncomeHistoryScreen
       ├── updates                   → UpdatesScreen
       ├── document_simulator_entry  → DocumentSimulatorEntryScreen
       ├── document_simulator/{type} → DocumentSimulatorScreen
       ├── postal_profiles           → PostalProfilesScreen
       └── create_edit_postal_profile → CreateEditPostalProfileScreen
```

### 2.2 شريط التنقل السفلي (Bottom Navigation Bar)

يظهر فقط على الشاشات الرئيسية الأربعة المعرّفة في `mainBottomNavScreens`:

| الموضع | الشاشة | الأيقونة | التسمية |
|---|---|---|---|
| 1 | `Home` | `Home` (Filled/Outlined) | الرئيسية |
| 2 | `Analytics` | `PieChart` (Filled/Outlined) | الإحصائيات |
| — | *فراغ (مكان FAB)* | — | — |
| 4 | `Accounts` | `AccountBalanceWallet` (Filled/Outlined) | الحسابات |
| 5 | `Settings` | *أفاتار "خ"* | الإعدادات |

**خصائص شريط التنقل:**
- تصميم **Notched** (مقطوع في الوسط لمكان FAB)
- ارتفاع `68.dp` مع `RoundedCornerShape(24.dp)`
- ظل خفيف (`elevation: 12.dp`) للحصول على تأثير عائم
- يتلاشى/يظهر بتحريك `slideInVertically + fadeIn` عند التنقل
- عناصر NavItem لها تأثير **scale animation** عند الاختيار (`scale: 1.15x` عند التحديد)
- أيقونات تتحول بين `Filled` و `Outlined` حسب حالة التحديد
- زر Settings يستخدم **أفاتار دائري** مع تدرج لوني (`Primary → Purple`) بدلاً من أيقونة

### 2.3 حركات التنقل (Navigation Transitions)

جميع التنقلات تستخدم نفس نمط الحركة:

| الاتجاه | حركة الدخول | حركة الخروج |
|---|---|---|
| Forward (دفع) | `slideInHorizontally(+it/6)` + `fadeIn(300ms)` | `slideOutHorizontally(-it/6)` + `fadeOut(200ms)` |
| Back (رجوع) | `slideInHorizontally(-it/6)` + `fadeIn(300ms)` | `slideOutHorizontally(+it/6)` + `fadeOut(200ms)` |

التأثير: انزلاق خفيف (1/6 العرض) مع تلاشي — يعطي إحساسًا سلسًا وغير مزعج.

### 2.4 نقاط الدخول للمسارات العميقة

بعض الشاشات تتلقّى معاملات عبر URL:

| الشاشة | المعاملات | المثال |
|---|---|---|
| `AddTransaction` | `type, transactionId, date, draft` | `add_transaction?type=EXPENSE&date=1716000000` |
| `BudgetGoalDetails` | `budgetId` (path) | `budget_goal_details/5` |
| `EditTemplate` | `templateId` (path) | `edit_template/3` |
| `DocumentSimulator` | `docType` (path) | `document_simulator/CHEQUE` |
| `CreateEditPostalProfile` | `profileId` (query, nullable) | `create_edit_postal_profile?profileId=2` |

### 2.5 التدفق من الشاشة الرئيسية (Home → Deeplinks)

الشاشة الرئيسية تعمل كـ **Hub مركزي** مع 13 رابط تنقل:

```
Home Screen
  ├── + إضافة عملية     → AddTransaction (EXPENSE)
  ├── + إضافة دخل       → AddTransaction (INCOME)
  ├── عرض كل المعاملات  → Transactions
  ├── عرض سجل المداخيل  → IncomeHistory
  ├── الحسابات          → Accounts
  ├── الادخار           → Savings
  ├── الاشتراكات         → Subscriptions
  ├── أهداف الميزانية   → BudgetGoals
  ├── التحويل           → Transfer
  ├── الديون            → Debts
  ├── الإشعارات         → Notifications
  ├── البحث             → Search
  └── المساعد البريدي  → DocumentSimulatorEntry
```

---

## 3. طبقة التبعيات (Dependency Injection)

### 3.1 AppContainer — الـ Container المركزي

بدون Hilt/Dagger، التطبيق يستخدم نمط **Service Locator** يدوي:

```
AppContainer (interface)
  └── AppContainerImpl (lazy singletons)
       ├── database: AppDatabase (Room)
       ├── updateRepository
       ├── transactionRepository
       ├── accountRepository
       ├── categoryRepository
       ├── incomeRepository
       ├── savingRepository
       ├── subscriptionRepository
       ├── budgetGoalRepository
       ├── categorizationRepository
       ├── categorizationEngine
       ├── notificationRepository
       ├── financialPlanRepository
       ├── aiRepository
       ├── debtRepository
       ├── transferRepository
       ├── exportRepository
       ├── transactionTemplateRepository
       ├── backupManager
       ├── postalProfileRepository
       ├── backupRepository
       ├── preferencesManager
       └── 18+ Use Cases (budget, savings, debt, transfer, export, categorization, templates)
```

### 3.2 ViewModelFactory

`ViewModelFactory` يحصل على `AppContainer` وينشئ ViewModels عند الطلب عبر NavGraph:

```kotlin
val viewModel: SomeViewModel = viewModel(factory = factory)
```

---

## 4. التكاملات الخارجية

| التكامل | المكان | الوظيفة |
|---|---|---|
| **Gemini AI** | `AiRepositoryImpl` | محادثة ذكية مع تحليل مالي (GEMINI_API_KEY) |
| **GitHub CDN** | `UpdateRepositoryImpl` | فحص تحديثات `update.json` وتحميل APK |
| **Notifications** | `MainActivity` + `SystemNotificationHelper` | إشعارات النظام (Android 13+) |
| **Backup** | `BackupManager` | تصدير/استيراد قاعدة البيانات كـ ZIP |
| **FileProvider** | `AndroidManifest` | مشاركة ملفات PDF المُصدّرة |

---

## 5. نظام التحديث داخل التطبيق

```
MainActivity (بعد 3 ثوانٍ)
  └── checkForUpdates() → update.json (GitHub raw CDN)
       └── مقارنة versionName + versionCode
            └── إرسال إشعار داخل التطبيق عند وجود تحديث

AppShell (ON_RESUME)
  └── UpdateBottomBar (3 مراحل)
       ├── المرحلة 1: تنبيه بوجود تحديث جديد
       ├── المرحلة 2: تحميل خلفي مع شريط تقدّم
       └── المرحلة 3: تثبيت فوري
```

`update.json` هو **المصدر الوحيد للحقيقة** — يحتوي: `versionCode, versionName, updateIdentity, publishedAt, apkUrl, apkSize, apkSha256, mandatory, releaseNotes`.
