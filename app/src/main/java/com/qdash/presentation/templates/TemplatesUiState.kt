package com.qdash.presentation.templates

import com.qdash.domain.model.TransactionTemplate
import com.qdash.domain.model.Category
import com.qdash.domain.model.Account

data class TemplatesUiState(
    val templates: List<TransactionTemplate> = emptyList(),
    val frequentTemplates: List<TransactionTemplate> = emptyList(),
    val pinnedTemplates: List<TransactionTemplate> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<TransactionTemplate> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteConfirmation: TransactionTemplate? = null,
    val duplicateWarningTemplate: TransactionTemplate? = null
)
