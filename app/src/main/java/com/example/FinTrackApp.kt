package com.example

import android.app.Application
import android.content.Context
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
    }
}
