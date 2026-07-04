package com.qdash.domain.usecase.salary

import com.qdash.domain.model.AffectedObligation
import com.qdash.domain.repository.IncomeRepository

class UpdateSalaryDelayUseCase(
    private val incomeRepository: IncomeRepository
) {
    suspend operator fun invoke(
        delayId: Long,
        newDelayDays: Int,
        newDate: Long,
        newSeverityScore: Int,
        affectedObligations: List<AffectedObligation>
    ) {
        incomeRepository.updateSalaryDelay(
            delayId = delayId,
            newDelayDays = newDelayDays,
            newDate = newDate,
            newSeverityScore = newSeverityScore,
            affectedObligations = affectedObligations
        )
    }
}
