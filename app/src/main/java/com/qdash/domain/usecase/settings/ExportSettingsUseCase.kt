package com.qdash.domain.usecase.settings

import android.net.Uri
import com.qdash.domain.model.BackupFileInfo
import com.qdash.domain.repository.BackupRepository

class ExportSettingsUseCase(private val backupRepository: BackupRepository) {
    suspend fun backupLocalJson(): Result<String> {
        return backupRepository.backupLocalJsonData()
    }

    suspend fun exportBackupToFolder(
        folderUri: Uri,
        pwd: CharArray?,
        includeAttachments: Boolean,
        maxKeepBackups: Int,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): Result<BackupFileInfo> {
        return backupRepository.exportBackupToFolder(folderUri, pwd, includeAttachments, maxKeepBackups, onProgress)
    }

    suspend fun exportBackupV2(
        outputUri: Uri,
        pwd: CharArray?,
        includeAttachments: Boolean,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): Result<Unit> {
        return backupRepository.exportBackupV2(outputUri, pwd, includeAttachments, onProgress)
    }
}
