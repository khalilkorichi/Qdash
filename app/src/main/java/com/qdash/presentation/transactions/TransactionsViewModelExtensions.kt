package com.qdash.presentation.transactions

import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.usecase.transaction.BulkEditTransactionsUseCase
import com.qdash.domain.usecase.transaction.BulkEditParams
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch


/**
 * Extension methods for TransactionsViewModel to perform category and template operations,
 * and bulk transaction editing. Extracted to keep TransactionsViewModel.kt under the SIZE-001 500-line threshold.
 */

internal fun findLocalCategorySuggestion(
    note: String,
    stateSnapshot: TransactionsUiState,
    keywordMatcher: com.qdash.data.categorization.KeywordMatcher,
    transactionType: TransactionType? = null
): Pair<CategorySuggestion, Category>? {
    val normalized = keywordMatcher.normalize(note)
    if (normalized.isEmpty()) return null

    val expectedType = transactionType?.takeIf { it == TransactionType.EXPENSE || it == TransactionType.INCOME }
    val candidateCategories = if (expectedType != null) {
        stateSnapshot.categories.filter { it.type.name == expectedType.name }
    } else {
        stateSnapshot.categories
    }
    val candidateIds = candidateCategories.map { it.id }.toSet()

    // 1. Fast local history check from loaded transactions (strictly matching transaction type)
    val historyMatchCatId = stateSnapshot.transactions
        .asSequence()
        .filter { tx ->
            tx.categoryId != null &&
                tx.categoryId in candidateIds &&
                (expectedType == null || tx.type == expectedType) &&
                !tx.note.isNullOrBlank()
        }
        .firstOrNull { tx ->
            val txNorm = keywordMatcher.normalize(tx.note.orEmpty())
            txNorm == normalized || (normalized.length >= 4 && keywordMatcher.scoreKeywordMatch(txNorm, normalized) >= 400)
        }?.categoryId

    if (historyMatchCatId != null) {
        val cat = candidateCategories.find { it.id == historyMatchCatId }
        if (cat != null) {
            return Pair(
                CategorySuggestion(
                    suggestedCategoryId = cat.id,
                    suggestionSource = SuggestionSource.HISTORY,
                    confidenceScore = 0.94f
                ),
                cat
            )
        }
    }

    // 2. Fast Algerian Darja & Franco-Algerian keyword dictionary check (strictly matching transaction type)
    val kwMatch = com.qdash.data.categorization.AlgerianKeywordDictionary.findMatchingCategory(
        normalizedTitle = normalized,
        transactionType = expectedType?.name
    )
    if (kwMatch != null) {
        fun findFlexible(targetName: String?, requireSub: Boolean): Category? {
            if (targetName.isNullOrBlank()) return null
            val aliases = com.qdash.data.categorization.RuleBasedCategorizationEngine.getCategoryAliases(targetName)
            val pool = if (requireSub) candidateCategories.filter { it.parentId != null } else candidateCategories
            return pool.find { it.name.equals(targetName, ignoreCase = true) }
                ?: pool.find { it.name.contains(targetName) || targetName.contains(it.name) }
                ?: pool.find { cat -> aliases.any { cat.name.contains(it) } }
        }
        val targetCat = findFlexible(kwMatch.seededSubcategoryTarget, requireSub = true)
            ?: findFlexible(kwMatch.subcategoryName, requireSub = true)
            ?: findFlexible(kwMatch.seededCategoryTarget, requireSub = false)
            ?: findFlexible(kwMatch.categoryName, requireSub = false)

        if (targetCat != null) {
            return Pair(
                CategorySuggestion(
                    suggestedCategoryId = targetCat.id,
                    suggestionSource = SuggestionSource.KEYWORD,
                    confidenceScore = 0.92f
                ),
                targetCat
            )
        }
    }
    return null
}

