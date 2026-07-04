package com.qdash.domain.usecase.categorization

import com.qdash.data.categorization.KeywordMatcher
import com.qdash.data.local.entities.UserCategoryMappingEntity
import com.qdash.domain.repository.CategorizationRepository

class LearnCategoryMappingUseCase(
    private val repository: CategorizationRepository
) {
    private val matcher = KeywordMatcher()

    suspend operator fun invoke(text: String, categoryId: Long) {
        val normalized = matcher.normalize(text)
        if (normalized.isEmpty()) return

        val existing = repository.getMappingByText(normalized)
        if (existing != null) {
            val updated = existing.copy(
                categoryId = categoryId,
                usageCount = existing.usageCount + 1,
                lastUsedAt = System.currentTimeMillis()
            )
            repository.updateMapping(updated)
        } else {
            val newMapping = UserCategoryMappingEntity(
                normalizedText = normalized,
                categoryId = categoryId,
                usageCount = 1,
                lastUsedAt = System.currentTimeMillis()
            )
            repository.insertMapping(newMapping)
        }
    }
}
