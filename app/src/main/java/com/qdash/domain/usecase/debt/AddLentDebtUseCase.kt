package com.qdash.domain.usecase.debt

import com.qdash.domain.model.*
import com.qdash.domain.repository.DebtRepository
import com.qdash.domain.repository.TransactionRepository

class AddLentDebtUseCase(
    private val debtRepository: DebtRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        title: String,
        debtorName: String,
        totalAmount: Double,
        linkedAccountId: Long,
        dueDate: Long? = null,
        notes: String? = null,
        color: String = "#10B981",
        createdAt: Long = System.currentTimeMillis()
    ): Long {
        // 1. Create withdrawal transaction (EXPENSE) from the lending account
        val noteText = if (notes.isNullOrBlank()) "إقراض / سلفة: $title ($debtorName)" else "$notes [سلفة: $title]"
        val txId = transactionRepository.insertTransaction(
            Transaction(
                amount = totalAmount,
                type = TransactionType.EXPENSE,
                categoryId = DebtConstants.DEBT_EXPENSE_CATEGORY_ID,
                accountId = linkedAccountId,
                note = noteText,
                date = createdAt
            )
        )

        // 2. Create Debt entity with direction OWED_TO_ME
        val debt = RegularDebt(
            title = title,
            creditorName = debtorName,
            totalAmount = totalAmount,
            remainingAmount = totalAmount,
            dueDate = dueDate,
            linkedAccountId = linkedAccountId,
            notes = notes,
            color = color,
            icon = "payments",
            createdAt = createdAt,
            isClosed = false,
            direction = DebtDirection.OWED_TO_ME,
            initialTransactionId = txId
        )

        return debtRepository.insertDebt(debt)
    }
}
