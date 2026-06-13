package com.example.presentation.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.*
import com.example.core.utils.FormatterUtils
import com.example.domain.model.TransactionType
import com.example.presentation.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.designsystem.components.*
import com.example.ui.designsystem.tokens.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalDensity
import com.example.domain.model.Transaction
import com.example.domain.model.Category


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = com.example.presentation.navigation.LocalNavController.current
    var showDeleteDialog by remember { mutableStateOf<com.example.domain.model.Transaction?>(null) }
    var showActionMenuForTransaction by remember { mutableStateOf<com.example.domain.model.Transaction?>(null) }
    var activeSummarySheetDay by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Compute summary values
    val totalExpenses = remember(uiState.filteredTransactions, uiState.selectedType) {
        if (uiState.selectedType != null && uiState.selectedType != TransactionType.EXPENSE) {
            0.0
        } else {
            uiState.filteredTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
        }
    }
    val totalIncome = remember(uiState.filteredTransactions, uiState.selectedType) {
        if (uiState.selectedType != null && uiState.selectedType != TransactionType.INCOME) {
            0.0
        } else {
            uiState.filteredTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
        }
    }
    val netBalance = totalIncome - totalExpenses
    val netColor by animateColorAsState(
        targetValue = if (netBalance >= 0) IncomeGreen else ExpenseRed,
        animationSpec = tween(durationMillis = 400),
        label = "netColor"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        val groupedTransactions = remember(uiState.filteredTransactions) {
            uiState.filteredTransactions.groupBy { FormatterUtils.formatDate(it.date) }
        }

        var visibleRecentCount by remember(uiState.filteredTransactions, uiState.selectedCalendarDate) {
            mutableStateOf(5)
        }

        val recentTxs = remember(uiState.filteredTransactions, visibleRecentCount) {
            uiState.filteredTransactions.take(visibleRecentCount)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // ── Unified Screen Header ────────────────────────────────────────
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

            // ── Search box under Title ────────────────────────────────────────
            item(key = "search_box") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input")
                            .clip(RoundedCornerShape(28.dp)),
                        placeholder = {
                            Text(
                                "بحث باسم المعاملة أو غرض الشراء...",
                                color = TextGray,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
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
                            cursorColor = Primary
                        )
                    )
                }
            }

            // ── Unified Summary & Filters Card ──────────────────────────────
            item(key = "summary_and_filters") {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(60.dp)
                            .shimmerEffect(ShapeTokens.Md)
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Summary Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Section 1: Expenses
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(5.dp).background(ExpenseRed, CircleShape))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("المصاريف", fontSize = 11.sp, color = TextGray)
                                    }
                                    Text(FormatterUtils.formatCurrency(totalExpenses), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                                }
                                // Divider
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                                
                                // Section 2: Income
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(5.dp).background(IncomeGreen, CircleShape))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("المداخيل", fontSize = 11.sp, color = TextGray)
                                    }
                                    Text(FormatterUtils.formatCurrency(totalIncome), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                                }
                                // Divider
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                                
                                // Section 3: Net
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(5.dp).background(netColor, CircleShape))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("الصافي", fontSize = 11.sp, color = TextGray)
                                    }
                                    Text(FormatterUtils.formatCurrency(netBalance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = netColor)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Centered & Compact Filter Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val chipHeight = 32.dp
                                val textStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                
                                FilterChip(
                                    selected = uiState.selectedType == null,
                                    onClick = { viewModel.onTypeSelected(null) },
                                    label = { Text("الكل", style = textStyle) },
                                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(50.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Primary, selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.height(chipHeight).padding(horizontal = 2.dp)
                                )
                                
                                FilterChip(
                                    selected = uiState.selectedType == TransactionType.EXPENSE,
                                    onClick = { viewModel.onTypeSelected(TransactionType.EXPENSE) },
                                    label = { Text("مصاريف", style = textStyle) },
                                    leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(50.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ExpenseRed, selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.height(chipHeight).padding(horizontal = 2.dp)
                                )
                                
                                FilterChip(
                                    selected = uiState.selectedType == TransactionType.INCOME,
                                    onClick = { viewModel.onTypeSelected(TransactionType.INCOME) },
                                    label = { Text("مداخيل", style = textStyle) },
                                    leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(50.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IncomeGreen, selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.height(chipHeight).padding(horizontal = 2.dp)
                                )
                                
                                FilterChip(
                                    selected = uiState.selectedType == TransactionType.TRANSFER,
                                    onClick = { viewModel.onTypeSelected(TransactionType.TRANSFER) },
                                    label = { Text("تحويلات", style = textStyle) },
                                    leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(50.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TransferBlue, selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.height(chipHeight).padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Financial Activity Calendar ──────────────────────────────────
            item(key = "calendar") {
                FinancialActivityCalendar(
                    uiState = uiState,
                    viewModel = viewModel,
                    listState = listState,
                    onDayDoubleTapped = { activeSummarySheetDay = it }
                )
            }

            // ── Metric Switcher Row ──────────────────────────────────────────
            item(key = "metric_switcher") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showHelpDialog by remember { mutableStateOf(false) }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val modes = listOf(
                            "COUNT" to "العمليات",
                            "EXPENSE" to "المصاريف",
                            "INCOME" to "المداخيل",
                            "CASHFLOW" to "صافي الحركة",
                            "SCORE" to "النشاط"
                        )

                        items(modes) { (modeCode, label) ->
                            val isModeSelected = uiState.selectedMetricMode == modeCode
                            val modeColor = when (modeCode) {
                                "COUNT" -> Primary
                                "EXPENSE" -> ExpenseRed
                                "INCOME" -> IncomeGreen
                                "CASHFLOW" -> TransferBlue
                                "SCORE" -> SavingsAmber
                                else -> Primary
                            }
                            Surface(
                                onClick = { viewModel.onMetricModeChanged(modeCode) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isModeSelected) modeColor else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold, fontSize = 11.sp
                                    ),
                                    color = if (isModeSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showHelpDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "معلومات المؤشرات",
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showHelpDialog) {
                        AlertDialog(
                            onDismissRequest = { showHelpDialog = false },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            title = {
                                Text(
                                    text = "دليل المؤشرات المالية",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Right
                                )
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        "• النشاط اليومي: يوضح عدد المعاملات والعمليات المسجلة كل يوم في هذا الشهر.",
                                        "• المصاريف: حجم الإنفاقات المالية اليومية. الدوائر الحمراء الكبرى تدل على مصاريف ضخمة.",
                                        "• المداخيل: وارداتك المالية اليومية. الدوائر الخضراء الكبرى تمثل أيام استلام الرواتب والمكاسب.",
                                        "• صافي الحركة: يعرض الفارق بين الواردات والمصاريف اليومية.",
                                        "• مؤشر السرعة: خوارزمية ذكية تدمج بين تكرار معاملاتك وحجم مبالغك المتداولة."
                                    ).forEach { line ->
                                        Text(text = line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Right)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showHelpDialog = false }) {
                                    Text("حسناً", fontWeight = FontWeight.Bold, color = Primary)
                                }
                            }
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // ── آخر العمليات والإنفاق — دائم أسفل التقويم مباشرة ─────────────
            // ══════════════════════════════════════════════════════════════════
            item(key = "recent_ops_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Title and Icon (rendered on the right in RTL)
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

                    // 2. Clear filter badge (rendered on the left in RTL)
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

            // ── Recent transactions (last 5 or day-filtered) ─────────────────
            if (uiState.isLoading) {
                items(4, key = { "recent_skeleton_$it" }) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .fillMaxWidth()
                            .height(72.dp)
                            .shimmerEffect(ShapeTokens.Md)
                    )
                }
            } else {
                if (recentTxs.isEmpty()) {
                    item(key = "recent_empty") {
                        EmptyStateView(
                            title = if (uiState.selectedCalendarDate != null) "لا عمليات في هذا اليوم" else "لا توجد عمليات مسجلة!",
                            description = if (uiState.selectedCalendarDate != null) "لم يتم تسجيل أي معاملات في هذا التاريخ." else "اضغط على زر الإضافة لتسجيل أول عملية مالية.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        )
                    }
                } else {
                    items(recentTxs, key = { "recent_${it.id}" }) { tx ->
                        val cat = uiState.categories.firstOrNull { it.id == tx.categoryId }
                        val accName = uiState.accounts.firstOrNull { it.id == tx.accountId }?.name ?: "غير معروف"
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .animateItemPlacement()
                        ) {
                            SwipeableTransactionRow(
                                transaction = tx,
                                category = cat,
                                accountName = accName,
                                onEdit = {
                                    val route = Screen.AddTransaction.createRoute(tx.type.name, tx.id)
                                    navController?.navigate(route)
                                },
                                onDelete = {
                                    showDeleteDialog = tx
                                },
                                onClick = { showActionMenuForTransaction = tx }
                            )
                        }
                    }

                    if (uiState.filteredTransactions.size > visibleRecentCount) {
                        item(key = "load_more_button") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { visibleRecentCount += 5 },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Primary.copy(alpha = 0.08f),
                                        contentColor = Primary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "عرض المزيد من العمليات (${uiState.filteredTransactions.size - visibleRecentCount})",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
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


    // ── Professional Action Menu Dialog ──────────────────────────────────────
    if (showActionMenuForTransaction != null) {
        val tx = showActionMenuForTransaction!!
        val cat = uiState.categories.firstOrNull { it.id == tx.categoryId }
        val catColor = try {
            Color(android.graphics.Color.parseColor(cat?.color ?: "#6C63FF"))
        } catch (e: Exception) {
            Primary
        }
        val txAmountText = FormatterUtils.formatCurrency(tx.amount)
        
        AlertDialog(
            onDismissRequest = { showActionMenuForTransaction = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(catColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tx.type) {
                                TransactionType.EXPENSE -> Icons.Default.ArrowUpward
                                TransactionType.INCOME -> Icons.Default.ArrowDownward
                                TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                            },
                            contentDescription = null,
                            tint = when (tx.type) {
                                TransactionType.EXPENSE -> ExpenseRed
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.TRANSFER -> TransferBlue
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "التحكم بالعملية المالية",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${tx.note ?: cat?.name ?: "عملية مالية"} • $txAmountText",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = TextGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: Edit Transaction
                    Surface(
                        onClick = {
                            val route = Screen.AddTransaction.createRoute(tx.type.name, tx.id)
                            navController?.navigate(route)
                            showActionMenuForTransaction = null
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "تعديل بيانات العملية",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Option 2: Delete Transaction
                    Surface(
                        onClick = {
                            showDeleteDialog = tx
                            showActionMenuForTransaction = null
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = ExpenseRed.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "حذف العملية نهائياً",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRed,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showActionMenuForTransaction = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "إلغاء",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextGray
                    )
                }
            }
        )
    }

    // ── Delete Confirmation Dialog ───────────────────────────────────────────
    if (showDeleteDialog != null) {
        val txToDelete = showDeleteDialog!!
        AppDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = "حذف العملية المالية",
            text = "هل أنت متأكد من رغبتك في حذف هذا الإنفاق؟ سيتم موازنة الرصيد وتحديث الحساب تلقائياً.",
            confirmButtonText = "نعم، حذف",
            onConfirm = {
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
            dismissButtonText = "إلغاء",
            isDestructive = true,
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = ColorTokens.Danger
                )
            }
        )
    }

    // ── Daily Financial Summary Bottom Sheet drawer ───────────────────────────
    if (activeSummarySheetDay != null) {
        val selectedDayTs = activeSummarySheetDay!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        val dateHeaderFormatted = remember(selectedDayTs) {
            FormatterUtils.formatDate(selectedDayTs)
        }
        
        val dayAggregate = remember(uiState.dailyAggregates, selectedDayTs) {
            uiState.dailyAggregates.find { it.localDateTimestamp == selectedDayTs }
        }
        
        val dayTransactions = remember(uiState.transactions, selectedDayTs) {
            uiState.transactions.filter { tx ->
                tx.date >= selectedDayTs && tx.date < selectedDayTs + 86400000L
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = { activeSummarySheetDay = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(44.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(TextGray.copy(alpha = 0.25f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "الملخص المالي اليومي",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = dateHeaderFormatted,
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Income & Expenses Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val incomeValue = dayAggregate?.totalIncome ?: 0.0
                    val expenseValue = dayAggregate?.totalExpense ?: 0.0
                    
                    // Income Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = IncomeGreen.copy(alpha = 0.04f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "مداخيل اليوم",
                                fontSize = 11.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatterUtils.formatCurrency(incomeValue),
                                fontSize = 16.sp,
                                color = IncomeGreen,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    
                    // Expense Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, ExpenseRed.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ExpenseRed.copy(alpha = 0.04f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "مصاريف اليوم",
                                fontSize = 11.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatterUtils.formatCurrency(expenseValue),
                                fontSize = 16.sp,
                                color = ExpenseRed,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Net Cashflow Banner
                val netVal = (dayAggregate?.totalIncome ?: 0.0) - (dayAggregate?.totalExpense ?: 0.0)
                val netBgColor = if (netVal >= 0) IncomeGreen.copy(alpha = 0.06f) else ExpenseRed.copy(alpha = 0.06f)
                val netBorderColor = if (netVal >= 0) IncomeGreen.copy(alpha = 0.2f) else ExpenseRed.copy(alpha = 0.2f)
                val netTextColor = if (netVal >= 0) IncomeGreen else ExpenseRed
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(netBgColor)
                        .border(1.dp, netBorderColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الصافي المالي:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (netVal >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = netTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = FormatterUtils.formatCurrency(netVal),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = netTextColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val expenseTransactions = dayTransactions.filter { it.type == TransactionType.EXPENSE }
                if (expenseTransactions.isNotEmpty()) {
                    Text(
                        text = "توزيع مصاريف اليوم حسب الفئة",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.End)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val categoryGroups = expenseTransactions.groupBy { it.categoryId }
                    val totalExpensesSum = expenseTransactions.sumOf { it.amount }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        categoryGroups.forEach { (catId, txs) ->
                            val category = uiState.categories.find { it.id == catId }
                            val catName = category?.name ?: "أخرى"
                            val catColorHex = category?.color ?: "#6C63FF"
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(catColorHex))
                            } catch (e: Exception) {
                                Primary
                            }
                            
                            val amountSum = txs.sumOf { it.amount }
                            val percent = if (totalExpensesSum > 0) amountSum / totalExpensesSum else 0.0
                            
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = FormatterUtils.formatCurrency(amountSum),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = catName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                LinearProgressIndicator(
                                    progress = { percent.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = catColor,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                } else {
                    // Custom Gorgeous Inline Empty State (does not clip)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = IncomeGreen.copy(alpha = 0.02f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "يوم بدون مصاريف!",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "لم تقم بتسجيل أي مصاريف في هذا اليوم. استمر في التحكم بميزانيتك!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Bottom Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dismiss button
                    OutlinedButton(
                        onClick = { activeSummarySheetDay = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        border = BorderStroke(1.dp, TextGray.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "إغلاق",
                            color = TextGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    // Filter / view daily transactions button
                    Button(
                        onClick = {
                            viewModel.onCalendarDateSelected(selectedDayTs)
                            activeSummarySheetDay = null
                        },
                        modifier = Modifier
                            .weight(2.5f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "عرض عمليات اليوم",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Summary Mini Card ────────────────────────────────────────────────────────
@Composable
private fun SummaryMiniCard(
    label: String,
    amount: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Md,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextGray,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = FormatterUtils.formatCurrency(amount),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SummaryMiniCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(72.dp)
            .shimmerEffect(ShapeTokens.Md)
    )
}

@Composable
private fun TransactionItemSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shimmerEffect(ShapeTokens.Md)
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FinancialActivityCalendar(
    uiState: TransactionsUiState,
    viewModel: TransactionsViewModel,
    listState: LazyListState,
    onDayDoubleTapped: (Long) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val monthsArabic = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "ماي", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    
    val daysHeader = listOf("س", "ح", "ن", "ث", "ر", "خ", "ج")
    
    val calendar = remember(uiState.visibleYear, uiState.visibleMonth) {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, uiState.visibleYear)
            set(java.util.Calendar.MONTH, uiState.visibleMonth)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    val firstDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    
    val startOffset = when (firstDayOfWeek) {
        java.util.Calendar.SATURDAY -> 0
        java.util.Calendar.SUNDAY -> 1
        java.util.Calendar.MONDAY -> 2
        java.util.Calendar.TUESDAY -> 3
        java.util.Calendar.WEDNESDAY -> 4
        java.util.Calendar.THURSDAY -> 5
        java.util.Calendar.FRIDAY -> 6
        else -> 0
    }
    
    val totalCells = startOffset + daysInMonth
    val rowsCount = (totalCells + 6) / 7
    
    val activeColor = when (uiState.selectedMetricMode) {
        "COUNT" -> Primary
        "EXPENSE" -> ExpenseRed
        "INCOME" -> IncomeGreen
        "SCORE" -> SavingsAmber
        "CASHFLOW" -> TransferBlue
        else -> Primary
    }
    
    val maxVal = remember(uiState.dailyAggregates, uiState.selectedMetricMode) {
        uiState.dailyAggregates.maxOfOrNull { agg ->
            when (uiState.selectedMetricMode) {
                "COUNT" -> agg.transactionCount.toDouble()
                "EXPENSE" -> agg.totalExpense
                "INCOME" -> agg.totalIncome
                "SCORE" -> agg.activityScore
                "CASHFLOW" -> kotlin.math.abs(agg.netCashflow)
                else -> 0.0
            }
        } ?: 0.0
    }
    
    val lnMax = kotlin.math.ln(maxVal + 1.0)
    
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.OUTLINED,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newMonth = if (uiState.visibleMonth == 0) 11 else uiState.visibleMonth - 1
                        val newYear = if (uiState.visibleMonth == 0) uiState.visibleYear - 1 else uiState.visibleYear
                        viewModel.onCalendarMonthChanged(newYear, newMonth)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "الشهر السابق",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = "${monthsArabic[uiState.visibleMonth]} ${uiState.visibleYear}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(
                    onClick = {
                        val newMonth = if (uiState.visibleMonth == 11) 0 else uiState.visibleMonth + 1
                        val newYear = if (uiState.visibleMonth == 11) uiState.visibleYear + 1 else uiState.visibleYear
                        viewModel.onCalendarMonthChanged(newYear, newMonth)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "الشهر التالي",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysHeader.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = TextGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val navController = com.example.presentation.navigation.LocalNavController.current
                for (row in 0 until rowsCount) {
                    var selectedColInThisRow: Int? = null
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - startOffset + 1
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val dayTimestamp = remember(dayNumber) {
                                        java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.YEAR, uiState.visibleYear)
                                            set(java.util.Calendar.MONTH, uiState.visibleMonth)
                                            set(java.util.Calendar.DAY_OF_MONTH, dayNumber)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    }
                                    
                                    val isSelected = uiState.selectedCalendarDate == dayTimestamp
                                    if (isSelected) {
                                        selectedColInThisRow = col
                                    }
                                    val dayAggregate = uiState.dailyAggregates.find { it.localDateTimestamp == dayTimestamp }
                                    
                                    val metricValue = when (uiState.selectedMetricMode) {
                                        "COUNT" -> dayAggregate?.transactionCount?.toDouble() ?: 0.0
                                        "EXPENSE" -> dayAggregate?.totalExpense ?: 0.0
                                        "INCOME" -> dayAggregate?.totalIncome ?: 0.0
                                        "SCORE" -> dayAggregate?.activityScore ?: 0.0
                                        "CASHFLOW" -> dayAggregate?.netCashflow ?: 0.0
                                        else -> 0.0
                                    }
                                    
                                    val metricValueForScale = if (uiState.selectedMetricMode == "CASHFLOW") {
                                        kotlin.math.abs(metricValue)
                                    } else {
                                        metricValue
                                    }
                                    
                                    val cellActiveColor = if (uiState.selectedMetricMode == "CASHFLOW") {
                                        when {
                                            metricValue > 0.0 -> IncomeGreen
                                            metricValue < 0.0 -> ExpenseRed
                                            else -> TextGray
                                        }
                                    } else {
                                        activeColor
                                    }
                                    
                                    val lnVal = kotlin.math.ln(metricValueForScale + 1.0)
                                    val ratio = if (lnMax > 0.0) lnVal / lnMax else 0.0
                                    
                                    val (bubbleSize, showGlow) = when {
                                        metricValueForScale == 0.0 -> Pair(4.dp, false)
                                        ratio <= 0.25 -> Pair(12.dp, false)
                                        ratio <= 0.60 -> Pair(20.dp, false)
                                        ratio <= 0.90 -> Pair(28.dp, false)
                                        else -> Pair(36.dp, true)
                                    }
                                    
                                    val alphaVal = when {
                                        metricValueForScale == 0.0 -> 0.15f
                                        ratio <= 0.25 -> 0.35f
                                        ratio <= 0.60 -> 0.60f
                                        ratio <= 0.90 -> 0.85f
                                        else -> 1.0f
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelected) {
                                                        viewModel.onCalendarDateSelected(null)
                                                    } else {
                                                        viewModel.onCalendarDateSelected(dayTimestamp)
                                                    }
                                                },
                                                onDoubleClick = {
                                                    onDayDoubleTapped(dayTimestamp)
                                                }
                                            )
                                            .background(
                                                color = if (isSelected) cellActiveColor.copy(alpha = 0.12f) else Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Surface(
                                                modifier = Modifier.size(36.dp),
                                                shape = CircleShape,
                                                color = Color.Transparent,
                                                border = BorderStroke(1.5.dp, cellActiveColor)
                                            ) {}
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(bubbleSize)
                                                .clip(CircleShape)
                                                .background(
                                                    color = if (metricValueForScale == 0.0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else cellActiveColor.copy(alpha = alphaVal)
                                                )
                                        ) {
                                            if (showGlow && !isSelected) {
                                                Surface(
                                                    modifier = Modifier.fillMaxSize(),
                                                    shape = CircleShape,
                                                    color = Color.Transparent,
                                                    border = BorderStroke(1.dp, cellActiveColor.copy(alpha = 0.5f))
                                                ) {}
                                            }
                                        }
                                        
                                        Text(
                                            text = "$dayNumber",
                                            color = when {
                                                metricValueForScale > 0.0 -> Color.White
                                                isSelected -> MaterialTheme.colorScheme.onSurface
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (metricValueForScale > 0.0 || isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(16.dp),
                                            shape = CircleShape,
                                            color = Color.Transparent,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        ) {}
                                    }
                                }
                            }
                        }
                    }

                    // Anchored capsule under selected day circle row
                    if (selectedColInThisRow != null) {
                        val coroutineScope = rememberCoroutineScope()
                        val horizontalBias = -1f + (selectedColInThisRow / 3f)
                        
                        // Select a premium, dynamic entrance/exit transition depending on cell position
                        // Saturday is index 0 (far right), Sunday is 1 (right)
                        // Friday is index 6 (far left), Thursday is 5 (left)
                        val enterTransition = remember(selectedColInThisRow) {
                            when (selectedColInThisRow) {
                                0 -> slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                1 -> slideInHorizontally(
                                    initialOffsetX = { it / 2 },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                5 -> slideInHorizontally(
                                    initialOffsetX = { -it / 2 },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                6 -> slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                else -> slideInVertically(
                                    initialOffsetY = { -30 },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + expandVertically(
                                    expandFrom = Alignment.Top,
                                    animationSpec = tween(durationMillis = 250)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                            }
                        }

                        val exitTransition = remember(selectedColInThisRow) {
                            when (selectedColInThisRow) {
                                0 -> slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                                1 -> slideOutHorizontally(
                                    targetOffsetX = { it / 2 },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                                5 -> slideOutHorizontally(
                                    targetOffsetX = { -it / 2 },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                                6 -> slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                                else -> slideOutVertically(
                                    targetOffsetY = { -30 },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + shrinkVertically(
                                    shrinkTowards = Alignment.Top,
                                    animationSpec = tween(durationMillis = 200)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                            }
                        }

                        key(uiState.selectedCalendarDate) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = true,
                                enter = enterTransition,
                                exit = exitTransition,
                                modifier = Modifier
                                    .zIndex(10f)
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        // Report 0 height so it floats overlaying rows below it!
                                        layout(placeable.width, 0) {
                                            placeable.placeRelative(0, 0)
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = androidx.compose.ui.BiasAlignment(horizontalBias, -1f)
                                ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    // Arrow (connected directly to the capsule box)
                                    val arrowColor = Color(0xFF1E1E24)
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier.size(width = 14.dp, height = 7.dp)
                                    ) {
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(size.width / 2f, 0f)
                                            lineTo(size.width, size.height)
                                            lineTo(0f, size.height)
                                            close()
                                        }
                                        drawPath(path, color = arrowColor)
                                    }

                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    // Capsule container holding buttons side-by-side
                                    Surface(
                                        color = Color(0xFF1E1E24),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Plus Button (Button 1)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.15f))
                                                    .combinedClickable(
                                                        onClick = {
                                                            val selectedTs = uiState.selectedCalendarDate!!
                                                            val route = com.example.presentation.navigation.Screen.AddTransaction.createRoute("EXPENSE", null, selectedTs)
                                                            navController?.navigate(route)
                                                        },
                                                        onLongClick = {
                                                            android.widget.Toast.makeText(context, "أضف عملية في هذا اليوم", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Eye Button (Button 2)
                                            val currentSelectedTs = uiState.selectedCalendarDate
                                            val currentDayAggregate = uiState.dailyAggregates.find { it.localDateTimestamp == currentSelectedTs }
                                            val hasAnyActivity = currentDayAggregate != null && currentDayAggregate.transactionCount > 0

                                            if (hasAnyActivity) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.15f))
                                                        .combinedClickable(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    listState.animateScrollToItem(5)
                                                                }
                                                            },
                                                            onLongClick = {
                                                                android.widget.Toast.makeText(context, "\u0639\u0631\u0636\u0020\u0639\u0645\u0644\u064a\u0627\u062a\u0020\u0627\u0644\u064a\u0648\u0645", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Visibility,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
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
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableTransactionRow(
    transaction: Transaction,
    category: Category?,
    accountName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val minSwipeDistance = with(density) { 80.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Background Actions Layer
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit action (Right in RTL - Blue)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(TransferBlue)
                    .clickable { 
                        onEdit()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = "\u062a\u0639\u062f\u064a\u0644", tint = Color.White)
            }

            // Delete action (Left in RTL - Red)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(ExpenseRed)
                    .clickable { 
                        onDelete()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "\u062d\u0630\u0641", tint = Color.White)
            }
        }

        // Foreground Content Layer
        val animOffset by androidx.compose.animation.core.animateFloatAsState(targetValue = offsetX)
        Box(
            modifier = Modifier
                .offset(x = with(density) { animOffset.toDp() })
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = when {
                                offsetX > minSwipeDistance * 0.5f -> minSwipeDistance
                                offsetX < -minSwipeDistance * 0.5f -> -minSwipeDistance
                                else -> 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            // Subtracting dragAmount to compensate for RTL layout coordinate inversion in Compose absolute offset.
                            // When dragging to the right (dragAmount > 0), we want the row to shift to the right, which translates to a negative offset in RTL.
                            offsetX = (offsetX - dragAmount).coerceIn(-minSwipeDistance, minSwipeDistance)
                        }
                    )
                }
        ) {
            TransactionItem(
                transaction = transaction,
                category = category,
                accountName = accountName,
                onClick = {
                    if (offsetX != 0f) {
                        offsetX = 0f
                    } else {
                        onClick()
                    }
                }
            )
        }
    }
}


