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
        if (runAttemptCount >= 3) return Result.failure()
        val app = applicationContext as? FinTrackApp ?: return Result.failure()
        val driveSyncRepository = app.container.driveSyncRepository
        val preferencesManager = app.container.preferencesManager

        if (!preferencesManager.isGoogleLinked) {
            return Result.success()
        }

        if (preferencesManager.hasPendingBackupRestoreCheck) {
            return Result.success()
        }

        return try {
            val result = driveSyncRepository.uploadToAppData(applicationContext)
            if (result.isSuccess) {
                Result.success()
            } else if (runAttemptCount >= 3) {
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }
}
