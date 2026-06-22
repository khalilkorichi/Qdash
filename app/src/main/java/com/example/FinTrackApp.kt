package com.example

import android.app.Application
import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.data.update.UpdateCheckWorker
import java.util.concurrent.TimeUnit
import com.example.core.di.AppContainer
import com.example.core.di.AppContainerImpl
import com.example.core.utils.SystemNotificationHelper

class FinTrackApp : Application() {
    lateinit var container: AppContainer

    override fun attachBaseContext(base: Context) {
        val prefs = com.example.core.preferences.PreferencesManager(base)
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
        SystemNotificationHelper.createNotificationChannel(this)
        schedulePeriodicUpdateChecks()
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
}
