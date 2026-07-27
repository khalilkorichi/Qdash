package com.qdash.presentation.search

import androidx.compose.runtime.Immutable

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.domain.repository.AccountRepository
import com.qdash.domain.repository.CategoryRepository
import com.qdash.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class SearchUiState(
    val query: String = "",
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isSearching: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val suggestedCategories: List<Category> = emptyList()
)

private const val PREFS_NAME = "fintrack_search_prefs"
private const val KEY_RECENT_SEARCHES = "recent_searches"
private const val MAX_RECENT_SEARCHES = 8
private const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val accountRepo: AccountRepository,
    private val preferencesManager: com.qdash.core.preferences.PreferencesManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
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
                        accounts.filter { it.name.contains(q, ignoreCase = true) }
                    }
                ) { transactions, categories, accounts ->
                    _isSearching.value = false
                    Triple(transactions, categories, accounts)
                }.flowOn(kotlinx.coroutines.Dispatchers.Default)
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _searchResults,
        _isSearching,
        _recentSearches,
        _suggestedCategories
    ) { query, results, searching, recents, suggestedCats ->
        val (transactions, categories, accounts) = results
        SearchUiState(
            query = query,
            transactions = transactions,
            categories = categories,
            accounts = accounts,
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
