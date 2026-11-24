package com.qdash.presentation.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType
import com.qdash.ui.designsystem.components.shimmerEffect
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import kotlinx.coroutines.delay

internal data class TypeVisual(val color: Color, val icon: ImageVector)

private val EntranceEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
internal fun notificationVisual(type: NotificationType): TypeVisual {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background == ColorTokens.BackgroundDark

    val successColor = if (isDark) ColorTokens.SuccessDark else Color(0xFF16A34A)
    val dangerColor = if (isDark) ColorTokens.DangerDark else Color(0xFFDC2626)
    val warningColor = if (isDark) ColorTokens.WarningDark else Color(0xFFD97706)
    val infoColor = if (isDark) ColorTokens.InfoDark else Color(0xFF2563EB)

    return when (type) {
        NotificationType.APP_UPDATE -> TypeVisual(infoColor, Icons.Default.SystemUpdate)
        NotificationType.BUDGET_ALERT -> TypeVisual(dangerColor, Icons.Default.PieChart)
        NotificationType.SUBSCRIPTION_REMINDER -> TypeVisual(infoColor, Icons.Default.Receipt)
        NotificationType.SAVINGS_MILESTONE -> TypeVisual(successColor, Icons.Default.Savings)
        NotificationType.DEBT_DUE -> TypeVisual(warningColor, Icons.Default.CreditCard)
        NotificationType.BACKUP_DONE -> TypeVisual(primaryColor, Icons.Default.CloudDone)
        NotificationType.EXPORT_DONE -> TypeVisual(primaryColor, Icons.Default.PictureAsPdf)
        NotificationType.SALARY_ADDED -> TypeVisual(successColor, Icons.AutoMirrored.Filled.TrendingUp)
        NotificationType.TIP -> TypeVisual(warningColor, Icons.Default.Lightbulb)
        NotificationType.OVERSPEND_FORECAST -> TypeVisual(dangerColor, Icons.Default.Warning)
        NotificationType.SMART_REMINDER -> TypeVisual(primaryColor, Icons.Default.Schedule)
    }
}

internal fun timeAgo(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    val result = when {
        minutes < 1 -> "الآن"
        minutes == 1L -> "منذ دقيقة"
        minutes == 2L -> "منذ دقيقتين"
        minutes in 3..10 -> "منذ $minutes دقائق"
        minutes < 60 -> "منذ $minutes دقيقة"
        hours == 1L -> "منذ ساعة"
        hours == 2L -> "منذ ساعتين"
        hours in 3..10 -> "منذ $hours ساعات"
        hours < 24 -> "منذ $hours ساعة"
        days == 1L -> "منذ يوم"
        days == 2L -> "منذ يومين"
        days in 3..10 -> "منذ $days أيام"
        else -> "منذ $days يوماً"
    }
    return FormatterUtils.convertNumerals(result)
}

/**
 * Circular header/card action button built without Material3 IconButton's implicit 48dp
 * background expansion so sibling circles respect exact spacing and never overlap.
 */
@Composable
internal fun NotificationCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    iconSize: Dp = 17.dp
) {
    com.qdash.ui.designsystem.components.AppCircleIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        size = size,
        iconSize = iconSize,
        tint = tint,
        containerColor = containerColor,
        borderColor = borderColor
    )
}

