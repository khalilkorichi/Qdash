package com.qdash.domain.usecase.transaction

import com.qdash.domain.repository.TransactionRepository

data class BulkEditParams(
    val transactionIds: List<Long>,
    val newCategoryId: Long?,       // null = no change
    val newAccountId: Long?         // null = no change
)

class BulkEditTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(params: BulkEditParams): Result<Int> {
        return transactionRepository.bulkEditTransactions(params)
    }
}
