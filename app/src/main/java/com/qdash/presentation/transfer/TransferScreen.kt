package com.qdash.presentation.transfer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.TransferRecord
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form inputs
    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var feeAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var transferDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Dropdown visibility
    var showFromDropdown by remember { mutableStateOf(false) }
    var showToDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.accounts) {
        if (uiState.accounts.isNotEmpty()) {
            if (fromAccountId == null) fromAccountId = uiState.accounts.first().id
            if (toAccountId == null && uiState.accounts.size > 1) toAccountId = uiState.accounts[1].id
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("transfer_screen"),
        topBar = {
            FinTrackTopBar(title = "تحويل الأرصدة والمبالغ")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // MAIN HERO TRANSFER HUB
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "إجراء عملية تحويل داخلي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TransferBlue
                        )

                        // 1. HERO AMOUNT INPUT CONTAINER
                        val fromAccount = uiState.accounts.find { it.id == fromAccountId }
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
                                        onValueChange = { amount = it },
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
                                        text = "د.ج",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TransferBlue
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (fromAccount != null) "الرصيد المتاح بالمصدر: ${fromAccount.balance.toInt()} د.ج" else "الرصيد المتاح بالمصدر: 0 د.ج",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
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
                                    com.qdash.domain.model.AccountType.BARIDIMOB -> Color(0xFF005CA9)
                                    com.qdash.domain.model.AccountType.CCP -> Color(0xFFF59E0B)
                                    com.qdash.domain.model.AccountType.CASH -> Color(0xFF22C55E)
                                    com.qdash.domain.model.AccountType.SAVINGS -> Color(0xFF3B82F6)
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
                                                com.qdash.domain.model.AccountType.BARIDIMOB -> Icons.Default.Smartphone
                                                com.qdash.domain.model.AccountType.CCP -> Icons.Default.AccountBalance
                                                com.qdash.domain.model.AccountType.CASH -> Icons.Default.Payments
                                                com.qdash.domain.model.AccountType.SAVINGS -> Icons.Default.Savings
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
                                                text = FormatterUtils.formatCurrency(fromAccount.balance),
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
                                    uiState.accounts.forEach { account ->
                                        DropdownMenuItem(
                                            text = { Text("${account.name} (رصيد: ${account.balance.toInt()} د.ج)") },
                                            onClick = {
                                                fromAccountId = account.id
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
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz, // Horizontal swap indicator fits interactive reverse action perfectly
                                    contentDescription = "عكس الحسابات",
                                    tint = TransferBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Destination account card selector
                            Box(modifier = Modifier.weight(1f)) {
                                val toAccount = uiState.accounts.find { it.id == toAccountId }
                                val tintTo = when (toAccount?.type) {
                                    com.qdash.domain.model.AccountType.BARIDIMOB -> Color(0xFF005CA9)
                                    com.qdash.domain.model.AccountType.CCP -> Color(0xFFF59E0B)
                                    com.qdash.domain.model.AccountType.CASH -> Color(0xFF22C55E)
                                    com.qdash.domain.model.AccountType.SAVINGS -> Color(0xFF3B82F6)
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
                                                com.qdash.domain.model.AccountType.BARIDIMOB -> Icons.Default.Smartphone
                                                com.qdash.domain.model.AccountType.CCP -> Icons.Default.AccountBalance
                                                com.qdash.domain.model.AccountType.CASH -> Icons.Default.Payments
                                                com.qdash.domain.model.AccountType.SAVINGS -> Icons.Default.Savings
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
                                                text = FormatterUtils.formatCurrency(toAccount.balance),
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
                                    uiState.accounts.forEach { account ->
                                        DropdownMenuItem(
                                            text = { Text("${account.name} (رصيد: ${account.balance.toInt()} د.ج)") },
                                            onClick = {
                                                toAccountId = account.id
                                                showToDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. ADDITIONAL DETAILS
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = feeAmount,
                                onValueChange = { feeAmount = it },
                                label = { Text("الرسوم (د.ج)") },
                                placeholder = { Text("0") },
                                leadingIcon = { Icon(Icons.Default.Percent, null, tint = TextGray, modifier = Modifier.size(18.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Date button inline
                            Box(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.DateRange, null, tint = TransferBlue, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("تاريخ العملية", style = MaterialTheme.typography.labelSmall, color = TextGray, fontSize = 8.sp)
                                        Text(
                                            text = FormatterUtils.formatDate(transferDate),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("سبب التحويل وملاحظات") },
                            placeholder = { Text("مثال: لتغذية ظرف المصاريف الأسبوعية") },
                            leadingIcon = { Icon(Icons.Default.EditNote, null, tint = TextGray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (showDatePicker) {
                            AppDatePickerDialog(
                                initialSelectedDateMillis = transferDate,
                                onDismissRequest = { showDatePicker = false },
                                onDateSelected = { transferDate = it },
                                confirmButtonColor = TransferBlue
                            )
                        }

                        // 4. SUBMIT ACTION BUTTON
                        AppButton(
                            onClick = {
                                val fromId = fromAccountId
                                val toId = toAccountId
                                val amt = amount.toDoubleOrNull() ?: 0.0
                                val fee = feeAmount.toDoubleOrNull()

                                if (fromId != null && toId != null && amt > 0) {
                                    if (fromId == toId) return@AppButton
                                    viewModel.executeTransfer(
                                        fromAccountId = fromId,
                                        toAccountId = toId,
                                        amount = amt,
                                        feeAmount = fee,
                                        note = note.ifBlank { null },
                                        date = transferDate
                                    ) { success ->
                                        if (success) {
                                            amount = ""
                                            feeAmount = ""
                                            note = ""
                                            transferDate = System.currentTimeMillis()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            variant = ButtonVariant.SOLID,
                            intent = ButtonIntent.INFO,
                            isLoading = uiState.isLoading,
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, null) }
                        ) {
                            Text("تأكيد وإجراء التحويل المالي", fontWeight = FontWeight.Bold)
                        }

                        uiState.error?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // SECTION HEADER: RECENT TRANSFERS
            item {
                Text(
                    text = "سجل العمليات والتحويلات الأخيرة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // TIMELINE / LIST OF TRANSFERS (Cardless, Inline Design)
            if (uiState.transfers.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "لا توجد حركات تحويل مسجلة",
                        description = "يمكنك تحويل الأرصدة والسيولة المالية بين حساباتك المختلفة لتتبع الأرصدة بدقة وتسجيل الرسوم المرفقة."
                    )
                }
            } else {
                items(uiState.transfers) { record ->
                    val fromAcct = uiState.accounts.find { it.id == record.fromAccountId }
                    val toAcct = uiState.accounts.find { it.id == record.toAccountId }
                    val fromName = fromAcct?.name ?: "حساب مصدر"
                    val toName = toAcct?.name ?: "حساب مستقبل"

                    val tintFrom = when (fromAcct?.type) {
                        com.qdash.domain.model.AccountType.BARIDIMOB -> Color(0xFF005CA9)
                        com.qdash.domain.model.AccountType.CCP -> Color(0xFFF59E0B)
                        com.qdash.domain.model.AccountType.CASH -> Color(0xFF22C55E)
                        com.qdash.domain.model.AccountType.SAVINGS -> Color(0xFF3B82F6)
                        else -> TransferBlue
                    }
                    val tintTo = when (toAcct?.type) {
                        com.qdash.domain.model.AccountType.BARIDIMOB -> Color(0xFF005CA9)
                        com.qdash.domain.model.AccountType.CCP -> Color(0xFFF59E0B)
                        com.qdash.domain.model.AccountType.CASH -> Color(0xFF22C55E)
                        com.qdash.domain.model.AccountType.SAVINGS -> Color(0xFF3B82F6)
                        else -> IncomeGreen
                    }

                    Column {
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
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Points left for Arabic RTL
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
                                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                    Text(
                                        text = sdf.format(Date(record.date)),
                                        fontSize = 9.sp,
                                        color = TextGray.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${record.amount.toInt()} د.ج",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TransferBlue
                                )
                                if (record.feeAmount != null && record.feeAmount > 0) {
                                    Text(
                                        text = "العمولة: ${record.feeAmount.toInt()} د.ج",
                                        fontSize = 10.sp,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Medium
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

