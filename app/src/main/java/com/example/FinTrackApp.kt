package com.example

import android.app.Application
import com.example.core.di.AppContainer
import com.example.core.di.AppContainerImpl
import com.example.core.utils.SystemNotificationHelper

class FinTrackApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
        SystemNotificationHelper.createNotificationChannel(this)
    }
}