fun TransactionsViewModel.saveAsTemplate(
    name: String,
    amount: Double,
    type: TransactionType,
    accountId: Long,
    targetAccountId: Long?,
    categoryId: Long?,
    subcategoryId: Long?,
    notes: String?,
    iconEmoji: String,
    isPinned: Boolean
) {
    viewModelScope.launch {
        val template = TransactionTemplate(
            name = name,
            amount = amount,
            transactionType = type,
            accountId = accountId,
            targetAccountId = targetAccountId,
            categoryId = categoryId,
            subcategoryId = subcategoryId,
            notes = notes,
            iconEmoji = iconEmoji,
            colorHex = String.format("#%06X", (0xFFFFFF and when (type) {
                TransactionType.EXPENSE -> 0xFFEF4444.toInt()
                TransactionType.INCOME -> 0xFF22C55E.toInt()
                TransactionType.TRANSFER -> 0xFF3B82F6.toInt()
            })),
            isPinned = isPinned,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        templateRepository.insertTemplate(template)
    }
}

fun TransactionsViewModel.learnMapping(text: String, categoryId: Long) {
    viewModelScope.launch {
        learnCategoryMappingUseCase(text, categoryId)
    }
}

fun TransactionsViewModel.createCategoryAndSelect(
    name: String,
    typeStr: String,
    color: String,
    icon: String,
    onCreated: (Long) -> Unit
) {
    viewModelScope.launch {
        try {
            val catType = if (typeStr == "INCOME") CategoryType.INCOME else CategoryType.EXPENSE
            val newCat = Category(
                name = name,
                type = catType,
                color = color,
                icon = icon,
                isSystem = false
            )
            val id = categoryRepository.insertCategory(newCat)
            onCreated(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun TransactionsViewModel.addCategory(
    name: String,
    type: CategoryType,
    icon: String,
    color: String,
    parentId: Long? = null
) {
    viewModelScope.launch {
        val category = Category(
            name = name,
            type = type,
            icon = icon,
            color = color,
            parentId = parentId
        )
        categoryRepository.insertCategory(category)
    }
}

fun TransactionsViewModel.bulkEdit(newCategoryId: Long?, newAccountId: Long?) {
    val selectedIds = _uiState.value.selectedTransactionIds.toList()
    if (selectedIds.isEmpty()) return
    viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true) }
        bulkEditTransactionsUseCase(BulkEditParams(selectedIds, newCategoryId, newAccountId))
            .onSuccess { count ->
                clearTransactionSelection()
                _uiState.update { it.copy(isSaving = false) }
                _bulkEditEvent.emit(BulkEditEvent.Success(count))
            }
            .onFailure { error ->
                _uiState.update { it.copy(isSaving = false, error = error.localizedMessage) }
                _bulkEditEvent.emit(BulkEditEvent.Error(error.localizedMessage ?: "فشل تحديث العمليات"))
            }
    }
}

fun TransactionsViewModel.deleteSelectedTransactions() {
    val ids = _uiState.value.selectedTransactionIds.toList()
    if (ids.isEmpty()) return
    viewModelScope.launch {
        try {
            transactionRepository.deleteTransactionsBulk(ids)
            clearTransactionSelection()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.localizedMessage) }
        }
    }
}

fun TransactionsViewModel.changeCategoryForSelectedTransactions(newCategoryId: Long) {
    val ids = _uiState.value.selectedTransactionIds.toList()
    if (ids.isEmpty()) return
    viewModelScope.launch {
        try {
            transactionRepository.updateTransactionsCategoryBulk(ids, newCategoryId)
            clearTransactionSelection()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.localizedMessage) }
        }
    }
}

fun TransactionsViewModel.mergeCategories(sourceCategoryId: Long, targetCategoryId: Long) {
    viewModelScope.launch {
        try {
            categoryRepository.mergeCategories(sourceCategoryId, targetCategoryId)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.localizedMessage) }
        }
    }
}

fun TransactionsViewModel.toggleSmartCategorySort() {
    val nextVal = !preferencesManager.smartCategorySortEnabled
    preferencesManager.smartCategorySortEnabled = nextVal
    _uiState.update { it.copy(smartCategorySortEnabled = nextVal) }
}

