package com.qdash.presentation.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.asStable
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.presentation.navigation.LocalBottomBarPadding
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TextGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onNavigateToAddAccount: (Long?) -> Unit = {},
    onNavigateToAccountDetails: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
    val PrimaryDark = if (isDark) MaterialTheme.colorScheme.primaryContainer else com.qdash.ui.theme.PrimaryDark

    val accountColorPalette = remember(Primary) {
        listOf(
            "#6C63FF" to Primary,
            "#22C55E" to IncomeGreen,
            "#EF4444" to ExpenseRed,
            "#3B82F6" to com.qdash.ui.theme.TransferBlue,
            "#F59E0B" to com.qdash.ui.theme.SavingsAmber,
            "#06B6D4" to Color(0xFF06B6D4),
            "#8B5CF6" to Color(0xFF8B5CF6),
            "#EC4899" to Color(0xFFEC4899)
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomBarPadding = LocalBottomBarPadding.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()

    val showTotalBalance = uiState.showBalances
    var showArchivedSection by rememberSaveable { mutableStateOf(false) }
    var activeAccounts by remember(uiState.accounts) {
        mutableStateOf(uiState.accounts.filter { !it.isArchived }.sortedBy { it.sortOrder })
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val lazyListState = rememberLazyListState()

    var showAddAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showTransferDialog by rememberSaveable { mutableStateOf(false) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }

    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var accountToEmpty by remember { mutableStateOf<Account?>(null) }
    var countdownSeconds by remember { mutableStateOf(5) }

    LaunchedEffect(accountToEmpty) {
        if (accountToEmpty != null) {
            countdownSeconds = 5
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds--
            }
        }
    }

    var fromAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var transferAmount by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.accounts) {
        if (uiState.accounts.isNotEmpty()) {
            if (fromAccountId == null) {
                fromAccountId = uiState.accounts.first().id
            }
            if (toAccountId == null) {
                toAccountId = uiState.accounts.lastOrNull { it.id != fromAccountId }?.id ?: uiState.accounts.first().id
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "حسناً",
                    duration = SnackbarDuration.Short
                )
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("accounts_screen"),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                UnifiedScreenHeader(
                    title = "الحسابات المالية",
                    subtitle = "تتبع أرصدتك وقم بإجراء تحويلات بين حساباتك",
                    showBackButton = false,
                    onBackClick = onBack
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = bottomBarPadding + 16.dp
                    )
                ) {

                item {
                    if (uiState.isLoading) {
                        NetWealthCardSkeleton()
                    } else {
                        val netWorthAccounts = remember(uiState.accounts) {
                            uiState.accounts.filter { !it.isArchived && it.isActive }
                        }
                        val total = netWorthAccounts.sumOf { it.balance }
                        NetWealthCard(
                            total = total,
                            showTotalBalance = showTotalBalance,
                            onToggleTotalBalance = { viewModel.toggleTotalBalanceVisibility() },
                            activeAccountsCount = netWorthAccounts.size,
                            isDark = isDark,
                            Primary = Primary,
                            PrimaryDark = PrimaryDark
                        )
                    }
                }

                item {
                    AccountActionsRow(
                        onAddAccountClick = { onNavigateToAddAccount(null) },
                        onTransferClick = { showTransferDialog = true },
                        isLoading = uiState.isLoading,
                        accountsCount = uiState.accounts.size
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قائمة حساباتك الجارية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (uiState.isLoading) {
                    items(3) {
                        AccountItemSkeleton()
                    }
                } else {
                    itemsIndexed(activeAccounts, key = { _, account -> account.id }) { index, account ->
                        val accountTxs = remember(uiState.transactions, account.id) {
                            uiState.transactions.filter { it.accountId == account.id || it.toAccountId == account.id }
                        }

                        val isDragging = index == draggedIndex
                        val offsetY = if (isDragging) dragOffsetY else 0f

                        val haptic = LocalHapticFeedback.current
                        val currentIndex by rememberUpdatedState(index)

                        val dragHandleModifier = Modifier.pointerInput(account.id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedIndex = currentIndex
                                    dragOffsetY = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y

                                    val currentDragged = draggedIndex
                                    if (currentDragged != null) {
                                        val draggedItemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == account.id }
                                        if (draggedItemInfo != null) {
                                            val draggedCenter = draggedItemInfo.offset + dragOffsetY + draggedItemInfo.size / 2f
                                            val targetItem = lazyListState.layoutInfo.visibleItemsInfo
                                                .filter { it.key != account.id }
                                                .firstOrNull { item ->
                                                    val itemIndex = activeAccounts.indexOfFirst { it.id == item.key }
                                                    if (itemIndex != -1) {
                                                        draggedCenter > item.offset && draggedCenter < item.offset + item.size
                                                    } else false
                                                }

                                            if (targetItem != null) {
                                                val targetIndex = activeAccounts.indexOfFirst { it.id == targetItem.key }
                                                if (targetIndex != -1 && targetIndex != currentDragged) {
                                                    val newList = activeAccounts.toMutableList()
                                                    val temp = newList[currentDragged]
                                                    newList[currentDragged] = newList[targetIndex]
                                                    newList[targetIndex] = temp
                                                    activeAccounts = newList
                                                    draggedIndex = targetIndex
                                                    dragOffsetY -= (targetItem.offset - draggedItemInfo.offset)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                    viewModel.updateAccountsOrder(activeAccounts)
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                }
                            )
                        }

                        DraggableAccountItem(
                            account = account,
                            transactions = accountTxs.asStable(),
                            categories = uiState.categories.asStable(),
                            showBalance = uiState.accountBalancesVisibility[account.id] ?: true,
                            onToggleBalance = { viewModel.toggleAccountBalanceVisibility(account.id) },
                            onEdit = { onNavigateToAddAccount(account.id) },
                            onDelete = { accountToDelete = account },
                            onCardClick = { onNavigateToAccountDetails(account.id) },
                            isDragging = isDragging,
                            isSomethingDragging = draggedIndex != null,
                            dragOffsetY = offsetY,
                            dragHandleModifier = dragHandleModifier,
                            modifier = Modifier.animateItem()
                        )
                    }


                    if (activeAccounts.isEmpty() && uiState.archivedAccounts.isEmpty()) {
                        item {
                            EmptyAccountsState(
                                onAddAccountClick = { onNavigateToAddAccount(null) }
                            )
                        }
                    }

                    if (uiState.archivedAccounts.isNotEmpty()) {
                        item(key = "archived_accounts_section_header") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showArchivedSection = !showArchivedSection }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = null,
                                            tint = TextGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "الحسابات المؤرشفة (${uiState.archivedAccounts.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showArchivedSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = TextGray
                                    )
                                }
                            }
                        }

                        if (showArchivedSection) {
                            items(uiState.archivedAccounts, key = { "archived_${it.id}" }) { archivedAcc ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToAccountDetails(archivedAcc.id) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = archivedAcc.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = FormatterUtils.formatCurrency(archivedAcc.balance, currencyCode = archivedAcc.currency),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextGray
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.unarchiveAccount(archivedAcc.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Primary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Unarchive,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "إلغاء الأرشفة",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } // end PullToRefreshBox
    }

    uiState.deleteError?.let { deleteErr ->
        AppDialog(
            onDismissRequest = { viewModel.clearDeleteError() },
            title = "تعذر حذف الحساب",
            text = deleteErr,
            confirmButtonText = "حسناً، فهمت",
            onConfirm = { viewModel.clearDeleteError() },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
        )
    }

    AccountsDialogs(
        uiState = uiState,
        viewModel = viewModel,
        showEditSheet = showEditSheet,
        onEditSheetDismiss = {
            showEditSheet = false
            viewModel.setEditingAccount(null)
        },
        showAddAccountDialog = showAddAccountDialog,
        onAddAccountDialogDismiss = { showAddAccountDialog = false },
        showTransferDialog = showTransferDialog,
        onTransferDialogDismiss = { showTransferDialog = false },
        accountToDelete = accountToDelete,
        onDeleteDialogDismiss = { accountToDelete = null },
        onDeleteConfirm = { acc ->
            viewModel.deleteAccount(acc)
            accountToDelete = null
        },
        accountToEmpty = accountToEmpty,
        onEmptyDialogDismiss = { accountToEmpty = null },
        onEmptyConfirm = { acc ->
            viewModel.editAccount(acc.copy(balance = 0.0))
            accountToEmpty = null
        },
        countdownSeconds = countdownSeconds,
        fromAccountId = fromAccountId,
        onFromAccountChange = { fromAccountId = it },
        toAccountId = toAccountId,
        onToAccountChange = { toAccountId = it },
        transferAmount = transferAmount,
        onTransferAmountChange = { transferAmount = it },
        accountColorPalette = accountColorPalette,
        Primary = Primary
    )
}
}
