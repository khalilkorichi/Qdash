package com.qdash.domain.usecase.onboarding

import com.qdash.core.preferences.PreferencesManager

/**
 * Marks onboarding as fully completed.
 * Wraps [PreferencesManager.markOnboardingCompleted] so that
 * ViewModels depend on the domain layer, not the data layer directly.
 */
class CompleteOnboardingUseCase(
    private val preferencesManager: PreferencesManager
) {
    operator fun invoke() {
        preferencesManager.markOnboardingCompleted()
    }
}
