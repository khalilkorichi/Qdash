package com.example.domain.usecase.budget

import com.example.domain.model.BudgetGoal
import com.example.domain.repository.BudgetGoalRepository

class DeleteBudgetGoalUseCase(
    private val budgetGoalRepository: BudgetGoalRepository
) {
    suspend operator fun invoke(budgetGoal: BudgetGoal) {
        budgetGoalRepository.deleteBudgetGoal(budgetGoal)
    }
}
