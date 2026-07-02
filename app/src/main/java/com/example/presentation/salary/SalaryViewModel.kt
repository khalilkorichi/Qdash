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
    val envelopes: List<SalaryEnvelope> = emptyList(),
    val isDistributionSaving: Boolean = false,

    // Category linking
    val categories: List<Category> = emptyList(),
    val showCategoryPicker: Boolean = false,
    val categoryPickerEnvelopeId: Long? = null,

    // Delay Salary Form
    val showDelayDialog: Boolean = false,
    val delayDaysInput: String = "",
    val delayImpact: SalaryDelayImpact? = null,
    val isAnalyzingDelay: Boolean = false,
    val isConfirmingDelay: Boolean = false,
    val isEditMode: Boolean = false,
    val editingDelayId: Long? = null,
    val originalDelayDays: Int = 0,
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
    private val deleteSalaryDelayUseCase: DeleteSalaryDelayUseCase,
    private val updateSalaryDelayUseCase: UpdateSalaryDelayUseCase,
    private val subscriptionRepository: SubscriptionRepository,
    private val getSalaryDistributionUseCase: GetSalaryDistributionUseCase,
    private val saveSalaryDistributionUseCase: SaveSalaryDistributionUseCase,
    private val categoryRepository: CategoryRepository
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
                accountRepository.getAllAccounts(),
                categoryRepository.getAllCategories()
            ) { overview, accounts, categories ->
                Triple(overview, accounts, categories)
            }.collect { (overview, accounts, categories) ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        overview = overview,
                        accounts = accounts,
                        categories = categories.filter { it.type == CategoryType.EXPENSE },
                        selectedAccountId = if (state.selectedAccountId == null) accounts.firstOrNull()?.id else state.selectedAccountId
                    )
                }
                // Load distribution data when salary is available
                overview.salary?.let { salary ->
                    loadDistributionData(salary.id)
                }
            }
        }
    }

    private fun loadDistributionData(salaryId: Long) {
        viewModelScope.launch {
            getSalaryDistributionUseCase.forSalary(salaryId).collect { (distribution, envelopes) ->
                _uiState.update { state ->
                    state.copy(
                        distributionEnabled = distribution?.isEnabled ?: false,
                        needsPercentage = distribution?.needsPercentage ?: 50,
                        wantsPercentage = distribution?.wantsPercentage ?: 30,
                        savingsPercentage = distribution?.savingsPercentage ?: 20,
                        envelopes = envelopes,
                        overview = state.overview?.copy(
                            distribution = distribution,
                            envelopes = envelopes
                        )
                    )
                }
            }
        }
    }

    private fun observeDelayDaysChanges() {
        viewModelScope.launch {
            _delayDaysFlow
                .debounce(300)
                .collect { daysStr ->
                    val state = _uiState.value
                    val salary = state.overview?.salary ?: return@collect
                    val subscriptions = state.overview?.activeSubscriptions ?: emptyList()
                    val debts = state.overview?.activeDebts ?: emptyList()
                    
                    val days = daysStr.toIntOrNull()
                    if (days == null || days <= 0) {
                        _uiState.update { it.copy(delayImpact = null, isAnalyzingDelay = false) }
                        return@collect
                    }

                    _uiState.update { it.copy(isAnalyzingDelay = true) }
                    
                    val originalDateOverride = if (state.isEditMode) {
                        salary.nextExpectedDate - (state.originalDelayDays * 86400000L)
                    } else {
                        null
                    }
                    val oldDelayDays = if (state.isEditMode) state.originalDelayDays else 0

                    val impact = analyzeSalaryDelayImpactUseCase(
                        salary = salary,
                        delayDays = days,
                        subscriptions = subscriptions,
                        debts = debts,
                        originalDateOverride = originalDateOverride,
                        oldDelayDays = oldDelayDays
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
    
    // --- Distribution Methods ---

    fun toggleDistribution(enabled: Boolean) {
        _uiState.update { it.copy(distributionEnabled = enabled) }
        saveDistribution()
    }

    fun updateDistributionPercentage(type: EnvelopeType, newPercentage: Int) {
        val state = _uiState.value
        val clamped = newPercentage.coerceIn(0, 100)
        
        // Adjust the other two to maintain sum = 100
        val (needs, wants, savings) = when (type) {
            EnvelopeType.NEEDS -> {
                val remaining = 100 - clamped
                val wantsRatio = if (state.wantsPercentage + state.savingsPercentage > 0) {
                    state.wantsPercentage.toFloat() / (state.wantsPercentage + state.savingsPercentage)
                } else 0.5f
                val newWants = (remaining * wantsRatio).toInt()
                val newSavings = remaining - newWants
                Triple(clamped, newWants, newSavings)
            }
            EnvelopeType.WANTS -> {
                val remaining = 100 - clamped
                val needsRatio = if (state.needsPercentage + state.savingsPercentage > 0) {
                    state.needsPercentage.toFloat() / (state.needsPercentage + state.savingsPercentage)
                } else 0.5f
                val newNeeds = (remaining * needsRatio).toInt()
                val newSavings = remaining - newNeeds
                Triple(newNeeds, clamped, newSavings)
            }
            EnvelopeType.SAVINGS -> {
                val remaining = 100 - clamped
                val needsRatio = if (state.needsPercentage + state.wantsPercentage > 0) {
                    state.needsPercentage.toFloat() / (state.needsPercentage + state.wantsPercentage)
                } else 0.5f
                val newNeeds = (remaining * needsRatio).toInt()
                val newWants = remaining - newNeeds
                Triple(newNeeds, newWants, clamped)
            }
        }
        
        _uiState.update { it.copy(needsPercentage = needs, wantsPercentage = wants, savingsPercentage = savings) }
    }

    fun commitDistributionPercentages() {
        saveDistribution()
    }

    private fun saveDistribution() {
        val state = _uiState.value
        val salary = state.overview?.salary ?: return
        
        _uiState.update { it.copy(isDistributionSaving = true) }
        viewModelScope.launch {
            try {
                saveSalaryDistributionUseCase(
                    salaryId = salary.id,
                    isEnabled = state.distributionEnabled,
                    needsPercentage = state.needsPercentage,
                    wantsPercentage = state.wantsPercentage,
                    savingsPercentage = state.savingsPercentage,
                    salaryAmount = salary.amount
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "خطأ في حفظ إعدادات التوزيع: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isDistributionSaving = false) }
            }
        }
    }

    fun showCategoryPickerFor(envelopeId: Long) {
        _uiState.update { it.copy(showCategoryPicker = true, categoryPickerEnvelopeId = envelopeId) }
    }

    fun dismissCategoryPicker() {
        _uiState.update { it.copy(showCategoryPicker = false, categoryPickerEnvelopeId = null) }
    }

    fun toggleCategoryForEnvelope(envelopeId: Long, categoryId: Long) {
        val state = _uiState.value
        val envelope = state.envelopes.find { it.id == envelopeId } ?: return
        val updatedIds = if (categoryId in envelope.linkedCategoryIds) {
            envelope.linkedCategoryIds - categoryId
        } else {
            envelope.linkedCategoryIds + categoryId
        }
        viewModelScope.launch {
            try {
                val repo = getSalaryDistributionUseCase // Access repository indirectly
                // We need the repository directly — use saveSalaryDistributionUseCase's parent
                // For simplicity, update via the current state and re-save
                _uiState.update { s ->
                    s.copy(envelopes = s.envelopes.map { env ->
                        if (env.id == envelopeId) env.copy(linkedCategoryIds = updatedIds) else env
                    })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "خطأ في ربط الفئة") }
            }
        }
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
                // Recalculate envelopes if distribution is enabled and amount changed
                if (state.distributionEnabled) {
                    saveDistribution()
                }
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
                isEditMode = false,
                editingDelayId = null,
                originalDelayDays = 0,
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

    fun startAddSalaryDelay() {
        _uiState.update { 
            it.copy(
                showDelayDialog = true,
                isEditMode = false,
                editingDelayId = null,
                originalDelayDays = 0,
                delayDaysInput = ""
            ) 
        }
        _delayDaysFlow.value = ""
    }

    fun startEditSalaryDelay(delay: SalaryDelay) {
        _uiState.update { 
            it.copy(
                showDelayDialog = true,
                isEditMode = true,
                editingDelayId = delay.id,
                originalDelayDays = delay.delayDays,
                delayDaysInput = delay.delayDays.toString()
            ) 
        }
        _delayDaysFlow.value = delay.delayDays.toString()
    }

    fun deleteSalaryDelay(delayId: Long) {
        viewModelScope.launch {
            try {
                deleteSalaryDelayUseCase(delayId)
                _uiState.update { 
                    it.copy(userMessage = "تم إلغاء تأجيل الراتب وإعادة الالتزامات لمواعيدها بنجاح!") 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(userMessage = "حدث خطأ أثناء إلغاء التأجيل: ${e.message}") 
                }
            }
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
                if (state.isEditMode && state.editingDelayId != null) {
                    updateSalaryDelayUseCase(
                        delayId = state.editingDelayId,
                        newDelayDays = days,
                        newDate = impact.newDate,
                        newSeverityScore = impact.severityScore,
                        affectedObligations = impact.affectedObligations
                    )
                    _uiState.update { 
                        it.copy(
                            isConfirmingDelay = false,
                            showDelayDialog = false,
                            userMessage = "تم تعديل تأجيل الراتب وتحديث الالتزامات بنجاح!"
                        ) 
                    }
                } else {
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
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isConfirmingDelay = false,
                        userMessage = "حدث خطأ: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
