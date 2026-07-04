# تحليل تجربة المستخدم وتفصيل الشاشات — تطبيق FinTrack DZ (قداشّ)

> **تاريخ التحليل:** 2026-06-22  
> **الإصدار:** v1.0.0.12 (versionCode 12)  
> **المصدر:** فحص مباشر لملفات `presentation/*/` و `app/AppShell.kt`

---

## 1. التدفق العام لتجربة المستخدم (User Flow)

### 1.1 أول فتح للتطبيق (First Launch)

```
┌─────────────────────┐
│     Onboarding       │ ← يظهر فقط إذا isFirstLaunch == true
│  (شاشة الترحيب)      │
└────────┬────────────┘
         │ onFinished()
         ├─ إرسال إشعار ترحيب 🇩🇿
         ├─ showNotification()
         └─ navigate(Home) { popUpTo(Onboarding) { inclusive = true } }
              │
              ▼
┌─────────────────────┐
│       Home           │ ← الشاشة الرئيسية
│   (الرئيسية)         │
└─────────────────────┘
```

### 1.2 الإطلاق المتكرر (Normal Launch)

```
┌─────────────────────┐
│    FinTrackApp        │
│  ┌─ KdachTheme ──┐  │ ← يُطبّق الـ Theme و RTL
│  │  ┌─ AppShell ─┐│  │ ← يُنشئ NavController + UpdateSystem + AI
│  │  │ NavHost    ││  │ ← startDestination = Home
│  │  │            ││  │
│  │  │ ┌─ Home ──┐││  │
│  │  │ │         │││  │ ← جاري عرض الشاشة
│  │  │ └─────────┘││  │
│  │  └────────────┘│  │
│  └────────────────┘  │
│                      │
│  [Update Check ← 3s] │ ← فحص تحديثات في الخلفية
│  [BottomNavBar]      │ ← شريط التنقل
│  [FAB (Radial)]      │ ← زر الإضافة الدائري
│  [AI Bubble]         │ ← فقاعة المساعد الذكي
└─────────────────────┘
```

### 1.3 المكونات العامة (Global Overlays)

تظهر فوق كل الشاشات الرئيسية:

| المكون | الموقع | الشروط | الوظيفة |
|---|---|---|---|
| **FinTrackBottomNavBar** | أسفل الشاشة | فقط على `mainBottomNavScreens` | التنقل بين 4 شاشات رئيسية |
| **AddActionFabContainer** | وسط أسفل الشاشة | فقط على `mainBottomNavScreens` | قائمة دائرية: إضافة إنفاق/دخل/تحويل/ادخار/دين |
| **FloatingAiBubble** | أسفل يمين الشاشة | فقط على `mainBottomNavScreens` | فقاعة المساعد الذكي "قداشّ" |
| **MiniChatOverlay** | أسفل يمين الشاشة | عندما يفتح المستخدم المحادثة المصغّرة | محادثة سريعة مع المساعد |
| **UpdateBottomBar** | فوق شريط التنقل | عند وجود تحديث متاح | 3 مراحل: تنبيه → تحميل → تثبيت |

> **ترتيب الطبقات (Z-order):**
> NavHost → UpdateBottomBar → BottomNavBar → FAB → AI Bubble/MiniChat

---

## 2. تفصيل الشاشات

### 2.1 شاشة الترحيب (Onboarding)

| الخاصية | التفاصيل |
|---|---|
| **المسار** | `Onboarding.route = "onboarding"` |
| **الملف** | `presentation/onboarding/OnboardingScreen.kt` |
| **الوظيفة** | تعريف المستخدم الجديد بالتطبيق |
| **التفاعل** | زر "إنهاء" → يُرسل إشعار ترحيب + ينتقل إلى Home |
| **التصميم** | وضع فاتح دائمًا (لا يُفعّل الـ dark theme أثناء Onboarding) |

---

### 2.2 الشاشة الرئيسية (Home)

| الخاصية | التفاصيل |
|---|---|
| **المسار** | `Home.route = "home"` — الشاشة الأولى بعد Onboarding |
| **الملفات** | `home/HomeScreen.kt`, `HomeComponents.kt`, `HomeViewModel.kt` |
| **Pull to Refresh** | ✅ نعم |
| **الأقسام (قابلة للتخصيص):** | |

