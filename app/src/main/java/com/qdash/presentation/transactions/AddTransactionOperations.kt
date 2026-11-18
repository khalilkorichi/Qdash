package com.qdash.presentation.transactions

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.model.CategoryType
import com.qdash.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun performSaveTransaction(
    rawAmount: String,
    activeBudget: BudgetGoal?,
    type: TransactionType,
    effectiveCategoryId: Long?,
    effectiveAccountId: Long?,
    toAccountId: Long?,
    isSalaryAutomation: Boolean,
    isRecurring: Boolean,
    recurringPeriod: String,
    selectedTags: List<String>,
    transactionDate: Long,
    occurredAt: Long?,
    transactionId: Long?,
    note: String,
    viewModel: TransactionsViewModel,
    uiState: TransactionsUiState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    context: Context,
    onSavingsConfirmRequired: () -> Unit
) {
    val parsedAmountVal = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
    if (parsedAmountVal > 0) {
        val budget = activeBudget
        val isBudgetWarningOrExceeded = if (budget != null) {
            val newSpent = budget.spentAmount + parsedAmountVal
            val isExceeded = newSpent > budget.amountLimit
            val isWarning = !isExceeded && newSpent >= (budget.amountLimit * 0.90)
            isExceeded || isWarning
        } else false

        if (isBudgetWarningOrExceeded) {
            scope.launch {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(150)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        val categoryId = if (type == TransactionType.TRANSFER) null else effectiveCategoryId
        val accountId = effectiveAccountId
        if ((type != TransactionType.TRANSFER && categoryId == null) || accountId == null) {
            Toast.makeText(context, "يرجى اختيار حساب وفئة صالحين قبل الحفظ.", Toast.LENGTH_LONG).show()
            return
        }

        val selectedAccount = uiState.accounts.find { it.id == accountId }
        val isSavings = selectedAccount?.type == com.qdash.domain.model.AccountType.SAVINGS
        
        if (type == TransactionType.INCOME) {
            if (isSalaryAutomation) {
                if (transactionId != null) {
                    viewModel.updateTransaction(
                        id = transactionId,
                        amount = parsedAmountVal,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        toAccountId = null,
                        note = note.ifBlank { null },
                        date = transactionDate,
                        isRecurring = isRecurring,
                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                        kind = com.qdash.domain.model.TransactionKind.SALARY,
                        occurredAt = occurredAt
                    )
                } else {
                    viewModel.addTransaction(
                        amount = parsedAmountVal,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        toAccountId = null,
                        note = note.ifBlank { null },
                        date = transactionDate,
                        isRecurring = isRecurring,
                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                        kind = com.qdash.domain.model.TransactionKind.SALARY,
                        occurredAt = occurredAt
                    )
                }
            } else if (isSavings) {
                onSavingsConfirmRequired()
            } else {
                if (transactionId != null) {
                    viewModel.updateTransaction(
                        id = transactionId,
                        amount = parsedAmountVal,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        toAccountId = null,
                        note = note.ifBlank { null },
                        date = transactionDate,
                        isRecurring = isRecurring,
                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                        kind = com.qdash.domain.model.TransactionKind.INCOME,
                        occurredAt = occurredAt
                    )
                } else {
                    viewModel.addTransaction(
                        amount = parsedAmountVal,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        toAccountId = null,
                        note = note.ifBlank { null },
                        date = transactionDate,
                        isRecurring = isRecurring,
                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                        kind = com.qdash.domain.model.TransactionKind.INCOME,
                        occurredAt = occurredAt
                    )
                }
            }
        } else if (type == TransactionType.EXPENSE) {
            val kind = if (isSavings) com.qdash.domain.model.TransactionKind.SAVINGS_WITHDRAWAL else com.qdash.domain.model.TransactionKind.EXPENSE
            if (transactionId != null) {
                viewModel.updateTransaction(
                    id = transactionId,
                    amount = parsedAmountVal,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    toAccountId = null,
                    note = note.ifBlank { null },
                    date = transactionDate,
                    isRecurring = isRecurring,
                    recurringPeriod = if (isRecurring) recurringPeriod else null,
                    tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                    kind = kind,
                    occurredAt = occurredAt
                )
            } else {
                viewModel.addTransaction(
                    amount = parsedAmountVal,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    toAccountId = null,
                    note = note.ifBlank { null },
                    date = transactionDate,
                    isRecurring = isRecurring,
                    recurringPeriod = if (isRecurring) recurringPeriod else null,
                    tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                    kind = kind,
                    occurredAt = occurredAt
                )
            }
        } else {
            if (transactionId != null) {
                viewModel.updateTransaction(
                    id = transactionId,
                    amount = parsedAmountVal,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    note = note.ifBlank { null },
                    date = transactionDate,
                    isRecurring = isRecurring,
                    recurringPeriod = if (isRecurring) recurringPeriod else null,
                    tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                    kind = com.qdash.domain.model.TransactionKind.TRANSFER,
                    occurredAt = occurredAt
                )
            } else {
                viewModel.addTransaction(
                    amount = parsedAmountVal,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    note = note.ifBlank { null },
                    date = transactionDate,
                    isRecurring = isRecurring,
                    recurringPeriod = if (isRecurring) recurringPeriod else null,
                    tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                    kind = com.qdash.domain.model.TransactionKind.TRANSFER,
                    occurredAt = occurredAt
                )
            }
        }
    }
}


fun parseDraftJson(
    draftJson: String?,
    onResult: (
        amountVal: Double,
        type: TransactionType,
        catId: Long?,
        subcatId: Long?,
        accountId: Long?,
        targetAccountId: Long?,
        notes: String?
    ) -> Unit
) {
    if (draftJson.isNullOrBlank()) return
    val decoded = try {
        if (draftJson.trimStart().startsWith("%7B", ignoreCase = true)) {
            java.net.URLDecoder.decode(draftJson, "UTF-8")
        } else {
            draftJson
        }
    } catch (_: Exception) {
        draftJson
    }

    try {
        val json = org.json.JSONObject(decoded)
        val parsedAmountVal = json.optDouble("amount", 0.0).let { if (it.isNaN()) 0.0 else it }
        val parsedType = runCatching {
            TransactionType.valueOf(json.optString("type", "EXPENSE"))
        }.getOrDefault(TransactionType.EXPENSE)
        val parsedCatId = if (!json.isNull("categoryId") && json.has("categoryId")) json.optLong("categoryId").takeIf { it > 0L } else null
        val parsedSubcatId = if (!json.isNull("subcategoryId") && json.has("subcategoryId")) json.optLong("subcategoryId").takeIf { it > 0L } else null
        val parsedAccountId = if (!json.isNull("accountId") && json.has("accountId")) json.optLong("accountId").takeIf { it > 0L } else null
        val parsedTargetAccountId = if (!json.isNull("targetAccountId") && json.has("targetAccountId")) json.optLong("targetAccountId").takeIf { it > 0L } else null
        val parsedNotes = if (!json.isNull("notes") && json.has("notes")) json.optString("notes").takeIf { it.isNotEmpty() } else null

        onResult(parsedAmountVal, parsedType, parsedCatId, parsedSubcatId, parsedAccountId, parsedTargetAccountId, parsedNotes)
        return
    } catch (_: Exception) {
        // Fallback to legacy key:value parser below
    }

    try {
        val cleanedJson = decoded.trim().removePrefix("{").removeSuffix("}").trim()
        val pairs = cleanedJson.split(",")
        var parsedAmountVal = 0.0
        var parsedType = TransactionType.EXPENSE
        var parsedCatId: Long? = null
        var parsedSubcatId: Long? = null
        var parsedAccountId: Long? = null
        var parsedTargetAccountId: Long? = null
        var parsedNotes: String? = null
        
        pairs.forEach { pair ->
            val parts = pair.split(":")
            if (parts.size >= 2) {
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts.drop(1).joinToString(":").trim().removeSurrounding("\"")
                if (value != "null") {
                    when (key) {
                        "amount" -> parsedAmountVal = value.toDoubleOrNull() ?: 0.0
                        "type" -> parsedType = runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)
                        "categoryId" -> parsedCatId = value.toLongOrNull()
                        "subcategoryId" -> parsedSubcatId = value.toLongOrNull()
                        "accountId" -> parsedAccountId = value.toLongOrNull()
                        "targetAccountId" -> parsedTargetAccountId = value.toLongOrNull()
                        "notes" -> parsedNotes = value.replace("\\\"", "\"")
                    }
                }
            }
        }
        onResult(parsedAmountVal, parsedType, parsedCatId, parsedSubcatId, parsedAccountId, parsedTargetAccountId, parsedNotes)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadInitialTransaction(
    transactionId: Long?,
    transactions: List<com.qdash.domain.model.Transaction>,
    categories: List<com.qdash.domain.model.Category>,
    onResult: (
        amountStr: String,
        note: String,
        isRecurring: Boolean,
        recurringPeriod: String,
        tags: List<String>,
        type: TransactionType,
        selectedCategoryId: Long?,
        subcategoryId: Long?,
        selectedAccountId: Long?,
        toAccountId: Long?,
        transactionDate: Long,
        occurredAt: Long?
    ) -> Unit
) {
    if (transactionId == null || transactions.isEmpty()) return
    val transaction = transactions.find { it.id == transactionId } ?: return
    val amtStr = transaction.amount.toString().replace(".0", "")
    val note = transaction.note ?: ""
    val isRecurring = transaction.isRecurring
    val recurringPeriod = transaction.recurringPeriod ?: "MONTHLY"
    val tags = transaction.tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val type = transaction.type
    
    var selectedCategoryId: Long? = null
    var subcategoryId: Long? = null
    val cat = categories.find { it.id == transaction.categoryId }
    if (cat != null) {
        if (cat.parentId != null) {
            selectedCategoryId = cat.parentId
            subcategoryId = cat.id
        } else {
            selectedCategoryId = cat.id
            subcategoryId = null
        }
    } else {
        selectedCategoryId = transaction.categoryId
        subcategoryId = null
    }
    onResult(
        amtStr, note, isRecurring, recurringPeriod, tags, type,
        selectedCategoryId, subcategoryId, transaction.accountId, transaction.toAccountId, transaction.date,
        transaction.occurredAt
    )
}


fun getFormattedDisplayAmount(rawAmount: String, operatorsList: Set<String>): String {
    val result = if (rawAmount == "0" || rawAmount.isEmpty()) {
        "0"
    } else {
        val parts = rawAmount.split(" ")
        parts.map { part ->
            if (operatorsList.contains(part)) {
                part
            } else if (part.isEmpty()) {
                ""
            } else {
                val subParts = part.split(".")
                val integerPart = subParts[0]
                val decimalPart = if (subParts.size > 1) subParts[1] else null
                val formattedInteger = try {
                    val longVal = integerPart.toLongOrNull()
                    if (longVal != null) {
                        java.text.DecimalFormat("#,###").format(longVal)
                    } else {
                        integerPart
                    }
                } catch (e: Exception) {
                    integerPart
                }
                buildString {
                    append(formattedInteger)
                    if (decimalPart != null) {
                        append(".")
                        append(decimalPart)
                    } else if (part.endsWith(".")) {
                        append(".")
                    }
                }
            }
        }.joinToString(" ")
    }
    return com.qdash.core.utils.FormatterUtils.convertNumerals(result)
}

fun calculateExpectedBalances(
    accounts: List<com.qdash.domain.model.Account>,
    selectedAccountId: Long?,
    toAccountId: Long?,
    type: TransactionType,
    parsedAmount: Double,
    isEditMode: Boolean = false,
    originalAmount: Double = 0.0,
    originalType: TransactionType? = null,
    originalAccountId: Long? = null,
    originalToAccountId: Long? = null
): Map<Long, Double> {
    return accounts.associate { acc ->
        var baseBalance = acc.balance

        if (isEditMode && originalType != null) {
            if (acc.id == originalAccountId) {
                baseBalance = when (originalType) {
                    TransactionType.EXPENSE -> acc.balance + originalAmount
                    TransactionType.INCOME -> acc.balance - originalAmount
                    TransactionType.TRANSFER -> acc.balance + originalAmount
                }
            } else if (originalType == TransactionType.TRANSFER && acc.id == originalToAccountId) {
                baseBalance = acc.balance - originalAmount
            }
        }

        val expected = when (type) {
            TransactionType.EXPENSE -> {
                if (acc.id == selectedAccountId) baseBalance - parsedAmount else baseBalance
            }
            TransactionType.INCOME -> {
                if (acc.id == selectedAccountId) baseBalance + parsedAmount else baseBalance
            }
            TransactionType.TRANSFER -> {
                when (acc.id) {
                    selectedAccountId -> baseBalance - parsedAmount
                    toAccountId -> baseBalance + parsedAmount
                    else -> baseBalance
                }
            }
        }
        acc.id to expected
    }
}

fun getLivePreviewAmount(rawAmount: String, hasOperators: Boolean): String {
    val result = if (hasOperators) {
        val eval = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
        if (eval % 1 == 0.0) {
            java.text.DecimalFormat("#,###").format(eval.toLong())
        } else {
            "%,.2f".format(eval).replace(",", ".")
        }
    } else {
        ""
    }
    return com.qdash.core.utils.FormatterUtils.convertNumerals(result)
}

@Composable
fun AddTransactionEffects(
    uiState: TransactionsUiState,
    transactionId: Long?,
    draftJson: String?,
    type: TransactionType,
    selectedCategoryId: Long?,
    selectedAccountId: Long?,
    toAccountId: Long?,
    onBack: () -> Unit,
    viewModel: TransactionsViewModel,
    context: Context,
    onRawAmountChange: (TextFieldValue) -> Unit,
    onNoteChange: (String) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    onRecurringPeriodChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onSelectedCategoryIdChange: (Long?) -> Unit,
    onSubcategoryIdChange: (Long?) -> Unit,
    onSelectedAccountIdChange: (Long?) -> Unit,
    onToAccountIdChange: (Long?) -> Unit,
    onTransactionDateChange: (Long) -> Unit,
    onOccurredAtChange: (Long?) -> Unit,
    onSalaryAutomationChange: (Boolean) -> Unit,
    onKeypadExpandedChange: (Boolean) -> Unit,
    hasLoadedInitialData: Boolean,
    onLoadedInitialDataChange: (Boolean) -> Unit,
    hasLoadedDraft: Boolean,
    onLoadedDraftChange: (Boolean) -> Unit,
    onInitEditMode: ((amount: Double, type: TransactionType, accountId: Long?, toAccountId: Long?) -> Unit)? = null
) {
    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.consumeSaveCompleted()
            onBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    LaunchedEffect(uiState.transactions, transactionId) {
        if (!hasLoadedInitialData && transactionId != null && uiState.transactions.isNotEmpty()) {
            val tx = uiState.transactions.find { it.id == transactionId }
            if (tx != null) {
                onInitEditMode?.invoke(tx.amount, tx.type, tx.accountId, tx.toAccountId)
            }
            loadInitialTransaction(transactionId, uiState.transactions, uiState.categories) { amtStr, n, isRec, recPer, tgs, tp, selCatId, subCatId, selAccId, toAccId, date, time ->
                onRawAmountChange(TextFieldValue(amtStr, selection = TextRange(amtStr.length)))
                onNoteChange(n)
                onRecurringChange(isRec)
                onRecurringPeriodChange(recPer)
                onTagsChange(tgs)
                onTypeChange(tp)
                onSelectedCategoryIdChange(selCatId)
                onSubcategoryIdChange(subCatId)
                onSelectedAccountIdChange(selAccId)
                onToAccountIdChange(toAccId)
                onTransactionDateChange(date)
                onOccurredAtChange(time)
                onKeypadExpandedChange(false)
                onLoadedInitialDataChange(true)
            }
        }
    }

    LaunchedEffect(draftJson, uiState.categories) {
        if (!hasLoadedDraft && !draftJson.isNullOrBlank()) {
            parseDraftJson(draftJson) { amt, tp, catId, subcatId, accId, targetAccId, notes ->
                val amtStr = if (amt > 0.0) amt.toString().replace(".0", "") else "0"
                onRawAmountChange(TextFieldValue(amtStr, selection = TextRange(amtStr.length)))
                onTypeChange(tp)
                val resolvedCat = catId?.let { id -> uiState.categories.find { it.id == id } }
                if (subcatId != null) {
                    onSelectedCategoryIdChange(catId)
                    onSubcategoryIdChange(subcatId)
                } else if (resolvedCat?.parentId != null) {
                    onSelectedCategoryIdChange(resolvedCat.parentId)
                    onSubcategoryIdChange(resolvedCat.id)
                } else {
                    onSelectedCategoryIdChange(catId)
                    onSubcategoryIdChange(null)
                }
                onSelectedAccountIdChange(accId)
                onToAccountIdChange(targetAccId)
                onNoteChange(notes ?: "")
                onKeypadExpandedChange(false)
                onLoadedDraftChange(true)
            }
        }
    }

    LaunchedEffect(uiState.categories, uiState.accounts, type) {
        if (transactionId != null || hasLoadedDraft) return@LaunchedEffect
        val nonArchivedAccounts = uiState.accounts.filter { !it.isArchived }
        if (selectedCategoryId == null && uiState.categories.isNotEmpty()) {
            val firstCat = uiState.categories.firstOrNull {
                it.parentId == null && when (type) {
                    TransactionType.INCOME -> it.type == CategoryType.INCOME
                    else                   -> it.type == CategoryType.EXPENSE
                }
            }?.id
            onSelectedCategoryIdChange(firstCat)
        }
        if (selectedAccountId == null && nonArchivedAccounts.isNotEmpty()) {
            val defaultAcc = nonArchivedAccounts.find { it.isDefault }?.id
                ?: nonArchivedAccounts.first().id
            onSelectedAccountIdChange(defaultAcc)
        }
        if (toAccountId == null && nonArchivedAccounts.size > 1) {
            val toAcc = nonArchivedAccounts.lastOrNull { it.id != selectedAccountId }?.id
                ?: nonArchivedAccounts.first().id
            onToAccountIdChange(toAcc)
        }
    }

    LaunchedEffect(selectedCategoryId, uiState.categories) {
        if (selectedCategoryId != null) {
            val category = uiState.categories.find { it.id == selectedCategoryId }
            if (category?.name?.contains("راتب") == true || category?.name?.contains("الراتب") == true) {
                onSalaryAutomationChange(true)
            }
        }
    }

    var previousCategories = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<List<com.qdash.domain.model.Category>>(emptyList())
    }
    LaunchedEffect(uiState.categories) {
        if (previousCategories.value.isNotEmpty() && uiState.categories.size > previousCategories.value.size) {
            val newlyAdded = uiState.categories.filter { newCat ->
                previousCategories.value.none { oldCat -> oldCat.id == newCat.id }
            }
            newlyAdded.firstOrNull()?.let { newCat ->
                if (newCat.parentId != null) {
                    onSelectedCategoryIdChange(newCat.parentId)
                    onSubcategoryIdChange(newCat.id)
                } else {
                    onSelectedCategoryIdChange(newCat.id)
                    onSubcategoryIdChange(null)
                }
            }
        }
        previousCategories.value = uiState.categories
    }
}

fun performConfirmedSavingsTransaction(
    rawAmount: String,
    transactionId: Long?,
    type: TransactionType,
    effectiveCategoryId: Long?,
    effectiveAccountId: Long?,
    note: String,
    transactionDate: Long,
    isRecurring: Boolean,
    recurringPeriod: String,
    selectedTags: List<String>,
    kind: com.qdash.domain.model.TransactionKind,
    occurredAt: Long?,
    viewModel: TransactionsViewModel
) {
    val parsedAmountVal = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
    val tagsStr = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null
    val periodStr = if (isRecurring) recurringPeriod else null
    if (transactionId != null) {
        viewModel.updateTransaction(
            id = transactionId,
            amount = parsedAmountVal,
            type = type,
            categoryId = effectiveCategoryId ?: 1L,
            accountId = effectiveAccountId ?: 1L,
            toAccountId = null,
            note = note.ifBlank { null },
            date = transactionDate,
            isRecurring = isRecurring,
            recurringPeriod = periodStr,
            tags = tagsStr,
            kind = kind,
            occurredAt = occurredAt
        )
    } else {
        viewModel.addTransaction(
            amount = parsedAmountVal,
            type = type,
            categoryId = effectiveCategoryId ?: 1L,
            accountId = effectiveAccountId ?: 1L,
            toAccountId = null,
            note = note.ifBlank { null },
            date = transactionDate,
            isRecurring = isRecurring,
            recurringPeriod = periodStr,
            tags = tagsStr,
            kind = kind,
            occurredAt = occurredAt
        )
    }
}

