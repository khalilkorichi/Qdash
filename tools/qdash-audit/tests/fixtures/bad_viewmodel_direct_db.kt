// fixtures/bad_viewmodel_direct_db.kt
// BAD: ViewModel accessing DAO directly — triggers ARCH-003
package com.qdash.presentation.transactions

import com.qdash.data.local.AppDatabase
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class TransactionsViewModel(private val database: AppDatabase) : ViewModel() {

    fun loadTransactions() {
        CoroutineScope(Dispatchers.IO).launch {
            // BAD: direct DAO call from ViewModel
            val transactions = database.transactionDao().getAllTransactions()
        }
    }

    fun saveTransaction(amount: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            // BAD: direct DAO call from ViewModel
            database.transactionDao().insertTransaction(
                com.qdash.data.local.entities.TransactionEntity(
                    id = 0, amount = amount, type = "EXPENSE", accountId = 1,
                    categoryId = 1, note = "", date = System.currentTimeMillis()
                )
            )
        }
    }
}
