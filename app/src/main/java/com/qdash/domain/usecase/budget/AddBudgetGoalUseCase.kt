package com.qdash.domain.usecase.budget

import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.repository.BudgetGoalRepository

class AddBudgetGoalUseCase(
    private val budgetGoalRepository: BudgetGoalRepository
) {
    suspend operator fun invoke(budgetGoal: BudgetGoal): Long {
        return budgetGoalRepository.insertBudgetGoal(budgetGoal)
    }
}