#### الأقسام الداخلية:

| القسم | الوظيفة |
|---|---|
| **PremiumTopBalanceCard** | بطاقة الرصيد الإجمالي مع الدخل/المصروف الشهري، زر إخفاء/إظهار الأرصدة |
| **Setup Reminder** | تنبيه إعداد الحسابات (يظهر فقط إذا لا توجد أرصدة) |
| **split_cards** | بطاقات الدخل والمصاريف المنقسمة مع نسب التغيير |
| **context_templates** | قوالب ذكية سياقية + اقتراحات AI |
| **templates** | القوالب المثبتة (LazyRow أفقية) |
| **recent_transactions** | آخر المعاملات |
| **accounts_section** | ملخص الحسابات (LazyRow أفقية) |
| **savings_progress** | تقدم أهداف الادخار |
| **subscriptions** | الاشتراكات القادمة |
| **budget_goals** | تقدم أهداف الميزانية |

#### روابط التنقل من Home (13 رابط):

```
Home
├── إضافة عملية EXPENSE     → AddTransaction
├── إضافة دخل INCOME       → AddTransaction  
├── عرض كل المعاملات        → Transactions
├── سجل المداخيل            → IncomeHistory
├── الحسابات                → Accounts
├── الادخار                 → Savings
├── الاشتراكات              → Subscriptions
├── أهداف الميزانية        → BudgetGoals
├── التحويل                 → Transfer
├── الديون                  → Debts
├── الإشعارات              → Notifications
├── البحث                  → Search
└── المساعد البريدي       → DocumentSimulatorEntry
```

---

### 2.3 شاشة إضافة عملية (Add Transaction)

| الخاصية | التفاصيل |
|---|---|
| **المسار** | `add_transaction?type={type}&transactionId={transactionId}&date={date}&draft={draft}` |
| **الملفات** | `transactions/AddTransactionScreen.kt`, `AddTransactionComponents.kt` |
| **الوظيفة** | إضافة/تعديل معاملة مالية (دخل أو إنفاق) |
| **المعاملات** | `type`: EXPENSE/INCOME, `transactionId`: للتعديل, `date`: تاريخ مُسبق, `draft`: JSON قالب |
| **الميزات** | لوحة أرقام مخصصة، مُنسّق Western numerals، فئات مع ألوان مخصصة، حسابات، ملاحظات، تكرار |

---

### 2.4 شاشة الإحصائيات (Analytics) — الشاشة الأكبر

| الخاصية | التفاصيل |
|---|---|
| **المسار** | `Analytics.route = "analytics"` |
| **الملفات** | `analytics/AnalyticsScreen.kt` (1432 سطر), `AnalyticsComponents.kt`, `AnalyticsViewModel.kt`, `DashboardComponents.kt` |
| **Pull to Refresh** | ✅ نعم |

#### هيكل التبويبات الحالي (2 تبويب):

**تبويب 0: "التقارير العامة"**
| المكون | الوظيفة |
|---|---|
| `UnifiedScreenHeader` | عنوان: "التقارير والتحليلات" |
| `SmartDateNavigator` | مُبدّل الفترة (اليوم/الأسبوع/الشهر/السنة/الكل) مع تنقل للأمام/الخلف |
| `InteractiveDonutCard` | مخطط دائري تفاعلي (top-5 فئات) + قائمة وسائل شرح + اختيار لون بالنقر الطويل |
| Summary Cards | بطاقتان: "أعلى إنفاق مفرد" + "معدل الادخار" |
| `SmartInsightsCard` | التحليلات الذكية والتوقعات (3 علامات فرعية: صندوق الطوارئ / توقعات الراتب / سلوك الأيام) |
| Bar Chart | التدفق النقدي التاريخي (الدخل vs المصروف) — Canvas مخصص |
| `SavingsChallengesSection` | تحديات الادخار (تحدي 52 أسبوعاً) |
| Empty States | حالات فارغة مخصصة |

