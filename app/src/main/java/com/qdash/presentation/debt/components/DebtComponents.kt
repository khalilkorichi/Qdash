package com.qdash.presentation.debt.components

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.*
import com.qdash.presentation.components.UnifiedCategoryIcon
import com.qdash.presentation.components.safeParseColor
import com.qdash.presentation.navigation.LocalBottomBarPadding
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TransferBlue
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class DebtListFilter {
    ALL, INSTALLMENT, REGULAR, ACTIVE_ONLY, COMPLETED
}

@Composable
fun DebtsMainContent(
    debts: List<Debt>,
    orderedPlanDebts: List<Debt>,
    insights: List<String>,
    strategyResults: List<DebtStrategyResult>,
    selectedStrategy: String,
    selectedDirection: DebtDirection,
    onDirectionChange: (DebtDirection) -> Unit,
    onStrategyChange: (String) -> Unit,
    onSelectDebt: (Debt) -> Unit,
    onPayClick: (Debt) -> Unit,
    onAddPrimaryClick: (DebtListFilter) -> Unit
) {
    val bottomPadding = LocalBottomBarPadding.current
    var selectedFilter by remember(selectedDirection) { mutableStateOf(DebtListFilter.ALL) }

    val owedByMeAll = remember(debts) { debts.filter { it.direction == DebtDirection.OWED_BY_ME } }
    val owedToMeAll = remember(debts) { debts.filter { it.direction == DebtDirection.OWED_TO_ME } }

    val owedByMeActiveCount = remember(owedByMeAll) { owedByMeAll.count { !it.isClosed } }
    val owedToMeActiveCount = remember(owedToMeAll) { owedToMeAll.count { !it.isClosed } }

    // Top priority debt (#1 target from GetDebtPlanUseCase)
    val topPriorityTargetId = remember(orderedPlanDebts) {
        orderedPlanDebts.firstOrNull { !it.isClosed }?.id
    }

    // Build displayed list sorted by strategy (for OWED_BY_ME) or due date (for OWED_TO_ME)
    val displayedDebts = remember(
        owedByMeAll,
        owedToMeAll,
        orderedPlanDebts,
        selectedDirection,
        selectedFilter
    ) {
        if (selectedDirection == DebtDirection.OWED_BY_ME) {
            val activeOrdered = if (orderedPlanDebts.isNotEmpty()) {
                orderedPlanDebts
            } else {
                owedByMeAll.filter { !it.isClosed }
            }
            val closedOrdered = owedByMeAll.filter { it.isClosed }.sortedByDescending { it.createdAt }

            when (selectedFilter) {
                DebtListFilter.ALL -> activeOrdered + closedOrdered
                DebtListFilter.INSTALLMENT -> activeOrdered.filterIsInstance<InstallmentDebt>()
                DebtListFilter.REGULAR -> activeOrdered.filterIsInstance<RegularDebt>()
                DebtListFilter.COMPLETED -> closedOrdered
                DebtListFilter.ACTIVE_ONLY -> activeOrdered
            }
        } else {
            val activeLent = owedToMeAll
                .filter { !it.isClosed }
                .sortedWith(compareBy<Debt> { it.dueDate ?: Long.MAX_VALUE }.thenByDescending { it.remainingAmount })
            val closedLent = owedToMeAll.filter { it.isClosed }.sortedByDescending { it.createdAt }

            when (selectedFilter) {
                DebtListFilter.ALL -> activeLent + closedLent
                DebtListFilter.ACTIVE_ONLY -> activeLent
                DebtListFilter.COMPLETED -> closedLent
                else -> activeLent + closedLent
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 24.dp)
    ) {
        // 1. TOP DIRECTION SWITCHER (2-Pill Segmented Toggle)
        item {
            SegmentedPillToggle(
                option1Label = "التزامات عليّ (${FormatterUtils.convertNumerals(owedByMeActiveCount.toString())})",
                option2Label = "ديون لي عند الغير (${FormatterUtils.convertNumerals(owedToMeActiveCount.toString())})",
                selectedIndex = if (selectedDirection == DebtDirection.OWED_BY_ME) 0 else 1,
                onOptionSelected = { idx ->
                    onDirectionChange(if (idx == 0) DebtDirection.OWED_BY_ME else DebtDirection.OWED_TO_ME)
                }
            )
        }

        // 2. UNIFIED HERO SUMMARY CARD ADAPTING TO SELECTED DIRECTION
        item {
            DebtsHeroSummaryCard(
                directionDebts = if (selectedDirection == DebtDirection.OWED_BY_ME) owedByMeAll else owedToMeAll,
                allDebts = debts,
                selectedDirection = selectedDirection
            )
        }

        // 3. DEDICATED SMART PAYOFF PLAN CARD (Shown for OWED_BY_ME when active debts exist)
        if (selectedDirection == DebtDirection.OWED_BY_ME && owedByMeActiveCount > 0) {
            item {
                SmartPayoffPlanCard(
                    strategyResults = strategyResults,
                    selectedStrategy = selectedStrategy,
                    topTargetDebt = orderedPlanDebts.firstOrNull { !it.isClosed },
                    onStrategyChange = onStrategyChange
                )
            }
        }

        // 4. SUBTLE ACTIONABLE INSIGHT BANNER (if overdue or urgent)
        if (insights.isNotEmpty() && (owedByMeActiveCount > 0 || owedToMeActiveCount > 0)) {
            val primaryInsight = insights.firstOrNull()
            if (!primaryInsight.isNullOrBlank()) {
                item {
                    val accent = if (selectedDirection == DebtDirection.OWED_TO_ME) IncomeGreen else TransferBlue
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = CardVariant.SOLID,
                        shape = ShapeTokens.Md,
                        backgroundColor = accent.copy(alpha = 0.08f),
                        borderStroke = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = FormatterUtils.convertNumerals(primaryInsight),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 5. SECTION HEADER + PRIMARY ADD CTA + COMPACT SINGLE-ROW FILTER CHIPS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDirection == DebtDirection.OWED_BY_ME) "قائمة الالتزامات" else "قائمة السلفيات المستحقة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    AppButton(
                        onClick = { onAddPrimaryClick(selectedFilter) },
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.PRIMARY,
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    ) {
                        Text(
                            text = if (selectedDirection == DebtDirection.OWED_BY_ME) "إضافة التزام" else "إضافة سلفة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                val filterOptions = remember(selectedDirection, owedByMeAll, owedToMeAll) {
                    if (selectedDirection == DebtDirection.OWED_BY_ME) {
                        val activeList = owedByMeAll.filter { !it.isClosed }
                        val instCount = activeList.count { it is InstallmentDebt }
                        val regCount = activeList.count { it is RegularDebt }
                        val closedCount = owedByMeAll.count { it.isClosed }
                        listOf(
                            DebtListFilter.ALL to "الكل (${owedByMeAll.size})",
                            DebtListFilter.INSTALLMENT to "أقساط مجدولة ($instCount)",
                            DebtListFilter.REGULAR to "ديون مرنة ($regCount)",
                            DebtListFilter.COMPLETED to "مكتملة ($closedCount)"
                        )
                    } else {
                        val activeCount = owedToMeAll.count { !it.isClosed }
                        val closedCount = owedToMeAll.count { it.isClosed }
                        listOf(
                            DebtListFilter.ALL to "الكل (${owedToMeAll.size})",
                            DebtListFilter.ACTIVE_ONLY to "نشطة ($activeCount)",
                            DebtListFilter.COMPLETED to "مكتملة ($closedCount)"
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions, key = { it.first.name }) { (filter, label) ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = ShapeTokens.Full,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(
                                text = FormatterUtils.convertNumerals(label),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. DEBT LIST CARDS OR CLEAN EMPTY STATE
        if (displayedDebts.isEmpty()) {
            item {
                val (emptyTitle, emptyDesc) = when {
                    selectedFilter == DebtListFilter.COMPLETED ->
                        "لا توجد سجلات مكتملة بعد" to "ستظهر هنا الديون والسلفيات التي تم سدادها أو تحصيلها بالكامل."
                    selectedDirection == DebtDirection.OWED_TO_ME ->
                        "لا توجد سلفيات عند الغير" to "يمكنك تسجيل أي مبلغ أقرضته للغير وخصمه من محفظتك لمتابعة تحصيله بدقة."
                    selectedFilter == DebtListFilter.INSTALLMENT ->
                        "لا توجد أقساط مجدولة" to "أضف قرضاً أو تقسيطاً شهرياً ليدخل تلقائياً في خطة السداد الذكية."
                    selectedFilter == DebtListFilter.REGULAR ->
                        "لا توجد ديون مرنة" to "لا توجد ديون شخصية أو التزامات مرنة نشطة حالياً."
                    else ->
                        "سجلك خالٍ من الالتزامات" to "لا توجد التزامات مالية قائمة حالياً. اضغط على «إضافة التزام» لتسجيل قرض أو دين جديد."
                }
                EmptyStateView(
                    title = emptyTitle,
                    description = emptyDesc
                )
            }
        } else {
            items(displayedDebts, key = { it.id }) { debt ->
                val isTargetOne = selectedDirection == DebtDirection.OWED_BY_ME &&
                        !debt.isClosed &&
                        debt.id == topPriorityTargetId &&
                        owedByMeActiveCount > 0
                DebtListCard(
                    debt = debt,
                    isTopPriorityTarget = isTargetOne,
                    onSelectDebt = onSelectDebt,
                    onPayClick = onPayClick
                )
            }
        }
    }
}

@Composable
fun DebtsHeroSummaryCard(
    directionDebts: List<Debt>,
    allDebts: List<Debt>,
    selectedDirection: DebtDirection
) {
    val isLent = selectedDirection == DebtDirection.OWED_TO_ME
    val activeDebts = remember(directionDebts) { directionDebts.filter { !it.isClosed } }
    val totalRemaining = remember(activeDebts) { activeDebts.sumOf { it.remainingAmount } }
    val totalOriginal = remember(directionDebts) { directionDebts.sumOf { it.totalAmount } }
    val totalSettled = remember(directionDebts) {
        directionDebts.sumOf { (it.totalAmount - it.remainingAmount).coerceAtLeast(0.0) }
    }
    val progress = remember(totalOriginal, totalSettled, directionDebts) {
        if (directionDebts.isEmpty() || totalOriginal <= 0.0) 0f
        else (totalSettled / totalOriginal).toFloat().coerceIn(0f, 1f)
    }
    val nearestDueDebt = remember(activeDebts) {
        activeDebts.filter { it.dueDate != null }.minByOrNull { it.dueDate!! }
    }

    val totalOwedByMe = remember(allDebts) {
        allDebts.filter { it.direction == DebtDirection.OWED_BY_ME && !it.isClosed }.sumOf { it.remainingAmount }
    }
    val totalOwedToMe = remember(allDebts) {
        allDebts.filter { it.direction == DebtDirection.OWED_TO_ME && !it.isClosed }.sumOf { it.remainingAmount }
    }
    val hasBothDirections = remember(allDebts) {
        allDebts.any { it.direction == DebtDirection.OWED_BY_ME } &&
                allDebts.any { it.direction == DebtDirection.OWED_TO_ME }
    }

    val accentColor = if (isLent) IncomeGreen else MaterialTheme.colorScheme.primary
    val amountColor = when {
        activeDebts.isEmpty() -> MaterialTheme.colorScheme.onSurface
        isLent -> IncomeGreen
        else -> ExpenseRed
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top label + Active Count pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLent) "إجمالي المستحقات المتبقية للتحصيل" else "إجمالي الالتزامات المتبقية للسداد",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val badgeColor = if (activeDebts.isEmpty()) IncomeGreen else accentColor
                Box(
                    modifier = Modifier
                        .clip(ShapeTokens.Full)
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val countLabel = if (activeDebts.isEmpty()) {
                        "لا توجد التزامات نشطة"
                    } else if (isLent) {
                        "${FormatterUtils.convertNumerals(activeDebts.size.toString())} سلفة نشطة"
                    } else {
                        "${FormatterUtils.convertNumerals(activeDebts.size.toString())} التزام نشط"
                    }
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            // Main Remaining Total
            Text(
                text = FormatterUtils.formatCurrency(totalRemaining),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = amountColor
            )

            // Progress section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pct = (progress * 100).roundToInt()
                    Text(
                        text = if (isLent) "نسبة التحصيل: ${FormatterUtils.convertNumerals(pct.toString())}%"
                        else "نسبة السداد: ${FormatterUtils.convertNumerals(pct.toString())}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = (if (isLent) "المحصّل: " else "المسدد: ") + FormatterUtils.formatCurrency(totalSettled),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (progress >= 1f) IncomeGreen else accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // Footer Metadata Row: Nearest Due Date + Total Original / Net Position
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Nearest Due Date
                val isOverdue = nearestDueDebt?.dueDate != null && nearestDueDebt.dueDate!! < System.currentTimeMillis()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ShapeTokens.Md)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isLent) "أقرب موعد تحصيل" else "أقرب استحقاق قادم",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (nearestDueDebt?.dueDate != null) {
                                FormatterUtils.formatDate(nearestDueDebt.dueDate!!) + if (isOverdue) " (متأخر)" else ""
                            } else {
                                "غير محدد"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Metric 2: Net Position (if user uses both directions) or Original Total
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ShapeTokens.Md)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (hasBothDirections) {
                            val net = totalOwedToMe - totalOwedByMe
                            Text(
                                text = "صافي مركز الديون",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val prefix = if (net > 0) "+" else if (net < 0) "-" else ""
                            Text(
                                text = "$prefix${FormatterUtils.formatCurrency(kotlin.math.abs(net))}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (net >= 0) IncomeGreen else ExpenseRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = if (isLent) "إجمالي السلفيات المسجلة" else "إجمالي أصل الالتزامات",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatterUtils.formatCurrency(totalOriginal),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartPayoffPlanCard(
    strategyResults: List<DebtStrategyResult>,
    selectedStrategy: String,
    topTargetDebt: Debt?,
    onStrategyChange: (String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    val snowballRes = remember(strategyResults) {
        strategyResults.find { it.strategyName.contains("كرة الثلج") }
    }
    val avalancheRes = remember(strategyResults) {
        strategyResults.find { it.strategyName.contains("الانهيار") }
    }
    val activeResult = when (selectedStrategy) {
        "avalanche" -> avalancheRes ?: snowballRes
        else -> snowballRes ?: avalancheRes
    }
    val interestSavedByAvalanche = remember(snowballRes, avalancheRes) {
        if (snowballRes != null && avalancheRes != null) {
            (snowballRes.totalInterestCharged - avalancheRes.totalInterestCharged).coerceAtLeast(0.0)
        } else 0.0
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "خطة السداد الذكية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اختر الاستراتيجية لترتيب أولويات السداد تلقائياً",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3-Option Strategy Selector
            val strategies = listOf(
                Triple("snowball", "كرة الثلج", "الأصغر أولاً"),
                Triple("avalanche", "الانهيار الجليدي", "الأعلى فائدة"),
                Triple("priority", "حسب الأولوية", "الأهم والأقرب")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                strategies.forEach { (id, title, subtitle) ->
                    val isSelected = selectedStrategy == id
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(ShapeTokens.Md)
                            .background(
                                if (isSelected) primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = ShapeTokens.Md
                            )
                            .clickable { onStrategyChange(id) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isSelected) primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Simulation Metrics Grid
            if (activeResult != null) {
                val monthsInt = ceil(activeResult.durationInMonths).toInt().coerceAtLeast(1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlanMetricTile(
                        label = "مدة التصفية المقدّرة",
                        value = "${FormatterUtils.convertNumerals(monthsInt.toString())} شهر",
                        valueColor = primary,
                        modifier = Modifier.weight(1f)
                    )
                    PlanMetricTile(
                        label = "التخصيص الشهري الموصى به",
                        value = FormatterUtils.formatCurrency(activeResult.monthlyPaymentNeeded),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlanMetricTile(
                        label = "تاريخ التحرر المتوقع",
                        value = FormatterUtils.formatDate(activeResult.estimatedDebtFreeDate),
                        valueColor = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    val interestText = when {
                        selectedStrategy == "avalanche" && interestSavedByAvalanche >= 10.0 ->
                            "توفير ${FormatterUtils.formatCurrency(interestSavedByAvalanche)}"
                        activeResult.totalInterestCharged > 1.0 ->
                            FormatterUtils.formatCurrency(activeResult.totalInterestCharged)
                        else -> "٠ د.ج (بدون فوائد)"
                    }
                    val interestColor = when {
                        selectedStrategy == "avalanche" && interestSavedByAvalanche >= 10.0 -> IncomeGreen
                        activeResult.totalInterestCharged > 1.0 -> SavingsAmber
                        else -> IncomeGreen
                    }
                    PlanMetricTile(
                        label = if (selectedStrategy == "avalanche" && interestSavedByAvalanche >= 10.0) "الوفر في الفوائد" else "إجمالي تكلفة الفوائد",
                        value = interestText,
                        valueColor = interestColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Target #1 summary pill
                if (topTargetDebt != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeTokens.Md)
                            .background(primary.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "الهدف الأول الآن: ${topTargetDebt.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = FormatterUtils.formatCurrency(topTargetDebt.remainingAmount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanMetricTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(ShapeTokens.Md)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DebtListCard(
    debt: Debt,
    isTopPriorityTarget: Boolean = false,
    onSelectDebt: (Debt) -> Unit,
    onPayClick: (Debt) -> Unit
) {
    val paidAmount = (debt.totalAmount - debt.remainingAmount).coerceAtLeast(0.0)
    val progress = if (debt.totalAmount > 0.0) (paidAmount / debt.totalAmount).toFloat().coerceIn(0f, 1f) else 1f
    val isLent = debt.direction == DebtDirection.OWED_TO_ME
    val isClosed = debt.isClosed
    val isPastDue = debt.dueDate != null && debt.dueDate!! < System.currentTimeMillis() && !isClosed
    val isUrgent = !isLent && debt is InstallmentDebt && debt.priority == 1 && !isClosed

    val primary = MaterialTheme.colorScheme.primary
    val customAccent = safeParseColor(
        debt.color,
        if (isLent) IncomeGreen else if (debt is InstallmentDebt) ExpenseRed else SavingsAmber
    )
    val cardBorder = when {
        isTopPriorityTarget -> BorderStroke(1.5.dp, primary)
        isPastDue -> BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.55f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        onClick = { onSelectDebt(debt) },
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderStroke = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Target #1 Priority Banner
            if (isTopPriorityTarget) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(primary.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الهدف الأول للسداد الآن 🎯",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primary
                    )
                    Text(
                        text = "أولوية الخطة #١",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = primary
                    )
                }
            }

            // Header: Icon + Title + Creditor/Debtor + Status/Type Badge + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconTint = if (isClosed) IncomeGreen else customAccent
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isClosed) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            UnifiedCategoryIcon(
                                iconKey = debt.icon.ifBlank { if (isLent) "payments" else "credit_card" },
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = debt.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val partyPrefix = if (isLent) "المدين: " else "الدائن: "
                        Text(
                            text = partyPrefix + debt.creditorName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    val (badgeLabel, badgeColor) = when {
                        isClosed -> (if (isLent) "مستردة" else "مكتمل") to IncomeGreen
                        isPastDue -> "متأخر" to ExpenseRed
                        isUrgent -> "أولوية قصوى" to ExpenseRed
                        isLent -> "سلفة للغير" to IncomeGreen
                        debt is InstallmentDebt -> "أقساط" to TransferBlue
                        else -> "دين مرن" to SavingsAmber
                    }
                    Box(
                        modifier = Modifier
                            .clip(ShapeTokens.Full)
                            .background(badgeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "عرض التفاصيل",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Progress Bar + Paid vs Remaining amounts
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "المتبقي",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FormatterUtils.formatCurrency(debt.remainingAmount),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = when {
                                isClosed -> IncomeGreen
                                isLent -> IncomeGreen
                                else -> ExpenseRed
                            }
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isLent) "المحصّل من ${FormatterUtils.formatCurrency(debt.totalAmount)}"
                            else "المدفوع من ${FormatterUtils.formatCurrency(debt.totalAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${FormatterUtils.formatCurrency(paidAmount)} (${FormatterUtils.convertNumerals((progress * 100).roundToInt().toString())}%)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape),
                    color = if (isClosed || isLent) IncomeGreen else primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Footer: Due date / Installment metadata + ONE Clean Primary Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val metaText = when {
                        debt.dueDate != null -> {
                            val prefix = if (isLent) "الاسترداد: " else "الاستحقاق: "
                            prefix + FormatterUtils.formatDate(debt.dueDate!!)
                        }
                        debt is InstallmentDebt -> {
                            val rateSuffix = if (debt.interestRate > 0.0) " • فائدة ${debt.interestRate}%" else ""
                            "القسط الشهري: ${FormatterUtils.formatCurrency(debt.minimumPayment)}$rateSuffix"
                        }
                        else -> if (isLent) "سلفة مرنة بدون تاريخ محدد" else "سداد مرن على دفعات"
                    }
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPastDue) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isPastDue) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isClosed) {
                    Spacer(modifier = Modifier.width(10.dp))
                    val actionText = if (isLent) "تحصيل دفعة" else "تسديد دفعة"
                    AppButton(
                        onClick = { onPayClick(debt) },
                        variant = ButtonVariant.SOLID,
                        intent = if (isLent) ButtonIntent.SUCCESS else ButtonIntent.PRIMARY,
                        shape = ShapeTokens.Md
                    ) {
                        Text(
                            text = actionText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
