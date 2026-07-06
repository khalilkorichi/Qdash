package com.qdash.domain.usecase.settings

import android.net.Uri
import com.qdash.domain.model.RestorePreview
import com.qdash.domain.repository.BackupRepository

class RestoreBackupUseCase(private val backupRepository: BackupRepository) {
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
    }
}
