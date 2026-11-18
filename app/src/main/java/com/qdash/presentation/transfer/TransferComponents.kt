package com.qdash.presentation.transfer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.AccountType
import com.qdash.domain.model.TransferRecord
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import java.text.SimpleDateFormat
import java.util.*
import com.qdash.presentation.transactions.TransactionDateSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFormCard(
    accounts: List<Account>,
    isLoading: Boolean,
    error: String?,
    onExecuteTransfer: (
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        feeAmount: Double?,
        note: String?,
        date: Long,
        occurredAt: Long?,
        onSuccess: (Boolean) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    // Form inputs
    val selectableAccounts = remember(accounts) {
        accounts.filter { !it.isArchived }
    }
    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var feeAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var transferDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var occurredAt by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }

    // Dropdown visibility
    var showFromDropdown by remember { mutableStateOf(false) }
    var showToDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(selectableAccounts) {
        if (selectableAccounts.isNotEmpty()) {
            if (fromAccountId == null || selectableAccounts.none { it.id == fromAccountId }) {
                fromAccountId = selectableAccounts.first().id
            }
            if (toAccountId == null || selectableAccounts.none { it.id == toAccountId }) {
                toAccountId = selectableAccounts.firstOrNull { it.id != fromAccountId }?.id
                    ?: selectableAccounts.first().id
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجراء عملية تحويل داخلي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TransferBlue
                )
                if (selectableAccounts.size >= 2) {
                    Surface(
                        onClick = {
                            val temp = fromAccountId
                            fromAccountId = toAccountId
                            toAccountId = temp
                            localError = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = TransferBlue.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "تبديل الحسابات",
                                tint = TransferBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "عكس الحسابات",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TransferBlue
                            )
                        }
                    }
                }
            }

            // 1. HERO AMOUNT INPUT CONTAINER
            val fromAccount = selectableAccounts.find { it.id == fromAccountId }
            val sourceCurrencySymbol = FormatterUtils.currencySymbol(fromAccount?.currency ?: "DZD")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(TransferBlue.copy(alpha = 0.04f))
                    .border(1.dp, TransferBlue.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "مبلغ التحويل المطلوب إرساله",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                localError = null
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TransferBlue,
                                textAlign = TextAlign.Center
                            ),
                            placeholder = {
                                Text(
                                    "0.00",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextGray.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.width(180.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = sourceCurrencySymbol,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TransferBlue
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (fromAccount != null) {
                            "الرصيد المتاح بالمصدر: ${FormatterUtils.formatCurrency(fromAccount.balance, currencyCode = fromAccount.currency)}"
                        } else {
                            "الرصيد المتاح بالمصدر: ${FormatterUtils.formatCurrency(0.0)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )

                    // Quick Percentage Chips (25%, 50%, 100%)
                    if (fromAccount != null && fromAccount.balance > 0.0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                0.25 to "25%",
                                0.50 to "50%",
                                1.00 to "100%"
                            ).forEach { (pct, label) ->
                                Surface(
                                    onClick = {
                                        val calc = fromAccount.balance * pct
                                        amount = if (calc % 1.0 == 0.0) {
                                            calc.toLong().toString()
                                        } else {
                                            "%.2f".format(Locale.US, calc)
                                        }
                                        localError = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = TransferBlue.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, TransferBlue.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = TransferBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. CONNECTED INTERACTIVE FLOW BADGES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source account card selector
                Box(modifier = Modifier.weight(1f)) {
                    val tintFrom = when (fromAccount?.type) {
                        AccountType.BARIDIMOB -> Color(0xFF005CA9)
                        AccountType.CCP -> Color(0xFFF59E0B)
                        AccountType.CASH -> Color(0xFF22C55E)
                        AccountType.SAVINGS -> Color(0xFF3B82F6)
                        else -> TransferBlue
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(tintFrom.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                color = if (showFromDropdown) tintFrom else tintFrom.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { showFromDropdown = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(tintFrom.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (fromAccount?.type) {
                                    AccountType.BARIDIMOB -> Icons.Default.Smartphone
                                    AccountType.CCP -> Icons.Default.AccountBalance
                                    AccountType.CASH -> Icons.Default.Payments
                                    AccountType.SAVINGS -> Icons.Default.Savings
                                    else -> Icons.Default.CreditCard
                                },
                                contentDescription = null,
                                tint = tintFrom,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("من حساب (المصدر)", style = MaterialTheme.typography.labelSmall, color = TextGray, fontSize = 9.sp)
                            Text(
                                text = fromAccount?.name ?: "اختر الحساب",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            if (fromAccount != null) {
                                Text(
                                    text = FormatterUtils.formatCurrency(fromAccount.balance, currencyCode = fromAccount.currency),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tintFrom.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showFromDropdown,
                        onDismissRequest = { showFromDropdown = false }
                    ) {
                        selectableAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (${FormatterUtils.formatCurrency(account.balance, currencyCode = account.currency)})") },
                                onClick = {
                                    fromAccountId = account.id
                                    localError = null
                                    showFromDropdown = false
                                }
                            )
                        }
                    }
                }

                // Centered flow arrow connector (Clickable to swap accounts)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TransferBlue.copy(alpha = 0.1f))
                        .border(1.dp, TransferBlue.copy(alpha = 0.15f), CircleShape)
                        .clickable {
                            val temp = fromAccountId
                            fromAccountId = toAccountId
                            toAccountId = temp
                            localError = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "عكس الحسابات",
                        tint = TransferBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Destination account card selector
                Box(modifier = Modifier.weight(1f)) {
                    val toAccount = selectableAccounts.find { it.id == toAccountId }
                    val tintTo = when (toAccount?.type) {
                        AccountType.BARIDIMOB -> Color(0xFF005CA9)
                        AccountType.CCP -> Color(0xFFF59E0B)
                        AccountType.CASH -> Color(0xFF22C55E)
                        AccountType.SAVINGS -> Color(0xFF3B82F6)
                        else -> IncomeGreen
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(tintTo.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                color = if (showToDropdown) tintTo else tintTo.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { showToDropdown = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(tintTo.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (toAccount?.type) {
                                    AccountType.BARIDIMOB -> Icons.Default.Smartphone
                                    AccountType.CCP -> Icons.Default.AccountBalance
                                    AccountType.CASH -> Icons.Default.Payments
                                    AccountType.SAVINGS -> Icons.Default.Savings
                                    else -> Icons.Default.CreditCard
                                },
                                contentDescription = null,
                                tint = tintTo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إلى حساب (المستقبل)", style = MaterialTheme.typography.labelSmall, color = TextGray, fontSize = 9.sp)
                            Text(
                                text = toAccount?.name ?: "اختر الحساب",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            if (toAccount != null) {
                                Text(
                                    text = FormatterUtils.formatCurrency(toAccount.balance, currencyCode = toAccount.currency),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tintTo.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showToDropdown,
                        onDismissRequest = { showToDropdown = false }
                    ) {
                        selectableAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (${FormatterUtils.formatCurrency(account.balance, currencyCode = account.currency)})") },
                                onClick = {
                                    toAccountId = account.id
                                    localError = null
                                    showToDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            if (fromAccountId != null && fromAccountId == toAccountId) {
                Text(
                    text = "لا يمكن التحويل إلى نفس الحساب! يرجى اختيار حساب مستقبل مختلف.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExpenseRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            // 3. ADDITIONAL DETAILS (Fee & Note)
            OutlinedTextField(
                value = feeAmount,
                onValueChange = { feeAmount = it },
                label = { Text("الرسوم ($sourceCurrencySymbol)") },
                placeholder = { Text("0") },
                leadingIcon = { Icon(Icons.Default.Percent, null, tint = TextGray, modifier = Modifier.size(18.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("ملاحظة التحويل (اختياري)") },
                placeholder = { Text("مثال: تغذية حساب بريدي موب...") },
                leadingIcon = { Icon(Icons.Default.EditNote, null, tint = TextGray, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            TransactionDateSelector(
                transactionDate = transferDate,
                onDateChanged = { transferDate = it },
                typeAccentColor = TransferBlue,
                occurredAt = occurredAt,
                onOccurredAtChanged = { occurredAt = it }
            )

            // 4. SUBMIT ACTION BUTTON
            AppButton(
                onClick = {
                    val fromId = fromAccountId
                    val toId = toAccountId
                    val amt = AmountConversionEngine.parseFlexibleAmount(amount) ?: 0.0
                    val fee = AmountConversionEngine.parseFlexibleAmount(feeAmount)

                    if (fromId == null || toId == null) {
                        localError = "يرجى اختيار الحساب المصدر والحساب المستقبل."
                        return@AppButton
                    }
                    if (fromId == toId) {
                        localError = "لا يمكن التحويل إلى نفس الحساب! يرجى اختيار حساب مستقبل مختلف."
                        return@AppButton
                    }
                    if (amt <= 0.0) {
                        localError = "يرجى إدخال مبلغ تحويل صحيح أكبر من الصفر."
                        return@AppButton
                    }
                    localError = null
                    onExecuteTransfer(fromId, toId, amt, fee, note.ifBlank { null }, transferDate, occurredAt) { success ->
                        if (success) {
                            amount = ""
                            feeAmount = ""
                            note = ""
                            localError = null
                            transferDate = System.currentTimeMillis()
                            occurredAt = System.currentTimeMillis()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.INFO,
                isLoading = isLoading,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, null) }
            ) {
                Text("تأكيد وإجراء التحويل المالي", fontWeight = FontWeight.Bold)
            }

            (localError ?: error)?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = ExpenseRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TransferRecordItem(
    record: TransferRecord,
    accounts: List<Account>,
    modifier: Modifier = Modifier
) {
    val fromAcct = accounts.find { it.id == record.fromAccountId }
    val toAcct = accounts.find { it.id == record.toAccountId }
    val fromName = fromAcct?.name ?: "حساب مصدر"
    val toName = toAcct?.name ?: "حساب مستقبل"

    val tintFrom = when (fromAcct?.type) {
        AccountType.BARIDIMOB -> Color(0xFF005CA9)
        AccountType.CCP -> Color(0xFFF59E0B)
        AccountType.CASH -> Color(0xFF22C55E)
        AccountType.SAVINGS -> Color(0xFF3B82F6)
        else -> TransferBlue
    }
    val tintTo = when (toAcct?.type) {
        AccountType.BARIDIMOB -> Color(0xFF005CA9)
        AccountType.CCP -> Color(0xFFF59E0B)
        AccountType.CASH -> Color(0xFF22C55E)
        AccountType.SAVINGS -> Color(0xFF3B82F6)
        else -> IncomeGreen
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TransferBlue.copy(alpha = 0.02f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(TransferBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = TransferBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = fromName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = tintFrom
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward, // Auto-mirrors to point LEFT (←) in RTL: fromName ← toName
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = TextGray
                        )
                        Text(
                            text = toName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = tintTo
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = record.note ?: "تحويل داخلي لتسوية السيولة",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        fontSize = 11.sp
                    )
                    val displayTime = if (record.occurredAt != null) {
                        FormatterUtils.formatDateTime(record.occurredAt)
                    } else {
                        FormatterUtils.formatDate(record.date)
                    }
                    Text(
                        text = displayTime,
                        fontSize = 9.sp,
                        color = TextGray.copy(alpha = 0.8f)
                    )
                }
            }

            val recordCurrency = fromAcct?.currency ?: toAcct?.currency ?: "DZD"
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatterUtils.formatCurrency(record.amount, currencyCode = recordCurrency),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TransferBlue
                )
                if (record.feeAmount != null && record.feeAmount > 0) {
                    Text(
                        text = "العمولة: ${FormatterUtils.formatCurrency(record.feeAmount, currencyCode = recordCurrency)}",
                        fontSize = 10.sp,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
