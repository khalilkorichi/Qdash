# تحليل الهوية البصرية ونظام التصميم — تطبيق FinTrack DZ (قداشّ)

> **تاريخ التحليل:** 2026-06-22  
> **الإصدار:** v1.0.0.12 (versionCode 12)  
> **المصدر:** فحص مباشر لملفات `ui/theme/` و `ui/designsystem/`

---

## 1. فلسفة التصميم

التطبيق يتبع **نمط تصميم مستوحى من Notion (Notion-like Sleek Monochrome)** مع لمسات ديناميكية من **HeroUI**. المبادئ الأساسية:

- **سطح مسطّح (Flat Surfaces):** لا ظلال ثقيلة — ظل صفر على البطاقات، حدود رقيقة `1.dp` بدلاً من الظلال
- **ألوان أحادية (Monochrome Brand):** أسود/رمادي داكن كأساس مع ألوان دلالية حيّة فقط عند الحاجة
- **حدود رقيقة (Thin Outlines):** كل عنصر تفاعلي محاط بحدود رقيقة تمنح إحساسًا بالوصولية والدقة
- **حركات فيزيائية (Physics-based Motions):** Spring animations بدلاً من Tweens الخطية
- **دعم كامل لـ RTL:** كل الحركات والتخطيطات تتكيف تلقائيًا مع اتجاه النص العربي

---

## 2. نظام الألوان (Color System)

### 2.1 الألوان الأساسية (Brand Colors — ColorTokens)

مستوحاة من Notion Sleek Monochrome — أسود فاخر متدرّج:

| الرمز | Hex | الاستخدام |
|---|---|---|
| `Primary` | `#191919` | اللون الأساسي — Deep Charcoal/Black |
| `PrimaryDark` | `#0F0F0F` | أسود عميق |
| `PrimaryLight` | `#2E2E2E` | رمادي داكن كلون ثانوي |

### 2.2 الألوان الدلالية (Semantic Accents)

ألوان حيّة ذات تباين عالي في كلا الوضعين:

| الرمز | Hex | المعنى | الاستخدام |
|---|---|---|---|
| `Success` | `#22C55E` | إيجابي / دخل | أرصدة الدخل، معدلات الادخار، النمو |
| `Danger` | `#EF4444` | سلبي / إنفاق | المصاريف، التحذيرات، العجز |
| `Warning` | `#F59E0B` | تحذير / ادخار | الادخار، التحديات، التنبيهات المتوسطة |
| `Info` | `#3B82F6` | معلوماتي / تحويل | التحويلات، المعلومات العامة |

### 2.3 ألوان الوضع الفاتح (Light Theme)

مستوحاة من أوراق Notion البيضاء الدافئة:

| الرمز | Hex | الاستخدام |
|---|---|---|
| `BackgroundLight` | `#FBFBFA` | خلفية التطبيق — ورقة بيضاء دافئة |
| `SurfaceLight` | `#FFFFFF` | سطح البطاقات — أبيض نظيف |
| `CardLight` | `#FFFFFF` | خلفية البطاقات |
| `TextPrimaryLight` | `#1A1A1A` | النص الأساسي — حبر أسود |
| `TextSecondaryLight` | `#6A6A65` | النص الثانوي — رمادي حبري |
| `BorderLight` | `#E9E9E6` | الحدود — رمادي ورقي رقيق |

### 2.4 ألوان الوضع الداكن (Dark Theme)

مستوحاة من وضع Notion الداكن المميّز:

| الرمز | Hex | الاستخدام |
|---|---|---|
| `BackgroundDark` | `#09090B` | خلفية التطبيق — لوح داكن مميّز |
| `SurfaceDark` | `#121214` | سطح البطاقات — خلفية زنك داكنة |
| `CardDark` | `#1C1C1F` | خلفية البطاقات — زنك داكن |
| `ElevatedSurfaceDark` | `#252528` | أسطح مرتفعة — حوارات/أوراق سفلية |
| `TextPrimaryDark` | `#F4F4F5` | نص أساسي — أبيض مائل للرمادي |
| `TextSecondaryDark` | `#94A3B8` | نص ثانوي — رمادي أردوازي |
| `TextMutedDark` | `#64748B` | نص خافت |
| `BorderDark` | `#27272A` | حدود — تباين عالي |
| `DividerDark` | `#1E1E21` | فواصل — أغمق من الخلفية |

### 2.5 ألوان الـ Material 3 المُخصصة

