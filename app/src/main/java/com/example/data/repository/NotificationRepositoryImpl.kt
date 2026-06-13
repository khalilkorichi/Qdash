package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.NotificationDao
import com.example.domain.model.AppNotification
import com.example.domain.model.NotificationType
import com.example.domain.model.toDomain
import com.example.domain.model.toEntity
import com.example.domain.repository.NotificationRepository
import com.example.core.utils.SystemNotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
    private val context: Context
) : NotificationRepository {

    override fun getAllNotifications(): Flow<List<AppNotification>> {
        return notificationDao.getAllNotifications().map { list -> list.map { it.toDomain() } }
    }

    override fun getUnreadNotifications(): Flow<List<AppNotification>> {
        return notificationDao.getUnreadNotifications().map { list -> list.map { it.toDomain() } }
    }

    override fun getUnreadCount(): Flow<Int> {
        return notificationDao.getUnreadCount()
    }

    override fun getNotificationsByType(type: NotificationType): Flow<List<AppNotification>> {
        return notificationDao.getNotificationsByType(type.name).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertNotification(notification: AppNotification): Long {
        val id = notificationDao.insertNotification(notification.toEntity())
        SystemNotificationHelper.showNotification(context, notification.copy(id = id))
        return id
    }

    override suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    override suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    override suspend fun deleteNotification(notification: AppNotification) {
        notificationDao.deleteNotification(notification.toEntity())
    }

    override suspend fun clearAll() {
        notificationDao.clearAll()
    }
}
