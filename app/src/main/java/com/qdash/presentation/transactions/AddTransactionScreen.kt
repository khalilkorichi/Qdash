package com.qdash.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.domain.model.TransactionType
import com.qdash.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionsViewModel,
    addTxViewModel: AddTransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.qdash.presentation.ViewModelFactory(
            (LocalContext.current.applicationContext as com.qdash.FinTrackApp).container,
            LocalContext.current
        )
    ),
    initialType: String = "EXPENSE",
    transactionId: Long? = null,
    initialDate: Long? = null,
    draftJson: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val showAmountWords = uiState.isAmountWordsEnabled

    var rawAmountValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue("0", selection = TextRange(1)))
    }
    val rawAmount = rawAmountValue.text
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var isAddingSubcategory by rememberSaveable { mutableStateOf(false) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }
    var newCategoryIcon by rememberSaveable { mutableStateOf("📁") }
    var newCategoryColor by rememberSaveable { mutableStateOf("#8B5CF6") }

    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var subcategoryId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Auto-select newly created category or subcategory
    var previousCategories by remember { mutableStateOf<List<com.qdash.domain.model.Category>>(emptyList()) }
    LaunchedEffect(uiState.categories) {
        if (previousCategories.isNotEmpty() && uiState.categories.size > previousCategories.size) {
            val newlyAdded = uiState.categories.filter { newCat ->
                previousCategories.none { oldCat -> oldCat.id == newCat.id }
            }
            newlyAdded.firstOrNull()?.let { newCat ->
                if (newCat.parentId != null) {
                    selectedCategoryId = newCat.parentId
                    subcategoryId = newCat.id
                } else {
                    selectedCategoryId = newCat.id
                    subcategoryId = null
                }
            }
        }
        previousCategories = uiState.categories
    }

    var note by rememberSaveable { mutableStateOf("") }
    var isRecurring by rememberSaveable { mutableStateOf(false) }
    var recurringPeriod by rememberSaveable { mutableStateOf("MONTHLY") }
    var selectedTags by rememberSaveable(saver = Saver<MutableState<List<String>>, String>(
        save = { it.value.joinToString(",") },
        restore = { mutableStateOf(it.split(",").filter { tag -> tag.isNotBlank() }) }
    )) { mutableStateOf<List<String>>(emptyList()) }
    var isKeypadExpanded by rememberSaveable { mutableStateOf(true) }
    var transactionDate by rememberSaveable { mutableStateOf(initialDate ?: System.currentTimeMillis()) }
    var occurredAt by rememberSaveable { mutableStateOf<Long?>(System.currentTimeMillis()) }

    var type by rememberSaveable(saver = Saver<MutableState<TransactionType>, String>(
        save = { it.value.name },
        restore = { mutableStateOf(TransactionType.valueOf(it)) }
    )) {
        mutableStateOf(
            when (initialType) {
                "INCOME"   -> TransactionType.INCOME
                "TRANSFER" -> TransactionType.TRANSFER
                else       -> TransactionType.EXPENSE
            }
        )
    }

    var selectedAccountId   by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId         by rememberSaveable { mutableStateOf<Long?>(null) }

    var isSalaryAutomation by rememberSaveable { mutableStateOf(false) }
    var showSavingsConfirmDialog by remember { mutableStateOf(false) }

    var hasLoadedInitialData by rememberSaveable { mutableStateOf(false) }
    var hasLoadedDraft by rememberSaveable { mutableStateOf(false) }

    val parsedAmount = remember(rawAmount) {
        com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
    }

    val expectedBalances = remember(
        uiState.accounts, selectedAccountId, toAccountId, type, parsedAmount,
        addTxViewModel.isEditMode, addTxViewModel.originalAmount, addTxViewModel.originalType,
        addTxViewModel.originalAccountId, addTxViewModel.originalToAccountId
    ) {
        addTxViewModel.calculatePreviewBalances(
            accounts = uiState.accounts,
            selectedAccountId = selectedAccountId,
            toAccountId = toAccountId,
            type = type,
            parsedAmount = parsedAmount
        )
    }

    val activeBudget = remember(selectedCategoryId, uiState.budgetGoals, type) {
        if (type == TransactionType.EXPENSE) {
            uiState.budgetGoals.find { it.linkedCategoryId == selectedCategoryId }
        } else {
            null
        }
    }

    val effectiveCategoryId = subcategoryId ?: selectedCategoryId
    val effectiveAccountId = selectedAccountId
        ?: uiState.accounts.find { it.isDefault }?.id
        ?: uiState.accounts.firstOrNull()?.id
    val canSaveTransaction = parsedAmount > 0.0 &&
        (type == TransactionType.TRANSFER || effectiveCategoryId != null) &&
        effectiveAccountId != null &&
        (type != TransactionType.TRANSFER || (toAccountId != null && effectiveAccountId != toAccountId)) &&
        !uiState.isSaving

    AddTransactionEffects(
        uiState = uiState,
        transactionId = transactionId,
        draftJson = draftJson,
        type = type,
        selectedCategoryId = selectedCategoryId,
        selectedAccountId = selectedAccountId,
        toAccountId = toAccountId,
        onBack = onBack,
        viewModel = viewModel,
        context = context,
        onRawAmountChange = { rawAmountValue = it },
        onNoteChange = { note = it },
        onRecurringChange = { isRecurring = it },
        onRecurringPeriodChange = { recurringPeriod = it },
        onTagsChange = { selectedTags = it },
        onTypeChange = { type = it },
        onSelectedCategoryIdChange = { selectedCategoryId = it },
        onSubcategoryIdChange = { subcategoryId = it },
        onSelectedAccountIdChange = { selectedAccountId = it },
        onToAccountIdChange = { toAccountId = it },
        onTransactionDateChange = { transactionDate = it },
        onOccurredAtChange = { occurredAt = it },
        onSalaryAutomationChange = { isSalaryAutomation = it },
        onKeypadExpandedChange = { isKeypadExpanded = it },
        hasLoadedInitialData = hasLoadedInitialData,
        onLoadedInitialDataChange = { hasLoadedInitialData = it },
        hasLoadedDraft = hasLoadedDraft,
        onLoadedDraftChange = { hasLoadedDraft = it },
        onInitEditMode = { amt, tp, accId, toAccId ->
            addTxViewModel.initEditMode(amt, tp, accId, toAccId)
        }
    )

    var showSaveTemplateDialog by remember { mutableStateOf(false) }

    val operatorsList = setOf("+", "-", "×", "÷")

    val displayAmount: String = remember(rawAmount) {
        getFormattedDisplayAmount(rawAmount, operatorsList)
    }

    val hasOperators = remember(rawAmount) {
        operatorsList.any { rawAmount.contains(it) }
    }
    val livePreviewAmount: String = remember(rawAmount) {
        getLivePreviewAmount(rawAmount, hasOperators)
    }

    val typeAccentColor = when (type) {
        TransactionType.EXPENSE  -> ExpenseRed
        TransactionType.INCOME   -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }

    val scrollState = rememberScrollState()

    val onSaveClick = {
        performSaveTransaction(
            rawAmount = rawAmount,
            activeBudget = activeBudget,
            type = type,
            effectiveCategoryId = effectiveCategoryId,
            effectiveAccountId = effectiveAccountId,
            toAccountId = toAccountId,
            isSalaryAutomation = isSalaryAutomation,
            isRecurring = isRecurring,
            recurringPeriod = recurringPeriod,
            selectedTags = selectedTags,
            transactionDate = transactionDate,
            occurredAt = occurredAt,
            transactionId = transactionId,
            note = note,
            viewModel = viewModel,
            uiState = uiState,
            haptic = haptic,
            scope = scope,
            context = context,
            onSavingsConfirmRequired = { showSavingsConfirmDialog = true }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("add_transaction_screen")
    ) {
        FinTrackTopBar(
            title = if (transactionId != null) "تعديل العملية المالية" else "إضافة عملية مالية",
            showBackButton = true,
            onBackClick = onBack
        )

        // Form content
        AddTransactionFormContent(
            type = type,
            onTypeChange = { type = it },
            rawAmountValue = rawAmountValue,
            onAmountValueChange = { rawAmountValue = it },
            displayAmount = displayAmount,
            livePreviewAmount = livePreviewAmount,
            showAmountWords = showAmountWords,
            note = note,
            onNoteChange = { note = it },
            selectedTags = selectedTags,
            onTagsChange = { selectedTags = it },
            activeBudget = activeBudget,
            uiState = uiState,
            selectedCategoryId = selectedCategoryId,
            onSelectedCategoryIdChange = { selectedCategoryId = it },
            subcategoryId = subcategoryId,
            onSubcategoryIdChange = { subcategoryId = it },
            selectedAccountId = selectedAccountId,
            onSelectedAccountIdChange = { selectedAccountId = it },
            toAccountId = toAccountId,
            onToAccountIdChange = { toAccountId = it },
            transactionDate = transactionDate,
            onTransactionDateChange = { transactionDate = it },
            occurredAt = occurredAt,
            onOccurredAtChange = { occurredAt = it },
            isRecurring = isRecurring,
            onRecurringChange = { isRecurring = it },
            recurringPeriod = recurringPeriod,
            onRecurringPeriodChange = { recurringPeriod = it },
            isSalaryAutomation = isSalaryAutomation,
            onSalaryAutomationChange = { isSalaryAutomation = it },
            onAddMainCategory = {
                newCategoryName = ""
                newCategoryIcon = "📁"
                newCategoryColor = "#8B5CF6"
                isAddingSubcategory = false
                showAddCategoryDialog = true
            },
            onAddSubCategory = {
                newCategoryName = ""
                newCategoryIcon = "📁"
                newCategoryColor = "#8B5CF6"
                isAddingSubcategory = true
                showAddCategoryDialog = true
            },
            onSmartSortToggle = { viewModel.toggleSmartCategorySort() },
            onAiSuggest = {
                viewModel.onNoteChanged(
                    note.ifBlank { "auto" },
                    parsedAmount,
                    selectedAccountId
                )
            },
            onAcceptSuggestion = { catId ->
                selectedCategoryId = catId
                viewModel.learnMapping(note, catId)
                viewModel.clearSuggestion()
            },
            onAcceptAndCreateSuggestion = {
                val newSuggestion = uiState.currentSuggestion
                if (newSuggestion != null && newSuggestion.newCategoryName != null) {
                    viewModel.createCategoryAndSelect(
                        name = newSuggestion.newCategoryName,
                        typeStr = newSuggestion.newCategoryType ?: "EXPENSE",
                        color = newSuggestion.newCategoryColor ?: "#7f7f7f",
                        icon = newSuggestion.newCategoryIcon ?: "category",
                        onCreated = { id ->
                            selectedCategoryId = id
                            viewModel.learnMapping(note, id)
                            viewModel.clearSuggestion()
                        }
                    )
                }
            },
            onDismissSuggestion = { viewModel.clearSuggestion() },
            expectedBalances = expectedBalances,
            parsedAmount = parsedAmount,
            typeAccentColor = typeAccentColor,
            primaryColor = primaryColor,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        )

        // Bottom bar keypad & save actions
        AddTransactionBottomBar(
            isKeypadExpanded = isKeypadExpanded,
            onKeypadExpandedChange = { isKeypadExpanded = it },
            displayAmount = displayAmount,
            rawAmountValue = rawAmountValue,
            onAmountValueChange = { rawAmountValue = it },
            type = type,
            transactionId = transactionId,
            rawAmount = rawAmount,
            canSaveTransaction = canSaveTransaction,
            typeAccentColor = typeAccentColor,
            onSaveClick = onSaveClick,
            onShowSaveTemplateDialog = { showSaveTemplateDialog = true }
        )

        // Dialogs Container
        AddTransactionDialogsContainer(
            showAddCategoryDialog = showAddCategoryDialog,
            onDismissAddCategory = { showAddCategoryDialog = false },
            showSavingsConfirmDialog = showSavingsConfirmDialog,
            onDismissSavingsConfirm = { showSavingsConfirmDialog = false },
            showSaveTemplateDialog = showSaveTemplateDialog,
            onDismissSaveTemplate = { showSaveTemplateDialog = false },
            type = type,
            typeAccentColor = typeAccentColor,
            isAddingSubcategory = isAddingSubcategory,
            selectedCategoryId = selectedCategoryId,
            selectedAccountId = selectedAccountId,
            toAccountId = toAccountId,
            subcategoryId = subcategoryId,
            rawAmount = rawAmount,
            note = note,
            transactionDate = transactionDate,
            isRecurring = isRecurring,
            recurringPeriod = recurringPeriod,
            selectedTags = selectedTags,
            transactionId = transactionId,
            uiState = uiState,
            viewModel = viewModel,
            onConfirmSavingsAction = { kind ->
                val parsedAmountVal = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
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
                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
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
                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                        kind = kind,
                        occurredAt = occurredAt
                    )
                }
            }
        )
    }
}