fun TransactionsViewModel.addTransaction(
    amount: Double,
    type: TransactionType,
    categoryId: Long?,
    accountId: Long,
    toAccountId: Long?,
    note: String?,
    date: Long,
    isRecurring: Boolean,
    recurringPeriod: String? = null,
    tags: String? = null,
    kind: TransactionKind? = null,
    occurredAt: Long? = null
) {
    viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true, saveCompleted = false, error = null) }
        try {
            val resolvedKind = kind ?: when (type) {
                TransactionType.TRANSFER -> TransactionKind.TRANSFER
                TransactionType.INCOME -> {
                    val acc = _uiState.value.accounts.find { it.id == accountId }
                    if (acc?.type == AccountType.SAVINGS) TransactionKind.SAVINGS_CONTRIBUTION else TransactionKind.INCOME
                }
                TransactionType.EXPENSE -> {
                    val acc = _uiState.value.accounts.find { it.id == accountId }
                    if (acc?.type == AccountType.SAVINGS) TransactionKind.SAVINGS_WITHDRAWAL else TransactionKind.EXPENSE
                }
            }
            val transaction = Transaction(
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                toAccountId = toAccountId,
                note = note,
                date = date,
                isRecurring = isRecurring,
                recurringPeriod = recurringPeriod,
                tags = tags,
                kind = resolvedKind,
                occurredAt = occurredAt
            )
            transactionRepository.insertTransaction(transaction)

            if (type == TransactionType.INCOME) {
                val category = _uiState.value.categories.find { it.id == categoryId }
                if (category?.name?.contains("راتب") == true || category?.name?.contains("الراتب") == true) {
                    val sources = incomeRepository.getAllIncomeSources().firstOrNull() ?: emptyList()
                    val existingSalary = sources.find { it.type == "SALARY" && it.accountId == accountId }
                    if (existingSalary == null) {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = date }
                        incomeRepository.insertIncomeSource(
                            IncomeSource(
                                name = note?.takeIf { it.isNotBlank() } ?: "الراتب الشهري",
                                amount = amount,
                                type = "SALARY",
                                accountId = accountId,
                                dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH),
                                isActive = true
                            )
                        )
                    }
                }
            }
            _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false, error = e.localizedMessage ?: "تعذر حفظ العملية.") }
        }
    }
}

fun TransactionsViewModel.updateTransaction(
    id: Long,
    amount: Double,
    type: TransactionType,
    categoryId: Long?,
    accountId: Long,
    toAccountId: Long?,
    note: String?,
    date: Long,
    isRecurring: Boolean,
    recurringPeriod: String? = null,
    tags: String? = null,
    kind: TransactionKind? = null,
    occurredAt: Long? = null
) {
    viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true, saveCompleted = false, error = null) }
        try {
            val resolvedKind = kind ?: when (type) {
                TransactionType.TRANSFER -> TransactionKind.TRANSFER
                TransactionType.INCOME -> {
                    val acc = _uiState.value.accounts.find { it.id == accountId }
                    if (acc?.type == AccountType.SAVINGS) TransactionKind.SAVINGS_CONTRIBUTION else TransactionKind.INCOME
                }
                TransactionType.EXPENSE -> {
                    val acc = _uiState.value.accounts.find { it.id == accountId }
                    if (acc?.type == AccountType.SAVINGS) TransactionKind.SAVINGS_WITHDRAWAL else TransactionKind.EXPENSE
                }
            }
            val transaction = Transaction(
                id = id,
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                toAccountId = toAccountId,
                note = note,
                date = date,
                isRecurring = isRecurring,
                recurringPeriod = recurringPeriod,
                tags = tags,
                kind = resolvedKind,
                occurredAt = occurredAt
            )
            transactionRepository.updateTransaction(transaction)
            _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false, error = e.localizedMessage ?: "تعذر حفظ العملية.") }
        }
    }
}


