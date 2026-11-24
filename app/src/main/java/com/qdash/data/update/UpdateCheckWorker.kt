package com.qdash.data.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qdash.BuildConfig
import com.qdash.FinTrackApp
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (runAttemptCount >= 3) return Result.failure()
        val app = applicationContext as? FinTrackApp ?: return Result.failure()
        val container = app.container
        val updateRepository = container.updateRepository
        val notificationRepository = container.notificationRepository
        val prefs = container.preferencesManager

        try {
            val result = updateRepository.checkForUpdates()
            if (result.isSuccess) {
                val updateInfo = result.getOrThrow()
                prefs.lastUpdateCheckTime = System.currentTimeMillis()

                val localVersionName = BuildConfig.VERSION_NAME
                val localVersionCode = BuildConfig.VERSION_CODE
                val isNewerVersion = isVersionNewer(localVersionName, updateInfo.versionName)
                val isNewerVersionCode = updateInfo.versionCode > localVersionCode

                if (updateInfo.hasUpdate && (isNewerVersion || isNewerVersionCode) && prefs.lastNotifiedUpdateVersion != updateInfo.versionName) {
                    prefs.lastNotifiedUpdateVersion = updateInfo.versionName
                    val notification = AppNotification(
                        title = "تحديث جديد متوفر! 🎉",
                        message = "إصدار جديد من التطبيق (${updateInfo.versionName}) متوفر الآن للتحميل.",
                        type = NotificationType.APP_UPDATE,
                        deepLinkRoute = com.qdash.presentation.navigation.Screen.Updates.route
                    )
                    notificationRepository.insertNotification(notification)
                }
                return Result.success()
            } else {
                return if (runAttemptCount >= 3) Result.failure() else Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return if (runAttemptCount >= 3) Result.failure() else Result.retry()
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
