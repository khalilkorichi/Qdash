package com.qdash.presentation.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.ui.theme.*

/**
 * Two-column summary card row: "Largest Expense" + "Savings Rate".
 * Extracted from AnalyticsScreen to keep it under the SIZE-002 line limit.
 */
@Composable
fun AnalyticsSummaryCards(
    largestExpenseName: String,
    largestExpense: Double,
    savingsRate: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Largest Expense card ──
        AppCard(
            modifier = Modifier.weight(1f),
            variant = CardVariant.SOLID,
            shape = ShapeTokens.Xl,
            backgroundColor = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(ExpenseRed.copy(alpha = 0.08f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.02f))
                            )
                        )
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(ExpenseRed, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("أعلى إنفاق مفرد", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = largestExpenseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatterUtils.formatCurrency(largestExpense),
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Savings Rate card ──
        AppCard(
            modifier = Modifier.weight(1f),
            variant = CardVariant.SOLID,
            shape = ShapeTokens.Xl,
            backgroundColor = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(IncomeGreen.copy(alpha = 0.08f), IncomeGreen.copy(alpha = 0.02f))
                            )
                        )
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    val savingsRatePercent = (savingsRate * 100).toInt()
                    val rateColor = if (savingsRatePercent >= 0) IncomeGreen else ExpenseRed
                    val rateText = if (savingsRatePercent >= 0) "+$savingsRatePercent%" else "$savingsRatePercent%"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(rateColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("معدل الادخار", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = rateText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = rateColor)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (msg, icon, iconColor) = when {
                            savingsRate >= 0.2f -> Triple("حالة مالية ممتازة ومستقرة", Icons.Default.TrendingUp, IncomeGreen)
                            savingsRate >= 0.1f -> Triple("حالة جيدة", Icons.Default.TrendingFlat, SavingsAmber)
                            else               -> Triple("تحتاج لتقليل نفقاتك", Icons.Default.TrendingDown, ExpenseRed)
                        }
                        Text(text = msg, style = MaterialTheme.typography.labelSmall, color = TextGray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