@Composable
internal fun FilterRow(
    selectedFilterLabel: String,
    unreadCountByFilter: Map<String, Int>,
    onFilterSelected: (FilterChipInfo) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background == ColorTokens.BackgroundDark
    val surfaceColor = if (isDark) ColorTokens.CardDark else ColorTokens.SurfaceLight
    val defaultBorderColor = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notification_filter_row"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FILTER_CHIPS, key = { it.label }) { chip ->
                val selected = chip.label == selectedFilterLabel
                val unreadInChip = unreadCountByFilter[chip.label] ?: 0
                NotificationFilterPill(
                    label = chip.label,
                    selected = selected,
                    showUnreadDot = !selected && unreadInChip > 0 && chip.label != "الكل",
                    primaryColor = primaryColor,
                    unselectedSurface = surfaceColor,
                    unselectedBorder = defaultBorderColor,
                    onClick = { onFilterSelected(chip) }
                )
            }
        }
        HorizontalDivider(
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun NotificationFilterPill(
    label: String,
    selected: Boolean,
    showUnreadDot: Boolean,
    primaryColor: Color,
    unselectedSurface: Color,
    unselectedBorder: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = MotionTokens.springResponsive(),
        label = "filter_pill_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) primaryColor else unselectedSurface,
        animationSpec = MotionTokens.tweenShort(),
        label = "filter_pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) primaryColor else unselectedBorder,
        animationSpec = MotionTokens.tweenShort(),
        label = "filter_pill_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MotionTokens.tweenShort(),
        label = "filter_pill_text"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .heightIn(min = 34.dp)
            .clip(ShapeTokens.Full)
            .background(bgColor)
            .border(1.dp, borderColor, ShapeTokens.Full)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .semantics { role = Role.Tab }
            .testTag("filter_chip_$label"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showUnreadDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(primaryColor, CircleShape)
            )
        }
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
internal fun NotificationList(
    grouped: Map<String, List<AppNotification>>,
    onMarkAsRead: (AppNotification) -> Unit,
    onDelete: (AppNotification) -> Unit,
    onActionClick: (AppNotification, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notifications_list"),
        contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var sectionOffset = 0
        grouped.forEach { (sectionLabel, notifications) ->
            val currentOffset = sectionOffset
            sectionOffset += notifications.size

            item(key = "header_$sectionLabel", contentType = "section_header") {
                SectionHeader(
                    label = sectionLabel,
                    count = notifications.size,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(220),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        fadeOutSpec = tween(160)
                    )
                )
            }
            itemsIndexed(
                items = notifications,
                key = { _, n -> n.id },
                contentType = { _, _ -> "notification_card" }
            ) { localIndex, notification ->
                val globalIndex = currentOffset + localIndex
                SwipeableNotificationItem(
                    notification = notification,
                    staggerIndex = globalIndex,
                    onMarkAsRead = onMarkAsRead,
                    onDelete = onDelete,
                    onActionClick = onActionClick,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(260),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        fadeOutSpec = tween(200)
                    )
                )
            }
        }
    }
}

