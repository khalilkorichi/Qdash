package com.qdash.presentation.transactions.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.model.SuggestionSource
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.components.UnifiedCategoryIcon
import com.qdash.presentation.components.safeParseColor
import com.qdash.ui.theme.TextGray

/**
 * Unified Category Selector + Integrated Smart Suggestion Card.
 * Keeps Category Selection as the primary card while seamlessly integrating:
 * 1. The inline "اقتراح ذكي" trigger button inside the primary category header.
 * 2. Quick 1-tap category & contextual subcategory strips for fast selection.
 * 3. An animated lower Smart Suggestion compartment that expands smoothly as part of the same card.
 */
@Composable
fun UnifiedCategorySelectorCard(
    categories: List<Category>,
    transactions: List<Transaction>,
    type: TransactionType,
    selectedCategoryId: Long?,
    subcategoryId: Long?,
    typeAccentColor: Color,
    smartSortEnabled: Boolean,
    suggestion: CategorySuggestion?,
    matchedCategory: Category?,
    isSuggestingCategory: Boolean,
    onToggleSmartSort: () -> Unit,
    onCategorySelected: (parentCatId: Long?, subCatId: Long?) -> Unit,
    onAddMainCategory: () -> Unit,
    onAddSubCategory: () -> Unit,
    onAiSuggest: () -> Unit,
    onAcceptSuggestion: (Long) -> Unit,
    onAcceptAndCreateSuggestion: () -> Unit,
    onDismissSuggestion: () -> Unit,
    onPickerOpened: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val primaryThemeColor = MaterialTheme.colorScheme.primary
    var showPickerSheet by remember { mutableStateOf(false) }

    val categoryFrequencies = remember(transactions) {
        val counts = HashMap<Long, Int>()
        for (tx in transactions) {
            val cid = tx.categoryId ?: continue
            counts[cid] = (counts[cid] ?: 0) + 1
        }
        counts
    }

    val typeCategories = remember(categories, type) {
        categories.filter { it.type.name == type.name }
    }

    val sortedParents = remember(typeCategories, smartSortEnabled, categoryFrequencies) {
        val parents = typeCategories.filter { it.parentId == null }
        if (smartSortEnabled) {
            parents.sortedByDescending { parent ->
                val direct = categoryFrequencies[parent.id] ?: 0
                val childrenSum = typeCategories
                    .asSequence()
                    .filter { it.parentId == parent.id }
                    .sumOf { categoryFrequencies[it.id] ?: 0 }
                direct + childrenSum
            }
        } else {
            parents.sortedBy { it.sortOrder }
        }
    }

    val selectedParentSubcategories = remember(typeCategories, selectedCategoryId, smartSortEnabled, categoryFrequencies) {
        if (selectedCategoryId == null) {
            emptyList()
        } else {
            val subs = typeCategories.filter { it.parentId == selectedCategoryId }
            if (smartSortEnabled) {
                subs.sortedByDescending { categoryFrequencies[it.id] ?: 0 }
            } else {
                subs.sortedBy { it.sortOrder }
            }
        }
    }

    val pickerDisplayList = remember(
        sortedParents,
        typeCategories,
        selectedCategoryId,
        subcategoryId,
        smartSortEnabled,
        categoryFrequencies
    ) {
        val items = ArrayList<PickerCategoryRowItem>(typeCategories.size)
        for (parent in sortedParents) {
            val isParentDirectlySelected = selectedCategoryId == parent.id && subcategoryId == null
            val parentUsage = (categoryFrequencies[parent.id] ?: 0) +
                typeCategories.asSequence().filter { it.parentId == parent.id }.sumOf { categoryFrequencies[it.id] ?: 0 }
            items.add(PickerCategoryRowItem.MainCategory(parent, isParentDirectlySelected, parentUsage))

            val children = typeCategories.filter { it.parentId == parent.id }
            val sortedChildren = if (smartSortEnabled) {
                children.sortedByDescending { categoryFrequencies[it.id] ?: 0 }
            } else {
                children.sortedBy { it.sortOrder }
            }
            for (child in sortedChildren) {
                items.add(
                    PickerCategoryRowItem.SubCategory(
                        category = child,
                        parentCategory = parent,
                        isSelected = subcategoryId == child.id,
                        usageCount = categoryFrequencies[child.id] ?: 0
                    )
                )
            }
        }
        items
    }

    val selectedMainCat = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId }
    }
    val selectedSubCat = remember(subcategoryId, categories) {
        categories.find { it.id == subcategoryId }
    }
    val selectedLabel = remember(selectedMainCat, selectedSubCat) {
        when {
            selectedSubCat != null && selectedMainCat != null -> "${selectedMainCat.name} ← ${selectedSubCat.name}"
            selectedMainCat != null -> selectedMainCat.name
            else -> "اختيار فئة المعاملة"
        }
    }
    val selectedColor = remember(selectedMainCat, selectedSubCat, typeAccentColor) {
        safeParseColor(selectedSubCat?.color ?: selectedMainCat?.color, typeAccentColor)
    }
    val selectedIcon = selectedSubCat?.icon ?: selectedMainCat?.icon ?: "📁"
    val effectiveSelectedId = subcategoryId ?: selectedCategoryId

    val hasActionableSuggestion = suggestion != null &&
        (matchedCategory != null || !suggestion.newCategoryName.isNullOrBlank())
    val showSuggestionCompartment = isSuggestingCategory || hasActionableSuggestion

    val suggestionCatColor = remember(matchedCategory, suggestion, primaryThemeColor) {
        safeParseColor(matchedCategory?.color ?: suggestion?.newCategoryColor, primaryThemeColor)
    }

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            hasActionableSuggestion -> suggestionCatColor.copy(alpha = 0.50f)
            isSuggestingCategory -> primaryThemeColor.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f)
        },
        animationSpec = tween(durationMillis = 260),
        label = "unified_cat_border"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (showSuggestionCompartment) 1.25.dp else 1.dp,
            color = cardBorderColor
        ),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── 1. PRIMARY BOX: Selected Category + Integrated "اقتراح ذكي" Button + Quick Chips ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Main category selector trigger
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onPickerOpened()
                                showPickerSheet = true
                            }
                            .padding(vertical = 2.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(selectedColor.copy(alpha = 0.14f))
                                .border(
                                    width = 1.dp,
                                    color = selectedColor.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            UnifiedCategoryIcon(
                                iconKey = selectedIcon,
                                tint = selectedColor,
                                modifier = Modifier.size(21.dp),
                                emojiFontSize = 17.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "فئة المعاملة",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "تغيير الفئة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Integrated "اقتراح ذكي" Button inside the Primary Box
                    Surface(
                        onClick = {
                            if (!isSuggestingCategory) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAiSuggest()
                            }
                        },
                        shape = RoundedCornerShape(11.dp),
                        color = primaryThemeColor.copy(alpha = if (isSuggestingCategory) 0.16f else 0.10f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = primaryThemeColor.copy(alpha = if (isSuggestingCategory) 0.55f else 0.32f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isSuggestingCategory) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 1.8.dp,
                                    color = primaryThemeColor
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = primaryThemeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = if (isSuggestingCategory) "يحلل…" else "اقتراح ذكي",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryThemeColor
                            )
                        }
                    }
                }

                // Quick 1-Tap Main Category Strip
                if (sortedParents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = sortedParents,
                            key = { "quick_parent_${it.id}" }
                        ) { parent ->
                            val isSelected = selectedCategoryId == parent.id
                            val catColor = safeParseColor(parent.color, typeAccentColor)
                            QuickCategoryPill(
                                label = parent.name,
                                iconKey = parent.icon,
                                accentColor = catColor,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedCategoryId != parent.id) {
                                        onCategorySelected(parent.id, null)
                                    } else if (subcategoryId != null) {
                                        onCategorySelected(parent.id, null)
                                    }
                                }
                            )
                        }
                        item(key = "quick_add_main") {
                            QuickAddCategoryPill(
                                label = "فئة",
                                accentColor = typeAccentColor,
                                onClick = onAddMainCategory
                            )
                        }
                    }
                }

                // Contextual Subcategory Strip (smoothly reveals when selected main category has subcategories)
                AnimatedVisibility(
                    visible = selectedParentSubcategories.isNotEmpty(),
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(150)) + fadeOut(tween(120))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item(key = "sub_label") {
                                Text(
                                    text = "الفرعية:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                            }
                            items(
                                items = selectedParentSubcategories,
                                key = { "quick_sub_${it.id}" }
                            ) { sub ->
                                val isSubSelected = subcategoryId == sub.id
                                val subColor = safeParseColor(sub.color, selectedColor)
                                QuickCategoryPill(
                                    label = sub.name,
                                    iconKey = sub.icon,
                                    accentColor = subColor,
                                    isSelected = isSubSelected,
                                    compact = true,
                                    onClick = {
                                        if (isSubSelected) {
                                            onCategorySelected(selectedCategoryId, null)
                                        } else {
                                            onCategorySelected(selectedCategoryId, sub.id)
                                        }
                                    }
                                )
                            }
                            item(key = "quick_add_sub") {
                                QuickAddCategoryPill(
                                    label = "فرعية",
                                    accentColor = selectedColor,
                                    compact = true,
                                    onClick = onAddSubCategory
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. INTEGRATED SMART SUGGESTION COMPARTMENT (Animated Lower Part of Same Card) ──
            AnimatedVisibility(
                visible = showSuggestionCompartment,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 240)) +
                    slideInVertically(
                        initialOffsetY = { -it / 4 },
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 130))
            ) {
                IntegratedSmartSuggestionCompartment(
                    suggestion = suggestion,
                    matchedCategory = matchedCategory,
                    isSuggestingCategory = isSuggestingCategory,
                    allCategories = categories,
                    effectiveSelectedCategoryId = effectiveSelectedId,
                    primaryThemeColor = primaryThemeColor,
                    onAcceptSuggestion = { catId ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAcceptSuggestion(catId)
                    },
                    onAcceptAndCreateSuggestion = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAcceptAndCreateSuggestion()
                    },
                    onDismissSuggestion = onDismissSuggestion
                )
            }
        }
    }

    if (showPickerSheet) {
        CategoryPickerBottomSheet(
            displayList = pickerDisplayList,
            smartSortEnabled = smartSortEnabled,
            typeAccentColor = typeAccentColor,
            onToggleSmartSort = onToggleSmartSort,
            onCategorySelected = onCategorySelected,
            onAddMainCategory = onAddMainCategory,
            onAddSubCategory = onAddSubCategory,
            onDismiss = { showPickerSheet = false }
        )
    }
}

