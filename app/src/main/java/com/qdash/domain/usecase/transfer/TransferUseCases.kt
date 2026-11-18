package com.qdash.domain.usecase.transfer

import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TransferBetweenAccountsUseCase(
    private val transferRepository: TransferRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(request: TransferRequest): Boolean {
        if (request.fromAccountId == request.toAccountId || request.amount <= 0.0) return false
        val fromAccount = accountRepository.getAccountById(request.fromAccountId) ?: return false
        val toAccount = accountRepository.getAccountById(request.toAccountId) ?: return false
        
        val neededAmount = request.amount + (request.feeAmount ?: 0.0)
        if (fromAccount.balance < neededAmount) {
            return false // Insufficient funds
        }
        
        val referenceId = "TRF-${UUID.randomUUID().toString().take(6).uppercase()}"
        val occurredAt = request.occurredAt ?: System.currentTimeMillis()
        
        // 1. Atomic mirrored TRANSFER transaction (does NOT pollute income/expense analytics)
        transactionRepository.insertTransaction(
            Transaction(
                amount = request.amount,
                type = TransactionType.TRANSFER,
                categoryId = null,
                accountId = request.fromAccountId,
                toAccountId = request.toAccountId,
                note = request.note?.takeIf { it.isNotBlank() } ?: "تحويل رصيد: ${fromAccount.name} ← ${toAccount.name}",
                date = request.date,
                kind = TransactionKind.TRANSFER,
                occurredAt = occurredAt
            )
        )
        
        // 2. Optional transfer fee from source account (categoryId = null avoids magic-ID FK crashes)
        val fee = request.feeAmount
        if (fee != null && fee > 0) {
            transactionRepository.insertTransaction(
                Transaction(
                    amount = fee,
                    type = TransactionType.EXPENSE,
                    categoryId = null,
                    accountId = request.fromAccountId,
                    note = "رسوم عملية تحويل ($referenceId)",
                    date = request.date,
                    kind = TransactionKind.EXPENSE,
                    occurredAt = occurredAt
                )
            )
        }
        
        // 3. Save transfer record
        val transferRecord = TransferRecord(
            fromAccountId = request.fromAccountId,
            toAccountId = request.toAccountId,
            amount = request.amount,
            feeAmount = request.feeAmount,
            note = request.note,
            date = request.date,
            referenceId = referenceId,
            occurredAt = occurredAt
        )
        transferRepository.insertTransfer(transferRecord)
        return true
    }
}


class GetTransfersUseCase(
    private val transferRepository: TransferRepository
) {
    operator fun invoke(): Flow<List<TransferRecord>> {
        return transferRepository.getAllTransfers()
    }
}

class ValidateTransferUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(fromAccountId: Long, amount: Double, feeAmount: Double?): Boolean {
        val account = accountRepository.getAccountById(fromAccountId) ?: return false
        val totalNeeded = amount + (feeAmount ?: 0.0)
        return account.balance >= totalNeeded
    }
}
