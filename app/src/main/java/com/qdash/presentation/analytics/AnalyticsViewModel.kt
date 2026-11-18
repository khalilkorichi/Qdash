package com.qdash.presentation.analytics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryShare(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: String,
    val transactionCount: Int = 0,
    val directOnly: Boolean = false
)

data class CashFlowTrend(
    val periodLabel: String,
    val income: Double,
    val expense: Double
)

@Immutable
data class AnalyticsUiState(
    val selectedPeriod: String = "ALL", // "ALL", "DAY", "WEEK", "MONTH", "YEAR"
    val spendingsByCategory: List<CategoryShare> = emptyList(),
    val trendData: List<CashFlowTrend> = emptyList(),
    val largestExpense: Double = 0.0,
    val largestExpenseName: String = "لايوجد",
    val savingsRate: Float = 0f,
    val hasIncomeInPeriod: Boolean = false,
    val periodIncomeSum: Double = 0.0,
    val periodExpensesSum: Double = 0.0,
    val budgetConsumptionPercent: Float = 0f,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedDayOffset: Int = 0, // 0 = today, 1 = yesterday, ...
    val selectedWeekOffset: Int = 0, // 0 = this week, 1 = last week, ...
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),

    // Spend Projection & Salary Cycle
    val hasSalarySource: Boolean = false,
    val salaryDayOfMonth: Int = 1,
    val salaryAmount: Double = 0.0,
    val salaryCycleStartLabel: String = "",
    val salaryCycleEndLabel: String = "",
    val daysRemainingInCycle: Int = 0,
    val totalDaysInCycle: Int = 30,
    val salaryCyclePercentageElapsed: Float = 0f,
    val projectedEndMonthSpending: Double = 0.0,
    val isProjectedToExceedBudget: Boolean = false,
    val referenceBudget: Double = 0.0,

    // Weekend vs Weekday Spending
    val weekendExpensesSum: Double = 0.0,
    val weekdayExpensesSum: Double = 0.0,
    val weekendDailyAverage: Double = 0.0,
    val weekdayDailyAverage: Double = 0.0,
    val weekendPercentage: Float = 0f,
    val weekdayPercentage: Float = 0f,
    val hasWeekendData: Boolean = false,

    // Emergency Fund Runway & Savings Goals
    val totalSavingsAmount: Double = 0.0,
    val averageMonthlyExpense: Double = 0.0,
    val emergencyFundRunwayMonths: Float = 0f,
    val emergencyFundStatus: String = "SAFE", // "CRITICAL", "ACCEPTABLE", "SAFE", "EXCELLENT"
    val savingGoals: List<SavingGoal> = emptyList(),

    // PDF Export status
    val exportingProgressText: String? = null,
    val exportResult: ExportResult? = null,
    val exportError: String? = null,
    val isDatabaseEmpty: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val periodTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),

    // Dashboard & Comparison State
    val dashboardTab: Int = 0,
    val dashboardPeriod: String = "MONTHLY", // "MONTHLY", "ANNUALLY"
    val dashboardMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val dashboardYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val compareMonthA: Int = Calendar.getInstance().get(Calendar.MONTH),
    val compareYearA: Int = Calendar.getInstance().get(Calendar.YEAR),
    val compareMonthB: Int = run {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.get(Calendar.MONTH)
    },
    val compareYearB: Int = run {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.get(Calendar.YEAR)
    }
)

