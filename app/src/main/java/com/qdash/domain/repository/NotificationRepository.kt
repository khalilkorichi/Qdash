package com.qdash.domain.repository

import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<AppNotification>>
    fun getUnreadNotifications(): Flow<List<AppNotification>>
    fun getUnreadCount(): Flow<Int>
    fun getNotificationsByType(type: NotificationType): Flow<List<AppNotification>>
    suspend fun insertNotification(notification: AppNotification): Long
    suspend fun markAsRead(id: Long)
    suspend fun markAllAsRead()
    suspend fun deleteNotification(notification: AppNotification)
    suspend fun clearAll()
}