| الوضع | `primary` | `onPrimary` | `secondary` | `tertiary` | `error` |
|---|---|---|---|---|---|
| **Dark** | `#818CF8` (Indigo-400) | `#09090B` | `#38BDF8` (Sky-400) | `#FBBF24` (Amber-400) | `#F87171` (Red-400) |
| **Light** | `#191919` (Charcoal) | `#FFFFFF` | `#3B82F6` (Blue) | `#F59E0B` (Amber) | `#EF4444` (Red) |

> **ملاحظة:** في الوضع الداكن، اللون الأساسي يتحول إلى Indigo حيّ (`#818CF8`) لتوفير تباين كافٍ على الخلفية الداكنة، بينما يبقى أسودًا في الوضع الفاتح. هذا اختيار مميّز يمنح التطبيق شخصية مزدوجة.

### 2.6 الألوان الدلالية الباستيلية للوضع الداكن

| الرمز | Hex | ملاحظة |
|---|---|---|
| `SuccessDark` | `#4ADE80` | أخضر فاتح (Green-400) |
| `DangerDark` | `#F87171` | أحمر فاتح (Red-400) |
| `WarningDark` | `#FBBF24` | كهرماني فاتح (Amber-400) |
| `InfoDark` | `#38BDF8` | أزرق فاتح (Sky-400) |

---

## 3. نظام الطباعة (Typography System)

### 3.1 الخط

التطبيق يستخدم **خط IBM Plex Sans Arabic** — خط احترافي مُحسّن للغة العربية:

| الاسم | الحقل في `R.font` | الأوزان المتوفرة |
|---|---|---|
| `IBMPlexSansArabic` | `ibmplexsansarabic_*` | Light, Regular, Medium, SemiBold, Bold |

> **ملاحظة:** جميع الأوزان متوفرة مسبقًا كملفات موارد في `res/font/`. لا يوجد استخدام لخطوط النظام الافتراضية — التطبيق يعتمد كليًا على IBM Plex Sans Arabic.

### 3.2 نسق الطباعة (Typography Scale)

استنادًا إلى `Typography` المعرّف في `Type.kt`:

| النمط | الحجم | الوزن | ارتفاع السطر | الحرف | الاستخدام |
|---|---|---|---|---|---|
| `displayLarge` | 32sp | Bold | 40sp | -0.5sp | عناوين رئيسية كبيرة (نادرة) |
| `displayMedium` | 28sp | Bold | 36sp | -0.3sp | عناوين كبيرة |
| `headlineLarge` | 24sp | Bold | 32sp | -0.3sp | عناوين الصفحات |
| `headlineMedium` | 22sp | SemiBold | 28sp | -0.2sp | عناوين فرعية |
| `titleLarge` | 20sp | Bold | 26sp | -0.2sp | عناوين البطاقات والأقسام |
| `titleMedium` | 15sp | Medium | 22sp | 0sp | عناوين داخل البطاقات |
| `titleSmall` | 13sp | Medium | 18sp | 0sp | عناوين صغيرة |
| `bodyLarge` | 15sp | Normal | 22sp | 0sp | نص رئيسي |
| `bodyMedium` | 13sp | Normal | 18sp | 0sp | نص عادي |
| `bodySmall` | 12sp | Normal | 16sp | 0sp | نص صغير |
| `labelLarge` | 13sp | Medium | 18sp | 0sp | تسميات |
| `labelMedium` | 12sp | Medium | 16sp | 0sp | تسميات صغيرة |
| `labelSmall` | 11sp | Medium | 14sp | 0sp | أصغر تسميات |

### 3.3 ملاحظات حول الطباعة

- جميع الأنماط تستخدم **letter spacing سلبي** (من -0.5sp إلى -0.2sp) في العناوين الكبيرة — مما يعطي إحساسًا بـ **tightly-kerned premium typography**
- النصوص العادية والصغيرة لا تستخدم letter spacing — تبقى طبيعية
- الأحجام تتراوح بين `11sp` (الأصغر) و `32sp` (الأكبر) — نطاق مناسب للقراءة المريحة

---

## 4. نظام الأشكال (Shape System)

معرّف في `ShapeTokens.kt`:

| الرمز | القيمة | الاستخدام النموذجي |
|---|---|---|
| `None` | `0.dp` | عناصر بدون تدوير |
| `Xs` | `4.dp` | حبات صغيرة، علامات |
| `Sm` | `6.dp` | أزرار صغيرة |
| `Md` | `8.dp` | البطاقات الافتراضية، الحقول النصية |
| `Lg` | `12.dp` | أقسام كبيرة، أزرار محورية |
| `Xl` | `16.dp` | بطاقات رئيسية |
| `Xxl` | `24.dp` | شريط التنقل السفلي، أوراق سفلية |
| `Full` | `99.dp` | حبات الدائرة، الأزرار الدائرية |

