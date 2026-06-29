package com.example.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.FinTrackApp
import com.example.domain.model.AppNotification
import com.example.domain.model.NotificationType
import kotlinx.coroutines.flow.first

class FinancialAlertsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FinTrackApp ?: return Result.failure()
        val container = app.container

        val debtRepository = container.debtRepository
        val subscriptionRepository = container.subscriptionRepository
        val notificationRepository = container.notificationRepository

        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        try {
            // Get existing notifications to prevent duplicates
            val existingNotifications = notificationRepository.getAllNotifications().first()

            // 1. Process Debts
            val activeDebts = debtRepository.getActiveDebts().first()
            for (debt in activeDebts) {
                if (!debt.isClosed && debt.dueDate != null) {
                    val diff = debt.dueDate - now
                    val daysLeft = diff / oneDayMillis
                    
                    // Notify if due in 3 days or less, but still in the future or today
                    if (daysLeft in 0..3) {
                        // Check if we already notified for this debt in the last 7 days
                        val alreadyNotified = existingNotifications.any {
                            it.type == NotificationType.DEBT_DUE &&
                                    it.relatedEntityId == debt.id &&
                                    (now - it.timestamp) < 7L * 24 * 60 * 60 * 1000L
                        }

                        if (!alreadyNotified) {
                            val msg = if (daysLeft == 0L) {
                                "يستحق سداد دين لـ ${debt.creditorName} بقيمة ${debt.remainingAmount.toInt()} د.ج اليوم!"
                            } else {
                                "يستحق سداد دين لـ ${debt.creditorName} بقيمة ${debt.remainingAmount.toInt()} د.ج خلال $daysLeft أيام."
                            }
                            val notification = AppNotification(
                                title = "تذكير بسداد دين 💸",
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

            // 2. Process Subscriptions
            val activeSubs = subscriptionRepository.getActiveSubscriptions().first()
            for (sub in activeSubs) {
                if (sub.isActive) {
                    val diff = sub.nextBillingDate - now
                    val daysLeft = diff / oneDayMillis

                    // Notify if due in reminderDaysBefore or less
                    if (daysLeft in 0..sub.reminderDaysBefore) {
                        // Check if we already notified for this subscription in the last 15 days
                        val alreadyNotified = existingNotifications.any {
                            it.type == NotificationType.SUBSCRIPTION_REMINDER &&
                                    it.relatedEntityId == sub.id &&
                                    (now - it.timestamp) < 15L * 24 * 60 * 60 * 1000L
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

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
