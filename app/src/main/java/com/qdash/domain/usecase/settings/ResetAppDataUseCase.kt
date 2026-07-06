package com.qdash.domain.usecase.settings

import com.qdash.domain.repository.BackupRepository

class ResetAppDataUseCase(private val backupRepository: BackupRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return backupRepository.resetAllData()
    }
}
