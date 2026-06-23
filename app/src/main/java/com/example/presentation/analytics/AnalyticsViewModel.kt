package com.example.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Calendar

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

    private fun loadAnalytics() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                combine(
                    transactionRepository.getAllTransactions(),
                    categoryRepository.getAllCategories(),
                    accountRepository.getAllAccounts(),
                    incomeRepository.getAllIncomeSources(),
                    savingRepository.getAllSavingGoals()
                ) { transactions, categories, accounts, incomeSources, savingGoals ->
                    // 1. Identify Salary Cycle setup
                    val salarySource = incomeSources.firstOrNull { it.type == "SALARY" && it.isActive }
                    val hasSalary = salarySource != null
                    val salaryDay = salarySource?.dayOfMonth ?: 1
                    val salaryAmt = salarySource?.amount ?: 0.0

                    // Compute start/end date for selected period (custom salary month if MONTH is selected)
                    val periodTransactions = when (_uiState.value.selectedPeriod) {
                        "YEAR" -> transactions.filter {
                            val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
                            txCal.get(Calendar.YEAR) == _uiState.value.selectedYear
                        }
                        "WEEK" -> {
                            val targetCal = Calendar.getInstance().apply {
                                add(Calendar.WEEK_OF_YEAR, -_uiState.value.selectedWeekOffset)
                                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val startOfWeek = targetCal.timeInMillis
                            val endOfWeek = startOfWeek + (7L * 24 * 60 * 60 * 1000)
                            transactions.filter { it.date in startOfWeek until endOfWeek }
                        }
                        "DAY" -> {
                            val targetCal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, -_uiState.value.selectedDayOffset)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val startOfDay = targetCal.timeInMillis
                            val endOfDay = startOfDay + (24L * 60 * 60 * 1000)
                            transactions.filter { it.date in startOfDay until endOfDay }
                        }
                        "MONTH" -> {
                            if (hasSalary) {
                                val range = getSalaryCycleRangeForAnchor(salaryDay, _uiState.value.selectedMonth, _uiState.value.selectedYear)
                                transactions.filter { it.date in range.first.timeInMillis..range.second.timeInMillis }
                            } else {
                                transactions.filter {
                                    val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
                                    txCal.get(Calendar.MONTH) == _uiState.value.selectedMonth &&
                                             txCal.get(Calendar.YEAR) == _uiState.value.selectedYear
                                }
                            }
                        }
                        else -> { // ALL / No filter
                            transactions
                        }
                    }

                    // Expenses
                    val expensesOnly = periodTransactions.filter { it.kind == TransactionKind.EXPENSE || it.kind == TransactionKind.SAVINGS_WITHDRAWAL }
                    val totalExpensesSum = expensesOnly.sumOf { it.amount }

                    // Category shares
                    val shares = expensesOnly.groupBy { it.categoryId }.map { (catId, txList) ->
                        val cat = categories.firstOrNull { it.id == catId }
                        val sum = txList.sumOf { it.amount }
                        CategoryShare(
                            categoryId = catId ?: 0L,
                            categoryName = cat?.name ?: "أخرى",
                            amount = sum,
                            percentage = if (totalExpensesSum > 0) (sum / totalExpensesSum).toFloat() else 0f,
                            color = cat?.color ?: "#6C63FF"
                        )
                    }.sortedByDescending { it.amount }

                    // Income vs Expense trend calculations (historical)
                    val realTrend = mutableListOf<CashFlowTrend>()
                    val arabicMonths = arrayOf(
                        "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
                        "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
                    )

                    // Compute trend for the last 5 months — always using full calendar months
                    // to avoid partial-month bias when the current day != 1
                    val todayCal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    for (monthOffset in 4 downTo 0) {
                        val targetCal = (todayCal.clone() as Calendar).apply {
                            add(Calendar.MONTH, -monthOffset)
                        }
                        val yr = targetCal.get(Calendar.YEAR)
                        val mth = targetCal.get(Calendar.MONTH)
                        val monthLabel = "${arabicMonths[mth]} ${yr % 100}"

                        val monthTxs = if (hasSalary) {
                            val range = getSalaryCycleRangeForAnchor(salaryDay, mth, yr)
                            transactions.filter { it.date in range.first.timeInMillis..range.second.timeInMillis }
                        } else {
                            // Use full month: from 00:00:00 of day 1 to 23:59:59 of last day
                            val monthStart = targetCal.timeInMillis
                            val monthEnd = (targetCal.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis
                            transactions.filter { it.date in monthStart..monthEnd }
                        }

                        val incomeSum = monthTxs.filter { it.kind == TransactionKind.INCOME || it.kind == TransactionKind.SALARY }.sumOf { it.amount }
                        val expenseSum = monthTxs.filter { it.kind == TransactionKind.EXPENSE || it.kind == TransactionKind.SAVINGS_WITHDRAWAL }.sumOf { it.amount }
                        realTrend.add(CashFlowTrend(monthLabel, incomeSum, expenseSum))
                    }

                    // Largest Expense
                    val maxExpense = expensesOnly.maxByOrNull { it.amount }
                    val maxExpenseAmount = maxExpense?.amount ?: 0.0
                    val maxExpenseCategory = categories.firstOrNull { it.id == maxExpense?.categoryId }?.name ?: "لايوجد"

                    // Savings rate
                    val incomeSum = periodTransactions.filter { it.kind == TransactionKind.INCOME || it.kind == TransactionKind.SALARY }.sumOf { it.amount }
                    val rate = if (incomeSum > 0) ((incomeSum - totalExpensesSum) / incomeSum).toFloat() else 0f

                    // Global budget limits
                    val limitTotal = categories.filter { it.type == CategoryType.EXPENSE && it.budgetLimit != null }.sumOf { it.budgetLimit ?: 0.0 }
                    val consumption = if (limitTotal > 0) (totalExpensesSum / limitTotal).toFloat() else 0f

                    // Spend Projection & Salary Cycle
                    var cycleStartLabel = ""
                    var cycleEndLabel = ""
                    var daysRemaining = 0
                    var totalDays = 30
                    var cyclePercentageElapsed = 0f
                    var projectedSpend = 0.0
                    var isProjectedToExceed = false
                    var refBudget = 0.0

                    if (_uiState.value.selectedPeriod == "MONTH") {
                        val range = if (hasSalary) {
                            getSalaryCycleRangeForAnchor(salaryDay, _uiState.value.selectedMonth, _uiState.value.selectedYear)
                        } else {
                            val start = Calendar.getInstance().apply {
                                set(Calendar.YEAR, _uiState.value.selectedYear)
                                set(Calendar.MONTH, _uiState.value.selectedMonth)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val end = start.clone() as Calendar
                            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                            end.set(Calendar.HOUR_OF_DAY, 23)
                            end.set(Calendar.MINUTE, 59)
                            end.set(Calendar.SECOND, 59)
                            Pair(start, end)
                        }

                        val startMillis = range.first.timeInMillis
                        val endMillis = range.second.timeInMillis

                        val dayFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale("ar"))
                        cycleStartLabel = dayFormat.format(range.first.time)
                        cycleEndLabel = dayFormat.format(range.second.time)

                        val todayMillis = System.currentTimeMillis()
                        totalDays = ((endMillis - startMillis) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)

                        if (todayMillis in startMillis..endMillis) {
                            val elapsedMillis = todayMillis - startMillis
                            cyclePercentageElapsed = (elapsedMillis.toFloat() / (endMillis - startMillis).toFloat()).coerceIn(0f, 1f)
                            daysRemaining = ((endMillis - todayMillis) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)

                            val elapsedDays = (elapsedMillis / (24 * 60 * 60 * 1000)).coerceAtLeast(1)
                            projectedSpend = (totalExpensesSum / elapsedDays) * totalDays
                        } else if (todayMillis > endMillis) {
                            cyclePercentageElapsed = 1f
                            daysRemaining = 0
                            projectedSpend = totalExpensesSum
                        } else {
                            cyclePercentageElapsed = 0f
                            daysRemaining = totalDays
                            projectedSpend = 0.0
                        }

                        refBudget = if (limitTotal > 0) limitTotal else if (hasSalary) salaryAmt else 0.0
                        isProjectedToExceed = refBudget > 0.0 && projectedSpend > refBudget
                    }

                    // Weekend vs Weekday Spending
                    var weekendSum = 0.0
                    var weekdaySum = 0.0
                    var weekendAvg = 0.0
                    var weekdayAvg = 0.0
                    var weekendPct = 0f
                    var weekdayPct = 0f
                    var hasWkData = false

                    if (_uiState.value.selectedPeriod != "DAY" && expensesOnly.isNotEmpty()) {
                        val cal = Calendar.getInstance()
                        expensesOnly.forEach { tx ->
                            cal.timeInMillis = tx.date
                            val dow = cal.get(Calendar.DAY_OF_WEEK)
                            if (dow == Calendar.FRIDAY || dow == Calendar.SATURDAY) {
                                weekendSum += tx.amount
                            } else {
                                weekdaySum += tx.amount
                            }
                        }

                        val bounds = when (_uiState.value.selectedPeriod) {
                            "YEAR" -> {
                                val s = Calendar.getInstance().apply { set(Calendar.YEAR, _uiState.value.selectedYear); set(Calendar.DAY_OF_YEAR, 1) }
                                val e = Calendar.getInstance().apply { set(Calendar.YEAR, _uiState.value.selectedYear); set(Calendar.DAY_OF_YEAR, getActualMaximum(Calendar.DAY_OF_YEAR)) }
                                Pair(s.timeInMillis, e.timeInMillis)
                            }
                            "WEEK" -> {
                                val s = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -_uiState.value.selectedWeekOffset); set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
                                val e = s.clone() as Calendar; e.add(Calendar.DAY_OF_YEAR, 6)
                                Pair(s.timeInMillis, e.timeInMillis)
                            }
                            "MONTH" -> {
                                val range = if (hasSalary) {
                                    getSalaryCycleRangeForAnchor(salaryDay, _uiState.value.selectedMonth, _uiState.value.selectedYear)
                                } else {
                                    val s = Calendar.getInstance().apply { set(Calendar.YEAR, _uiState.value.selectedYear); set(Calendar.MONTH, _uiState.value.selectedMonth); set(Calendar.DAY_OF_MONTH, 1) }
                                    val e = s.clone() as Calendar; e.set(Calendar.DAY_OF_MONTH, e.getActualMaximum(Calendar.DAY_OF_MONTH))
                                    Pair(s, e)
                                }
                                Pair(range.first.timeInMillis, range.second.timeInMillis)
                            }
                            else -> {
                                val minTx = transactions.minOfOrNull { it.date } ?: System.currentTimeMillis()
                                val maxTx = transactions.maxOfOrNull { it.date } ?: System.currentTimeMillis()
                                Pair(minTx, maxTx)
                            }
                        }

                        val weekendWeekdayCounts = countWeekendDaysInRange(bounds.first, bounds.second)
                        val weekendCount = weekendWeekdayCounts.first
                        val weekdayCount = weekendWeekdayCounts.second

                        weekendAvg = weekendSum / weekendCount
                        weekdayAvg = weekdaySum / weekdayCount
                        val totalSpend = weekendSum + weekdaySum
                        if (totalSpend > 0) {
                            weekendPct = (weekendSum / totalSpend).toFloat()
                            weekdayPct = (weekdaySum / totalSpend).toFloat()
                        }
                        hasWkData = true
                    }

                    // Emergency Fund Runway
                    val savingsAccsBalance = accounts.filter { it.type == AccountType.SAVINGS }.sumOf { it.balance }
                    val savingsGoalsSum = savingGoals.sumOf { it.currentAmount }
                    val totalSavings = savingsAccsBalance + savingsGoalsSum

                    val threeMonthsAgoMillis = Calendar.getInstance().apply { add(Calendar.MONTH, -3); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
                    val historicalExpenses = transactions.filter { (it.kind == TransactionKind.EXPENSE || it.kind == TransactionKind.SAVINGS_WITHDRAWAL) && it.date >= threeMonthsAgoMillis }
                    val earliestTx = transactions.filter { it.kind == TransactionKind.EXPENSE || it.kind == TransactionKind.SAVINGS_WITHDRAWAL }.minOfOrNull { it.date } ?: System.currentTimeMillis()
                    val spanMillis = System.currentTimeMillis() - earliestTx
                    val spanMonths = (spanMillis.toDouble() / (30.0 * 24 * 60 * 60 * 1000)).coerceIn(1.0, 3.0)
                    val totalHistExpenses = historicalExpenses.sumOf { it.amount }
                    val avgMonthlyExpense = totalHistExpenses / spanMonths

                    val runway = if (avgMonthlyExpense > 0) (totalSavings / avgMonthlyExpense).toFloat() else 0f
                    val runwayStatus = when {
                        runway < 1.0f -> "CRITICAL"
                        runway < 3.0f -> "ACCEPTABLE"
                        runway < 6.0f -> "SAFE"
                        else -> "EXCELLENT"
                    }

                    AnalyticsUiState(
                        selectedPeriod = _uiState.value.selectedPeriod,
                        spendingsByCategory = shares,
                        trendData = realTrend,
                        largestExpense = maxExpenseAmount,
                        largestExpenseName = maxExpenseCategory,
                        savingsRate = rate,
                        budgetConsumptionPercent = consumption.coerceIn(0f, 1f),
                        selectedDayOffset = _uiState.value.selectedDayOffset,
                        selectedWeekOffset = _uiState.value.selectedWeekOffset,
                        selectedMonth = _uiState.value.selectedMonth,
                        selectedYear = _uiState.value.selectedYear,
                        isLoading = false,

                        hasSalarySource = hasSalary,
                        salaryDayOfMonth = salaryDay,
                        salaryAmount = salaryAmt,
                        salaryCycleStartLabel = cycleStartLabel,
                        salaryCycleEndLabel = cycleEndLabel,
                        daysRemainingInCycle = daysRemaining,
                        totalDaysInCycle = totalDays,
                        salaryCyclePercentageElapsed = cyclePercentageElapsed,
                        projectedEndMonthSpending = projectedSpend,
                        isProjectedToExceedBudget = isProjectedToExceed,
                        referenceBudget = refBudget,

                        weekendExpensesSum = weekendSum,
                        weekdayExpensesSum = weekdaySum,
                        weekendDailyAverage = weekendAvg,
                        weekdayDailyAverage = weekdayAvg,
                        weekendPercentage = weekendPct,
                        weekdayPercentage = weekdayPct,
                        hasWeekendData = hasWkData,

                        totalSavingsAmount = totalSavings,
                        averageMonthlyExpense = avgMonthlyExpense,
                        emergencyFundRunwayMonths = runway,
                        emergencyFundStatus = runwayStatus,
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

    private fun getSalaryCycleRangeForAnchor(salaryDay: Int, anchorMonth: Int, anchorYear: Int): Pair<Calendar, Calendar> {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, anchorYear)
            set(Calendar.MONTH, anchorMonth)
            add(Calendar.MONTH, -1) // starts in previous month
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, salaryDay.coerceAtMost(maxDay))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.MONTH, 1)
        val nextMonthMax = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        endCal.set(Calendar.DAY_OF_MONTH, salaryDay.coerceAtMost(nextMonthMax))
        endCal.add(Calendar.MILLISECOND, -1)
        return Pair(startCal, endCal)
    }

    private fun countWeekendDaysInRange(startMillis: Long, endMillis: Long): Pair<Int, Int> {
        var weekendCount = 0
        var weekdayCount = 0
        val cal = Calendar.getInstance()
        cal.timeInMillis = startMillis
        val maxEnd = endMillis.coerceAtMost(startMillis + 366L * 24 * 60 * 60 * 1000)
        while (cal.timeInMillis <= maxEnd) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
                weekendCount++
            } else {
                weekdayCount++
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return Pair(weekendCount.coerceAtLeast(1), weekdayCount.coerceAtLeast(1))
    }

    private fun sumOfExpenses(txs: List<Transaction>): Double {
        return txs.filter { it.kind == TransactionKind.EXPENSE || it.kind == TransactionKind.SAVINGS_WITHDRAWAL }.sumOf { it.amount }
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

