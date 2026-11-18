package com.qdash.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qdash.FinTrackApp
import com.qdash.core.preferences.PreferencesManager
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.BudgetStatus
import com.qdash.domain.model.NotificationType
import kotlinx.coroutines.flow.first

class FinancialAlertsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (runAttemptCount >= 3) return Result.failure()
        val app = applicationContext as? FinTrackApp ?: return Result.failure()
        val container = app.container
        val prefs = PreferencesManager(applicationContext)

        if (!prefs.notifBillingReminder && !prefs.notifSalaryReminder && !prefs.notifBudgetAlert) {
            return Result.success()
        }

        val debtRepository = container.debtRepository
        val subscriptionRepository = container.subscriptionRepository
        val incomeRepository = container.incomeRepository
        val notificationRepository = container.notificationRepository

        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        try {
            // Get existing notifications to prevent duplicates
            val existingNotifications = notificationRepository.getAllNotifications().first()

            if (prefs.notifBillingReminder) {
                // 1. Process Debts
                val activeDebts = debtRepository.getActiveDebts().first()
                for (debt in activeDebts) {
                    val dueDate = debt.dueDate
                    if (!debt.isClosed && dueDate != null) {
                        val diff = dueDate - now
                        if (diff >= -oneDayMillis) {
                            val daysLeft = if (diff <= 0L) 0L else diff / oneDayMillis

                            // Notify if due in 3 days or less, but still today or in the near future
                            if (daysLeft in 0..3) {
                                val alreadyNotified = existingNotifications.any {
                                    it.type == NotificationType.DEBT_DUE &&
                                            it.relatedEntityId == debt.id &&
                                            (now - it.timestamp) < 7L * oneDayMillis
                                }

                                if (!alreadyNotified) {
                                    val titleText: String
                                    val msg: String
                                    if (debt.direction == com.qdash.domain.model.DebtDirection.OWED_TO_ME) {
                                        titleText = "تذكير باسترداد سلفة 💸"
                                        msg = if (daysLeft == 0L) {
                                            "يستحق استرداد سلفة من ${debt.creditorName} بقيمة ${debt.remainingAmount.toInt()} د.ج اليوم!"
                                        } else {
                                            "يستحق استرداد سلفة من ${debt.creditorName} بقيمة ${debt.remainingAmount.toInt()} د.ج خلال $daysLeft أيام."
                                        }
                                    } else {
                                        val isRegular = debt is com.qdash.domain.model.RegularDebt
                                        val term = if (isRegular) "دين" else "قسط قرض"
                                        titleText = if (isRegular) "تذكير بسداد دين 💸" else "تذكير بقسط قرض قريب 💸"
                                        msg = if (daysLeft == 0L) {
                                            "يستحق سداد $term لـ ${debt.creditorName} بقيمة ${debt.remainingAmount.toInt()} د.ج اليوم!"
                                        } else {
                                            "يستحق سداد $term لـ ${debt.creditorName} بقيمة ${debt.remainingAmount.toInt()} د.ج خلال $daysLeft أيام."
                                        }
                                    }
                                    val notification = AppNotification(
                                        title = titleText,
                                        message = msg,
                                        type = NotificationType.DEBT_DUE,
                                        relatedEntityId = debt.id,
                                        deepLinkRoute = "debts"
                                    )
                                    notificationRepository.insertNotification(notification)
                                }
                            }
                        }
                    }
                }

                // 2. Process Subscriptions
                val activeSubs = subscriptionRepository.getActiveSubscriptions().first()
                for (sub in activeSubs) {
                    if (sub.isActive) {
                        val diff = sub.nextBillingDate - now
                        if (diff >= -oneDayMillis) {
                            val daysLeft = if (diff <= 0L) 0L else diff / oneDayMillis

                            // Notify if due in reminderDaysBefore or less
                            if (daysLeft in 0..sub.reminderDaysBefore) {
                                val alreadyNotified = existingNotifications.any {
                                    it.type == NotificationType.SUBSCRIPTION_REMINDER &&
                                            it.relatedEntityId == sub.id &&
                                            (now - it.timestamp) < 15L * oneDayMillis
                                }

                                if (!alreadyNotified) {
                                    val msg = if (daysLeft == 0L) {
                                        "سيتم تجديد اشتراكك في ${sub.name} بقيمة ${sub.amount.toInt()} د.ج اليوم!"
                                    } else {
                                        "سيتم تجديد اشتراكك في ${sub.name} بقيمة ${sub.amount.toInt()} د.ج خلال $daysLeft أيام."
                                    }
                                    val notification = AppNotification(
                                        title = "تجديد اشتراك قريب 📅",
                                        message = msg,
                                        type = NotificationType.SUBSCRIPTION_REMINDER,
                                        relatedEntityId = sub.id,
                                        deepLinkRoute = "subscriptions"
                                    )
                                    notificationRepository.insertNotification(notification)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Process Salary Reminders
            if (prefs.notifSalaryReminder) {
                val activeSalaries = incomeRepository.getActiveIncomeSources().first()
                    .filter { it.isActive && it.type.equals("SALARY", ignoreCase = true) }
                for (salary in activeSalaries) {
                    val diff = salary.nextExpectedDate - now
                    if (diff >= -oneDayMillis) {
                        val daysLeft = if (diff <= 0L) 0L else diff / oneDayMillis
                        if (daysLeft in 0..1) {
                            val alreadyNotified = existingNotifications.any {
                                it.type == NotificationType.SALARY_ADDED &&
                                        it.relatedEntityId == salary.id &&
                                        (now - it.timestamp) < 7L * oneDayMillis
                            }
                            if (!alreadyNotified) {
                                val msg = if (daysLeft == 0L) {
                                    "اليوم هو الموعد المتوقع لاستلام ${salary.name} بقيمة ${salary.amount.toInt()} د.ج."
                                } else {
                                    "يقترب موعد استلام ${salary.name} بقيمة ${salary.amount.toInt()} د.ج غداً."
                                }
                                notificationRepository.insertNotification(
                                    AppNotification(
                                        title = "تذكير بموعد الراتب 💰",
                                        message = msg,
                                        type = NotificationType.SALARY_ADDED,
                                        relatedEntityId = salary.id,
                                        deepLinkRoute = "salary"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. Process Budget Alerts
            if (prefs.notifBudgetAlert) {
                val budgetGoals = container.getBudgetGoalsUseCase().first()
                val alerts = container.getBudgetAlertsUseCase(budgetGoals)
                for (alert in alerts) {
                    if (alert.status == BudgetStatus.EXCEEDED || alert.status == BudgetStatus.CRITICAL) {
                        val alreadyNotified = existingNotifications.any {
                            it.type == NotificationType.BUDGET_ALERT &&
                                    it.relatedEntityId == alert.budgetId &&
                                    (now - it.timestamp) < oneDayMillis
                        }
                        if (!alreadyNotified) {
                            val title = if (alert.status == BudgetStatus.EXCEEDED) {
                                "تجاوز الميزانية! ⚠️"
                            } else {
                                "تنبيه ميزانية حرج! ⚠️"
                            }
                            notificationRepository.insertNotification(
                                AppNotification(
                                    title = title,
                                    message = alert.message,
                                    type = NotificationType.BUDGET_ALERT,
                                    relatedEntityId = alert.budgetId,
                                    deepLinkRoute = "budget_goals"
                                )
                            )
                        }
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }
}
