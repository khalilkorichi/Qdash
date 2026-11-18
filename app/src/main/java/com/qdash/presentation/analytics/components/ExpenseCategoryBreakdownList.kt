package com.qdash.presentation.analytics.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Category
import com.qdash.presentation.analytics.CategoryShare
import com.qdash.presentation.analytics.HierarchicalCategoryShare
import com.qdash.presentation.components.UnifiedCategoryIcon
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.tokens.ShapeTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseCategoryBreakdownList(
    hierarchicalShares: List<HierarchicalCategoryShare>,
    categories: List<Category>,
    parsedShareColors: Map<String, Color?>,
    selectedCategory: CategoryShare?,
    expandedCategoryIds: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    onSelectedCategoryChange: (CategoryShare?) -> Unit,
    onOpenCategoryTransactions: (CategoryShare) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Subtle interaction guide strip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "اضغط مطولاً على الفئة لعرض عملياتها • انقر على الفئة الفرعية للتفاصيل",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        hierarchicalShares.forEach { item ->
            val share = item.rootShare
            val rootCat = item.rootCategory
            val parseColor = parsedShareColors[share.categoryName] ?: MaterialTheme.colorScheme.primary
            val isRootSelected = selectedCategory?.categoryId == share.categoryId && selectedCategory.directOnly == share.directOnly
            val isChildSelected = item.subcategoryShares.any { it.categoryId == selectedCategory?.categoryId } ||
                (item.directShare != null && selectedCategory?.categoryId == item.directShare.categoryId && selectedCategory.directOnly)
            val isAnySelected = isRootSelected || isChildSelected
            val isExpanded = item.isExpandable && expandedCategoryIds.contains(share.categoryId)

            val chevronRotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 240),
                label = "chevronRotation_${share.categoryId}"
            )

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isAnySelected) 1.5.dp else 1.dp,
                        color = if (isAnySelected) parseColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = ShapeTokens.Lg
                    ),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg,
                backgroundColor = if (isAnySelected) {
                    parseColor.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                },
                onClick = null
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Parent Category Row: Tap = select & expand/collapse, Long-Press = open transactions detail box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeTokens.Lg)
                            .combinedClickable(
                                onClick = {
                                    if (item.isExpandable) {
                                        onToggleExpand(share.categoryId)
                                    }
                                    onSelectedCategoryChange(if (isRootSelected) null else share)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenCategoryTransactions(share)
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(parseColor.copy(alpha = 0.14f), CircleShape)
                                .border(1.dp, parseColor.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            UnifiedCategoryIcon(
                                iconKey = rootCat?.icon,
                                tint = parseColor,
                                modifier = Modifier.size(20.dp),
                                emojiFontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = share.categoryName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = FormatterUtils.formatCurrency(share.amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (share.transactionCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                    ) {
                                        Text(
                                            text = "${share.transactionCount} عملية",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (item.isExpandable) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = parseColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${item.subcategoryShares.size} فرعية",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = parseColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { share.percentage.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = parseColor,
                                    trackColor = parseColor.copy(alpha = 0.14f)
                                )

                                Text(
                                    text = "${(share.percentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (isAnySelected) parseColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Quick 1-tap transactions detail button alongside long-press
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenCategoryTransactions(share)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "عرض عمليات ${share.categoryName}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        if (item.isExpandable) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isExpanded) parseColor.copy(alpha = 0.14f) else Color.Transparent)
                                    .clickable { onToggleExpand(share.categoryId) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "طي الفئات الفرعية" else "عرض الفئات الفرعية",
                                    tint = if (isExpanded) parseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(chevronRotation)
                                )
                            }
                        }
                    }

                    // Expanded Subcategories List: Tap OR Long-Press opens full subcategory transactions!
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(animationSpec = tween(260)) + fadeIn(animationSpec = tween(180)),
                        exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(140))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                                .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )

                            item.subcategoryShares.forEach { subShare ->
                                val subCatModel = categories.firstOrNull { it.id == subShare.categoryId }
                                    ?: categories.firstOrNull { it.name == subShare.categoryName }
                                val subColor = parsedShareColors[subShare.categoryName] ?: parseColor
                                val isSubSelected = selectedCategory?.categoryId == subShare.categoryId && !selectedCategory.directOnly
                                val parentPct = if (share.amount > 0) ((subShare.amount / share.amount) * 100).toInt() else 0

                                SubcategoryBreakdownItemRow(
                                    title = subShare.categoryName,
                                    iconKey = subCatModel?.icon,
                                    accentColor = subColor,
                                    amount = subShare.amount,
                                    transactionCount = subShare.transactionCount,
                                    parentSharePercent = parentPct,
                                    totalSharePercent = (subShare.percentage * 100).toInt(),
                                    isSelected = isSubSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectedCategoryChange(subShare)
                                        onOpenCategoryTransactions(subShare)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOpenCategoryTransactions(subShare)
                                    }
                                )
                            }

                            if (item.directShare != null) {
                                val directShare = item.directShare
                                val isDirectSelected = selectedCategory?.categoryId == directShare.categoryId && selectedCategory.directOnly
                                val directParentPct = if (share.amount > 0) ((directShare.amount / share.amount) * 100).toInt() else 0

                                SubcategoryBreakdownItemRow(
                                    title = "${share.categoryName} (عمليات مباشرة)",
                                    iconKey = rootCat?.icon ?: "receipt_long",
                                    accentColor = parseColor,
                                    amount = directShare.amount,
                                    transactionCount = directShare.transactionCount,
                                    parentSharePercent = directParentPct,
                                    totalSharePercent = (directShare.percentage * 100).toInt(),
                                    isSelected = isDirectSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectedCategoryChange(directShare)
                                        onOpenCategoryTransactions(directShare)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOpenCategoryTransactions(directShare)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubcategoryBreakdownItemRow(
    title: String,
    iconKey: String?,
    accentColor: Color,
    amount: Double,
    transactionCount: Int,
    parentSharePercent: Int,
    totalSharePercent: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Subtle tree connector bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(42.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = if (isSelected) 0.9f else 0.35f))
        )
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .border(
                    width = if (isSelected) 1.4.dp else 0.8.dp,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) accentColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(accentColor.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    UnifiedCategoryIcon(
                        iconKey = iconKey,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp),
                        emojiFontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (transactionCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = accentColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "$transactionCount",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = FormatterUtils.formatCurrency(amount),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { (parentSharePercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(CircleShape),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.14f)
                        )
                        Text(
                            text = "$parentSharePercent% من الفئة • $totalSharePercent% إجمالاً",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "عرض العمليات",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
