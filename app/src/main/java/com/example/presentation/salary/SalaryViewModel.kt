package com.example.presentation.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import com.example.domain.usecase.salary.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SalaryUiState(
    val isLoading: Boolean = false,
    val overview: SalaryManagementOverview? = null,
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
    val savingsPercentage: Int = 20,

    // Delay Salary Form
    val showDelayDialog: Boolean = false,
    val delayDaysInput: String = "",
    val delayImpact: SalaryDelayImpact? = null,
    val isAnalyzingDelay: Boolean = false,
    val isConfirmingDelay: Boolean = false,
    val userMessage: String? = null
) {
    // Keep legacy support for IncomeSource list
    val incomeSources: List<IncomeSource>
        get() = overview?.salary?.let { listOf(it) } ?: emptyList()
}

@OptIn(FlowPreview::class)
class SalaryViewModel(
    private val incomeRepository: IncomeRepository,
    private val accountRepository: AccountRepository,
    private val getSalaryManagementOverviewUseCase: GetSalaryManagementOverviewUseCase,
    private val analyzeSalaryDelayImpactUseCase: AnalyzeSalaryDelayImpactUseCase,
    private val confirmSalaryDelayUseCase: ConfirmSalaryDelayUseCase,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalaryUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _delayDaysFlow = MutableStateFlow("")

    init {
        loadData()
        observeDelayDaysChanges()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getSalaryManagementOverviewUseCase(),
                accountRepository.getAllAccounts()
            ) { overview, accounts ->
                _uiState.value.copy(
                    isLoading = false,
                    overview = overview,
                    accounts = accounts,
                    selectedAccountId = if (_uiState.value.selectedAccountId == null) accounts.firstOrNull()?.id else _uiState.value.selectedAccountId
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun observeDelayDaysChanges() {
        viewModelScope.launch {
            _delayDaysFlow
                .debounce(300)
                .collect { daysStr ->
                    val salary = _uiState.value.overview?.salary ?: return@collect
                    val subscriptions = _uiState.value.overview?.activeSubscriptions ?: emptyList()
                    val debts = _uiState.value.overview?.activeDebts ?: emptyList()
                    
                    val days = daysStr.toIntOrNull()
                    if (days == null || days <= 0) {
                        _uiState.update { it.copy(delayImpact = null, isAnalyzingDelay = false) }
                        return@collect
                    }

                    _uiState.update { it.copy(isAnalyzingDelay = true) }
                    val impact = analyzeSalaryDelayImpactUseCase(
                        salary = salary,
                        delayDays = days,
                        subscriptions = subscriptions,
                        debts = debts
                    )
                    _uiState.update { it.copy(delayImpact = impact, isAnalyzingDelay = false) }
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
                    amount = sourceToEdit?.amount?.let { amt ->
                        if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
                    } ?: "",
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
        val normalized = com.example.core.utils.FormatterUtils.normalizeAmount(amount)
        val filtered = normalized.filter { it.isDigit() || it == '.' }
        val parts = filtered.split(".")
        val cleaned = if (parts.size > 2) {
            parts[0] + "." + parts.subList(1, parts.size).joinToString("") { it.filter { c -> c.isDigit() } }
        } else {
            filtered
        }
        _uiState.update { it.copy(amount = cleaned) }
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
                nextExpectedDate = System.currentTimeMillis()
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

    // Delay Dialog Methods
    fun setShowDelayDialog(show: Boolean) {
        _uiState.update { 
            it.copy(
                showDelayDialog = show,
                delayDaysInput = "",
                delayImpact = null,
                userMessage = null
            ) 
        }
        _delayDaysFlow.value = ""
    }

    fun onDelayDaysChange(days: String) {
        val filtered = days.filter { it.isDigit() }
        _uiState.update { it.copy(delayDaysInput = filtered) }
        _delayDaysFlow.value = filtered
    }

    fun toggleSubscriptionAutoShift(subscription: Subscription) {
        viewModelScope.launch {
            val updated = subscription.copy(isAutoShiftableBySalary = !subscription.isAutoShiftableBySalary)
            subscriptionRepository.updateSubscription(updated)
        }
    }

    fun confirmSalaryDelay() {
        val state = _uiState.value
        val salary = state.overview?.salary ?: return
        val days = state.delayDaysInput.toIntOrNull() ?: return
        val impact = state.delayImpact ?: return

        if (state.isConfirmingDelay) return

        _uiState.update { it.copy(isConfirmingDelay = true) }
        viewModelScope.launch {
            try {
                confirmSalaryDelayUseCase(
                    salaryId = salary.id,
                    delayDays = days,
                    originalDate = salary.nextExpectedDate,
                    newDate = impact.newDate,
                    severityScore = impact.severityScore,
                    affectedObligations = impact.affectedObligations
                )
                _uiState.update { 
                    it.copy(
                        isConfirmingDelay = false,
                        showDelayDialog = false,
                        userMessage = "تم تأجيل موعد الراتب وتحديث الالتزامات بنجاح!"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isConfirmingDelay = false,
                        userMessage = "حدث خطأ أثناء تأجيل الراتب: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
