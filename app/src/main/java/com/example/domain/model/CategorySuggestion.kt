package com.example.domain.model

data class CategorySuggestion(
    val suggestedCategoryId: Long?,
    val suggestionSource: SuggestionSource,
    val confidenceScore: Float,
    val userAcceptedSuggestion: Boolean = false
)

enum class SuggestionSource {
    RULE, HISTORY, AI, NONE
}
