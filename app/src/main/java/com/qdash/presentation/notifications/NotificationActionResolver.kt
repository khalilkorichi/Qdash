package com.qdash.presentation.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.ui.graphics.vector.ImageVector
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType
import com.qdash.presentation.navigation.Screen

internal data class FilterChipInfo(
    val label: String,
    val types: Set<NotificationType>
)

internal data class NotificationActionSpec(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val isHighlighted: Boolean = false
)

// Covers all NotificationType enum values cleanly without hiding any category
internal val FILTER_CHIPS = listOf(
    FilterChipInfo("الكل", emptySet()),
    FilterChipInfo("التحديثات", setOf(NotificationType.APP_UPDATE)),
    FilterChipInfo("الميزانية", setOf(NotificationType.BUDGET_ALERT, NotificationType.OVERSPEND_FORECAST)),
    FilterChipInfo("الاشتراكات", setOf(NotificationType.SUBSCRIPTION_REMINDER)),
    FilterChipInfo("الراتب", setOf(NotificationType.SALARY_ADDED)),
    FilterChipInfo("الادخار", setOf(NotificationType.SAVINGS_MILESTONE)),
    FilterChipInfo("الديون", setOf(NotificationType.DEBT_DUE)),
    FilterChipInfo("التذكيرات", setOf(NotificationType.SMART_REMINDER)),
    FilterChipInfo("النسخ والتصدير", setOf(NotificationType.BACKUP_DONE, NotificationType.EXPORT_DONE)),
    FilterChipInfo("نصائح", setOf(NotificationType.TIP))
)

/**
 * Resolves the effective notification category, transparently upgrading legacy update
 * notifications previously stored under [NotificationType.TIP].
 */
internal fun AppNotification.effectiveType(): NotificationType {
    if (type == NotificationType.APP_UPDATE) return NotificationType.APP_UPDATE
    if (deepLinkRoute == Screen.Updates.route) return NotificationType.APP_UPDATE
    if (type == NotificationType.TIP && (title.contains("تحديث") || message.contains("إصدار جديد"))) {
        return NotificationType.APP_UPDATE
    }
    return type
}

internal fun notificationCategoryLabel(type: NotificationType): String = when (type) {
    NotificationType.APP_UPDATE -> "تحديث التطبيق"
    NotificationType.BUDGET_ALERT -> "الميزانية"
    NotificationType.OVERSPEND_FORECAST -> "توقع الإنفاق"
    NotificationType.SUBSCRIPTION_REMINDER -> "الاشتراكات"
    NotificationType.SAVINGS_MILESTONE -> "الادخار"
    NotificationType.DEBT_DUE -> "الديون"
    NotificationType.BACKUP_DONE -> "النسخ الاحتياطي"
    NotificationType.EXPORT_DONE -> "التقارير"
    NotificationType.SALARY_ADDED -> "الراتب"
    NotificationType.TIP -> "نصيحة"
    NotificationType.SMART_REMINDER -> "تذكير ذكي"
}

/**
 * Maps every notification type and context to its direct in-app action button and destination route.
 */
internal fun resolveNotificationAction(notification: AppNotification): NotificationActionSpec {
    val deepLink = notification.deepLinkRoute?.takeIf { it.isNotBlank() }
    return when (notification.effectiveType()) {
        NotificationType.APP_UPDATE -> NotificationActionSpec(
            label = "تحديث الآن",
            icon = Icons.Default.SystemUpdate,
            route = Screen.Updates.route,
            isHighlighted = true
        )
        NotificationType.BUDGET_ALERT -> if (deepLink == Screen.Salary.route) {
            NotificationActionSpec(
                label = "مراجعة الظرف",
                icon = Icons.Default.AccountBalanceWallet,
                route = Screen.Salary.route
            )
        } else {
            NotificationActionSpec(
                label = "مراجعة الميزانية",
                icon = Icons.Default.PieChart,
                route = deepLink ?: Screen.BudgetGoals.route
            )
        }
        NotificationType.OVERSPEND_FORECAST -> NotificationActionSpec(
            label = "عرض الميزانية",
            icon = Icons.Default.Insights,
            route = deepLink ?: Screen.BudgetGoals.route
        )
        NotificationType.SUBSCRIPTION_REMINDER -> NotificationActionSpec(
            label = "عرض الاشتراكات",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            route = deepLink ?: Screen.Subscriptions.route
        )
        NotificationType.DEBT_DUE -> {
            val isLent = notification.title.contains("سلفة") || notification.title.contains("استرداد")
            NotificationActionSpec(
                label = if (isLent) "متابعة السلفة" else "سداد الآن",
                icon = Icons.Default.Payments,
                route = deepLink ?: Screen.Debts.route
            )
        }
        NotificationType.SALARY_ADDED -> {
            val isAdded = notification.title.contains("تم إضافة")
            NotificationActionSpec(
                label = if (isAdded) "توزيع الراتب" else "إدارة الراتب",
                icon = Icons.Default.AccountBalanceWallet,
                route = deepLink ?: Screen.Salary.route
            )
        }
        NotificationType.SAVINGS_MILESTONE -> NotificationActionSpec(
            label = "عرض الادخار",
            icon = Icons.Default.Savings,
            route = deepLink ?: Screen.Savings.route
        )
        NotificationType.BACKUP_DONE -> NotificationActionSpec(
            label = "النسخ الاحتياطي",
            icon = Icons.Default.CloudDone,
            route = deepLink ?: Screen.Backup.route
        )
        NotificationType.EXPORT_DONE -> NotificationActionSpec(
            label = "عرض التقارير",
            icon = Icons.Default.PictureAsPdf,
            route = deepLink ?: Screen.Export.route
        )
        NotificationType.SMART_REMINDER -> if (deepLink == Screen.Salary.route) {
            NotificationActionSpec(
                label = "مراجعة الراتب",
                icon = Icons.Default.AccountBalanceWallet,
                route = Screen.Salary.route
            )
        } else {
            val target = if (deepLink == null || deepLink == Screen.Home.route) {
                Screen.AddTransaction.createRoute("EXPENSE")
            } else {
                deepLink
            }
            NotificationActionSpec(
                label = "تسجيل معاملة",
                icon = Icons.Default.AddCircleOutline,
                route = target
            )
        }
        NotificationType.TIP -> when {
            deepLink == Screen.AiChat.route -> NotificationActionSpec(
                label = "اسأل المساعد",
                icon = Icons.Default.AutoAwesome,
                route = Screen.AiChat.route
            )
            deepLink != null && deepLink != Screen.Home.route -> NotificationActionSpec(
                label = "عرض التفاصيل",
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                route = deepLink
            )
            else -> NotificationActionSpec(
                label = "الذهاب للرئيسية",
                icon = Icons.Default.Home,
                route = Screen.Home.route
            )
        }
    }
}

internal fun formatNotificationSubtitle(unreadCount: Int, totalCount: Int): String {
    val raw = when {
        unreadCount == 1 -> "إشعار واحد غير مقروء"
        unreadCount == 2 -> "إشعاران غير مقروءان"
        unreadCount in 3..10 -> "$unreadCount إشعارات غير مقروءة"
        unreadCount > 10 -> "$unreadCount إشعار غير مقروء"
        totalCount > 0 -> "جميع الإشعارات مقروءة"
        else -> "تابع تنبيهات ميزانيتك وحساباتك"
    }
    return FormatterUtils.convertNumerals(raw)
}