@Composable
private fun IntegratedSmartSuggestionCompartment(
    suggestion: CategorySuggestion?,
    matchedCategory: Category?,
    isSuggestingCategory: Boolean,
    allCategories: List<Category>,
    effectiveSelectedCategoryId: Long?,
    primaryThemeColor: Color,
    onAcceptSuggestion: (Long) -> Unit,
    onAcceptAndCreateSuggestion: () -> Unit,
    onDismissSuggestion: () -> Unit
) {
    val hasSuggestion = suggestion != null &&
        (matchedCategory != null || !suggestion.newCategoryName.isNullOrBlank())

    val catColor = safeParseColor(
        matchedCategory?.color ?: suggestion?.newCategoryColor,
        primaryThemeColor
    )

    // Subtle entrance badge scale pulse whenever a new suggestion arrives
    val badgeScale = remember { Animatable(0.88f) }
    LaunchedEffect(suggestion?.suggestedCategoryId, suggestion?.newCategoryName, isSuggestingCategory) {
        if (hasSuggestion) {
            badgeScale.snapTo(0.86f)
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        catColor.copy(alpha = 0.09f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
                    )
                )
            )
    ) {
        // Luminous integrated top divider connecting primary box & suggestion compartment
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            catColor.copy(alpha = 0.55f),
                            primaryThemeColor.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )

        if (isSuggestingCategory && !hasSuggestion) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(primaryThemeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = primaryThemeColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المساعد الذكي يحلل تفاصيل العملية…",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "يتم مطابقة الوصف والمبلغ مع الفئات المناسبة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (suggestion != null) {
            val parentName = matchedCategory?.parentId?.let { pid ->
                allCategories.firstOrNull { it.id == pid }?.name
            }
            val baseName = matchedCategory?.name ?: suggestion.newCategoryName?.takeIf { it.isNotBlank() }
            if (baseName != null) {
                val fullCategoryPath = if (parentName != null) "$parentName ← $baseName" else baseName
                val categoryId = matchedCategory?.id
                val isNewCategory = categoryId == null && !suggestion.newCategoryName.isNullOrBlank()
                val isAlreadySelected = categoryId != null && categoryId == effectiveSelectedCategoryId
                val confidencePercent = (suggestion.confidenceScore * 100).toInt().coerceIn(0, 100)
                val sourceLabel = when (suggestion.suggestionSource) {
                    SuggestionSource.HISTORY -> "سجل سابق"
                    SuggestionSource.KEYWORD -> "مطابقة ذكية"
                    SuggestionSource.RULE -> "قاعدة معرفية"
                    SuggestionSource.AI -> "ذكاء اصطناعي"
                    SuggestionSource.NONE -> "اقتراح تلقائي"
                }
                val catIcon = matchedCategory?.icon ?: suggestion.newCategoryIcon

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    scaleX = badgeScale.value
                                    scaleY = badgeScale.value
                                }
                                .clip(RoundedCornerShape(11.dp))
                                .background(catColor.copy(alpha = 0.16f))
                                .border(
                                    width = 1.dp,
                                    color = catColor.copy(alpha = 0.30f),
                                    shape = RoundedCornerShape(11.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!catIcon.isNullOrBlank()) {
                                UnifiedCategoryIcon(
                                    iconKey = catIcon,
                                    tint = catColor,
                                    modifier = Modifier.size(18.dp),
                                    emojiFontSize = 15.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = primaryThemeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = sourceLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = primaryThemeColor
                                )
                                if (confidencePercent > 0) {
                                    Text(
                                        text = "$confidencePercent%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                if (isNewCategory) {
                                    Text(
                                        text = "فئة جديدة",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = primaryThemeColor,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(primaryThemeColor.copy(alpha = 0.14f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = fullCategoryPath,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                if (categoryId != null) {
                                    onAcceptSuggestion(categoryId)
                                } else if (isNewCategory) {
                                    onAcceptAndCreateSuggestion()
                                }
                            },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAlreadySelected) {
                                    primaryThemeColor.copy(alpha = 0.16f)
                                } else {
                                    primaryThemeColor
                                },
                                contentColor = if (isAlreadySelected) {
                                    primaryThemeColor
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    isNewCategory -> "إنشاء وتطبيق"
                                    isAlreadySelected -> "تأكيد"
                                    else -> "تطبيق"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismissSuggestion,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "تجاهل الاقتراح",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCategoryPill(
    label: String,
    iconKey: String,
    accentColor: Color,
    isSelected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (compact) 9.dp else 10.dp),
        color = if (isSelected) {
            accentColor.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.dp else 0.5.dp,
            color = if (isSelected) {
                accentColor.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 5.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            UnifiedCategoryIcon(
                iconKey = iconKey,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (compact) 13.dp else 14.dp),
                emojiFontSize = if (compact) 11.sp else 12.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickAddCategoryPill(
    label: String,
    accentColor: Color,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (compact) 9.dp else 10.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 9.dp,
                vertical = if (compact) 5.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