**تبويب 1: "لوحة التحكم والمقارنة"**
| المكون | الوظيفة |
|---|---|
| Period Switcher | شهرياً / سنوياً + Date Picker |
| `DashboardOverviewCard` | ملخص الدخل والاستهلاك (حلقة دائرية + نسبة الاستهلاك) |
| `CategoryVsIncomeChartCard` | نسبة المصاريف من الدخل (Donut/Bar toggle) ⚠️ مكرّر |
| `MonthComparisonCard` | مقارنة شهرين جنبًا إلى جنب (GroupedBarChart) |

---

### 2.5 شاشة الحسابات (Accounts)

| الخاصية | التفاصيل |
|---|---|
| **المسار** | `Accounts.route = "accounts"` |
| **الملف** | `accounts/AccountsScreen.kt` |
| **الوظيفة** | عرض وإدارة الحسابات المالية |
| **أنواع الحسابات** | BANK, CCP, BARIDIMOB, CASH, SAVINGS, WALLET, OTHER |

---

### 2.6 شاشة الإعدادات (Settings)

| الخاصية | التفاصيل |
|---|---|
| **المسار** | `Settings.route = "settings"` |
| **الملف** | `settings/SettingsScreen.kt` |
| **الوظيفة** | إعدادات التطبيق + مركز الوصول للميزات المتقدمة |
| **روابط التنقل الخارجية:** | |

```
Settings
├── أهداف الميزانية     → BudgetGoals
├── الديون              → Debts
├── التحويل            → Transfer
├── تصدير التقارير     → Export
├── الفئات             → Categories
├── الخطط المالية       → FinancialPlans
└── إدارة الراتب       → Salary
```

---

### 2.7 الشاشات الفرعية (الوصول عبر Home أو Settings أو NavDeepLinks)

| الشاشة | المسار | الوظيفة | نقطة الوصول |
|---|---|---|---|
| **المعاملات** | `transactions` | سجل جميع المعاملات | Home → "عرض الكل" |
| **سجل المداخيل** | `income_history` | سجل مصادر الدخل | Home → "سجل المداخيل" |
| **الادخار** | `savings` | أهداف الادخار ومتابعتها | Home → "الادخار" |
| **الاشتراكات** | `subscriptions` | إدارة الاشتراكات المتكررة | Home → "الاشتراكات" |
| **الديون** | `debts` | إدارة الديون وخطة السداد | Home → "الديون" |
| **التحويل** | `transfer` | تحويل بين الحسابات | Home → "التحويل" |
| **تصدير التقارير** | `export` | تصدير PDF (شهري/إحصائي/ادخار/ديون/كشف حساب) | Settings → "تصدير" |
| **أهداف الميزانية** | `budget_goals` | إنشاء/متابعة أهداف الميزانية | Home → "الميزانية" أو Settings |
| **إدارة الراتب** | `salary` | إدارة مصادر الدخل الشهرية | Settings → "الراتب" |
| **الفئات** | `categories` | إدارة فئات المصاريف والدخل | Settings → "الفئات" |
| **الخطط المالية** | `financial_plans` | تخطيط مالي متقدم | Settings → "الخطط المالية" |
| **القوالب** | `templates` | قوالب المعاملات المتكررة | Home → القوالب المثبتة |
| **إنشاء/تعديل قالب** | `create_template` / `edit_template/{id}` | إنشاء أو تعديل قالب | Templates → "إنشاء" / "تعديل" |
| **النسخ الاحتياطي** | `backup` | تصدير/استيراد قاعدة البيانات | Settings |
| **التحديثات** | `updates` | إدارة تحديثات التطبيق | Settings |
| **الإشعارات** | `notifications` | عرض الإشعارات داخل التطبيق | Home → "الإشعارات" أو رأس الصفحة |
| **البحث** | `search` | بحث شامل في المعاملات | رأس كل صفحة |
| **المساعد الذكي** | `ai_chat` | محادثة كاملة مع المساعد "قداشّ" | فقاعة AI → Full Screen |
| **المساعد البريدي** | `document_simulator_entry` | اختيار نوع الوثيقة البريدية | Home → "المساعد البريدي" |
| **محاكي الوثائق** | `document_simulator/{type}` | محاكاة وثيقة بريدية | DocumentSimulatorEntry |
| **ملفات الحسابات البريدية** | `postal_profiles` | إدارة الحسابات البريدية | DocumentSimulatorEntry → "إدارة الملفات" |
| **إنشاء/تعديل ملف بريدي** | `create_edit_postal_profile?profileId={id}` | إنشاء/تعديل ملف حساب بريدي | PostalProfiles |

