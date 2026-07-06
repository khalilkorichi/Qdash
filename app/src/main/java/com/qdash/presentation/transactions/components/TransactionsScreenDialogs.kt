package com.qdash.presentation.transactions.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.qdash.domain.model.Transaction
import com.qdash.presentation.transactions.BulkDeleteDialog
import com.qdash.presentation.transactions.BulkCategoryDialog
import com.qdash.presentation.transactions.DeleteTransactionDialog
import com.qdash.presentation.transactions.TransactionsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreenDialogs(
    uiState: TransactionsUiState,
    showActionMenuForTransaction: Transaction?,
    onDismissActionMenu: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransactionRequested: (Transaction) -> Unit,
    showDeleteDialog: Transaction?,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteTransaction: (Transaction) -> Unit,
    showBulkDeleteDialog: Boolean,
    onDismissBulkDelete: () -> Unit,
    onConfirmBulkDelete: () -> Unit,
    showBulkCategoryDialog: Boolean,
    onDismissBulkCategory: () -> Unit,
    onConfirmBulkCategory: (Long) -> Unit,
    showFilterSheet: Boolean,
    onDismissFilterSheet: () -> Unit,
    onApplyFilters: (Double?, Long?, Long?) -> Unit,
    onClearFilters: () -> Unit,
    activeSummarySheetDay: Long?,
    onDismissSummarySheet: () -> Unit,
    onViewTransactionsForDay: (Long) -> Unit,
    showBulkEditSheet: Boolean,
    onDismissBulkEdit: () -> Unit,
    onConfirmBulkEdit: (Long?, Long?) -> Unit
) {
    // —— Transaction Action Menu Dialog
    if (showActionMenuForTransaction != null) {
        val tx = showActionMenuForTransaction
        TransactionActionMenuDialog(
            transaction = tx,
            categories = uiState.categories,
            onDismiss = onDismissActionMenu,
            onEdit = { onEditTransaction(tx) },
            onDelete = { onDeleteTransactionRequested(tx) }
        )
    }

    // —— Delete Confirmation Dialog
    showDeleteDialog?.let { txToDelete ->
        DeleteTransactionDialog(
            transaction = txToDelete,
            onDismiss = onDismissDeleteDialog,
            onConfirm = { onConfirmDeleteTransaction(txToDelete) }
        )
    }

    // —— Bulk Delete Confirmation Dialog
    if (showBulkDeleteDialog) {
        BulkDeleteDialog(
            selectedCount = uiState.selectedTransactionIds.size,
            onDismiss = onDismissBulkDelete,
            onConfirm = onConfirmBulkDelete
        )
    }

    // —— Bulk Category Selection Dialog
    if (showBulkCategoryDialog) {
        BulkCategoryDialog(
            categories = uiState.categories,
            onDismiss = onDismissBulkCategory,
            onConfirm = onConfirmBulkCategory
        )
    }

    // —— Advanced Filters Bottom Sheet
    if (showFilterSheet) {
        AdvancedFiltersBottomSheet(
            filterMinAmount = uiState.filterMinAmount,
            filterStartDate = uiState.filterStartDate,
            filterEndDate = uiState.filterEndDate,
            onApplyFilters = onApplyFilters,
            onClearFilters = onClearFilters,
            onDismiss = onDismissFilterSheet
        )
    }

    // —— Daily Financial Summary Bottom Sheet drawer
    if (activeSummarySheetDay != null) {
        DailySummaryBottomSheet(
            selectedDayTs = activeSummarySheetDay,
            dailyAggregates = uiState.dailyAggregates,
            transactions = uiState.transactions,
            categories = uiState.categories,
            onDismiss = onDismissSummarySheet,
            onViewTransactionsForDay = { onViewTransactionsForDay(activeSummarySheetDay) }
        )
    }

    // —— Bulk Edit Bottom Sheet
    if (showBulkEditSheet) {
        val bulkEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        BulkEditBottomSheet(
            selectedCount = uiState.selectedTransactionIds.size,
            categories = uiState.categories,
            accounts = uiState.accounts,
            onConfirm = onConfirmBulkEdit,
            onDismissRequest = onDismissBulkEdit,
            sheetState = bulkEditSheetState
        )
    }
}
