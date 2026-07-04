package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_plans")
data class FinancialPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "MONTHLY_SPENDING", "EMERGENCY_FUND", "TRAVEL_SAVINGS", "DEBT_PAYOFF", "ROOM_SETUP", "FAMILY_BUDGET", "CUSTOM"
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val linkedAccountIds: String = "", // comma-separated account IDs
    val linkedCategoryIds: String = "", // comma-separated category IDs
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "PAUSED", "CANCELLED"
    val notes: String? = null,
    val color: String = "#6C63FF",
    val icon: String = "flag",
    val createdAt: Long = System.currentTimeMillis()
)
