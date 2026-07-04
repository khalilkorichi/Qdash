package com.qdash

import android.app.Application
import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.qdash.data.update.UpdateCheckWorker
import com.qdash.data.notification.FinancialAlertsWorker
import com.qdash.data.notification.SmartReminderSchedulerWorker
import java.util.concurrent.TimeUnit
import com.qdash.core.di.AppContainer
import com.qdash.core.di.AppContainerImpl
import com.qdash.core.utils.SystemNotificationHelper

class FinTrackApp : Application() {
    lateinit var container: AppContainer

    override fun attachBaseContext(base: Context) {
        val prefs = com.qdash.core.preferences.PreferencesManager(base)
        val locale = java.util.Locale(prefs.appLanguage)
        java.util.Locale.setDefault(locale)
        val config = base.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
        container.preferencesManager.loadInitialThemeSync()
        SystemNotificationHelper.createNotificationChannel(this)
        schedulePeriodicUpdateChecks()
        schedulePeriodicFinancialAlerts()
        schedulePeriodicSmartReminders()
    }

    private fun schedulePeriodicUpdateChecks() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PeriodicUpdateCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                updateCheckRequest
            )
        } catch (e: Exception) {
            android.util.Log.w("FinTrackApp", "WorkManager initialization/scheduling failed, this is normal in tests.", e)
        }
    }

    private fun schedulePeriodicFinancialAlerts() {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val financialAlertsRequest = PeriodicWorkRequestBuilder<FinancialAlertsWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PeriodicFinancialAlerts",
                ExistingPeriodicWorkPolicy.KEEP,
                financialAlertsRequest
            )
        } catch (e: Exception) {
            android.util.Log.w("FinTrackApp", "FinancialAlertsWorker scheduling failed, this is normal in tests.", e)
        }
    }

    private fun schedulePeriodicSmartReminders() {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val schedulerRequest = PeriodicWorkRequestBuilder<SmartReminderSchedulerWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SmartReminderScheduler",
                ExistingPeriodicWorkPolicy.KEEP,
                schedulerRequest
            )
        } catch (e: Exception) {
            android.util.Log.w("FinTrackApp", "SmartReminderSchedulerWorker scheduling failed, this is normal in tests.", e)
        }
    }
}
