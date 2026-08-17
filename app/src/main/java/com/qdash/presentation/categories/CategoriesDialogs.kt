package com.qdash.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
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
fun EditCategoryDialog(
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
fun MergeCategoryDialog(
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
fun MoveCategoryDialog(
    category: Category,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (newParentId: Long?) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val isCurrentlySubcategory = category.parentId != null

    // Candidates for new parent (must be a root category of the same type and not itself)
    val candidateParents = remember(allCategories, category) {
        allCategories.filter {
            it.id != category.id &&
            it.type == category.type &&
            it.parentId == null
        }
    }

    // Selected destination: null means "Make Root Category", non-null means "Make Subcategory under this ID"
    var selectedTargetParentId by remember(category) {
        mutableStateOf<Long?>(
            if (isCurrentlySubcategory) category.parentId else candidateParents.firstOrNull()?.id
        )
    }
    var makeRootSelected by remember(category) {
        mutableStateOf(false)
    }
    var expandedDropdown by remember { mutableStateOf(false) }

    val currentParent = remember(category, allCategories) {
        if (category.parentId != null) {
            allCategories.firstOrNull { it.id == category.parentId }
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("نقل وتنظيم الفئة", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Category Header Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = parseHex(category.color).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseHex(category.color)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconVector(category.icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCurrentlySubcategory) "الحالة الحالية: فئة فرعية تابعة لـ \"${currentParent?.name ?: "فئة أخرى"}\""
                                       else "الحالة الحالية: فئة رئيسية",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    }
                }

                Text(
                    text = "حدد الهيكل الجديد لهذه الفئة:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Option 1: Promote to Root Category (if it is currently a subcategory)
                if (isCurrentlySubcategory) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (makeRootSelected) Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (makeRootSelected) Primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                makeRootSelected = true
                                selectedTargetParentId = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = makeRootSelected,
                                onClick = {
                                    makeRootSelected = true
                                    selectedTargetParentId = null
                                }
                            )
                            Column {
                                Text(
                                    "جعلها كفئة رئيسية مستقلة",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "ستظهر كفئة قائمة بذاتها وتستطيع إضافة فئات فرعية تحتها",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }

                // Option 2: Move under a Parent Category
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!makeRootSelected) Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!makeRootSelected) Primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            makeRootSelected = false
                            if (selectedTargetParentId == null) {
                                selectedTargetParentId = candidateParents.firstOrNull()?.id
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = !makeRootSelected,
                                onClick = {
                                    makeRootSelected = false
                                    if (selectedTargetParentId == null) {
                                        selectedTargetParentId = candidateParents.firstOrNull()?.id
                                    }
                                }
                            )
                            Column {
                                Text(
                                    text = if (isCurrentlySubcategory) "نقل إلى فئة رئيسية أخرى" else "نقل كفئة فرعية تابعة لفئة رئيسية",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (!isCurrentlySubcategory) "ستصبح جزءاً من الفئة المختارة وتُحفظ فئاتها الفرعية معها"
                                           else "ستنتقل هذه الفئة الفرعية لتصبح تحت الفئة الرئيسية الجديدة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }

                        if (!makeRootSelected) {
                            if (candidateParents.isEmpty()) {
                                Text(
                                    text = "لا توجد فئات رئيسية أخرى متوفرة للنقل إليها.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 36.dp)
                                )
                            } else {
                                val targetParentCat = candidateParents.find { it.id == selectedTargetParentId }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 36.dp)
                                ) {
                                    OutlinedTextField(
                                        value = targetParentCat?.name ?: "اختر الفئة الرئيسية",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("الفئة الرئيسية الحاضنة") },
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
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        candidateParents.forEach { parentCat ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .clip(CircleShape)
                                                                .background(parseHex(parentCat.color)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = getIconVector(parentCat.icon),
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                        Text(parentCat.name, fontWeight = FontWeight.SemiBold)
                                                    }
                                                },
                                                onClick = {
                                                    selectedTargetParentId = parentCat.id
                                                    expandedDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isEnabled = if (makeRootSelected) {
                isCurrentlySubcategory
            } else {
                selectedTargetParentId != null && (selectedTargetParentId != category.parentId || !isCurrentlySubcategory)
            }
            TextButton(
                enabled = isEnabled,
                onClick = {
                    val newParent = if (makeRootSelected) null else selectedTargetParentId
                    onConfirm(newParent)
                }
            ) {
                Text("تأكيد النقل", fontWeight = FontWeight.Bold, color = if (isEnabled) Primary else TextGray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

