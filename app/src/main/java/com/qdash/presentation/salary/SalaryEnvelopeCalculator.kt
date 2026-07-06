package com.qdash.presentation.salary

import com.qdash.domain.model.EnvelopeType

/**
 * Pure calculator for adjusting envelope distribution percentages to sum up to 100.
 * Extracted from SalaryViewModel to keep the ViewModel under the SIZE-001 500-line limit.
 */
object SalaryEnvelopeCalculator {

    data class EnvelopePercentages(
        val needs: Int,
        val wants: Int,
        val savings: Int
    )

    /**
     * Updates the percentage of a given envelope type and adjusts the other two to maintain sum = 100.
     */
    fun calculateAdjustedPercentages(
        type: EnvelopeType,
        newPercentage: Int,
        currentNeeds: Int,
        currentWants: Int,
        currentSavings: Int
    ): EnvelopePercentages {
        val clamped = newPercentage.coerceIn(0, 100)
        val remaining = 100 - clamped

        val (needs, wants, savings) = when (type) {
            EnvelopeType.NEEDS -> {
                val wantsRatio = if (currentWants + currentSavings > 0) {
                    currentWants.toFloat() / (currentWants + currentSavings)
                } else 0.5f
                val newWants = (remaining * wantsRatio).toInt()
                val newSavings = remaining - newWants
                Triple(clamped, newWants, newSavings)
            }
            EnvelopeType.WANTS -> {
                val needsRatio = if (currentNeeds + currentSavings > 0) {
                    currentNeeds.toFloat() / (currentNeeds + currentSavings)
                } else 0.5f
                val newNeeds = (remaining * needsRatio).toInt()
                val newSavings = remaining - newNeeds
                Triple(newNeeds, clamped, newSavings)
            }
            EnvelopeType.SAVINGS -> {
                val needsRatio = if (currentNeeds + currentWants > 0) {
                    currentNeeds.toFloat() / (currentNeeds + currentWants)
                } else 0.5f
                val newNeeds = (remaining * needsRatio).toInt()
                val newWants = remaining - newNeeds
                Triple(newNeeds, newWants, clamped)
            }
        }

        return EnvelopePercentages(needs, wants, savings)
    }
}
