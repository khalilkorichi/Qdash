package com.qdash.domain.usecase.settings

import android.net.Uri
import com.qdash.domain.model.RestorePreview
import com.qdash.domain.repository.BackupRepository
import com.qdash.domain.usecase.onboarding.CompleteOnboardingUseCase

class RestoreBackupUseCase(
    private val backupRepository: BackupRepository,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) {
    suspend fun restoreLocalJson(): Result<Unit> {
        return backupRepository.restoreLocalJsonData()
    }

    suspend fun getRestorePreview(
        inputUri: Uri,
        pwd: CharArray?
    ): Result<RestorePreview> {
        return backupRepository.getRestorePreview(inputUri, pwd)
    }

    suspend fun performRestoreV2(
        preview: RestorePreview,
        selectedTables: List<String>?,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): Result<Unit> {
        return backupRepository.performRestoreV2(preview, selectedTables, onProgress)
            .also { result ->
                if (result.isSuccess) {
                    // A restored backup means the user is an existing user —
                    // they should never see onboarding again.
                    completeOnboardingUseCase()
                }
            }
    }
}

