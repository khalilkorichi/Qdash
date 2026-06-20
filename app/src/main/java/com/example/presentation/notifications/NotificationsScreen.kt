package com.example.presentation.notifications

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.example.domain.model.AppNotification
import com.example.domain.model.NotificationType
import com.example.core.ui.components.UnifiedScreenHeader
import com.example.ui.designsystem.components.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

// ---------------------------------------------------------------------------
// Theme colours (mapped to ColorTokens for modern brand identity compliance)
// ---------------------------------------------------------------------------
private val Primary = com.example.ui.designsystem.tokens.ColorTokens.Primary
private val IncomeGreen = com.example.ui.designsystem.tokens.ColorTokens.Success
private val ExpenseRed = com.example.ui.designsystem.tokens.ColorTokens.Danger
private val SavingsAmber = com.example.ui.designsystem.tokens.ColorTokens.Warning
private val TransferBlue = com.example.ui.designsystem.tokens.ColorTokens.Info
private val TextGray = com.example.ui.designsystem.tokens.ColorTokens.TextGray
private val BackgroundLight = com.example.ui.designsystem.tokens.ColorTokens.BackgroundLight

// ---------------------------------------------------------------------------
// Helper – notification type → colour & icon
// ---------------------------------------------------------------------------
private data class TypeVisual(val color: Color, val icon: ImageVector)

@Composable
private fun notificationVisual(type: NotificationType): TypeVisual {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background == com.example.ui.designsystem.tokens.ColorTokens.BackgroundDark
    
    val successColor = if (isDark) com.example.ui.designsystem.tokens.ColorTokens.SuccessDark else com.example.ui.designsystem.tokens.ColorTokens.Success
    val dangerColor = if (isDark) com.example.ui.designsystem.tokens.ColorTokens.DangerDark else com.example.ui.designsystem.tokens.ColorTokens.Danger
    val warningColor = if (isDark) com.example.ui.designsystem.tokens.ColorTokens.WarningDark else com.example.ui.designsystem.tokens.ColorTokens.Warning
    val infoColor = if (isDark) com.example.ui.designsystem.tokens.ColorTokens.InfoDark else com.example.ui.designsystem.tokens.ColorTokens.Info

    return when (type) {
        NotificationType.BUDGET_ALERT       -> TypeVisual(dangerColor,    Icons.Default.PieChart)
        NotificationType.SUBSCRIPTION_REMINDER -> TypeVisual(infoColor, Icons.Default.Receipt)
        NotificationType.SAVINGS_MILESTONE  -> TypeVisual(successColor,   Icons.Default.Savings)
        NotificationType.DEBT_DUE           -> TypeVisual(warningColor,  Icons.Default.CreditCard)
        NotificationType.BACKUP_DONE        -> TypeVisual(primaryColor,  Icons.Default.CloudDone)
        NotificationType.EXPORT_DONE        -> TypeVisual(primaryColor,  Icons.Default.PictureAsPdf)
        NotificationType.SALARY_ADDED       -> TypeVisual(successColor,   Icons.Default.TrendingUp)
        NotificationType.TIP                -> TypeVisual(warningColor,  Icons.Default.Lightbulb)
        NotificationType.OVERSPEND_FORECAST -> TypeVisual(dangerColor,    Icons.Default.Warning)
    }
}

private fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    val result = when {
        minutes < 1    -> "الآن"
        minutes < 60   -> "منذ $minutes دقيقة"
        hours < 24     -> "منذ $hours ساعة"
        days == 1L     -> "منذ يوم"
        else           -> "منذ $days أيام"
    }
    return com.example.core.utils.FormatterUtils.convertNumerals(result)
}

// ---------------------------------------------------------------------------
// Filter chip config
// ---------------------------------------------------------------------------
private data class FilterChipInfo(val label: String, val type: NotificationType?)

private val FILTER_CHIPS = listOf(
    FilterChipInfo("الكل",         null),
    FilterChipInfo("الميزانية",    NotificationType.BUDGET_ALERT),
    FilterChipInfo("الاشتراكات",   NotificationType.SUBSCRIPTION_REMINDER),
    FilterChipInfo("الادخار",      NotificationType.SAVINGS_MILESTONE),
    FilterChipInfo("الديون",       NotificationType.DEBT_DUE),
    FilterChipInfo("نصائح",        NotificationType.TIP)
)

// ===========================================================================
// Main composable
// ===========================================================================
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
                                    text = com.example.core.utils.FormatterUtils.convertNumerals("${uiState.unreadCount} جديدة"),
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
                // Filter chips
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
        } // end PullToRefreshBox
    }
}

