package com.qdash.presentation.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.presentation.home.ChartSkeleton
import com.qdash.presentation.home.ExpenseTrendPoint
import com.qdash.ui.theme.TextGray

@Composable
fun ExpenseAnalysisChartCard(
    expenseTrendData: List<ExpenseTrendPoint>,
    chartPeriod: String,
    isLoading: Boolean,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "تحليل المصروفات",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            ChartSkeleton()
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Time Period Selector Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            "DAY" to "يومي",
                            "WEEK" to "أسبوعي",
                            "MONTH" to "شهري",
                            "YEAR" to "سنوي"
                        ).forEach { (p, label) ->
                            val isSelected = chartPeriod == p
                            val bg = if (isSelected) primary else MaterialTheme.colorScheme.background
                            val tc = if (isSelected) Color.White else TextGray
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .clickable { onPeriodSelected(p) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tc,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Line Chart Canvas
                    if (expenseTrendData.isEmpty() || expenseTrendData.all { it.amount == 0.0 }) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد مصروفات متاحة في هذه الفترة", color = TextGray, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        val maxAmount = expenseTrendData.maxOfOrNull { it.amount }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            val w = size.width
                            val h = size.height - 20.dp.toPx() // Reserve 20dp for labels
                            val stepX = w / (expenseTrendData.size - 1).coerceAtLeast(1)

                            val path = Path()
                            expenseTrendData.forEachIndexed { i, pt ->
                                val x = i * stepX
                                val y = h - ((pt.amount.toFloat() / maxAmount) * h * 0.8f) // leave top padding
                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    val prevX = (i - 1) * stepX
                                    val prevY = h - ((expenseTrendData[i - 1].amount.toFloat() / maxAmount) * h * 0.8f)
                                    path.cubicTo(
                                        prevX + (x - prevX) / 2f, prevY,
                                        prevX + (x - prevX) / 2f, y,
                                        x, y
                                    )
                                }
                            }

                            // Draw gradient fill
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(primary.copy(alpha = 0.2f), Color.Transparent),
                                    startY = 0f,
                                    endY = h
                                ),
                                style = Fill
                            )

                            // Draw stroke
                            drawPath(
                                path = path,
                                color = primary,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )

                            // Draw points & labels
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            expenseTrendData.forEachIndexed { i, pt ->
                                val x = i * stepX
                                val y = h - ((pt.amount.toFloat() / maxAmount) * h * 0.8f)

                                // Find the highest point to highlight
                                val isMax = pt.amount == expenseTrendData.maxOf { it.amount } && pt.amount > 0.0

                                if (isMax) {
                                    drawCircle(color = Color.Black, radius = 7.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                    drawCircle(color = primary, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                } else {
                                    drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                    drawCircle(color = primary, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                }

                                if (expenseTrendData.size <= 7 || i % (expenseTrendData.size / 7) == 0 || i == expenseTrendData.lastIndex) {
                                    drawContext.canvas.nativeCanvas.drawText(pt.label, x, h + 15.dp.toPx(), textPaint)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
