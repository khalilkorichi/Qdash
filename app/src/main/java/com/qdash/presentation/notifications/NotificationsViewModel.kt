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
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val unreadCountByFilter: Map<String, Int> = emptyMap(),
    val selectedFilter: NotificationType? = null,
    val selectedFilterLabel: String = "الكل",
    val selectedFilterTypes: Set<NotificationType> = emptySet(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _selectedFilterGroup = MutableStateFlow<Pair<String, Set<NotificationType>>>("الكل" to emptySet())
    private val _isRefreshing = MutableStateFlow(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.cleanupOldNotifications(100)
        }
    }

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepository.getAllNotifications().distinctUntilChanged(),
        _selectedFilterGroup,
        _isRefreshing
    ) { allNotifications, filterGroup, isRefreshing ->
        val (filterLabel, filterTypes) = filterGroup
        val unreadCount = allNotifications.count { !it.isRead }
        val unreadByFilter = FILTER_CHIPS.associate { chip ->
            val count = if (chip.types.isEmpty()) {
                unreadCount
            } else {
                allNotifications.count { !it.isRead && it.effectiveType() in chip.types }
            }
            chip.label to count
        }
        val filtered = if (filterTypes.isNotEmpty()) {
            allNotifications.filter { it.effectiveType() in filterTypes }
        } else {
            allNotifications
        }
        NotificationsUiState(
            notifications = filtered,
            totalCount = allNotifications.size,
            unreadCount = unreadCount,
            unreadCountByFilter = unreadByFilter,
            selectedFilter = filterTypes.singleOrNull(),
            selectedFilterLabel = filterLabel,
            selectedFilterTypes = filterTypes,
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

    fun restoreNotification(notification: AppNotification) {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.insertNotification(notification)
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.clearAll()
        }
    }

    fun setFilter(type: NotificationType?) {
        if (type == null) {
            _selectedFilterGroup.value = "الكل" to emptySet()
        } else {
            val matchingChip = FILTER_CHIPS.firstOrNull { type in it.types }
            _selectedFilterGroup.value = (matchingChip?.label ?: type.name) to setOf(type)
        }
    }

    fun setFilterGroup(label: String, types: Set<NotificationType>) {
        _selectedFilterGroup.value = label to types
    }
}
