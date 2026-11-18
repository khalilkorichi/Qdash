package com.qdash.domain.usecase.categorization

import com.qdash.domain.categorization.CategorizationEngine
import com.qdash.domain.model.CategorySuggestion

class GetCategorySuggestionUseCase(
    private val engine: CategorizationEngine
) {
    suspend operator fun invoke(
        title: String,
        amount: Double? = null,
        accountId: Long? = null,
        transactionType: String? = null,
        forceAi: Boolean = false
    ): CategorySuggestion {
        return engine.suggestCategory(title, amount, accountId, transactionType, forceAi)
    }
}
