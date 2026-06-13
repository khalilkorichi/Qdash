package com.example.presentation.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Account
import com.example.domain.model.IncomeSource
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SalaryUiState(
    val isLoading: Boolean = false,
    val incomeSources: List<IncomeSource> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val showAddDialog: Boolean = false,
    // Form fields for adding/editing
    val editingId: Long? = null,
    val name: String = "راتبي الأساسي",
    val amount: String = "",
    val dayOfMonth: Int = 1,
    val selectedAccountId: Long? = null,
    // Distribution config
    val distributionEnabled: Boolean = false,
    val needsPercentage: Int = 50,
    val wantsPercentage: Int = 30,
    val savingsPercentage: Int = 20
)

class SalaryViewModel(
    private val incomeRepository: IncomeRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalaryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                incomeRepository.getAllIncomeSources(),
                accountRepository.getAllAccounts()
            ) { sources, accounts ->
                _uiState.value.copy(
                    incomeSources = sources.filter { it.type == "SALARY" },
                    accounts = accounts,
                    selectedAccountId = if (_uiState.value.selectedAccountId == null) accounts.firstOrNull()?.id else _uiState.value.selectedAccountId
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setShowAddDialog(show: Boolean, sourceToEdit: IncomeSource? = null) {
        if (show) {
            _uiState.update { 
                it.copy(
                    showAddDialog = true,
                    editingId = sourceToEdit?.id,
                    name = sourceToEdit?.name ?: "راتبي الأساسي",
                    amount = sourceToEdit?.amount?.toString() ?: "",
                    dayOfMonth = sourceToEdit?.dayOfMonth ?: 1,
                    selectedAccountId = sourceToEdit?.accountId ?: it.accounts.firstOrNull()?.id
                )
            }
        } else {
            _uiState.update { it.copy(showAddDialog = false, editingId = null, amount = "") }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onAmountChange(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun onDayOfMonthChange(day: Int) {
        _uiState.update { it.copy(dayOfMonth = day) }
    }

    fun onAccountSelected(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }
    
    fun toggleDistribution(enabled: Boolean) {
        _uiState.update { it.copy(distributionEnabled = enabled) }
    }

    fun updateDistribution(needs: Int, wants: Int, savings: Int) {
        _uiState.update { it.copy(needsPercentage = needs, wantsPercentage = wants, savingsPercentage = savings) }
    }

    fun saveSalary() {
        val state = _uiState.value
        val amountDouble = state.amount.toDoubleOrNull() ?: return
        val accountId = state.selectedAccountId ?: return

        viewModelScope.launch {
            val source = IncomeSource(
                id = state.editingId ?: 0,
                name = state.name,
                amount = amountDouble,
                type = "SALARY",
                accountId = accountId,
                dayOfMonth = state.dayOfMonth,
                isActive = true,
                nextExpectedDate = System.currentTimeMillis() // Simplified logic
            )
            
            if (state.editingId == null) {
                incomeRepository.insertIncomeSource(source)
            } else {
                incomeRepository.updateIncomeSource(source)
            }
            setShowAddDialog(false)
        }
    }

    fun deleteSalary(source: IncomeSource) {
        viewModelScope.launch {
            incomeRepository.deleteIncomeSource(source)
        }
    }
}
