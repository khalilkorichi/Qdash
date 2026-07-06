// fixtures/good_viewmodel.kt
// GOOD: Clean ViewModel — should produce ZERO architecture/threading issues
package com.qdash.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.repository.TransactionRepository
import com.qdash.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoodTransactionsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _transactions.value = transactionRepository.getAllTransactions()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTransaction(amount: Double, categoryId: Long, note: String) {
        viewModelScope.launch {
            transactionRepository.addTransaction(amount, categoryId, note)
            loadTransactions()
        }
    }
}
