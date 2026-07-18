package com.qdash.domain.usecase.settings

import android.content.Context
import com.qdash.domain.repository.DriveSyncRepository
import com.qdash.domain.usecase.onboarding.CompleteOnboardingUseCase

class RestoreFromDriveUseCase(
    private val driveSyncRepository: DriveSyncRepository,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) {
    suspend operator fun invoke(context: Context): Result<Boolean> {
        return driveSyncRepository.downloadFromAppData(context).also { result ->
            if (result.isSuccess && result.getOrThrow()) {
                completeOnboardingUseCase()
            }
        }
    }
}
