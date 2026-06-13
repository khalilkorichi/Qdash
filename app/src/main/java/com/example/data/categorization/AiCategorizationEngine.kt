package com.example.data.categorization

import com.example.domain.model.CategorySuggestion

interface AiCategorizationEngine : CategorizationEngine {
    // Placeholder interface for optional advanced AI features
}

class FakeAiCategorizationEngine : AiCategorizationEngine {
    override suspend fun suggestCategory(title: String, amount: Double?, accountId: Long?): CategorySuggestion {
        // Placeholder returning no suggestion with 0 confidence
        return CategorySuggestion(
            suggestedCategoryId = null,
            suggestionSource = com.example.domain.model.SuggestionSource.NONE,
            confidenceScore = 0.0f
        )
    }
}
