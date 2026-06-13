package com.example.domain.repository

import com.example.domain.model.BudgetGoal
import kotlinx.coroutines.flow.Flow

interface BudgetGoalRepository {
    fun getAllBudgetGoals(): Flow<List<BudgetGoal>>
    fun getActiveBudgetGoals(): Flow<List<BudgetGoal>>
    suspend fun getBudgetGoalById(id: Long): BudgetGoal?
    suspend fun insertBudgetGoal(budgetGoal: BudgetGoal): Long
    suspend fun updateBudgetGoal(budgetGoal: BudgetGoal)
    suspend fun deleteBudgetGoal(budgetGoal: BudgetGoal)
    suspend fun updateSpentAmount(id: Long, spentAmount: Double)
}