> **ملاحظة:** الشكل الأكثر استخدامًا فعليًا في الشاشات هو `RoundedCornerShape(16.dp)` و `RoundedCornerShape(20.dp)` و `RoundedCornerShape(24.dp)` — وهي أكبر من `Xxl`. يبدو أن التطبيق يميل إلى زوايا أكبر في البطاقات الرئيسية.

---

## 5. نظام المسافات (Spacing System)

معرّف في `SpacingTokens.kt`:

| الرمز | القيمة | الاستخدام النموذجي |
|---|---|---|
| `None` | `0.dp` | بدون مسافة |
| `Xxs` | `2.dp` | فواصل ضيقة |
| `Xs` | `4.dp` | مسافات صغيرة جدًا |
| `Sm` | `8.dp` | مسافات داخلية صغيرة |
| `Md` | `12.dp` | مسافات متوسطة |
| `Lg` | `16.dp` | المسافة الأكثر استخدامًا (padding أفقي) |
| `Xl` | `20.dp` | مسافات داخل البطاقات |
| `Xxl` | `24.dp` | رأس الصفحات |
| `Xxxl` | `32.dp` | مساحات كبيرة |
| `Giant` | `48.dp` | مسافات ضخمة |

> **ملاحظة:** على الرغم من وجود `SpacingTokens`، الكود الفعلي يستخدم أحيانًا قيمًا ثابتة مثل `16.dp` و `20.dp` و `24.dp` مباشرة في `Modifier.padding()` بدلاً من استخدام الرموز. هذا يعني أن النظام موجود كمرجع لكنه غير مُنفّذ بالكامل.

---

## 6. نظام الحركة (Motion / Animation System)

### 6.1 الرموز المميزة (MotionTokens)

| الدالة / القيمة | التفاصيل |
|---|---|
| `DurationShort` | 120ms — حركات سريعة (تغيير لون NavItem) |
| `DurationMedium` | 250ms — حركات متوسطة (تبديل التبويبات) |
| `DurationLong` | 400ms — حركات بطيئة (ظهور الأقسام) |
| `springFluid()` | Spring (damping: low bouncy, stiffness: low) — حركات سائلة |
| `springResponsive()` | Spring (damping: no bouncy, stiffness: medium) — استجابة سريعة |
| `springBouncy()` | Spring (damping: 0.65, stiffness: medium) — حركات مرنة |

### 6.2 تأثيرات الحركة المُستخدمة فعليًا

| التأثير | المكان | التفاصيل |
|---|---|---|
| **Scale on press** | الأزرار، البطاقات | `scale: 0.96x` (أزرار) / `0.98x` (بطاقات) عند الضغط |
| **Spring animations** | NavItem, Avatar | `scale: 1.15x` عند التحديد مع `DampingRatioLowBouncy` |
| **Color transitions** | Tab backgrounds, text | `animateColorAsState(tween 120-350ms)` |
| **Slide + Fade** | التنقل بين الصفحات | `slideInHorizontally(±it/6)` + `fadeIn(300ms)` |
| **Pull to Refresh** | الشاشات الرئيسية | Material 3 `PullToRefreshBox` |
| **Shimmer loading** | Skeleton screens | `shimmerEffect()` مع `FastOutSlowInEasing` — يتوقف في وضع توفير الطاقة |
| **PullToRefresh** | Analytics, Home | Material 3 `PullToRefreshBox` |

### 6.3 ميزة توفير الطاقة (Power Save Mode)

تأثير الـ Shimmer يتوقف تمامًا في وضع توفير الطاقة (`PowerManager.isPowerSaveMode`) ويعرض لونًا ثابتًا بدلاً من التحريك. هذه تفصيلة تُظهر اهتمامًا بأداء البطارية.

---

## 7. نظام المكونات (Component Library)

### 7.1 مكونات نظام التصميم (`ui/designsystem/components/`)

