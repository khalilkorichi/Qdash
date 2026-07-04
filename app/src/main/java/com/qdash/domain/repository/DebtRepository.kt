package com.qdash.domain.repository

import com.qdash.domain.model.Debt
import com.qdash.domain.model.DebtPayment
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun getAllDebts(): Flow<List<Debt>>
    fun getActiveDebts(): Flow<List<Debt>>
    suspend fun getDebtById(id: Long): Debt?
    suspend fun insertDebt(debt: Debt): Long
    suspend fun updateDebt(debt: Debt)
    suspend fun deleteDebt(debt: Debt)

    fun getAllPayments(): Flow<List<DebtPayment>>
    fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPayment>>
    suspend fun insertPayment(payment: DebtPayment): Long
    suspend fun deletePayment(payment: DebtPayment)
    suspend fun deletePaymentsForDebt(debtId: Long)
}
