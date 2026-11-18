package com.qdash.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.designsystem.components.AppDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val grouped by viewModel.groupedNotifications.collectAsStateWithLifecycle()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                val subtitleText = if (uiState.unreadCount > 0) {
                    FormatterUtils.convertNumerals("${uiState.unreadCount} إشعارات غير مقروءة")
                } else {
                    "تابع تنبيهات ميزانيتك وحساباتك"
                }

                UnifiedScreenHeader(
                    title = "مركز الإشعارات",
                    subtitle = subtitleText,
                    showBackButton = true,
                    onBackClick = onBack,
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (uiState.unreadCount > 0) {
                                IconButton(
                                    onClick = { viewModel.markAllAsRead() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(primaryColor.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "قراءة الكل",
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (uiState.totalCount > 0) {
                                IconButton(
                                    onClick = { showClearAllDialog = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(errorColor.copy(alpha = 0.1f), CircleShape)
                                        .testTag("clear_all_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "مسح الكل",
                                        tint = errorColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                if (uiState.isLoading) {
                    FilterChipsRowSkeleton()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
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
                        selectedFilterLabel = uiState.selectedFilterLabel,
                        onFilterSelected = { chip ->
                            viewModel.setFilterGroup(chip.label, chip.types)
                        }
                    )

                    if (uiState.notifications.isEmpty()) {
                        EmptyState(isFiltered = uiState.totalCount > 0)
                    } else {
                        NotificationList(
                            grouped = grouped,
                            onMarkAsRead = { viewModel.markAsRead(it.id) },
                            onDelete = { deleted ->
                                viewModel.deleteNotification(deleted)
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = "تم حذف الإشعار",
                                        actionLabel = "تراجع",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreNotification(deleted)
                                    }
                                }
                            },
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

    if (showClearAllDialog) {
        AppDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = "مسح جميع الإشعارات",
            text = "هل أنت متأكد من حذف جميع الإشعارات؟ لا يمكن التراجع عن هذا الإجراء.",
            confirmButtonText = "مسح الكل",
            onConfirm = { viewModel.clearAll() },
            dismissButtonText = "إلغاء",
            onDismiss = { showClearAllDialog = false },
            isDestructive = true,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = errorColor
                )
            }
        )
    }
}
