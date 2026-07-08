package com.qdash.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.Account
import com.qdash.domain.model.Amana
import com.qdash.domain.model.Transaction
import com.qdash.domain.repository.AccountRepository
import com.qdash.domain.repository.AmanaRepository
import com.qdash.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountDetailsUiState(
    val account: Account? = null,
    val transactions: List<Transaction> = emptyList(),
    val amanas: List<Amana> = emptyList(),
    val totalAmanaForAccount: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSavingAmana: Boolean = false
) {
    val realBalance: Double
        get() = (account?.balance ?: 0.0) - totalAmanaForAccount
}

class AccountDetailsViewModel(
    private val accountId: Long,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val amanaRepository: AmanaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDetailsUiState())
    val uiState: StateFlow<AccountDetailsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    transactionRepository.getTransactionsByAccount(accountId),
                    amanaRepository.getAmanasByAccount(accountId),
                    amanaRepository.getTotalAmanaAmountForAccount(accountId)
                ) { transactions, amanas, totalAmana ->
                    Triple(transactions, amanas, totalAmana)
                }.collect { (transactions, amanas, totalAmana) ->
                    val account = accountRepository.getAccountById(accountId)
                    _uiState.update {
                        it.copy(
                            account = account,
                            transactions = transactions,
                            amanas = amanas,
                            totalAmanaForAccount = totalAmana,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun addAmana(name: String, ownerName: String, amount: Double, notes: String?) {
        if (name.isBlank() || ownerName.isBlank() || amount <= 0) {
            _uiState.update { it.copy(error = "يرجى تعبئة جميع الحقول بشكل صحيح") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAmana = true) }
            try {
                val amana = Amana(
                    accountId = accountId,
                    name = name.trim(),
                    ownerName = ownerName.trim(),
                    amount = amount,
                    notes = notes?.trim()
                )
                amanaRepository.insertAmana(amana)
                _uiState.update { it.copy(isSavingAmana = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingAmana = false, error = e.localizedMessage) }
            }
        }
    }

    fun deleteAmana(amana: Amana) {
        viewModelScope.launch {
            try {
                amanaRepository.deleteAmana(amana)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
