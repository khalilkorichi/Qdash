package com.qdash.domain.usecase.salary

import com.qdash.domain.repository.IncomeRepository

class DeleteSalaryDelayUseCase(
    private val incomeRepository: IncomeRepository
) {
    suspend operator fun invoke(delayId: Long) {
        incomeRepository.deleteSalaryDelay(delayId)
    }
}
