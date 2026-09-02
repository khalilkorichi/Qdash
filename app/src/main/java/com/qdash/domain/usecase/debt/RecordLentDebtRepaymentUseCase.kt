package com.qdash.domain.usecase.debt

import com.qdash.domain.model.*
import com.qdash.domain.repository.DebtRepository
import com.qdash.domain.repository.TransactionRepository

class RecordLentDebtRepaymentUseCase(
    private val debtRepository: DebtRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        debtId: Long,
        receivingAccountId: Long,
        amount: Double,
        note: String? = null,
        paymentDate: Long = System.currentTimeMillis()
    ): Long {
        val debt = debtRepository.getDebtById(debtId) ?: return -1L
        if (debt.direction != DebtDirection.OWED_TO_ME) {
            return -1L
        }

        // 1. Create deposit transaction (INCOME) into the receiving account
        val repaymentNote = if (note.isNullOrBlank()) "استرداد سلفة: ${debt.title} من ${debt.creditorName}" else "$note [استرداد: ${debt.title}]"
        val txId = transactionRepository.insertTransaction(
            Transaction(
                amount = amount,
                type = TransactionType.INCOME,
                categoryId = 15L, // Category "أخرى" (INCOME)
                accountId = receivingAccountId,
                note = repaymentNote,
                date = paymentDate
            )
        )

        // 2. Record DebtPayment
        val payment = DebtPayment(
            debtId = debtId,
            accountId = receivingAccountId,
            amount = amount,
            paymentDate = paymentDate,
            paymentType = DebtPaymentType.MANUAL,
            note = note,
            linkedTransactionId = txId
        )
        val paymentId = debtRepository.insertPayment(payment)

        // 3. Update remaining amount & close if completed
        val newRemaining = maxOf(0.0, debt.remainingAmount - amount)
        val updatedDebt = debt.copyDebt(
            remainingAmount = newRemaining,
            isClosed = newRemaining <= 0.0
        )
        debtRepository.updateDebt(updatedDebt)

        return paymentId
    }
}
