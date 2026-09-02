package com.qdash.presentation.debt.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtDetailsContent(
    debt: Debt,
    accounts: List<Account>,
    selectedDebtPayments: List<DebtPayment>,
    onPayClick: () -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit,
    onForgiveClick: (Debt) -> Unit,
    onCloseDebt: (Long) -> Unit,
    onCancelPaymentClick: (DebtPayment) -> Unit = {}
) {
    val progress = if (debt.totalAmount > 0) ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).toFloat() else 1f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // SUMMARY DETAILS CARD
        item {
            DebtSummaryCard(debt = debt, accounts = accounts, progress = progress)
        }

        // PAY TRANSACTION ACTION TRIGGER
        if (!debt.isClosed) {
            item {
                val payButtonText = if (debt.direction == DebtDirection.OWED_TO_ME) "تحصيل دفعة استرداد" else "تسديد"
                AppButton(
                    onClick = onPayClick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.SUCCESS,
                    shape = ShapeTokens.Lg,
                    leadingIcon = { Icon(Icons.Default.CreditCard, null) }
                ) {
                    Text(
                        text = payButtonText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // STRATEGY ACTIONS (CLOSE, FORGIVE, EDIT, DELETE)
        item {
            DebtDetailActionsCard(
                debt = debt,
                onCloseDebt = onCloseDebt,
                onForgiveClick = onForgiveClick,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick
            )
        }

        // PAYMENT HISTORY LIST HEADER
        item {
            val historyTitle = if (debt.direction == DebtDirection.OWED_TO_ME) "سجل دفعات الاسترداد المالي" else "سجل تسوية السداد المالي"
            Text(historyTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (selectedDebtPayments.isEmpty()) {
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Lg,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        val emptyMsg = if (debt.direction == DebtDirection.OWED_TO_ME) "لم تسجل أي دفعات استرداد لهذه السلفة بعد." else "لم تسجل أي دفعات سداد لهذا الالتزام بعد."
                        Text(emptyMsg, textAlign = TextAlign.Center, color = TextGray)
                    }
                }
            }
        } else {
            items(selectedDebtPayments, key = { it.id }) { payment ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Lg,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(IncomeGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                val dateStr = sdf.format(Date(payment.paymentDate))
                                val defaultNote = if (debt.direction == DebtDirection.OWED_TO_ME) {
                                    "استرداد سلفة"
                                } else if (debt is RegularDebt) {
                                    "تسديد سداد دين"
                                } else {
                                    "تسديد قسط دين منتظم"
                                }
                                Text(payment.note ?: defaultNote, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextGray)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sign = if (debt.direction == DebtDirection.OWED_TO_ME) "+" else "-"
                            Text(
                                text = "$sign ${payment.amount.toInt()} د.ج",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            // Undo payment button: allowed for Regular debts, or closed debts
                            if (debt is RegularDebt || debt.isClosed) {
                                IconButton(
                                    onClick = { onCancelPaymentClick(payment) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo,
                                        contentDescription = "إلغاء الدفعة",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(20.dp)
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

@Composable
private fun DebtSummaryCard(debt: Debt, accounts: List<Account>, progress: Float) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (debt.isClosed) IncomeGreen else if (debt.direction == DebtDirection.OWED_TO_ME) IncomeGreen else ExpenseRed,
                        strokeWidth = 6.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
                Column {
                    val remainingLabel = if (debt.direction == DebtDirection.OWED_TO_ME) "المبلغ المتبقي للاسترداد" else "المبلغ المتبقي حالياً للسداد"
                    Text(remainingLabel, style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatterUtils.formatCurrency(debt.remainingAmount),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (debt.isClosed) IncomeGreen else if (debt.direction == DebtDirection.OWED_TO_ME) IncomeGreen else ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Decoupled metadata row based on subclass properties
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val metaItems = remember(debt, onSurfaceColor) {
                val creditorLabel = if (debt.direction == DebtDirection.OWED_TO_ME) "المدين" else "الجهة"
                val list = mutableListOf(
                    Triple("الأصل", FormatterUtils.formatCurrency(debt.totalAmount), onSurfaceColor),
                    Triple(creditorLabel, debt.creditorName, onSurfaceColor)
                )
                if (debt.direction == DebtDirection.OWED_TO_ME) {
                    list.add(Triple("النوع", "سلفة للغير", IncomeGreen))
                } else {
                    when (debt) {
                        is RegularDebt -> {
                            list.add(Triple("النوع", "دين مرن", SavingsAmber))
                        }
                        is InstallmentDebt -> {
                            list.add(Triple("الفائدة", "${debt.interestRate}%", ExpenseRed))
                            list.add(Triple("القسط الأدنى", "${debt.minimumPayment.toInt()} د.ج", onSurfaceColor))
                        }
                    }
                }
                list
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                metaItems.forEach { (label, value, valueColor) ->
                    AppCard(
                        modifier = Modifier.weight(1f),
                        variant = CardVariant.FLAT,
                        shape = ShapeTokens.Lg,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = TextGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = valueColor, maxLines = 1)
                        }
                    }
                }
            }

            if (debt.dueDate != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val isPastDue = debt.dueDate!! < System.currentTimeMillis() && !debt.isClosed
                val dateColor = if (debt.direction == DebtDirection.OWED_TO_ME && !isPastDue) IncomeGreen else ExpenseRed
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Event, null, tint = dateColor, modifier = Modifier.size(16.dp))
                    val label = if (debt.direction == DebtDirection.OWED_TO_ME) "تاريخ الاسترداد المتوقع" else if (debt is RegularDebt) "تاريخ الاستحقاق النهائى" else "تاريخ القسط القادم"
                    Text("$label: ${FormatterUtils.formatDate(debt.dueDate!!)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = dateColor)
                }
            }

            debt.linkedAccountId?.let { accId ->
                val accName = accounts.find { it.id == accId }?.name
                if (accName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = TextGray, modifier = Modifier.size(16.dp))
                        val accLabel = if (debt.direction == DebtDirection.OWED_TO_ME) "المحفظة الأصلية" else "الحساب المرتبط"
                        Text("$accLabel: $accName", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                }
            }

            if (!debt.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                AppCard(modifier = Modifier.fillMaxWidth(), variant = CardVariant.FLAT, shape = ShapeTokens.Lg, backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Notes, null, tint = TextGray, modifier = Modifier.size(16.dp))
                        Text(debt.notes!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtDetailActionsCard(
    debt: Debt,
    onCloseDebt: (Long) -> Unit,
    onForgiveClick: (Debt) -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!debt.isClosed) {
                AppButton(
                    onClick = { onCloseDebt(debt.id) },
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.SUCCESS,
                    leadingIcon = { Icon(Icons.Default.CheckCircle, "إغلاق", modifier = Modifier.size(16.dp)) }
                ) {
                    Text("إغلاق", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                val forgiveLabel = if (debt.direction == DebtDirection.OWED_TO_ME) "مسامحة" else "إعفاء"
                AppButton(
                    onClick = { onForgiveClick(debt) },
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.SUCCESS,
                    leadingIcon = { Icon(Icons.Default.CardGiftcard, forgiveLabel, modifier = Modifier.size(16.dp)) }
                ) {
                    Text(forgiveLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            AppButton(
                onClick = { onEditClick(debt) },
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY,
                leadingIcon = { Icon(Icons.Default.Edit, "تعديل", modifier = Modifier.size(16.dp)) }
            ) {
                Text("تعديل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
            AppButton(
                onClick = { onDeleteClick(debt) },
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.DANGER,
                leadingIcon = { Icon(Icons.Default.Delete, "مسح", modifier = Modifier.size(16.dp)) }
            ) {
                Text("مسح", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
