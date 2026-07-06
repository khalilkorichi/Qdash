package com.qdash.presentation.analytics.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.ui.theme.*
import com.qdash.presentation.analytics.CashFlowTrend
import com.qdash.presentation.analytics.HelpIconButton

/**
 * Cash-flow line chart card (income vs expense over time).
 * Extracted from AnalyticsScreen to keep it under the SIZE-002 line limit.
 */
@Composable
fun AnalyticsCashFlowChart(
    trendData: List<CashFlowTrend>,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val textPaint = remember(labelColor, density) {
        android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
    }
    val xTextPaint = remember(labelColor, density) {
        android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
    }
    val decimalFormatter = remember { java.text.DecimalFormat("#,##0") }
    var selectedTrendIndex by remember { mutableStateOf<Int?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التدفق النقدي التاريخي (الدخل vs المصروف)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                HelpIconButton(onClick = onHelpClick)
            }
            Spacer(modifier = Modifier.height(20.dp))

            val gridLineColor = MaterialTheme.colorScheme.outlineVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(trendData) {
                        detectTapGestures { offset ->
                            val leftPadding = 56.dp.toPx()
                            val rightPadding = 8.dp.toPx()
                            val chartWidth = size.width - leftPadding - rightPadding
                            if (trendData.size > 1) {
                                val stepX = chartWidth / (trendData.size - 1)
                                val clickedIndex = ((offset.x - leftPadding + stepX / 2f) / stepX)
                                    .toInt().coerceIn(0, trendData.size - 1)
                                selectedTrendIndex = if (selectedTrendIndex == clickedIndex) null else clickedIndex
                            } else if (trendData.isNotEmpty()) {
                                selectedTrendIndex = 0
                            }
                        }
                    }
            ) {
                val leftPadding = 56.dp.toPx()
                val rightPadding = 8.dp.toPx()
                val topPadding = 12.dp.toPx()
                val bottomPadding = 24.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding

                if (trendData.isNotEmpty()) {
                    val maxAmount = trendData.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0 } ?: 10000.0
                    val yAxisMax = maxAmount * 1.15

                    // ── 1. Gridlines & Y-Axis Labels ──
                    val gridLines = 4
                    for (i in 0 until gridLines) {
                        val fraction = i.toFloat() / (gridLines - 1)
                        val amount = yAxisMax * fraction
                        val y = size.height - bottomPadding - (fraction * chartHeight)
                        drawLine(
                            color = if (i == 0) gridLineColor.copy(alpha = 0.8f) else gridLineColor.copy(alpha = 0.25f),
                            start = Offset(leftPadding, y),
                            end = Offset(size.width - rightPadding, y),
                            strokeWidth = (if (i == 0) 1.5.dp else 1.dp).toPx(),
                            pathEffect = if (i > 0) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                        )
                        val labelText = FormatterUtils.convertNumerals("${decimalFormatter.format(amount)} دج")
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(labelText, leftPadding - 8.dp.toPx(), y + 4.dp.toPx(), textPaint)
                        }
                    }

                    // ── 2. Paths ──
                    val stepX = chartWidth / (trendData.size - 1).coerceAtLeast(1)
                    val incomePath = Path()
                    val expensePath = Path()

                    trendData.forEachIndexed { i, trend ->
                        val x = leftPadding + i * stepX
                        val yIncome = size.height - bottomPadding - ((trend.income / yAxisMax) * chartHeight).toFloat().coerceIn(0f, chartHeight)
                        val yExpense = size.height - bottomPadding - ((trend.expense / yAxisMax) * chartHeight).toFloat().coerceIn(0f, chartHeight)
                        if (i == 0) {
                            incomePath.moveTo(x, yIncome)
                            expensePath.moveTo(x, yExpense)
                        } else {
                            val prevX = leftPadding + (i - 1) * stepX
                            val prevYIncome = size.height - bottomPadding - ((trendData[i - 1].income / yAxisMax) * chartHeight).toFloat().coerceIn(0f, chartHeight)
                            val prevYExpense = size.height - bottomPadding - ((trendData[i - 1].expense / yAxisMax) * chartHeight).toFloat().coerceIn(0f, chartHeight)
                            incomePath.cubicTo(prevX + (x - prevX) / 2f, prevYIncome, prevX + (x - prevX) / 2f, yIncome, x, yIncome)
                            expensePath.cubicTo(prevX + (x - prevX) / 2f, prevYExpense, prevX + (x - prevX) / 2f, yExpense, x, yExpense)
                        }
                    }

                    // ── 3. Gradient Fills ──
                    if (trendData.size > 1) {
                        val incomeFillPath = Path().apply {
                            addPath(incomePath)
                            lineTo(leftPadding + chartWidth, size.height - bottomPadding)
                            lineTo(leftPadding, size.height - bottomPadding)
                            close()
                        }
                        drawPath(path = incomeFillPath, brush = Brush.verticalGradient(colors = listOf(IncomeGreen.copy(alpha = 0.18f), Color.Transparent), startY = topPadding, endY = size.height - bottomPadding), style = Fill)

                        val expenseFillPath = Path().apply {
                            addPath(expensePath)
                            lineTo(leftPadding + chartWidth, size.height - bottomPadding)
                            lineTo(leftPadding, size.height - bottomPadding)
                            close()
                        }
                        drawPath(path = expenseFillPath, brush = Brush.verticalGradient(colors = listOf(ExpenseRed.copy(alpha = 0.12f), Color.Transparent), startY = topPadding, endY = size.height - bottomPadding), style = Fill)
                    }

                    // ── 4. Line Strokes ──
                    drawPath(path = incomePath, color = IncomeGreen, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(path = expensePath, color = ExpenseRed, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

                    // ── 5. Points & X-Axis Labels ──
                    trendData.forEachIndexed { i, trend ->
                        val x = leftPadding + i * stepX
                        val yIncome = size.height - bottomPadding - ((trend.income / yAxisMax) * chartHeight).toFloat().coerceIn(0f, chartHeight)
                        val yExpense = size.height - bottomPadding - ((trend.expense / yAxisMax) * chartHeight).toFloat().coerceIn(0f, chartHeight)

                        if (selectedTrendIndex == i) {
                            drawLine(color = primaryColor.copy(alpha = 0.18f), start = Offset(x, topPadding), end = Offset(x, size.height - bottomPadding), strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
                            drawCircle(color = Color.Black, radius = 7.dp.toPx(), center = Offset(x, yIncome))
                            drawCircle(color = IncomeGreen, radius = 5.dp.toPx(), center = Offset(x, yIncome))
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, yIncome))
                            drawCircle(color = Color.Black, radius = 7.dp.toPx(), center = Offset(x, yExpense))
                            drawCircle(color = ExpenseRed, radius = 5.dp.toPx(), center = Offset(x, yExpense))
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, yExpense))
                        } else {
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, yIncome))
                            drawCircle(color = IncomeGreen, radius = 2.dp.toPx(), center = Offset(x, yIncome))
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, yExpense))
                            drawCircle(color = ExpenseRed, radius = 2.dp.toPx(), center = Offset(x, yExpense))
                        }
                        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(trend.periodLabel, x, size.height - bottomPadding + 18.dp.toPx(), xTextPaint) }
                    }
                }
            }

            // Selected point detail card
            selectedTrendIndex?.let { index ->
                val trend = trendData.getOrNull(index)
                if (trend != null) {
                    AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            variant = CardVariant.SOLID,
                            shape = ShapeTokens.Lg,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "تفاصيل التدفق: ${trend.periodLabel}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    IconButton(onClick = { selectedTrendIndex = null }, modifier = Modifier.size(20.dp)) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق التفاصيل", tint = TextGray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("الدخل", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                        Text(FormatterUtils.formatCurrency(trend.income), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = IncomeGreen)
                                    }
                                    Column {
                                        Text("المصاريف", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                        Text(FormatterUtils.formatCurrency(trend.expense), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
                                    }
                                    val diff = trend.income - trend.expense
                                    Column {
                                        Text("الصافي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                        Text(
                                            text = (if (diff >= 0) "+" else "") + FormatterUtils.formatCurrency(diff),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (diff >= 0) IncomeGreen else ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(IncomeGreen, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الدخل الكلي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(ExpenseRed, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("المصاريف", style = MaterialTheme.typography.labelSmall, color = TextGray)
                }
            }
        }
    }
}
