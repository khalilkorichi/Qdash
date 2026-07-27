package com.qdash.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Calendar
import androidx.compose.runtime.Immutable

data class CategoryShare(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: String
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
    val budgetConsumptionPercent: Float = 0f,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedDayOffset: Int = 0, // 0 = today, 1 = yesterday, ...
    val selectedWeekOffset: Int = 0, // 0 = this week, 1 = last week, ...
    val selectedMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
    val selectedYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),

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

    // Emergency Fund Runway
    val totalSavingsAmount: Double = 0.0,
    val averageMonthlyExpense: Double = 0.0,
    val emergencyFundRunwayMonths: Float = 0f,
    val emergencyFundStatus: String = "SAFE", // "CRITICAL", "ACCEPTABLE", "SAFE", "EXCELLENT"

    // PDF Export status
    val exportingProgressText: String? = null,
    val exportResult: ExportResult? = null,
    val exportError: String? = null,
    val isDatabaseEmpty: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),

    // Dashboard & Comparison State
    val dashboardTab: Int = 0, // 0 = General Analytics, 1 = Dashboard & Comparison
    val dashboardPeriod: String = "MONTHLY", // "MONTHLY", "ANNUALLY"
    val dashboardMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
    val dashboardYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val compareMonthA: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
    val compareYearA: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val compareMonthB: Int = run {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, -1)
        cal.get(java.util.Calendar.MONTH)
    },
    val compareYearB: Int = run {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, -1)
        cal.get(java.util.Calendar.YEAR)
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

    init {
        loadAnalytics()
    }

    fun setPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadAnalytics()
    }

    fun setMonth(month: Int, year: Int) {
        _uiState.update { it.copy(selectedMonth = month, selectedYear = year) }
        loadAnalytics()
    }

    fun setDayOffset(offset: Int) {
        _uiState.update { it.copy(selectedDayOffset = offset) }
        loadAnalytics()
    }

    fun setWeekOffset(offset: Int) {
        _uiState.update { it.copy(selectedWeekOffset = offset) }
        loadAnalytics()
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
        loadAnalytics()
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
        val s = _uiState.value
        when (s.selectedPeriod) {
            "DAY" -> if (s.selectedDayOffset < 6) {
                _uiState.update { it.copy(selectedDayOffset = s.selectedDayOffset + 1) }
            }
            "WEEK" -> if (s.selectedWeekOffset < 3) {
                _uiState.update { it.copy(selectedWeekOffset = s.selectedWeekOffset + 1) }
            }
            "MONTH" -> {
                val newMonth = if (s.selectedMonth == 0) 11 else s.selectedMonth - 1
                val newYear = if (s.selectedMonth == 0) s.selectedYear - 1 else s.selectedYear
                _uiState.update { it.copy(selectedMonth = newMonth, selectedYear = newYear) }
            }
            "YEAR" -> {
                val minYear = Calendar.getInstance().get(Calendar.YEAR) - 4
                if (s.selectedYear > minYear) {
                    _uiState.update { it.copy(selectedYear = s.selectedYear - 1) }
                }
            }
        }
        loadAnalytics()
    }

    fun navigateNext() {
        val s = _uiState.value
        when (s.selectedPeriod) {
            "DAY" -> if (s.selectedDayOffset > 0) {
                _uiState.update { it.copy(selectedDayOffset = s.selectedDayOffset - 1) }
            }
            "WEEK" -> if (s.selectedWeekOffset > 0) {
                _uiState.update { it.copy(selectedWeekOffset = s.selectedWeekOffset - 1) }
            }
            "MONTH" -> {
                val now = Calendar.getInstance()
                val maxMonth = now.get(Calendar.MONTH)
                val maxYear = now.get(Calendar.YEAR)
                if (s.selectedYear < maxYear || (s.selectedYear == maxYear && s.selectedMonth < maxMonth)) {
                    val newMonth = if (s.selectedMonth == 11) 0 else s.selectedMonth + 1
                    val newYear = if (s.selectedMonth == 11) s.selectedYear + 1 else s.selectedYear
                    _uiState.update { it.copy(selectedMonth = newMonth, selectedYear = newYear) }
                }
            }
            "YEAR" -> {
                val maxYear = Calendar.getInstance().get(Calendar.YEAR)
                if (s.selectedYear < maxYear) {
                    _uiState.update { it.copy(selectedYear = s.selectedYear + 1) }
                }
            }
        }
        loadAnalytics()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadAnalytics()
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadAnalytics() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        loadJob = viewModelScope.launch {
            try {
                combine(
                    transactionRepository.getAllTransactions(),
                    categoryRepository.getAllCategories(),
                    accountRepository.getAllAccounts(),
                    incomeRepository.getAllIncomeSources(),
                    savingRepository.getAllSavingGoals()
                ) { transactions, categories, accounts, incomeSources, savingGoals ->
                    val state = _uiState.value

                    val salarySource = incomeSources.firstOrNull { it.type == "SALARY" && it.isActive }
                    val hasSalary = salarySource != null
                    val salaryDay = salarySource?.dayOfMonth ?: 1
                    val salaryAmt = salarySource?.amount ?: 0.0

                    val periodTransactions = AnalyticsCalculator.filterByPeriod(transactions, state, hasSalary, salaryDay)
                    val expensesOnly = periodTransactions.filter { it.type == TransactionType.EXPENSE }
                    val totalExpensesSum = expensesOnly.sumOf { it.amount }

                    val shares = AnalyticsCalculator.buildCategoryShares(expensesOnly, categories, totalExpensesSum)
                    val realTrend = AnalyticsCalculator.buildCashFlowTrend(transactions)

                    val maxExpense = expensesOnly.maxByOrNull { it.amount }
                    val maxExpenseAmount = maxExpense?.amount ?: 0.0
                    val maxExpenseCategory = categories.firstOrNull { it.id == maxExpense?.categoryId }?.name ?: "لايوجد"

                    val incomeSum = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val rate = if (incomeSum > 0) ((incomeSum - totalExpensesSum) / incomeSum).toFloat() else 0f

                    val limitTotal = categories.filter { it.type == CategoryType.EXPENSE && it.budgetLimit != null }.sumOf { it.budgetLimit ?: 0.0 }
                    val consumption = if (limitTotal > 0) (totalExpensesSum / limitTotal).toFloat() else 0f

                    val projection = AnalyticsCalculator.computeSpendProjection(state, totalExpensesSum, hasSalary, salaryDay, salaryAmt, limitTotal)
                    val wkStats = AnalyticsCalculator.computeWeekendWeekdayStats(expensesOnly, transactions, state, hasSalary, salaryDay)
                    val efStats = AnalyticsCalculator.computeEmergencyFundStats(accounts, savingGoals, transactions)

                    state.copy(
                        spendingsByCategory = shares,
                        trendData = realTrend,
                        largestExpense = maxExpenseAmount,
                        largestExpenseName = maxExpenseCategory,
                        savingsRate = rate,
                        budgetConsumptionPercent = consumption.coerceIn(0f, 1f),
                        isLoading = false,

                        hasSalarySource = hasSalary,
                        salaryDayOfMonth = salaryDay,
                        salaryAmount = salaryAmt,
                        salaryCycleStartLabel = projection.cycleStartLabel,
                        salaryCycleEndLabel = projection.cycleEndLabel,
                        daysRemainingInCycle = projection.daysRemaining,
                        totalDaysInCycle = projection.totalDays,
                        salaryCyclePercentageElapsed = projection.cyclePercentageElapsed,
                        projectedEndMonthSpending = projection.projectedSpend,
                        isProjectedToExceedBudget = projection.isProjectedToExceed,
                        referenceBudget = projection.refBudget,

                        weekendExpensesSum = wkStats.weekendSum,
                        weekdayExpensesSum = wkStats.weekdaySum,
                        weekendDailyAverage = wkStats.weekendAvg,
                        weekdayDailyAverage = wkStats.weekdayAvg,
                        weekendPercentage = wkStats.weekendPct,
                        weekdayPercentage = wkStats.weekdayPct,
                        hasWeekendData = wkStats.hasData,

                        totalSavingsAmount = efStats.totalSavings,
                        averageMonthlyExpense = efStats.avgMonthlyExpense,
                        emergencyFundRunwayMonths = efStats.runway,
                        emergencyFundStatus = efStats.runwayStatus,
                        isDatabaseEmpty = transactions.isEmpty(),
                        transactions = transactions,
                        categories = categories
                    )
                }
                .flowOn(kotlinx.coroutines.Dispatchers.Default)
                .collect { calculatedState ->
                    _uiState.value = calculatedState
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
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }
}

