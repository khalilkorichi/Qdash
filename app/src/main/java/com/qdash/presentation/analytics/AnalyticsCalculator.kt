package com.qdash.presentation.analytics

import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.*
import java.util.Calendar

/**
 * Pure stateless calculator for analytics aggregations.
 * Extracted from AnalyticsViewModel to keep the ViewModel under the 500-line SIZE-001 threshold.
 */
object AnalyticsCalculator {

    val arabicMonths: Array<String> get() = FormatterUtils.getMonthNames()


    /** Filter transactions by the selected analytics period. */
    fun filterByPeriod(
        transactions: List<Transaction>,
        state: AnalyticsUiState,
        hasSalary: Boolean,
        salaryDay: Int
    ): List<Transaction> = when (state.selectedPeriod) {
        "YEAR" -> transactions.filter {
            val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
            txCal.get(Calendar.YEAR) == state.selectedYear
        }
        "WEEK" -> {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, -state.selectedWeekOffset)
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
                add(Calendar.DAY_OF_YEAR, -state.selectedDayOffset)
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
                val range = getSalaryCycleRangeForAnchor(salaryDay, state.selectedMonth, state.selectedYear)
                transactions.filter { it.date in range.first.timeInMillis..range.second.timeInMillis }
            } else {
                transactions.filter {
                    val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
                    txCal.get(Calendar.MONTH) == state.selectedMonth &&
                            txCal.get(Calendar.YEAR) == state.selectedYear
                }
            }
        }
        else -> transactions
    }

    /** Build category share breakdown for expense transactions. */
    fun buildCategoryShares(
        expensesOnly: List<Transaction>,
        categories: List<Category>,
        totalExpensesSum: Double
    ): List<CategoryShare> = expensesOnly
        .groupBy { it.categoryId }
        .map { (catId, txList) ->
            val cat = categories.firstOrNull { it.id == catId }
            val sum = txList.sumOf { it.amount }
            CategoryShare(
                categoryId = catId ?: 0L,
                categoryName = cat?.name ?: "أخرى",
                amount = sum,
                percentage = if (totalExpensesSum > 0) (sum / totalExpensesSum).toFloat() else 0f,
                color = cat?.color ?: "#6C63FF"
            )
        }
        .sortedByDescending { it.amount }

    /** Build last-5-months cash-flow trend data. */
    fun buildCashFlowTrend(transactions: List<Transaction>): List<CashFlowTrend> {
        val realTrend = mutableListOf<CashFlowTrend>()
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
            val monthStart = targetCal.timeInMillis
            val monthEnd = (targetCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            val monthTxs = transactions.filter { it.date in monthStart..monthEnd }
            val incomeSum = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expenseSum = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            realTrend.add(CashFlowTrend(monthLabel, incomeSum, expenseSum))
        }
        return realTrend
    }

    data class SpendProjection(
        val cycleStartLabel: String = "",
        val cycleEndLabel: String = "",
        val daysRemaining: Int = 0,
        val totalDays: Int = 30,
        val cyclePercentageElapsed: Float = 0f,
        val projectedSpend: Double = 0.0,
        val isProjectedToExceed: Boolean = false,
        val refBudget: Double = 0.0
    )

    /** Compute spend-projection and salary-cycle metadata (only for MONTH period). */
    fun computeSpendProjection(
        state: AnalyticsUiState,
        totalExpensesSum: Double,
        hasSalary: Boolean,
        salaryDay: Int,
        salaryAmt: Double,
        limitTotal: Double
    ): SpendProjection {
        if (state.selectedPeriod != "MONTH") return SpendProjection()

        val range = if (hasSalary) {
            getSalaryCycleRangeForAnchor(salaryDay, state.selectedMonth, state.selectedYear)
        } else {
            val start = Calendar.getInstance().apply {
                set(Calendar.YEAR, state.selectedYear)
                set(Calendar.MONTH, state.selectedMonth)
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

        val cycleStartLabel = FormatterUtils.formatShortDate(range.first.timeInMillis)
        val cycleEndLabel = FormatterUtils.formatShortDate(range.second.timeInMillis)

        val todayMillis = System.currentTimeMillis()
        val totalDays = ((endMillis - startMillis) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
        var daysRemaining = 0
        var cyclePercentageElapsed = 0f
        var projectedSpend = 0.0

        when {
            todayMillis in startMillis..endMillis -> {
                val elapsedMillis = todayMillis - startMillis
                cyclePercentageElapsed = (elapsedMillis.toFloat() / (endMillis - startMillis).toFloat()).coerceIn(0f, 1f)
                daysRemaining = ((endMillis - todayMillis) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                val elapsedDays = (elapsedMillis / (24 * 60 * 60 * 1000)).coerceAtLeast(1)
                projectedSpend = (totalExpensesSum / elapsedDays) * totalDays
            }
            todayMillis > endMillis -> {
                cyclePercentageElapsed = 1f
                daysRemaining = 0
                projectedSpend = totalExpensesSum
            }
            else -> {
                cyclePercentageElapsed = 0f
                daysRemaining = totalDays
                projectedSpend = 0.0
            }
        }

        val refBudget = if (limitTotal > 0) limitTotal else if (hasSalary) salaryAmt else 0.0
        val isProjectedToExceed = refBudget > 0.0 && projectedSpend > refBudget

        return SpendProjection(cycleStartLabel, cycleEndLabel, daysRemaining, totalDays,
            cyclePercentageElapsed, projectedSpend, isProjectedToExceed, refBudget)
    }

    data class WeekendWeekdayStats(
        val weekendSum: Double = 0.0,
        val weekdaySum: Double = 0.0,
        val weekendAvg: Double = 0.0,
        val weekdayAvg: Double = 0.0,
        val weekendPct: Float = 0f,
        val weekdayPct: Float = 0f,
        val hasData: Boolean = false
    )

    /** Compute weekend-vs-weekday spending stats. */
    fun computeWeekendWeekdayStats(
        expensesOnly: List<Transaction>,
        transactions: List<Transaction>,
        state: AnalyticsUiState,
        hasSalary: Boolean,
        salaryDay: Int
    ): WeekendWeekdayStats {
        if (state.selectedPeriod == "DAY" || expensesOnly.isEmpty()) return WeekendWeekdayStats()

        val cal = Calendar.getInstance()
        var weekendSum = 0.0
        var weekdaySum = 0.0
        expensesOnly.forEach { tx ->
            cal.timeInMillis = tx.date
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if (dow == Calendar.FRIDAY || dow == Calendar.SATURDAY) weekendSum += tx.amount
            else weekdaySum += tx.amount
        }

        val bounds = getStatsBounds(state, transactions, hasSalary, salaryDay)
        val (weekendCount, weekdayCount) = countWeekendDaysInRange(bounds.first, bounds.second)

        val weekendAvg = weekendSum / weekendCount
        val weekdayAvg = weekdaySum / weekdayCount
        val totalSpend = weekendSum + weekdaySum
        val weekendPct = if (totalSpend > 0) (weekendSum / totalSpend).toFloat() else 0f
        val weekdayPct = if (totalSpend > 0) (weekdaySum / totalSpend).toFloat() else 0f

        return WeekendWeekdayStats(weekendSum, weekdaySum, weekendAvg, weekdayAvg, weekendPct, weekdayPct, true)
    }

    data class EmergencyFundStats(
        val totalSavings: Double = 0.0,
        val avgMonthlyExpense: Double = 0.0,
        val runway: Float = 0f,
        val runwayStatus: String = "SAFE"
    )

    /** Compute emergency fund runway. */
    fun computeEmergencyFundStats(
        accounts: List<Account>,
        savingGoals: List<SavingGoal>,
        transactions: List<Transaction>
    ): EmergencyFundStats {
        val savingsAccsBalance = accounts.filter { it.type == AccountType.SAVINGS }.sumOf { it.balance }
        val savingsGoalsSum = savingGoals.sumOf { it.currentAmount }
        val totalSavings = savingsAccsBalance + savingsGoalsSum

        val threeMonthsAgoMillis = Calendar.getInstance().apply { add(Calendar.MONTH, -3); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        val historicalExpenses = transactions.filter { it.type == TransactionType.EXPENSE && it.date >= threeMonthsAgoMillis }
        val earliestTx = transactions.filter { it.type == TransactionType.EXPENSE }.minOfOrNull { it.date } ?: System.currentTimeMillis()
        val spanMillis = System.currentTimeMillis() - earliestTx
        val spanMonths = (spanMillis.toDouble() / (30.0 * 24 * 60 * 60 * 1000)).coerceIn(1.0, 3.0)
        val avgMonthlyExpense = historicalExpenses.sumOf { it.amount } / spanMonths

        val runway = if (avgMonthlyExpense > 0) (totalSavings / avgMonthlyExpense).toFloat() else 0f
        val runwayStatus = when {
            runway < 1.0f -> "CRITICAL"
            runway < 3.0f -> "ACCEPTABLE"
            runway < 6.0f -> "SAFE"
            else -> "EXCELLENT"
        }
        return EmergencyFundStats(totalSavings, avgMonthlyExpense, runway, runwayStatus)
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    fun getSalaryCycleRangeForAnchor(salaryDay: Int, anchorMonth: Int, anchorYear: Int): Pair<Calendar, Calendar> {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, anchorYear)
            set(Calendar.MONTH, anchorMonth)
            add(Calendar.MONTH, -1)
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

    private fun getStatsBounds(
        state: AnalyticsUiState,
        transactions: List<Transaction>,
        hasSalary: Boolean,
        salaryDay: Int
    ): Pair<Long, Long> = when (state.selectedPeriod) {
        "YEAR" -> {
            val s = Calendar.getInstance().apply { set(Calendar.YEAR, state.selectedYear); set(Calendar.DAY_OF_YEAR, 1) }
            val e = Calendar.getInstance().apply { set(Calendar.YEAR, state.selectedYear); set(Calendar.DAY_OF_YEAR, getActualMaximum(Calendar.DAY_OF_YEAR)) }
            Pair(s.timeInMillis, e.timeInMillis)
        }
        "WEEK" -> {
            val s = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -state.selectedWeekOffset); set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
            val e = s.clone() as Calendar; e.add(Calendar.DAY_OF_YEAR, 6)
            Pair(s.timeInMillis, e.timeInMillis)
        }
        "MONTH" -> {
            val range = if (hasSalary) {
                getSalaryCycleRangeForAnchor(salaryDay, state.selectedMonth, state.selectedYear)
            } else {
                val s = Calendar.getInstance().apply { set(Calendar.YEAR, state.selectedYear); set(Calendar.MONTH, state.selectedMonth); set(Calendar.DAY_OF_MONTH, 1) }
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

    private fun countWeekendDaysInRange(startMillis: Long, endMillis: Long): Pair<Int, Int> {
        var weekendCount = 0
        var weekdayCount = 0
        val cal = Calendar.getInstance()
        cal.timeInMillis = startMillis
        val maxEnd = endMillis.coerceAtMost(startMillis + 366L * 24 * 60 * 60 * 1000)
        while (cal.timeInMillis <= maxEnd) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) weekendCount++
            else weekdayCount++
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return Pair(weekendCount.coerceAtLeast(1), weekdayCount.coerceAtLeast(1))
    }
}
