package com.qdash.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qdash.FinTrackApp

class SyncToDriveWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FinTrackApp ?: return Result.failure()
        val driveSyncRepository = app.container.driveSyncRepository
        val preferencesManager = app.container.preferencesManager

        if (!preferencesManager.isGoogleLinked) {
            return Result.success()
        }

        if (preferencesManager.hasPendingBackupRestoreCheck) {
            return Result.success()
        }

        val result = driveSyncRepository.uploadToAppData(applicationContext)
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
