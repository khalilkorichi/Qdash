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
    AccountType.BARIDIMOB -> "بريدي موب"
    AccountType.CCP       -> "CCP"
    AccountType.CASH      -> "نقدي"
    AccountType.BANK      -> "بنك"
    AccountType.SAVINGS   -> "توفير"
    AccountType.WALLET    -> "محفظة"
    AccountType.OTHER     -> "أخرى"
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("kdach_prefs", android.content.Context.MODE_PRIVATE) }
    var showTotalBalance by remember { mutableStateOf(sharedPrefs.getBoolean("show_balance_total", true)) }

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
                    actionLabel = "حسناً",
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
                    title = "الحسابات والمحفظة",
                    subtitle = "أدر أرصدتك المصرفية والنقدية في مكان واحد",
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
                                                text = "الرصيد الإجمالي المتوفر",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.Medium
                                            )
                                            IconButton(
                                                onClick = {
                                                    showTotalBalance = !showTotalBalance
                                                    sharedPrefs.edit().putBoolean("show_balance_total", showTotalBalance).apply()
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (showTotalBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "إخفاء/إظهار",
                                                    tint = Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (showTotalBalance) "${"%,.0f".format(total)} دج" else "•••• دج",
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
                                        text = "${uiState.accounts.size} حسابات مالية نشطة",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "تتبع فوري",
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
                        Text("حساب جديد", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                        Text("تحويل مالي", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "حساباتي النشطة",
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
                                "لا توجد حسابات بعد",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "اضغط على \"حساب جديد\" لإضافة حسابك الأول",
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

    // ─── Edit Account Bottom Sheet ──────────────────────────────────────────────
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
                    "تعديل الحساب",
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
                    label = { Text("اسم الحساب") },
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
                    label = { Text("الرصيد (دج)") },
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
                    "لون الحساب:",
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
                        Text("إلغاء")
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
                        Text("حفظ التعديلات", color = Color.White)
                    }
                }
            }
        }
    }

    accountToDelete?.let { acc ->
        AppDialog(
            onDismissRequest = { accountToDelete = null },
            title = "حذف الحساب",
            text = "هل أنت متأكد من حذف الحساب \"${acc.name}\"؟ سيتم حذف الحساب نهائياً إذا لم تكن هناك معاملات مرتبطة به.",
            confirmButtonText = "نعم، احذف",
            onConfirm = {
                viewModel.deleteAccount(acc)
                accountToDelete = null
            },
            dismissButtonText = "إلغاء",
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
                    "تفريغ رصيد الحساب",
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
                        "هل أنت متأكد من تفريغ رصيد الحساب \"${acc.name}\" بالكامل؟ سيتم تصفير الرصيد وتعيينه إلى 0 دج.",
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (countdownSeconds > 0) {
                        Text(
                            "يرجى الانتظار ${countdownSeconds} ثوانٍ لتأكيد العملية...",
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "يمكنك الآن تأكيد العملية.",
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
                        Text("تأكيد تفريغ الحساب", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToEmpty = null }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // ─── Add Account Dialog ─────────────────────────────────────────────────────
    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, type, balance, color, icon ->
                viewModel.addAccount(name, type, balance, color, icon)
                showAddAccountDialog = false
            }
        )
    }

    // ─── Transfer Dialog ────────────────────────────────────────────────────────
    if (showTransferDialog && uiState.accounts.size >= 2) {
        val currentFrom = fromAccountId ?: uiState.accounts.first().id
        val currentTo = toAccountId ?: uiState.accounts.last().id

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = {
                Text(
                    "تحويل مالي بين الحسابات",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("الحساب المرسل:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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

                    Text("الحساب المستلم:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                        placeholder = { Text("مبلغ التحويل (دج)", color = TextGray) },
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
                            viewModel.executeTransfer(currentFrom, currentTo, amt, "تحويل داخلي")
                            showTransferDialog = false
                            transferAmount = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TransferBlue)
                ) {
                    Text("تنفيذ التحويل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun AccountItemCard(
    account: Account,
    transactions: StableList<Transaction>,
    categories: StableList<Category>,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
    onEmpty: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("kdach_prefs", android.content.Context.MODE_PRIVATE) }
    var showBalance by remember(account.id) {
        mutableStateOf(sharedPrefs.getBoolean("show_balance_acc_${account.id}", true))
    }

    var showMenu by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTxFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"
    val Primary = MaterialTheme.colorScheme.primary
    val accentColor = parseColor(account.color, Primary)

    val filteredTransactions = remember(transactions, selectedTxFilter, account.id) {
        transactions.items.filter { tx ->
            val isIncoming = tx.type == TransactionType.INCOME || (tx.type == TransactionType.TRANSFER && tx.toAccountId == account.id)
            when (selectedTxFilter) {
                "INCOME" -> isIncoming
                "EXPENSE" -> !isIncoming
                else -> true
            }
        }.take(5)
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .testTag("account_card_${account.id}"),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 0.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored left accent bar
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                )

                // Icon
                Box(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .size(44.dp)
                        .background(
                            if (account.type == AccountType.BARIDIMOB) Color.Transparent
                            else accentColor.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (account.type == AccountType.BARIDIMOB) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_baridimob),
                            contentDescription = "بريدي موب",
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = accountTypeIcon(account.type),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (account.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Star, contentDescription = "افتراضي", tint = SavingsAmber, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = accountTypeLabel(account.type),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (showBalance) FormatterUtils.formatCurrency(account.balance) else "•••• دج",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            IconButton(
                                onClick = {
                                    showBalance = !showBalance
                                    sharedPrefs.edit().putBoolean("show_balance_acc_${account.id}", showBalance).apply()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "إخفاء/إظهار",
                                    tint = TextGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // More menu
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Stop propagation */ }
                    )
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("account_more_${account.id}")
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "خيارات",
                            tint = TextGray
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Text("تعديل", color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = TransferBlue, modifier = Modifier.size(18.dp))
                                    Text("أرشفة", color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onArchive()
                            }
                        )
                        if (!account.isDefault) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = SavingsAmber, modifier = Modifier.size(18.dp))
                                        Text("تعيين كافتراضي", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onSetDefault()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                    Text("تفريغ الحساب", color = ExpenseRed)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEmpty()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                    Text("حذف", color = ExpenseRed)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Expandable Transactions Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp, top = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "العمليات الأخيرة لهذا الحساب",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${filteredTransactions.size} عمليات",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Elegant Filter Row (الكل | المداخيل | المصاريف)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filters = listOf(
                            Triple("ALL", "الكل", accentColor),
                            Triple("INCOME", "المداخيل", IncomeGreen),
                            Triple("EXPENSE", "المصاريف", ExpenseRed)
                        )
                        
                        filters.forEach { (filterType, label, color) ->
                            val isSelected = selectedTxFilter == filterType
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) color.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) color else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTxFilter = filterType }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(color, CircleShape)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredTransactions.isEmpty()) {
                        Text(
                            text = when (selectedTxFilter) {
                                "INCOME" -> "لا توجد مداخيل مسجلة لهذا الحساب حالياً."
                                "EXPENSE" -> "لا توجد مصاريف مسجلة لهذا الحساب حالياً."
                                else -> "لا توجد عمليات مسجلة لهذا الحساب حالياً."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredTransactions.forEach { tx ->
                                val cat = categories.items.firstOrNull { it.id == tx.categoryId }
                                val isIncoming = tx.type == TransactionType.INCOME || (tx.type == TransactionType.TRANSFER && tx.toAccountId == account.id)
                                val amountColor = if (isIncoming) IncomeGreen else ExpenseRed
                                val amountPrefix = if (isIncoming) "+" else "-"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Small category circle
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(cat?.color ?: "#6C63FF"))
                                        } catch (e: Exception) {
                                            accentColor
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(catColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getIconByName(cat?.icon ?: "receipt_long"),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = tx.note ?: cat?.name ?: "عملية مالية",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = FormatterUtils.formatDate(tx.date),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextGray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "$amountPrefix${FormatterUtils.formatCurrency(tx.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = amountColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Add Account Dialog ─────────────────────────────────────────────────────────
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, balance: Double, color: String, icon: String) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
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
    var accName by remember { mutableStateOf("") }
    var accType by remember { mutableStateOf(AccountType.BARIDIMOB) }
    var accBalance by remember { mutableStateOf("") }
    var accColor by remember { mutableStateOf("#6C63FF") }

    val accountTypes = listOf(
        AccountType.BARIDIMOB to "بريدي موب",
        AccountType.CCP       to "CCP",
        AccountType.CASH      to "نقدي",
        AccountType.BANK      to "بنك",
        AccountType.SAVINGS   to "توفير",
        AccountType.WALLET    to "محفظة"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "إضافة حساب مالي جديد",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = accName,
                    onValueChange = { accName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    placeholder = { Text("اسم الحساب (مثال: بريدي موب شخصي)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                OutlinedTextField(
                    value = accBalance,
                    onValueChange = { accBalance = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_balance_input"),
                    placeholder = { Text("الرصيد الافتتاحي (دج)", color = TextGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text("نوع الحساب:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)

                // Type selector in 2 rows of 3
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accountTypes.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (type, label) ->
                                val isSelected = accType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { accType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Text("اللون:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accountColorPalette.forEach { (hex, color) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (accColor == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { accColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (accColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = accBalance.toDoubleOrNull() ?: 0.0
                    if (accName.isNotBlank() && bal >= 0) {
                        val icon = when (accType) {
                            AccountType.BARIDIMOB -> "phonelink_ring"
                            AccountType.CCP -> "credit_card"
                            AccountType.BANK -> "account_balance"
                            AccountType.SAVINGS -> "savings"
                            AccountType.WALLET -> "account_balance_wallet"
                            else -> "payments"
                        }
                        onConfirm(accName, accType, bal, accColor, icon)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("حفظ الحساب", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun NetWealthCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(146.dp)
            .shimmerEffect(RoundedCornerShape(24.dp))
    )
}

@Composable
private fun AccountItemSkeleton(modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shimmerEffect(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Content details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(16.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(14.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