@Composable
internal fun SectionHeader(
    label: String,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == ColorTokens.BackgroundDark
    val pillBg = if (isDark) ColorTokens.CardDark else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val borderColor = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (count != null && count > 0) {
            Box(
                modifier = Modifier
                    .clip(ShapeTokens.Full)
                    .background(pillBg)
                    .border(1.dp, borderColor, ShapeTokens.Full)
                    .padding(horizontal = 9.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = FormatterUtils.convertNumerals(count.toString()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableNotificationItem(
    notification: AppNotification,
    staggerIndex: Int,
    onMarkAsRead: (AppNotification) -> Unit,
    onDelete: (AppNotification) -> Unit,
    onActionClick: (AppNotification, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shouldStagger = staggerIndex in 0..7
    val enterProgress = remember(notification.id) {
        Animatable(if (shouldStagger) 0f else 1f)
    }
    LaunchedEffect(notification.id) {
        if (shouldStagger && enterProgress.value < 1f) {
            delay((staggerIndex * 32L).coerceAtMost(192L))
            enterProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = EntranceEasing
                )
            )
        }
    }

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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                alpha = enterProgress.value
                translationY = (1f - enterProgress.value) * 26f
            },
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val errorColor = MaterialTheme.colorScheme.error
                val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(ShapeTokens.Xl)
                        .background(errorColor.copy(alpha = 0.14f))
                        .border(1.dp, errorColor.copy(alpha = 0.30f), ShapeTokens.Xl)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = errorColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "حذف",
                            color = errorColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        content = {
            NotificationItemCard(
                notification = notification,
                onMarkAsRead = onMarkAsRead,
                onDelete = onDelete,
                onActionClick = onActionClick
            )
        }
    )
}

@Composable
internal fun NotificationItemCard(
    notification: AppNotification,
    onMarkAsRead: (AppNotification) -> Unit,
    onDelete: (AppNotification) -> Unit,
    onActionClick: (AppNotification, String) -> Unit
) {
    val effectiveType = remember(notification) { notification.effectiveType() }
    val actionSpec = remember(notification) { resolveNotificationAction(notification) }
    val visual = notificationVisual(effectiveType)
    val categoryLabel = remember(effectiveType) { notificationCategoryLabel(effectiveType) }
    val isDark = MaterialTheme.colorScheme.background == ColorTokens.BackgroundDark
    val errorColor = MaterialTheme.colorScheme.error
    val successColor = if (isDark) ColorTokens.SuccessDark else Color(0xFF16A34A)

    val baseSurface = if (isDark) ColorTokens.CardDark else ColorTokens.SurfaceLight
    val defaultBorder = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight

    // Composite unread tint onto opaque base surface so SwipeToDismissBox background never bleeds through
    val targetBg = if (!notification.isRead) {
        visual.color.copy(alpha = if (isDark) 0.11f else 0.045f).compositeOver(baseSurface)
    } else {
        baseSurface
    }
    val targetBorder = if (!notification.isRead) {
        visual.color.copy(alpha = if (isDark) 0.36f else 0.24f)
    } else {
        defaultBorder
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = MotionTokens.tweenMedium(),
        label = "notif_card_bg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = MotionTokens.tweenMedium(),
        label = "notif_card_border"
    )

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.985f else 1f,
        animationSpec = MotionTokens.springResponsive(),
        label = "notif_card_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(ShapeTokens.Xl)
            .clickable(
                interactionSource = cardInteractionSource,
                indication = ripple(bounded = true),
                onClick = { onActionClick(notification, actionSpec.route) }
            )
            .testTag("notification_item_${notification.id}"),
        shape = ShapeTokens.Xl,
        color = animatedBg,
        border = BorderStroke(1.dp, animatedBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // ── Header: Leading Icon Circle + Category Badge / Time / Title + Unread Dot ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(visual.color.copy(alpha = if (isDark) 0.16f else 0.10f), CircleShape)
                        .border(1.dp, visual.color.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(ShapeTokens.Full)
                                .background(visual.color.copy(alpha = if (isDark) 0.18f else 0.10f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = categoryLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = visual.color,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                        Text(
                            text = timeAgo(notification.timestamp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            maxLines = 1
                        )
                    }

                    Text(
                        text = notification.title,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedVisibility(
                    visible = !notification.isRead,
                    enter = fadeIn(MotionTokens.tweenShort()) + scaleIn(initialScale = 0.6f),
                    exit = fadeOut(MotionTokens.tweenShort()) + scaleOut(targetScale = 0.6f)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(16.dp)
                            .background(visual.color.copy(alpha = 0.18f), CircleShape)
                            .testTag("unread_dot_${notification.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(visual.color, CircleShape)
                        )
                    }
                }
            }

            // ── Body Message ──────────────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notification.message,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // ── Action Footer Bar ─────────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 0.8.dp,
                color = if (isDark) ColorTokens.BorderDark.copy(alpha = 0.85f) else ColorTokens.BorderLight
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotificationPrimaryActionButton(
                    actionSpec = actionSpec,
                    accentColor = visual.color,
                    isDark = isDark,
                    onClick = { onActionClick(notification, actionSpec.route) },
                    modifier = Modifier.testTag("notification_action_${notification.id}")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = !notification.isRead,
                        enter = fadeIn(MotionTokens.tweenShort()) +
                            scaleIn(initialScale = 0.8f) +
                            expandHorizontally(expandFrom = Alignment.CenterHorizontally),
                        exit = fadeOut(MotionTokens.tweenShort()) +
                            scaleOut(targetScale = 0.8f) +
                            shrinkHorizontally(shrinkTowards = Alignment.CenterHorizontally)
                    ) {
                        NotificationCircleIconButton(
                            icon = Icons.Default.Done,
                            contentDescription = "تعليم كمقروء",
                            tint = successColor,
                            containerColor = successColor.copy(alpha = if (isDark) 0.14f else 0.10f),
                            borderColor = successColor.copy(alpha = if (isDark) 0.30f else 0.24f),
                            onClick = { onMarkAsRead(notification) },
                            size = 34.dp,
                            iconSize = 17.dp,
                            modifier = Modifier.testTag("mark_read_${notification.id}")
                        )
                    }

                    NotificationCircleIconButton(
                        icon = Icons.Default.DeleteOutline,
                        contentDescription = "حذف الإشعار",
                        tint = errorColor,
                        containerColor = errorColor.copy(alpha = if (isDark) 0.13f else 0.08f),
                        borderColor = errorColor.copy(alpha = if (isDark) 0.28f else 0.20f),
                        onClick = { onDelete(notification) },
                        size = 34.dp,
                        iconSize = 17.dp,
                        modifier = Modifier.testTag("delete_notification_${notification.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPrimaryActionButton(
    actionSpec: NotificationActionSpec,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = MotionTokens.springResponsive(),
        label = "action_pill_scale"
    )

    val containerColor = if (actionSpec.isHighlighted) {
        accentColor
    } else {
        accentColor.copy(alpha = if (isDark) 0.16f else 0.10f)
    }
    val contentColor = if (actionSpec.isHighlighted) {
        if (isDark) ColorTokens.BackgroundDark else Color.White
    } else {
        accentColor
    }
    val borderColor = if (actionSpec.isHighlighted) {
        accentColor
    } else {
        accentColor.copy(alpha = if (isDark) 0.32f else 0.24f)
    }

    Row(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 34.dp)
            .clip(ShapeTokens.Full)
            .background(containerColor)
            .border(1.dp, borderColor, ShapeTokens.Full)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 7.dp)
            .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = actionSpec.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = actionSpec.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun EmptyState(
    isFiltered: Boolean = false,
    onResetFilter: (() -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background == ColorTokens.BackgroundDark

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag("notifications_empty"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(primaryColor.copy(alpha = if (isDark) 0.16f else 0.08f), CircleShape)
                .border(1.dp, primaryColor.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFiltered) Icons.Default.FilterListOff else Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (isFiltered) "لا توجد إشعارات في هذا التصنيف" else "لا توجد إشعارات",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isFiltered) {
                "جرّب اختيار تصنيف آخر لعرض الإشعارات السابقة"
            } else {
                "ستظهر هنا تنبيهات الميزانية والتحديثات والاشتراكات وأكثر"
            },
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (isFiltered && onResetFilter != null) {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .clip(ShapeTokens.Full)
                    .background(primaryColor)
                    .clickable(onClick = onResetFilter)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
                    .semantics { role = Role.Button }
                    .testTag("reset_filter_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عرض جميع الإشعارات",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
internal fun FilterChipsRowSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 34.dp)
                    .shimmerEffect(ShapeTokens.Full)
            )
        }
    }
}

@Composable
internal fun NotificationItemSkeleton(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background == ColorTokens.BackgroundDark
    val surfaceColor = if (isDark) ColorTokens.CardDark else ColorTokens.SurfaceLight
    val borderColor = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = ShapeTokens.Xl,
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shimmerEffect(CircleShape)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(105.dp)
                            .height(14.dp)
                            .shimmerEffect(ShapeTokens.Full)
                    )
                    Box(
                        modifier = Modifier
                            .width(170.dp)
                            .height(14.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(11.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 104.dp, height = 34.dp)
                        .shimmerEffect(ShapeTokens.Full)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .shimmerEffect(CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .shimmerEffect(CircleShape)
                    )
                }
            }
        }
    }
}
