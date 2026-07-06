package com.qdash.presentation.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.UnifiedScreenHeader
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val grouped by viewModel.groupedNotifications.collectAsState()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (uiState.notifications.isNotEmpty()) {
                ClearAllBar(onClearAll = { viewModel.clearAll() })
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                UnifiedScreenHeader(
                    title = "مركز الإشعارات",
                    subtitle = "تابع تنبيهات ميزانيتك ومستجدات حساباتك أولاً بأول",
                    showBackButton = true,
                    onBackClick = onBack,
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.unreadCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = com.qdash.core.utils.FormatterUtils.convertNumerals("${uiState.unreadCount} جديدة"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                TextButton(onClick = { viewModel.markAllAsRead() }) {
                                    Text("قراءة الكل", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                )
                if (uiState.isLoading) {
                    FilterChipsRowSkeleton()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item {
                            SectionHeader(label = "تحميل الإشعارات...")
                        }
                        items(5) {
                            NotificationItemSkeleton()
                        }
                    }
                } else {
                    FilterRow(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )

                    if (uiState.notifications.isEmpty()) {
                        EmptyState()
                    } else {
                        NotificationList(
                            grouped = grouped,
                            onMarkAsRead = { viewModel.markAsRead(it.id) },
                            onDelete = { viewModel.deleteNotification(it) },
                            onTap = { notification ->
                                viewModel.markAsRead(notification.id)
                                notification.deepLinkRoute?.let { onNavigateTo(it) }
                            }
                        )
                    }
                }
            }
        }
    }
}
