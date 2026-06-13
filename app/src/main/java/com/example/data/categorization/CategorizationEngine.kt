package com.example.data.categorization

import com.example.domain.model.CategorySuggestion

interface CategorizationEngine {
    suspend fun suggestCategory(title: String, amount: Double? = null, accountId: Long? = null): CategorySuggestion
}
