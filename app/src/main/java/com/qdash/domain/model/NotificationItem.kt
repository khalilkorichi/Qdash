package com.qdash.domain.model

import com.qdash.data.local.entities.NotificationEntity
import com.qdash.domain.model.common.Identifiable

enum class NotificationType {
    BUDGET_ALERT, SUBSCRIPTION_REMINDER, SAVINGS_MILESTONE,
    DEBT_DUE, BACKUP_DONE, EXPORT_DONE, SALARY_ADDED,
    TIP, OVERSPEND_FORECAST, SMART_REMINDER
}

data class AppNotification(
    override val /* contract */ id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val deepLinkRoute: String? = null,
    val relatedEntityId: Long? = null
) : Identifiable

fun NotificationEntity.toDomain() = AppNotification(
    id = id,
    title = title,
    message = message,
    type = NotificationType.valueOf(type),
    isRead = isRead,
    timestamp = timestamp,
    deepLinkRoute = deepLinkRoute,
    relatedEntityId = relatedEntityId
)

fun AppNotification.toEntity() = NotificationEntity(
    id = id,
    title = title,
    message = message,
    type = type.name,
    isRead = isRead,
    timestamp = timestamp,
    deepLinkRoute = deepLinkRoute,
    relatedEntityId = relatedEntityId
)
