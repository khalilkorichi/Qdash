package com.example.domain.model

data class CategorySuggestion(
    val suggestedCategoryId: Long?,
    val suggestionSource: SuggestionSource,
    val confidenceScore: Float,
    val userAcceptedSuggestion: Boolean = false,
    val newCategoryName: String? = null,
    val newCategoryType: String? = null,
    val newCategoryColor: String? = null,
    val newCategoryIcon: String? = null
)

enum class SuggestionSource {
    RULE, HISTORY, AI, NONE
}
