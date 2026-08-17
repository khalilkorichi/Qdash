package com.qdash.presentation.analytics

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.domain.model.CardAiContext
import com.qdash.domain.model.CardAiContextType
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.analytics.components.*
import com.qdash.presentation.navigation.LocalNavController
import com.qdash.presentation.navigation.Screen
import com.qdash.ui.theme.*
import java.util.Calendar
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    cardAiChatViewModel: CardAiChatViewModel,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf<CategoryShare?>(null) }
    var activeExplanationInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var longClickedCategory by remember { mutableStateOf<CategoryShare?>(null) }
    var activeAiChatContext by remember { mutableStateOf<CardAiContext?>(null) }

    val currentPeriodRange = remember(uiState) { computePeriodRange(uiState) }

    // Dashboard statistics (Tab 1)
    val dashboardTransactions = remember(uiState.transactions, uiState.dashboardPeriod, uiState.dashboardMonth, uiState.dashboardYear) {
        uiState.transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            if (uiState.dashboardPeriod == "MONTHLY") {
                cal.get(Calendar.MONTH) == uiState.dashboardMonth && cal.get(Calendar.YEAR) == uiState.dashboardYear
            } else {
                cal.get(Calendar.YEAR) == uiState.dashboardYear
            }
        }
    }
    val dashIncome = remember(dashboardTransactions) {
        dashboardTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val dashExpenses = remember(dashboardTransactions) {
        dashboardTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    LaunchedEffect(uiState.spendingsByCategory) { selectedCategory = null }
    LaunchedEffect(uiState.dashboardTab) { selectedCategory = null }

    val periodLabel = when (uiState.selectedPeriod) {
        "ALL"   -> "الكل"
        "DAY"   -> "اليوم"
        "WEEK"  -> "الأسبوع"
        "MONTH" -> "الشهر"
        "YEAR"  -> "السنة"
        else    -> uiState.selectedPeriod
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("analytics_screen")
    ) { innerPadding ->

        // ── Category detail dialog (long-press) ──
        longClickedCategory?.let { category ->
            val categoryTxs by remember(category.categoryName, uiState.transactions, uiState.categories) {
                derivedStateOf {
                    val catId = uiState.categories.firstOrNull { it.name == category.categoryName }?.id
                    uiState.transactions.filter { it.categoryId == catId }.sortedByDescending { it.date }
                }
            }
            CategoryColorPickerDialog(
                category = category,
                categoryTxs = categoryTxs,
                allCategories = uiState.categories,
                onDismiss = { longClickedCategory = null },
                onColorChange = { id, color -> viewModel.updateCategoryColor(id, color) },
                onMoveCategory = { id, parentId -> viewModel.moveCategory(id, parentId) }
            )
        }

        // ── Explanation info dialog ──
        activeExplanationInfo?.let { (title, msg) ->
            ExplanationInfoDialog(
                title = title,
                message = msg,
                onDismiss = { activeExplanationInfo = null }
            )
        }

        // ── Export dialogs ──
        uiState.exportingProgressText?.let { progress ->
            ExportProgressDialog(progressText = progress)
        }
        uiState.exportResult?.fileUri?.let { uri ->
            ExportResultDialog(fileUri = uri, context = context, onDismiss = { viewModel.clearExportState() })
        }
        uiState.exportError?.let { err ->
            ExportErrorDialog(errorMessage = err, onDismiss = { viewModel.clearExportState() })
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Header ──
                item {
                    UnifiedScreenHeader(
                        title = "التقارير والتحليلات",
                        subtitle = "حلل سلوكك الإنفاقي وحقق أهدافك الادخارية الذكية",
                        showBackButton = false,
                        actions = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = uiState.spendingsByCategory.isNotEmpty() && uiState.dashboardTab == 0,
                                    enter = androidx.compose.animation.fadeIn(animationSpec = tween(350)),
                                    exit = androidx.compose.animation.fadeOut(animationSpec = tween(350))
                                ) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.12f)) {
                                        Text(
                                            text = "عرض: $periodLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .clickable { viewModel.exportPdfReport() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = "تصدير تقرير PDF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )
                }

                // ── Tabs Row ──
                item {
                    AnalyticsTabsRow(
                        selectedTab = uiState.dashboardTab,
                        onTabSelected = { viewModel.setDashboardTab(it) }
                    )
                }

                // ── Tab 0: Reports ──
                if (uiState.dashboardTab == 0) {

                    item {
                        SmartDateNavigator(
                            uiState = uiState,
                            onPeriodChange = { viewModel.setPeriod(it) },
                            onPrev = { viewModel.navigatePrev() },
                            onNext = { viewModel.navigateNext() }
                        )
                    }

                    if (uiState.isLoading) {
                        item { DonutChartSkeleton() }
                    } else if (uiState.spendingsByCategory.isNotEmpty()) {
                        item {
                            InteractiveDonutCard(
                                shares = uiState.spendingsByCategory,
                                selectedCategory = selectedCategory,
                                onSelectedCategoryChange = { selectedCategory = it },
                                onAiChatClick = { ctx ->
                                    activeAiChatContext = ctx
                                    cardAiChatViewModel.openSheet(ctx)
                                },
                                onCategoryLongClick = { longClickedCategory = it },
                                categories = uiState.categories,
                                periodStart = currentPeriodRange.first,
                                periodEnd = currentPeriodRange.second
                            )
                        }
                    }

                    if (uiState.isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryCardSkeleton(modifier = Modifier.weight(1f))
                                SummaryCardSkeleton(modifier = Modifier.weight(1f))
                            }
                        }
                    } else if (uiState.spendingsByCategory.isNotEmpty()) {
                        item {
                            AnalyticsSummaryCards(
                                largestExpenseName = uiState.largestExpenseName,
                                largestExpense = uiState.largestExpense,
                                savingsRate = uiState.savingsRate
                            )
                        }
                    }

                    if (uiState.isLoading) {
                        item { BarChartSkeleton(modifier = Modifier.padding(horizontal = 16.dp)) }
                    } else if (uiState.spendingsByCategory.isNotEmpty()) {
                        item {
                            AnalyticsCashFlowChart(
                                trendData = uiState.trendData,
                                onHelpClick = {
                                    activeExplanationInfo = "التدفق النقدي التاريخي" to "يقارن هذا المخطط تاريخياً بين التدفقات النقدية الداخلة (إجمالي الدخل المالي) والتدفقات الخارجة (إجمالي النفقات والمصاريف) على مدار الفترات الزمنية السابقة.\n\nالفائدة: يساعدك في رصد اتجاه نموك المالي؛ فبقاء عمود الدخل أعلى باستمرار من عمود المصاريف يضمن زيادة ثروتك وبناء ملاءة مالية متينة."
                                }
                            )
                        }
                    }

                    if (!uiState.isLoading && uiState.spendingsByCategory.isEmpty()) {
                        item {
                            if (uiState.isDatabaseEmpty) {
                                AnalyticsEmptyState(
                                    onAddTransactionClick = { navController?.navigate(Screen.AddTransaction.createRoute("EXPENSE")) },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            } else {
                                SimplePeriodEmptyState(
                                    selectedPeriod = uiState.selectedPeriod,
                                    onAddTransactionClick = { navController?.navigate(Screen.AddTransaction.createRoute("EXPENSE")) },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }

                // ── Tab 1: Comparison ──
                } else if (uiState.dashboardTab == 1) {
                    item {
                        AnalyticsComparisonTabContent(
                            uiState = uiState,
                            dashIncome = dashIncome,
                            dashExpenses = dashExpenses,
                            periodStart = currentPeriodRange.first,
                            periodEnd = currentPeriodRange.second,
                            onPeriodChange = { viewModel.setDashboardPeriod(it) },
                            onMonthChange = { viewModel.setDashboardMonth(it) },
                            onYearChange = { viewModel.setDashboardYear(it) },
                            onCompareMonthAChange = { m, y -> viewModel.setCompareMonthA(m, y) },
                            onCompareMonthBChange = { m, y -> viewModel.setCompareMonthB(m, y) },
                            onAiChatClick = { ctx ->
                                activeAiChatContext = ctx
                                cardAiChatViewModel.openSheet(ctx)
                            }
                        )
                    }

                // ── Tab 2: Analytics Insights ──
                } else if (uiState.dashboardTab == 2) {
                    item {
                        AnalyticsInsightsTabContent(
                            uiState = uiState,
                            periodStart = currentPeriodRange.first,
                            periodEnd = currentPeriodRange.second,
                            onAiChatClick = { ctx ->
                                activeAiChatContext = ctx
                                cardAiChatViewModel.openSheet(ctx)
                            }
                        )
                    }

                // ── Tab 3: Savings ──
                } else if (uiState.dashboardTab == 3) {
                    item {
                        AnalyticsSavingsTabContent(uiState = uiState)
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (activeAiChatContext != null) {
            CardAiChatSheet(
                viewModel = cardAiChatViewModel,
                onDismiss = {
                    activeAiChatContext = null
                    cardAiChatViewModel.closeSheet()
                }
            )
        }
    }
}

private fun computePeriodRange(uiState: AnalyticsUiState): Pair<Long, Long> {
    val firstDayOfWeek = Calendar.getInstance().firstDayOfWeek
    return when (uiState.selectedPeriod) {
        "YEAR" -> {
            val start = Calendar.getInstance().apply {
                set(Calendar.YEAR, uiState.selectedYear)
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).apply {
                set(Calendar.MONTH, Calendar.DECEMBER); set(Calendar.DAY_OF_MONTH, 31)
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }
            Pair(start.timeInMillis, end.timeInMillis)
        }
        "WEEK" -> {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, -uiState.selectedWeekOffset)
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val startOfWeek = targetCal.timeInMillis
            Pair(startOfWeek, startOfWeek + (7L * 24 * 60 * 60 * 1000) - 1)
        }
        "DAY" -> {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -uiState.selectedDayOffset)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = targetCal.timeInMillis
            Pair(startOfDay, startOfDay + (24L * 60 * 60 * 1000) - 1)
        }
        "MONTH" -> {
            if (uiState.hasSalarySource) {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, uiState.selectedYear)
                    set(Calendar.MONTH, uiState.selectedMonth)
                    add(Calendar.MONTH, -1)
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, uiState.salaryDayOfMonth.coerceAtMost(maxDay))
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val endCal = (startCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, 1)
                    val nextMonthMax = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, uiState.salaryDayOfMonth.coerceAtMost(nextMonthMax))
                    add(Calendar.MILLISECOND, -1)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            } else {
                val start = Calendar.getInstance().apply {
                    set(Calendar.YEAR, uiState.selectedYear); set(Calendar.MONTH, uiState.selectedMonth); set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val end = (start.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
        }
        else -> Pair(0L, Long.MAX_VALUE)
    }
}
