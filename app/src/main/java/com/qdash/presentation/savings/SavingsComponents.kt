package com.qdash.presentation.savings

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.SavingGoal
import com.qdash.domain.model.SavingsContributionType
import com.qdash.presentation.components.safeParseColor
import com.qdash.presentation.navigation.LocalBottomBarPadding
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
    val bottomPadding = LocalBottomBarPadding.current
    val nonArchivedGoals = remember(uiState.goals) { uiState.goals.filter { !it.isArchived } }
    val archivedGoals = remember(uiState.goals) { uiState.goals.filter { it.isArchived } }
    val displayedGoals = if (uiState.showArchived) archivedGoals else nonArchivedGoals

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 24.dp)
    ) {
        // SAVINGS METRICS OVERVIEW CARD
        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xxl,
                backgroundColor = Color.Transparent
            ) {
                val totalSaved = nonArchivedGoals.sumOf { it.currentAmount }
                val totalTarget = nonArchivedGoals.sumOf { it.targetAmount }
                val completionPct = if (totalTarget > 0) (totalSaved / totalTarget * 100).toInt() else 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SavingsAmber, SavingsAmber.copy(alpha = 0.85f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "إجمالي مدخراتك في الأهداف النشطة",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FormatterUtils.formatCurrency(totalSaved),
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
                                Text("أهداف نشطة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Text("${nonArchivedGoals.count { !it.isCompleted }} أهداف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("أهداف مكتملة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Text("${nonArchivedGoals.count { it.isCompleted }} أهداف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("نسبة الإنجاز", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Text("$completionPct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        LinearProgressIndicator(
                            progress = { if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f },
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
        if (!uiState.showArchived && uiState.insights.isNotEmpty()) {
            item {
                Text(
                    text = "إضاءات وحوافز مالية ذكية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.insights.take(2).forEach { insight ->
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(TransferBlue.copy(alpha = 0.14f), CircleShape),
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
                                    text = FormatterUtils.convertNumerals(insight.text),
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
                Text(
                    text = if (uiState.showArchived) "الأهداف المؤرشفة (${archivedGoals.size})" else "أهدافك الادخارية الحالية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!uiState.showArchived) {
                    AppButton(
                        onClick = onCreateGoalClick,
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.PRIMARY,
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    ) {
                        Text("أضف هدفاً", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // GOALS LIST
        if (displayedGoals.isEmpty()) {
            item {
                EmptyStateView(
                    title = if (uiState.showArchived) "لا توجد أهداف مؤرشفة" else "لا توجد أهداف ادخارية نشطة بعد",
                    description = if (uiState.showArchived) {
                        "يمكنك أرشفة الأهداف القديمة للاحتفاظ بسجلك نظيفاً والرجوع إليها وقتما تشاء."
                    } else {
                        "إنشاء الأهداف الادخارية يعطيك دافعاً مالياً قوياً. ابدأ بإنشاء أول هدف ادخاري لك الآن!"
                    }
                )
            }
        } else {
            items(displayedGoals, key = { it.id }) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                val badgeColor = safeParseColor(goal.color, SavingsAmber)

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
                                modifier = Modifier.weight(1f),
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
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (goal.isPaused) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "موقّف مؤقتاً",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (goal.isArchived) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "مؤرشف",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppCircleIconButton(
                                    icon = Icons.Default.AddCircle,
                                    contentDescription = "إيداع مدخرات",
                                    tint = IncomeGreen,
                                    containerColor = IncomeGreen.copy(alpha = 0.12f),
                                    size = 34.dp,
                                    iconSize = 18.dp,
                                    onClick = { onAddContribution(goal) }
                                )
                                if (goal.currentAmount > 0.0) {
                                    AppCircleIconButton(
                                        icon = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "سحب مدخرات",
                                        tint = MaterialTheme.colorScheme.error,
                                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                        size = 34.dp,
                                        iconSize = 18.dp,
                                        onClick = { onWithdrawSavings(goal) }
                                    )
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
                                Text("المبلغ المدخّر", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FormatterUtils.formatCurrency(goal.currentAmount), fontWeight = FontWeight.Bold, color = badgeColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("المبلغ المستهدف", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FormatterUtils.formatCurrency(goal.targetAmount), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (goal.deadline != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "الموعد المستهدف: ${FormatterUtils.formatDate(goal.deadline)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
    onEdit: () -> Unit,
    onDeleteClick: () -> Unit = { viewModel.deleteGoal(goal.id) }
) {
    val bottomPadding = LocalBottomBarPadding.current
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val accentColor = safeParseColor(goal.color, SavingsAmber)
    val isPaused = goal.isPaused
    val isArchived = goal.isArchived

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 24.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الرصيد المدخر لهذا الهدف", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatterUtils.formatCurrency(goal.currentAmount),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                            if (goal.deadline != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الموعد المستهدف: ${FormatterUtils.formatDate(goal.deadline)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                            Text("المستهدف الكلي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FormatterUtils.formatCurrency(goal.targetAmount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("نوع الاستراتيجية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val stratText = when (goal.strategy.lowercase(Locale.US)) {
                                "monthly" -> "شهري منتظم"
                                "weekly" -> "أسبوعي دوري"
                                "leftover" -> "بواقي الحساب"
                                else -> "يدوي مرن"
                            }
                            Text(stratText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TransferBlue)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("نسبة الاكتمال", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = IncomeGreen)
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

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("البداية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("رُبع الطريق", fontSize = 10.sp, color = if (progress >= 0.25f) accentColor else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("النصف", fontSize = 10.sp, color = if (progress >= 0.5f) accentColor else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("المعظم", fontSize = 10.sp, color = if (progress >= 0.75f) accentColor else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("تم التحقيق!", fontSize = 10.sp, color = if (progress >= 1.0f) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // PREDICTIVE FORECAST CARD
        if (uiState.forecastText.isNotBlank()) {
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Xl,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingUp, null, tint = TransferBlue)
                        Column {
                            Text("توقعات وتيرة الادخار", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(uiState.forecastText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // QUICK DEPOSIT / WITHDRAW ACTION ROW
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
                    Text("إيداع الآن", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                AppButton(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.DANGER,
                    shape = ShapeTokens.Lg,
                    enabled = goal.currentAmount > 0.0,
                    leadingIcon = { Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("سحب مدخرات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // MANAGEMENT CONTROL ROW
        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surface
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
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(if (isPaused) "استئناف" else "تعليق مؤقت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    AppButton(
                        onClick = { viewModel.archiveGoal(goal.id) },
                        variant = ButtonVariant.LIGHT,
                        intent = ButtonIntent.PRIMARY,
                        leadingIcon = {
                            Icon(
                                imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(if (isArchived) "إلغاء الأرشفة" else "أرشفة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    AppButton(
                        onClick = onDeleteClick,
                        variant = ButtonVariant.LIGHT,
                        intent = ButtonIntent.DANGER,
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, "حذف", modifier = Modifier.size(18.dp)) }
                    ) {
                        Text("حذف الهدف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CHRONOLOGICAL LOGS & CONTRIBUTION HISTORY LIST
        item {
            Text(
                text = "السجل الزمني للحركات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.selectedGoalHistory.isEmpty()) {
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Lg,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لم يتم تسجيل أي إيداعات أو سحوبات بعد في هذا الهدف الادخاري.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.selectedGoalHistory, key = { it.id }) { contribution ->
                val isDeposit = contribution.type == SavingsContributionType.DEPOSIT
                val sign = if (isDeposit) "+" else "-"
                val amountColor = if (isDeposit) IncomeGreen else MaterialTheme.colorScheme.error
                val iconValue = if (isDeposit) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignTop
                val bgValue = amountColor.copy(alpha = 0.12f)
                val accountName = uiState.accounts.find { it.id == contribution.accountId }?.name

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Lg,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(bgValue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconValue, contentDescription = null, tint = amountColor, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val sdf = remember { SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault()) }
                                val dateStr = FormatterUtils.convertNumerals(sdf.format(Date(contribution.date)))
                                Text(
                                    text = contribution.note?.takeIf { it.isNotBlank() } ?: if (isDeposit) "إيداع في الحصالة" else "سحب من الحصالة",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sub = if (accountName != null) "$dateStr • $accountName" else dateStr
                                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = "$sign ${FormatterUtils.formatCurrency(contribution.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
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
