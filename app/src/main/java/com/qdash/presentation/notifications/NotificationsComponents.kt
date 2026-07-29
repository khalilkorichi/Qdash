package com.qdash.presentation.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType
import com.qdash.ui.designsystem.components.shimmerEffect

internal data class TypeVisual(val color: Color, val icon: ImageVector)

internal data class FilterChipInfo(val label: String, val type: NotificationType?)

internal val FILTER_CHIPS = listOf(
    FilterChipInfo("الكل",         null),
    FilterChipInfo("الميزانية",    NotificationType.BUDGET_ALERT),
    FilterChipInfo("الاشتراكات",   NotificationType.SUBSCRIPTION_REMINDER),
    FilterChipInfo("الادخار",      NotificationType.SAVINGS_MILESTONE),
    FilterChipInfo("الديون",       NotificationType.DEBT_DUE),
    FilterChipInfo("التذكيرات",     NotificationType.SMART_REMINDER),
    FilterChipInfo("نصائح",        NotificationType.TIP)
)

@Composable
internal fun notificationVisual(type: NotificationType): TypeVisual {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background == com.qdash.ui.designsystem.tokens.ColorTokens.BackgroundDark
    
    val successColor = if (isDark) com.qdash.ui.designsystem.tokens.ColorTokens.SuccessDark else com.qdash.ui.designsystem.tokens.ColorTokens.Success
    val dangerColor = if (isDark) com.qdash.ui.designsystem.tokens.ColorTokens.DangerDark else com.qdash.ui.designsystem.tokens.ColorTokens.Danger
    val warningColor = if (isDark) com.qdash.ui.designsystem.tokens.ColorTokens.WarningDark else com.qdash.ui.designsystem.tokens.ColorTokens.Warning
    val infoColor = if (isDark) com.qdash.ui.designsystem.tokens.ColorTokens.InfoDark else com.qdash.ui.designsystem.tokens.ColorTokens.Info

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
        NotificationType.SMART_REMINDER     -> TypeVisual(primaryColor,   Icons.Default.Schedule)
    }
}

internal fun timeAgo(timestamp: Long): String {
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
    return com.qdash.core.utils.FormatterUtils.convertNumerals(result)
}

@Composable
internal fun FilterRow(
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
        items(FILTER_CHIPS, key = { it.label }) { chip ->
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

@Composable
internal fun NotificationList(
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
            item(key = "header_$sectionLabel") {
                SectionHeader(label = sectionLabel)
            }
            items(
                items = notifications,
                key = { n -> n.id }
            ) { notification ->
                SwipeableNotificationItem(
                    notification = notification,
                    onMarkAsRead = onMarkAsRead,
                    onDelete = onDelete,
                    onTap = onTap
                )
            }
        }
    }
}

@Composable
internal fun SectionHeader(label: String) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableNotificationItem(
    notification: AppNotification,
    onMarkAsRead: (AppNotification) -> Unit,
    onDelete: (AppNotification) -> Unit,
    onTap: (AppNotification) -> Unit
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                currentOnDelete(notification)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = MaterialTheme.colorScheme.error
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("حذف", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(Icons.Default.Delete, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
        },
        content = {
            NotificationItemCard(
                notification = notification,
                onMarkAsRead = onMarkAsRead,
                onTap = onTap
            )
        }
    )
}

@Composable
internal fun NotificationItemCard(
    notification: AppNotification,
    onMarkAsRead: (AppNotification) -> Unit,
    onTap: (AppNotification) -> Unit
) {
    val visual = notificationVisual(notification.type)
    val isDark = MaterialTheme.colorScheme.background == com.qdash.ui.designsystem.tokens.ColorTokens.BackgroundDark
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

@Composable
internal fun EmptyState() {
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

@Composable
internal fun ClearAllBar(onClearAll: () -> Unit) {
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
internal fun FilterChipsRowSkeleton(modifier: Modifier = Modifier) {
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
internal fun NotificationItemSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shimmerEffect(CircleShape)
            )
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
