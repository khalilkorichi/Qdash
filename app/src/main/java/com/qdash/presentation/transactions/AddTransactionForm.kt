package com.qdash.presentation.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.model.TransactionType
import com.qdash.ui.theme.TransferBlue
import com.qdash.ui.theme.TextGray

@OptIn(ExperimentalLayoutApi::class)
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
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Transaction type selector chip bar
        TypeSelectorBar(
            selected = type,
            onSelect = onTypeChange
        )

        // Amount display card
        AmountDisplayCard(
            rawAmountValue = rawAmountValue,
            onValueChange = onAmountValueChange,
            displayAmount = displayAmount,
            livePreviewAmount = livePreviewAmount,
            accentColor = typeAccentColor,
            showAmountWords = showAmountWords,
            onTap = {}
        )

        // Note input (Moved to the top)
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("note_input"),
            placeholder = {
                Text(
                    text = "أضف ملاحظة أو سبب المعاملة…",
                    color = TextGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null, tint = TextGray)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = typeAccentColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = typeAccentColor
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        // Real-time non-blocking smart category suggestion row
        com.qdash.presentation.transactions.components.SmartCategorySuggestionRow(
            suggestion = uiState.currentSuggestion,
            matchedCategory = uiState.suggestedCategory,
            onAcceptSuggestion = { categoryId ->
                onAcceptSuggestion(categoryId)
            },
            onDismissSuggestion = onDismissSuggestion
        )

        TagsSection(
            selectedTags = selectedTags,
            onTagsChanged = onTagsChange,
            typeAccentColor = typeAccentColor
        )

        BudgetWarningCard(
            activeBudget = activeBudget,
            inputAmount = parsedAmount
        )

        // Category dropdown (not for Transfer)
        if (type != TransactionType.TRANSFER) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel(text = "الفئة")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                BorderStroke(1.dp, TransferBlue),
                                RoundedCornerShape(8.dp)
                            )
                            .background(TransferBlue.copy(alpha = 0.08f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAiSuggest()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TransferBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "اقتراح ذكي",
                                style = MaterialTheme.typography.labelSmall,
                                color = TransferBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                CategoryDropdownSelector(
                    categories = uiState.categories,
                    transactions = uiState.transactions,
                    type = type,
                    selectedCategoryId = selectedCategoryId,
                    subcategoryId = subcategoryId,
                    typeAccentColor = typeAccentColor,
                    smartSortEnabled = uiState.smartCategorySortEnabled,
                    onToggleSmartSort = onSmartSortToggle,
                    onCategorySelected = { parentCatId, subCatId ->
                        onSelectedCategoryIdChange(parentCatId)
                        onSubcategoryIdChange(subCatId)
                    },
                    onAddMainCategory = onAddMainCategory,
                    onAddSubCategory = onAddSubCategory,
                    suggestionData = remember(uiState.currentSuggestion, uiState.suggestedCategory, type) {
                        val sugg = uiState.currentSuggestion
                        if (sugg != null && type == TransactionType.EXPENSE) {
                            CategorySuggestionData(
                                existingCategory = uiState.suggestedCategory,
                                newCategoryName = sugg.newCategoryName,
                                newCategoryColor = sugg.newCategoryColor,
                                newCategoryIcon = sugg.newCategoryIcon,
                                confidenceScore = sugg.confidenceScore
                            )
                        } else null
                    },
                    onAcceptSuggestion = onAcceptSuggestion,
                    onAcceptAndCreateSuggestion = onAcceptAndCreateSuggestion,
                    onDismissSuggestion = onDismissSuggestion
                )
            }
        }

        if (uiState.accounts.isNotEmpty()) {
            Column {
                SectionLabel(
                    text = when (type) {
                        TransactionType.INCOME   -> "الإيداع في حِساب"
                        TransactionType.EXPENSE  -> "الدفع من حِساب"
                        TransactionType.TRANSFER -> "من حساب"
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                AccountPickerRow(
                    accounts = uiState.accounts,
                    selectedId = selectedAccountId,
                    accentColor = typeAccentColor,
                    disabledId = null,
                    expectedBalances = expectedBalances,
                    parsedAmount = parsedAmount,
                    onSelect = onSelectedAccountIdChange
                )
            }
        }

        // Destination account (Transfer only)
        if (type == TransactionType.TRANSFER && uiState.accounts.isNotEmpty()) {
            Column {
                SectionLabel(text = "إلى حساب")
                Spacer(modifier = Modifier.height(6.dp))
                AccountPickerRow(
                    accounts = uiState.accounts,
                    selectedId = toAccountId,
                    accentColor = TransferBlue,
                    disabledId = null,
                    expectedBalances = expectedBalances,
                    parsedAmount = parsedAmount,
                    onSelect = onToAccountIdChange
                )
            }
        }

        TransactionDateSelector(
            transactionDate = transactionDate,
            onDateChanged = onTransactionDateChange,
            typeAccentColor = typeAccentColor,
            occurredAt = occurredAt,
            onOccurredAtChanged = onOccurredAtChange
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

        Spacer(modifier = Modifier.height(8.dp))
    }
}
