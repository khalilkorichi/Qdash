package com.qdash.data.local.dao

import androidx.room.*
import com.qdash.data.local.entities.BudgetGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetGoalDao {
    @Query("""
        SELECT bg.id, bg.title, bg.linkedCategoryId, bg.budgetType, bg.amountLimit,
            COALESCE((
                SELECT SUM(t.amount)
                FROM transactions t
                LEFT JOIN categories c ON t.categoryId = c.id
                WHERE t.type = 'EXPENSE'
                  AND t.kind = 'EXPENSE'
                  AND t.date BETWEEN bg.startDate AND bg.endDate
                  AND (
                      bg.budgetType != 'CATEGORY'
                      OR bg.linkedCategoryId IS NULL
                      OR t.categoryId = bg.linkedCategoryId
                      OR c.parentId = bg.linkedCategoryId
                  )
            ), 0.0) AS spentAmount,
            bg.startDate, bg.endDate, bg.alertThresholdPercent, bg.isActive, bg.color, bg.icon, bg.createdAt
        FROM budget_goals bg
        ORDER BY bg.createdAt DESC
    """)
    fun getAllBudgetGoals(): Flow<List<BudgetGoalEntity>>

    @Query("""
        SELECT bg.id, bg.title, bg.linkedCategoryId, bg.budgetType, bg.amountLimit,
            COALESCE((
                SELECT SUM(t.amount)
                FROM transactions t
                LEFT JOIN categories c ON t.categoryId = c.id
                WHERE t.type = 'EXPENSE'
                  AND t.kind = 'EXPENSE'
                  AND t.date BETWEEN bg.startDate AND bg.endDate
                  AND (
                      bg.budgetType != 'CATEGORY'
                      OR bg.linkedCategoryId IS NULL
                      OR t.categoryId = bg.linkedCategoryId
                      OR c.parentId = bg.linkedCategoryId
                  )
            ), 0.0) AS spentAmount,
            bg.startDate, bg.endDate, bg.alertThresholdPercent, bg.isActive, bg.color, bg.icon, bg.createdAt
        FROM budget_goals bg
        WHERE bg.isActive = 1
        ORDER BY bg.createdAt DESC
    """)
    fun getActiveBudgetGoals(): Flow<List<BudgetGoalEntity>>

    @Query("""
        SELECT bg.id, bg.title, bg.linkedCategoryId, bg.budgetType, bg.amountLimit,
            COALESCE((
                SELECT SUM(t.amount)
                FROM transactions t
                LEFT JOIN categories c ON t.categoryId = c.id
                WHERE t.type = 'EXPENSE'
                  AND t.kind = 'EXPENSE'
                  AND t.date BETWEEN bg.startDate AND bg.endDate
                  AND (
                      bg.budgetType != 'CATEGORY'
                      OR bg.linkedCategoryId IS NULL
                      OR t.categoryId = bg.linkedCategoryId
                      OR c.parentId = bg.linkedCategoryId
                  )
            ), 0.0) AS spentAmount,
            bg.startDate, bg.endDate, bg.alertThresholdPercent, bg.isActive, bg.color, bg.icon, bg.createdAt
        FROM budget_goals bg
        WHERE bg.id = :id
    """)
    suspend fun getBudgetGoalById(id: Long): BudgetGoalEntity?

    @Upsert
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
