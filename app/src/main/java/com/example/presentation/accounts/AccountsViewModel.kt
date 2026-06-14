package com.example.presentation.accounts

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.core.preferences.PreferencesManager

@Immutable
data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val archivedAccounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val deleteError: String? = null,
    val editingAccount: Account? = null,
    val showBalances: Boolean = true,
    val accountBalancesVisibility: Map<Long, Boolean> = emptyMap()
)

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadAccounts()
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleTotalBalanceVisibility() {
        val nextVal = !preferencesManager.showBalanceTotal
        preferencesManager.showBalanceTotal = nextVal
        _uiState.update { it.copy(showBalances = nextVal) }
    }

    fun toggleAccountBalanceVisibility(accountId: Long) {
        val nextVal = !preferencesManager.getShowBalanceAcc(accountId)
        preferencesManager.setShowBalanceAcc(accountId, nextVal)
        _uiState.update { state ->
            val updatedMap = state.accountBalancesVisibility.toMutableMap().apply {
                put(accountId, nextVal)
            }
            state.copy(accountBalancesVisibility = updatedMap)
        }
    }

    private fun loadAccounts() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                combine(
                    accountRepository.getAllAccounts(),
                    accountRepository.getArchivedAccounts(),
                    transactionRepository.getAllTransactions(),
                    categoryRepository.getAllCategories()
                ) { active, archived, txs, cats ->
                    AccountsUiState(
                        accounts = active,
                        archivedAccounts = archived,
                        transactions = txs,
                        categories = cats,
                        showBalances = preferencesManager.showBalanceTotal,
                        accountBalancesVisibility = active.associate { it.id to preferencesManager.getShowBalanceAcc(it.id) },
                        isLoading = false
                    )
                }
                .flowOn(kotlinx.coroutines.Dispatchers.Default)
                .collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun addAccount(name: String, type: AccountType, balance: Double, color: String, icon: String) {
        viewModelScope.launch {
            val account = Account(
                name = name,
                type = type,
                balance = balance,
                color = color,
                icon = icon
            )
            accountRepository.insertAccount(account)
        }
    }

    fun editAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.updateAccount(account)
            _uiState.update { it.copy(editingAccount = null) }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            val txCount = accountRepository.getTransactionCountForAccount(account.id)
            if (txCount > 0) {
                _uiState.update {
                    it.copy(deleteError = "لا يمكن حذف الحساب لأنه يحتوي على $txCount معاملة مالية. يمكنك أرشفته بدلاً من ذلك.")
                }
            } else {
                accountRepository.deleteAccount(account)
            }
        }
    }

    fun archiveAccount(id: Long) {
        viewModelScope.launch {
            accountRepository.archiveAccount(id)
        }
    }

    fun unarchiveAccount(id: Long) {
        viewModelScope.launch {
            accountRepository.unarchiveAccount(id)
        }
    }

    fun setDefaultAccount(id: Long) {
        viewModelScope.launch {
            accountRepository.setDefaultAccount(id)
        }
    }

    fun setEditingAccount(account: Account?) {
        _uiState.update { it.copy(editingAccount = account) }
    }

    fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun executeTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, note: String?) {
        viewModelScope.launch {
            val transferTx = Transaction(
                amount = amount,
                type = TransactionType.TRANSFER,
                categoryId = 12L,
                accountId = fromAccountId,
                toAccountId = toAccountId,
                note = note ?: "تحويل بين الحسابات",
                date = System.currentTimeMillis()
            )
            transactionRepository.insertTransaction(transferTx)
        }
    }
}
