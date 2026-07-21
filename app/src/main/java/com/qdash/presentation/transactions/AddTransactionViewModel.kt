package com.qdash.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.usecase.transaction.GetSmartCategorySuggestionUseCase
import com.qdash.domain.usecase.transaction.LearnCategoryMappingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Dedicated ViewModel for AddTransactionScreen state handling and debounced smart category suggestions.
 */
class AddTransactionViewModel(
    private val getSmartCategorySuggestionUseCase: GetSmartCategorySuggestionUseCase,
    private val learnCategoryMappingUseCase: LearnCategoryMappingUseCase
) : ViewModel() {

    private val _suggestion = MutableStateFlow<CategorySuggestion?>(null)
    val suggestion: StateFlow<CategorySuggestion?> = _suggestion.asStateFlow()

    private var suggestionJob: Job? = null

    var isEditMode: Boolean = false
        private set
    var originalAmount: Double = 0.0
        private set
    var originalType: com.qdash.domain.model.TransactionType? = null
        private set
    var originalAccountId: Long? = null
        private set
    var originalToAccountId: Long? = null
        private set

    fun initEditMode(
        amount: Double,
        type: com.qdash.domain.model.TransactionType,
        accountId: Long?,
        toAccountId: Long? = null
    ) {
        isEditMode = true
        originalAmount = amount
        originalType = type
        originalAccountId = accountId
        originalToAccountId = toAccountId
    }

    fun calculatePreviewBalances(
        accounts: List<com.qdash.domain.model.Account>,
        selectedAccountId: Long?,
        toAccountId: Long?,
        type: com.qdash.domain.model.TransactionType,
        parsedAmount: Double
    ): Map<Long, Double> {
        return calculateExpectedBalances(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            toAccountId = toAccountId,
            type = type,
            parsedAmount = parsedAmount,
            isEditMode = isEditMode,
            originalAmount = originalAmount,
            originalType = originalType,
            originalAccountId = originalAccountId,
            originalToAccountId = originalToAccountId
        )
    }

    fun onTitleOrNoteChanged(text: String, amount: Double? = null, accountId: Long? = null) {
        suggestionJob?.cancel()
        if (text.trim().isEmpty()) {
            _suggestion.value = null
            return
        }

        suggestionJob = viewModelScope.launch {
            try {
                // Non-blocking 200ms input debouncing
                delay(200L)
                val result = getSmartCategorySuggestionUseCase(text, amount, accountId)
                _suggestion.value = result
            } catch (e: Exception) {
                // Silently ignore cancellation or error
            }
        }
    }

    fun acceptSuggestion(text: String, categoryId: Long) {
        viewModelScope.launch {
            try {
                learnCategoryMappingUseCase(text, categoryId)
                _suggestion.value = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSuggestion() {
        _suggestion.value = null
    }
}
