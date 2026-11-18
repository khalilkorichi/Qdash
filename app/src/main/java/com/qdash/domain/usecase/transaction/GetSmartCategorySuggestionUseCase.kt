package com.qdash.domain.usecase.transaction

import com.qdash.domain.categorization.CategorizationEngine
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.model.SuggestionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case to get non-blocking, smart category suggestions based on transaction title, amount,
 * account ID, and transaction type (EXPENSE / INCOME).
 * Runs strictly on Dispatchers.Default.
 */
class GetSmartCategorySuggestionUseCase(
    private val engine: CategorizationEngine
) {
    suspend operator fun invoke(
        title: String,
        amount: Double? = null,
        accountId: Long? = null,
        transactionType: String? = null,
        forceAi: Boolean = false
    ): CategorySuggestion = withContext(Dispatchers.Default) {
        if (title.isBlank() && !forceAi) {
            return@withContext CategorySuggestion(
                suggestedCategoryId = null,
                suggestionSource = SuggestionSource.NONE,
                confidenceScore = 0.0f
            )
        }
        if (transactionType == null && !forceAi) {
            engine.suggestCategory(title, amount, accountId)
        } else {
            engine.suggestCategory(title, amount, accountId, transactionType, forceAi)
        }
    }
}
