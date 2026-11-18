package com.qdash.domain.categorization

import com.qdash.domain.model.CategorySuggestion

interface CategorizationEngine {
    suspend fun suggestCategory(title: String, amount: Double? = null, accountId: Long? = null): CategorySuggestion

    suspend fun suggestCategory(
        title: String,
        amount: Double? = null,
        accountId: Long? = null,
        transactionType: String? = null,
        forceAi: Boolean = false
    ): CategorySuggestion = suggestCategory(title, amount, accountId)
}
