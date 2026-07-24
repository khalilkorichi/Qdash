package com.qdash.domain.usecase.ai

import com.qdash.core.preferences.PreferencesManager

class GetAiSettingsUseCase(
    private val preferencesManager: PreferencesManager
) {
    operator fun invoke(): String {
        return preferencesManager.selectedAiModel
    }
}
