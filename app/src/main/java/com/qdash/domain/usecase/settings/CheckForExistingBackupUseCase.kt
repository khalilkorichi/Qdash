package com.qdash.domain.usecase.settings

import android.content.Context
import com.qdash.domain.model.BackupFileMetadata
import com.qdash.domain.repository.DriveSyncRepository

class CheckForExistingBackupUseCase(
    private val driveSyncRepository: DriveSyncRepository
) {
    suspend operator fun invoke(context: Context): Result<BackupFileMetadata?> {
        return driveSyncRepository.checkIfBackupExists(context)
    }
}
