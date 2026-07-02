package com.example.domain.usecase.salary

import com.example.domain.model.*
import com.example.domain.repository.SalaryDistributionRepository

class SaveSalaryDistributionUseCase(
    private val repository: SalaryDistributionRepository
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

            SalaryEnvelope(
                id = existingEnvelope?.id ?: 0,
                distributionId = distributionId,
                type = type,
                label = label,
                percentage = percentage,
                allocatedAmount = allocatedAmount,
                spentAmount = existingEnvelope?.spentAmount ?: 0.0,
                linkedCategoryIds = existingEnvelope?.linkedCategoryIds ?: emptyList(),
                color = color,
                icon = icon
            )
        }

        repository.saveEnvelopes(updatedEnvelopes)
    }
}
