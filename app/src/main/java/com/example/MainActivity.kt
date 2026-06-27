package com.example

import android.content.Context
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val container = (applicationContext as FinTrackApp).container
        val factory = ViewModelFactory(container, applicationContext)

        // Read stable prefs once — these do NOT change during navigation
        val prefs = container.preferencesManager
        val isFirstLaunch = prefs.isFirstLaunch
        val startDestination = Screen.Splash.route

        val isDark = if (isFirstLaunch) false else {
            val mode = prefs.cachedTheme.get()
            when (mode) {
                com.example.core.preferences.PreferencesManager.ThemeMode.DARK -> true
                com.example.core.preferences.PreferencesManager.ThemeMode.LIGHT -> false
                com.example.core.preferences.PreferencesManager.ThemeMode.SYSTEM -> {
                    val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }
        }

        enableEdgeToEdge(
            statusBarStyle = if (isDark) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            },
            navigationBarStyle = if (isDark) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )

        // Apply correct window background color before first Compose frame draws
        val bgClr = if (isDark) 0xFF09090B.toInt() else 0xFFFBFBFA.toInt()
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgClr))

        // Request runtime notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Initialize formatting prefs once at startup
        com.example.core.utils.FormatterUtils.hideDecimals = prefs.hideDecimalsEnabled
        com.example.core.utils.FormatterUtils.useWesternNumerals = prefs.useWesternNumerals

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
