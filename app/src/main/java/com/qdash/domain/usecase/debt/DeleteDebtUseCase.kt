package com.qdash.domain.usecase.debt

import com.qdash.domain.repository.DebtRepository
import com.qdash.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

class DeleteDebtUseCase(
    private val debtRepository: DebtRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(debtId: Long): Result<Unit> {
        val debt = debtRepository.getDebtById(debtId)
            ?: return Result.failure(IllegalArgumentException("الدين غير موجود في قاعدة البيانات!"))

        // 1. Get all payments for this debt
        val payments = debtRepository.getPaymentsForDebt(debtId).first()

        // 2. Loop through payments and delete linked transactions to rollback account balances
        payments.forEach { payment ->
            payment.linkedTransactionId?.let { txId ->
                transactionRepository.deleteTransactionById(txId)
            }
        }

        // 3. Delete all payment records of this debt
        debtRepository.deletePaymentsForDebt(debtId)

        // 4. Delete the debt itself
        debtRepository.deleteDebt(debt)

        return Result.success(Unit)
    }
}
