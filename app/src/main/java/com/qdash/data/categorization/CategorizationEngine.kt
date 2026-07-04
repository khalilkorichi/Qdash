package com.qdash.data.categorization

import com.qdash.domain.model.CategorySuggestion

interface CategorizationEngine {
    suspend fun suggestCategory(title: String, amount: Double? = null, accountId: Long? = null): CategorySuggestion
}
