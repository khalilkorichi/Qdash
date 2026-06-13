package com.example.data.repository

import com.example.data.local.dao.BudgetGoalDao
import com.example.domain.model.BudgetGoal
import com.example.domain.model.toDomain
import com.example.domain.model.toEntity
import com.example.domain.repository.BudgetGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetGoalRepositoryImpl(
    private val budgetGoalDao: BudgetGoalDao
) : BudgetGoalRepository {

    override fun getAllBudgetGoals(): Flow<List<BudgetGoal>> {
        return budgetGoalDao.getAllBudgetGoals().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveBudgetGoals(): Flow<List<BudgetGoal>> {
        return budgetGoalDao.getActiveBudgetGoals().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getBudgetGoalById(id: Long): BudgetGoal? {
        return budgetGoalDao.getBudgetGoalById(id)?.toDomain()
    }

    override suspend fun insertBudgetGoal(budgetGoal: BudgetGoal): Long {
        return budgetGoalDao.insertBudgetGoal(budgetGoal.toEntity())
    }

    override suspend fun updateBudgetGoal(budgetGoal: BudgetGoal) {
        budgetGoalDao.updateBudgetGoal(budgetGoal.toEntity())
    }

    override suspend fun deleteBudgetGoal(budgetGoal: BudgetGoal) {
        budgetGoalDao.deleteBudgetGoal(budgetGoal.toEntity())
    }

    override suspend fun updateSpentAmount(id: Long, spentAmount: Double) {
        budgetGoalDao.updateSpentAmount(id, spentAmount)
    }
}
