package com.qdash.domain.usecase.debt

import com.qdash.domain.model.copyDebt
import com.qdash.domain.repository.DebtRepository
import com.qdash.domain.repository.TransactionRepository

class CancelDebtPaymentUseCase(
    private val debtRepository: DebtRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(paymentId: Long): Result<Unit> {
        // 1. Fetch payment by ID
        val payment = debtRepository.getPaymentById(paymentId)
            ?: return Result.failure(IllegalArgumentException("دفعة السداد غير موجودة في قاعدة البيانات!"))

        // 2. Fetch associated debt
        val debt = debtRepository.getDebtById(payment.debtId)
            ?: return Result.failure(IllegalArgumentException("الدين المرتبط بهذه الدفعة غير موجود!"))

        // 3. Rollback the expense transaction from the wallet
        payment.linkedTransactionId?.let { txId ->
            transactionRepository.deleteTransactionById(txId)
        }

        // 4. Update the remaining amount of the debt and activate it if it was closed
        val newRemaining = debt.remainingAmount + payment.amount
        val updatedDebt = debt.copyDebt(
            remainingAmount = newRemaining,
            isClosed = false // If it was closed, re-opening it is required
        )
        debtRepository.updateDebt(updatedDebt)

        // 5. Delete the payment record
        debtRepository.deletePayment(payment)

        return Result.success(Unit)
    }
}
