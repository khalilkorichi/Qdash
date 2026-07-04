package com.qdash.presentation.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.*
import com.qdash.ui.theme.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: TemplatesViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToPreFill: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()

    // Handle ViewModel navigation event channel
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TemplatesEvent.NavigateToPreFill -> onNavigateToPreFill(event.draftJson)
                TemplatesEvent.NavigateBack -> onBack()
            }
        }
    }

    var selectedSortMode by remember { mutableStateOf("usage") } // "usage", "name", "recent"
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) } // null = all

    // Process lists dynamically
    val displayedTemplates = remember(
        uiState.templates,
        uiState.searchResults,
        uiState.isSearching,
        selectedSortMode,
        selectedTypeFilter
    ) {
        val baseList = if (uiState.isSearching) uiState.searchResults else uiState.templates
        val filtered = if (selectedTypeFilter != null) {
            baseList.filter { it.transactionType == selectedTypeFilter }
        } else {
            baseList
        }
        when (selectedSortMode) {
            "name" -> filtered.sortedBy { it.name }
            "recent" -> filtered.sortedByDescending { it.updatedAt }
            else -> filtered.sortedByDescending { it.usageCount } // Default: most used
        }
    }

    Scaffold(
        topBar = {
            FinTrackTopBar(
                title = "قوالب المعاملات",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Default.Add, contentDescription = "قالب جديد", tint = Primary)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة قالب")
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
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("بحث في القوالب المتاحة…", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Filtering Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    null to "الكل",
                    TransactionType.EXPENSE to "مصاريف",
                    TransactionType.INCOME to "مداخيل",
                    TransactionType.TRANSFER to "تحويلات"
                ).forEach { (type, label) ->
                    val isSelected = selectedTypeFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = type },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Smart suggestion strip (if any frequent templates exist and not searching)
                if (uiState.frequentTemplates.isNotEmpty() && !uiState.isSearching) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "القوالب الأكثر استخداماً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.frequentTemplates) { template ->
                                    FrequentTemplateChip(
                                        template = template,
                                        onClick = { viewModel.onUseTemplate(template.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Pinned templates group (only when not searching)
                if (uiState.pinnedTemplates.isNotEmpty() && !uiState.isSearching) {
                    item {
                        Text(
                            text = "القوالب المثبتة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    items(uiState.pinnedTemplates) { template ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            TemplateItemCard(
                                template = template,
                                categories = uiState.categories,
                                accounts = uiState.accounts,
                                onClick = { viewModel.onUseTemplate(template.id) },
                                onEdit = { onNavigateToEdit(template.id) },
                                onDelete = { viewModel.showDeleteConfirmation(template) },
                                onTogglePin = { viewModel.onTogglePin(template.id, !template.isPinned) }
                            )
                        }
                    }
                }

                // All templates section
                item {
                    Text(
                        text = if (uiState.isSearching) "نتائج البحث" else "جميع القوالب المتاحة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (displayedTemplates.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد قوالب تطابق خياراتك حالياً.",
                                color = TextGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(displayedTemplates) { template ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            TemplateItemCard(
                                template = template,
                                categories = uiState.categories,
                                accounts = uiState.accounts,
                                onClick = { viewModel.onUseTemplate(template.id) },
                                onEdit = { onNavigateToEdit(template.id) },
                                onDelete = { viewModel.showDeleteConfirmation(template) },
                                onTogglePin = { viewModel.onTogglePin(template.id, !template.isPinned) }
                            )
                        }
                    }
                }
            }
        }
        } // end PullToRefreshBox
    }

    // Delete Confirmation Dialog
    val deleteConfirm = uiState.showDeleteConfirmation
    if (deleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteConfirmation(null) },
            title = { Text("حذف قالب المعاملة", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف قالب \"${deleteConfirm.name}\"؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onDeleteTemplate(deleteConfirm.id) }) {
                    Text("حذف نهائي", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteConfirmation(null) }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TemplateItemCard(
    template: TransactionTemplate,
    categories: List<Category>,
    accounts: List<Account>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val accountName = accounts.find { it.id == template.accountId }?.name ?: "حساب محذوف"
    val categoryName = categories.find { it.id == template.categoryId }?.name ?: "فئة عامة"
    
    val typeAccentColor = when (template.transactionType) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Emoji Icon bubble
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(typeAccentColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    val emoji = template.iconEmoji
                    if (emoji != null && emoji.isNotBlank()) {
                        Text(text = emoji, fontSize = 22.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = typeAccentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = template.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (template.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "مثبت",
                                tint = Primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$categoryName • $accountName",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = FormatterUtils.formatCurrency(template.amount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = typeAccentColor
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = TextGray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تعديل") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Primary) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (template.isPinned) "إلغاء التثبيت" else "تثبيت في الرئيسية") },
                            onClick = {
                                showMenu = false
                                onTogglePin()
                            },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = Primary) }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف القالب", color = ExpenseRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequentTemplateChip(
    template: TransactionTemplate,
    onClick: () -> Unit
) {
    val typeAccentColor = when (template.transactionType) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, typeAccentColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val emoji = template.iconEmoji
            if (emoji != null && emoji.isNotBlank()) {
                Text(text = emoji, fontSize = 16.sp)
            } else {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = typeAccentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(text = template.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = FormatterUtils.formatCurrency(template.amount), fontSize = 11.sp, color = typeAccentColor, fontWeight = FontWeight.ExtraBold)
        }
    }
}
