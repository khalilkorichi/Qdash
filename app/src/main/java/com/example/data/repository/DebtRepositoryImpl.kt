package com.example.data.repository

import com.example.data.local.dao.DebtDao
import com.example.data.local.dao.DebtPaymentDao
import com.example.domain.model.Debt
import com.example.domain.model.DebtPayment
import com.example.domain.model.toDomain
import com.example.domain.model.toEntity
import com.example.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DebtRepositoryImpl(
    private val debtDao: DebtDao,
    private val debtPaymentDao: DebtPaymentDao
) : DebtRepository {

    override fun getAllDebts(): Flow<List<Debt>> {
        return debtDao.getAllDebts().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveDebts(): Flow<List<Debt>> {
        return debtDao.getActiveDebts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getDebtById(id: Long): Debt? {
        return debtDao.getDebtById(id)?.toDomain()
    }

    override suspend fun insertDebt(debt: Debt): Long {
        return debtDao.insertDebt(debt.toEntity())
    }

    override suspend fun updateDebt(debt: Debt) {
        debtDao.updateDebt(debt.toEntity())
    }

    override suspend fun deleteDebt(debt: Debt) {
        debtDao.deleteDebt(debt.toEntity())
    }

    override fun getAllPayments(): Flow<List<DebtPayment>> {
        return debtPaymentDao.getAllPayments().map { list -> list.map { it.toDomain() } }
    }

    override fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPayment>> {
        return debtPaymentDao.getPaymentsForDebt(debtId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertPayment(payment: DebtPayment): Long {
        return debtPaymentDao.insertPayment(payment.toEntity())
    }

    override suspend fun deletePayment(payment: DebtPayment) {
        debtPaymentDao.deletePayment(payment.toEntity())
    }

    override suspend fun deletePaymentsForDebt(debtId: Long) {
        debtPaymentDao.deletePaymentsForDebt(debtId)
    }
}
