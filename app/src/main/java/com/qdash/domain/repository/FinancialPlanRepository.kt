package com.qdash.domain.repository

import com.qdash.domain.model.FinancialPlan
import com.qdash.domain.model.FinancialPlanStatus
import kotlinx.coroutines.flow.Flow

interface FinancialPlanRepository {
    fun getAllPlans(): Flow<List<FinancialPlan>>
    fun getActivePlans(): Flow<List<FinancialPlan>>
    suspend fun getPlanById(id: Long): FinancialPlan?
    suspend fun insertPlan(plan: FinancialPlan): Long
    suspend fun updatePlan(plan: FinancialPlan)
    suspend fun deletePlan(plan: FinancialPlan)
    suspend fun updateCurrentAmount(id: Long, amount: Double)
    suspend fun updateStatus(id: Long, status: FinancialPlanStatus)
}
