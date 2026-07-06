package com.qdash.presentation.debt.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Debt
import com.qdash.domain.model.DebtStrategyResult
import com.qdash.domain.model.DebtType
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray
import java.util.Locale

@Composable
fun DebtsMainContent(
    debts: List<Debt>,
    insights: List<String>,
    strategyResults: List<DebtStrategyResult>,
    selectedStrategy: String,
    onStrategyChange: (String) -> Unit,
    onSelectDebt: (Debt) -> Unit,
    onPayClick: (Debt) -> Unit,
    onAddDebtClick: () -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit,
    onForgiveClick: (Debt) -> Unit,
    onPaymentsHistoryClick: (Debt) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // SUMMARY METRICS HERO CARD
        item {
            DebtsSummaryHeroCard(debts = debts)
        }

        // STRATEGY COMPILER DISPLAY
        if (strategyResults.isNotEmpty() && debts.any { !it.isClosed }) {
            item {
                DebtsStrategyHeader(
                    selectedStrategy = selectedStrategy,
                    primaryColor = primary,
                    onStrategyChange = onStrategyChange
                )
            }
            item {
                DebtsStrategyResultCard(
                    strategyResults = strategyResults,
                    selectedStrategy = selectedStrategy,
                    primaryColor = primary
                )
            }
        }

        // MOTIVATIONAL ALERTS & INSIGHTS
        if (insights.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    insights.take(1).forEach { insightText ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = CardVariant.SOLID,
                            shape = ShapeTokens.Lg,
                            backgroundColor = ExpenseRed.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                Text(FormatterUtils.convertNumerals(insightText), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // DEBTS LIST SECTION HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("شروط وجداول الديون القائمة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AppButton(
                    onClick = onAddDebtClick,
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.DANGER,
                    leadingIcon = { Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("تسجيل دين", fontWeight = FontWeight.Bold)
                }
            }
        }

        // DEBT CARDS
        if (debts.isEmpty()) {
            item {
                EmptyStateView(
                    title = "لا توجد ديون مسجلة",
                    description = "مبروك! لا توجد التزامات مالية قائمة مسجلة بالبرنامج. يمكنك إضافة أي التزام لمراقبته وتسويته بذكاء."
                )
            }
        } else {
            val orderedDebts = if (selectedStrategy == "snowball") {
                debts.sortedBy { it.remainingAmount }
            } else {
                debts.sortedByDescending { it.interestRate ?: 0.0 }
            }
            items(orderedDebts, key = { it.id }) { debt ->
                DebtListCard(
                    debt = debt,
                    onSelectDebt = onSelectDebt,
                    onPayClick = onPayClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    onForgiveClick = onForgiveClick,
                    onPaymentsHistoryClick = onPaymentsHistoryClick
                )
            }
        }
    }
}

