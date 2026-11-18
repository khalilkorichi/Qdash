package com.qdash.presentation.analytics.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.analytics.CategoryShare
import com.qdash.presentation.categories.MoveCategoryDialog
import com.qdash.presentation.components.UnifiedCategoryIcon
import com.qdash.presentation.components.safeParseColor
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Professional Category & Subcategory Transactions Detail Modal.
 * Displays complete transaction history for a parent category or subcategory,
 * summary metrics, interactive subcategory filter pills, sort toggle, color customization,
 * and hierarchy organization while strictly adhering to the active Light/Dark theme.
 */
@Composable
fun CategoryColorPickerDialog(
    category: CategoryShare,
    categoryTxs: List<Transaction>,
    allCategories: List<Category> = emptyList(),
    accounts: List<Account> = emptyList(),
    periodLabel: String = "الكل",
    onDismiss: (newColor: String) -> Unit,
    onColorChange: (categoryId: Long, color: String) -> Unit,
    onMoveCategory: ((categoryId: Long, newParentId: Long?) -> Unit)? = null
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedColor by remember(category.categoryId, category.color) { mutableStateOf(category.color) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var sortByHighestAmount by remember { mutableStateOf(false) }
    // null = All, -1L = Direct parent only, >0L = specific subcategory ID
    var activeSubFilterId by remember(category.categoryId, category.directOnly) { mutableStateOf<Long?>(null) }

    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val categoryModel = remember(category.categoryId, category.categoryName, allCategories) {
        allCategories.firstOrNull { it.id == category.categoryId }
            ?: allCategories.firstOrNull { it.name == category.categoryName }
    }

    val accentColor = safeParseColor(selectedColor, MaterialTheme.colorScheme.primary)

    val colorPresets = remember {
        listOf(
            "#6C63FF", "#22C55E", "#EF4444", "#3B82F6", "#F59E0B",
            "#EC4899", "#8B5CF6", "#06B6D4", "#10B981", "#F97316"
        )
    }
    val rainbowBrush = remember {
        Brush.sweepGradient(
            colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
        )
    }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.US) }

    // Available subcategory filter tabs when viewing a parent category with subcategories
    val childSubcategoriesWithTxs = remember(categoryModel, category.directOnly, categoryTxs, allCategories) {
        if (categoryModel == null || categoryModel.parentId != null || category.directOnly) {
            emptyList()
        } else {
            allCategories
                .filter { it.parentId == categoryModel.id }
                .mapNotNull { sub ->
                    val count = categoryTxs.count { it.categoryId == sub.id }
                    if (count > 0) sub to count else null
                }
                .sortedByDescending { it.second }
        }
    }
    val directParentTxCount = remember(categoryModel, category.directOnly, categoryTxs, childSubcategoriesWithTxs) {
        if (childSubcategoriesWithTxs.isEmpty() || categoryModel == null) 0
        else categoryTxs.count { it.categoryId == categoryModel.id }
    }

    val displayedTxs = remember(categoryTxs, activeSubFilterId, sortByHighestAmount, categoryModel) {
        val filtered = when (activeSubFilterId) {
            null -> categoryTxs
            -1L -> categoryTxs.filter { it.categoryId == categoryModel?.id }
            else -> categoryTxs.filter { it.categoryId == activeSubFilterId }
        }
        if (sortByHighestAmount) {
            filtered.sortedWith(compareByDescending<Transaction> { it.amount }.thenByDescending { it.date })
        } else {
            filtered.sortedByDescending { it.date }
        }
    }

    val displayedTotal = remember(displayedTxs) { displayedTxs.sumOf { it.amount } }
    val maxTxAmount = remember(displayedTxs) { displayedTxs.maxOfOrNull { it.amount } ?: 0.0 }

    if (showCustomColorDialog) {
        CustomColorSliderDialog(
            initialColor = selectedColor,
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { hex ->
                selectedColor = hex
                showCustomColorDialog = false
            }
        )
    }

    if (showMoveDialog && categoryModel != null) {
        MoveCategoryDialog(
            category = categoryModel,
            allCategories = allCategories,
            onDismiss = { showMoveDialog = false },
            onConfirm = { newParentId ->
                onMoveCategory?.invoke(categoryModel.id, newParentId)
                showMoveDialog = false
                onDismiss(selectedColor)
            }
        )
    }

    val dismissAndSave = {
        if (category.categoryId > 0L && selectedColor != category.color) {
            onColorChange(category.categoryId, selectedColor)
        }
        onDismiss(selectedColor)
    }

    Dialog(
        onDismissRequest = dismissAndSave,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 650.dp)
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.35f),
                    shape = ShapeTokens.Xl
                ),
            variant = CardVariant.SOLID,
            shape = ShapeTokens.Xl,
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // ── 1. Tinted Header Banner ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.18f))
                                    .border(1.2.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                UnifiedCategoryIcon(
                                    iconKey = categoryModel?.icon,
                                    tint = accentColor,
                                    modifier = Modifier.size(23.dp),
                                    emojiFontSize = 18.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.categoryName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val hierarchyLabel = when {
                                    category.directOnly -> "عمليات الفئة الرئيسية مباشرة • $periodLabel"
                                    categoryModel?.parentId != null -> {
                                        val parentName = categoryMap[categoryModel.parentId]?.name ?: "فئة رئيسية"
                                        "فرعية ضمن: $parentName • $periodLabel"
                                    }
                                    childSubcategoriesWithTxs.isNotEmpty() ->
                                        "فئة رئيسية (${childSubcategoriesWithTxs.size} فرعية) • $periodLabel"
                                    else -> "سجل عمليات الفئة • $periodLabel"
                                }
                                Text(
                                    text = hierarchyLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (categoryModel != null && onMoveCategory != null && !category.directOnly) {
                                HeaderActionCircleButton(
                                    icon = Icons.Default.DriveFileMove,
                                    contentDescription = "نقل وتنظيم الفئة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = { showMoveDialog = true }
                                )
                            }
                            if (category.categoryId > 0L && !category.directOnly) {
                                HeaderActionCircleButton(
                                    icon = Icons.Default.Palette,
                                    contentDescription = "تعديل اللون",
                                    tint = accentColor,
                                    onClick = { showColorPicker = !showColorPicker }
                                )
                            }
                            HeaderActionCircleButton(
                                icon = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = dismissAndSave
                            )
                        }
                    }

                    // ── 2. 3-Metric Summary Strip ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryStatTile(
                            label = "إجمالي العمليات",
                            value = FormatterUtils.formatCurrency(displayedTotal),
                            accentColor = accentColor,
                            modifier = Modifier.weight(1.15f)
                        )
                        SummaryStatTile(
                            label = "العدد والنسبة",
                            value = "${displayedTxs.size} عملية (${(category.percentage * 100).toInt()}%)",
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            label = "أعلى عملية",
                            value = FormatterUtils.formatCurrency(maxTxAmount),
                            accentColor = ExpenseRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Optional Expandable Color Picker ──
                AnimatedVisibility(
                    visible = showColorPicker,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(120))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "لون الفئة:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(colorPresets) { hex ->
                                val cVal = safeParseColor(hex, Color.Gray)
                                val isCurrent = selectedColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(cVal)
                                        .border(
                                            width = if (isCurrent) 2.5.dp else 0.dp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex }
                                )
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(rainbowBrush)
                                        .clickable { showCustomColorDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "لون مخصص",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // ── 3. Subcategory Filter Chips + Sort Bar ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (childSubcategoriesWithTxs.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                SubFilterChip(
                                    label = "الكل (${categoryTxs.size})",
                                    selected = activeSubFilterId == null,
                                    accentColor = accentColor,
                                    onClick = { activeSubFilterId = null }
                                )
                            }
                            items(childSubcategoriesWithTxs, key = { it.first.id }) { (sub, count) ->
                                val subCol = safeParseColor(sub.color, accentColor)
                                SubFilterChip(
                                    label = "${sub.name} ($count)",
                                    selected = activeSubFilterId == sub.id,
                                    accentColor = subCol,
                                    onClick = {
                                        activeSubFilterId = if (activeSubFilterId == sub.id) null else sub.id
                                    }
                                )
                            }
                            if (directParentTxCount > 0) {
                                item {
                                    SubFilterChip(
                                        label = "مباشر ($directParentTxCount)",
                                        selected = activeSubFilterId == -1L,
                                        accentColor = accentColor,
                                        onClick = {
                                            activeSubFilterId = if (activeSubFilterId == -1L) null else -1L
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تفاصيل العمليات الكاملة",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { sortByHighestAmount = !sortByHighestAmount }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (sortByHighestAmount) "الترتيب: الأعلى مبلغاً" else "الترتيب: الأحدث أولاً",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // ── 4. Virtualized LazyColumn of All Category/Subcategory Transactions ──
                if (displayedTxs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "لا توجد عمليات مسجلة ضمن هذه الفئة في الفترة المحددة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedTxs, key = { it.id }) { tx ->
                            val txCat = categoryMap[tx.categoryId]
                            val txAcc = accountMap[tx.accountId]
                            val showSubBadge = categoryModel?.parentId == null && !category.directOnly && txCat != null && txCat.id != categoryModel?.id
                            val txColor = safeParseColor(txCat?.color, accentColor)
                            val shareOfDisplayed = if (displayedTotal > 0.0) ((tx.amount / displayedTotal) * 100).toInt() else 0

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(txColor.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        UnifiedCategoryIcon(
                                            iconKey = txCat?.icon ?: categoryModel?.icon,
                                            tint = txColor,
                                            modifier = Modifier.size(18.dp),
                                            emojiFontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = tx.note?.takeIf { it.isNotBlank() } ?: (txCat?.name ?: category.categoryName),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (showSubBadge && txCat != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = txColor.copy(alpha = 0.14f)
                                                ) {
                                                    Text(
                                                        text = txCat.name,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = txColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (txAcc != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AccountBalanceWallet,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Text(
                                                            text = txAcc.name,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = FormatterUtils.convertNumerals(dateFormatter.format(Date(tx.occurredAt ?: tx.date))),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = FormatterUtils.formatCurrency(tx.amount, txAcc?.currency ?: "DZD"),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                        )
                                        if (displayedTxs.size > 1 && shareOfDisplayed > 0) {
                                            Text(
                                                text = "$shareOfDisplayed% من الفئة",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun HeaderActionCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp
                ),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SubFilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
