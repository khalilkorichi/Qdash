package com.qdash.presentation.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.qdash.domain.model.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TransferBlue
import com.qdash.ui.theme.TextGray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
private fun formatAmount(amount: Double): String {
    return com.qdash.core.utils.FormatterUtils.formatCurrency(amount)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return com.qdash.core.utils.FormatterUtils.convertNumerals(sdf.format(Date(timestamp)))
}

private fun categoryTypeLabel(type: CategoryType) = when (type) {
    CategoryType.EXPENSE -> "مصاريف"
    CategoryType.INCOME  -> "دخل"
}

private fun transactionTypeColor(type: TransactionType) = when (type) {
    TransactionType.INCOME   -> IncomeGreen
    TransactionType.EXPENSE  -> ExpenseRed
    TransactionType.TRANSFER -> TransferBlue
}

private data class FilterChipDef(val label: String)

private val ADVANCED_FILTERS = listOf(
    FilterChipDef("التاريخ"),
    FilterChipDef("المبلغ"),
    FilterChipDef("النوع"),
    FilterChipDef("الفئة"),
    FilterChipDef("الحساب")
)

private val COMMON_SEARCH_SUGGESTIONS = listOf(
    Triple("💰", "راتب شهري", "راتب"),
    Triple("🛒", "مشتريات البقالة", "بقالة"),
    Triple("⚡", "فاتورة الكهرباء", "كهرباء"),
    Triple("🍔", "مطاعم ومقاهي", "مطعم"),
    Triple("🚗", "بنزين وسيارة", "بنزين"),
    Triple("🏠", "إيجار وسكن", "إيجار")
)

