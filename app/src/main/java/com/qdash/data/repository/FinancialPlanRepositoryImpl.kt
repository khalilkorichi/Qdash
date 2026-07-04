package com.qdash.data.repository

import com.qdash.data.local.dao.FinancialPlanDao
import com.qdash.domain.model.FinancialPlan
import com.qdash.domain.model.FinancialPlanStatus
import com.qdash.domain.model.toDomain
import com.qdash.domain.model.toEntity
import com.qdash.domain.repository.FinancialPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinancialPlanRepositoryImpl(
    private val planDao: FinancialPlanDao
) : FinancialPlanRepository {

    override fun getAllPlans(): Flow<List<FinancialPlan>> {
        return planDao.getAllPlans().map { list -> list.map { it.toDomain() } }
    }

    override fun getActivePlans(): Flow<List<FinancialPlan>> {
        return planDao.getActivePlans().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPlanById(id: Long): FinancialPlan? {
        return planDao.getPlanById(id)?.toDomain()
    }

    override suspend fun insertPlan(plan: FinancialPlan): Long {
        return planDao.insertPlan(plan.toEntity())
    }

    override suspend fun updatePlan(plan: FinancialPlan) {
        planDao.updatePlan(plan.toEntity())
    }

    override suspend fun deletePlan(plan: FinancialPlan) {
        planDao.deletePlan(plan.toEntity())
    }

    override suspend fun updateCurrentAmount(id: Long, amount: Double) {
        planDao.updateCurrentAmount(id, amount)
    }

    override suspend fun updateStatus(id: Long, status: FinancialPlanStatus) {
        planDao.updateStatus(id, status.name)
    }
}
