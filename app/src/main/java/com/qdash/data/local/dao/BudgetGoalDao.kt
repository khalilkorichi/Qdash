package com.qdash.data.local.dao

import androidx.room.*
import com.qdash.data.local.entities.BudgetGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetGoalDao {
    @Query("SELECT * FROM budget_goals ORDER BY createdAt DESC")
    fun getAllBudgetGoals(): Flow<List<BudgetGoalEntity>>

    @Query("SELECT * FROM budget_goals WHERE isActive = 1")
    fun getActiveBudgetGoals(): Flow<List<BudgetGoalEntity>>

    @Query("SELECT * FROM budget_goals WHERE id = :id")
    suspend fun getBudgetGoalById(id: Long): BudgetGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetGoal(budgetGoal: BudgetGoalEntity): Long

    @Update
    suspend fun updateBudgetGoal(budgetGoal: BudgetGoalEntity)

    @Delete
    suspend fun deleteBudgetGoal(budgetGoal: BudgetGoalEntity)

    @Query("UPDATE budget_goals SET spentAmount = :spentAmount WHERE id = :id")
    suspend fun updateSpentAmount(id: Long, spentAmount: Double)

    @Query("UPDATE budget_goals SET linkedCategoryId = :targetCategoryId WHERE linkedCategoryId = :sourceCategoryId")
    suspend fun mergeBudgetGoalsCategory(sourceCategoryId: Long, targetCategoryId: Long)
}
