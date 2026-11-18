package com.qdash.domain.usecase.salary

import com.qdash.domain.model.*
import com.qdash.domain.repository.SalaryDistributionRepository
import com.qdash.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first

class SaveSalaryDistributionUseCase(
    private val repository: SalaryDistributionRepository,
    private val categoryRepository: CategoryRepository
) {
    /**
     * Save or update distribution settings, then create/update the 3 envelopes.
     * @param salaryAmount Current salary amount, used to calculate envelope allocations.
     */
    suspend operator fun invoke(
        salaryId: Long,
        isEnabled: Boolean,
        needsPercentage: Int,
        wantsPercentage: Int,
        savingsPercentage: Int,
        salaryAmount: Double
    ) {
        require(needsPercentage + wantsPercentage + savingsPercentage == 100) {
            "Percentages must sum to 100"
        }

        val existing = repository.getDistributionForSalaryOnce(salaryId)
        val now = System.currentTimeMillis()

        val distributionId = if (existing != null) {
            val updated = existing.copy(
                isEnabled = isEnabled,
                needsPercentage = needsPercentage,
                wantsPercentage = wantsPercentage,
                savingsPercentage = savingsPercentage,
                updatedAt = now
            )
            repository.updateDistribution(updated)
            existing.id
        } else {
            repository.insertDistribution(
                SalaryDistribution(
                    salaryId = salaryId,
                    isEnabled = isEnabled,
                    needsPercentage = needsPercentage,
                    wantsPercentage = wantsPercentage,
                    savingsPercentage = savingsPercentage,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        // Fetch all categories for smart auto-mapping (supports both legacy & Algerian seeded names)
        val allCategories = categoryRepository.getAllCategories().first()
        val needsKeywords = listOf("إيجار", "كهرباء", "ماء", "إنترنت", "فواتير", "بقالة", "مارشي", "تاكسي", "حافلة", "مواصلات", "وقود", "صحة", "صيدلية", "عيادة", "عائلي", "تعليم", "البريد", "الذهبية")
        val wantsKeywords = listOf("مطاعم", "أكل سريع", "قهوة", "ملابس", "تسوق", "كوسميتيك", "عناية", "ألعاب", "ترفيه", "مناسبات", "رمضان")
        val savingsKeywords = listOf("ادخار", "توفير", "استثمار")

        // Create or update envelopes
        val existingEnvelopes = repository.getEnvelopesForDistributionOnce(distributionId)
        
        val envelopeConfigs = listOf(
            Triple(EnvelopeType.NEEDS, "احتياجات", needsPercentage) to Pair("#4CAF50", "home"),
            Triple(EnvelopeType.WANTS, "رغبات", wantsPercentage) to Pair("#FF9800", "gamepad"),
            Triple(EnvelopeType.SAVINGS, "ادخار", savingsPercentage) to Pair("#2196F3", "savings")
        )

        val updatedEnvelopes = envelopeConfigs.map { (config, style) ->
            val (type, label, percentage) = config
            val (color, icon) = style
            val allocatedAmount = salaryAmount * percentage / 100.0
            val existingEnvelope = existingEnvelopes.find { it.type == type }

            val linkedCategoryIds = if (existingEnvelope != null) {
                existingEnvelope.linkedCategoryIds
            } else {
                val keywords = when (type) {
                    EnvelopeType.NEEDS -> needsKeywords
                    EnvelopeType.WANTS -> wantsKeywords
                    EnvelopeType.SAVINGS -> savingsKeywords
                }
                allCategories.filter { cat ->
                    cat.type == CategoryType.EXPENSE && keywords.any { kw -> cat.name.contains(kw) }
                }.map { it.id }
            }

            SalaryEnvelope(
                id = existingEnvelope?.id ?: 0,
                distributionId = distributionId,
                type = type,
                label = label,
                percentage = percentage,
                allocatedAmount = allocatedAmount,
                spentAmount = existingEnvelope?.spentAmount ?: 0.0,
                linkedCategoryIds = linkedCategoryIds,
                linkedAccountId = existingEnvelope?.linkedAccountId,
                color = color,
                icon = icon
            )
        }

        repository.saveEnvelopes(updatedEnvelopes)
    }
}
