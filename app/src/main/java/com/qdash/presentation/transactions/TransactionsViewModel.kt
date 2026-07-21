package com.qdash.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import com.qdash.domain.usecase.categorization.GetCategorySuggestionUseCase
import com.qdash.domain.usecase.categorization.LearnCategoryMappingUseCase

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.room.withTransaction
import com.qdash.domain.usecase.transaction.BulkEditTransactionsUseCase
import com.qdash.domain.usecase.transaction.BulkEditParams
import com.qdash.domain.usecase.transaction.FilterTransactionsUseCase
import com.qdash.domain.usecase.transaction.TransactionFilterParams

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isAmountWordsEnabled: Boolean = true,
    val smartCategorySortEnabled: Boolean = false,
    
    // Filters
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    
    // Smart Suggestions state
    val currentSuggestion: CategorySuggestion? = null,
    val suggestedCategory: Category? = null,
    
    val filterLargeOnly: Boolean = false,
    val filterBaridiMobOnly: Boolean = false,
    
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val error: String? = null,

    // Advanced Filters
    val filterMinAmount: Double? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null,

    // Bulk actions
    val selectedTransactionIds: Set<Long> = emptySet(),
 
    // Budget goals
    val budgetGoals: List<BudgetGoal> = emptyList(),
 
    // Activity Calendar state
    val dailyAggregates: List<DailyFinancialAggregate> = emptyList(),
    val selectedCalendarDate: Long? = null,
    val selectedMetricMode: String = "COUNT", // "COUNT", "EXPENSE", "INCOME", "SCORE"
    val visibleMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
    val visibleYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
)

private data class FilterState(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val filterLargeOnly: Boolean = false,
    val filterBaridiMobOnly: Boolean = false,
    val filterMinAmount: Double? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null
)

private data class CalendarState(
    val selectedDate: Long?,
    val visibleMonth: Int,
    val visibleYear: Int,
    val metricMode: String
)

