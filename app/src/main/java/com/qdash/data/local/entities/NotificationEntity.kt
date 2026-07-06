package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isRead", "timestamp"]),
        Index(value = ["type", "timestamp"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // "BUDGET_ALERT", "SUBSCRIPTION_REMINDER", "SAVINGS_MILESTONE", "DEBT_DUE", "BACKUP_DONE", "EXPORT_DONE", "SALARY_ADDED", "TIP", "OVERSPEND_FORECAST"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val deepLinkRoute: String? = null, // e.g. "budget_goals", "savings", "debts"
    // NOTE: relatedEntityId is intentionally NOT a @ForeignKey — it is a polymorphic reference
    // that can point to budget goals, savings goals, debts, or subscriptions.
    // Room cannot enforce multi-table FK constraints; referential integrity is managed in code.
    // qdash-audit: suppress DB-005
    val relatedEntityId: Long? = null  // ID of related entity (budget, savings goal, etc.)
)
