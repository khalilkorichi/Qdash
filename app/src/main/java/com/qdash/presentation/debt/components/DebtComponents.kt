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
import com.qdash.domain.model.*
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
    onAddDebtClick: (DebtType) -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit,
    onForgiveClick: (Debt) -> Unit,
    onPaymentsHistoryClick: (Debt) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    var selectedTab by remember { mutableStateOf(0) } // 0: Regular, 1: Installment
    val tabs = listOf("الديون العادية", "الديون المقسطة")

    val filteredDebts = remember(debts, selectedTab) {
        if (selectedTab == 0) {
            debts.filterIsInstance<RegularDebt>()
        } else {
            debts.filterIsInstance<InstallmentDebt>()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // SUMMARY METRICS HERO CARD
        item {
            DebtsSummaryHeroCard(debts = filteredDebts)
        }

        // TABS FOR SEPARATION
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }
        }

        // STRATEGY COMPILER DISPLAY (Only for Installments)
        if (selectedTab == 1 && strategyResults.isNotEmpty() && filteredDebts.any { !it.isClosed }) {
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
                Text(
                    text = if (selectedTab == 0) "شروط وجداول الديون العادية" else "شروط وجداول القروض والأقساط",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AppButton(
                    onClick = {
                        val type = if (selectedTab == 0) DebtType.REGULAR else DebtType.INSTALLMENT
                        onAddDebtClick(type)
                    },
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.DANGER,
                    leadingIcon = { Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text(if (selectedTab == 0) "سلفة جديدة" else "قرض جديد", fontWeight = FontWeight.Bold)
                }
            }
        }

        // DEBT CARDS
        if (filteredDebts.isEmpty()) {
            item {
                EmptyStateView(
                    title = if (selectedTab == 0) "لا توجد ديون عادية" else "لا توجد ديون مقسطة",
                    description = if (selectedTab == 0) 
                        "لا توجد التزامات ديون شخصية أو سلف بسيطة مسجلة حالياً." 
                    else 
                        "لا توجد قروض بنكية أو خطط تقسيط تجارية مسجلة حالياً."
                )
            }
        } else {
            val orderedDebts = if (selectedTab == 1) {
                if (selectedStrategy == "snowball") {
                    filteredDebts.sortedBy { it.remainingAmount }
                } else {
                    filteredDebts.sortedByDescending { (it as InstallmentDebt).interestRate }
                }
            } else {
                filteredDebts.sortedBy { it.remainingAmount }
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
fun DebtsSummaryHeroCard(debts: List<Debt>) {
    val totalRemaining = debts.sumOf { it.remainingAmount }
    val totalPaid = debts.sumOf { it.totalAmount - it.remainingAmount }
    val progress = if (debts.isEmpty()) 1f else (totalPaid / debts.sumOf { it.totalAmount }).toFloat()

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "إجمالي الالتزامات المالية القائمة",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = FormatterUtils.formatCurrency(totalRemaining),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "نسبة الإنجاز في السداد: ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
                Text(
                    text = "تم سداد: ${FormatterUtils.formatCurrency(totalPaid)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun DebtsStrategyHeader(
    selectedStrategy: String,
    primaryColor: Color,
    onStrategyChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("خطط واستراتيجيات تصفية القروض", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("snowball" to "كرة الثلج", "avalanche" to "الانهيار").forEach { (id, label) ->
                val isSelected = selectedStrategy == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onStrategyChange(id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
fun DebtsStrategyResultCard(
    strategyResults: List<DebtStrategyResult>,
    selectedStrategy: String,
    primaryColor: Color
) {
    val result = strategyResults.find { 
        it.strategyName.contains(if (selectedStrategy == "snowball") "كرة الثلج" else "الانهيار")
    } ?: strategyResults.firstOrNull()

    result?.let {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            variant = CardVariant.OUTLINED,
            shape = ShapeTokens.Lg,
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(it.strategyName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = primaryColor)
                    Box(modifier = Modifier.background(primaryColor.copy(alpha = 0.12f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        val roundedMonths = String.format(Locale.getDefault(), "%.1f", it.durationInMonths)
                        Text("${roundedMonths} شهر", fontSize = 10.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "الدفعات الشهرية الإجمالية المطلوبة: ${it.monthlyPaymentNeeded.toInt()} د.ج / شهر",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                it.paymentScheduleSummary?.let { summary ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(summary, style = MaterialTheme.typography.labelSmall, color = TextGray)
                }
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
    val isUrgent = debt is InstallmentDebt && debt.priority == 1
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
                            val typeLabel = if (debt is RegularDebt) "عادي" else "مقسط"
                            val typeColor = if (debt is RegularDebt) SavingsAmber else ExpenseRed
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
                    Text(if (debt is RegularDebt) "النوع" else "القسط / الفائدة", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    when (debt) {
                        is RegularDebt -> {
                            Text("سلفة", fontWeight = FontWeight.Bold, color = SavingsAmber)
                        }
                        is InstallmentDebt -> {
                            val interestStr = "${debt.interestRate}%"
                            Text("${debt.minimumPayment.toInt()} د.ج / $interestStr", fontWeight = FontWeight.Bold)
                        }
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
