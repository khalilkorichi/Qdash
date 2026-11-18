package com.qdash.presentation.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.transactions.components.UnifiedCategorySelectorCard
import com.qdash.ui.theme.TextGray
import com.qdash.ui.theme.TransferBlue

@Composable
fun AddTransactionFormContent(
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    rawAmountValue: TextFieldValue,
    onAmountValueChange: (TextFieldValue) -> Unit,
    displayAmount: String,
    livePreviewAmount: String,
    showAmountWords: Boolean,
    note: String,
    onNoteChange: (String) -> Unit,
    selectedTags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    activeBudget: BudgetGoal?,
    uiState: TransactionsUiState,
    selectedCategoryId: Long?,
    onSelectedCategoryIdChange: (Long?) -> Unit,
    subcategoryId: Long?,
    onSubcategoryIdChange: (Long?) -> Unit,
    selectedAccountId: Long?,
    onSelectedAccountIdChange: (Long?) -> Unit,
    toAccountId: Long?,
    onToAccountIdChange: (Long?) -> Unit,
    transactionDate: Long,
    onTransactionDateChange: (Long) -> Unit,
    occurredAt: Long?,
    onOccurredAtChange: (Long?) -> Unit,
    isRecurring: Boolean,
    onRecurringChange: (Boolean) -> Unit,
    recurringPeriod: String,
    onRecurringPeriodChange: (String) -> Unit,
    isSalaryAutomation: Boolean,
    onSalaryAutomationChange: (Boolean) -> Unit,
    onAddMainCategory: () -> Unit,
    onAddSubCategory: () -> Unit,
    onSmartSortToggle: () -> Unit,
    onAiSuggest: () -> Unit,
    onAcceptSuggestion: (Long) -> Unit,
    onAcceptAndCreateSuggestion: () -> Unit,
    onDismissSuggestion: () -> Unit,
    expectedBalances: Map<Long, Double>,
    parsedAmount: Double,
    typeAccentColor: Color,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    onNoteFocused: () -> Unit = {},
    onAmountTap: () -> Unit = {},
    onCategoryPickerOpened: () -> Unit = {}
) {
    val nonArchivedAccounts = remember(uiState.accounts) {
        uiState.accounts.filter { !it.isArchived }
    }
    val effectiveFromAccountId = selectedAccountId
        ?: nonArchivedAccounts.find { it.isDefault }?.id
        ?: nonArchivedAccounts.firstOrNull()?.id

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // 1. Transaction Type Selector
        TypeSelectorBar(
            selected = type,
            onSelect = onTypeChange
        )

        // 2. Amount Display Card
        AmountDisplayCard(
            rawAmountValue = rawAmountValue,
            onValueChange = onAmountValueChange,
            displayAmount = displayAmount,
            livePreviewAmount = livePreviewAmount,
            accentColor = typeAccentColor,
            showAmountWords = showAmountWords,
            onTap = onAmountTap
        )

        // 3. Note Input (directly above Unified Category Card so Smart Suggestions appear right beneath)
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onNoteFocused()
                    }
                }
                .testTag("note_input"),
            placeholder = {
                Text(
                    text = "أضف ملاحظة أو سبب المعاملة (مثلاً: قهوة، فليكسي، قضيان…)",
                    color = TextGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null, tint = TextGray)
            },
            trailingIcon = if (note.isNotEmpty()) {
                {
                    IconButton(onClick = { onNoteChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "مسح الملاحظة",
                            tint = TextGray
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = typeAccentColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = typeAccentColor
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        // 4. Unified Category Selector + Integrated Smart Suggestion Box (Expense / Income)
        if (type != TransactionType.TRANSFER) {
            UnifiedCategorySelectorCard(
                categories = uiState.categories,
                transactions = uiState.transactions,
                type = type,
                selectedCategoryId = selectedCategoryId,
                subcategoryId = subcategoryId,
                typeAccentColor = typeAccentColor,
                smartSortEnabled = uiState.smartCategorySortEnabled,
                suggestion = uiState.currentSuggestion,
                matchedCategory = uiState.suggestedCategory,
                isSuggestingCategory = uiState.isSuggestingCategory,
                onToggleSmartSort = onSmartSortToggle,
                onCategorySelected = { parentCatId, subCatId ->
                    onSelectedCategoryIdChange(parentCatId)
                    onSubcategoryIdChange(subCatId)
                },
                onAddMainCategory = onAddMainCategory,
                onAddSubCategory = onAddSubCategory,
                onAiSuggest = onAiSuggest,
                onAcceptSuggestion = onAcceptSuggestion,
                onAcceptAndCreateSuggestion = onAcceptAndCreateSuggestion,
                onDismissSuggestion = onDismissSuggestion,
                onPickerOpened = onCategoryPickerOpened
            )

            BudgetWarningCard(
                activeBudget = activeBudget,
                inputAmount = parsedAmount
            )
        }

        // 5. Source Account Selector
        if (nonArchivedAccounts.isNotEmpty()) {
            Column {
                SectionLabel(
                    text = when (type) {
                        TransactionType.INCOME   -> "الإيداع في حساب"
                        TransactionType.EXPENSE  -> "الدفع من حساب"
                        TransactionType.TRANSFER -> "من حساب"
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                AccountPickerRow(
                    accounts = nonArchivedAccounts,
                    selectedId = effectiveFromAccountId,
                    accentColor = typeAccentColor,
                    disabledId = null,
                    expectedBalances = expectedBalances,
                    parsedAmount = parsedAmount,
                    onSelect = { chosenId ->
                        onSelectedAccountIdChange(chosenId)
                        if (type == TransactionType.TRANSFER && toAccountId == chosenId) {
                            val fallbackTo = nonArchivedAccounts.firstOrNull { it.id != chosenId }?.id
                            onToAccountIdChange(fallbackTo)
                        }
                    }
                )
            }
        }

        // 6. Destination Account Selector (Transfer only)
        if (type == TransactionType.TRANSFER && nonArchivedAccounts.isNotEmpty()) {
            Column {
                SectionLabel(text = "إلى حساب")
                Spacer(modifier = Modifier.height(6.dp))
                AccountPickerRow(
                    accounts = nonArchivedAccounts,
                    selectedId = toAccountId,
                    accentColor = TransferBlue,
                    disabledId = effectiveFromAccountId,
                    expectedBalances = expectedBalances,
                    parsedAmount = parsedAmount,
                    onSelect = onToAccountIdChange
                )
            }
        }

        // 7. Compact Date & Time Selector
        TransactionDateSelector(
            transactionDate = transactionDate,
            onDateChanged = onTransactionDateChange,
            typeAccentColor = typeAccentColor,
            occurredAt = occurredAt,
            onOccurredAtChanged = onOccurredAtChange
        )

        // 8. Tags & Recurring / Automation Options
        TagsSection(
            selectedTags = selectedTags,
            onTagsChanged = onTagsChange,
            typeAccentColor = typeAccentColor
        )

        RecurringOptionsSection(
            isRecurring = isRecurring,
            onRecurringChanged = onRecurringChange,
            recurringPeriod = recurringPeriod,
            onPeriodChanged = onRecurringPeriodChange,
            type = type,
            isSalaryAutomation = isSalaryAutomation,
            onSalaryAutomationChanged = onSalaryAutomationChange,
            typeAccentColor = typeAccentColor,
            primaryColor = primaryColor
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}
