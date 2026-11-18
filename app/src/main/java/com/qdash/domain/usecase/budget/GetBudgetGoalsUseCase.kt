package com.qdash.domain.usecase.budget

import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.repository.BudgetGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetBudgetGoalsUseCase(
    private val budgetGoalRepository: BudgetGoalRepository,
    @Suppress("unused") private val calculateBudgetSpentUseCase: CalculateBudgetSpentUseCase
) {
    operator fun invoke(): Flow<List<BudgetGoal>> {
        // spentAmount is computed reactively inside SQLite (including child subcategories)
        // without N+1 full-table loads or feedback write-loops.
        return budgetGoalRepository.getAllBudgetGoals()
    }
}
