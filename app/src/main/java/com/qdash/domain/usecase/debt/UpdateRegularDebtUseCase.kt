package com.qdash.domain.usecase.debt

import com.qdash.domain.model.DebtDirection
import com.qdash.domain.model.RegularDebt
import com.qdash.domain.repository.DebtRepository
import com.qdash.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

class UpdateRegularDebtUseCase(
    private val debtRepository: DebtRepository,
    private val transactionRepository: TransactionRepository? = null
) {
    suspend operator fun invoke(
        debtId: Long,
        title: String,
        creditorName: String,
        totalAmount: Double,
        dueDate: Long?,
        linkedAccountId: Long?,
        notes: String?,
        color: String,
        icon: String
    ): Result<Unit> {
        val existingDebt = debtRepository.getDebtById(debtId)
            ?: return Result.failure(IllegalArgumentException("الدين غير موجود في قاعدة البيانات!"))

        if (existingDebt !is RegularDebt) {
            return Result.failure(IllegalArgumentException("هذا الالتزام ليس ديناً عادياً!"))
        }

        // Fetch payments to calculate what has already been paid / collected
        val payments = debtRepository.getPaymentsForDebt(debtId).first()
        val totalPaid = payments.sumOf { it.amount }

        if (totalAmount < totalPaid) {
            return Result.failure(
                IllegalArgumentException("المبلغ الإجمالي الجديد ($totalAmount د.ج) لا يمكن أن يكون أقل من إجمالي المبالغ المدفوعة سابقاً ($totalPaid د.ج)!")
            )
        }

        val resolvedAccountId = if (existingDebt.direction == DebtDirection.OWED_TO_ME && existingDebt.initialTransactionId != null) {
            linkedAccountId ?: existingDebt.linkedAccountId
        } else {
            linkedAccountId
        }

        // Sync initial wallet withdrawal transaction when editing a lent debt (OWED_TO_ME)
        if (transactionRepository != null &&
            existingDebt.direction == DebtDirection.OWED_TO_ME &&
            existingDebt.initialTransactionId != null
        ) {
            val initialTx = transactionRepository.getTransactionById(existingDebt.initialTransactionId)
            if (initialTx != null) {
                val targetAccountId = resolvedAccountId ?: initialTx.accountId
                val updatedNote = if (notes.isNullOrBlank()) {
                    "إقراض / سلفة: $title ($creditorName)"
                } else {
                    "$notes [سلفة: $title]"
                }
                if (initialTx.amount != totalAmount || initialTx.accountId != targetAccountId || initialTx.note != updatedNote) {
                    transactionRepository.updateTransaction(
                        initialTx.copy(
                            amount = totalAmount,
                            accountId = targetAccountId,
                            note = updatedNote
                        )
                    )
                }
            }
        }

        val rawRemaining = maxOf(0.0, totalAmount - totalPaid)
        val isNowClosed = rawRemaining <= 0.005
        val newRemaining = if (isNowClosed) 0.0 else rawRemaining
        val updatedDebt = existingDebt.copy(
            title = title,
            creditorName = creditorName,
            totalAmount = totalAmount,
            remainingAmount = newRemaining,
            dueDate = dueDate,
            linkedAccountId = resolvedAccountId,
            notes = notes,
            color = color,
            icon = icon,
            isClosed = isNowClosed
        )

        debtRepository.updateDebt(updatedDebt)
        return Result.success(Unit)
    }
}
