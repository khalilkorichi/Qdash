package com.qdash.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qdash.FinTrackApp
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType
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
                val dueDate = debt.dueDate
                if (!debt.isClosed && dueDate != null) {
                    val diff = dueDate - now
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
