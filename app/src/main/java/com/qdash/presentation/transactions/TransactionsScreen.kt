package com.qdash.presentation.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.*
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.navigation.Screen
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.qdash.domain.model.Transaction
import androidx.activity.compose.BackHandler
import com.qdash.presentation.transactions.components.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTotal by viewModel.selectedTotal.collectAsStateWithLifecycle()
    val navController = com.qdash.presentation.navigation.LocalNavController.current
    val haptic = LocalHapticFeedback.current
    var showDeleteDialog by remember { mutableStateOf<Transaction?>(null) }
    var showActionMenuForTransaction by remember { mutableStateOf<Transaction?>(null) }
    var activeSummarySheetDay by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showBulkCategoryDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showBulkEditSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.selectedTransactionIds.isNotEmpty()) {
        viewModel.clearTransactionSelection()
    }

    LaunchedEffect(Unit) {
        viewModel.bulkEditEvent.collect { event ->
            when (event) {
                is BulkEditEvent.Success -> {
                    showBulkEditSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "تم تحديث العمليات بنجاح",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is BulkEditEvent.Error -> {
                    showBulkEditSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.error,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    // Compute summary values
    val totalExpenses = remember(uiState.filteredTransactions, uiState.selectedType) {
        if (uiState.selectedType != null && uiState.selectedType != TransactionType.EXPENSE) 0.0
        else uiState.filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val totalIncome = remember(uiState.filteredTransactions, uiState.selectedType) {
        if (uiState.selectedType != null && uiState.selectedType != TransactionType.INCOME) 0.0
        else uiState.filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpenses
    val netColor by animateColorAsState(
        targetValue = if (netBalance >= 0) IncomeGreen else ExpenseRed,
        animationSpec = tween(durationMillis = 400),
        label = "netColor"
    )

    val selectedTransactions = remember(uiState.transactions, uiState.selectedTransactionIds) {
        uiState.transactions.filter { it.id in uiState.selectedTransactionIds }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        var visibleRecentCount by remember(uiState.filteredTransactions, uiState.selectedCalendarDate) {
            mutableStateOf(5)
        }

        val recentTxs = remember(uiState.filteredTransactions, visibleRecentCount) {
            uiState.filteredTransactions.take(visibleRecentCount)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {

                // ── Unified Screen Header ────────────────────────────────────
                item(key = "header") {
                    UnifiedScreenHeader(
                        title = "سجل المعاملات",
                        subtitle = "تتبع تفاصيل وارداتك ومصروفاتك اليومية بدقة",
                        showBackButton = true,
                        onBackClick = onBack,
                        actions = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${uiState.filteredTransactions.size} عمليات",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }

                // ── Search box under Title ───────────────────────────────────
                item(key = "search_box") {
                    val hasActiveFilters = uiState.filterMinAmount != null || uiState.filterStartDate != null || uiState.filterEndDate != null
                    TransactionsSearchBar(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                        hasActiveFilters = hasActiveFilters,
                        onFilterClick = { showFilterSheet = true }
                    )
                }

                // ── Unified Summary & Filters Card ───────────────────────────
                item(key = "summary_and_filters") {
                    TransactionsSummaryCard(
                        isLoading = uiState.isLoading,
                        totalExpenses = totalExpenses,
                        totalIncome = totalIncome,
                        netBalance = netBalance,
                        netColor = netColor,
                        selectedType = uiState.selectedType,
                        onTypeSelected = { viewModel.onTypeSelected(it) },
                        categories = uiState.categories,
                        transactions = uiState.transactions,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onCategorySelected = { viewModel.onCategorySelected(it) }
                    )
                }

                item(key = "spacer_after_summary") {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Financial Activity Calendar ──────────────────────────────
                item(key = "calendar") {
                    FinancialActivityCalendar(
                        uiState = uiState,
                        viewModel = viewModel,
                        listState = listState,
                        onDayDoubleTapped = { activeSummarySheetDay = it }
                    )
                }

                // ── Metric Switcher Row ──────────────────────────────────────
                item(key = "metric_switcher") {
                    MetricSwitcherRow(
                        selectedMetricMode = uiState.selectedMetricMode,
                        onMetricModeChanged = { viewModel.onMetricModeChanged(it) }
                    )
                }

                // ── آخر العمليات والإنفاق — دائم أسفل التقويم مباشرة ─────────────
                item(key = "recent_ops_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = if (uiState.selectedCalendarDate != null) "عمليات اليوم المحدد" else "آخر العمليات والإنفاق",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        if (uiState.selectedCalendarDate != null) {
                            Surface(
                                onClick = { viewModel.onCalendarDateSelected(null) },
                                shape = RoundedCornerShape(10.dp),
                                color = ExpenseRed.copy(alpha = 0.10f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إلغاء فلتر اليوم",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "إلغاء الفلتر",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }
                }

                // ── Recent transactions list section ─────────────────────────
                recentTransactionsSection(
                    isLoading = uiState.isLoading,
                    recentTxs = recentTxs,
                    categories = uiState.categories,
                    accounts = uiState.accounts,
                    selectedTransactionIds = uiState.selectedTransactionIds,
                    selectedCalendarDate = uiState.selectedCalendarDate,
                    visibleRecentCount = visibleRecentCount,
                    filteredTransactionsCount = uiState.filteredTransactions.size,
                    primaryColor = Primary,
                    onEdit = { tx ->
                        val route = Screen.AddTransaction.createRoute(tx.type.name, tx.id)
                        navController?.navigate(route)
                    },
                    onDelete = { tx ->
                        showDeleteDialog = tx
                    },
                    onRowClick = { tx ->
                        if (uiState.selectedTransactionIds.isNotEmpty()) {
                            viewModel.toggleTransactionSelection(tx.id)
                        } else {
                            showActionMenuForTransaction = tx
                        }
                    },
                    onRowLongClick = { tx ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleTransactionSelection(tx.id)
                    },
                    onLoadMore = { visibleRecentCount += 5 },
                    selectedAccountId = uiState.selectedAccountId
                )
            }

            SelectionFloatingBar(
                selectedTransactions = selectedTransactions,
                categories = uiState.categories,
                accounts = uiState.accounts,
                selectedTotal = selectedTotal,
                onEditClick = { showBulkEditSheet = true },
                onCloseClick = { viewModel.clearTransactionSelection() },
                onRemoveTransaction = { tx ->
                    viewModel.toggleTransactionSelection(tx.id)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp)
                    .zIndex(10f)
            )
        }
    }

    // ── Dialogs and Bottom Sheets Wrapper ────────────────────────────────────
    TransactionsScreenDialogs(
        uiState = uiState,
        showActionMenuForTransaction = showActionMenuForTransaction,
        onDismissActionMenu = { showActionMenuForTransaction = null },
        onEditTransaction = { tx ->
            val route = Screen.AddTransaction.createRoute(tx.type.name, tx.id)
            navController?.navigate(route)
            showActionMenuForTransaction = null
        },
        onDeleteTransactionRequested = { tx ->
            showDeleteDialog = tx
            showActionMenuForTransaction = null
        },
        showDeleteDialog = showDeleteDialog,
        onDismissDeleteDialog = { showDeleteDialog = null },
        onConfirmDeleteTransaction = { txToDelete ->
            viewModel.deleteTransaction(txToDelete)
            showDeleteDialog = null
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "تم حذف العملية بنجاح",
                    actionLabel = "تراجع",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreLastDeletedTransaction()
                }
            }
        },
        showBulkDeleteDialog = showBulkDeleteDialog,
        onDismissBulkDelete = { showBulkDeleteDialog = false },
        onConfirmBulkDelete = {
            viewModel.deleteSelectedTransactions()
            showBulkDeleteDialog = false
        },
        showBulkCategoryDialog = showBulkCategoryDialog,
        onDismissBulkCategory = { showBulkCategoryDialog = false },
        onConfirmBulkCategory = { newCatId ->
            viewModel.changeCategoryForSelectedTransactions(newCatId)
            showBulkCategoryDialog = false
        },
        showFilterSheet = showFilterSheet,
        onDismissFilterSheet = { showFilterSheet = false },
        onApplyFilters = { minAmount, startDate, endDate ->
            viewModel.setAdvancedFilters(minAmount, startDate, endDate)
            showFilterSheet = false
        },
        onClearFilters = {
            viewModel.clearAdvancedFilters()
            showFilterSheet = false
        },
        activeSummarySheetDay = activeSummarySheetDay,
        onDismissSummarySheet = { activeSummarySheetDay = null },
        onViewTransactionsForDay = { dayTs ->
            viewModel.onCalendarDateSelected(dayTs)
            activeSummarySheetDay = null
        },
        showBulkEditSheet = showBulkEditSheet,
        onDismissBulkEdit = { showBulkEditSheet = false },
        onConfirmBulkEdit = { newCatId, newAccId ->
            viewModel.bulkEdit(newCatId, newAccId)
        }
    )
}
