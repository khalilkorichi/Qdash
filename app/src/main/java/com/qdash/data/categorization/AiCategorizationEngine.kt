package com.qdash.data.categorization

import com.qdash.domain.model.CategorySuggestion

interface AiCategorizationEngine : CategorizationEngine {
    // Placeholder interface for optional advanced AI features
}

class FakeAiCategorizationEngine : AiCategorizationEngine {
    override suspend fun suggestCategory(title: String, amount: Double?, accountId: Long?): CategorySuggestion {
        // Placeholder returning no suggestion with 0 confidence
        return CategorySuggestion(
            suggestedCategoryId = null,
            suggestionSource = com.qdash.domain.model.SuggestionSource.NONE,
            confidenceScore = 0.0f
        )
    }
}
