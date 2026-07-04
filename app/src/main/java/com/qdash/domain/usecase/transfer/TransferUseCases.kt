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
        val fromAccount = accountRepository.getAccountById(request.fromAccountId) ?: return false
        val toAccount = accountRepository.getAccountById(request.toAccountId) ?: return false
        
        val neededAmount = request.amount + (request.feeAmount ?: 0.0)
        if (fromAccount.balance < neededAmount) {
            return false // Insufficient funds
        }
        
        val referenceId = "TRF-${UUID.randomUUID().toString().take(6).uppercase()}"
        
        // 1. Transaction corresponding to reduction from source account
        transactionRepository.insertTransaction(
            Transaction(
                amount = request.amount,
                type = TransactionType.EXPENSE,
                categoryId = 12L, // other
                accountId = request.fromAccountId,
                toAccountId = request.toAccountId,
                note = request.note ?: "تحويل رصيد إلى ${toAccount.name}",
                date = request.date
            )
        )
        
        // 2. Transaction corresponding to amplification on destination account
        transactionRepository.insertTransaction(
            Transaction(
                amount = request.amount,
                type = TransactionType.INCOME,
                categoryId = 12L, // other
                accountId = request.toAccountId,
                note = request.note ?: "استلام رصيد محول من ${fromAccount.name}",
                date = request.date
            )
        )
        
        // 3. Optional transfer fee from source account
        val fee = request.feeAmount
        if (fee != null && fee > 0) {
            transactionRepository.insertTransaction(
                Transaction(
                    amount = fee,
                    type = TransactionType.EXPENSE,
                    categoryId = 5L, // bills / fees
                    accountId = request.fromAccountId,
                    note = "رسوم عملية تحويل ($referenceId)",
                    date = request.date
                )
            )
        }
        
        // 4. Save transfer record
        val transferRecord = TransferRecord(
            fromAccountId = request.fromAccountId,
            toAccountId = request.toAccountId,
            amount = request.amount,
            feeAmount = request.feeAmount,
            note = request.note,
            date = request.date,
            referenceId = referenceId
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
