package com.qdash.presentation.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens
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
    val isDark = MaterialTheme.colorScheme.background == ColorTokens.BackgroundDark
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
                val subtitleText = formatNotificationSubtitle(
                    unreadCount = uiState.unreadCount,
                    totalCount = uiState.totalCount
                )

                UnifiedScreenHeader(
                    title = "مركز الإشعارات",
                    subtitle = subtitleText,
                    showBackButton = true,
                    onBackClick = onBack,
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimatedVisibility(
                                visible = uiState.unreadCount > 0,
                                enter = fadeIn(MotionTokens.tweenShort()) + scaleIn(initialScale = 0.85f),
                                exit = fadeOut(MotionTokens.tweenShort()) + scaleOut(targetScale = 0.85f)
                            ) {
                                NotificationCircleIconButton(
                                    icon = Icons.Default.DoneAll,
                                    contentDescription = "قراءة الكل",
                                    tint = primaryColor,
                                    containerColor = primaryColor.copy(alpha = if (isDark) 0.16f else 0.09f),
                                    borderColor = primaryColor.copy(alpha = if (isDark) 0.30f else 0.18f),
                                    onClick = { viewModel.markAllAsRead() },
                                    size = 36.dp,
                                    iconSize = 18.dp,
                                    modifier = Modifier.testTag("mark_all_read_button")
                                )
                            }
                            AnimatedVisibility(
                                visible = uiState.totalCount > 0,
                                enter = fadeIn(MotionTokens.tweenShort()) + scaleIn(initialScale = 0.85f),
                                exit = fadeOut(MotionTokens.tweenShort()) + scaleOut(targetScale = 0.85f)
                            ) {
                                NotificationCircleIconButton(
                                    icon = Icons.Default.DeleteOutline,
                                    contentDescription = "مسح الكل",
                                    tint = errorColor,
                                    containerColor = errorColor.copy(alpha = if (isDark) 0.15f else 0.08f),
                                    borderColor = errorColor.copy(alpha = if (isDark) 0.30f else 0.20f),
                                    onClick = { showClearAllDialog = true },
                                    size = 36.dp,
                                    iconSize = 18.dp,
                                    modifier = Modifier.testTag("clear_all_button")
                                )
                            }
                        }
                    }
                )

                AnimatedContent(
                    targetState = uiState.isLoading,
                    transitionSpec = {
                        fadeIn(MotionTokens.tweenMedium()).togetherWith(fadeOut(MotionTokens.tweenShort()))
                    },
                    label = "notifications_loading_transition"
                ) { loading ->
                    if (loading) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            FilterChipsRowSkeleton()
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    SectionHeader(label = "تحميل الإشعارات...")
                                }
                                items(5) {
                                    NotificationItemSkeleton()
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            FilterRow(
                                selectedFilterLabel = uiState.selectedFilterLabel,
                                unreadCountByFilter = uiState.unreadCountByFilter,
                                onFilterSelected = { chip ->
                                    viewModel.setFilterGroup(chip.label, chip.types)
                                }
                            )

                            AnimatedContent(
                                targetState = uiState.notifications.isEmpty(),
                                transitionSpec = {
                                    (fadeIn(MotionTokens.tweenMedium()) + scaleIn(initialScale = 0.97f))
                                        .togetherWith(fadeOut(MotionTokens.tweenShort()))
                                },
                                label = "notifications_empty_transition"
                            ) { isEmpty ->
                                if (isEmpty) {
                                    EmptyState(
                                        isFiltered = uiState.totalCount > 0,
                                        onResetFilter = if (uiState.totalCount > 0) {
                                            { viewModel.setFilterGroup("الكل", emptySet()) }
                                        } else {
                                            null
                                        }
                                    )
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
                                        onActionClick = { notification, route ->
                                            if (!notification.isRead) {
                                                viewModel.markAsRead(notification.id)
                                            }
                                            onNavigateTo(route)
                                        }
                                    )
                                }
                            }
                        }
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
            onConfirm = {
                viewModel.clearAll()
                showClearAllDialog = false
            },
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
