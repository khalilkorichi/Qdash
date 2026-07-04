package com.qdash.domain.usecase.budget

import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.repository.BudgetGoalRepository

class DeleteBudgetGoalUseCase(
    private val budgetGoalRepository: BudgetGoalRepository
) {
    suspend operator fun invoke(budgetGoal: BudgetGoal) {
        budgetGoalRepository.deleteBudgetGoal(budgetGoal)
    }
}