---

## 3. أنماط التفاعل (Interaction Patterns)

### 3.1 أنماط التحميل (Loading Patterns)

| النمط | المكان | التفاصيل |
|---|---|---|
| **Shimmer Skeletons** | Analytics | `DonutChartSkeleton`, `SummaryCardSkeleton`, `BarChartSkeleton` مع `shimmerEffect()` |
| **Pull to Refresh** | Home, Analytics | Material 3 `PullToRefreshBox` |
| **Circular Progress** | Buttons (loading state), Export Dialog | `AppLoadingState`, `CircularProgressIndicator` |
| **Inline Loading** | Navigation transitions | شريحة + تلاشي أثناء التنقل |

### 3.2 أنماط الحالات الفارغة (Empty States)

| النمط | المكان | التفاصيل |
|---|---|---|
| **AnalyticsEmptyState** | Analytics (لا توجد معاملات) | مع زر "إضافة عملية" |
| **SimplePeriodEmptyState** | Analytics (لا توجد معاملات في الفترة) | يوضح الفترة المحددة |
| **AppEmptyState** | General | أيقونة + عنوان + وصف |

### 3.3 أنماط القوائم والحركات

| النمط | التفاصيل |
|---|---|
| **LazyColumn** | الشاشات الرئيسية تستخدم `LazyColumn` مع `Arrangement.spacedBy(16.dp)` |
| **LazyRow** | البطاقات الأفقية (الحسابات، القوالب) |
| **Radial FAB** | قائمة دائرية تتوسع عند الضغط مع 5 خيارات (إنفاق/دخل/تحويل/ادخار/دين) |
| **Long Press** | على عناصر Donut → تفتح نافذة تعديل لون الفئة |
| **Tap on Chart** | Bar Chart → يعرض تفاصيل الفترة المحددة |

---

## 4. تحليل تجربة المستخدم (UX Assessment)

### 4.1 نقاط القوة

| البعد | التقييم | التفاصيل |
|---|---|---|
| **التنقل** | ⭐⭐⭐⭐⭐ | شريط سفلي بـ 4 شاشات رئيسية + FAB مركزي + ذكاء اصطناعي عائم = وصول سريع لكل شيء |
| **المعلومات الهرمية** | ⭐⭐⭐⭐ | Home يعرض أهم المعلومات (الرصيد، آخر المعاملات، الحسابات) مع روابط للتفاصيل |
| **التعيين الذكي** | ⭐⭐⭐⭐ | قوالب سياقية + اقتراحات AI + تصنيف تلقائي يقلل الإدخال اليدوي |
| **نظام التحديثات** | ⭐⭐⭐⭐⭐ | تحديث شفاف بدون Play Store — شريط سفلي بـ 3 مراحل (تنبيه → تحميل → تثبيت) |
| **الأمان المالي** | ⭐⭐⭐⭐ | إخفاء الأرصدة + نسخ احتياطي كـ ZIP + لا بيانات سحابية (كله محلي) |
| **الشعور الاحترافي** | ⭐⭐⭐⭐⭐ | تصميم Notion-like فاخر + حركات فيزيائية + دعم RTL كامل |

### 4.2 فرص التحسين المحتملة

| البعد | الملاحظة | التأثير |
|---|---|---|
| **إعادة هيكلة Analytics** | التبويب الحالي فيه عنصر مكرّر (CategoryVsIncomeChartCard) + SmartInsightsCard مُدمج في تبويب عام | يمكن تقسيم إلى تبويبات أكثر تخصصًا |
| **التنقل العميق** | بعض الميزات المتقدمة (Export, Plans, Backup) تحتاج 3+ نقرات للوصول إليها | يمكن إضافة اختصارات في Home |
| **التبويبات داخل الصفحات** | بعض الشاشات لا تستخدم التبويبات (مثل Accounts, Transactions) رغم كثرة المحتوى | تبويبات لتحسين التنظيم |
| **SpacingTokens** | النظام موجود لكن غير مُطبّق بالكامل — بعض القيم hard-coded | توحيد لضمان تناسق أفضل |
| **الحوار بالعربية** | كل النصوص باللغة العربية مع دعم كامل لـ RTL | ✅ ممتاز |

