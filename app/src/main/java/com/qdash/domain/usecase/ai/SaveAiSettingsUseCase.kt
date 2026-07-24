package com.qdash.domain.usecase.ai

import com.qdash.core.preferences.PreferencesManager

class SaveAiSettingsUseCase(
    private val preferencesManager: PreferencesManager
) {
    operator fun invoke(modelId: String) {
        preferencesManager.selectedAiModel = modelId
    }
}
