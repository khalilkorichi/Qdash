package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_goals",
    indices = [Index(value = ["linkedCategoryId"])]
)
data class BudgetGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val linkedCategoryId: Long?,
    val budgetType: String, // "CATEGORY", "CUSTOM", "GLOBAL"
    val amountLimit: Double,
    val spentAmount: Double = 0.0,
    val startDate: Long,
    val endDate: Long,
    val alertThresholdPercent: Int = 80,
    val isActive: Boolean = true,
    val color: String,
    val icon: String,
    val createdAt: Long = System.currentTimeMillis()
)
