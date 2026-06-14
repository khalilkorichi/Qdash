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

    var showBulkCategoryDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

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

            // â”€â”€ Unified Screen Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item(key = "header") {
                UnifiedScreenHeader(
                    title = "ط³ط¬ظ„ ط§ظ„ظ…ط¹ط§ظ…ظ„ط§طھ",
                    subtitle = "طھطھط¨ط¹ طھظپط§طµظٹظ„ ظˆط§ط±ط¯ط§طھظƒ ظˆظ…طµط±ظˆظپط§طھظƒ ط§ظ„ظٹظˆظ…ظٹط© ط¨ط¯ظ‚ط©",
                    showBackButton = true,
                    onBackClick = onBack,
                    actions = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${uiState.filteredTransactions.size} ط¹ظ…ظ„ظٹط§طھ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }

            // â”€â”€ Search box under Title â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item(key = "search_box") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input")
                            .clip(RoundedCornerShape(28.dp)),
                        placeholder = {
                            Text(
                                "ط¨ط­ط« ط¨ط§ط³ظ… ط§ظ„ظ…ط¹ط§ظ…ظ„ط© ط£ظˆ ط؛ط±ط¶ ط§ظ„ط´ط±ط§ط،...",
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
                    
                    val hasActiveFilters = uiState.filterMinAmount != null || uiState.filterStartDate != null || uiState.filterEndDate != null
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (hasActiveFilters) Primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (hasActiveFilters) Primary else Color.Transparent,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = "طھطµظپظٹط© ظ…طھظ‚ط¯ظ…ط©",
                            tint = if (hasActiveFilters) Primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // â”€â”€ Unified Summary & Filters Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                                        Text("ط§ظ„ظ…طµط§ط±ظٹظپ", fontSize = 11.sp, color = TextGray)
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
                                        Text("ط§ظ„ظ…ط¯ط§ط®ظٹظ„", fontSize = 11.sp, color = TextGray)
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
                                        Text("ط§ظ„طµط§ظپظٹ", fontSize = 11.sp, color = TextGray)
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
                                    label = { Text("ط§ظ„ظƒظ„", style = textStyle) },
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
                                    label = { Text("ظ…طµط§ط±ظٹظپ", style = textStyle) },
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
                                    label = { Text("ظ…ط¯ط§ط®ظٹظ„", style = textStyle) },
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
                                    label = { Text("طھط­ظˆظٹظ„ط§طھ", style = textStyle) },
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

            // â”€â”€ Financial Activity Calendar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item(key = "calendar") {
                FinancialActivityCalendar(
                    uiState = uiState,
                    viewModel = viewModel,
                    listState = listState,
                    onDayDoubleTapped = { activeSummarySheetDay = it }
                )
            }

            // â”€â”€ Metric Switcher Row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                            "COUNT" to "ط§ظ„ط¹ظ…ظ„ظٹط§طھ",
                            "EXPENSE" to "ط§ظ„ظ…طµط§ط±ظٹظپ",
                            "INCOME" to "ط§ظ„ظ…ط¯ط§ط®ظٹظ„",
                            "CASHFLOW" to "طµط§ظپظٹ ط§ظ„ط­ط±ظƒط©",
                            "SCORE" to "ط§ظ„ظ†ط´ط§ط·"
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
                            contentDescription = "ظ…ط¹ظ„ظˆظ…ط§طھ ط§ظ„ظ…ط¤ط´ط±ط§طھ",
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
                                    text = "ط¯ظ„ظٹظ„ ط§ظ„ظ…ط¤ط´ط±ط§طھ ط§ظ„ظ…ط§ظ„ظٹط©",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Right
                                )
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        "â€¢ ط§ظ„ظ†ط´ط§ط· ط§ظ„ظٹظˆظ…ظٹ: ظٹظˆط¶ط­ ط¹ط¯ط¯ ط§ظ„ظ…ط¹ط§ظ…ظ„ط§طھ ظˆط§ظ„ط¹ظ…ظ„ظٹط§طھ ط§ظ„ظ…ط³ط¬ظ„ط© ظƒظ„ ظٹظˆظ… ظپظٹ ظ‡ط°ط§ ط§ظ„ط´ظ‡ط±.",
                                        "â€¢ ط§ظ„ظ…طµط§ط±ظٹظپ: ط­ط¬ظ… ط§ظ„ط¥ظ†ظپط§ظ‚ط§طھ ط§ظ„ظ…ط§ظ„ظٹط© ط§ظ„ظٹظˆظ…ظٹط©. ط§ظ„ط¯ظˆط§ط¦ط± ط§ظ„ط­ظ…ط±ط§ط، ط§ظ„ظƒط¨ط±ظ‰ طھط¯ظ„ ط¹ظ„ظ‰ ظ…طµط§ط±ظٹظپ ط¶ط®ظ…ط©.",
                                        "â€¢ ط§ظ„ظ…ط¯ط§ط®ظٹظ„: ظˆط§ط±ط¯ط§طھظƒ ط§ظ„ظ…ط§ظ„ظٹط© ط§ظ„ظٹظˆظ…ظٹط©. ط§ظ„ط¯ظˆط§ط¦ط± ط§ظ„ط®ط¶ط±ط§ط، ط§ظ„ظƒط¨ط±ظ‰ طھظ…ط«ظ„ ط£ظٹط§ظ… ط§ط³طھظ„ط§ظ… ط§ظ„ط±ظˆط§طھط¨ ظˆط§ظ„ظ…ظƒط§ط³ط¨.",
                                        "â€¢ طµط§ظپظٹ ط§ظ„ط­ط±ظƒط©: ظٹط¹ط±ط¶ ط§ظ„ظپط§ط±ظ‚ ط¨ظٹظ† ط§ظ„ظˆط§ط±ط¯ط§طھ ظˆط§ظ„ظ…طµط§ط±ظٹظپ ط§ظ„ظٹظˆظ…ظٹط©.",
                                        "â€¢ ظ…ط¤ط´ط± ط§ظ„ط³ط±ط¹ط©: ط®ظˆط§ط±ط²ظ…ظٹط© ط°ظƒظٹط© طھط¯ظ…ط¬ ط¨ظٹظ† طھظƒط±ط§ط± ظ…ط¹ط§ظ…ظ„ط§طھظƒ ظˆط­ط¬ظ… ظ…ط¨ط§ظ„ط؛ظƒ ط§ظ„ظ…طھط¯ط§ظˆظ„ط©."
                                    ).forEach { line ->
                                        Text(text = line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Right)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showHelpDialog = false }) {
                                    Text("ط­ط³ظ†ط§ظ‹", fontWeight = FontWeight.Bold, color = Primary)
                                }
                            }
                        )
                    }
                }
            }

            // â•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گ
            // â”€â”€ ط¢ط®ط± ط§ظ„ط¹ظ…ظ„ظٹط§طھ ظˆط§ظ„ط¥ظ†ظپط§ظ‚ â€” ط¯ط§ط¦ظ… ط£ط³ظپظ„ ط§ظ„طھظ‚ظˆظٹظ… ظ…ط¨ط§ط´ط±ط© â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            // â•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گâ•گ
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
                            text = if (uiState.selectedCalendarDate != null) "ط¹ظ…ظ„ظٹط§طھ ط§ظ„ظٹظˆظ… ط§ظ„ظ…ط­ط¯ط¯" else "ط¢ط®ط± ط§ظ„ط¹ظ…ظ„ظٹط§طھ ظˆط§ظ„ط¥ظ†ظپط§ظ‚",
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
                                    contentDescription = "ط¥ظ„ط؛ط§ط، ظپظ„طھط± ط§ظ„ظٹظˆظ…",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ط¥ظ„ط؛ط§ط، ط§ظ„ظپظ„طھط±",
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

            // â”€â”€ Recent transactions (last 5 or day-filtered) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                            title = if (uiState.selectedCalendarDate != null) "ظ„ط§ ط¹ظ…ظ„ظٹط§طھ ظپظٹ ظ‡ط°ط§ ط§ظ„ظٹظˆظ…" else "ظ„ط§ طھظˆط¬ط¯ ط¹ظ…ظ„ظٹط§طھ ظ…ط³ط¬ظ„ط©!",
                            description = if (uiState.selectedCalendarDate != null) "ظ„ظ… ظٹطھظ… طھط³ط¬ظٹظ„ ط£ظٹ ظ…ط¹ط§ظ…ظ„ط§طھ ظپظٹ ظ‡ط°ط§ ط§ظ„طھط§ط±ظٹط®." else "ط§ط¶ط؛ط· ط¹ظ„ظ‰ ط²ط± ط§ظ„ط¥ط¶ط§ظپط© ظ„طھط³ط¬ظٹظ„ ط£ظˆظ„ ط¹ظ…ظ„ظٹط© ظ…ط§ظ„ظٹط©.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        )
                    }
                } else {
                    items(recentTxs, key = { "recent_${it.id}" }) { tx ->
                        val cat = uiState.categories.firstOrNull { it.id == tx.categoryId }
                        val accName = uiState.accounts.firstOrNull { it.id == tx.accountId }?.name ?: "ط؛ظٹط± ظ…ط¹ط±ظˆظپ"
                        val isSelected = uiState.selectedTransactionIds.contains(tx.id)
                        val isSelectionActive = uiState.selectedTransactionIds.isNotEmpty()
                        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

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
                                onClick = {
                                    if (isSelectionActive) {
                                        viewModel.toggleTransactionSelection(tx.id)
                                    } else {
                                        showActionMenuForTransaction = tx
                                    }
                                },
                                isSelected = isSelected,
                                isSelectionActive = isSelectionActive,
                                onLongClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    viewModel.toggleTransactionSelection(tx.id)
                                }
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
                                            text = "ط¹ط±ط¶ ط§ظ„ظ…ط²ظٹط¯ ظ…ظ† ط§ظ„ط¹ظ…ظ„ظٹط§طھ (${uiState.filteredTransactions.size - visibleRecentCount})",
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


    // â”€â”€ Professional Action Menu Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                        text = "ط§ظ„طھط­ظƒظ… ط¨ط§ظ„ط¹ظ…ظ„ظٹط© ط§ظ„ظ…ط§ظ„ظٹط©",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${tx.note ?: cat?.name ?: "ط¹ظ…ظ„ظٹط© ظ…ط§ظ„ظٹط©"} â€¢ $txAmountText",
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
                                text = "طھط¹ط¯ظٹظ„ ط¨ظٹط§ظ†ط§طھ ط§ظ„ط¹ظ…ظ„ظٹط©",
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
                                text = "ط­ط°ظپ ط§ظ„ط¹ظ…ظ„ظٹط© ظ†ظ‡ط§ط¦ظٹط§ظ‹",
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
                        text = "ط¥ظ„ط؛ط§ط،",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextGray
                    )
                }
            }
        )
    }

    // â”€â”€ Delete Confirmation Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (showDeleteDialog != null) {
        val txToDelete = showDeleteDialog!!
        AppDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = "ط­ط°ظپ ط§ظ„ط¹ظ…ظ„ظٹط© ط§ظ„ظ…ط§ظ„ظٹط©",
            text = "ظ‡ظ„ ط£ظ†طھ ظ…طھط£ظƒط¯ ظ…ظ† ط±ط؛ط¨طھظƒ ظپظٹ ط­ط°ظپ ظ‡ط°ط§ ط§ظ„ط¥ظ†ظپط§ظ‚طں ط³ظٹطھظ… ظ…ظˆط§ط²ظ†ط© ط§ظ„ط±طµظٹط¯ ظˆطھط­ط¯ظٹط« ط§ظ„ط­ط³ط§ط¨ طھظ„ظ‚ط§ط¦ظٹط§ظ‹.",
            confirmButtonText = "ظ†ط¹ظ…طŒ ط­ط°ظپ",
            onConfirm = {
                viewModel.deleteTransaction(txToDelete)
                showDeleteDialog = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "طھظ… ط­ط°ظپ ط§ظ„ط¹ظ…ظ„ظٹط© ط¨ظ†ط¬ط§ط­",
                        actionLabel = "طھط±ط§ط¬ط¹",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreLastDeletedTransaction()
                    }
                }
            },
            dismissButtonText = "ط¥ظ„ط؛ط§ط،",
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

    // â”€â”€ Bulk Delete Confirmation Dialog â”€â”€
    if (showBulkDeleteDialog) {
        AppDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = "ط­ط°ظپ ط§ظ„ط¹ظ…ظ„ظٹط§طھ ط§ظ„ظ…ط­ط¯ط¯ط©",
            text = "ظ‡ظ„ ط£ظ†طھ ظ…طھط£ظƒط¯ ظ…ظ† ط±ط؛ط¨طھظƒ ظپظٹ ط­ط°ظپ ${uiState.selectedTransactionIds.size} ط¹ظ…ظ„ظٹط© ظ…ط¬ظ…ط¹ط© ظ†ظ‡ط§ط¦ظٹط§ظ‹طں ظ„ط§ ظٹظ…ظƒظ† ط§ظ„طھط±ط§ط¬ط¹ ط¹ظ† ظ‡ط°ط§ ط§ظ„ط¥ط¬ط±ط§ط، ظˆط³ظٹطھظ… طھط­ط¯ظٹط« ط§ظ„ط­ط³ط§ط¨ط§طھ طھظ„ظ‚ط§ط¦ظٹط§ظ‹.",
            confirmButtonText = "ظ†ط¹ظ…طŒ ط­ط°ظپ ط§ظ„ظƒظ„",
            onConfirm = {
                viewModel.deleteSelectedTransactions()
                showBulkDeleteDialog = false
            },
            dismissButtonText = "ط¥ظ„ط؛ط§ط،",
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

    // â”€â”€ Bulk Category Selection Dialog â”€â”€
    if (showBulkCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showBulkCategoryDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "طھط؛ظٹظٹط± ظپط¦ط© ط§ظ„ط¹ظ…ظ„ظٹط§طھ ط§ظ„ظ…ط­ط¯ط¯ط©",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ط§ط®طھط± ط§ظ„ظپط¦ط© ط§ظ„ط¬ط¯ظٹط¯ط© ظ„ظ†ظ‚ظ„ ط§ظ„ط¹ظ…ظ„ظٹط§طھ ط§ظ„ظ…ط­ط¯ط¯ط© ط¥ظ„ظٹظ‡ط§:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.categories) { category ->
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(category.color))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Surface(
                                onClick = {
                                    viewModel.changeCategoryForSelectedTransactions(category.id)
                                    showBulkCategoryDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBulkCategoryDialog = false }) {
                    Text("ط¥ظ„ط؛ط§ط،", color = TextGray)
                }
            }
        )
    }

    // â”€â”€ Advanced Filters Bottom Sheet â”€â”€
    if (showFilterSheet) {
        val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var tempMinAmount by remember { mutableStateOf(uiState.filterMinAmount?.toString() ?: "") }
        var tempStartDate by remember { mutableStateOf(uiState.filterStartDate) }
        var tempEndDate by remember { mutableStateOf(uiState.filterEndDate) }
        
        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = filterSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "طھطµظپظٹط© ظ…طھظ‚ط¯ظ…ط© ظ„ظ„ظ…ط¹ط§ظ…ظ„ط§طھ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 1. Min Amount Filter
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ظ…ط¨ط§ظ„ط؛ ط£ظƒط¨ط± ظ…ظ† ط£ظˆ طھط³ط§ظˆظٹ (ط¯ط¬):",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempMinAmount,
                        onValueChange = { tempMinAmount = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ظ…ط«ط§ظ„: 1000", textAlign = TextAlign.Right) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 2. Date Range Filter
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ط§ظ„ظ†ط·ط§ظ‚ ط§ظ„ط²ظ…ظ†ظٹ:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // End Date Button
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = tempEndDate?.let { FormatterUtils.formatDate(it) } ?: "طھط§ط±ظٹط® ط§ظ„ظ†ظ‡ط§ظٹط©",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Start Date Button
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = tempStartDate?.let { FormatterUtils.formatDate(it) } ?: "طھط§ط±ظٹط® ط§ظ„ط¨ط¯ط§ظٹط©",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reset Button
                    OutlinedButton(
                        onClick = {
                            tempMinAmount = ""
                            tempStartDate = null
                            tempEndDate = null
                            viewModel.clearAdvancedFilters()
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("ط¥ط¹ط§ط¯ط© طھط¹ظٹظٹظ†", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }

                    // Apply Button
                    Button(
                        onClick = {
                            val minVal = tempMinAmount.toDoubleOrNull()
                            viewModel.setAdvancedFilters(minVal, tempStartDate, tempEndDate)
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("طھط·ط¨ظٹظ‚ ط§ظ„ظپظ„طھط±", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // DatePickers Dialogs
        if (showStartDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = tempStartDate ?: System.currentTimeMillis())
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        tempStartDate = datePickerState.selectedDateMillis
                        showStartDatePicker = false
                    }) {
                        Text("طھط£ظƒظٹط¯", fontWeight = FontWeight.Bold, color = Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) {
                        Text("ط¥ظ„ط؛ط§ط،", color = TextGray)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showEndDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = tempEndDate ?: System.currentTimeMillis())
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        tempEndDate = datePickerState.selectedDateMillis
                        showEndDatePicker = false
                    }) {
                        Text("طھط£ظƒظٹط¯", fontWeight = FontWeight.Bold, color = Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) {
                        Text("ط¥ظ„ط؛ط§ط،", color = TextGray)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    // â”€â”€ Daily Financial Summary Bottom Sheet drawer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                    text = "ط§ظ„ظ…ظ„ط®طµ ط§ظ„ظ…ط§ظ„ظٹ ط§ظ„ظٹظˆظ…ظٹ",
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
                                text = "ظ…ط¯ط§ط®ظٹظ„ ط§ظ„ظٹظˆظ…",
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
                                text = "ظ…طµط§ط±ظٹظپ ط§ظ„ظٹظˆظ…",
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
                        text = "ط§ظ„طµط§ظپظٹ ط§ظ„ظ…ط§ظ„ظٹ:",
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
                        text = "طھظˆط²ظٹط¹ ظ…طµط§ط±ظٹظپ ط§ظ„ظٹظˆظ… ط­ط³ط¨ ط§ظ„ظپط¦ط©",
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
                            val catName = category?.name ?: "ط£ط®ط±ظ‰"
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
                                text = "ظٹظˆظ… ط¨ط¯ظˆظ† ظ…طµط§ط±ظٹظپ!",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ظ„ظ… طھظ‚ظ… ط¨طھط³ط¬ظٹظ„ ط£ظٹ ظ…طµط§ط±ظٹظپ ظپظٹ ظ‡ط°ط§ ط§ظ„ظٹظˆظ…. ط§ط³طھظ…ط± ظپظٹ ط§ظ„طھط­ظƒظ… ط¨ظ…ظٹط²ط§ظ†ظٹطھظƒ!",
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
                            text = "ط¥ط؛ظ„ط§ظ‚",
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
                                text = "ط¹ط±ط¶ ط¹ظ…ظ„ظٹط§طھ ط§ظ„ظٹظˆظ…",
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
}

