package com.qdash.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Amana
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionKind
import com.qdash.ui.designsystem.components.AppBottomSheet
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppEmptyState
import com.qdash.ui.designsystem.components.AppInput
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    viewModel: AccountDetailsViewModel,
    onBack: () -> Unit,
    onEditAccount: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddAmanaSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, "حسناً", duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) { // Amana tab
                ExtendedFloatingActionButton(
                    onClick = { showAddAmanaSheet = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("إضافة أمانة") }
                )
            } else if (uiState.account != null) {
                FloatingActionButton(
                    onClick = { onEditAccount(uiState.account!!.id) },
                    containerColor = Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل الحساب")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            UnifiedScreenHeader(
                title = uiState.account?.name ?: "تفاصيل الحساب",
                subtitle = "تفاصيل الرصيد والمعاملات والأمانات",
                showBackButton = true,
                onBackClick = onBack
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                return@Scaffold
            }

            val account = uiState.account ?: return@Scaffold

            // --- Header balance card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الرصيد الكلي",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextGray
                            )
                            Text(
                                text = FormatterUtils.formatCurrency(account.balance),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (!account.isActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ExpenseRed.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "معطّل",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ExpenseRed
                                )
                            }
                        }
                    }

                    if (uiState.totalAmanaForAccount > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("الأمانات", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Text(
                                    text = "- ${FormatterUtils.formatCurrency(uiState.totalAmanaForAccount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("الرصيد الفعلي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Text(
                                    text = FormatterUtils.formatCurrency(uiState.realBalance),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = IncomeGreen
                                )
                            }
                        }
                    }
                }
            }

            // --- Tabs ---
            val tabs = listOf("المعاملات", "الأمانات")
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    )
                }
            }

            // --- Tab content ---
            when (selectedTab) {
                0 -> TransactionsTab(transactions = uiState.transactions)
                1 -> AmanasTab(
                    amanas = uiState.amanas,
                    onDeleteAmana = { viewModel.deleteAmana(it) }
                )
            }
        }
    }

    // --- Add Amana Bottom Sheet ---
    if (showAddAmanaSheet) {
        AddAmanaBottomSheet(
            isSaving = uiState.isSavingAmana,
            onDismiss = { showAddAmanaSheet = false },
            onConfirm = { name, ownerName, amount, notes ->
                viewModel.addAmana(name, ownerName, amount, notes)
                showAddAmanaSheet = false
            }
        )
    }
}

@Composable
private fun TransactionsTab(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            AppEmptyState(
                title = "لا توجد معاملات",
                description = "لم يتم تسجيل أي معاملة لهذا الحساب بعد",
                icon = Icons.Default.ReceiptLong
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions, key = { it.id }) { tx ->
                TransactionListItem(transaction = tx)
            }
        }
    }
}

@Composable
private fun TransactionListItem(transaction: Transaction) {
    val isExpense = transaction.kind == TransactionKind.EXPENSE || transaction.kind == TransactionKind.SAVINGS_WITHDRAWAL
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    val amountPrefix = if (isExpense) "-" else "+"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note ?: "معاملة",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = FormatterUtils.formatShortDate(transaction.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
            Text(
                text = "$amountPrefix ${FormatterUtils.formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
        }
    }
}

@Composable
private fun AmanasTab(
    amanas: List<Amana>,
    onDeleteAmana: (Amana) -> Unit
) {
    if (amanas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            AppEmptyState(
                title = "لا توجد أمانات",
                description = "اضغط على زر + لإضافة مبلغ أمانة مؤمَّن في هذا الحساب",
                icon = Icons.Default.Lock
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp, ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(amanas, key = { it.id }) { amana ->
                AmanaListItem(amana = amana, onDelete = { onDeleteAmana(amana) })
            }
        }
    }
}

@Composable
private fun AmanaListItem(amana: Amana, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ExpenseRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = amana.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "بصاحب: ${amana.ownerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
                if (!amana.notes.isNullOrBlank()) {
                    Text(
                        text = amana.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatterUtils.formatCurrency(amana.amount),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = ExpenseRed
                )
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = TextGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف الأمانة") },
            text = { Text("هل أنت متأكد من حذف أمانة \"${amana.name}\" (${FormatterUtils.formatCurrency(amana.amount)})؟") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("حذف", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAmanaBottomSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, ownerName: String, amount: Double, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AppBottomSheet(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "إضافة أمانة",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                AppInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "اسم الأمانة",
                    placeholder = "مثال: أمانة والدي",
                    modifier = Modifier.fillMaxWidth()
                )

                AppInput(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = "اسم صاحب الأمانة",
                    placeholder = "مثال: الوالد، محمد...",
                    modifier = Modifier.fillMaxWidth()
                )

                AppInput(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "المبلغ (د.ج)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                AppInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "ملاحظات (اختياري)",
                    placeholder = "أي تفاصيل إضافية...",
                    modifier = Modifier.fillMaxWidth()
                )

                AppButton(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        onConfirm(name, ownerName, amount, notes.ifBlank { null })
                    },
                    isLoading = isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة الأمانة")
                }
            }
        }
    }
}
