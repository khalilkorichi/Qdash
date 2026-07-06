package com.qdash.domain.usecase.transaction

import com.qdash.domain.model.Account
import com.qdash.domain.model.AccountType
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType

data class TransactionFilterParams(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val filterLargeOnly: Boolean = false,
    val filterBaridiMobOnly: Boolean = false,
    val filterMinAmount: Double? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null,
    val selectedCalendarDate: Long? = null
)

class FilterTransactionsUseCase {
    operator fun invoke(
        transactions: List<Transaction>,
        accounts: List<Account>,
        params: TransactionFilterParams
    ): List<Transaction> {
        return transactions.filter { tx ->
            val matchesQuery = params.query.isBlank() || tx.note?.contains(params.query, ignoreCase = true) == true
            val matchesType = params.type == null || tx.type == params.type
            val matchesCat = params.categoryId == null || tx.categoryId == params.categoryId
            val matchesAcc = params.accountId == null || tx.accountId == params.accountId
            val matchesCalendarDate = params.selectedCalendarDate?.let { sel -> tx.date >= sel && tx.date < sel + 86400000L } ?: true
            val matchesLarge = !params.filterLargeOnly || tx.amount >= 10000.0
            val matchesBaridi = !params.filterBaridiMobOnly || run {
                val acc = accounts.find { it.id == tx.accountId }
                acc?.type == AccountType.BARIDIMOB
            }
            val matchesMinAmount = params.filterMinAmount == null || tx.amount >= params.filterMinAmount
            val matchesStartDate = params.filterStartDate == null || tx.date >= params.filterStartDate
            val matchesEndDate = params.filterEndDate == null || tx.date <= params.filterEndDate

            matchesQuery && matchesType && matchesCat && matchesAcc && matchesCalendarDate && matchesLarge && matchesBaridi && matchesMinAmount && matchesStartDate && matchesEndDate
        }
    }
}
