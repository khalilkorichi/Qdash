package com.example.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BudgetGoal
import com.example.domain.model.BudgetType
import com.example.domain.model.BudgetStatus
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SavingsAmber
import com.example.ui.theme.TextGray
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetGoalCard(
    budget: BudgetGoal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val progress = budget.usagePercent.toFloat().coerceIn(0.0f, 1.0f)
    val progressColor = when (budget.status) {
        BudgetStatus.SAFE -> IncomeGreen
        BudgetStatus.WARNING -> SavingsAmber
        BudgetStatus.CRITICAL -> ExpenseRed
        BudgetStatus.EXCEEDED -> ExpenseRed
    }

    val dzdAmountLimit = "${String.format("%,.0f", budget.amountLimit)} د.ج"
    val dzdAmountSpent = "${String.format("%,.0f", budget.spentAmount)} د.ج"
    val dzdRemaining = "${String.format("%,.0f", budget.remainingAmount.coerceAtLeast(0.0))} د.ج"

    val now = System.currentTimeMillis()
    val totalDuration = budget.endDate - now
    val daysLeft = if (totalDuration <= 0) {
        "انتهت المدة"
    } else {
        val days = TimeUnit.MILLISECONDS.toDays(totalDuration)
        if (days == 0L) "ينتهي اليوم" else "متبقي $days يوم"
    }

    val typeLabel = when (budget.budgetType) {
        BudgetType.CATEGORY -> "ميزانية فئة"
        BudgetType.CUSTOM -> "ميزانية مخصصة"
        BudgetType.GLOBAL -> "ميزانية عامة"
    }

    val customColor = try {
        Color(android.graphics.Color.parseColor(budget.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            customColor.copy(alpha = 0.25f) // Color-coded premium border matching the budget's custom category color!
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(customColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(budget.icon),
                            contentDescription = null,
                            tint = customColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = budget.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$typeLabel • $daysLeft",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }

                BudgetStatusChip(status = budget.status)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المنفق: $dzdAmountSpent",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "الحد: $dzdAmountLimit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Premium Custom double box indicator to support horizontal gradient fills
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(progressColor, progressColor.copy(alpha = 0.7f))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (budget.remainingAmount < 0) "المبلغ المتجاوز: ${String.format("%,.0f", -budget.remainingAmount)} د.ج" else "المتبقي: $dzdRemaining",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (budget.remainingAmount < 0) ExpenseRed else IncomeGreen
                )

                Text(
                    text = "${(budget.usagePercent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = progressColor
                )
            }
        }
    }
}