### 4.3 تقييم التنقل

```
عمق التنقل (Clicks to Reach):
═════════════════════════════════════════════════════════════
Home                    = 0 clicks (مباشر)
├── Add Transaction      = 1 click  (FAB → إنفاق)
├── Add Income           = 1 click  (FAB → دخل)
├── Transfer             = 1 click  (FAB → تحويل)
├── Analytics            = 1 click  (Bottom Nav)
├── Accounts             = 1 click  (Bottom Nav)
├── Settings             = 1 click  (Bottom Nav)
├── AI Chat              = 1 click  (Floating Bubble)
├── Search               = 1 click  (Search icon in header)
├── Notifications       = 1 click  (Notification icon in header)
├── Debts                = 1 click  (FAB → ديون)
├── Savings              = 1 click  (Home → ادخار)
├── Subscriptions        = 1 click  (Home → اشتراكات)
├── BudgetGoals          = 1 click  (Home → ميزانية)
├── Transactions         = 1 click  (Home → عرض الكل)
├── Templates            = 1 click  (Home → القوالب)
├── Categories           = 2 clicks (Settings → فئات)
├── Export               = 2 clicks (Settings → تصدير)
├── Financial Plans      = 2 clicks (Settings → خطط)
├── Salary               = 2 clicks (Settings → راتب)
├── Backup               = 2 clicks (Settings → نسخ)
├── Updates              = 2 clicks (Settings → تحديثات)
├── Document Simulator   = 2 clicks (Home → مساعد بريدي)
├── BudgetGoal Details   = 2 clicks (BudgetGoals → تفاصيل)
└── Edit Template        = 2 clicks (Templates → تعديل)
```

> **النتيجة:** أكثر من 80% من الوظائف يمكن الوصول إليها بنقرة واحدة أو نقرتين — هذا ممتاز لتطبيق مالي.

---

## 5. الخصوصية الجزائرية (Algeria-Specific Features)

| الميزة | التفاصيل |
|---|---|
| **العملة** | DZD (الدينار الجزائري) — العملة الافتراضية لكل الأرصدة |
| **أنواع الحسابات** | CCP (حساب بريدي) + BaridiMob (الحساب الجوال البريدي) |
| **المحاكي البريدي** | محاكاة وثائق بريدية (شيك، حوالة، إيداع نقدي) |
| **اللغة** | عربية كاملة مع أرقام عربية/هندية اختيارية |
| **الأسماء الشهرية** | أسماء الجزائرية (جانفي، فيفري، مارس...) بدلاً من العربية الفصحى |
| **التسمية** | "قداشّ" — اللهجة الجزائرية (كم نسبة/كم المبلغ) |

---

## 6. ملخص تجربة المستخدم

| البعد | التقييم | الملاحظة |
|---|---|---|
| **سهولة التعلم** | ⭐⭐⭐⭐ | Onboarding بسيط + شريط تنقل واضح + FAB مركزي |
| **الكفاءة** | ⭐⭐⭐⭐⭐ | 80%+ من الوظائف بنقرة واحدة + قوالب ذكية |
| **التنظيم** | ⭐⭐⭐⭐ | صفحات متخصصة لكن بعضها يحتاج إعادة تنظيم (Analytics) |
| **الجمالية** | ⭐⭐⭐⭐⭐ | تصميم Notion-like فاخر + حركات فيزيائية |
| **الأمان والخصوصية** | ⭐⭐⭐⭐⭐ | بيانات محلية بالكامل + نسخ احتياطي + إخفاء الأرصدة |
| **الخصوصية الجزائرية** | ⭐⭐⭐⭐⭐ | CCP/BaridiMob + أسماء شهرية جزائرية + "قداشّ" |
| **إمكانية الوصول** | ⭐⭐⭐⭐ | RTL كامل + أرقام عربية/غربية + Power Save mode |