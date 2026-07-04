package com.qdash.data.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.qdash.BuildConfig
import com.qdash.MainActivity
import com.qdash.R
import com.qdash.core.utils.FormatterUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class UpdateDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "kdach_downloads"
        const val NOTIFICATION_ID = 8827
        
        // Thread-safe map to store paused state per APK URL
        private val pausedUrls = ConcurrentHashMap<String, Boolean>()

        fun setPaused(url: String, paused: Boolean) {
            pausedUrls[url] = paused
            Log.d("UpdateDownloadWorker", "Set paused for URL: $url to $paused")
        }

        fun isPaused(url: String): Boolean {
            return pausedUrls[url] == true
        }

        fun clearPaused(url: String) {
            pausedUrls.remove(url)
        }
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        val apkUrl = inputData.getString("apkUrl") ?: return Result.failure()
        val apkSha256 = inputData.getString("apkSha256") ?: return Result.failure()
        val versionName = inputData.getString("versionName") ?: "1.0.0"
        val apkSize = inputData.getLong("apkSize", 0L)
        val infoJson = inputData.getString("infoJson") ?: ""

        Log.d("UpdateDownloadWorker", "Starting download for $apkUrl (size: $apkSize)")
        
        createNotificationChannel()
        clearPaused(apkUrl)

        // Set initial state
        updateRepositoryState(DownloadState.Downloading(0, "", "", getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)))

        val tempFile = File(context.cacheDir, "Qdash-update-temp.apk")
        val startBytes = if (tempFile.exists()) tempFile.length() else 0L

        // If file is already fully downloaded, verify and return success
        if (apkSize > 0L && startBytes >= apkSize) {
            val app = context.applicationContext as? com.qdash.FinTrackApp
            val repository = app?.container?.updateRepository
            if (repository != null && repository.verifyApkSha256(tempFile, apkSha256)) {
                val savedFile = repository.saveDownloadedApk(tempFile, versionName)
                showSuccessNotification(savedFile, versionName)
                val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)
                updateRepositoryState(DownloadState.Success(savedFile, info))
                return Result.success()
            } else {
                tempFile.delete()
            }
        }

        // Establish connection
        val requestBuilder = Request.Builder().url(apkUrl)
        if (startBytes > 0L) {
            requestBuilder.addHeader("Range", "bytes=$startBytes-")
        }
        val request = requestBuilder.build()

        try {
            setForeground(createForegroundInfo(0, "جاري البدء...", "جاري الحساب...", false, infoJson, apkUrl, apkSize, apkSha256, versionName))
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Server returned code ${response.code}")
            }

            val body = response.body ?: throw IOException("Empty response body")
            val remainingLength = body.contentLength()
            val totalLength = if (startBytes > 0L && (response.code == 206 || response.code == 200)) {
                if (response.code == 206) remainingLength + startBytes else remainingLength
            } else {
                remainingLength
            }

            val isAppend = startBytes > 0L && response.code == 206
            if (startBytes > 0L && response.code == 200 && tempFile.exists()) {
                tempFile.delete()
            }

            body.byteStream().use { input ->
                FileOutputStream(tempFile, isAppend).use { output ->
                    val buffer = ByteArray(16384) // 16KB buffer for smooth I/O
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    var lastEmittedTime = System.currentTimeMillis()
                    var lastEmittedBytes = 0L
                    val speedWindow = LinkedList<Long>()
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // Check if paused
                        if (isPaused(apkUrl)) {
                            Log.d("UpdateDownloadWorker", "Download paused by user request")
                            val currentTotalRead = totalBytesRead + (if (isAppend) startBytes else 0L)
                            val percentage = if (totalLength > 0L) ((currentTotalRead * 100) / totalLength).toInt().coerceIn(0, 100) else 0
                            showPausedNotification(percentage, infoJson, apkUrl, apkSize, apkSha256, versionName)
                            
                            val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)
                            updateRepositoryState(DownloadState.Paused(percentage, info))
                            return Result.success()
                        }

                        // Check if cancelled/stopped by WorkManager
                        if (isStopped) {
                            Log.d("UpdateDownloadWorker", "Download stopped/cancelled")
                            return Result.failure()
                        }

                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val now = System.currentTimeMillis()
                        val timeDelta = now - lastEmittedTime

                        // Calculate speed and ETA every 1 second
                        if (timeDelta >= 1000L) {
                            val byteDelta = totalBytesRead - lastEmittedBytes
                            val speedBytesPerSec = (byteDelta * 1000L) / timeDelta
                            
                            if (speedWindow.size >= 5) {
                                speedWindow.removeFirst()
                            }
                            speedWindow.add(speedBytesPerSec)
                            val avgSpeed = speedWindow.average().toLong()

                            val currentTotalRead = totalBytesRead + (if (isAppend) startBytes else 0L)
                            val progressPercent = if (totalLength > 0L) ((currentTotalRead * 100) / totalLength).toInt().coerceIn(0, 100) else 0

                            val speedStr = formatSpeed(avgSpeed)
                            val remainingBytes = totalLength - currentTotalRead
                            val etaStr = formatEta(remainingBytes, avgSpeed)

                            // Update notification and WorkManager progress
                            setForeground(createForegroundInfo(progressPercent, speedStr, etaStr, false, infoJson, apkUrl, apkSize, apkSha256, versionName))
                            setProgress(workDataOf(
                                "progress" to progressPercent,
                                "speed" to speedStr,
                                "eta" to etaStr,
                                "isPaused" to false
                            ))

                            val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)
                            updateRepositoryState(DownloadState.Downloading(progressPercent, speedStr, etaStr, info))

                            lastEmittedTime = now
                            lastEmittedBytes = totalBytesRead
                        }
                    }
                }
            }

            // Download finished, verify integrity
            val app = context.applicationContext as? com.qdash.FinTrackApp
            val repository = app?.container?.updateRepository
            val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)
            
            if (repository != null) {
                setForeground(createForegroundInfo(100, "جاري التحقق...", "لحظات...", false, infoJson, apkUrl, apkSize, apkSha256, versionName))
                val isValid = repository.verifyApkSha256(tempFile, apkSha256)
                if (isValid) {
                    val savedFile = repository.saveDownloadedApk(tempFile, versionName)
                    showSuccessNotification(savedFile, versionName)
                    updateRepositoryState(DownloadState.Success(savedFile, info))
                    return Result.success()
                } else {
                    tempFile.delete()
                    showFailureNotification("فشلت عملية التحقق من سلامة الملف (SHA256 Mismatch)")
                    updateRepositoryState(DownloadState.Error("فشلت عملية التحقق من سلامة الملف (SHA256 Mismatch)", info))
                    return Result.failure()
                }
            } else {
                showFailureNotification("خطأ غير متوقع في تهيئة النظام")
                updateRepositoryState(DownloadState.Error("خطأ غير متوقع في تهيئة النظام", info))
                return Result.failure()
            }
        } catch (e: Exception) {
            Log.e("UpdateDownloadWorker", "Error downloading update", e)
            showFailureNotification(e.localizedMessage ?: "فشل تحميل الملف")
            val info = getUpdateInfoFromJson(infoJson, apkUrl, apkSize, apkSha256, versionName)
            updateRepositoryState(DownloadState.Error(e.localizedMessage ?: "فشل تحميل الملف", info))
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحديثات التطبيق",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تحميل وتثبيت تحديثات التطبيق"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        progress: Int,
        speed: String,
        eta: String,
        isPaused: Boolean,
        infoJson: String,
        apkUrl: String,
        apkSize: Long,
        apkSha256: String,
        versionName: String
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construct Custom RemoteViews
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_download)
        
        remoteViews.setViewVisibility(R.id.notification_btn_install, android.view.View.GONE)
        remoteViews.setViewVisibility(R.id.notification_btn_pause, android.view.View.VISIBLE)
        remoteViews.setViewVisibility(R.id.notification_btn_cancel, android.view.View.VISIBLE)
        
        val titleText = if (isPaused) "تم إيقاف تحميل التحديث مؤقتاً" else "جاري تحميل التحديث (v$versionName)..."
        remoteViews.setTextViewText(R.id.notification_title, titleText)
        
        val arabicProgress = FormatterUtils.convertNumerals("$progress%")
        remoteViews.setTextViewText(R.id.notification_progress_text, arabicProgress)
        remoteViews.setProgressBar(R.id.notification_progress_bar, 100, progress, false)
        
        val infoText = if (isPaused) {
            "تم الإيقاف مؤقتاً عند $arabicProgress"
        } else {
            "السرعة: $speed • المتبقي: $eta"
        }
        remoteViews.setTextViewText(R.id.notification_info, infoText)

        // Action Buttons Setup
        val actionAction = if (isPaused) NotificationActionReceiver.ACTION_RESUME else NotificationActionReceiver.ACTION_PAUSE
        val actionText = if (isPaused) "استئناف" else "إيقاف مؤقت"

        val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = actionAction
            putExtra(NotificationActionReceiver.EXTRA_UPDATE_INFO, infoJson)
        }
        val pendingAction = PendingIntent.getBroadcast(
            context,
            1,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setTextViewText(R.id.notification_btn_pause, actionText)
        remoteViews.setOnClickPendingIntent(R.id.notification_btn_pause, pendingAction)

        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CANCEL
            putExtra(NotificationActionReceiver.EXTRA_UPDATE_INFO, infoJson)
        }
        val pendingCancel = PendingIntent.getBroadcast(
            context,
            2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.notification_btn_cancel, pendingCancel)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingIntent)
            .setOngoing(!isPaused)
            .setAutoCancel(false)
            .build()
    }

    private fun createForegroundInfo(
        progress: Int,
        speed: String,
        eta: String,
        isPaused: Boolean,
        infoJson: String,
        apkUrl: String,
        apkSize: Long,
        apkSha256: String,
        versionName: String
    ): ForegroundInfo {
        val notification = buildNotification(progress, speed, eta, isPaused, infoJson, apkUrl, apkSize, apkSha256, versionName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID, 
                notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showPausedNotification(
        progress: Int,
        infoJson: String,
        apkUrl: String,
        apkSize: Long,
        apkSha256: String,
        versionName: String
    ) {
        val notification = buildNotification(progress, "", "", true, infoJson, apkUrl, apkSize, apkSha256, versionName)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showSuccessNotification(file: File, versionName: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        // Direct install intent
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pendingInstall = PendingIntent.getActivity(
            context,
            10,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construct Custom RemoteViews for completed/success state
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_download)
        
        remoteViews.setTextViewText(R.id.notification_title, "اكتمل تحميل التحديث 🎉")
        
        val arabicProgress = FormatterUtils.convertNumerals("100%")
        remoteViews.setTextViewText(R.id.notification_progress_text, arabicProgress)
        remoteViews.setProgressBar(R.id.notification_progress_bar, 100, 100, false)
        
        remoteViews.setTextViewText(R.id.notification_info, "الإصدار v$versionName جاهز للتثبيت الآن.")
        
        // Configure button visibilities
        remoteViews.setViewVisibility(R.id.notification_btn_pause, android.view.View.GONE)
        remoteViews.setViewVisibility(R.id.notification_btn_cancel, android.view.View.GONE)
        remoteViews.setViewVisibility(R.id.notification_btn_install, android.view.View.VISIBLE)
        
        // Bind installation trigger to the "Install Now" button
        remoteViews.setOnClickPendingIntent(R.id.notification_btn_install, pendingInstall)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingInstall)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFailureNotification(errorMessage: String) {
        val retryIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingRetry = PendingIntent.getActivity(
            context,
            20,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("فشل تحميل التحديث")
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingRetry)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        val locale = Locale("ar")
        val formatted = when {
            bytesPerSec >= 1024 * 1024 -> String.format(locale, "%.2f م.ب/ث", bytesPerSec.toDouble() / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format(locale, "%.1f ك.ب/ث", bytesPerSec.toDouble() / 1024)
            else -> String.format(locale, "%d ب/ث", bytesPerSec)
        }
        return FormatterUtils.convertNumerals(formatted)
    }

    private fun formatEta(remainingBytes: Long, bytesPerSec: Long): String {
        if (bytesPerSec <= 0 || remainingBytes <= 0) return FormatterUtils.convertNumerals("حساب...")
        
        val totalSeconds = remainingBytes / bytesPerSec
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val formatted = when {
            hours > 0 -> "${hours}س و ${minutes}د"
            minutes > 0 -> "${minutes}د و ${seconds}ث"
            else -> "${seconds}ثانية"
        }
        return FormatterUtils.convertNumerals(formatted)
    }

    private fun updateRepositoryState(state: DownloadState) {
        val app = context.applicationContext as? com.qdash.FinTrackApp
        val repository = app?.container?.updateRepository as? UpdateRepositoryImpl
        repository?.setDownloadState(state)
    }

    private fun getUpdateInfoFromJson(
        infoJson: String,
        apkUrl: String,
        apkSize: Long,
        apkSha256: String,
        versionName: String
    ): UpdateInfo {
        if (infoJson.isNotBlank()) {
            try {
                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(UpdateInfo::class.java)
                return adapter.fromJson(infoJson) ?: createFallbackInfo(apkUrl, apkSize, apkSha256, versionName)
            } catch (e: Exception) {
                // fallback
            }
        }
        return createFallbackInfo(apkUrl, apkSize, apkSha256, versionName)
    }

    private fun createFallbackInfo(
        apkUrl: String,
        apkSize: Long,
        apkSha256: String,
        versionName: String
    ): UpdateInfo {
        return UpdateInfo(
            hasUpdate = true,
            versionCode = 1,
            versionName = versionName,
            updateIdentity = System.currentTimeMillis(),
            apkUrl = apkUrl,
            apkSize = apkSize,
            apkSha256 = apkSha256,
            mandatory = false,
            releaseNotes = null
        )
    }
}
