package com.example.presentation.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
    val PrimaryDark = if (isDark) MaterialTheme.colorScheme.primaryContainer else com.example.ui.theme.PrimaryDark
    val gradientColors = if (isDark) listOf(Primary, PrimaryDark) else listOf(Primary, Primary)
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

    var activeAccounts by remember(uiState.accounts) { mutableStateOf(uiState.accounts) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val lazyListState = rememberLazyListState()

    // Add Account state
    var showAddAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showTransferDialog by rememberSaveable { mutableStateOf(false) }

    // Edit Account bottom sheet state
    var showEditSheet by rememberSaveable { mutableStateOf(false) }

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
    var fromAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var transferAmount by rememberSaveable { mutableStateOf("") }

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
            state = lazyListState,
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
                                        colors = gradientColors
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
                                                onClick = { viewModel.toggleTotalBalanceVisibility() },
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
                                            text = if (showTotalBalance) FormatterUtils.formatCurrency(total) else "•••• دج",
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
                                        text = FormatterUtils.convertNumerals("${uiState.accounts.size} حسابات مالية نشطة"),
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
                    AppButton(
                        onClick = { showAddAccountDialog = true },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.PRIMARY,
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    ) {
                        Text("حساب جديد", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    AppButton(
                        onClick = { showTransferDialog = true },
                        enabled = !uiState.isLoading && uiState.accounts.size >= 2,
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.INFO,
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    ) {
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
                itemsIndexed(activeAccounts, key = { _, account -> account.id }) { index, account ->
                    val accountTxs = remember(uiState.transactions, account.id) {
                        uiState.transactions
                            .filter { it.accountId == account.id || it.toAccountId == account.id }
                    }
                    val isDragging = draggedIndex == index
                    val offsetY = if (isDragging) dragOffsetY else 0f

                    val dragScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.03f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dragScale"
                    )
                    val dragElevation by animateDpAsState(
                        targetValue = if (isDragging) 8.dp else 0.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "dragElevation"
                    )
                    val dragAlpha by animateFloatAsState(
                        targetValue = if (draggedIndex != null && !isDragging) 0.65f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "dragAlpha"
                    )

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
                                                    val itemStart = item.offset
                                                    val itemEnd = item.offset + item.size
                                                     draggedCenter > itemStart && draggedCenter < itemEnd
                                                } else {
                                                    false
                                                }
                                            }

                                        if (targetItem != null) {
                                            val targetIndex = activeAccounts.indexOfFirst { it.id == targetItem.key }
                                            if (targetIndex != -1 && targetIndex != currentDragged) {
                                                val newList = activeAccounts.toMutableList()
                                                val temp = newList[currentDragged]
                                                newList[currentDragged] = newList[targetIndex]
                                                newList[targetIndex] = temp

                                                val deltaY = targetItem.offset - draggedItemInfo.offset

                                                activeAccounts = newList
                                                draggedIndex = targetIndex
                                                dragOffsetY -= deltaY
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 15f else 1f)
                    ) {
                        if (isDragging) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        color = MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            )
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
                            onEmpty = { accountToEmpty = account },
                            modifier = Modifier
                                .animateItem()
                                .scale(dragScale)
                                .graphicsLayer { alpha = dragAlpha }
                                .offset { IntOffset(0, offsetY.roundToInt()) },
                            dragHandleModifier = dragHandleModifier
                        )
                    }
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

    // â”€â”€â”€ Edit Account Bottom Sheet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (showEditSheet && uiState.editingAccount != null) {
        val acc = uiState.editingAccount!!
        var editName by remember(acc.id) { mutableStateOf(acc.name) }
        var editBalance by remember(acc.id) { mutableStateOf(acc.balance.toInt().toString()) }
        var editColor by remember(acc.id) { mutableStateOf(acc.color) }

        AppBottomSheet(
            onDismissRequest = {
                showEditSheet = false
                viewModel.setEditingAccount(null)
            }
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

                AppInput(
                    value = editName,
                    onValueChange = { editName = it },
                    modifier = Modifier.testTag("edit_account_name_input"),
                    label = "اسم الحساب"
                )

                AppInput(
                    value = editBalance,
                    onValueChange = { editBalance = it },
                    modifier = Modifier.testTag("edit_account_balance_input"),
                    label = "الرصيد (دج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation()
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
                    AppButton(
                        onClick = {
                            showEditSheet = false
                            viewModel.setEditingAccount(null)
                        },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.LIGHT,
                        intent = ButtonIntent.PRIMARY
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }
                    AppButton(
                        onClick = {
                            val bal = com.example.core.utils.FormatterUtils.normalizeAmount(editBalance).toDoubleOrNull() ?: acc.balance
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
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.PRIMARY
                    ) {
                        Text("حفظ التعديلات", color = Color.White, fontWeight = FontWeight.Bold)
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
                    AppButton(
                        onClick = {
                            viewModel.editAccount(acc.copy(balance = 0.0))
                            accountToEmpty = null
                        },
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.DANGER
                    ) {
                        Text("تأكيد تفريغ الحساب", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                AppButton(
                    onClick = { accountToEmpty = null },
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
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

                    AppInput(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        modifier = Modifier.testTag("transfer_amount_input"),
                        placeholder = "مبلغ التحويل (دج)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation()
                    )
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        val amt = com.example.core.utils.FormatterUtils.normalizeAmount(transferAmount).toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.executeTransfer(currentFrom, currentTo, amt, "تحويل داخلي")
                            showTransferDialog = false
                            transferAmount = ""
                        }
                    },
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.INFO
                ) {
                    Text("تنفيذ التحويل", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                AppButton(
                    onClick = { showTransferDialog = false },
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
