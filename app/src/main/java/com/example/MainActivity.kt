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
        com.example.core.utils.FormatterUtils.useWesternNumerals = prefs.useWesternNumerals

        // Automatic update checking on application cold start
        val updateRepository = container.updateRepository
        val notificationRepository = container.notificationRepository
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                delay(3000) // 3-second delay to let the app finish drawing the home screen
                updateRepository.checkForUpdates().onSuccess { updateInfo ->
                    val localVersionName = BuildConfig.VERSION_NAME
                    val localVersionCode = BuildConfig.VERSION_CODE
                    val isNewerVersion = isVersionNewer(localVersionName, updateInfo.versionName)
                    val isNewerVersionCode = updateInfo.versionCode > localVersionCode
                    
                    if (updateInfo.hasUpdate && (isNewerVersion || isNewerVersionCode) && prefs.lastNotifiedUpdateVersion != updateInfo.versionName) {
                        prefs.lastNotifiedUpdateVersion = updateInfo.versionName
                        val notification = com.example.domain.model.AppNotification(
                            title = "تحديث جديد متوفر! 🎉",
                            message = "إصدار جديد من التطبيق (${updateInfo.versionName}) متوفر الآن للتحميل.",
                            type = com.example.domain.model.NotificationType.TIP
                        )
                        notificationRepository.insertNotification(notification)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            FinTrackApp(
                container = container,
                factory = factory,
                startDestination = startDestination,
                isFirstLaunch = isFirstLaunch
            )
        }
    }

    private fun isVersionNewer(local: String, remote: String): Boolean {
        val localParts = local.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until maxLength) {
            val localVal = localParts.getOrElse(i) { 0 }
            val remoteVal = remoteParts.getOrElse(i) { 0 }
            if (remoteVal > localVal) return true
            if (localVal > remoteVal) return false
        }
        return false
    }
}