// ---------------------------------------------------------------------------
// TopBar
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsTopBar(
    unreadCount: Int,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "مركز الإشعارات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unreadCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Primary
                    ) {
                        Text(
                            text = com.example.core.utils.FormatterUtils.convertNumerals(unreadCount.toString()),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (unreadCount > 0) {
                TextButton(onClick = onMarkAllRead) {
                    Text("قراءة الكل", color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.shadow(elevation = 2.dp)
    )
}

// ---------------------------------------------------------------------------
// Filter row
// ---------------------------------------------------------------------------
@Composable
private fun FilterRow(
    selectedFilter: NotificationType?,
    onFilterSelected: (NotificationType?) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("notification_filter_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FILTER_CHIPS) { chip ->
            val selected = chip.type == selectedFilter
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(chip.type) },
                label = {
                    Text(
                        chip.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = Primary,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Notifications list
// ---------------------------------------------------------------------------
@Composable
private fun NotificationList(
    grouped: Map<String, List<AppNotification>>,
    onMarkAsRead: (AppNotification) -> Unit,
    onDelete: (AppNotification) -> Unit,
    onTap: (AppNotification) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notifications_list"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        grouped.forEach { (sectionLabel, notifications) ->
            // Section header
            item(key = "header_$sectionLabel") {
                SectionHeader(label = sectionLabel)
            }
            // Notification items
            itemsIndexed(
                items = notifications,
                key = { _, n -> n.id }
            ) { index, notification ->
                AnimatedNotificationItem(
                    notification = notification,
                    index = index,
                    onMarkAsRead = onMarkAsRead,
                    onDelete = onDelete,
                    onTap = onTap
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------
@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

// ---------------------------------------------------------------------------
// Animated item wrapper
// ---------------------------------------------------------------------------
@Composable
private fun AnimatedNotificationItem(
    notification: AppNotification,
    index: Int,
    onMarkAsRead: (AppNotification) -> Unit,
    onDelete: (AppNotification) -> Unit,
    onTap: (AppNotification) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(notification.id) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(
                durationMillis = 350,
                delayMillis = (index * 40).coerceAtMost(300),
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 350,
                delayMillis = (index * 40).coerceAtMost(300)
            )
        )
    ) {
        SwipeableNotificationItem(
            notification = notification,
            onMarkAsRead = onMarkAsRead,
            onDelete = onDelete,
            onTap = onTap
        )
    }
}

// ---------------------------------------------------------------------------
// Swipeable item (manual swipe-to-delete)
// ---------------------------------------------------------------------------
@Composable
private fun SwipeableNotificationItem(
    notification: AppNotification,
    onMarkAsRead: (AppNotification) -> Unit,
    onDelete: (AppNotification) -> Unit,
    onTap: (AppNotification) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxOffset = -300f
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipe_offset"
    )
    val deleteThreshold = -200f
    val revealDelete = animatedOffsetX < -80f

    val deleteColor = MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (revealDelete) deleteColor.copy(alpha = 0.1f)
                else Color.Transparent
            )
    ) {
        // Delete background hint
        if (revealDelete) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = deleteColor,
                    modifier = Modifier.size(22.dp)
                )
                Text("حذف", color = deleteColor, fontWeight = FontWeight.Bold)
            }
        }

        // Foreground card
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(animatedOffsetX.toInt(), 0) }
                .pointerInput(notification.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < deleteThreshold) {
                                onDelete(notification)
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = (offsetX + dragAmount).coerceIn(maxOffset, 0f)
                            offsetX = newOffset
                        }
                    )
                }
        ) {
            NotificationItemCard(
                notification = notification,
                onMarkAsRead = onMarkAsRead,
                onTap = onTap
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Notification card
// ---------------------------------------------------------------------------
@Composable
private fun NotificationItemCard(
    notification: AppNotification,
    onMarkAsRead: (AppNotification) -> Unit,
    onTap: (AppNotification) -> Unit
) {
    val visual = notificationVisual(notification.type)
    val isDark = MaterialTheme.colorScheme.background == com.example.ui.designsystem.tokens.ColorTokens.BackgroundDark
    val alpha = if (isDark) 0.12f else 0.08f
    val bgColor = if (!notification.isRead) {
        visual.color.copy(alpha = alpha)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .then(
                if (!notification.isRead) {
                    Modifier.drawBehind {
                        // Left (RTL: leading) border indicator — 3 dp wide
                        drawRect(
                            color = visual.color,
                            size = androidx.compose.ui.geometry.Size(6f, size.height)
                        )
                    }
                } else Modifier
            )
            .clickable {
                if (!notification.isRead) onMarkAsRead(notification)
                onTap(notification)
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("notification_item_${notification.id}"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(visual.color.copy(alpha = 0.12f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.color,
                modifier = Modifier.size(22.dp)
            )
        }

        // Text block
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = notification.title,
                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = notification.message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Text(
                text = timeAgo(notification.timestamp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // Unread dot
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(visual.color, shape = CircleShape)
                    .align(Alignment.Top)
            )
        }
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------
@Composable
private fun EmptyState() {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notifications_empty"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "لا توجد إشعارات",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ستظهر هنا إشعاراتك حول الميزانية والاشتراكات وأكثر",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Bottom "مسح الكل" bar
// ---------------------------------------------------------------------------
@Composable
private fun ClearAllBar(onClearAll: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val deleteColor = MaterialTheme.colorScheme.error

    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(
                onClick = { showConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = deleteColor),
                border = BorderStroke(1.dp, deleteColor),
                modifier = Modifier.testTag("clear_all_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("مسح الكل", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("مسح الإشعارات", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف جميع الإشعارات؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onClearAll()
                }) {
                    Text("مسح", color = deleteColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun FilterChipsRowSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 32.dp)
                    .shimmerEffect(RoundedCornerShape(50.dp))
            )
        }
    }
}

@Composable
private fun NotificationItemSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon circle outline
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shimmerEffect(CircleShape)
            )
            // Text block outline
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(10.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(8.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}
