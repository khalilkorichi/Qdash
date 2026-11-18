package com.qdash.presentation.debt.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.*
import com.qdash.presentation.navigation.LocalBottomBarPadding
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun DebtDetailsContent(
    debt: Debt,
    accounts: List<Account>,
    selectedDebtPayments: List<DebtPayment>,
    onPayClick: () -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit,
    onForgiveClick: (Debt) -> Unit,
    onPaymentsHistoryClick: (Debt) -> Unit = {},
    onCancelPaymentClick: (DebtPayment) -> Unit = {}
) {
    val bottomPadding = LocalBottomBarPadding.current
    val paidAmount = (debt.totalAmount - debt.remainingAmount).coerceAtLeast(0.0)
    val progress = if (debt.totalAmount > 0.0) (paidAmount / debt.totalAmount).toFloat().coerceIn(0f, 1f) else 1f
    val isLent = debt.direction == DebtDirection.OWED_TO_ME

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 24.dp)
    ) {
        // 1. SUMMARY DETAILS CARD
        item {
            DebtSummaryCard(debt = debt, accounts = accounts, progress = progress)
        }

        // 2. PRIMARY PROMINENT CTA (Pay / Collect)
        if (!debt.isClosed) {
            item {
                val payButtonText = if (isLent) "تحصيل سلفة" else "تسديد دفعة"
                AppButton(
                    onClick = onPayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    variant = ButtonVariant.SOLID,
                    intent = if (isLent) ButtonIntent.SUCCESS else ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg,
                    leadingIcon = {
                        Icon(
                            imageVector = if (isLent) Icons.Default.Payments else Icons.Default.CreditCard,
                            contentDescription = null
                        )
                    }
                ) {
                    Text(
                        text = payButtonText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // 3. CLEAN SECONDARY ACTION ROW (NO DUPLICATE "إغلاق" BUTTON!)
        item {
            DebtDetailActionsCard(
                debt = debt,
                paymentsCount = selectedDebtPayments.size,
                onEditClick = onEditClick,
                onPaymentsHistoryClick = onPaymentsHistoryClick,
                onForgiveClick = onForgiveClick,
                onDeleteClick = onDeleteClick
            )
        }

        // 4. PAYMENT TIMELINE HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val historyTitle = if (isLent) "سجل دفعات التحصيل" else "سجل دفعات السداد"
                Text(
                    text = historyTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (selectedDebtPayments.isNotEmpty()) {
                    Text(
                        text = "${FormatterUtils.convertNumerals(selectedDebtPayments.size.toString())} دفعة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5. PAYMENT ITEMS (WITH UNDO ENABLED FOR BOTH REGULAR & INSTALLMENT DEBTS)
        if (selectedDebtPayments.isEmpty()) {
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Lg,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val emptyMsg = if (isLent) {
                            "لم تُسجّل أي دفعات تحصيل لهذه السلفة حتى الآن."
                        } else {
                            "لم تُسجّل أي دفعات سداد لهذا الالتزام حتى الآن."
                        }
                        Text(
                            text = emptyMsg,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(selectedDebtPayments, key = { it.id }) { payment ->
                val accountName = accounts.find { it.id == payment.accountId }?.name
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val sdf = remember { SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault()) }
                                val dateStr = FormatterUtils.convertNumerals(sdf.format(Date(payment.paymentDate)))
                                val defaultNote = when {
                                    isLent -> "تحصيل دفعة سلفة"
                                    debt is RegularDebt -> "دفعة سداد مرنة"
                                    payment.paymentType == DebtPaymentType.EXTRA -> "سداد مبكر / إضافي"
                                    else -> "تسديد قسط مجدول"
                                }
                                Text(
                                    text = payment.note?.takeIf { it.isNotBlank() } ?: defaultNote,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val metaSub = if (accountName != null) "$dateStr • $accountName" else dateStr
                                Text(
                                    text = metaSub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val sign = if (isLent) "+" else "-"
                            Text(
                                text = "$sign ${FormatterUtils.formatCurrency(payment.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLent) IncomeGreen else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { onCancelPaymentClick(payment) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "التراجع عن الدفعة",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
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
    val isLent = debt.direction == DebtDirection.OWED_TO_ME
    val paidAmount = (debt.totalAmount - debt.remainingAmount).coerceAtLeast(0.0)
    val ringColor = when {
        debt.isClosed -> IncomeGreen
        isLent -> IncomeGreen
        else -> MaterialTheme.colorScheme.primary
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = ringColor,
                        strokeWidth = 7.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${FormatterUtils.convertNumerals((progress * 100).roundToInt().toString())}%",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    val remainingLabel = if (isLent) "المتبقي للتحصيل" else "المتبقي للسداد"
                    Text(
                        text = remainingLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatterUtils.formatCurrency(debt.remainingAmount),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (debt.isClosed || isLent) IncomeGreen else ExpenseRed
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = (if (isLent) "تم تحصيل: " else "تم سداد: ") + FormatterUtils.formatCurrency(paidAmount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val metaItems = remember(debt, onSurfaceColor) {
                val creditorLabel = if (isLent) "المدين" else "الدائن / الجهة"
                val list = mutableListOf(
                    Triple("المبلغ الأصلي", FormatterUtils.formatCurrency(debt.totalAmount), onSurfaceColor),
                    Triple(creditorLabel, debt.creditorName, onSurfaceColor)
                )
                if (isLent) {
                    list.add(Triple("التصنيف", "سلفة للغير", IncomeGreen))
                } else {
                    when (debt) {
                        is RegularDebt -> list.add(Triple("التصنيف", "دين مرن", SavingsAmber))
                        is InstallmentDebt -> {
                            list.add(Triple("القسط الشهري", FormatterUtils.formatCurrency(debt.minimumPayment), onSurfaceColor))
                            if (debt.interestRate > 0.0) {
                                list.add(Triple("نسبة الفائدة", "${debt.interestRate}%", ExpenseRed))
                            }
                        }
                    }
                }
                list
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                metaItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (label, value, valueColor) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(ShapeTokens.Md)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = valueColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (debt.dueDate != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val isPastDue = debt.dueDate!! < System.currentTimeMillis() && !debt.isClosed
                val dateColor = when {
                    isPastDue -> ExpenseRed
                    isLent -> IncomeGreen
                    else -> MaterialTheme.colorScheme.primary
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = dateColor, modifier = Modifier.size(16.dp))
                    val label = if (isLent) "تاريخ الاسترداد المتوقع" else if (debt is RegularDebt) "تاريخ الاستحقاق النهائي" else "تاريخ القسط القادم"
                    Text(
                        text = "$label: ${FormatterUtils.formatDate(debt.dueDate!!)}" + if (isPastDue) " (متأخر)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = dateColor
                    )
                }
            }

            debt.linkedAccountId?.let { accId ->
                val accName = accounts.find { it.id == accId }?.name
                if (accName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        val accLabel = if (isLent) "المحفظة المرتبطة" else "حساب السداد الافتراضي"
                        Text(
                            text = "$accLabel: $accName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!debt.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeTokens.Md)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = debt.notes!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtDetailActionsCard(
    debt: Debt,
    paymentsCount: Int,
    onEditClick: (Debt) -> Unit,
    onPaymentsHistoryClick: (Debt) -> Unit,
    onForgiveClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit
) {
    val isLent = debt.direction == DebtDirection.OWED_TO_ME
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Edit
            AppButton(
                onClick = { onEditClick(debt) },
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY,
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
            ) {
                Text("تعديل", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            // 2. Payment History Sheet
            AppButton(
                onClick = { onPaymentsHistoryClick(debt) },
                modifier = Modifier.weight(1.25f),
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY,
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
            ) {
                val countSuffix = if (paymentsCount > 0) " (${FormatterUtils.convertNumerals(paymentsCount.toString())})" else ""
                Text("سجل الدفعات$countSuffix", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            // 3. Forgive / Waive (with Confirmation Dialog — only when active)
            if (!debt.isClosed) {
                val forgiveLabel = if (isLent) "مسامحة" else "إعفاء"
                AppButton(
                    onClick = { onForgiveClick(debt) },
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.SUCCESS,
                    leadingIcon = { Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp)) }
                ) {
                    Text(forgiveLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            // 4. Destructive Delete Icon Button (with Confirmation Dialog)
            Surface(
                onClick = { onDeleteClick(debt) },
                shape = ShapeTokens.Md,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
