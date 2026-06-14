package com.example

import android.content.Context
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.presentation.ViewModelFactory
import com.example.presentation.app.FinTrackApp
import com.example.presentation.navigation.Screen

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val container = (newBase.applicationContext as? FinTrackApp)?.container
        val prefs = container?.preferencesManager ?: com.example.core.preferences.PreferencesManager(newBase)
        val locale = java.util.Locale(prefs.appLanguage)
        java.util.Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request runtime notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val container = (applicationContext as FinTrackApp).container
        val factory = ViewModelFactory(container, applicationContext)

        // Read stable prefs once — these do NOT change during navigation
        val prefs = container.preferencesManager
        val isFirstLaunch = prefs.isFirstLaunch
        val startDestination = if (isFirstLaunch) Screen.Onboarding.route else Screen.Home.route

        // Initialize formatting prefs once at startup
        com.example.core.utils.FormatterUtils.hideDecimals = prefs.hideDecimalsEnabled

        setContent {
            FinTrackApp(
                container = container,
                factory = factory,
                startDestination = startDestination,
                isFirstLaunch = isFirstLaunch
            )
        }
    }
}
