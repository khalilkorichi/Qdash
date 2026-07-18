package com.qdash.domain.usecase.onboarding

import com.qdash.core.preferences.PreferencesManager

/**
 * Returns whether the user has already completed onboarding.
 * Used at startup to determine the initial navigation destination.
 *
 * Returns `true`  → user is returning; route to Home.
 * Returns `false` → new user; route to Onboarding.
 */
class GetOnboardingStateUseCase(
    private val preferencesManager: PreferencesManager
) {
    operator fun invoke(): Boolean = !preferencesManager.isFirstLaunch
}
