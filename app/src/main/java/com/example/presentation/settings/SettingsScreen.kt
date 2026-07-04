package com.example.presentation.settings

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.DriveSyncCard
import com.example.core.ui.components.FinTrackTopBar
import com.example.ui.theme.*
import com.example.ui.designsystem.components.*
import com.example.ui.designsystem.tokens.ShapeTokens
import kotlinx.coroutines.launch

// ─── Tab Definitions ──────────────────────────────────────────────────────────
private data class SettingsTab(val label: String, val icon: ImageVector)

private val settingsTabs = listOf(
    SettingsTab("عام", Icons.Default.Settings),
    SettingsTab("الإشعارات", Icons.Default.Notifications),
    SettingsTab("النسخ الاحتياطي", Icons.Default.Backup),
    SettingsTab("الفئات", Icons.Default.Category),
    SettingsTab("متقدمة", Icons.Default.Tune)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    backupViewModel: com.example.presentation.backup.BackupViewModel,
    onNavigateToBudgetGoals: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToFinancialPlans: () -> Unit = {},
    onNavigateToSalary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var showGoogleConnectDialog by remember { mutableStateOf(false) }
    var inputEmail by remember { mutableStateOf("") }
    var showDashboardCustomizationDialog by remember { mutableStateOf(false) }

    // Notification toggles
    var notifBillingReminder by remember { mutableStateOf(true) }
    var notifSalaryReminder by remember { mutableStateOf(true) }
    var notifBudgetAlert by remember { mutableStateOf(true) }
    var notifWeeklyReport by remember { mutableStateOf(false) }
    var notifGoalProgress by remember { mutableStateOf(true) }

    val pagerState = rememberPagerState(initialPage = 0) { settingsTabs.size }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        FinTrackTopBar(title = "الإعدادات")

        // ── Premium Pill Tab Bar (sticky) ────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                settingsTabs.forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabContent"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) Primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabBorder"
                    )

                    val pillShape = RoundedCornerShape(12.dp)

                    Box(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(bgColor)
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = pillShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tab.label,
                                color = contentColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── Pager ──────────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> GeneralTab(
                    uiState = uiState,
                    onToggleDark = { viewModel.toggleDarkTheme(it) },
                    onToggleHideDecimals = { viewModel.toggleHideDecimals(it) },
                    onToggleAmountWords = { viewModel.toggleAmountWords(it) },
                    onToggleWesternNumerals = { viewModel.toggleWesternNumerals(it) },
                    onNavigateToBudgetGoals = onNavigateToBudgetGoals,
                    onNavigateToDebts = onNavigateToDebts,
                    onNavigateToTransfer = onNavigateToTransfer,
                    onNavigateToExport = onNavigateToExport,
                    onNavigateToFinancialPlans = onNavigateToFinancialPlans,
                    onNavigateToSalary = onNavigateToSalary,
                    onCustomiseDashboardClick = { showDashboardCustomizationDialog = true }
                )
                1 -> NotificationsTab(
                    billingReminder = notifBillingReminder,
                    salaryReminder = notifSalaryReminder,
                    budgetAlert = notifBudgetAlert,
                    weeklyReport = notifWeeklyReport,
                    goalProgress = notifGoalProgress,
                    onBillingToggle = { notifBillingReminder = it },
                    onSalaryToggle = { notifSalaryReminder = it },
                    onBudgetToggle = { notifBudgetAlert = it },
                    onWeeklyToggle = { notifWeeklyReport = it },
                    onGoalToggle = { notifGoalProgress = it }
                )
                2 -> com.example.presentation.backup.BackupScreen(
                    viewModel = backupViewModel,
                    onBack = {},
                    showTopBar = false
                )
                3 -> CategoriesTab(onNavigateToCategories = onNavigateToCategories)
                4 -> AdvancedTab()
            }
        }
    }

    // ─── Connect Google Dialog ────────────────────────────────────────────────
    if (showGoogleConnectDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleConnectDialog = false },
            title = {
                Text(
                    "ربط حساب Google Drive",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "أدخل بريدك الإلكتروني للمزامنة السحابية الآمنة.",
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppInput(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_email_input"),
                        placeholder = "example@gmail.com"
                    )
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        if (inputEmail.contains("@") && inputEmail.contains(".")) {
                            viewModel.connectGoogleDriveAccount(inputEmail)
                            showGoogleConnectDialog = false
                            inputEmail = ""
                            Toast.makeText(context, "تم ربط الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY
                ) { Text("توثيق وربط", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                AppButton(
                    onClick = { showGoogleConnectDialog = false },
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // ─── Confirm Restore Dialog ───────────────────────────────────────────────
    if (showConfirmRestoreDialog) {
        val context2 = LocalContext.current
        AlertDialog(
            onDismissRequest = { showConfirmRestoreDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "تأكيد استعادة البيانات",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            },
            text = {
                Text(
                    "تحذير: ستعمل الاستعادة على دمج بيانات النسخة الاحتياطية مع البيانات الحالية. هل أنت متأكد من المتابعة؟",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.runRestore(
                            onSuccess = { Toast.makeText(context2, "تمت استعادة بياناتك بنجاح!", Toast.LENGTH_LONG).show() },
                            onFailure = { err -> Toast.makeText(context2, "فشل الاستعادة: $err", Toast.LENGTH_LONG).show() }
                        )
                        showConfirmRestoreDialog = false
                    },
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY
                ) { Text("نعم، تأكيد", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                AppButton(
                    onClick = { showConfirmRestoreDialog = false },
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showDashboardCustomizationDialog) {
        var sectionsOrder by remember {
            mutableStateOf(uiState.dashboardSectionsOrder.toMutableList())
        }
        var visibleMap by remember {
            mutableStateOf(uiState.dashboardSectionsVisibility.toMutableMap())
        }
        var draggedIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffsetY by remember { mutableStateOf(0f) }
        val density = LocalDensity.current
        val itemHeightPx = with(density) { 70.dp.toPx() }

        AlertDialog(
            onDismissRequest = { showDashboardCustomizationDialog = false },
            title = {
                Text(
                    "تخصيص أقسام الصفحة الرئيسية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "استخدم مقبض السحب (☰) لترتيب العناصر بالسحب والإفلات، أو الأسهم، والمفاتيح لإظهار/إخفاء أي قسم.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AppButton(
                            onClick = {
                                val defaultOrder = "split_cards,context_templates,templates,quick_actions,accounts,chart,budget,subscriptions,recent_transactions".split(",")
                                sectionsOrder = defaultOrder.toMutableList()
                                val defaultVisibility = defaultOrder.associateWith { true }.toMutableMap()
                                visibleMap = defaultVisibility
                            },
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.PRIMARY,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "استعادة الافتراضي",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text("استعادة الافتراضي", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    sectionsOrder.forEachIndexed { index, section ->
                        val label = when (section) {
                            "split_cards" -> "بطاقة الدخل والمصاريف"
                            "context_templates" -> "اقتراحات ذكية (حسب الوقت)"
                            "templates" -> "القوالب المثبتة"
                            "quick_actions" -> "الوصول السريع"
                            "accounts" -> "حساباتي المالية"
                            "chart" -> "تحليل المصروفات"
                            "budget" -> "الميزانية الشهرية"
                            "subscriptions" -> "الاشتراكات القادمة"
                            "recent_transactions" -> "آخر العمليات"
                            else -> section
                        }

                        val isDragging = draggedIndex == index
                        val offsetY = if (isDragging) dragOffsetY else 0f

                        AppCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, offsetY.roundToInt()) }
                                .zIndex(if (isDragging) 10f else 1f),
                            variant = CardVariant.SOLID,
                            shape = ShapeTokens.Md,
                            backgroundColor = if (isDragging) 
                                    MaterialTheme.colorScheme.surfaceVariant 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "سحب للترتيب",
                                        tint = if (isDragging) Primary else TextGray.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(24.dp)
                                            .pointerInput(index) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        draggedIndex = index
                                                        dragOffsetY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        
                                                        val targetIndex = draggedIndex
                                                        if (targetIndex != null) {
                                                            val offsetIndexDiff = (dragOffsetY / itemHeightPx).roundToInt()
                                                            val newIndex = (targetIndex + offsetIndexDiff).coerceIn(0, sectionsOrder.size - 1)
                                                            if (newIndex != targetIndex) {
                                                                val newList = sectionsOrder.toMutableList()
                                                                val temp = newList[targetIndex]
                                                                newList[targetIndex] = newList[newIndex]
                                                                newList[newIndex] = temp
                                                                sectionsOrder = newList
                                                                draggedIndex = newIndex
                                                                dragOffsetY -= offsetIndexDiff * itemHeightPx
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        draggedIndex = null
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggedIndex = null
                                                        dragOffsetY = 0f
                                                    }
                                                )
                                            }
                                    )

                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val newList = sectionsOrder.toMutableList()
                                                val temp = newList[index]
                                                newList[index] = newList[index - 1]
                                                newList[index - 1] = temp
                                                sectionsOrder = newList
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "للأعلى",
                                            tint = if (index > 0) Primary else TextGray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < sectionsOrder.size - 1) {
                                                val newList = sectionsOrder.toMutableList()
                                                val temp = newList[index]
                                                newList[index] = newList[index + 1]
                                                newList[index + 1] = temp
                                                sectionsOrder = newList
                                            }
                                        },
                                        enabled = index < sectionsOrder.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "للأسفل",
                                            tint = if (index < sectionsOrder.size - 1) Primary else TextGray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right
                                )

                                Switch(
                                    checked = visibleMap[section] ?: true,
                                    onCheckedChange = { isChecked ->
                                        val newMap = visibleMap.toMutableMap()
                                        newMap[section] = isChecked
                                        visibleMap = newMap
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        viewModel.saveDashboardCustomization(sectionsOrder, visibleMap)
                        showDashboardCustomizationDialog = false
                        Toast.makeText(context, "تم حفظ الترتيب الجديد بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                AppButton(
                    onClick = { showDashboardCustomizationDialog = false },
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ─── TAB 1: General ──────────────────────────────────────────────────────────
@Composable
private fun GeneralTab(
    uiState: SettingsUiState,
    onToggleDark: (Boolean) -> Unit,
    onToggleHideDecimals: (Boolean) -> Unit,
    onToggleAmountWords: (Boolean) -> Unit,
    onToggleWesternNumerals: (Boolean) -> Unit,
    onNavigateToBudgetGoals: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToFinancialPlans: () -> Unit,
    onNavigateToSalary: () -> Unit,
    onCustomiseDashboardClick: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    var showAboutDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.isLoading) {
            SettingsSectionTitle("المظهر واللغة")
            SettingsItemSkeleton()
            SettingsItemSkeleton()
            SettingsItemSkeleton()
            SettingsSectionTitle("الأدوات والإدارة")
            repeat(6) { SettingsItemSkeleton() }
            Spacer(modifier = Modifier.height(96.dp))
        } else {
            SettingsSectionTitle("المظهر واللغة")

            SettingsItem(
                icon = if (uiState.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                iconTint = Color(0xFF7C3AED),
                title = "الوضع المظلم",
                subtitle = if (uiState.isDarkTheme) "وضع الليل مفعّل" else "وضع النهار مفعّل",
                trailing = {
                    Switch(
                        checked = uiState.isDarkTheme,
                        onCheckedChange = onToggleDark,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsItem(
                icon = Icons.Default.MonetizationOn,
                iconTint = IncomeGreen,
                title = "العملة الأساسية",
                subtitle = "الدينار الجزائري (DZD)",
                trailing = {
                    Text("DZD", style = MaterialTheme.typography.labelLarge, color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
            )

            SettingsItem(
                icon = Icons.Default.MoneyOff,
                iconTint = SavingsAmber,
                title = "إخفاء الفواصل الصفرية",
                subtitle = "إزالة الصفرين (00.) من عرض المبالغ المالية",
                trailing = {
                    Switch(
                        checked = uiState.isHideDecimalsEnabled,
                        onCheckedChange = onToggleHideDecimals,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsItem(
                icon = Icons.Default.TextFields,
                iconTint = TransferBlue,
                title = "توضيح المبلغ بالحروف",
                subtitle = "عرض المبلغ بالدينار والسنتيم كتابةً",
                trailing = {
                    Switch(
                        checked = uiState.isAmountWordsEnabled,
                        onCheckedChange = onToggleAmountWords,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsItem(
                icon = Icons.Default.Language,
                iconTint = Primary,
                title = "نظام الأرقام",
                subtitle = if (uiState.useWesternNumerals) "الأرقام الغربية (0-9)" else "الأرقام العربية (٠-٩)",
                trailing = {
                    Switch(
                        checked = uiState.useWesternNumerals,
                        onCheckedChange = onToggleWesternNumerals,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsSectionTitle("الأدوات والإدارة")

            val navController = com.example.presentation.navigation.LocalNavController.current
            SettingsNavItem(
                icon = Icons.Default.ReceiptLong,
                iconTint = Primary,
                title = "قوالب المعاملات",
                subtitle = "حفظ المعاملات المتكررة لسرعة تسجيلها",
                onClick = { navController?.navigate(com.example.presentation.navigation.Screen.Templates.route) }
            )

            SettingsNavItem(
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = IncomeGreen,
                title = "إدارة الراتب",
                subtitle = "إدارة مصادر الدخل وتوزيعها تلقائياً",
                onClick = onNavigateToSalary
            )

            SettingsNavItem(
                icon = Icons.Default.PieChart,
                iconTint = Primary,
                title = "أهداف الميزانية",
                subtitle = "تتبع الإنفاق الذكي والتنبيهات",
                onClick = onNavigateToBudgetGoals
            )

            SettingsNavItem(
                icon = Icons.Default.CreditCard,
                iconTint = ExpenseRed,
                title = "خطة الديون",
                subtitle = "جدولة وتسوية الالتزامات المالية",
                onClick = onNavigateToDebts
            )

            SettingsNavItem(
                icon = Icons.Default.SyncAlt,
                iconTint = TransferBlue,
                title = "تحويل الرصيد",
                subtitle = "تحويل الأرصدة بين الحسابات",
                onClick = onNavigateToTransfer
            )

            SettingsNavItem(
                icon = Icons.Default.PictureAsPdf,
                iconTint = Color(0xFFDC2626),
                title = "تصدير التقارير PDF",
                subtitle = "كشوف الحساب والتحليلات",
                onClick = onNavigateToExport
            )

            SettingsNavItem(
                icon = Icons.Default.Flag,
                iconTint = IncomeGreen,
                title = "الخطط المالية",
                subtitle = "خطط الادخار والأهداف طويلة الأمد",
                onClick = onNavigateToFinancialPlans
            )

            SettingsSectionTitle("تخصيص الواجهة")

            SettingsNavItem(
                icon = Icons.Default.Tune,
                iconTint = Primary,
                title = "ترتيب وإخفاء أقسام الواجهة",
                subtitle = "تخصيص البطاقات والترتيب في الصفحة الرئيسية",
                onClick = onCustomiseDashboardClick
            )

            SettingsSectionTitle("عن التطبيق")

            SettingsLogoNavItem(
                painter = painterResource(id = com.example.R.drawable.ic_app_logo),
                title = "نسخة التطبيق",
                subtitle = "قداشّ — الإصدار التجاري",
                onClick = { showAboutDialog = true }
            )

            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text("إغلاق", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.ic_app_logo),
                                    contentDescription = "Qdash Logo",
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "قداشّ",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الإصدار v${com.example.BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextGray
                            )
                        }
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تطبيق مالي شخصي لإدارة الميزانية والمصاريف اليومية، مصمم خصيصاً للمستخدمين الجزائريين.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "صنع بكل ❤️ في الجزائر",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            SettingsNavItem(
                icon = Icons.Default.SystemUpdate,
                iconTint = Primary,
                title = "تحديثات التطبيق",
                subtitle = "التحقق من وجود تحديثات جديدة وتثبيتها",
                onClick = { navController?.navigate(com.example.presentation.navigation.Screen.Updates.route) }
            )

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

// ─── TAB 2: Notifications ────────────────────────────────────────────────────
@Composable
private fun NotificationsTab(
    billingReminder: Boolean,
    salaryReminder: Boolean,
    budgetAlert: Boolean,
    weeklyReport: Boolean,
    goalProgress: Boolean,
    onBillingToggle: (Boolean) -> Unit,
    onSalaryToggle: (Boolean) -> Unit,
    onBudgetToggle: (Boolean) -> Unit,
    onWeeklyToggle: (Boolean) -> Unit,
    onGoalToggle: (Boolean) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSectionTitle("إشعارات الدخل والفواتير")

        SettingsItem(
            icon = Icons.Default.ReceiptLong,
            iconTint = SavingsAmber,
            title = "تذكير الفواتير",
            subtitle = "إشعار قبل موعد دفع الفواتير بـ 3 أيام",
            trailing = {
                Switch(
                    checked = billingReminder,
                    onCheckedChange = onBillingToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        SettingsItem(
            icon = Icons.Default.Payments,
            iconTint = IncomeGreen,
            title = "تذكير الراتب",
            subtitle = "إشعار عند توقع استلام الراتب الشهري",
            trailing = {
                Switch(
                    checked = salaryReminder,
                    onCheckedChange = onSalaryToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        SettingsSectionTitle("إشعارات الميزانية والأهداف")

        SettingsItem(
            icon = Icons.Default.Warning,
            iconTint = ExpenseRed,
            title = "تنبيهات تجاوز الميزانية",
            subtitle = "إشعار عند الاقتراب من حد الميزانية (80%)",
            trailing = {
                Switch(
                    checked = budgetAlert,
                    onCheckedChange = onBudgetToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        SettingsItem(
            icon = Icons.Default.Flag,
            iconTint = TransferBlue,
            title = "تقدم الأهداف الادخارية",
            subtitle = "إشعار عند اكتمال نسبة مئوية من الهدف",
            trailing = {
                Switch(
                    checked = goalProgress,
                    onCheckedChange = onGoalToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        SettingsSectionTitle("تقارير دورية")

        SettingsItem(
            icon = Icons.Default.BarChart,
            iconTint = Color(0xFF8B5CF6),
            title = "التقرير الأسبوعي",
            subtitle = "ملخص أسبوعي للإنفاق والادخار كل أحد",
            trailing = {
                Switch(
                    checked = weeklyReport,
                    onCheckedChange = onWeeklyToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}

// ─── TAB 3: Backup ───────────────────────────────────────────────────────────
// ─── TAB 3: Backup ───────────────────────────────────────────────────────────
@Composable
private fun BackupTab(
    uiState: SettingsUiState,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    onConnectGoogleClick: () -> Unit,
    onDisconnectGoogle: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.isLoading || uiState.isSyncing) {
            SettingsSectionTitle("النسخ الاحتياطي السحابي")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .shimmerEffect(RoundedCornerShape(16.dp))
            )
            SettingsSectionTitle("إعداد حساب Google Drive")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .shimmerEffect(RoundedCornerShape(16.dp))
            )
            SettingsSectionTitle("إجراءات الاستعادة")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shimmerEffect(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(96.dp))
        } else {
            SettingsSectionTitle("النسخ الاحتياطي السحابي")

            DriveSyncCard(
                lastBackupDate = uiState.lastBackupDate,
                onBackupClick = onBackupClick,
                onRestoreClick = onRestoreClick
            )

            SettingsSectionTitle("إعداد حساب Google Drive")

            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val email = uiState.connectedAccountEmail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (email != null) IncomeGreen else TextGray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                if (email != null) "حساب Google متصل" else "حساب السحابة غير متصل",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                email ?: "اربط حسابك لحماية بياناتك المالية",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    }
                    if (email != null) {
                        AppButton(
                            onClick = onDisconnectGoogle,
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.DANGER
                        ) { Text("إلغاء الربط", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    } else {
                        AppButton(
                            onClick = onConnectGoogleClick,
                            variant = ButtonVariant.SOLID,
                            intent = ButtonIntent.PRIMARY,
                            shape = ShapeTokens.Md
                        ) { Text("ربط الحساب", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            SettingsSectionTitle("إعدادات النسخ التلقائي")

            SettingsItem(
                icon = Icons.Default.CloudSync,
                iconTint = Primary,
                title = "النسخ الاحتياطي التلقائي",
                subtitle = "نسخ تلقائي دوري كل أول الشهر",
                trailing = {
                    Switch(
                        checked = uiState.isAutoBackupEnabled,
                        onCheckedChange = onAutoBackupToggle,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            )

            SettingsSectionTitle("إجراءات الاستعادة")

            // Backup now button
            AppButton(
                onClick = onBackupClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY,
                shape = ShapeTokens.Lg,
                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("نسخ احتياطي الآن", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Restore from backup
            AppButton(
                onClick = onRestoreClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.BORDERED,
                intent = ButtonIntent.PRIMARY,
                shape = ShapeTokens.Lg,
                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("استعادة من النسخة الاحتياطية", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

// ─── TAB 4: Categories ───────────────────────────────────────────────────────
@Composable
private fun CategoriesTab(onNavigateToCategories: () -> Unit) {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionTitle("إدارة فئات المعاملات")

        // Hero card to navigate
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            variant = CardVariant.SOLID,
            shape = ShapeTokens.Xl,
            onClick = onNavigateToCategories,
            backgroundColor = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(SavingsAmber.copy(alpha = 0.8f), Color(0xFFEC4899).copy(alpha = 0.6f))
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "إدارة الفئات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "أضف وعدّل وحذف فئات المصاريف والدخل وفئاتها الفرعية",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("فتح إدارة الفئات", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        SettingsSectionTitle("معلومات الفئات")

        SettingsItem(
            icon = Icons.Default.Info,
            iconTint = TransferBlue,
            title = "الفئات النظامية",
            subtitle = "لا يمكن حذف الفئات المدمجة مع التطبيق، لكن يمكن تعديلها",
            trailing = {}
        )

        SettingsItem(
            icon = Icons.Default.AccountTree,
            iconTint = Primary,
            title = "الفئات الفرعية",
            subtitle = "يمكنك إنشاء فئات فرعية داخل كل فئة رئيسية للتصنيف الدقيق",
            trailing = {}
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}

// ─── TAB 5: Advanced ─────────────────────────────────────────────────────────
@Composable
private fun AdvancedTab() {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSectionTitle("الخصوصية والبيانات")

        SettingsNavItem(
            icon = Icons.Default.DeleteForever,
            iconTint = ExpenseRed,
            title = "مسح كل البيانات",
            subtitle = "حذف جميع المعاملات والحسابات والفئات نهائياً",
            onClick = { /* confirm dialog */ }
        )

        SettingsNavItem(
            icon = Icons.Default.RestartAlt,
            iconTint = SavingsAmber,
            title = "إعادة الضبط الكامل",
            subtitle = "إرجاع التطبيق إلى الإعدادات الافتراضية الأولى",
            onClick = { /* confirm dialog */ }
        )

        SettingsSectionTitle("التشخيص والتقارير")

        SettingsItem(
            icon = Icons.Default.Storage,
            iconTint = Color(0xFF8B5CF6),
            title = "حجم قاعدة البيانات",
            subtitle = "يتم حساب هذا الحجم محلياً على الجهاز",
            trailing = {
                Text("محلي", style = MaterialTheme.typography.labelSmall, color = TextGray)
            }
        )

        SettingsItem(
            icon = Icons.Default.BugReport,
            iconTint = TextGray,
            title = "وضع التصحيح",
            subtitle = "عرض السجلات التقنية وتشخيص الأخطاء",
            trailing = {
                Text("معطّل", style = MaterialTheme.typography.labelSmall, color = TextGray)
            }
        )

        SettingsSectionTitle("معلومات التطبيق")

        SettingsItem(
            icon = Icons.Default.Info,
            iconTint = Primary,
            title = "الإصدار الحالي",
            subtitle = "قداشّ — نسخة الإنتاج",
            trailing = {
                Text("v1.0.0", style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.Bold)
            }
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}

// ─── Shared Composables ───────────────────────────────────────────────────────

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp,
            fontSize = 11.sp
        ),
        color = TextGray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun SettingsLogoNavItem(
    painter: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "v${com.example.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray
            )
        }
    }
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                Icons.Default.ArrowBack, // points forward in RTL
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsItemSkeleton(modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shimmerEffect(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(10.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 24.dp)
                    .shimmerEffect(RoundedCornerShape(12.dp))
            )
        }
    }
}

