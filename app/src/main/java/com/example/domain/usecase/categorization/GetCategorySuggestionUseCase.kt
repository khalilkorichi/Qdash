package com.example.domain.usecase.categorization

import com.example.data.categorization.CategorizationEngine
import com.example.domain.model.CategorySuggestion

class GetCategorySuggestionUseCase(
    private val engine: CategorizationEngine
) {
    suspend operator fun invoke(title: String, amount: Double? = null, accountId: Long? = null): CategorySuggestion {
        return engine.suggestCategory(title, amount, accountId)
    }
}
