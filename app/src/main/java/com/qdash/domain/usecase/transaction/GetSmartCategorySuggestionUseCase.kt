package com.qdash.domain.usecase.transaction

import com.qdash.domain.categorization.CategorizationEngine
import com.qdash.domain.model.CategorySuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case to get non-blocking, smart category suggestions based on transaction title, amount, and account ID.
 * Runs strictly on Dispatchers.Default.
 */
class GetSmartCategorySuggestionUseCase(
    private val engine: CategorizationEngine
) {
    suspend operator fun invoke(
        title: String,
        amount: Double? = null,
        accountId: Long? = null
    ): CategorySuggestion = withContext(Dispatchers.Default) {
        if (title.isBlank()) {
            return@withContext CategorySuggestion(
                suggestedCategoryId = null,
                suggestionSource = com.qdash.domain.model.SuggestionSource.NONE,
                confidenceScore = 0.0f
            )
        }
        engine.suggestCategory(title, amount, accountId)
    }
}
