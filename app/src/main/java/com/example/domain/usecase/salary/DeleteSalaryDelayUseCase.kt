package com.example.domain.usecase.salary

import com.example.domain.repository.IncomeRepository

class DeleteSalaryDelayUseCase(
    private val incomeRepository: IncomeRepository
) {
    suspend operator fun invoke(delayId: Long) {
        incomeRepository.deleteSalaryDelay(delayId)
    }
}
