package com.qdash.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qdash.FinTrackApp
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType
import kotlinx.coroutines.flow.first
import java.util.Calendar

class SmartReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FinTrackApp ?: return Result.failure()
        val container = app.container
        val notificationRepository = container.notificationRepository

        val now = System.currentTimeMillis()
        
        try {
            // Prevent spam: check if notified in the last 2 hours
            val existing = notificationRepository.getAllNotifications().first()
            val recentlyNotified = existing.any {
                it.type == NotificationType.SMART_REMINDER &&
                        (now - it.timestamp) < 2 * 60 * 60 * 1000L
            }
            if (recentlyNotified) {
                return Result.success()
            }

            // Determine time of day and build messages
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            val (title, message) = when (hour) {
                in 5..11 -> {
                    val msgs = listOf(
                        "صباح الخير! ☀️ هل أنفقت شيئاً هذا الصباح؟ سجل مصاريف الفطور أو القهوة الآن.",
                        "يوم مالي منظم يبدأ من الصباح! سجل أي مصاريف أو فواتير صباحية لتظل ميزانيتك دقيقة."
                    )
                    Pair("تذكير صباحي ☀️", msgs.random())
                }
                in 12..16 -> {
                    val msgs = listOf(
                        "تذكير منتصف اليوم ☕ هل قمت بتسجيل مصاريف الغداء أو المواصلات اليوم؟",
                        "أهلاً بك! خذ دقيقة لتسجيل معاملاتك للنصف الأول من اليوم وحافظ على انضباطك المالي."
                    )
                    Pair("تذكير منتصف اليوم ☕", msgs.random())
                }
                else -> { // Evening or Night
                    val msgs = listOf(
                        "مراجعة مسائية 🌙 دعنا نراجع ونوثق مصاريف اليوم قبل نسيانها.",
                        "هل انتهى يومك المالي؟ سجل معاملات اليوم الآن لتستمر في تتبع ميزانيتك باحترافية."
                    )
                    Pair("مراجعة مسائية 🌙", msgs.random())
                }
            }

            val notification = AppNotification(
                title = title,
                message = message,
                type = NotificationType.SMART_REMINDER,
                deepLinkRoute = "home"
            )
            notificationRepository.insertNotification(notification)

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
