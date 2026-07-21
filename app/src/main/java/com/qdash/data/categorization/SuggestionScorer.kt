package com.qdash.data.categorization

import com.qdash.domain.model.SuggestionSource

class SuggestionScorer {
    fun calculateConfidence(source: SuggestionSource, matchScore: Float): Float {
        return when (source) {
            SuggestionSource.HISTORY -> (0.85f + (matchScore * 0.15f)).coerceAtMost(1.0f)
            SuggestionSource.KEYWORD -> (0.80f + (matchScore * 0.15f)).coerceAtMost(1.0f)
            SuggestionSource.RULE -> (0.75f + (matchScore * 0.15f)).coerceAtMost(1.0f)
            SuggestionSource.AI -> (0.60f + (matchScore * 0.30f)).coerceAtMost(1.0f)
            SuggestionSource.NONE -> 0.0f
        }
    }
}
