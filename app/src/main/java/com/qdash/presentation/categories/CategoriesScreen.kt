package com.qdash.presentation.categories

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*

private val iconOptions = listOf(
    "restaurant", "shopping_bag", "home", "directions_car", "bolt", "school",
    "medical_services", "sports_esports", "coffee", "shopping_cart", "local_taxi",
    "work", "savings", "star", "flag", "favorite"
)

private val colorOptions = listOf(
    "#6C63FF", "#22C55E", "#EF4444", "#3B82F6", "#F59E0B",
    "#EC4899", "#8B5CF6", "#06B6D4", "#10B981", "#F97316"
)

private fun parseHex(hex: String): Color {
    return try {
        val clean = hex.trimStart('#')
        Color(android.graphics.Color.parseColor("#$clean"))
    } catch (e: Exception) { Color(0xFF6C63FF) }
}

private fun categoryTypeLabel(type: CategoryType) = when (type) {
    CategoryType.EXPENSE -> "مصروف"
    CategoryType.INCOME -> "دخل"
}

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onBack: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
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
                                modifier = Modifier.padding(start = 32.dp, top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.subcategories.forEach { sub ->
                                    SubcategoryItem(
                                        subcategory = sub,
                                        onDelete = { viewModel.deleteCategory(sub) }
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

@Composable
private fun CategoryCard(
    category: Category,
    isExpanded: Boolean,
    subCount: Int,
    onExpandClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMergeClick: () -> Unit,
    onAddSubcategory: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val color = parseHex(category.color)

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color icon circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconVector(category.icon),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (category.isSystem) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "نظامي",
                            modifier = Modifier.size(14.dp),
                            tint = TextGray
                        )
                    }
                }
                if (subCount > 0) {
                    Text(
                        text = "$subCount فئة فرعية",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
            }

            // Expand arrow
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextGray
            )

            // More options
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("✏️ تعديل") },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("🔗 دمج مع فئة أخرى") },
                        onClick = { showMenu = false; onMergeClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("إضافة فئة فرعية") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = { showMenu = false; onAddSubcategory() }
                    )
                    if (!category.isSystem) {
                        DropdownMenuItem(
                            text = { Text("حذف الفئة", color = ExpenseRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ExpenseRed) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryItem(
    subcategory: Category,
    onDelete: () -> Unit
) {
    val color = parseHex(subcategory.color)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowForwardIos, null, tint = color, modifier = Modifier.size(14.dp))
        }

        Text(
            text = subcategory.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!subcategory.isSystem) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, tint = TextGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    defaultType: CategoryType,
    parentCategory: Category?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: CategoryType, icon: String, color: String, parentId: Long?) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(defaultType) }
    var selectedIcon by remember { mutableStateOf("star") }
    var selectedColor by remember { mutableStateOf("#6C63FF") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (parentCategory != null) "إضافة فئة فرعية" else "فئة جديدة",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (parentCategory != null) {
                    Text(
                        text = "فئة رئيسية: ${parentCategory.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextGray
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الفئة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (parentCategory == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryType.values().forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(categoryTypeLabel(type)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Icon picker
                Text("اختر أيقونة:", style = MaterialTheme.typography.labelMedium, color = TextGray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconOptions.forEach { iconName ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedIcon == iconName) Primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedIcon = iconName }
                                .then(
                                    if (selectedIcon == iconName)
                                        Modifier.background(Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconVector(iconName),
                                contentDescription = null,
                                tint = if (selectedIcon == iconName) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Color picker
                Text("اختر لوناً:", style = MaterialTheme.typography.labelMedium, color = TextGray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val color = parseHex(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (selectedColor == hex)
                                        Modifier.padding(3.dp).background(Color.White, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedType, selectedIcon, selectedColor, parentCategory?.id)
                    }
                }
            ) {
                Text("إضافة", fontWeight = FontWeight.Bold, color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun EditCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    var name by remember { mutableStateOf(category.name) }
    var selectedIcon by remember { mutableStateOf(category.icon) }
    var selectedColor by remember { mutableStateOf(category.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل الفئة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الفئة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Icon picker
                Text("اختر أيقونة:", style = MaterialTheme.typography.labelMedium, color = TextGray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconOptions.forEach { iconName ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedIcon == iconName) Primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedIcon = iconName }
                                .then(
                                    if (selectedIcon == iconName)
                                        Modifier.background(Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconVector(iconName),
                                contentDescription = null,
                                tint = if (selectedIcon == iconName) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Color picker
                Text("اختر لوناً:", style = MaterialTheme.typography.labelMedium, color = TextGray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val color = parseHex(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (selectedColor == hex)
                                        Modifier.padding(3.dp).background(Color.White, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(category.copy(name = name, icon = selectedIcon, color = selectedColor))
                    }
                }
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold, color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun MergeCategoryDialog(
    sourceCategory: Category,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (targetCategoryId: Long) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val eligibleCategories = allCategories.filter {
        it.id != sourceCategory.id && it.type == sourceCategory.type
    }
    var selectedTargetId by remember { mutableStateOf(eligibleCategories.firstOrNull()?.id) }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("دمج الفئة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "سيتم حذف فئة \"${sourceCategory.name}\" ونقل جميع المعاملات والميزانيات المرتبطة بها تلقائياً إلى الفئة المختارة أدناه.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (eligibleCategories.isEmpty()) {
                    Text(
                        text = "لا توجد فئات أخرى متوافقة للدمج معها.",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val targetCat = eligibleCategories.find { it.id == selectedTargetId }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = targetCat?.name ?: "اختر الفئة المستهدفة",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("دمج مع الفئة") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expandedDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            eligibleCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedTargetId = cat.id
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedTargetId != null,
                onClick = {
                    selectedTargetId?.let { onConfirm(it) }
                }
            ) {
                Text("تأكيد الدمج", fontWeight = FontWeight.Bold, color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun CategoryRowSkeleton(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circle outline
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shimmerEffect(CircleShape)
            )
            // Title and subcategories count outline
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
            // Arrow indicator outline
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shimmerEffect(CircleShape)
            )
        }
    }
}

private fun getIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_bag" -> Icons.Default.ShoppingBag
        "home" -> Icons.Default.Home
        "directions_car" -> Icons.Default.DirectionsCar
        "bolt" -> Icons.Default.Bolt
        "school" -> Icons.Default.School
        "medical_services" -> Icons.Default.MedicalServices
        "sports_esports" -> Icons.Default.SportsEsports
        "coffee" -> Icons.Default.Coffee
        "shopping_cart" -> Icons.Default.ShoppingCart
        "local_taxi" -> Icons.Default.LocalTaxi
        "work" -> Icons.Default.Work
        "savings" -> Icons.Default.Savings
        "star" -> Icons.Default.Star
        "flag" -> Icons.Default.Flag
        "favorite" -> Icons.Default.Favorite
        else -> Icons.Default.Category
    }
}

