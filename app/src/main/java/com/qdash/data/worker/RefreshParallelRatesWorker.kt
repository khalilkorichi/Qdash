package com.qdash.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qdash.FinTrackApp

/**
 * Periodic WorkManager worker that refreshes parallel market exchange rates every 15 minutes.
 * Only runs when network is connected (enforced via Constraints in FinTrackApp).
 */
class RefreshParallelRatesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as FinTrackApp).container
            val result = container.refreshParallelRatesUseCase()
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
