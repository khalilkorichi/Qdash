package com.qdash.presentation.salary

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import com.qdash.domain.usecase.salary.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@Immutable
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

    // Category linking (non-null envelopeId when picker is open)
    val categories: List<Category> = emptyList(),
    val showCategoryPicker: Long? = null,

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
    val categoryPickerEnvelopeId: Long? get() = showCategoryPicker
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
    private val depositSalaryNowUseCase: DepositSalaryNowUseCase,
    private val subscriptionRepository: SubscriptionRepository,
    private val getSalaryDistributionUseCase: GetSalaryDistributionUseCase,
    private val saveSalaryDistributionUseCase: SaveSalaryDistributionUseCase,
    private val categoryRepository: CategoryRepository,
    private val salaryDistributionRepository: SalaryDistributionRepository,
    private val transferBetweenAccountsUseCase: com.qdash.domain.usecase.transfer.TransferBetweenAccountsUseCase,
    private val transactionRepository: TransactionRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalaryUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _delayDaysFlow = MutableStateFlow("")
    private var mainLoadJob: Job? = null
    private var distributionJob: Job? = null

    init {
        loadData()
        observeDelayDaysChanges()
    }

    private fun loadData() {
        mainLoadJob?.cancel()
        mainLoadJob = viewModelScope.launch {
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
                val salary = overview.salary
                if (salary != null) {
                    loadDistributionData(salary.id)
                } else {
                    distributionJob?.cancel()
                    distributionJob = null
                }
            }
        }
    }

    private fun loadDistributionData(salaryId: Long) {
        distributionJob?.cancel()
        distributionJob = viewModelScope.launch {
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
                .debounce(250)
                .collect { daysStr ->
                    val state = _uiState.value
                    val salary = state.overview?.salary ?: return@collect
                    val subscriptions = state.overview.activeSubscriptions
                    // Only consider debts owed BY the user (exclude lent money OWED_TO_ME)
                    val debts = state.overview.activeDebts.filter { it.direction == DebtDirection.OWED_BY_ME }

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
        val normalized = com.qdash.core.utils.FormatterUtils.normalizeAmount(amount)
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
        _uiState.update { it.copy(dayOfMonth = day.coerceIn(1, 31)) }
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
        val result = SalaryEnvelopeCalculator.calculateAdjustedPercentages(
            type = type,
            newPercentage = newPercentage,
            currentNeeds = state.needsPercentage,
            currentWants = state.wantsPercentage,
            currentSavings = state.savingsPercentage
        )
        _uiState.update {
            it.copy(
                needsPercentage = result.needs,
                wantsPercentage = result.wants,
                savingsPercentage = result.savings
            )
        }
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
        _uiState.update { it.copy(showCategoryPicker = envelopeId) }
    }

    fun dismissCategoryPicker() {
        _uiState.update { it.copy(showCategoryPicker = null) }
    }

    fun toggleCategoryForEnvelope(envelopeId: Long, categoryId: Long) {
        val state = _uiState.value
        val envelope = state.envelopes.find { it.id == envelopeId } ?: return
        val updatedIds = if (categoryId in envelope.linkedCategoryIds) {
            envelope.linkedCategoryIds - categoryId
        } else {
            envelope.linkedCategoryIds + categoryId
        }
        _uiState.update { s ->
            s.copy(envelopes = s.envelopes.map { env ->
                if (env.id == envelopeId) env.copy(linkedCategoryIds = updatedIds) else env
            })
        }
        viewModelScope.launch {
            try {
                salaryDistributionRepository.updateLinkedCategories(envelopeId, updatedIds)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "خطأ في حفظ الفئات المرتبطة: ${e.message}") }
            }
        }
    }

    fun saveSalary() {
        val state = _uiState.value
        val amountDouble = state.amount.toDoubleOrNull()
        if (state.name.isBlank()) {
            _uiState.update { it.copy(userMessage = "الرجاء إدخال اسم الراتب") }
            return
        }
        if (amountDouble == null || amountDouble <= 0.0) {
            _uiState.update { it.copy(userMessage = "الرجاء إدخال قيمة راتب صحيحة أكبر من الصفر") }
            return
        }
        val accountId = state.selectedAccountId
        if (accountId == null) {
            _uiState.update { it.copy(userMessage = "الرجاء اختيار الحساب المستلم للراتب") }
            return
        }

        val clampedDay = state.dayOfMonth.coerceIn(1, 31)
        val expectedDate = computeNextExpectedPayday(clampedDay)

        viewModelScope.launch {
            val source = IncomeSource(
                id = state.editingId ?: 0,
                name = state.name.trim(),
                amount = amountDouble,
                type = "SALARY",
                accountId = accountId,
                dayOfMonth = clampedDay,
                isActive = true,
                nextExpectedDate = expectedDate
            )

            if (state.editingId == null) {
                val insertedId = incomeRepository.insertIncomeSource(source)
                if (state.distributionEnabled && insertedId > 0) {
                    saveSalaryDistributionUseCase(
                        salaryId = insertedId,
                        isEnabled = true,
                        needsPercentage = state.needsPercentage,
                        wantsPercentage = state.wantsPercentage,
                        savingsPercentage = state.savingsPercentage,
                        salaryAmount = amountDouble
                    )
                }
                _uiState.update { it.copy(userMessage = "تم حفظ إعدادات الراتب بنجاح") }
            } else {
                incomeRepository.updateIncomeSource(source)
                if (state.distributionEnabled) {
                    saveSalaryDistributionUseCase(
                        salaryId = source.id,
                        isEnabled = true,
                        needsPercentage = state.needsPercentage,
                        wantsPercentage = state.wantsPercentage,
                        savingsPercentage = state.savingsPercentage,
                        salaryAmount = amountDouble
                    )
                }
                _uiState.update { it.copy(userMessage = "تم تحديث إعدادات الراتب بنجاح") }
            }
            setShowAddDialog(false)
        }
    }

    private fun computeNextExpectedPayday(dayOfMonth: Int, referenceMillis: Long = System.currentTimeMillis()): Long {
        val todayStart = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val candidate = (todayStart.clone() as Calendar).apply {
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, 9)
        }

        if (candidate.timeInMillis < todayStart.timeInMillis) {
            candidate.set(Calendar.DAY_OF_MONTH, 1)
            candidate.add(Calendar.MONTH, 1)
            val nextMaxDay = candidate.getActualMaximum(Calendar.DAY_OF_MONTH)
            candidate.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, nextMaxDay))
        }
        return candidate.timeInMillis
    }

    private fun advancePaydayByOneMonth(anchorMillis: Long, dayOfMonth: Int): Long {
        val nextCal = Calendar.getInstance().apply {
            timeInMillis = anchorMillis
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextCal.timeInMillis
    }

    fun linkAccountToEnvelope(envelopeId: Long, accountId: Long?) {
        val updatedEnvelopes = _uiState.value.envelopes.map { env ->
            if (env.id == envelopeId) env.copy(linkedAccountId = accountId) else env
        }
        _uiState.update { it.copy(envelopes = updatedEnvelopes) }

        viewModelScope.launch {
            try {
                salaryDistributionRepository.saveEnvelopes(updatedEnvelopes)
            } catch (_: Exception) {
                _uiState.update { it.copy(userMessage = "خطأ في ربط الحساب بالظرف") }
            }
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
        setShowDelayDialog(true)
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

    fun depositSalaryNow(
        delayId: Long? = null,
        fallbackTransactionRepo: TransactionRepository? = null
    ) {
        val state = _uiState.value
        val salary = state.overview?.salary ?: return
        val activeDelayId = delayId ?: state.overview.delays.firstOrNull()?.id

        viewModelScope.launch {
            try {
                if (activeDelayId != null) {
                    depositSalaryNowUseCase(salaryId = salary.id, delayId = activeDelayId)
                } else {
                    val txRepo = transactionRepository ?: fallbackTransactionRepo
                    val allCategories = categoryRepository.getAllCategories().first()
                    val salaryCatId = allCategories
                        .firstOrNull { it.type == CategoryType.INCOME && it.name.contains("راتب") }?.id
                        ?: allCategories.firstOrNull { it.type == CategoryType.INCOME }?.id

                    if (txRepo != null) {
                        txRepo.insertTransaction(
                            Transaction(
                                amount = salary.amount,
                                type = TransactionType.INCOME,
                                kind = TransactionKind.SALARY,
                                categoryId = salaryCatId,
                                accountId = salary.accountId,
                                note = "إيداع راتب: ${salary.name}",
                                date = System.currentTimeMillis(),
                                occurredAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        val account = accountRepository.getAccountById(salary.accountId)
                        if (account != null) {
                            accountRepository.updateAccount(account.copy(balance = account.balance + salary.amount))
                        }
                    }

                    val nextExpected = advancePaydayByOneMonth(salary.nextExpectedDate, salary.dayOfMonth)
                    incomeRepository.updateIncomeSource(salary.copy(nextExpectedDate = nextExpected))
                }

                // Run automatic sub-account envelope transfers on actual salary deposit
                if (state.distributionEnabled) {
                    state.envelopes.forEach { env ->
                        if (env.linkedAccountId != null &&
                            env.linkedAccountId != salary.accountId &&
                            env.allocatedAmount > 0.0
                        ) {
                            transferBetweenAccountsUseCase(
                                TransferRequest(
                                    fromAccountId = salary.accountId,
                                    toAccountId = env.linkedAccountId,
                                    amount = env.allocatedAmount,
                                    note = "توزيع تلقائي للراتب: ${env.label}",
                                    date = System.currentTimeMillis(),
                                    occurredAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                _uiState.update {
                    it.copy(userMessage = "تم استلام وإيداع الراتب في الحساب وتحديث الموعد القادم بنجاح!")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "حدث خطأ أثناء إيداع الراتب: ${e.message}")
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
