package com.qdash.domain.usecase.categorization

import com.qdash.data.categorization.CategorizationEngine
import com.qdash.domain.model.CategorySuggestion

class GetCategorySuggestionUseCase(
    private val engine: CategorizationEngine
) {
    suspend operator fun invoke(title: String, amount: Double? = null, accountId: Long? = null): CategorySuggestion {
        return engine.suggestCategory(title, amount, accountId)
    }
}
