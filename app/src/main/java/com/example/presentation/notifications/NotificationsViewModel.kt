package com.example.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppNotification
import com.example.domain.model.NotificationType
import com.example.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepository.getAllNotifications(),
        notificationRepository.getUnreadCount(),
        _selectedFilter,
        _isRefreshing
    ) { allNotifications, unreadCount, filter, isRefreshing ->
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState(isLoading = true)
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }

    /**
     * Groups the current filtered notifications by day, using Arabic day labels.
     * Returns a map of section label → list of notifications in that section.
     */
    val groupedNotifications: StateFlow<Map<String, List<AppNotification>>> =
        uiState.map { state ->
            groupByDay(state.notifications)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    private fun groupByDay(notifications: List<AppNotification>): Map<String, List<AppNotification>> {
        val now = Calendar.getInstance()
        val today = calendarMidnight(now)
        val yesterday = calendarMidnight(now).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Use LinkedHashMap to preserve insertion order (most-recent sections first)
        val result = LinkedHashMap<String, MutableList<AppNotification>>()

        notifications
            .sortedByDescending { it.timestamp }
            .forEach { notification ->
                val cal = Calendar.getInstance().apply { timeInMillis = notification.timestamp }
                val midnight = calendarMidnight(cal)
                val label = when {
                    midnight.timeInMillis == today.timeInMillis -> "اليوم"
                    midnight.timeInMillis == yesterday.timeInMillis -> "أمس"
                    else -> com.example.core.utils.FormatterUtils.convertNumerals(sdf.format(Date(notification.timestamp)))
                }
                result.getOrPut(label) { mutableListOf() }.add(notification)
            }

        return result
    }

    private fun calendarMidnight(source: Calendar): Calendar {
        return (source.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun deleteNotification(notification: AppNotification) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notification)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationRepository.clearAll()
        }
    }

    fun setFilter(type: NotificationType?) {
        _selectedFilter.value = type
    }
}
