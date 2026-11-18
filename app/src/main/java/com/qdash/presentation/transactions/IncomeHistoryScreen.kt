package com.qdash.presentation.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.*
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.TransactionKind
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.navigation.Screen
import com.qdash.presentation.transactions.components.TransactionActionMenuDialog
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeHistoryScreen(
    viewModel: TransactionsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = com.qdash.presentation.navigation.LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<com.qdash.domain.model.Transaction?>(null) }
    var showActionMenuForTransaction by remember { mutableStateOf<com.qdash.domain.model.Transaction?>(null) }
    var filterThisMonthOnly by remember { mutableStateOf(false) }

    val (monthStart, monthEnd) = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        Pair(start, cal.timeInMillis)
    }

    // Filter transactions to show only genuine Income (excluding internal transfers)
    val incomeTransactions = remember(uiState.filteredTransactions, filterThisMonthOnly, monthStart, monthEnd) {
        uiState.filteredTransactions.filter { tx ->
            tx.type == TransactionType.INCOME &&
                tx.kind != TransactionKind.TRANSFER &&
                (!filterThisMonthOnly || tx.date in monthStart..monthEnd)
        }
    }

    val groupedTransactions = remember(incomeTransactions) {
        incomeTransactions.groupBy { FormatterUtils.formatDate(it.date) }
    }

    val totalIncome = remember(incomeTransactions) {
        incomeTransactions.sumOf { it.amount }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("income_history_screen"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController?.navigate(Screen.AddTransaction.createRoute("INCOME"))
                },
                containerColor = IncomeGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة دخل جديد")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {

            // ── Unified Screen Header ────────────────────────────────────────
            UnifiedScreenHeader(
                title = "سجل المداخيل",
                subtitle = "عرض وتتبع مصادر الدخل والأرباح بشكل مخصص",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = IncomeGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${incomeTransactions.size} مداخيل",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            // ── Total Income Highlight Card ──────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إجمالي الواردات والمداخيل",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${FormatterUtils.formatCurrency(totalIncome)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = IncomeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Filled Search Field ──────────────────────────────────────
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                        .clip(RoundedCornerShape(28.dp)),
                    placeholder = {
                        Text(
                            "بحث في المداخيل...",
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = TextGray
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = primaryColor
                    )
                )

                // ── Month / All-Time Filter Chips ────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !filterThisMonthOnly,
                        onClick = { filterThisMonthOnly = false },
                        label = { Text("كل الفترات", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = filterThisMonthOnly,
                        onClick = { filterThisMonthOnly = true },
                        label = { Text("هذا الشهر", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Transaction List ─────────────────────────────────────────────
            if (incomeTransactions.isEmpty()) {
                EmptyStateView(
                    title = "لا مداخيل مسجلة مطابقة للبحث!",
                    description = "جرب إضافة دخل جديد عبر علامة الزائد (+) أسفل الشاشة.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTransactions.forEach { (dateHeader, txs) ->
                        item(key = "header_$dateHeader") {
                            // Accent right-border date header (RTL Start = Right)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(IncomeGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextGray,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        items(txs, key = { it.id }) { tx ->
                            val cat = uiState.categories.firstOrNull { it.id == tx.categoryId }
                            val accName = uiState.accounts
                                .firstOrNull { it.id == tx.accountId }?.name ?: "غير معروف"
                            val toAccName = uiState.accounts.firstOrNull { it.id == tx.toAccountId }?.name
                            TransactionItem(
                                transaction = tx,
                                category = cat,
                                accountName = accName,
                                onClick = { showActionMenuForTransaction = tx },
                                toAccountName = toAccName
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Professional Action Menu Dialog ──────────────────────────────────────
    if (showActionMenuForTransaction != null) {
        val tx = showActionMenuForTransaction!!
        TransactionActionMenuDialog(
            transaction = tx,
            categories = uiState.categories,
            onDismiss = { showActionMenuForTransaction = null },
            onEdit = {
                val route = Screen.AddTransaction.createRoute(tx.type.name, tx.id)
                navController?.navigate(route)
                showActionMenuForTransaction = null
            },
            onDelete = {
                showDeleteDialog = tx
                showActionMenuForTransaction = null
            }
        )
    }

    // ── Delete Confirmation Dialog ───────────────────────────────────────────
    if (showDeleteDialog != null) {
        val txToDelete = showDeleteDialog!!
        DeleteTransactionDialog(
            transaction = txToDelete,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                viewModel.deleteTransaction(txToDelete)
                showDeleteDialog = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "تم حذف الدخل بنجاح",
                        actionLabel = "تراجع",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreLastDeletedTransaction()
                    }
                }
            }
        )
    }
}
