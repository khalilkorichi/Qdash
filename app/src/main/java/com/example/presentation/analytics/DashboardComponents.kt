package com.example.presentation.analytics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.FormatterUtils
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.CardAiContext
import com.example.domain.model.CardAiContextType
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.designsystem.components.*
import com.example.ui.designsystem.tokens.*
import java.util.Calendar
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.*

private val arabicMonths = arrayOf(
    "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
    "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)

/**
 * 1. DashboardOverviewCard - Consumption of Income summary
 */
@Composable
fun DashboardOverviewCard(
    totalIncome: Double,
    totalExpenses: Double,
    onAiChatClick: (CardAiContext) -> Unit,
    modifier: Modifier = Modifier,
    periodStart: Long = 0L,
    periodEnd: Long = Long.MAX_VALUE
) {
    val savings = (totalIncome - totalExpenses).coerceAtLeast(0.0)
    val spentRatio = if (totalIncome > 0) (totalExpenses / totalIncome).toFloat() else 0f
    
    val animatedRatio by animateFloatAsState(
        targetValue = spentRatio.coerceIn(0f, 1.2f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "spentRatio"
    )

    // Color based on spent ratio
    val indicatorColor = when {
        spentRatio <= 0.70f -> IncomeGreen
        spentRatio <= 0.90f -> SavingsAmber
        else -> ExpenseRed
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ملخص الدخل والاستهلاك",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تحليل كفاءة الإنفاق مقارنة بالدخل الكلي",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                CardAiChatButton(
                    onClick = {
                        val chartDataString = org.json.JSONObject().apply {
                            put("totalIncome", totalIncome)
                            put("totalExpenses", totalExpenses)
                            put("savings", savings)
                            put("spentRatio", spentRatio)
                        }.toString()
                        
                        val cardContext = CardAiContext(
                            cardId = "dashboard_overview",
                            cardTitle = "ملخص الدخل والاستهلاك",
                            cardType = CardAiContextType.DashboardOverview,
                            chartData = chartDataString,
                            periodStart = periodStart,
                            periodEnd = periodEnd,
                            tooltipContent = "يقيس هذا الكارت نسبة ما تستهلكه مصاريفك الإجمالية من دخلك المالي الكلي.\n\nالهدف المالي: يفضل إبقاء هذه النسبة دون الـ 80% دائماً لضمان توفير 20% على الأقل كمدخرات للطوارئ والمستقبل."
                        )
                        onAiChatClick(cardContext)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Ring Chart
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = with(density) { 10.dp.toPx() }
                        val sizeMin = size.minDimension
                        val radius = (sizeMin - strokeWidth) / 2
                        val arcSize = Size(radius * 2, radius * 2)
                        val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)

                        // Draw background track
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )

                        // Draw progress arc
                        drawArc(
                            color = indicatorColor,
                            startAngle = -90f,
                            sweepAngle = animatedRatio * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(spentRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (spentRatio > 1f) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مستهلك",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextGray
                        )
                    }
                }

                // Summary details list
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(IncomeGreen, CircleShape))
                            Text("إجمالي الدخل", style = MaterialTheme.typography.labelMedium, color = TextGray)
                        }
                        Text(
                            text = FormatterUtils.formatCurrency(totalIncome),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = IncomeGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(ExpenseRed, CircleShape))
                            Text("إجمالي المصاريف", style = MaterialTheme.typography.labelMedium, color = TextGray)
                        }
                        Text(
                            text = FormatterUtils.formatCurrency(totalExpenses),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = ExpenseRed
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Text("صافي التوفير", style = MaterialTheme.typography.labelMedium, color = TextGray)
                        }
                        Text(
                            text = FormatterUtils.formatCurrency(savings),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (savings > 0) IncomeGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Exceed Warning
            if (totalExpenses > totalIncome && totalIncome > 0) {
                val deficit = totalExpenses - totalIncome
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ExpenseRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                        Text(
                            text = "تحذير: نفقاتك تتجاوز دخلك بـ ${FormatterUtils.formatCurrency(deficit)}. يرجى كبح الصرف.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. CategoryVsIncomeChartCard - Pie Chart or Bar Chart showing expenses as % of income
 */
@Composable
fun CategoryVsIncomeChartCard(
    transactions: List<Transaction>,
    categories: List<Category>,
    totalIncome: Double,
    selectedCategory: CategoryShare?,
    onSelectedCategoryChange: (CategoryShare?) -> Unit,
    onHelpClick: (String, String) -> Unit,
    onCategoryLongClick: (CategoryShare) -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    var viewMode by remember { mutableStateOf(ChartViewMode.DONUT) }

    // 1. Group expenses and calculate amounts
    val expensesOnly = remember(transactions) { transactions.filter { it.type == TransactionType.EXPENSE } }
    val totalExpenses = remember(expensesOnly) { expensesOnly.sumOf { it.amount } }

    // 2. Prepare Category Shares list
    val shares = remember(expensesOnly, categories, totalIncome, totalExpenses) {
        expensesOnly.groupBy { it.categoryId }.map { (catId, txList) ->
            val cat = categories.firstOrNull { it.id == catId }
            val sum = txList.sumOf { it.amount }
            // Percentage of income (or fallback to percentage of total expenses if income is 0)
            val pct = if (totalIncome > 0) (sum / totalIncome).toFloat() else if (totalExpenses > 0) (sum / totalExpenses).toFloat() else 0f
            CategoryShare(
                categoryId = catId ?: 0L,
                categoryName = cat?.name ?: "أخرى",
                amount = sum,
                percentage = pct,
                color = cat?.color ?: "#6C63FF"
            )
        }.sortedByDescending { it.amount }
    }

    // 3. For Donut Chart: We need slices that sum up to exactly 100% (or 1.0f) of the circle.
    // If totalExpenses <= totalIncome, we can represent totalIncome as the 100% circle:
    // Slices: categories + "Savings" slice.
    // If totalExpenses > totalIncome or totalIncome == 0, we represent totalExpenses as 100% of the circle, and just show details relative to income in the legend.
    val useIncomeAsBase = totalIncome > 0 && totalExpenses <= totalIncome
    val donutSlices = remember(shares, totalIncome, totalExpenses, useIncomeAsBase) {
        if (useIncomeAsBase) {
            val list = shares.map { share ->
                donutSliceData(
                    name = share.categoryName,
                    pctOfCircle = share.percentage,
                    color = share.color,
                    isSavings = false,
                    originalShare = share
                )
            }.toMutableList()
            // Add savings slice
            val savingsAmt = totalIncome - totalExpenses
            val savingsPct = (savingsAmt / totalIncome).toFloat()
            if (savingsPct > 0.01f) {
                list.add(
                    donutSliceData(
                        name = "الادخار / المتبقي",
                        pctOfCircle = savingsPct,
                        color = "#22C55E", // Green
                        isSavings = true,
                        originalShare = CategoryShare(0L, "الادخار / المتبقي", savingsAmt, savingsPct, "#22C55E")
                    )
                )
            }
            list
        } else {
            // Slices sum to 100% of expenses
            shares.map { share ->
                val pctOfExp = if (totalExpenses > 0) (share.amount / totalExpenses).toFloat() else 0f
                donutSliceData(
                    name = share.categoryName,
                    pctOfCircle = pctOfExp,
                    color = share.color,
                    isSavings = false,
                    originalShare = share
                )
            }
        }
    }

    val animatedProgresses = donutSlices.associate { slice ->
        val isSelected = selectedCategory?.categoryName == slice.name
        slice.name to animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "anim_${slice.name}"
        ).value
    }

    val parsedShareColors = remember(donutSlices) {
        donutSlices.associate { slice ->
            slice.name to (try { Color(android.graphics.Color.parseColor(slice.color)) } catch (e: Exception) { null })
        }
    }

    val parsedBarBrushes = remember(shares) {
        shares.associate { share ->
            val c = try { Color(android.graphics.Color.parseColor(share.color)) } catch (e: Exception) { Color.Gray }
            share.categoryName to Brush.verticalGradient(colors = listOf(c, c.copy(alpha = 0.5f)))
        }
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نسبة المصاريف من الدخل",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (useIncomeAsBase) "توزيع الدخل الكلي بين النفقات والادخار" else "توزيع المصاريف مقارنة بالدخل المتاح",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bar button
                        val barBg by animateColorAsState(
                            targetValue = if (viewMode == ChartViewMode.BAR) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = tween(220), label = "barBg"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(barBg)
                                .clickable { viewMode = ChartViewMode.BAR }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "أعمدة",
                                tint = if (viewMode == ChartViewMode.BAR) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Donut button
                        val donutBg by animateColorAsState(
                            targetValue = if (viewMode == ChartViewMode.DONUT) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = tween(220), label = "donutBg"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(donutBg)
                                .clickable { viewMode = ChartViewMode.DONUT }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PieChart,
                                contentDescription = "دائرة",
                                tint = if (viewMode == ChartViewMode.DONUT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (shares.isEmpty()) {
                Text(
                    text = "لا توجد مصاريف مسجلة لعرضها",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else if (viewMode == ChartViewMode.DONUT) {
                // Donut Canvas
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .pointerInput(donutSlices) {
                            val longPressTimeoutMs = 400L
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downTime = System.currentTimeMillis()
                                val downOffset = down.position
                                var longPressTriggered = false

                                do {
                                    val elapsed = System.currentTimeMillis() - downTime
                                    if (!longPressTriggered && elapsed >= longPressTimeoutMs) {
                                        longPressTriggered = true
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val dx = downOffset.x - centerX
                                        val dy = downOffset.y - centerY
                                        val distance = sqrt(dx * dx + dy * dy)
                                        val outerRadius = centerX
                                        val innerRadius = outerRadius - 38.dp.toPx()
                                        if (distance in innerRadius..outerRadius) {
                                            val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                            val chartAngle = (degrees + 90f + 360f) % 360f
                                            var currentAngle = 0f
                                            var bestMatch: donutSliceData? = null
                                            for (slice in donutSlices) {
                                                val sweep = slice.pctOfCircle * 360f
                                                if (chartAngle >= currentAngle && chartAngle < currentAngle + sweep) {
                                                    bestMatch = slice; break
                                                }
                                                currentAngle += sweep
                                            }
                                            if (bestMatch != null && !bestMatch.isSavings) {
                                                onCategoryLongClick(bestMatch.originalShare)
                                            }
                                        }
                                    }
                                    val event = withTimeoutOrNull(16) { awaitPointerEvent() } ?: continue
                                    val upPointer = event.changes.firstOrNull { it.changedToUp() }
                                    if (upPointer != null) {
                                        upPointer.consume()
                                        if (!longPressTriggered) {
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val dx = downOffset.x - centerX
                                            val dy = downOffset.y - centerY
                                            val distance = sqrt(dx * dx + dy * dy)
                                            val outerRadius = centerX
                                            val innerRadius = outerRadius - 38.dp.toPx()
                                            if (distance in innerRadius..outerRadius) {
                                                val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                val chartAngle = (degrees + 90f + 360f) % 360f
                                                var currentAngle = 0f
                                                var bestMatch: donutSliceData? = null
                                                for (slice in donutSlices) {
                                                    val sweep = slice.pctOfCircle * 360f
                                                    if (chartAngle >= currentAngle && chartAngle < currentAngle + sweep) {
                                                        bestMatch = slice; break
                                                    }
                                                    currentAngle += sweep
                                                }
                                                if (bestMatch != null) {
                                                    onSelectedCategoryChange(
                                                        if (selectedCategory?.categoryName == bestMatch.name) null
                                                        else bestMatch.originalShare
                                                    )
                                                }
                                            } else {
                                                onSelectedCategoryChange(null)
                                            }
                                        }
                                        break
                                    }
                                } while (true)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val themePrimaryColor = MaterialTheme.colorScheme.primary

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val outerRadius = centerX - 10.dp.toPx()

                        var currentAngle = -90f
                        donutSlices.forEach { slice ->
                            val sweep = slice.pctOfCircle * 360f
                            val parseColor = parsedShareColors[slice.name] ?: themePrimaryColor
                            val isSelected = selectedCategory?.categoryName == slice.name
                            val isActive = selectedCategory == null || isSelected
                            val progress = animatedProgresses[slice.name] ?: 0f

                            val strokeWidthVal = with(density) { (28.dp + (6.dp * progress)).toPx() }
                            val shiftX = if (progress > 0f) {
                                val midAngle = currentAngle + sweep / 2f
                                val rad = Math.toRadians(midAngle.toDouble())
                                val popOutDist = with(density) { (5.dp * progress).toPx() }
                                (popOutDist * cos(rad)).toFloat()
                            } else 0f
                            
                            val shiftY = if (progress > 0f) {
                                val midAngle = currentAngle + sweep / 2f
                                val rad = Math.toRadians(midAngle.toDouble())
                                val popOutDist = with(density) { (5.dp * progress).toPx() }
                                (popOutDist * sin(rad)).toFloat()
                            } else 0f

                            val r_mid = outerRadius - strokeWidthVal / 2f
                            val gap = if (donutSlices.size > 1) 2f else 0f
                            val adjustedSweep = (sweep - gap).coerceAtLeast(0f)

                            val topLeftOffset = Offset(centerX + shiftX - r_mid, centerY + shiftY - r_mid)
                            val arcSize = Size(r_mid * 2, r_mid * 2)

                            drawArc(
                                color = parseColor.copy(alpha = if (isActive) 1f else 0.25f),
                                startAngle = currentAngle + gap / 2f,
                                sweepAngle = adjustedSweep,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = arcSize,
                                style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round)
                            )
                            currentAngle += sweep
                        }
                    }

                    // Center label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectedCategoryChange(null) }
                            .padding(16.dp)
                    ) {
                        if (selectedCategory != null) {
                            Text(
                                text = selectedCategory.categoryName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatterUtils.formatCurrency(selectedCategory.amount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = if (useIncomeAsBase) "إجمالي الدخل" else "إجمالي المصاريف",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatterUtils.formatCurrency(if (useIncomeAsBase) totalIncome else totalExpenses),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Float labels
                    var accumulatedAngle = 0f
                    donutSlices.forEach { slice ->
                        val sweep = slice.pctOfCircle * 360f
                        val isSelected = selectedCategory?.categoryName == slice.name
                        if (isSelected) {
                            val midAngle = -90f + accumulatedAngle + sweep / 2f
                            val rad = Math.toRadians(midAngle.toDouble())
                            val progress = animatedProgresses[slice.name] ?: 0f
                            
                            val strokeWidthDp = 28.dp + 6.dp * progress
                            val radiusDp = 110.dp - 10.dp - strokeWidthDp / 2f
                            val finalRadiusDp = radiusDp + 5.dp * progress
                            
                            val badgeOffsetX = (finalRadiusDp.value * cos(rad)).toFloat().dp
                            val badgeOffsetY = (finalRadiusDp.value * sin(rad)).toFloat().dp
                            val parseColor = parsedShareColors[slice.name] ?: Primary

                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .absoluteOffset(x = badgeOffsetX, y = badgeOffsetY)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    border = BorderStroke(1.5.dp, parseColor),
                                    shape = RoundedCornerShape(16.dp),
                                    shadowElevation = 6.dp
                                ) {
                                    Text(
                                        text = "${slice.name} • ${(slice.pctOfCircle * 100).toInt()}%",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        accumulatedAngle += sweep
                    }
                }
            } else {
                // Bar Chart relative to income
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        shares.take(6).forEach { share ->
                            val parseColor = try { Color(android.graphics.Color.parseColor(share.color)) } catch (e: Exception) { Primary }
                            val isSelected = selectedCategory?.categoryName == share.categoryName
                            val isActive = selectedCategory == null || isSelected

                            val displayPct = if (totalIncome > 0) (share.amount / totalIncome).toFloat() else share.percentage

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectedCategoryChange(if (isSelected) null else share) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(130.dp * displayPct.coerceIn(0.05f, 1f))
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            parsedBarBrushes[share.categoryName] ?: Brush.verticalGradient(
                                                colors = listOf(parseColor, parseColor.copy(alpha = 0.5f))
                                            ),
                                            alpha = if (isActive) 1f else 0.3f
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) parseColor else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = share.categoryName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold, fontSize = 9.sp),
                                    color = if (isSelected) parseColor else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${(displayPct * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Income / Expense Warnings
            if (totalIncome == 0.0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "ملاحظة: لم يتم تسجيل أي دخل لهذه الفترة. تم حساب النسب المئوية مقارنة بإجمالي المصاريف.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Legend / Breakdown List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shares.forEach { share ->
                    val parseColor = try { Color(android.graphics.Color.parseColor(share.color)) } catch (e: Exception) { Primary }
                    val isSelected = selectedCategory?.categoryName == share.categoryName
                    val pctOfIncome = if (totalIncome > 0) (share.amount / totalIncome) else 0.0
                    val pctOfExp = if (totalExpenses > 0) (share.amount / totalExpenses) else 0.0

                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth(),
                        variant = CardVariant.SOLID,
                        shape = ShapeTokens.Lg,
                        backgroundColor = if (isSelected) parseColor.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        onClick = { onSelectedCategoryChange(if (isSelected) null else share) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(parseColor))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(share.categoryName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(FormatterUtils.formatCurrency(share.amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (totalIncome > 0) "نسبة من الدخل: ${(pctOfIncome * 100).toInt()}%" 
                                               else "نسبة من المصاريف: ${(pctOfExp * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                    if (totalIncome > 0) {
                                        Text(
                                            text = "من النفقات: ${(pctOfExp * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextGray
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

private data class donutSliceData(
    val name: String,
    val pctOfCircle: Float,
    val color: String,
    val isSavings: Boolean,
    val originalShare: CategoryShare
)

/**
 * 3. MonthComparisonCard - Compare expenses between two months
 */
@Composable
fun MonthComparisonCard(
    transactions: List<Transaction>,
    categories: List<Category>,
    compareMonthA: Int,
    compareYearA: Int,
    compareMonthB: Int,
    compareYearB: Int,
    onMonthAChange: (Int, Int) -> Unit,
    onMonthBChange: (Int, Int) -> Unit,
    onAiChatClick: (CardAiContext) -> Unit,
    modifier: Modifier = Modifier,
    periodStart: Long = 0L,
    periodEnd: Long = Long.MAX_VALUE
) {
    var showPickerA by remember { mutableStateOf(false) }
    var showPickerB by remember { mutableStateOf(false) }

    // 1. Filter transactions
    val txsA = remember(transactions, compareMonthA, compareYearA) {
        transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == compareMonthA && cal.get(Calendar.YEAR) == compareYearA
        }
    }

    val txsB = remember(transactions, compareMonthB, compareYearB) {
        transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == compareMonthB && cal.get(Calendar.YEAR) == compareYearB
        }
    }

    // 2. Calculations
    val expA = remember(txsA) { txsA.filter { it.type == TransactionType.EXPENSE } }
    val expB = remember(txsB) { txsB.filter { it.type == TransactionType.EXPENSE } }

    val totalExpA = remember(expA) { expA.sumOf { it.amount } }
    val totalExpB = remember(expB) { expB.sumOf { it.amount } }

    val diff = totalExpB - totalExpA
    val pctDiff = if (totalExpA > 0) (diff / totalExpA).toFloat() else 0f

    // 3. Category grouped amounts
    val catAmountsA = remember(expA) { expA.groupBy { it.categoryId }.mapValues { it.value.sumOf { tx -> tx.amount } } }
    val catAmountsB = remember(expB) { expB.groupBy { it.categoryId }.mapValues { it.value.sumOf { tx -> tx.amount } } }

    // All active category IDs in both months
    val allActiveCategoryIds = remember(catAmountsA, catAmountsB) {
        (catAmountsA.keys + catAmountsB.keys).toSet()
    }

    val comparisonList = remember(allActiveCategoryIds, categories, catAmountsA, catAmountsB) {
        allActiveCategoryIds.map { catId ->
            val cat = categories.firstOrNull { it.id == catId }
            val amtA = catAmountsA[catId] ?: 0.0
            val amtB = catAmountsB[catId] ?: 0.0
            val catDiff = amtB - amtA
            val catPct = if (amtA > 0) (catDiff / amtA).toFloat() else 0f
            CategoryComparison(
                categoryId = catId ?: -1L,
                name = cat?.name ?: "أخرى",
                color = cat?.color ?: "#6C63FF",
                amountA = amtA,
                amountB = amtB,
                diff = catDiff,
                pctDiff = catPct
            )
        }.sortedByDescending { max(it.amountA, it.amountB) }
    }

    if (showPickerA) {
        MonthYearPickerDialog(
            initialMonth = compareMonthA,
            initialYear = compareYearA,
            onDismiss = { showPickerA = false },
            onConfirm = { m, y ->
                onMonthAChange(m, y)
                showPickerA = false
            }
        )
    }

    if (showPickerB) {
        MonthYearPickerDialog(
            initialMonth = compareMonthB,
            initialYear = compareYearB,
            onDismiss = { showPickerB = false },
            onConfirm = { m, y ->
                onMonthBChange(m, y)
                showPickerB = false
            }
        )
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مقارنة المصاريف بين الأشهر",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تتبع تقلبات إنفاقك وتغير الصرف للفئات",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                CardAiChatButton(
                    onClick = {
                        val chartDataString = org.json.JSONObject().apply {
                            put("compareMonthA", compareMonthA + 1)
                            put("compareYearA", compareYearA)
                            put("compareMonthB", compareMonthB + 1)
                            put("compareYearB", compareYearB)
                            put("totalExpensesA", totalExpA)
                            put("totalExpensesB", totalExpB)
                            put("difference", diff)
                            put("percentageDifference", pctDiff)
                            
                            put("categoryComparisons", org.json.JSONArray().apply {
                                comparisonList.forEach {
                                    put(org.json.JSONObject().apply {
                                        put("category", it.name)
                                        put("amountA", it.amountA)
                                        put("amountB", it.amountB)
                                        put("difference", it.diff)
                                        put("percentageDifference", it.pctDiff)
                                    })
                                }
                            })
                        }.toString()
                        
                        val cardContext = CardAiContext(
                            cardId = "month_comparison",
                            cardTitle = "مقارنة المصاريف بين الأشهر",
                            cardType = CardAiContextType.MonthComparison,
                            chartData = chartDataString,
                            periodStart = periodStart,
                            periodEnd = periodEnd,
                            tooltipContent = "تسمح لك هذه الميزة باختيار أي شهرين ومقارنة حجم ومجالات الصرف بينهما.\n\nالفائدة: توضح لك بدقة الفئات التي ارتفع فيها الصرف فجأة لكي تبحث عن الأسباب وتحاول معالجتها في الفترات القادمة."
                        )
                        onAiChatClick(cardContext)
                    }
                )
            }

            // Selectors side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector Month A (Base Month)
                Button(
                    onClick = { showPickerA = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${arabicMonths[compareMonthA]} ${compareYearA}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Centered "VS" label
                Box(
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("مقابل", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = TextGray)
                }

                // Selector Month B (Comparison Month)
                Button(
                    onClick = { showPickerB = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = SavingsAmber, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${arabicMonths[compareMonthB]} ${compareYearB}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Compare summary stats card
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xl,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مجموع الصرف الأول", style = MaterialTheme.typography.labelSmall, color = TextGray)
                        Text(FormatterUtils.formatCurrency(totalExpA), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("مجموع الصرف الثاني", style = MaterialTheme.typography.labelSmall, color = TextGray)
                        Text(FormatterUtils.formatCurrency(totalExpB), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(12.dp))

                    // Comparison Result
                    Column(
                        modifier = Modifier.weight(1.2f),
                        horizontalAlignment = Alignment.End
                    ) {
                        val isIncrease = diff >= 0
                        val resColor = if (isIncrease) ExpenseRed else IncomeGreen
                        Text("الفارق الكلي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (isIncrease) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = resColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = (if (isIncrease) "+" else "") + "${(pctDiff * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 14.sp),
                                color = resColor
                            )
                        }
                        Text(
                            text = (if (isIncrease) "+" else "") + FormatterUtils.formatCurrency(diff),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = resColor
                        )
                    }
                }
            }

            if (comparisonList.isEmpty()) {
                Text(
                    text = "لا توجد مصاريف مسجلة للمقارنة في كلا الشهرين",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                )
            } else {
                // Grouped Bar Chart
                Text(
                    text = "مخطط المقارنة بالفئات (أزرق: الشهر الأول | برتقالي: الشهر الثاني)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextGray
                )

                GroupedBarChart(
                    comparisonList = comparisonList,
                    labelA = "${arabicMonths[compareMonthA]}",
                    labelB = "${arabicMonths[compareMonthB]}"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Breakdown list
                Text(
                    text = "تفصيل مقارنة النفقات بالفئات",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    comparisonList.forEach { item ->
                        val itemColor = try { Color(android.graphics.Color.parseColor(item.color)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                        val isIncrease = item.diff >= 0
                        val resColor = if (isIncrease) ExpenseRed else IncomeGreen

                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = CardVariant.SOLID,
                            shape = ShapeTokens.Xl,
                            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(itemColor))
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    
                                    // Change Tag
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = resColor.copy(alpha = 0.08f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isIncrease) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = resColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = (if (isIncrease) "+" else "") + "${(item.pctDiff * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = resColor
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("الشهر الأول", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                        Text(FormatterUtils.formatCurrency(item.amountA), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }

                                    Column {
                                        Text("الشهر الثاني", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                        Text(FormatterUtils.formatCurrency(item.amountB), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("التغير الفعلي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                        Text(
                                            text = (if (isIncrease) "+" else "") + FormatterUtils.formatCurrency(item.diff),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = resColor
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

data class CategoryComparison(
    val categoryId: Long,
    val name: String,
    val color: String,
    val amountA: Double,
    val amountB: Double,
    val diff: Double,
    val pctDiff: Float
)

/**
 * GroupedBarChart - Custom Canvas for month to month comparative bar chart (Scrollable)
 */
@Composable
fun GroupedBarChart(
    comparisonList: List<CategoryComparison>,
    labelA: String,
    labelB: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    
    val blueColor = MaterialTheme.colorScheme.primary
    val orangeColor = SavingsAmber

    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    
    val textPaint = remember(axisLabelColor, density) {
        android.graphics.Paint().apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
    }

    val xTextPaint = remember(axisLabelColor, density) {
        android.graphics.Paint().apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
    }

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    val maxVal = comparisonList.maxOfOrNull { max(it.amountA, it.amountB) }?.takeIf { it > 0 } ?: 10000.0
    val yMax = maxVal * 1.15

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Fixed Y-Axis Container
            Canvas(
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
            ) {
                val topPadding = 8.dp.toPx()
                val bottomPadding = 20.dp.toPx()
                val chartHeight = size.height - topPadding - bottomPadding
                val gridLines = 4
                
                val decimalFormat = java.text.DecimalFormat("#,##0")

                for (i in 0 until gridLines) {
                    val fraction = i.toFloat() / (gridLines - 1)
                    val amount = yMax * fraction
                    val y = size.height - bottomPadding - (fraction * chartHeight)
                    
                    val labelText = FormatterUtils.convertNumerals("${decimalFormat.format(amount)}")
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            labelText,
                            size.width - 6.dp.toPx(),
                            y + 3.dp.toPx(),
                            textPaint
                        )
                    }
                }
            }

            // Scrollable bars container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                val itemWidthDp = 75.dp
                val contentWidth = itemWidthDp * comparisonList.size.coerceAtLeast(4)

                Canvas(
                    modifier = Modifier
                        .width(contentWidth)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp)
                ) {
                    val topPadding = 8.dp.toPx()
                    val bottomPadding = 20.dp.toPx()
                    val chartHeight = size.height - topPadding - bottomPadding
                    val chartWidth = size.width

                    // 1. Draw Gridlines
                    val gridLines = 4
                    for (i in 0 until gridLines) {
                        val fraction = i.toFloat() / (gridLines - 1)
                        val y = size.height - bottomPadding - (fraction * chartHeight)

                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (i > 0) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                        )
                    }

                    // 2. Draw Grouped Bars
                    val numGroups = comparisonList.size
                    val groupWidth = chartWidth / numGroups
                    val barWidth = 12.dp.toPx()
                    val barSpacing = 2.dp.toPx()
                    val cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

                    comparisonList.forEachIndexed { idx, item ->
                        val centerX = (idx + 0.5f) * groupWidth

                        // Bar Heights
                        val heightA = ((item.amountA / yMax) * chartHeight).toFloat()
                        val heightB = ((item.amountB / yMax) * chartHeight).toFloat()

                        // Draw Bar A (Blue/Primary)
                        if (heightA > 0f) {
                            drawRoundRect(
                                color = blueColor,
                                topLeft = Offset(centerX - barWidth - barSpacing / 2, size.height - bottomPadding - heightA),
                                size = Size(barWidth, heightA),
                                cornerRadius = cornerRadius
                            )
                        }

                        // Draw Bar B (Orange/Amber)
                        if (heightB > 0f) {
                            drawRoundRect(
                                color = orangeColor,
                                topLeft = Offset(centerX + barSpacing / 2, size.height - bottomPadding - heightB),
                                size = Size(barWidth, heightB),
                                cornerRadius = cornerRadius
                            )
                        }

                        // Draw X-Axis category name label
                        drawIntoCanvas { canvas ->
                            val label = if (item.name.length > 7) item.name.take(6) + ".." else item.name
                            canvas.nativeCanvas.drawText(
                                label,
                                centerX,
                                size.height - bottomPadding + 14.dp.toPx(),
                                xTextPaint
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * MonthYearPickerDialog - A custom picker dialog to select month & year for comparison
 */
@Composable
fun MonthYearPickerDialog(
    initialMonth: Int,
    initialYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var selectedYear by remember { mutableStateOf(initialYear) }
    
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = remember { (currentYear - 4..currentYear).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onConfirm(selectedMonth, selectedYear) }) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        title = {
            Text(
                text = "اختر الشهر والسنة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Year selection row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("السنة:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        years.forEach { y ->
                            val isSelected = selectedYear == y
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedYear = y },
                                label = { Text("$y") }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Month Grid (4x3 layout)
                Text("الشهر:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val m = row * 3 + col
                                val isSelected = selectedMonth == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                        )
                                        .clickable { selectedMonth = m }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = arabicMonths[m],
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

/**
 * YearPickerDialog - A custom picker dialog to select year for comparison/dashboard
 */
@Composable
fun YearPickerDialog(
    initialYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = remember { (currentYear - 4..currentYear).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onConfirm(selectedYear) }) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        title = {
            Text(
                text = "اختر السنة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                years.forEach { y ->
                    val isSelected = selectedYear == y
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedYear = y },
                        label = { Text("$y") }
                    )
                }
            }
        }
    )
}