class TransactionsViewModel(
    internal val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    internal val categoryRepository: CategoryRepository,
    internal val incomeRepository: IncomeRepository,
    private val getCategorySuggestionUseCase: GetCategorySuggestionUseCase,
    internal val learnCategoryMappingUseCase: LearnCategoryMappingUseCase,
    private val budgetGoalRepository: com.qdash.domain.repository.BudgetGoalRepository,
    internal val templateRepository: TransactionTemplateRepository,
    internal val preferencesManager: com.qdash.core.preferences.PreferencesManager,
    internal val bulkEditTransactionsUseCase: BulkEditTransactionsUseCase
) : ViewModel() {


    private val filterTransactionsUseCase = FilterTransactionsUseCase()

    internal val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    internal val _bulkEditEvent = MutableSharedFlow<BulkEditEvent>()
    val bulkEditEvent: SharedFlow<BulkEditEvent> = _bulkEditEvent.asSharedFlow()


    val selectedTotal: StateFlow<Double> = uiState.map { state ->
        state.transactions.filter { it.id in state.selectedTransactionIds }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun consumeSaveCompleted() {
        _uiState.update { it.copy(saveCompleted = false) }
    }

    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    private val _filterLargeOnly = MutableStateFlow(false)
    private val _filterBaridiMobOnly = MutableStateFlow(false)
    private val _filterMinAmount = MutableStateFlow<Double?>(null)
    private val _filterStartDate = MutableStateFlow<Long?>(null)
    private val _filterEndDate = MutableStateFlow<Long?>(null)

    // Calendar state flows
    private val _selectedCalendarDate = MutableStateFlow<Long?>(null)
    private val _selectedMetricMode = MutableStateFlow("COUNT")
    private val _visibleMonth = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH))
    private val _visibleYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))

    init {
        loadTransactions()
    }

    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun loadTransactions() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val baseFilterFlow = combine(
                    _searchQuery.debounce(300L).onStart { emit("") },
                    _selectedType,
                    _selectedCategoryId,
                    _selectedAccountId
                ) { query, type, catId, accId ->
                    FilterState(
                        query = query,
                        type = type,
                        categoryId = catId,
                        accountId = accId
                    )
                }

                val advancedFilterFlow = combine(
                    _filterLargeOnly,
                    _filterBaridiMobOnly,
                    _filterMinAmount,
                    _filterStartDate,
                    _filterEndDate
                ) { largeOnly, baridiOnly, minAmount, startDate, endDate ->
                    Triple(largeOnly, baridiOnly, Triple(minAmount, startDate, endDate))
                }

                val filtersFlow = combine(
                    baseFilterFlow,
                    advancedFilterFlow,
                    _selectedCalendarDate
                ) { base, adv, calDate ->
                    val (largeOnly, baridiOnly, extra) = adv
                    val (minAmount, startDate, endDate) = extra
                    Pair(
                        base.copy(
                            filterLargeOnly = largeOnly,
                            filterBaridiMobOnly = baridiOnly,
                            filterMinAmount = minAmount,
                            filterStartDate = startDate,
                            filterEndDate = endDate
                        ),
                        calDate
                    )
                }

                val calendarStateFlow = combine(
                    _selectedCalendarDate,
                    _visibleMonth,
                    _visibleYear,
                    _selectedMetricMode
                ) { selectedDate, visibleMonth, visibleYear, metricMode ->
                    CalendarState(selectedDate, visibleMonth, visibleYear, metricMode)
                }

                val coreDataFlow = combine(
                    transactionRepository.getAllTransactions(),
                    categoryRepository.getAllCategories(),
                    accountRepository.getAllAccounts()
                ) { txs, cats, accs ->
                    Triple(txs, cats, accs)
                }

                val aggregatesFlow = combine(_visibleYear, _visibleMonth) { year, month ->
                    getMonthRange(year, month)
                }.flatMapLatest { range ->
                    transactionRepository.getDailyFinancialAggregatesForRange(range.first, range.second)
                }

                val budgetsFlow = budgetGoalRepository.getActiveBudgetGoals()

                combine(
                    coreDataFlow,
                    filtersFlow,
                    calendarStateFlow,
                    aggregatesFlow,
                    budgetsFlow
                ) { coreData, filtersPair, calendarState, aggregates, budgets ->
                    val (txs, cats, accs) = coreData
                    val filters = filtersPair.first
                    val selectedDate = filtersPair.second

                    val filtered = filterTransactionsUseCase(
                        transactions = txs,
                        accounts = accs,
                        params = TransactionFilterParams(
                            query = filters.query,
                            type = filters.type,
                            categoryId = filters.categoryId,
                            accountId = filters.accountId,
                            filterLargeOnly = filters.filterLargeOnly,
                            filterBaridiMobOnly = filters.filterBaridiMobOnly,
                            filterMinAmount = filters.filterMinAmount,
                            filterStartDate = filters.filterStartDate,
                            filterEndDate = filters.filterEndDate,
                            selectedCalendarDate = selectedDate
                        )
                    )
                    
                    _uiState.value.copy(
                        transactions = txs,
                        filteredTransactions = filtered,
                        categories = cats,
                        accounts = accs,
                        dailyAggregates = aggregates,
                        budgetGoals = budgets,
                        visibleMonth = calendarState.visibleMonth,
                        visibleYear = calendarState.visibleYear,
                        selectedCalendarDate = calendarState.selectedDate,
                        selectedMetricMode = calendarState.metricMode,
                        filterLargeOnly = filters.filterLargeOnly,
                        filterBaridiMobOnly = filters.filterBaridiMobOnly,
                        filterMinAmount = filters.filterMinAmount,
                        filterStartDate = filters.filterStartDate,
                        filterEndDate = filters.filterEndDate,
                        isAmountWordsEnabled = preferencesManager.amountWordsEnabled,
                        smartCategorySortEnabled = preferencesManager.smartCategorySortEnabled,
                        isLoading = false
                    )
                }
                .flowOn(kotlinx.coroutines.Dispatchers.Default)
                .collect { updatedState ->
                    _uiState.update { 
                        it.copy(
                            transactions = updatedState.transactions,
                            filteredTransactions = updatedState.filteredTransactions,
                            categories = updatedState.categories,
                            accounts = updatedState.accounts,
                            dailyAggregates = updatedState.dailyAggregates,
                            budgetGoals = updatedState.budgetGoals,
                            visibleMonth = updatedState.visibleMonth,
                            visibleYear = updatedState.visibleYear,
                            selectedCalendarDate = updatedState.selectedCalendarDate,
                            selectedMetricMode = updatedState.selectedMetricMode,
                            filterLargeOnly = updatedState.filterLargeOnly,
                            filterBaridiMobOnly = updatedState.filterBaridiMobOnly,
                            filterMinAmount = updatedState.filterMinAmount,
                            filterStartDate = updatedState.filterStartDate,
                            filterEndDate = updatedState.filterEndDate,
                            isAmountWordsEnabled = updatedState.isAmountWordsEnabled,
                            smartCategorySortEnabled = updatedState.smartCategorySortEnabled,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private var suggestionJob: kotlinx.coroutines.Job? = null

    fun onNoteChanged(note: String, amount: Double?, accountId: Long?) {
        suggestionJob?.cancel()
        if (note.trim().isEmpty()) {
            _uiState.update { it.copy(currentSuggestion = null, suggestedCategory = null) }
            return
        }
        suggestionJob = viewModelScope.launch {
            try {
                // Keypress debounce delay (200ms for fast feedback)
                kotlinx.coroutines.delay(200L)
                val suggestion = getCategorySuggestionUseCase(note, amount, accountId)
                val matchedCategory = _uiState.value.categories.find { it.id == suggestion.suggestedCategoryId }
                _uiState.update {
                    it.copy(
                        currentSuggestion = suggestion,
                        suggestedCategory = matchedCategory
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancelations
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun acceptSuggestion(noteText: String, categoryId: Long) {
        learnMapping(noteText, categoryId)
    }

    fun learnMapping(noteText: String, categoryId: Long) {
        viewModelScope.launch {
            try {
                learnCategoryMappingUseCase(noteText, categoryId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSuggestion() {
        _uiState.update { it.copy(currentSuggestion = null, suggestedCategory = null) }
    }




    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    fun onTypeSelected(type: TransactionType?) {
        _uiState.update { it.copy(selectedType = type) }
        _selectedType.value = type
    }

    fun onCategorySelected(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        _selectedCategoryId.value = categoryId
    }

    fun onAccountSelected(accountId: Long?) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
        _selectedAccountId.value = accountId
    }


    private var lastDeletedTransaction: Transaction? = null

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            lastDeletedTransaction = transaction
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun restoreLastDeletedTransaction() {
        val tx = lastDeletedTransaction ?: return
        viewModelScope.launch {
            transactionRepository.insertTransaction(tx)
            lastDeletedTransaction = null
        }
    }

    fun onCalendarDateSelected(timestamp: Long?) {
        _selectedCalendarDate.value = timestamp
    }

    fun onMetricModeChanged(mode: String) {
        _selectedMetricMode.value = mode
    }

    fun onCalendarMonthChanged(year: Int, month: Int) {
        _visibleYear.value = year
        _visibleMonth.value = month
        _selectedCalendarDate.value = null
    }

    fun toggleFilterLarge() {
        val newVal = !_uiState.value.filterLargeOnly
        _uiState.update { it.copy(filterLargeOnly = newVal) }
        _filterLargeOnly.value = newVal
    }

    fun toggleFilterBaridiMob() {
        val newVal = !_uiState.value.filterBaridiMobOnly
        _uiState.update { it.copy(filterBaridiMobOnly = newVal) }
        _filterBaridiMobOnly.value = newVal
    }


    fun setAdvancedFilters(minAmount: Double?, startDate: Long?, endDate: Long?) {
        _filterMinAmount.value = minAmount
        _filterStartDate.value = startDate
        _filterEndDate.value = endDate
    }

    fun clearAdvancedFilters() {
        _filterMinAmount.value = null
        _filterStartDate.value = null
        _filterEndDate.value = null
    }

    fun toggleTransactionSelection(id: Long) {
        _uiState.update { state ->
            val current = state.selectedTransactionIds
            val updated = if (current.contains(id)) current - id else current + id
            state.copy(selectedTransactionIds = updated)
        }
    }

    fun clearTransactionSelection() {
        _uiState.update { it.copy(selectedTransactionIds = emptySet()) }
    }

    fun selectAllTransactions() {
        _uiState.update { state ->
            val allIds = state.filteredTransactions.map { it.id }.toSet()
            state.copy(selectedTransactionIds = allIds)
        }
    }

}

sealed interface BulkEditEvent {
    data class Success(val count: Int) : BulkEditEvent
    data class Error(val error: String) : BulkEditEvent
}