// ===========================================================================
// Main composable
// ===========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onTransactionClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SearchTopBar(
                query = uiState.query,
                onQueryChange = { viewModel.onQueryChange(it) },
                onClear = { viewModel.clearQuery() },
                onBack = onBack,
                focusRequester = focusRequester,
                onSearch = {
                    if (uiState.query.isNotBlank()) {
                        viewModel.saveRecentSearch(uiState.query)
                        keyboardController?.hide()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Advanced filters row (always visible)
            AdvancedFiltersRow(
                selectedFilters = selectedFilters,
                onToggle = { filter ->
                    selectedFilters = if (selectedFilters.contains(filter)) {
                        selectedFilters - filter
                    } else {
                        selectedFilters + filter
                    }
                }
            )

            when {
                // Loading
                uiState.isSearching -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        userScrollEnabled = false
                    ) {
                        item {
                            SearchSectionHeader(title = "البحث عن نتائج...", count = 0)
                        }
                        items(5) {
                            SearchResultItemSkeleton()
                        }
                    }
                }

                // Empty query → show suggestions and recents
                uiState.query.isBlank() -> {
                    EmptyQueryState(
                        recentSearches = uiState.recentSearches,
                        suggestedCategories = uiState.suggestedCategories,
                        onSearchClick = { viewModel.onQueryChange(it) },
                        onRemove = { viewModel.removeRecentSearch(it) },
                        onClearAll = { viewModel.clearRecentSearches() }
                    )
                }

                // No results
                uiState.transactions.isEmpty() &&
                        uiState.categories.isEmpty() &&
                        uiState.accounts.isEmpty() -> {
                    SearchEmptyState(query = uiState.query)
                }

                // Results
                else -> {
                    SearchResultsList(
                        uiState = uiState,
                        onTransactionClick = { t ->
                            viewModel.saveRecentSearch(uiState.query)
                            onTransactionClick(t.id)
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Search TopBar
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
    onSearch: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("search_bar_input"),
                placeholder = {
                    Text(
                        "ابحث عن معاملات، حسابات، فئات...",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح",
                                tint = TextGray
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Advanced filter chips row
// ---------------------------------------------------------------------------
@Composable
private fun AdvancedFiltersRow(
    selectedFilters: Set<String>,
    onToggle: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("search_advanced_filters"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ADVANCED_FILTERS) { chip ->
            val selected = selectedFilters.contains(chip.label)
            FilterChip(
                selected = selected,
                onClick = { onToggle(chip.label) },
                label = {
                    Text(
                        chip.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Default state (empty query) with suggestions and recents
// ---------------------------------------------------------------------------
@Composable
private fun EmptyQueryState(
    recentSearches: List<String>,
    suggestedCategories: List<Category>,
    onSearchClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Recent Searches (if any)
        if (recentSearches.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "عمليات البحث الأخيرة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onClearAll, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("مسح الكل", color = TextGray, fontSize = 12.sp)
                    }
                }
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentSearches) { recent ->
                        InputChip(
                            selected = false,
                            onClick = { onSearchClick(recent) },
                            label = { Text(recent, fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemove(recent) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "إزالة",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = TextGray
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = InputChipDefaults.inputChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // 2. Quick Search Suggestions
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = "اقتراحات سريعة للبحث",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    COMMON_SEARCH_SUGGESTIONS.take(3).forEach { (emoji, label, query) ->
                        SuggestionCard(emoji = emoji, label = label, onClick = { onSearchClick(query) })
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    COMMON_SEARCH_SUGGESTIONS.drop(3).forEach { (emoji, label, query) ->
                        SuggestionCard(emoji = emoji, label = label, onClick = { onSearchClick(query) })
                    }
                }
            }
        }

        // 3. Dynamic Category Suggestions
        if (suggestedCategories.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "البحث حسب الفئة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestedCategories) { category ->
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(category.color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Card(
                            onClick = { onSearchClick(category.name) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = catColor.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, catColor.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(catColor.copy(alpha = 0.15f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = category.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = catColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Suggestion Card Component
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionCard(
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 16.sp)
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Search results list
// ---------------------------------------------------------------------------
@Composable
private fun SearchResultsList(
    uiState: SearchUiState,
    onTransactionClick: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_results_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ——— Transactions section ———
        if (uiState.transactions.isNotEmpty()) {
            item {
                SearchSectionHeader(
                    title = "المعاملات",
                    count = uiState.transactions.size
                )
            }
            itemsIndexed(
                items = uiState.transactions.take(5),
                key = { _, t -> "txn_${t.id}" }
            ) { index, transaction ->
                AnimatedResultItem(index = index) {
                    TransactionResultItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
            if (uiState.transactions.size > 5) {
                item {
                    ShowMoreRow(count = uiState.transactions.size - 5, label = "معاملة أخرى")
                }
            }
        }

        // ——— Accounts section ———
        if (uiState.accounts.isNotEmpty()) {
            item {
                SearchSectionHeader(
                    title = "الحسابات",
                    count = uiState.accounts.size
                )
            }
            itemsIndexed(
                items = uiState.accounts,
                key = { _, a -> "acc_${a.id}" }
            ) { index, account ->
                AnimatedResultItem(index = index) {
                    AccountResultItem(account = account)
                }
            }
        }

        // ——— Categories section ———
        if (uiState.categories.isNotEmpty()) {
            item {
                SearchSectionHeader(
                    title = "الفئات",
                    count = uiState.categories.size
                )
            }
            itemsIndexed(
                items = uiState.categories,
                key = { _, c -> "cat_${c.id}" }
            ) { index, category ->
                AnimatedResultItem(index = index) {
                    CategoryResultItem(category = category)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------
@Composable
private fun SearchSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "$count نتيجة",
            fontSize = 11.sp,
            color = TextGray
        )
    }
}

// ---------------------------------------------------------------------------
// Animated result item wrapper
// ---------------------------------------------------------------------------
@Composable
private fun AnimatedResultItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = (index * 35).coerceAtMost(250)
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = (index * 35).coerceAtMost(250)
            )
        )
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Transaction result item
// ---------------------------------------------------------------------------
@Composable
private fun TransactionResultItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val amountColor = transactionTypeColor(transaction.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Type indicator circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(amountColor.copy(alpha = 0.12f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (transaction.type) {
                    TransactionType.INCOME   -> Icons.Default.TrendingUp
                    TransactionType.EXPENSE  -> Icons.Default.TrendingDown
                    TransactionType.TRANSFER -> Icons.Default.SyncAlt
                },
                contentDescription = null,
                tint = amountColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = transaction.note?.ifBlank { null } ?: formatDate(transaction.date),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDate(transaction.date),
                fontSize = 11.sp,
                color = TextGray
            )
        }

        Text(
            text = formatAmount(transaction.amount),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = amountColor
        )
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(start = 74.dp))
}

// ---------------------------------------------------------------------------
// Account result item
// ---------------------------------------------------------------------------
@Composable
private fun AccountResultItem(account: Account) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(account.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(bgColor.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = bgColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = account.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = account.type.name,
                fontSize = 11.sp,
                color = TextGray
            )
        }

        Text(
            text = formatAmount(account.balance),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (account.balance >= 0) IncomeGreen else ExpenseRed
        )
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(start = 74.dp))
}

// ---------------------------------------------------------------------------
// Category result item
// ---------------------------------------------------------------------------
@Composable
private fun CategoryResultItem(category: Category) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(category.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(bgColor.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = bgColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = category.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = bgColor.copy(alpha = 0.12f),
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = categoryTypeLabel(category.type),
                    fontSize = 10.sp,
                    color = bgColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(start = 74.dp))
}

// ---------------------------------------------------------------------------
// Show more row
// ---------------------------------------------------------------------------
@Composable
private fun ShowMoreRow(count: Int, label: String) {
    TextButton(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "عرض $count $label أخرى",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Empty state (no results for query)
// ---------------------------------------------------------------------------
@Composable
private fun SearchEmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_no_results"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = TextGray,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "لا توجد نتائج",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "لم يتم العثور على نتائج لـ \"$query\"",
            fontSize = 13.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
    }
}

@Composable
private fun SearchResultItemSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon circle skeleton
        Box(
            modifier = Modifier
                .size(40.dp)
                .shimmerEffect(CircleShape)
        )
        // Title & subtitle details skeleton
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(14.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(10.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
        }
        // Trailing amount skeleton
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(16.dp)
                .shimmerEffect(RoundedCornerShape(4.dp))
        )
    }
}
