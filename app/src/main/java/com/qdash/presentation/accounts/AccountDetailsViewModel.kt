package com.qdash.presentation.accounts

import androidx.compose.runtime.Immutable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.Account
import com.qdash.domain.model.Amana
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.Category
import com.qdash.domain.repository.AccountRepository
import com.qdash.domain.repository.AmanaRepository
import com.qdash.domain.repository.TransactionRepository
import com.qdash.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AccountDetailsUiState(
    val account: Account? = null,
    val transactions: List<Transaction> = emptyList(),
    val amanas: List<Amana> = emptyList(),
    val totalAmanaForAccount: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
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
    private val amanaRepository: AmanaRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDetailsUiState())
    val uiState: StateFlow<AccountDetailsUiState> = _uiState.asStateFlow()

    private data class CombinedData(
        val transactions: List<Transaction>,
        val amanas: List<Amana>,
        val totalAmana: Double,
        val categories: List<Category>,
        val accounts: List<Account>
    )

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    transactionRepository.getTransactionsByAccount(accountId),
                    amanaRepository.getAmanasByAccount(accountId),
                    amanaRepository.getTotalAmanaAmountForAccount(accountId),
                    categoryRepository.getAllCategories(),
                    accountRepository.getAllAccounts()
                ) { flowArray ->
                    @Suppress("UNCHECKED_CAST")
                    CombinedData(
                        transactions = flowArray[0] as List<Transaction>,
                        amanas = flowArray[1] as List<Amana>,
                        totalAmana = flowArray[2] as Double,
                        categories = flowArray[3] as List<Category>,
                        accounts = flowArray[4] as List<Account>
                    )
                }.collect { data ->
                    val account = accountRepository.getAccountById(accountId)
                    _uiState.update {
                        it.copy(
                            account = account,
                            transactions = data.transactions,
                            amanas = data.amanas,
                            totalAmanaForAccount = data.totalAmana,
                            categories = data.categories,
                            accounts = data.accounts,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
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
