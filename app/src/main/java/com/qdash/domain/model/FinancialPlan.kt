package com.qdash.domain.model

import com.qdash.data.local.entities.FinancialPlanEntity
import com.qdash.domain.model.common.*

enum class FinancialPlanType {
    MONTHLY_SPENDING, EMERGENCY_FUND, TRAVEL_SAVINGS,
    DEBT_PAYOFF, ROOM_SETUP, FAMILY_BUDGET, CUSTOM
}

enum class FinancialPlanStatus {
    ACTIVE, COMPLETED, PAUSED, CANCELLED
}

data class FinancialPlan(
    override val /* contract */ id: Long = 0,
    override val /* contract */ title: String,
    val type: FinancialPlanType,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val linkedAccountIds: List<Long> = emptyList(),
    val linkedCategoryIds: List<Long> = emptyList(),
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val status: FinancialPlanStatus = FinancialPlanStatus.ACTIVE,
    override val /* contract */ notes: String? = null,
    override val /* contract */ color: String = "#6C63FF",
    override val /* contract */ icon: String = "flag",
    override val /* contract */ createdAt: Long = System.currentTimeMillis()
) : Identifiable, Titled, NotesHolder, ColorTagged, IconTagged, Timestamped {
    val progressPercent: Float
        get() = if (targetAmount <= 0) 0f else (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)

    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val isCompleted: Boolean
        get() = currentAmount >= targetAmount
}

fun FinancialPlanEntity.toDomain() = FinancialPlan(
    id = id,
    title = title,
    type = FinancialPlanType.valueOf(type),
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    linkedAccountIds = if (linkedAccountIds.isBlank()) emptyList()
                       else linkedAccountIds.split(",").mapNotNull { it.toLongOrNull() },
    linkedCategoryIds = if (linkedCategoryIds.isBlank()) emptyList()
                        else linkedCategoryIds.split(",").mapNotNull { it.toLongOrNull() },
    startDate = startDate,
    endDate = endDate,
    status = FinancialPlanStatus.valueOf(status),
    notes = notes,
    color = color,
    icon = icon,
    createdAt = createdAt
)

fun FinancialPlan.toEntity() = FinancialPlanEntity(
    id = id,
    title = title,
    type = type.name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    linkedAccountIds = linkedAccountIds.joinToString(","),
    linkedCategoryIds = linkedCategoryIds.joinToString(","),
    startDate = startDate,
    endDate = endDate,
    status = status.name,
    notes = notes,
    color = color,
    icon = icon,
    createdAt = createdAt
)
