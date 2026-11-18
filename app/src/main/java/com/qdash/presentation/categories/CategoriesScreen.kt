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
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.tokens.ColorTokens
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onBack: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("مصروف", "دخل")
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogParentCategory by remember { mutableStateOf<Category?>(null) }
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }

    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToMerge by remember { mutableStateOf<Category?>(null) }
    var categoryToMove by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    val currentType = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME
    val filteredRoot = remember(uiState.rootCategories, currentType) {
        uiState.rootCategories.filter { it.type == currentType }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "حسناً",
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionColor = Primary
                )
            }
        }
    ) { padding ->
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
                    IconButton(
                        onClick = {
                            addDialogParentCategory = null
                            showAddDialog = true
                        }
                    ) {
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
                        TextButton(
                            onClick = {
                                addDialogParentCategory = null
                                showAddDialog = true
                            }
                        ) {
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
                        val subList = remember(uiState.allCategories, category.id) {
                            uiState.allCategories
                                .filter { it.parentId == category.id }
                                .sortedBy { it.sortOrder }
                        }
                        val subCount = uiState.subcategoryCounts[category.id] ?: subList.size

                        CategoryCard(
                            category = category,
                            isExpanded = isExpanded,
                            subCount = subCount,
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
                            onDelete = { categoryToDelete = category },
                            onMergeClick = { categoryToMerge = category },
                            onAddSubcategory = {
                                addDialogParentCategory = category
                                showAddDialog = true
                            },
                            onMoveClick = { categoryToMove = category }
                        )

                        // Subcategories
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (subList.isNotEmpty()) {
                                    subList.forEach { sub ->
                                        SubcategoryItem(
                                            subcategory = sub,
                                            onEdit = { categoryToEdit = sub },
                                            onMove = { categoryToMove = sub },
                                            onDelete = { categoryToDelete = sub }
                                        )
                                    }
                                }

                                // Add subcategory button
                                TextButton(
                                    onClick = {
                                        addDialogParentCategory = category
                                        showAddDialog = true
                                    },
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
            defaultType = addDialogParentCategory?.type ?: currentType,
            parentCategory = addDialogParentCategory,
            onDismiss = {
                showAddDialog = false
                addDialogParentCategory = null
            },
            onConfirm = { name, type, icon, color, parentId ->
                viewModel.addCategory(name, type, icon, color, parentId)
                showAddDialog = false
                addDialogParentCategory = null
            }
        )
    }

    categoryToDelete?.let { cat ->
        AppDialog(
            onDismissRequest = { categoryToDelete = null },
            title = "حذف الفئة",
            text = if (cat.parentId == null) {
                "هل أنت متأكد من حذف الفئة \"${cat.name}\" وجميع فئاتها الفرعية؟ لن يتم الحذف إذا كانت مرتبطة بمعاملات مالية مسجلة."
            } else {
                "هل أنت متأكد من حذف الفئة الفرعية \"${cat.name}\"؟ لن يتم الحذف إذا كانت مرتبطة بمعاملات مالية مسجلة."
            },
            confirmButtonText = "نعم، حذف",
            onConfirm = {
                viewModel.deleteCategory(cat)
                categoryToDelete = null
            },
            dismissButtonText = "إلغاء",
            isDestructive = true,
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = ColorTokens.Danger)
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

    categoryToMove?.let { cat ->
        MoveCategoryDialog(
            category = cat,
            allCategories = uiState.allCategories.ifEmpty { uiState.rootCategories },
            onDismiss = { categoryToMove = null },
            onConfirm = { newParentId ->
                viewModel.moveCategory(cat.id, newParentId)
                categoryToMove = null
            }
        )
    }
}
