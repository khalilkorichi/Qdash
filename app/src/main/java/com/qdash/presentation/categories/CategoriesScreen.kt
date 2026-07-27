package com.qdash.presentation.categories

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onBack: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("مصروف", "دخل")
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }

    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToMerge by remember { mutableStateOf<Category?>(null) }

    val currentType = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME
    val filteredRoot = uiState.rootCategories.filter { it.type == currentType }

    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) { viewModel.clearError() }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            UnifiedScreenHeader(
                title = "إدارة الفئات",
                subtitle = "خصص فئات معاملاتك لتتبع إنفاقك بذكاء",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة فئة", tint = Primary)
                    }
                }
            )
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(6) {
                        CategoryRowSkeleton()
                    }
                }
            } else if (filteredRoot.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            "لا توجد فئات بعد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("+ إضافة فئة جديدة", color = Primary)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRoot, key = { it.id }) { category ->
                        val isExpanded = expandedCategoryId == category.id
                        val subCount = uiState.subcategories.count { it.parentId == category.id }

                        CategoryCard(
                            category = category,
                            isExpanded = isExpanded,
                            subCount = if (expandedCategoryId == category.id) uiState.subcategories.size else subCount,
                            onExpandClick = {
                                if (isExpanded) {
                                    expandedCategoryId = null
                                    viewModel.selectParent(null)
                                } else {
                                    expandedCategoryId = category.id
                                    viewModel.selectParent(category)
                                }
                            },
                            onEdit = { categoryToEdit = category },
                            onDelete = { viewModel.deleteCategory(category) },
                            onMergeClick = { categoryToMerge = category },
                            onAddSubcategory = { showAddDialog = true }
                        )

                        // Subcategories
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val subList = uiState.subcategories.filter { it.parentId == category.id }
                                if (subList.isNotEmpty()) {
                                    com.qdash.presentation.categories.components.SubcategoryChipGrid(
                                        subcategories = subList,
                                        selectedSubcategoryId = null,
                                        onSubcategorySelected = { sub ->
                                            // Handle subcategory click or edit
                                        }
                                    )
                                }

                                // Add subcategory button
                                TextButton(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("إضافة فئة فرعية", color = Primary, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            defaultType = currentType,
            parentCategory = if (expandedCategoryId != null) {
                uiState.rootCategories.firstOrNull { it.id == expandedCategoryId }
            } else null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, icon, color, parentId ->
                viewModel.addCategory(name, type, icon, color, parentId)
                showAddDialog = false
            }
        )
    }

    categoryToEdit?.let { cat ->
        EditCategoryDialog(
            category = cat,
            onDismiss = { categoryToEdit = null },
            onConfirm = { updated ->
                viewModel.updateCategory(updated)
                categoryToEdit = null
            }
        )
    }

    categoryToMerge?.let { cat ->
        MergeCategoryDialog(
            sourceCategory = cat,
            allCategories = uiState.rootCategories,
            onDismiss = { categoryToMerge = null },
            onConfirm = { targetId ->
                viewModel.mergeCategories(cat.id, targetId)
                categoryToMerge = null
            }
        )
    }
}
