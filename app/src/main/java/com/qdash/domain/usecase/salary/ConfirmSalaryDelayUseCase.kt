package com.qdash.domain.usecase.salary

import com.qdash.domain.model.*
import com.qdash.domain.repository.IncomeRepository
import com.qdash.domain.repository.SalaryDistributionRepository

class ConfirmSalaryDelayUseCase(
    private val incomeRepository: IncomeRepository,
    private val salaryDistributionRepository: SalaryDistributionRepository
) {
    suspend operator fun invoke(
        salaryId: Long,
        delayDays: Int,
        originalDate: Long,
        newDate: Long,
        severityScore: Int,
        affectedObligations: List<AffectedObligation>
    ) {
        // 1. Confirm the salary delay in the income repository
        incomeRepository.confirmSalaryDelay(
            salaryId = salaryId,
            delayDays = delayDays,
            originalDate = originalDate,
            newDate = newDate,
            severityScore = severityScore,
            affectedObligations = affectedObligations
        )

        // 2. Adjust salary envelopes dynamically if distribution is enabled
        try {
            val distribution = salaryDistributionRepository.getDistributionForSalaryOnce(salaryId)
            if (distribution != null && distribution.isEnabled) {
                val envelopes = salaryDistributionRepository.getEnvelopesForDistributionOnce(distribution.id)
                val wantsEnv = envelopes.find { it.type == EnvelopeType.WANTS }
                val needsEnv = envelopes.find { it.type == EnvelopeType.NEEDS }

                if (wantsEnv != null && needsEnv != null && delayDays >= 5) {
                    // Shift 20% of Wants budget to Needs safety margin
                    val shiftAmount = wantsEnv.allocatedAmount * 0.20
                    val updatedEnvelopes = envelopes.map { env ->
                        when (env.type) {
                            EnvelopeType.WANTS -> env.copy(allocatedAmount = env.allocatedAmount - shiftAmount)
                            EnvelopeType.NEEDS -> env.copy(allocatedAmount = env.allocatedAmount + shiftAmount)
                            else -> env
                        }
                    }
                    salaryDistributionRepository.saveEnvelopes(updatedEnvelopes)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
