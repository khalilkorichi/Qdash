package com.qdash.presentation.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.domain.repository.AccountRepository
import com.qdash.domain.repository.CategoryRepository
import com.qdash.domain.repository.TransactionRepository
import com.qdash.presentation.search.components.accountTypeLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class SearchUiState(
    val query: String = "",
    val selectedFilters: Set<String> = emptySet(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isSearching: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val suggestedCategories: List<Category> = emptyList()
)

private const val MAX_RECENT_SEARCHES = 8
private const val SEARCH_DEBOUNCE_MS = 300L
private val ENTITY_FILTERS = setOf("المعاملات", "الحسابات", "الفئات")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val accountRepo: AccountRepository,
    private val preferencesManager: com.qdash.core.preferences.PreferencesManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _isSearching = MutableStateFlow(false)
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())

    private val _suggestedCategories = categoryRepo.getAllCategories()
        .map { list -> list.take(8) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchResults = _query
        .debounce(SEARCH_DEBOUNCE_MS)
        .flatMapLatest { q ->
            if (q.isBlank()) {
                _isSearching.value = false
                flowOf(Triple(emptyList(), emptyList(), emptyList()))
            } else {
                _isSearching.value = true
                combine(
                    transactionRepo.searchTransactions(q),
                    categoryRepo.searchCategories(q),
                    accountRepo.getAllAccounts().map { accounts ->
                        accounts.filter {
                            it.name.contains(q, ignoreCase = true) ||
                                accountTypeLabel(it.type).contains(q, ignoreCase = true)
                        }
                    }
                ) { transactions, categories, accounts ->
                    _isSearching.value = false
                    Triple(transactions, categories, accounts)
                }.flowOn(kotlinx.coroutines.Dispatchers.Default)
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _searchResults,
        _isSearching,
        _recentSearches,
        _suggestedCategories,
        _selectedFilters
    ) { array ->
        val query = array[0] as String
        val results = array[1] as Triple<List<Transaction>, List<Category>, List<Account>>
        val searching = array[2] as Boolean
        val recents = array[3] as List<String>
        val suggestedCats = array[4] as List<Category>
        val filters = array[5] as Set<String>

        val (transactions, categories, accounts) = results
        val hasEntityFilter = filters.any { it in ENTITY_FILTERS }
        val wantExpenses = "المصاريف" in filters
        val wantIncomes = "المداخيل" in filters
        val hasTypeFilter = wantExpenses || wantIncomes

        val filteredTransactions = if (!hasEntityFilter || "المعاملات" in filters) {
            if (hasTypeFilter) {
                transactions.filter { tx ->
                    (wantExpenses && tx.type == TransactionType.EXPENSE) ||
                        (wantIncomes && tx.type == TransactionType.INCOME)
                }
            } else {
                transactions
            }
        } else {
            emptyList()
        }

        val filteredAccounts = if ("الحسابات" in filters || (!hasEntityFilter && !hasTypeFilter)) {
            accounts
        } else {
            emptyList()
        }

        val filteredCategories = if (!hasEntityFilter || "الفئات" in filters) {
            if (hasTypeFilter) {
                categories.filter { cat ->
                    (wantExpenses && cat.type == CategoryType.EXPENSE) ||
                        (wantIncomes && cat.type == CategoryType.INCOME)
                }
            } else {
                categories
            }
        } else {
            emptyList()
        }

        SearchUiState(
            query = query,
            selectedFilters = filters,
            transactions = filteredTransactions,
            categories = filteredCategories,
            accounts = filteredAccounts,
            isSearching = searching,
            recentSearches = recents,
            suggestedCategories = suggestedCats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState()
    )

    init {
        loadRecentSearches()
    }

    fun toggleFilter(filter: String) {
        _selectedFilters.update { current ->
            if (filter in current) current - filter else current + filter
        }
    }

    fun clearFilters() {
        _selectedFilters.value = emptySet()
    }

    fun onQueryChange(query: String) {
        _query.value = query
        if (query.isBlank()) {
            _isSearching.value = false
        } else {
            _isSearching.value = true
        }
    }

    fun clearQuery() {
        _query.value = ""
        _isSearching.value = false
    }

    fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        val current = _recentSearches.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val updated = current.take(MAX_RECENT_SEARCHES)
        _recentSearches.value = updated

        viewModelScope.launch {
            preferencesManager.recentSearches = updated.joinToString("|||")
        }
    }

    fun loadRecentSearches() {
        viewModelScope.launch {
            val raw = preferencesManager.recentSearches
            _recentSearches.value = if (raw.isBlank()) {
                emptyList()
            } else {
                raw.split("|||").filter { it.isNotBlank() }
            }
        }
    }

    fun removeRecentSearch(query: String) {
        val updated = _recentSearches.value.filter { it != query }
        _recentSearches.value = updated
        viewModelScope.launch {
            preferencesManager.recentSearches = updated.joinToString("|||")
        }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        viewModelScope.launch {
            preferencesManager.recentSearches = ""
        }
    }
}