@Composable
private fun DebtsSummaryHeroCard(debts: List<Debt>) {
    val totalRemaining = debts.filter { !it.isClosed }.sumOf { it.remainingAmount }
    val totalMin = debts.filter { !it.isClosed }.sumOf { it.minimumPayment }

    AppCard(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF141416), Color(0xFF381212))
                )
            )
        ) {
            Box(modifier = Modifier.size(150.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-30).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.03f)))
            Box(modifier = Modifier.size(100.dp).align(Alignment.BottomStart).offset(x = (-20).dp, y = 30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.04f)))
            Column(modifier = Modifier.padding(24.dp)) {
                Text("إجمالي التزامات الديون القائمة", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = FormatterUtils.convertNumerals("${String.format(Locale.getDefault(), "%,d", totalRemaining.toLong())} د.ج"),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("القسط الشهري الإجمالي", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${totalMin.toInt()} د.ج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("القضايا النشطة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${debts.count { !it.isClosed }} قضايا دين", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("الديون المغلقة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${debts.count { it.isClosed }} ذمة مبرأة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtsStrategyHeader(
    selectedStrategy: String,
    primaryColor: Color,
    onStrategyChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("مخطط تسوية الديون الاستراتيجي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(3.dp)
        ) {
            listOf("snowball" to "كرة الثلج", "avalanche" to "الانهيار").forEach { (id, label) ->
                val isSelected = selectedStrategy == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) primaryColor else Color.Transparent)
                        .clickable { onStrategyChange(id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtsStrategyResultCard(
    strategyResults: List<DebtStrategyResult>,
    selectedStrategy: String,
    primaryColor: Color
) {
    val currentStratResult = strategyResults.find {
        if (selectedStrategy == "snowball") it.strategyName.contains("الثلج")
        else it.strategyName.contains("الانهيار")
    } ?: strategyResults.firstOrNull() ?: DebtStrategyResult(
        strategyName = "جدولة افتراضية",
        durationInMonths = 0.0,
        estimatedDebtFreeDate = System.currentTimeMillis(),
        monthlyPaymentNeeded = 0.0,
        paymentScheduleSummary = "لا توجد تفاصيل حالياً."
    )

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(currentStratResult.strategyName, fontWeight = FontWeight.Bold, color = primaryColor)
                }
                Box(modifier = Modifier.background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("الحل الأمثل!", fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(currentStratResult.paymentScheduleSummary ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(FormatterUtils.convertNumerals("الفترة المقدرة للتصفية: ${String.format(Locale.getDefault(), "%.1f", currentStratResult.durationInMonths)} شهر"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
                Text("جاهز للتصفية الكاملة!", style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
        }
    }
}

@Composable
fun DebtListCard(
    debt: Debt,
    onSelectDebt: (Debt) -> Unit,
    onPayClick: (Debt) -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit,
    onForgiveClick: (Debt) -> Unit,
    onPaymentsHistoryClick: (Debt) -> Unit
) {
    val progress = if (debt.totalAmount > 0) ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).toFloat() else 1f
    val isUrgent = debt.priority == 1
    val isClosed = debt.isClosed
    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        onClick = { onSelectDebt(debt) },
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(
                            (if (isClosed) IncomeGreen else if (isUrgent) ExpenseRed else SavingsAmber).copy(alpha = 0.12f), CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isClosed) Icons.Default.CheckCircle else if (isUrgent) Icons.Default.NewReleases else Icons.Default.Event,
                            contentDescription = null,
                            tint = if (isClosed) IncomeGreen else if (isUrgent) ExpenseRed else SavingsAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(debt.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            val typeLabel = if (debt.debtType == DebtType.REGULAR) "عادي" else "مقسط"
                            val typeColor = if (debt.debtType == DebtType.REGULAR) SavingsAmber else ExpenseRed
                            Box(modifier = Modifier.background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(typeLabel, fontSize = 8.sp, color = typeColor, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(debt.creditorName, style = MaterialTheme.typography.labelSmall, color = TextGray)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isClosed) {
                        Box(modifier = Modifier.background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("كامل السداد", fontSize = 9.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        if (isUrgent) {
                            Box(modifier = Modifier.background(ExpenseRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("عاجل جداً", fontSize = 9.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ExpenseRed).clickable { onPayClick(debt) }.padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("تسديد دفعة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "خيارات الدين", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(if (isDark) ColorTokens.ElevatedSurfaceDark else MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(text = { Text("سجل دفعات السداد", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) }, leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp)) }, onClick = { menuExpanded = false; onPaymentsHistoryClick(debt) })
                            DropdownMenuItem(text = { Text("تعديل تفاصيل الدين", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) }, leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }, onClick = { menuExpanded = false; onEditClick(debt) })
                            if (!isClosed) {
                                DropdownMenuItem(text = { Text("إعفاء من المتبقي", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) }, leadingIcon = { Icon(Icons.Default.CardGiftcard, null, modifier = Modifier.size(18.dp)) }, onClick = { menuExpanded = false; onForgiveClick(debt) })
                            }
                            DropdownMenuItem(text = { Text("حذف السجل المالي", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth(), color = ExpenseRed) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = ExpenseRed, modifier = Modifier.size(18.dp)) }, onClick = { menuExpanded = false; onDeleteClick(debt) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("المبلغ المتبقي للحل", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = FormatterUtils.formatCurrency(debt.remainingAmount), fontWeight = FontWeight.Bold, color = if (isClosed) IncomeGreen else ExpenseRed)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (debt.debtType == DebtType.REGULAR) "النوع" else "القسط / الفائدة", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    if (debt.debtType == DebtType.REGULAR) {
                        Text("دين عادي", fontWeight = FontWeight.Bold, color = SavingsAmber)
                    } else {
                        val interestStr = if (debt.interestRate != null) "${debt.interestRate}%" else "مرن"
                        Text("${debt.minimumPayment.toInt()} د.ج / $interestStr", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (isClosed) IncomeGreen else ExpenseRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
