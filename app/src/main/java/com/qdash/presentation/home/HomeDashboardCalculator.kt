package com.qdash.presentation.home

import com.qdash.domain.model.CategoryType
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionKind
import com.qdash.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pure stateless helper for dashboard metric calculations.
 * Extracted from HomeViewModel to keep it under the SIZE-001 500-line threshold.
 * All functions are pure — no coroutines, no repositories, no side effects.
 */
object HomeDashboardCalculator {

    data class MonthlyStats(
        val monthlyIncome: Double,
        val monthlyExpense: Double,
        val incomeChangePercent: Double,
        val expenseChangePercent: Double,
        val budgetUsagePercent: Float
    )

    /**
     * Computes monthly income/expense totals, MoM change %, and budget consumption ratio.
     */
    fun computeMonthlyStats(
        transactions: List<Transaction>,
        categories: List<com.qdash.domain.model.Category>
    ): MonthlyStats {
        val currentMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val currentMonthEnd = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val prevMonthStart = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val prevMonthEnd = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        var monthlyIn = 0.0; var monthlyOut = 0.0
        var prevIn = 0.0;    var prevOut = 0.0

        transactions.forEach { tx ->
            val d = tx.date
            when {
                d in currentMonthStart..currentMonthEnd -> {
                    if (tx.kind == TransactionKind.INCOME || tx.kind == TransactionKind.SALARY) monthlyIn += tx.amount
                    else if (tx.kind == TransactionKind.EXPENSE || tx.kind == TransactionKind.SAVINGS_WITHDRAWAL) monthlyOut += tx.amount
                }
                d in prevMonthStart..prevMonthEnd -> {
                    if (tx.kind == TransactionKind.INCOME || tx.kind == TransactionKind.SALARY) prevIn += tx.amount
                    else if (tx.kind == TransactionKind.EXPENSE || tx.kind == TransactionKind.SAVINGS_WITHDRAWAL) prevOut += tx.amount
                }
            }
        }

        val incomeChange = if (prevIn > 0.0) ((monthlyIn - prevIn) / prevIn) * 100.0 else if (monthlyIn > 0.0) 100.0 else 0.0
        val expenseChange = if (prevOut > 0.0) ((monthlyOut - prevOut) / prevOut) * 100.0 else if (monthlyOut > 0.0) 100.0 else 0.0

        val expenseCatsWithLimits = categories.filter { it.type == CategoryType.EXPENSE && it.budgetLimit != null }
        var totalLimit = 0.0; var totalSpent = 0.0
        if (expenseCatsWithLimits.isNotEmpty()) {
            val ids = expenseCatsWithLimits.map { it.id }.toSet()
            totalLimit = expenseCatsWithLimits.sumOf { it.budgetLimit ?: 0.0 }
            transactions.forEach { tx ->
                if (tx.type == TransactionType.EXPENSE && tx.categoryId in ids && tx.date in currentMonthStart..currentMonthEnd) {
                    totalSpent += tx.amount
                }
            }
        }

        return MonthlyStats(
            monthlyIncome = monthlyIn,
            monthlyExpense = monthlyOut,
            incomeChangePercent = incomeChange,
            expenseChangePercent = expenseChange,
            budgetUsagePercent = if (totalLimit > 0) (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f) else 0f
        )
    }

    /**
     * Builds expense trend data points for the selected period.
     */
    fun computeTrendPoints(
        transactions: List<Transaction>,
        period: String
    ): List<ExpenseTrendPoint> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val points = mutableListOf<ExpenseTrendPoint>()

        when (period) {
            "WEEK" -> {
                val fmt = SimpleDateFormat("EEE", Locale("ar"))
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val oneDayMs = 24 * 60 * 60 * 1000L
                for (i in 6 downTo 0) {
                    val s = todayStart - i * oneDayMs; val e = s + oneDayMs - 1
                    points.add(ExpenseTrendPoint(fmt.format(java.util.Date(s)), expenses.filter { it.date in s..e }.sumOf { it.amount }))
                }
            }
            "MONTH" -> {
                val fmt = SimpleDateFormat("dd", Locale("ar"))
                val nowMs = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L
                for (i in 29 downTo 0 step 5) {
                    val target = nowMs - i * oneDayMs; val start = target - 5 * oneDayMs
                    points.add(ExpenseTrendPoint(fmt.format(java.util.Date(target)), expenses.filter { it.date in start..target }.sumOf { it.amount }))
                }
            }
            "YEAR" -> {
                val fmt = SimpleDateFormat("MMM", Locale("ar"))
                val baseCal = Calendar.getInstance()
                for (i in 11 downTo 0) {
                    val c = Calendar.getInstance().apply { timeInMillis = baseCal.timeInMillis; add(Calendar.MONTH, -i) }
                    val s = (c.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    val e = (c.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
                    points.add(ExpenseTrendPoint(fmt.format(c.time), expenses.filter { it.date in s..e }.sumOf { it.amount }))
                }
            }
            else -> { // DAY
                val fmt = SimpleDateFormat("HH:mm", Locale("ar"))
                val nowMs = System.currentTimeMillis()
                val fourHoursMs = 4L * 60 * 60 * 1000L
                for (i in 5 downTo 0) {
                    val blockEnd = nowMs - i * fourHoursMs; val blockStart = blockEnd - fourHoursMs
                    points.add(ExpenseTrendPoint(fmt.format(java.util.Date(blockEnd)), expenses.filter { it.date in blockStart..blockEnd }.sumOf { it.amount }))
                }
            }
        }
        return points
    }
}