class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val incomeRepository: IncomeRepository,
    private val savingRepository: SavingRepository,
    private val exportRepository: ExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private data class PeriodFilterParams(
        val selectedPeriod: String,
        val selectedDayOffset: Int,
        val selectedWeekOffset: Int,
        val selectedMonth: Int,
        val selectedYear: Int
    )

    private data class ComputedAnalyticsPayload(
        val spendingsByCategory: List<CategoryShare>,
        val trendData: List<CashFlowTrend>,
        val largestExpense: Double,
        val largestExpenseName: String,
        val savingsRate: Float,
        val hasIncomeInPeriod: Boolean,
        val periodIncomeSum: Double,
        val periodExpensesSum: Double,
        val budgetConsumptionPercent: Float,
        val hasSalarySource: Boolean,
        val salaryDayOfMonth: Int,
        val salaryAmount: Double,
        val projection: AnalyticsCalculator.SpendProjection,
        val wkStats: AnalyticsCalculator.WeekendWeekdayStats,
        val efStats: AnalyticsCalculator.EmergencyFundStats,
        val savingGoals: List<SavingGoal>,
        val isDatabaseEmpty: Boolean,
        val transactions: List<Transaction>,
        val periodTransactions: List<Transaction>,
        val categories: List<Category>,
        val accounts: List<Account>
    )

    private var loadJob: Job? = null

    init {
        loadAnalytics()
    }

    fun setPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun setMonth(month: Int, year: Int) {
        _uiState.update { it.copy(selectedMonth = month, selectedYear = year) }
    }

    fun setDayOffset(offset: Int) {
        _uiState.update { it.copy(selectedDayOffset = offset) }
    }

    fun setWeekOffset(offset: Int) {
        _uiState.update { it.copy(selectedWeekOffset = offset) }
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
    }

    fun setDashboardTab(tab: Int) {
        _uiState.update { it.copy(dashboardTab = tab) }
    }

    fun setDashboardPeriod(period: String) {
        _uiState.update { it.copy(dashboardPeriod = period) }
    }

    fun setDashboardMonth(month: Int) {
        _uiState.update { it.copy(dashboardMonth = month) }
    }

    fun setDashboardYear(year: Int) {
        _uiState.update { it.copy(dashboardYear = year) }
    }

    fun setCompareMonthA(month: Int, year: Int) {
        _uiState.update { it.copy(compareMonthA = month, compareYearA = year) }
    }

    fun setCompareMonthB(month: Int, year: Int) {
        _uiState.update { it.copy(compareMonthB = month, compareYearB = year) }
    }

    fun navigatePrev() {
        _uiState.update { s ->
            when (s.selectedPeriod) {
                "DAY" -> if (s.selectedDayOffset < 6) s.copy(selectedDayOffset = s.selectedDayOffset + 1) else s
                "WEEK" -> if (s.selectedWeekOffset < 3) s.copy(selectedWeekOffset = s.selectedWeekOffset + 1) else s
                "MONTH" -> {
                    val newMonth = if (s.selectedMonth == 0) 11 else s.selectedMonth - 1
                    val newYear = if (s.selectedMonth == 0) s.selectedYear - 1 else s.selectedYear
                    s.copy(selectedMonth = newMonth, selectedYear = newYear)
                }
                "YEAR" -> {
                    val minYear = Calendar.getInstance().get(Calendar.YEAR) - 4
                    if (s.selectedYear > minYear) s.copy(selectedYear = s.selectedYear - 1) else s
                }
                else -> s
            }
        }
    }

    fun navigateNext() {
        _uiState.update { s ->
            when (s.selectedPeriod) {
                "DAY" -> if (s.selectedDayOffset > 0) s.copy(selectedDayOffset = s.selectedDayOffset - 1) else s
                "WEEK" -> if (s.selectedWeekOffset > 0) s.copy(selectedWeekOffset = s.selectedWeekOffset - 1) else s
                "MONTH" -> {
                    val now = Calendar.getInstance()
                    val maxMonth = now.get(Calendar.MONTH)
                    val maxYear = now.get(Calendar.YEAR)
                    if (s.selectedYear < maxYear || (s.selectedYear == maxYear && s.selectedMonth < maxMonth)) {
                        val newMonth = if (s.selectedMonth == 11) 0 else s.selectedMonth + 1
                        val newYear = if (s.selectedMonth == 11) s.selectedYear + 1 else s.selectedYear
                        s.copy(selectedMonth = newMonth, selectedYear = newYear)
                    } else s
                }
                "YEAR" -> {
                    val maxYear = Calendar.getInstance().get(Calendar.YEAR)
                    if (s.selectedYear < maxYear) s.copy(selectedYear = s.selectedYear + 1) else s
                }
                else -> s
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadAnalytics(showInitialLoading = false)
            delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadAnalytics(showInitialLoading: Boolean = true) {
        loadJob?.cancel()
        if (showInitialLoading && _uiState.value.transactions.isEmpty()) {
            _uiState.update { it.copy(isLoading = true) }
        }
        val filterFlow = _uiState
            .map {
                PeriodFilterParams(
                    selectedPeriod = it.selectedPeriod,
                    selectedDayOffset = it.selectedDayOffset,
                    selectedWeekOffset = it.selectedWeekOffset,
                    selectedMonth = it.selectedMonth,
                    selectedYear = it.selectedYear
                )
            }
            .distinctUntilChanged()

        val repoFlow = combine(
            transactionRepository.getAllTransactions(),
            categoryRepository.getAllCategories(),
            accountRepository.getAllAccounts(),
            incomeRepository.getAllIncomeSources(),
            savingRepository.getAllSavingGoals()
        ) { transactions, categories, accounts, incomeSources, savingGoals ->
             Quintuple(transactions, categories, accounts, incomeSources, savingGoals)
        }

        loadJob = viewModelScope.launch {
            try {
                combine(repoFlow, filterFlow) { (transactions, categories, accounts, incomeSources, savingGoals), filter ->
                    val filterStateSnapshot = AnalyticsUiState(
                        selectedPeriod = filter.selectedPeriod,
                        selectedDayOffset = filter.selectedDayOffset,
                        selectedWeekOffset = filter.selectedWeekOffset,
                        selectedMonth = filter.selectedMonth,
                        selectedYear = filter.selectedYear
                    )

                    val salarySource = incomeSources.firstOrNull { it.type == "SALARY" && it.isActive }
                    val hasSalary = salarySource != null
                    val salaryDay = salarySource?.dayOfMonth ?: 1
                    val salaryAmt = salarySource?.amount ?: 0.0

                    val periodTransactions = AnalyticsCalculator.filterByPeriod(transactions, filterStateSnapshot, hasSalary, salaryDay)
                    val expensesOnly = periodTransactions.filter { it.type == TransactionType.EXPENSE }
                    val totalExpensesSum = expensesOnly.sumOf { it.amount }

                    val shares = AnalyticsCalculator.buildCategoryShares(expensesOnly, categories, totalExpensesSum)
                    val realTrend = AnalyticsCalculator.buildCashFlowTrend(transactions)

                    val maxExpense = expensesOnly.maxByOrNull { it.amount }
                    val maxExpenseAmount = maxExpense?.amount ?: 0.0
                    val maxExpenseCategory = categories.firstOrNull { it.id == maxExpense?.categoryId }?.name ?: "لايوجد"

                    val incomeSum = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val hasIncome = incomeSum > 0.0
                    val rate = if (hasIncome) ((incomeSum - totalExpensesSum) / incomeSum).toFloat() else 0f

                    val limitTotal = categories.filter { it.type == CategoryType.EXPENSE && it.budgetLimit != null }.sumOf { it.budgetLimit ?: 0.0 }
                    val consumption = if (limitTotal > 0) (totalExpensesSum / limitTotal).toFloat() else 0f

                    val projection = AnalyticsCalculator.computeSpendProjection(filterStateSnapshot, totalExpensesSum, hasSalary, salaryDay, salaryAmt, limitTotal)
                    val wkStats = AnalyticsCalculator.computeWeekendWeekdayStats(expensesOnly, transactions, filterStateSnapshot, hasSalary, salaryDay)
                    val efStats = AnalyticsCalculator.computeEmergencyFundStats(accounts, savingGoals, transactions)

                    ComputedAnalyticsPayload(
                        spendingsByCategory = shares,
                        trendData = realTrend,
                        largestExpense = maxExpenseAmount,
                        largestExpenseName = maxExpenseCategory,
                        savingsRate = rate,
                        hasIncomeInPeriod = hasIncome,
                        periodIncomeSum = incomeSum,
                        periodExpensesSum = totalExpensesSum,
                        budgetConsumptionPercent = consumption.coerceIn(0f, 1f),
                        hasSalarySource = hasSalary,
                        salaryDayOfMonth = salaryDay,
                        salaryAmount = salaryAmt,
                        projection = projection,
                        wkStats = wkStats,
                        efStats = efStats,
                        savingGoals = savingGoals,
                        isDatabaseEmpty = transactions.isEmpty() && savingGoals.isEmpty(),
                        transactions = transactions,
                        periodTransactions = periodTransactions,
                        categories = categories,
                        accounts = accounts
                    )
                }
                    .flowOn(Dispatchers.Default)
                    .collect { payload ->
                        _uiState.update { current ->
                            current.copy(
                                spendingsByCategory = payload.spendingsByCategory,
                                trendData = payload.trendData,
                                largestExpense = payload.largestExpense,
                                largestExpenseName = payload.largestExpenseName,
                                savingsRate = payload.savingsRate,
                                hasIncomeInPeriod = payload.hasIncomeInPeriod,
                                periodIncomeSum = payload.periodIncomeSum,
                                periodExpensesSum = payload.periodExpensesSum,
                                budgetConsumptionPercent = payload.budgetConsumptionPercent,
                                isLoading = false,
                                hasSalarySource = payload.hasSalarySource,
                                salaryDayOfMonth = payload.salaryDayOfMonth,
                                salaryAmount = payload.salaryAmount,
                                salaryCycleStartLabel = payload.projection.cycleStartLabel,
                                salaryCycleEndLabel = payload.projection.cycleEndLabel,
                                daysRemainingInCycle = payload.projection.daysRemaining,
                                totalDaysInCycle = payload.projection.totalDays,
                                salaryCyclePercentageElapsed = payload.projection.cyclePercentageElapsed,
                                projectedEndMonthSpending = payload.projection.projectedSpend,
                                isProjectedToExceedBudget = payload.projection.isProjectedToExceed,
                                referenceBudget = payload.projection.refBudget,
                                weekendExpensesSum = payload.wkStats.weekendSum,
                                weekdayExpensesSum = payload.wkStats.weekdaySum,
                                weekendDailyAverage = payload.wkStats.weekendAvg,
                                weekdayDailyAverage = payload.wkStats.weekdayAvg,
                                weekendPercentage = payload.wkStats.weekendPct,
                                weekdayPercentage = payload.wkStats.weekdayPct,
                                hasWeekendData = payload.wkStats.hasData,
                                totalSavingsAmount = payload.efStats.totalSavings,
                                averageMonthlyExpense = payload.efStats.avgMonthlyExpense,
                                emergencyFundRunwayMonths = payload.efStats.runway,
                                emergencyFundStatus = payload.efStats.runwayStatus,
                                savingGoals = payload.savingGoals,
                                isDatabaseEmpty = payload.isDatabaseEmpty,
                                transactions = payload.transactions,
                                periodTransactions = payload.periodTransactions,
                                categories = payload.categories,
                                accounts = payload.accounts
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun exportPdfReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(exportingProgressText = "جاري تحضير وتشكيل تقرير الإحصائيات PDF باللغة العربية...", exportError = null, exportResult = null) }
            delay(1500)
            val request = ExportReportRequest(
                reportType = "ANALYTICS_REPORT",
                language = "AR",
                includeCharts = true,
                includeDebtSection = true,
                includeSavingsSection = true,
                fileName = "تقرير_إحصائيات_فنتراك_${System.currentTimeMillis()}"
            )
            try {
                val result = exportRepository.exportPdfReport(request)
                if (result.success) {
                    _uiState.update { it.copy(exportingProgressText = null, exportResult = result) }
                } else {
                    _uiState.update { it.copy(exportingProgressText = null, exportError = result.message ?: "فشل التصدير") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportingProgressText = null, exportError = e.localizedMessage) }
            }
        }
    }

    fun clearExportState() {
        _uiState.update { it.copy(exportResult = null, exportError = null, exportingProgressText = null) }
    }

    fun updateCategoryColor(categoryId: Long, hexColor: String) {
        if (categoryId == 0L) return
        viewModelScope.launch {
            try {
                categoryRepository.getCategoryById(categoryId)?.let { category ->
                    val updatedCategory = category.copy(color = hexColor)
                    categoryRepository.updateCategory(updatedCategory)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun moveCategory(categoryId: Long, newParentId: Long?) {
        if (categoryId == 0L) return
        viewModelScope.launch {
            try {
                categoryRepository.moveCategory(categoryId, newParentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}
