package com.qdash.presentation.templates

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.domain.model.*
import com.qdash.presentation.navigation.LocalNavController
import com.qdash.presentation.navigation.Screen
import com.qdash.presentation.templates.components.*
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.theme.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val navController = LocalNavController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()

    // Handle ViewModel navigation event channel
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TemplatesEvent.NavigateToPreFill -> {
                    if (navController != null) {
                        navController.navigate(
                            Screen.AddTransaction.createRoute(
                                type = event.transactionType,
                                draft = event.draftJson
                            )
                        )
                    } else {
                        onNavigateToPreFill(event.draftJson)
                    }
                }
                TemplatesEvent.NavigateBack -> onBack()
            }
        }
    }

    var selectedSortMode by remember { mutableStateOf("usage") } // "usage", "recent", "name"
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) } // null = all

    val filteredPinnedTemplates = remember(uiState.pinnedTemplates, selectedTypeFilter) {
        if (selectedTypeFilter != null) {
            uiState.pinnedTemplates.filter { it.transactionType == selectedTypeFilter }
        } else {
            uiState.pinnedTemplates
        }
    }

    val showPinnedSection = filteredPinnedTemplates.isNotEmpty() && !uiState.isSearching

    // Process lists dynamically (deduplicating pinned templates when the Pinned section is shown)
    val displayedTemplates = remember(
        uiState.templates,
        uiState.searchResults,
        uiState.isSearching,
        selectedSortMode,
        selectedTypeFilter,
        showPinnedSection
    ) {
        val baseList = if (uiState.isSearching) uiState.searchResults else uiState.templates
        val filtered = baseList.filter { tpl ->
            (selectedTypeFilter == null || tpl.transactionType == selectedTypeFilter) &&
                (!showPinnedSection || !tpl.isPinned)
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
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Filtering Chips Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val typeFilters = listOf(
                        null to "الكل",
                        TransactionType.EXPENSE to "مصاريف",
                        TransactionType.INCOME to "مداخيل",
                        TransactionType.TRANSFER to "تحويلات"
                    )
                    items(typeFilters) { (type, label) ->
                        val isSelected = selectedTypeFilter == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTypeFilter = type },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Sort Chips Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(15.dp)
                            )
                            Text("ترتيب:", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.Bold)
                        }
                    }
                    val sortOptions = listOf(
                        "usage" to "الأكثر استخداماً",
                        "recent" to "الأحدث",
                        "name" to "الأبجدي"
                    )
                    items(sortOptions, key = { it.first }) { (mode, label) ->
                        val isSelected = selectedSortMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSortMode = mode },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.15f),
                                selectedLabelColor = Primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = Primary
                            )
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Smart suggestion strip (if any frequent templates exist and not searching)
                    if (uiState.frequentTemplates.isNotEmpty() && !uiState.isSearching) {
                        item(key = "frequent_strip") {
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
                                    items(uiState.frequentTemplates, key = { "freq_${it.id}" }) { template ->
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
                    if (showPinnedSection) {
                        item(key = "pinned_header") {
                            Text(
                                text = "القوالب المثبتة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(filteredPinnedTemplates, key = { "pinned_${it.id}" }) { template ->
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
                    if (displayedTemplates.isNotEmpty() || !showPinnedSection) {
                        item(key = "all_header") {
                            Text(
                                text = when {
                                    uiState.isSearching -> "نتائج البحث"
                                    showPinnedSection -> "بقية القوالب المتاحة"
                                    else -> "جميع القوالب المتاحة"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (displayedTemplates.isEmpty() && !showPinnedSection) {
                        item(key = "empty_state") {
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
                        items(displayedTemplates, key = { it.id }) { template ->
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
        AppDialog(
            onDismissRequest = { viewModel.showDeleteConfirmation(null) },
            title = "حذف قالب المعاملة",
            text = "هل أنت متأكد من رغبتك في حذف قالب \"${deleteConfirm.name}\"؟ لا يمكن التراجع عن هذا الإجراء.",
            confirmButtonText = "حذف نهائي",
            onConfirm = { viewModel.onDeleteTemplate(deleteConfirm.id) },
            dismissButtonText = "إلغاء",
            isDestructive = true,
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = ColorTokens.Danger)
            }
        )
    }
}
