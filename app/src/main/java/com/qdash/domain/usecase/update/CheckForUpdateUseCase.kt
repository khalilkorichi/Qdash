package com.qdash.domain.usecase.update

import com.qdash.domain.model.CheckingStep
import com.qdash.domain.model.UpdateInfo
import com.qdash.domain.repository.UpdateRepository

class CheckForUpdateUseCase(private val updateRepository: UpdateRepository) {
    suspend operator fun invoke(onStep: suspend (CheckingStep) -> Unit = {}): Result<UpdateInfo> {
        return updateRepository.checkForUpdates(onStep)
    }
}
