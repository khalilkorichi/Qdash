package com.example.domain.usecase.budget

import com.example.domain.model.BudgetGoal
import com.example.domain.repository.BudgetGoalRepository
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
