package com.qdash.domain.model

data class DailyFinancialAggregate(
    val localDateTimestamp: Long,
    val totalExpense: Double,
    val totalIncome: Double,
    val transactionCount: Int,
    val netCashflow: Double,
    val activityScore: Double
)
