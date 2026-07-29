package com.qdash.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qdash.FinTrackApp

/**
 * Periodic WorkManager worker that refreshes official exchange rates every 6 hours.
 * Only runs when network is connected (enforced via Constraints in FinTrackApp).
 * On failure, WorkManager retries automatically (exponential backoff).
 *
 * Respects isManualOverride flag — user-set rates are never overwritten.
 */
class RefreshOfficialRatesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as FinTrackApp).container
            val result = container.refreshOfficialRatesUseCase()
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
