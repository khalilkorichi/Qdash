package com.example.domain.usecase.budget

import com.example.domain.model.BudgetGoal
import com.example.domain.repository.BudgetGoalRepository

class AddBudgetGoalUseCase(
    private val budgetGoalRepository: BudgetGoalRepository
) {
    suspend operator fun invoke(budgetGoal: BudgetGoal): Long {
        return budgetGoalRepository.insertBudgetGoal(budgetGoal)
    }
}
