package com.example.presentation.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import com.example.presentation.components.getIconByName
import com.example.domain.model.Transaction
import com.example.domain.model.Category
import com.example.domain.model.TransactionType
import com.example.core.utils.FormatterUtils
import com.example.core.ui.StableList
import com.example.core.ui.asStable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.FinTrackTopBar
import com.example.core.ui.components.UnifiedScreenHeader
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.ui.theme.*
import com.example.ui.designsystem.components.*
import com.example.ui.designsystem.tokens.*
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private fun parseColor(hex: String, primaryColor: Color): Color {
    val accountColorPalette = listOf(
        "#6C63FF" to primaryColor,
        "#22C55E" to IncomeGreen,
        "#EF4444" to ExpenseRed,
        "#3B82F6" to TransferBlue,
        "#F59E0B" to SavingsAmber,
        "#06B6D4" to Color(0xFF06B6D4),
        "#8B5CF6" to Color(0xFF8B5CF6),
        "#EC4899" to Color(0xFFEC4899)
    )
    return accountColorPalette.firstOrNull { it.first == hex }?.second
        ?: try {
            val cleaned = hex.trimStart('#')
            Color(android.graphics.Color.parseColor("#$cleaned"))
        } catch (e: Exception) {
            primaryColor
        }
}

private fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.BARIDIMOB -> "ط¨ط±ظٹط¯ظٹ ظ…ظˆط¨"
    AccountType.CCP       -> "CCP"
    AccountType.CASH      -> "ظ†ظ‚ط¯ظٹ"
    AccountType.BANK      -> "ط¨ظ†ظƒ"
    AccountType.SAVINGS   -> "طھظˆظپظٹط±"
    AccountType.WALLET    -> "ظ…ط­ظپط¸ط©"
    AccountType.OTHER     -> "ط£ط®ط±ظ‰"
}

