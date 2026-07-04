package com.qdash.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qdash.FinTrackApp
import com.qdash.core.utils.SystemNotificationHelper
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType

class ScheduledBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FinTrackApp ?: return Result.failure()
        val backupManager = app.container.backupManager
        val preferencesManager = app.container.preferencesManager

        val folderUriStr = preferencesManager.backupFolderUri
        if (folderUriStr.isNullOrEmpty()) {
            sendFailureNotification("لم يتم تحديد مجلد للنسخ الاحتياطي التلقائي.")
            return Result.failure()
        }

        val folderUri = Uri.parse(folderUriStr)
        if (!backupManager.isFolderUriValid(folderUriStr)) {
            sendFailureNotification("مجلد النسخ المختار لم يعد صالحاً أو تم سحب الصلاحيات.")
            return Result.failure()
        }

        val maxKeep = preferencesManager.keepMaxBackupsCount

        return try {
            backupManager.exportBackupToFolder(
                folderUri = folderUri,
                password = null, // unencrypted for scheduled background backups
                includeAttachments = true,
                maxKeepBackups = maxKeep
            ).fold(
                onSuccess = { fileDetails ->
                    sendSuccessNotification(fileDetails.name)
                    // Update last backup date preference
                    val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    preferencesManager.lastBackupDate = formattedDate
                    Result.success()
                },
                onFailure = { error ->
                    sendFailureNotification(error.localizedMessage ?: "حدث خطأ غير معروف.")
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            sendFailureNotification(e.localizedMessage ?: "حدث خطأ غير معروف.")
            Result.failure()
        }
    }

    private fun sendSuccessNotification(filename: String) {
        val notification = AppNotification(
            id = System.currentTimeMillis() % 100000,
            title = "تم النسخ الاحتياطي التلقائي",
            message = "تم حفظ ملف النسخة الاحتياطية بنجاح: $filename",
            type = NotificationType.BACKUP_DONE
        )
        SystemNotificationHelper.showNotification(applicationContext, notification)
    }

    private fun sendFailureNotification(error: String) {
        val notification = AppNotification(
            id = (System.currentTimeMillis() % 100000) + 100000,
            title = "فشل النسخ الاحتياطي التلقائي",
            message = "السبب: $error",
            type = NotificationType.BACKUP_DONE // Keep under BACKUP_DONE to route correctly
        )
        SystemNotificationHelper.showNotification(applicationContext, notification)
    }
}
