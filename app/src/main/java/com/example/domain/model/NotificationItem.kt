package com.example.domain.model

import com.example.data.local.entities.NotificationEntity

enum class NotificationType {
    BUDGET_ALERT, SUBSCRIPTION_REMINDER, SAVINGS_MILESTONE,
    DEBT_DUE, BACKUP_DONE, EXPORT_DONE, SALARY_ADDED,
    TIP, OVERSPEND_FORECAST, SMART_REMINDER
}

data class AppNotification(
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val deepLinkRoute: String? = null,
    val relatedEntityId: Long? = null
)

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
