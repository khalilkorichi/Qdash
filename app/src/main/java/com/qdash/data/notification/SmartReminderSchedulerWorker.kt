package com.qdash.data.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import com.qdash.FinTrackApp
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SmartReminderSchedulerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FinTrackApp ?: return Result.failure()
        val container = app.container
        val transactionRepository = container.transactionRepository

        try {
            val peakHours = transactionRepository.getPeakTransactionHours()
            val now = System.currentTimeMillis()

            Log.d("SmartReminderScheduler", "Scheduling smart reminders for peak hours: $peakHours")

            for (hour in peakHours) {
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If target hour has already passed today, schedule for tomorrow
                if (targetCal.timeInMillis <= now) {
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val delayMs = targetCal.timeInMillis - now

                val reminderRequest = OneTimeWorkRequestBuilder<SmartReminderWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .addTag("SmartReminder")
                    .build()

                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "SmartReminder_Hour_$hour",
                    ExistingWorkPolicy.REPLACE,
                    reminderRequest
                )

                Log.d("SmartReminderScheduler", "Scheduled reminder for hour $hour (delay: ${delayMs / 1000}s)")
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
