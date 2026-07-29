package com.qdash.presentation.notifications

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType
import com.qdash.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.TimeZone

@Immutable
data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val selectedFilter: NotificationType? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<NotificationType?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.cleanupOldNotifications(100)
        }
    }

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepository.getAllNotifications().distinctUntilChanged(),
        _selectedFilter,
        _isRefreshing
    ) { allNotifications, filter, isRefreshing ->
        val unreadCount = allNotifications.count { !it.isRead }
        val filtered = if (filter != null) {
            allNotifications.filter { it.type == filter }
        } else {
            allNotifications
        }
        NotificationsUiState(
            notifications = filtered,
            unreadCount = unreadCount,
            selectedFilter = filter,
            isLoading = false,
            isRefreshing = isRefreshing
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState(isLoading = true)
    )

    val groupedNotifications: StateFlow<Map<String, List<AppNotification>>> =
        uiState.map { state ->
            groupByDay(state.notifications)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    private fun groupByDay(notifications: List<AppNotification>): Map<String, List<AppNotification>> {
        if (notifications.isEmpty()) return emptyMap()

        val timeZone = TimeZone.getDefault()
        val nowMs = System.currentTimeMillis()
        val offsetMs = timeZone.getOffset(nowMs).toLong()

        val msPerDay = 86_400_000L
        val todayEpochDay = (nowMs + offsetMs) / msPerDay
        val yesterdayEpochDay = todayEpochDay - 1

        val result = LinkedHashMap<String, MutableList<AppNotification>>()

        for (notification in notifications) {
            val notificationEpochDay = (notification.timestamp + offsetMs) / msPerDay
            val label = when (notificationEpochDay) {
                todayEpochDay -> "اليوم"
                yesterdayEpochDay -> "أمس"
                else -> com.qdash.core.utils.FormatterUtils.formatShortDate(notification.timestamp)
            }
            result.getOrPut(label) { ArrayList() }.add(notification)
        }

        return result
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            notificationRepository.cleanupOldNotifications(100)
            kotlinx.coroutines.delay(400)
            _isRefreshing.value = false
        }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.markAllAsRead()
        }
    }

    fun deleteNotification(notification: AppNotification) {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.deleteNotification(notification)
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.clearAll()
        }
    }

    fun setFilter(type: NotificationType?) {
        _selectedFilter.value = type
    }
}
