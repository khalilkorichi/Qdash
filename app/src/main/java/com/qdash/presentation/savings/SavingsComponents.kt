package com.qdash.presentation.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.domain.model.SavingGoal
import com.qdash.domain.model.SavingsContributionType
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SavingsDashboardContent(
    uiState: SavingsUiState,
    viewModel: SavingsViewModel,
    onSelectGoal: (SavingGoal) -> Unit,
    onAddContribution: (SavingGoal) -> Unit,
    onWithdrawSavings: (SavingGoal) -> Unit,
    onCreateGoalClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // SAVINGS METRICS OVERVIEW CARD
        item {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xxl,
                backgroundColor = Color.Transparent
            ) {
                val totalSaved = uiState.goals.sumOf { it.currentAmount }
                val totalTarget = uiState.goals.sumOf { it.targetAmount }
                val completionPct = if (totalTarget > 0) (totalSaved / totalTarget * 100).toInt() else 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(SavingsAmber, Color(0xFFD97706))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text("إجمالي مدخراتك بالمشروعات", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = com.qdash.core.utils.FormatterUtils.convertNumerals("${String.format(Locale.getDefault(), "%,d", totalSaved.toLong())} د.ج"),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("أهداف نشطة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("${uiState.goals.count { !it.isCompleted }} أهداف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("أهداف مكتملة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("${uiState.goals.count { it.isCompleted }} أهداف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("نسبة الإنجاز", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("$completionPct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        LinearProgressIndicator(
                            progress = { if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }

        // MOTIVATIONAL SMART SAVINGS INSIGHTS
        if (uiState.insights.isNotEmpty()) {
            item {
                Text("إضاءات وحوافز مالية ذكية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.insights.take(2).forEach { insight ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = CardVariant.SOLID,
                            shape = ShapeTokens.Lg,
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(TransferBlue.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (insight.isPositive) Icons.Default.ThumbUp else Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = TransferBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = com.qdash.core.utils.FormatterUtils.convertNumerals(insight.text),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION HEADER + NEW GOAL BUTTON
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("أهدافك الادخارية الحالية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AppButton(
                    onClick = onCreateGoalClick,
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.PRIMARY,
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("أضف هدفاً", fontWeight = FontWeight.Bold)
                }
            }
        }

        // GOALS LIST
        if (uiState.goals.isEmpty()) {
            item {
                EmptyStateView(
                    title = "لا توجد أهداف ادخارية نشطة بعد",
                    description = "إنشاء الأهداف الادخارية يعطيك دافعاً مالياً قوياً. ابدأ بإنشاء أول هدف ادخاري لك الآن!"
                )
            }
        } else {
            items(uiState.goals, key = { it.id }) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                val colorHex = goal.color
                val badgeColor = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(SavingsAmber)
                val isPaused = viewModel.isGoalPaused(goal.id)

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Xxl,
                    onClick = { onSelectGoal(goal) },
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        .size(10.dp)
                                        .background(badgeColor, CircleShape)
                                )
                                Text(
                                    text = goal.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isPaused) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("موقّف مؤقتاً", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { onAddContribution(goal) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.AddCircle, contentDescription = "تخصيص مدخرات", tint = IncomeGreen)
                                }
                                IconButton(onClick = { onWithdrawSavings(goal) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "سحب مدخرات", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("المبلغ المدخّر", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Text("${goal.currentAmount.toInt()} د.ج", fontWeight = FontWeight.Bold, color = badgeColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("المبلغ المستهدف", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Text("${goal.targetAmount.toInt()} د.ج", fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = badgeColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalDetailsContent(
    goal: SavingGoal,
    uiState: SavingsUiState,
    viewModel: SavingsViewModel,
    onAddDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(goal.color)) }.getOrDefault(SavingsAmber)
    val isPaused = viewModel.isGoalPaused(goal.id)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // DETAIL METRIC ROUND-UP CARD
        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xxl,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("وضع الادخار الحالي لهذا الهدف", style = MaterialTheme.typography.labelMedium, color = TextGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${goal.currentAmount.toInt()} د.ج", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(accentColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = accentColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("المستهدف الكلي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            Text("${goal.targetAmount.toInt()} د.ج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("نوع الاستراتيجية", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            val stratText = when (viewModel.getGoalStrategy(goal.id)) {
                                "monthly" -> "شهري منتظم"
                                "weekly" -> "أسبوعي دوري"
                                "leftover" -> "بواقي الحساب"
                                else -> "يدوي مرن"
                            }
                            Text(stratText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TransferBlue)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("نسبة الاكتمال", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Celebratory milestone ticks
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("البداية", fontSize = 10.sp, color = TextGray)
                        Text("رُبع الطريق", fontSize = 10.sp, color = if (progress >= 0.25f) accentColor else TextGray)
                        Text("النصف", fontSize = 10.sp, color = if (progress >= 0.5f) accentColor else TextGray)
                        Text("المعظم", fontSize = 10.sp, color = if (progress >= 0.75f) accentColor else TextGray)
                        Text("تم التحقيق!", fontSize = 10.sp, color = if (progress >= 1.0f) IncomeGreen else TextGray)
                    }
                }
            }
        }

        // AI PREDICTIVE FORECAST TIMELINE
        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xl,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingUp, null, tint = TransferBlue)
                    Column {
                        Text("توقعات ذكية لوتيرة الادخار", style = MaterialTheme.typography.labelSmall, color = TextGray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(uiState.forecastText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // QUICK TRANSACTION TOOLS & GOAL MANAGEMENT ACTION ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppButton(
                    onClick = onAddDeposit,
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.SUCCESS,
                    shape = ShapeTokens.Lg,
                    leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("إيداع الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                AppButton(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.DANGER,
                    shape = ShapeTokens.Lg,
                    leadingIcon = { Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("سحب مدخرات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ADVANCED STRATEGIC CONTROL ROW
        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppButton(
                        onClick = onEdit,
                        variant = ButtonVariant.LIGHT,
                        intent = ButtonIntent.PRIMARY,
                        leadingIcon = { Icon(Icons.Default.Edit, "تعديل", modifier = Modifier.size(18.dp)) }
                    ) {
                        Text("تعديل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    AppButton(
                        onClick = { viewModel.togglePauseGoal(goal.id) },
                        variant = ButtonVariant.LIGHT,
                        intent = ButtonIntent.PRIMARY,
                        leadingIcon = {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "تعليق تجميد",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(if (isPaused) "تنشيط" else "تعليق مؤقت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!goal.isCompleted) {
                        AppButton(
                            onClick = { viewModel.markGoalCompleted(goal.id) },
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.SUCCESS,
                            leadingIcon = { Icon(Icons.Default.CheckCircle, "حل", modifier = Modifier.size(18.dp)) }
                        ) {
                            Text("اكتمال الهدف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AppButton(
                        onClick = { viewModel.deleteGoal(goal.id) },
                        variant = ButtonVariant.LIGHT,
                        intent = ButtonIntent.DANGER,
                        leadingIcon = { Icon(Icons.Default.Delete, "حذف", modifier = Modifier.size(18.dp)) }
                    ) {
                        Text("حذف المشروع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CHRONOLOGICAL LOGS & CONTRIBUTION HISTORY LIST
        item {
            Text("سجل حركات الحصالة والTimeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (uiState.selectedGoalHistory.isEmpty()) {
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Lg,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لم يتم تسجيل أي إيداعات أو سحوبات بعد في هذا الهدف الادخاري.", textAlign = TextAlign.Center, color = TextGray)
                    }
                }
            }
        } else {
            items(uiState.selectedGoalHistory, key = { it.id }) { contribution ->
                val isDeposit = contribution.type == SavingsContributionType.DEPOSIT
                val sign = if (isDeposit) "+" else "-"
                val amountColor = if (isDeposit) IncomeGreen else Color.Red
                val iconValue = if (isDeposit) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignTop
                val bgValue = if (isDeposit) IncomeGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Lg,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                                    .background(bgValue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconValue, contentDescription = null, tint = amountColor, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                val dateStr = sdf.format(Date(contribution.date))
                                Text(
                                    text = contribution.note ?: if (isDeposit) "مساهمة ادخار" else "سحب مدخرات",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextGray)
                            }
                        }
                        Text(
                            text = "$sign ${contribution.amount.toInt()} د.ج",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsDashboardSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
        userScrollEnabled = false
    ) {
        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xxl,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .shimmerEffect(RoundedCornerShape(24.dp))
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        variant = CardVariant.SOLID,
                        shape = ShapeTokens.Lg,
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shimmerEffect(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(18.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(18.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
        }

        items(2) {
            AppCard(
                shape = ShapeTokens.Xl,
                variant = CardVariant.SOLID,
                backgroundColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect(RoundedCornerShape(18.dp))
                )
            }
        }
    }
}
