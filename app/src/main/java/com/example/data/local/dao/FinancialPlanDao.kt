package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.FinancialPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialPlanDao {
    @Query("SELECT * FROM financial_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<FinancialPlanEntity>>

    @Query("SELECT * FROM financial_plans WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActivePlans(): Flow<List<FinancialPlanEntity>>

    @Query("SELECT * FROM financial_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: Long): FinancialPlanEntity?

    @Query("SELECT * FROM financial_plans WHERE type = :type ORDER BY createdAt DESC")
    fun getPlansByType(type: String): Flow<List<FinancialPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: FinancialPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: FinancialPlanEntity)

    @Delete
    suspend fun deletePlan(plan: FinancialPlanEntity)

    @Query("UPDATE financial_plans SET currentAmount = :amount WHERE id = :id")
    suspend fun updateCurrentAmount(id: Long, amount: Double)

    @Query("UPDATE financial_plans SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
