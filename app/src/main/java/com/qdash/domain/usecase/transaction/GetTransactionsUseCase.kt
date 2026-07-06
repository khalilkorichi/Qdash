package com.qdash.domain.usecase.transaction

import com.qdash.domain.model.Transaction
import com.qdash.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(private val transactionRepository: TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> {
        return transactionRepository.getAllTransactions()
    }
}
