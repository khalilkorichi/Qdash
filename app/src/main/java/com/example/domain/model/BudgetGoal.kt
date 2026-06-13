package com.example.domain.model

import com.example.data.local.entities.BudgetGoalEntity

data class BudgetGoal(
    val id: Long = 0,
    val title: String,
    val linkedCategoryId: Long?,
    val budgetType: BudgetType,
    val amountLimit: Double,
    val spentAmount: Double = 0.0,
    val startDate: Long,
    val endDate: Long,
    val alertThresholdPercent: Int = 80,
    val isActive: Boolean = true,
    val color: String,
    val icon: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingAmount: Double
        get() = amountLimit - spentAmount

    val usagePercent: Double
        get() = if (amountLimit > 0) spentAmount / amountLimit else 0.0

    val status: BudgetStatus
        get() = when {
            usagePercent >= 1.0 -> BudgetStatus.EXCEEDED
            usagePercent >= (alertThresholdPercent.toDouble() / 100.0) -> BudgetStatus.CRITICAL
            usagePercent >= 0.5 -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }
}

enum class BudgetType {
    CATEGORY, CUSTOM, GLOBAL
}

enum class BudgetStatus {
    SAFE, WARNING, CRITICAL, EXCEEDED
}

// Mapper functions
fun BudgetGoalEntity.toDomain() = BudgetGoal(
    id = id,
    title = title,
    linkedCategoryId = linkedCategoryId,
    budgetType = BudgetType.valueOf(budgetType),
    amountLimit = amountLimit,
    spentAmount = spentAmount,
    startDate = startDate,
    endDate = endDate,
    alertThresholdPercent = alertThresholdPercent,
    isActive = isActive,
    color = color,
    icon = icon,
    createdAt = createdAt
)

fun BudgetGoal.toEntity() = BudgetGoalEntity(
    id = id,
    title = title,
    linkedCategoryId = linkedCategoryId,
    budgetType = budgetType.name,
    amountLimit = amountLimit,
    spentAmount = spentAmount,
    startDate = startDate,
    endDate = endDate,
    alertThresholdPercent = alertThresholdPercent,
    isActive = isActive,
    color = color,
    icon = icon,
    createdAt = createdAt
)
