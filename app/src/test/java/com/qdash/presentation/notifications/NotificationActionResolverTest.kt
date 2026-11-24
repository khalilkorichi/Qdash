package com.qdash.presentation.notifications

import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType
import com.qdash.presentation.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationActionResolverTest {

    @Test
    fun effectiveTypeUpgradesLegacyUpdateNotifications() {
        val legacyUpdate = AppNotification(
            id = 1L,
            title = "تحديث جديد متوفر! 🎉",
            message = "إصدار جديد من التطبيق (1.0.0.115) متوفر الآن للتحميل.",
            type = NotificationType.TIP,
            deepLinkRoute = null
        )
        val deepLinkedUpdate = AppNotification(
            id = 2L,
            title = "إصدار متاح",
            message = "قم بالتحديث",
            type = NotificationType.TIP,
            deepLinkRoute = Screen.Updates.route
        )
        val normalTip = AppNotification(
            id = 3L,
            title = "مرحباً بك في قداشّ",
            message = "نصيحة يومية لإدارة مصاريفك",
            type = NotificationType.TIP,
            deepLinkRoute = Screen.Home.route
        )

        assertEquals(NotificationType.APP_UPDATE, legacyUpdate.effectiveType())
        assertEquals(NotificationType.APP_UPDATE, deepLinkedUpdate.effectiveType())
        assertEquals(NotificationType.TIP, normalTip.effectiveType())
    }

    @Test
    fun resolveNotificationActionCoversAllNotificationTypes() {
        val updateAction = resolveNotificationAction(
            AppNotification(
                id = 1L,
                title = "تحديث جديد متوفر!",
                message = "نسخة جديدة متاحة",
                type = NotificationType.APP_UPDATE
            )
        )
        assertEquals("تحديث الآن", updateAction.label)
        assertEquals(Screen.Updates.route, updateAction.route)
        assertTrue(updateAction.isHighlighted)

        val legacyUpdateAction = resolveNotificationAction(
            AppNotification(
                id = 2L,
                title = "تحديث جديد متوفر! 🎉",
                message = "إصدار جديد من التطبيق (1.0.0.115)",
                type = NotificationType.TIP
            )
        )
        assertEquals("تحديث الآن", legacyUpdateAction.label)
        assertEquals(Screen.Updates.route, legacyUpdateAction.route)
        assertTrue(legacyUpdateAction.isHighlighted)

        val smartReminderAction = resolveNotificationAction(
            AppNotification(
                id = 3L,
                title = "مراجعة مسائية 🌙",
                message = "سجل معاملات اليوم الآن",
                type = NotificationType.SMART_REMINDER,
                deepLinkRoute = "home"
            )
        )
        assertEquals("تسجيل معاملة", smartReminderAction.label)
        assertEquals(Screen.AddTransaction.createRoute("EXPENSE"), smartReminderAction.route)
        assertFalse(smartReminderAction.isHighlighted)

        val salaryDelayReminder = resolveNotificationAction(
            AppNotification(
                id = 4L,
                title = "تأجيل تاريخ الراتب",
                message = "تم تأجيل الموعد",
                type = NotificationType.SMART_REMINDER,
                deepLinkRoute = Screen.Salary.route
            )
        )
        assertEquals("مراجعة الراتب", salaryDelayReminder.label)
        assertEquals(Screen.Salary.route, salaryDelayReminder.route)

        val budgetAction = resolveNotificationAction(
            AppNotification(
                id = 5L,
                title = "تجاوز الميزانية!",
                message = "تجاوزت حد الطعام",
                type = NotificationType.BUDGET_ALERT,
                deepLinkRoute = Screen.BudgetGoals.route
            )
        )
        assertEquals("مراجعة الميزانية", budgetAction.label)
        assertEquals(Screen.BudgetGoals.route, budgetAction.route)

        val envelopeBudgetAction = resolveNotificationAction(
            AppNotification(
                id = 6L,
                title = "تجاوز حد الظرف!",
                message = "استهلكت 100%",
                type = NotificationType.BUDGET_ALERT,
                deepLinkRoute = Screen.Salary.route
            )
        )
        assertEquals("مراجعة الظرف", envelopeBudgetAction.label)
        assertEquals(Screen.Salary.route, envelopeBudgetAction.route)

        val subAction = resolveNotificationAction(
            AppNotification(
                id = 7L,
                title = "تجديد اشتراك قريب",
                message = "سيتم التجديد",
                type = NotificationType.SUBSCRIPTION_REMINDER
            )
        )
        assertEquals("عرض الاشتراكات", subAction.label)
        assertEquals(Screen.Subscriptions.route, subAction.route)

        val debtPayAction = resolveNotificationAction(
            AppNotification(
                id = 8L,
                title = "تذكير بسداد دين",
                message = "يستحق اليوم",
                type = NotificationType.DEBT_DUE
            )
        )
        assertEquals("سداد الآن", debtPayAction.label)
        assertEquals(Screen.Debts.route, debtPayAction.route)

        val debtLentAction = resolveNotificationAction(
            AppNotification(
                id = 9L,
                title = "تذكير باسترداد سلفة",
                message = "يستحق اليوم",
                type = NotificationType.DEBT_DUE
            )
        )
        assertEquals("متابعة السلفة", debtLentAction.label)
        assertEquals(Screen.Debts.route, debtLentAction.route)

        val salaryAddedAction = resolveNotificationAction(
            AppNotification(
                id = 10L,
                title = "تم إضافة الراتب 💰",
                message = "تم إضافة راتبك",
                type = NotificationType.SALARY_ADDED
            )
        )
        assertEquals("توزيع الراتب", salaryAddedAction.label)
        assertEquals(Screen.Salary.route, salaryAddedAction.route)

        val savingsAction = resolveNotificationAction(
            AppNotification(
                id = 11L,
                title = "إنجاز ادخار",
                message = "وصلت 80%",
                type = NotificationType.SAVINGS_MILESTONE
            )
        )
        assertEquals("عرض الادخار", savingsAction.label)
        assertEquals(Screen.Savings.route, savingsAction.route)

        val backupAction = resolveNotificationAction(
            AppNotification(
                id = 12L,
                title = "تم النسخ الاحتياطي",
                message = "تم الحفظ",
                type = NotificationType.BACKUP_DONE
            )
        )
        assertEquals("النسخ الاحتياطي", backupAction.label)
        assertEquals(Screen.Backup.route, backupAction.route)

        val exportAction = resolveNotificationAction(
            AppNotification(
                id = 13L,
                title = "تم تصدير التقرير",
                message = "ملف PDF جاهز",
                type = NotificationType.EXPORT_DONE
            )
        )
        assertEquals("عرض التقارير", exportAction.label)
        assertEquals(Screen.Export.route, exportAction.route)

        val overspendAction = resolveNotificationAction(
            AppNotification(
                id = 14L,
                title = "توقع تجاوز",
                message = "معدل الإنفاق مرتفع",
                type = NotificationType.OVERSPEND_FORECAST
            )
        )
        assertEquals("عرض الميزانية", overspendAction.label)
        assertEquals(Screen.BudgetGoals.route, overspendAction.route)
    }

    @Test
    fun allNotificationTypesAreCoveredByFilterChips() {
        val coveredTypes = FILTER_CHIPS.flatMap { it.types }.toSet()
        assertEquals(NotificationType.entries.toSet(), coveredTypes)
    }

    @Test
    fun formatNotificationSubtitleHandlesSingularDualPluralAndAllRead() {
        com.qdash.core.utils.FormatterUtils.useWesternNumerals = true
        assertEquals("تابع تنبيهات ميزانيتك وحساباتك", formatNotificationSubtitle(0, 0))
        assertEquals("جميع الإشعارات مقروءة", formatNotificationSubtitle(0, 5))
        assertEquals("إشعار واحد غير مقروء", formatNotificationSubtitle(1, 5))
        assertEquals("إشعاران غير مقروءان", formatNotificationSubtitle(2, 5))
        assertEquals("5 إشعارات غير مقروءة", formatNotificationSubtitle(5, 5))
        assertEquals("16 إشعار غير مقروء", formatNotificationSubtitle(16, 20))
    }
}
