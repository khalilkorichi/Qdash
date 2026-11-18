package com.qdash.presentation.analytics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.CardAiContext
import com.qdash.domain.model.SavingGoal
import com.qdash.presentation.analytics.AnalyticsUiState
import com.qdash.presentation.analytics.EmergencyFundCard
import com.qdash.presentation.analytics.SalaryCycleCard
import com.qdash.presentation.analytics.WeekendWeekdayCard
import com.qdash.presentation.components.UnifiedCategoryIcon
import com.qdash.presentation.components.safeParseColor
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.ui.theme.*
import kotlin.math.roundToInt

/**
 * Content for Analytics Tab 2 (التحليلات — Insights).
 */
@Composable
fun AnalyticsInsightsTabContent(
    uiState: AnalyticsUiState,
    periodStart: Long,
    periodEnd: Long,
    onAiChatClick: (CardAiContext) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
            uiState.isDatabaseEmpty -> {
                EmptyStateView(
                    title = "التحليلات الذكية هادئة حالياً",
                    description = "أضف بعض المعاملات والمصاريف اليومية لتوليد تحليلات ذكية حول عادات الإنفاق وصندوق الطوارئ الخاص بك.",
                    icon = Icons.Default.PieChart,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                EmergencyFundCard(
                    uiState = uiState,
                    onAiChatClick = onAiChatClick,
                    periodStart = periodStart,
                    periodEnd = periodEnd
                )
                if (uiState.selectedPeriod == "MONTH" && uiState.spendingsByCategory.isNotEmpty()) {
                    SalaryCycleCard(
                        uiState = uiState,
                        onAiChatClick = onAiChatClick,
                        periodStart = periodStart,
                        periodEnd = periodEnd
                    )
                }
                WeekendWeekdayCard(
                    uiState = uiState,
                    onAiChatClick = onAiChatClick,
                    periodStart = periodStart,
                    periodEnd = periodEnd
                )
                if (uiState.spendingsByCategory.isEmpty()) {
                    AppCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        variant = CardVariant.SOLID,
                        shape = ShapeTokens.Xxl,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Text(
                                text = "لا توجد مصاريف مسجلة في هذه الفترة الزمنية لتوليد تحليلات الراتب ونهاية الأسبوع.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "جرب اختيار فترة زمنية أخرى أو إضافة معاملات إنفاق جديدة.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Content for Analytics Tab 3 (الادخار — Real Data-Driven Savings Analytics).
 */
@Composable
fun AnalyticsSavingsTabContent(
    uiState: AnalyticsUiState,
    onManageSavingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
            uiState.isDatabaseEmpty && uiState.savingGoals.isEmpty() -> {
                EmptyStateView(
                    title = "لا توجد بيانات ادخار بعد",
                    description = "أنشئ هدف ادخار جديداً أو سجل دخلك ومصاريفك لمتابعة معدل الادخار الفعلي وتقدم أهدافك المالية.",
                    icon = Icons.Default.Savings,
                    actionLabel = "إدارة أهداف الادخار",
                    onActionClick = onManageSavingsClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                RealSavingsAnalyticsSection(
                    uiState = uiState,
                    onManageSavingsClick = onManageSavingsClick
                )
            }
        }
    }
}

@Composable
private fun RealSavingsAnalyticsSection(
    uiState: AnalyticsUiState,
    onManageSavingsClick: () -> Unit
) {
    val activeGoals = remember(uiState.savingGoals) { uiState.savingGoals.filter { !it.isArchived } }
    val totalGoalSaved = remember(activeGoals) { activeGoals.sumOf { it.currentAmount } }
    val totalGoalTarget = remember(activeGoals) { activeGoals.sumOf { it.targetAmount } }
    val goalsProgress = remember(totalGoalSaved, totalGoalTarget) {
        if (totalGoalTarget > 0.0) (totalGoalSaved / totalGoalTarget).toFloat().coerceIn(0f, 1f) else 0f
    }
    val completedGoalsCount = remember(activeGoals) { activeGoals.count { it.isCompleted || (it.targetAmount > 0 && it.currentAmount >= it.targetAmount) } }
    val periodNetSavings = uiState.periodIncomeSum - uiState.periodExpensesSum

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Total Saved vs Target Hero Card
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            variant = CardVariant.SOLID,
            shape = ShapeTokens.Xl,
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إجمالي التقدم في أهداف الادخار",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${FormatterUtils.convertNumerals(activeGoals.size.toString())} هدف نشط • ${FormatterUtils.convertNumerals(completedGoalsCount.toString())} مكتمل",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AppButton(
                        onClick = onManageSavingsClick,
                        variant = ButtonVariant.LIGHT,
                        intent = ButtonIntent.PRIMARY,
                        shape = ShapeTokens.Md
                    ) {
                        Text("إدارة الأهداف", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("المبلغ المدخر حالياً", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = FormatterUtils.formatCurrency(totalGoalSaved),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = IncomeGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("الهدف الإجمالي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = FormatterUtils.formatCurrency(totalGoalTarget),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { goalsProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = IncomeGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    text = "نسبة الإنجاز الكلية: ${FormatterUtils.convertNumerals((goalsProgress * 100).roundToInt().toString())}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 2. Period Net Savings Rate & Emergency Runway Dual Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(
                modifier = Modifier.weight(1f),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xl,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "صافي فائض الفترة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val netColor = if (periodNetSavings >= 0) IncomeGreen else ExpenseRed
                    val netPrefix = if (periodNetSavings > 0) "+" else ""
                    Text(
                        text = "$netPrefix${FormatterUtils.formatCurrency(periodNetSavings)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = netColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val rateSub = if (uiState.hasIncomeInPeriod) {
                        "معدل التوفير: ${(uiState.savingsRate * 100).roundToInt()}%"
                    } else {
                        "لا يوجد دخل مسجل في الفترة"
                    }
                    Text(
                        text = rateSub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AppCard(
                modifier = Modifier.weight(1f),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xl,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                val runwayColor = when (uiState.emergencyFundStatus) {
                    "CRITICAL" -> ExpenseRed
                    "ACCEPTABLE" -> SavingsAmber
                    else -> IncomeGreen
                }
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "تغطية صندوق الطوارئ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatterUtils.convertNumerals("${String.format(java.util.Locale.US, "%.1f", uiState.emergencyFundRunwayMonths)} شهر"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = runwayColor
                    )
                    Text(
                        text = "السيولة الإجمالية: ${FormatterUtils.formatCurrency(uiState.totalSavingsAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. Active SavingGoal Progress Cards
        if (activeGoals.isEmpty()) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xl,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = SavingsAmber,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "لم تضف أهداف ادخار محددة بعد",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "أنشئ هدفاً مثل (صندوق الطوارئ، شراء سيارة، أو السفر) لمتابعة تقدمه هنا بالبيانات الحقيقية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    AppButton(
                        onClick = onManageSavingsClick,
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.PRIMARY
                    ) {
                        Text("إنشاء هدف ادخار الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Text(
                text = "تفاصيل أهدافك الادخارية",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            activeGoals.forEach { goal ->
                SavingGoalAnalyticsRow(
                    goal = goal,
                    onClick = onManageSavingsClick
                )
            }
        }
    }
}

@Composable
private fun SavingGoalAnalyticsRow(
    goal: SavingGoal,
    onClick: () -> Unit
) {
    val accent = safeParseColor(goal.color, IncomeGreen)
    val progress = if (goal.targetAmount > 0.0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        UnifiedCategoryIcon(
                            iconKey = goal.icon,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sub = when {
                            goal.isCompleted || remaining <= 0.0 -> "مكتمل بنجاح"
                            goal.isPaused -> "متوقف مؤقتاً • متبقي ${FormatterUtils.formatCurrency(remaining)}"
                            else -> "متبقي: ${FormatterUtils.formatCurrency(remaining)}"
                        }
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (goal.isPaused) SavingsAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${FormatterUtils.convertNumerals((progress * 100).roundToInt().toString())}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المدخر: ${FormatterUtils.formatCurrency(goal.currentAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "الهدف: ${FormatterUtils.formatCurrency(goal.targetAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