| المكون | الملف | الوظيفة | الأنواع (Variants) |
|---|---|---|---|
| **AppCard** | `AppCard.kt` | بطاقة أساسية | `SOLID`, `FLAT`, `OUTLINED`, `INTERACTIVE` |
| **AppButton** | `AppButton.kt` | زر قابل للتخصيص | `SOLID`, `BORDERED`, `FLAT`, `LIGHT` × `PRIMARY`, `SUCCESS`, `DANGER`, `WARNING`, `INFO` |
| **AppInput** | `AppInput.kt` | حقل إدخال نصي | مع label, placeholder, error, helper, icons |
| **AppDialog** | `AppDialog.kt` | حوار مخصص | مع/بدون تدمير، مع/بدون أيقونة |
| **AppEmptyState** | `AppEmptyState.kt` | حالة فارغة | مع أيقونة، عنوان، وصف |
| **AppLoadingState** | `AppLoadingState.kt` | حالة تحميل | `CircularProgressIndicator` + `AppSkeleton` + `shimmerEffect` |
| **AppSectionHeader** | `AppSectionHeader.kt` | رأس القسم مع أيقونة ذكية | يُختار أيقونة تلقائيًا حسب عنوان القسم |
| **AppBottomSheet** | `AppBottomSheet.kt` | ورقة سفلية | — |

### 7.2 المكونات المشتركة (`core/ui/components/`)

| المكون | الملف | الوظيفة |
|---|---|---|
| `UnifiedScreenHeader` | `UnifiedScreenHeader.kt` | رأس موحد للشاشات (عنوان + وصف + بحث + إشعارات) |
| `FinTrackTopBar` | `FinTrackTopBar.kt` | شريط علوي بسيط مع زر رجوع وعنوان |
| `BalanceHeroCard` | `BalanceHeroCard.kt` | بطاقة الرصيد الرئيسية |
| `TransactionItem` | `TransactionItem.kt` | عنصر معاملة واحد في القائمة |
| `AccountCard` | `AccountCard.kt` | بطاقة حساب |
| `CategoryChip` | `CategoryChip.kt` | شريحة فئة |
| `SavingsGoalCard` | `SavingsGoalCard.kt` | بطاقة هدف ادخار |
| `SubscriptionItem` | `SubscriptionItem.kt` | عنصر اشتراك |
| `BudgetProgressCard` | `BudgetProgressCard.kt` | بطاقة تقدم الميزانية |
| `EmptyStateView` | `EmptyStateView.kt` | عرض الحالة الفارغة |
| `AppComponents` | `AppComponents.kt` | مكونات عامة |
| `DriveSyncCard` | `DriveSyncCard.kt` | بطاقة مزامنة |

### 7.3 ملاحظات على المكونات

- **نمط البطاقة الافتراضي:** كل بطاقة تستخدم `CardDefaults.cardColors(containerColor: surfaceVariant)` مع `BorderStroke(1.dp, outlineVariant)` — وهو نمط **Notion-like مسطّح بحدود رقيقة**
- **أيقونات الحالة:** يتم استخدام `Box` بـ `background(primary.copy(alpha = 0.12f))` بداخلها `Icon` — نمط ثابت في كل الأقسام
- **الألوان الدلالية:** `IncomeGreen`, `ExpenseRed`, `SavingsAmber`, `TransferBlue` تُستخدم بشكل ثابت في كل البطاقات الإحصائية
- **رأس القسم الذكي:** `AppSectionHeader` يُختار أيقونة تلقائيًا حسب محتوى العنوان (مثلاً: يحتوي "الرئيسية" → `Home`، "الإحصائيات" → `Analytics`)

---

## 8. الـ Theme

### 8.1 إعداد Theme

```kotlin
KdachTheme(darkTheme = isDarkTheme) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl  // ← RTL دائمًا
    ) {
        FinTrackAppShell(...)
    }
}
```

- `dynamicColor = false` — ألوان العلامة التجارية ثابتة ولا تتأثر بألوان النظام
- الوضع الداكن لا يُفعّل أثناء Onboarding (يكون دائمًا فاتحًا)
- `LayoutDirection.Rtl` يُفرض على مستوى الجذر — كل شيء يتكيّف تلقائيًا

### 8.2 Edge-to-Edge

التطبيق يستخدم `enableEdgeToEdge()` مع شريط حالة وانتقال شفافين — يمنح إحساسًا بالانغماس الكامل في الشاشة.

---

## 9. ملخص الهوية البصرية

| البعد | الوصف |
|---|---|
| **الأسلوب** | Notion-like Sleek Monochrome + HeroUI dynamics |
| **الألوان** | أسود/رمادي كأساس، مع ألوان دلالية حيّة فقط |
| **الخط** | IBM Plex Sans Arabic (5 أوزان) |
| **الأشكال** | زوايا مستديرة كبيرة (16-24dp)، حدود رقيقة 1dp |
| **الحركة** | Spring-based + Tween، Power Save-aware |
| **الشعور العام** | تطبيق مالي احترافي، داكن وفاخر، مع دعم كامل للعربية RTL |
