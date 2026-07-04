package com.qdash.domain.usecase.budget

import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.repository.BudgetGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetBudgetGoalsUseCase(
    private val budgetGoalRepository: BudgetGoalRepository,
    private val calculateBudgetSpentUseCase: CalculateBudgetSpentUseCase
) {
    operator fun invoke(): Flow<List<BudgetGoal>> {
        return budgetGoalRepository.getAllBudgetGoals().map { list ->
            list.map { goal ->
                val recalculatedSpent = calculateBudgetSpentUseCase(goal)
                goal.copy(spentAmount = recalculatedSpent)
            }
        }
    }
}
