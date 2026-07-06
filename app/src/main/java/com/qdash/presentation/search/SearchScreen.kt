package com.qdash.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.qdash.presentation.search.components.*

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onTransactionClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SearchTopBar(
                query = uiState.query,
                onQueryChange = { viewModel.onQueryChange(it) },
                onClear = { viewModel.clearQuery() },
                onBack = onBack,
                focusRequester = focusRequester,
                onSearch = {
                    if (uiState.query.isNotBlank()) {
                        viewModel.saveRecentSearch(uiState.query)
                        keyboardController?.hide()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AdvancedFiltersRow(
                selectedFilters = selectedFilters,
                onToggle = { filter ->
                    selectedFilters = if (selectedFilters.contains(filter)) {
                        selectedFilters - filter
                    } else {
                        selectedFilters + filter
                    }
                }
            )

            when {
                uiState.isSearching -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        userScrollEnabled = false
                    ) {
                        item {
                            SearchSectionHeader(title = "البحث عن نتائج...", count = 0)
                        }
                        items(5) {
                            SearchResultItemSkeleton()
                        }
                    }
                }

                uiState.query.isBlank() -> {
                    EmptyQueryState(
                        recentSearches = uiState.recentSearches,
                        suggestedCategories = uiState.suggestedCategories,
                        onSearchClick = { viewModel.onQueryChange(it) },
                        onRemove = { viewModel.removeRecentSearch(it) },
                        onClearAll = { viewModel.clearRecentSearches() }
                    )
                }

                uiState.transactions.isEmpty() &&
                        uiState.categories.isEmpty() &&
                        uiState.accounts.isEmpty() -> {
                    SearchEmptyState(query = uiState.query)
                }

                else -> {
                    SearchResultsList(
                        uiState = uiState,
                        onTransactionClick = { t ->
                            viewModel.saveRecentSearch(uiState.query)
                            onTransactionClick(t.id)
                        }
                    )
                }
            }
        }
    }
}
