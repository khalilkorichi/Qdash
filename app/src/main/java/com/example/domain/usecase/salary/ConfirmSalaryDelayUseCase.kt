package com.example.domain.usecase.salary

import com.example.domain.model.AffectedObligation
import com.example.domain.repository.IncomeRepository

class ConfirmSalaryDelayUseCase(
    private val incomeRepository: IncomeRepository
) {
    suspend operator fun invoke(
        salaryId: Long,
        delayDays: Int,
        originalDate: Long,
        newDate: Long,
        severityScore: Int,
        affectedObligations: List<AffectedObligation>
    ) {
        incomeRepository.confirmSalaryDelay(
            salaryId = salaryId,
            delayDays = delayDays,
            originalDate = originalDate,
            newDate = newDate,
            severityScore = severityScore,
            affectedObligations = affectedObligations
        )
    }
}