private fun accountTypeIcon(type: AccountType): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
    AccountType.BARIDIMOB -> Icons.Default.PhoneAndroid
    AccountType.CCP       -> Icons.Default.CreditCard
    AccountType.CASH      -> Icons.Default.Payments
    AccountType.BANK      -> Icons.Default.AccountBalance
    AccountType.SAVINGS   -> Icons.Default.Savings
    AccountType.WALLET    -> Icons.Default.AccountBalanceWallet
    AccountType.OTHER     -> Icons.Default.MonetizationOn
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val PrimaryDark = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else com.example.ui.theme.PrimaryDark
    val accountColorPalette = remember(Primary) {
        listOf(
            "#6C63FF" to Primary,
            "#22C55E" to IncomeGreen,
            "#EF4444" to ExpenseRed,
            "#3B82F6" to TransferBlue,
            "#F59E0B" to SavingsAmber,
            "#06B6D4" to Color(0xFF06B6D4),
            "#8B5CF6" to Color(0xFF8B5CF6),
            "#EC4899" to Color(0xFFEC4899)
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()
    val showTotalBalance = uiState.showBalances

    // Add Account state
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Edit Account bottom sheet state
    var showEditSheet by remember { mutableStateOf(false) }

    // Delete confirmation state
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var accountToEmpty by remember { mutableStateOf<Account?>(null) }
    var countdownSeconds by remember { mutableStateOf(5) }

    LaunchedEffect(accountToEmpty) {
        if (accountToEmpty != null) {
            countdownSeconds = 5
            while (countdownSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                countdownSeconds--
            }
        }
    }

    // Transfer variables
    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var transferAmount by remember { mutableStateOf("") }

    LaunchedEffect(uiState.accounts) {
        if (uiState.accounts.isNotEmpty() && fromAccountId == null) {
            fromAccountId = uiState.accounts.first().id
            toAccountId = uiState.accounts.lastOrNull { it.id != fromAccountId }?.id
        }
    }

    // Show delete error as Snackbar
    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "ط­ط³ظ†ط§ظ‹",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearDeleteError()
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_screen"),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = ExpenseRed,
                    contentColor = Color.White,
                    actionColor = Color.White
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("accounts_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                UnifiedScreenHeader(
                    title = "ط§ظ„ط­ط³ط§ط¨ط§طھ ظˆط§ظ„ظ…ط­ظپط¸ط©",
                    subtitle = "ط£ط¯ط± ط£ط±طµط¯طھظƒ ط§ظ„ظ…طµط±ظپظٹط© ظˆط§ظ„ظ†ظ‚ط¯ظٹط© ظپظٹ ظ…ظƒط§ظ† ظˆط§ط­ط¯",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }

            // Summary Card (Glassmorphic Premium Redesign)
            item {
                if (uiState.isLoading) {
                    NetWealthCardSkeleton()
                } else {
                    val total = uiState.accounts.sumOf { it.balance }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Primary, PrimaryDark)
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                                            Text(
                                                text = "ط§ظ„ط±طµظٹط¯ ط§ظ„ط¥ط¬ظ…ط§ظ„ظٹ ط§ظ„ظ…طھظˆظپط±",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.Medium
                                            )
                                            IconButton(
                                                onClick = { viewModel.toggleTotalBalanceVisibility() },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (showTotalBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "ط¥ط®ظپط§ط،/ط¥ط¸ظ‡ط§ط±",
                                                    tint = Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (showTotalBalance) "${"%,.0f".format(total)} ط¯ط¬" else "â€¢â€¢â€¢â€¢ ط¯ط¬",
                                            style = MaterialTheme.typography.displayMedium.copy(
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${uiState.accounts.size} ط­ط³ط§ط¨ط§طھ ظ…ط§ظ„ظٹط© ظ†ط´ط·ط©",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "طھطھط¨ط¹ ظپظˆط±ظٹ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showAddAccountDialog = true },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ط­ط³ط§ط¨ ط¬ط¯ظٹط¯", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showTransferDialog = true },
                        enabled = !uiState.isLoading && uiState.accounts.size >= 2,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TransferBlue),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طھط­ظˆظٹظ„ ظ…ط§ظ„ظٹ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ط­ط³ط§ط¨ط§طھظٹ ط§ظ„ظ†ط´ط·ط©",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (uiState.isLoading) {
                items(4) {
                    AccountItemSkeleton()
                }
            } else {
                // Account cards
                items(uiState.accounts, key = { it.id }) { account ->
                    val accountTxs = remember(uiState.transactions, account.id) {
                        uiState.transactions
                            .filter { it.accountId == account.id || it.toAccountId == account.id }
                    }
                    AccountItemCard(
                        account = account,
                        transactions = accountTxs.asStable(),
                        categories = uiState.categories.asStable(),
                        showBalance = uiState.accountBalancesVisibility[account.id] ?: true,
                        onToggleBalance = { viewModel.toggleAccountBalanceVisibility(account.id) },
                        onEdit = {
                            viewModel.setEditingAccount(account)
                            showEditSheet = true
                        },
                        onArchive = { viewModel.archiveAccount(account.id) },
                        onSetDefault = { viewModel.setDefaultAccount(account.id) },
                        onDelete = { accountToDelete = account },
                        onEmpty = { accountToEmpty = account }
                    )
                }

                // Empty state
                if (uiState.accounts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "ظ„ط§ طھظˆط¬ط¯ ط­ط³ط§ط¨ط§طھ ط¨ط¹ط¯",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "ط§ط¶ط؛ط· ط¹ظ„ظ‰ \"ط­ط³ط§ط¨ ط¬ط¯ظٹط¯\" ظ„ط¥ط¶ط§ظپط© ط­ط³ط§ط¨ظƒ ط§ظ„ط£ظˆظ„",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        } // end PullToRefreshBox
    }

    // â”€â”€â”€ Edit Account Bottom Sheet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (showEditSheet && uiState.editingAccount != null) {
        val acc = uiState.editingAccount!!
        var editName by remember(acc.id) { mutableStateOf(acc.name) }
        var editBalance by remember(acc.id) { mutableStateOf(acc.balance.toInt().toString()) }
        var editColor by remember(acc.id) { mutableStateOf(acc.color) }

        ModalBottomSheet(
            onDismissRequest = {
                showEditSheet = false
                viewModel.setEditingAccount(null)
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "طھط¹ط¯ظٹظ„ ط§ظ„ط­ط³ط§ط¨",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_account_name_input"),
                    label = { Text("ط§ط³ظ… ط§ظ„ط­ط³ط§ط¨") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                OutlinedTextField(
                    value = editBalance,
                    onValueChange = { editBalance = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_account_balance_input"),
                    label = { Text("ط§ظ„ط±طµظٹط¯ (ط¯ط¬)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Color picker
                Text(
                    "ظ„ظˆظ† ط§ظ„ط­ط³ط§ط¨:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accountColorPalette.forEach { (hex, color) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (editColor == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { editColor = hex }
                                .testTag("color_circle_$hex")
                        ) {
                            if (editColor == hex) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showEditSheet = false
                            viewModel.setEditingAccount(null)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
                    ) {
                        Text("ط¥ظ„ط؛ط§ط،")
                    }
                    Button(
                        onClick = {
                            val bal = editBalance.toDoubleOrNull() ?: acc.balance
                            if (editName.isNotBlank()) {
                                viewModel.editAccount(
                                    acc.copy(
                                        name = editName,
                                        balance = bal,
                                        color = editColor
                                    )
                                )
                                showEditSheet = false
                                viewModel.setEditingAccount(null)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("ط­ظپط¸ ط§ظ„طھط¹ط¯ظٹظ„ط§طھ", color = Color.White)
                    }
                }
            }
        }
    }

    accountToDelete?.let { acc ->
        AppDialog(
            onDismissRequest = { accountToDelete = null },
            title = "ط­ط°ظپ ط§ظ„ط­ط³ط§ط¨",
            text = "ظ‡ظ„ ط£ظ†طھ ظ…طھط£ظƒط¯ ظ…ظ† ط­ط°ظپ ط§ظ„ط­ط³ط§ط¨ \"${acc.name}\"طں ط³ظٹطھظ… ط­ط°ظپ ط§ظ„ط­ط³ط§ط¨ ظ†ظ‡ط§ط¦ظٹط§ظ‹ ط¥ط°ط§ ظ„ظ… طھظƒظ† ظ‡ظ†ط§ظƒ ظ…ط¹ط§ظ…ظ„ط§طھ ظ…ط±طھط¨ط·ط© ط¨ظ‡.",
            confirmButtonText = "ظ†ط¹ظ…طŒ ط§ط­ط°ظپ",
            onConfirm = {
                viewModel.deleteAccount(acc)
                accountToDelete = null
            },
            dismissButtonText = "ط¥ظ„ط؛ط§ط،",
            isDestructive = true,
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ColorTokens.Danger, modifier = Modifier.size(20.dp))
            }
        )
    }

    accountToEmpty?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToEmpty = null },
            title = {
                Text(
                    "طھظپط±ظٹط؛ ط±طµظٹط¯ ط§ظ„ط­ط³ط§ط¨",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "ظ‡ظ„ ط£ظ†طھ ظ…طھط£ظƒط¯ ظ…ظ† طھظپط±ظٹط؛ ط±طµظٹط¯ ط§ظ„ط­ط³ط§ط¨ \"${acc.name}\" ط¨ط§ظ„ظƒط§ظ…ظ„طں ط³ظٹطھظ… طھطµظپظٹط± ط§ظ„ط±طµظٹط¯ ظˆطھط¹ظٹظٹظ†ظ‡ ط¥ظ„ظ‰ 0 ط¯ط¬.",
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (countdownSeconds > 0) {
                        Text(
                            "ظٹط±ط¬ظ‰ ط§ظ„ط§ظ†طھط¸ط§ط± ${countdownSeconds} ط«ظˆط§ظ†ظچ ظ„طھط£ظƒظٹط¯ ط§ظ„ط¹ظ…ظ„ظٹط©...",
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "ظٹظ…ظƒظ†ظƒ ط§ظ„ط¢ظ† طھط£ظƒظٹط¯ ط§ظ„ط¹ظ…ظ„ظٹط©.",
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (countdownSeconds == 0) {
                    Button(
                        onClick = {
                            viewModel.editAccount(acc.copy(balance = 0.0))
                            accountToEmpty = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Text("طھط£ظƒظٹط¯ طھظپط±ظٹط؛ ط§ظ„ط­ط³ط§ط¨", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToEmpty = null }) {
                    Text("ط¥ظ„ط؛ط§ط،", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // â”€â”€â”€ Add Account Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, type, balance, color, icon ->
                viewModel.addAccount(name, type, balance, color, icon)
                showAddAccountDialog = false
            }
        )
    }

    // â”€â”€â”€ Transfer Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (showTransferDialog && uiState.accounts.size >= 2) {
        val currentFrom = fromAccountId ?: uiState.accounts.first().id
        val currentTo = toAccountId ?: uiState.accounts.last().id

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = {
                Text(
                    "طھط­ظˆظٹظ„ ظ…ط§ظ„ظٹ ط¨ظٹظ† ط§ظ„ط­ط³ط§ط¨ط§طھ",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ط§ظ„ط­ط³ط§ط¨ ط§ظ„ظ…ط±ط³ظ„:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.accounts.forEach { acc ->
                            val isSelected = currentFrom == acc.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        fromAccountId = acc.id
                                        if (acc.id == toAccountId) {
                                            toAccountId = uiState.accounts.firstOrNull { it.id != acc.id }?.id
                                        }
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(acc.name, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Text("ط§ظ„ط­ط³ط§ط¨ ط§ظ„ظ…ط³طھظ„ظ…:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.accounts.forEach { acc ->
                            val isSelected = currentTo == acc.id
                            val disabled = acc.id == currentFrom
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> TransferBlue
                                            disabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable { if (!disabled) toAccountId = acc.id }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    acc.name,
                                    color = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            else if (isSelected) Color.White
                                            else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_amount_input"),
                        placeholder = { Text("ظ…ط¨ظ„ط؛ ط§ظ„طھط­ظˆظٹظ„ (ط¯ط¬)", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TransferBlue,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = transferAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.executeTransfer(currentFrom, currentTo, amt, "طھط­ظˆظٹظ„ ط¯ط§ط®ظ„ظٹ")
                            showTransferDialog = false
                            transferAmount = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TransferBlue)
                ) {
                    Text("طھظ†ظپظٹط° ط§ظ„طھط­ظˆظٹظ„", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("ط¥ظ„ط؛ط§ط،", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
