package com.example.presentation.debt

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.core.ui.components.EmptyStateView
import com.example.core.ui.components.FinTrackTopBar
import com.example.core.utils.FormatterUtils
import com.example.domain.model.Account
import com.example.domain.model.Debt
import com.example.domain.model.DebtPayment
import com.example.domain.model.DebtPaymentType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.widget.Toast
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: DebtViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()
    
    // Sub-screen navigation states
    var activeDebtForDetails by remember { mutableStateOf<Debt?>(null) }
    
    // Input form states
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf<Debt?>(null) }
    
    // Debt inputs
    var title by remember { mutableStateOf("") }
    var creditorName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var minimumPayment by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#EF4444") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }

    // Payment inputs
    var paymentAmount by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf(DebtPaymentType.MANUAL) }
    var paymentNote by remember { mutableStateOf("") }
    var sourceAccountId by remember { mutableStateOf<Long?>(null) }
    var paymentDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.accounts) {
        if (uiState.accounts.isNotEmpty() && selectedAccountId == null) {
            selectedAccountId = uiState.accounts.first().id
            sourceAccountId = uiState.accounts.first().id
        }
    }

    LaunchedEffect(activeDebtForDetails, uiState.debts) {
        activeDebtForDetails?.let { current ->
            val updated = uiState.debts.find { it.id == current.id }
            activeDebtForDetails = updated
            if (updated != null) {
                viewModel.selectDebt(updated.id)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("debts_screen"),
        topBar = {
            if (activeDebtForDetails != null) {
                FinTrackTopBar(
                    title = activeDebtForDetails!!.title,
                    showBackButton = true,
                    onBackClick = { activeDebtForDetails = null }
                )
            } else {
                FinTrackTopBar(title = "خطة وتسوية الديون والالتزامات")
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeDebtForDetails != null) {
                // DEBT DETAILS & REPAYMENT TIMELINE
                val debt = activeDebtForDetails!!
                DebtDetailsContent(
                    debt = debt,
                    uiState = uiState,
                    viewModel = viewModel,
                    onPayClick = {
                        paymentAmount = debt.minimumPayment.toInt().toString()
                        paymentNote = ""
                        paymentDate = System.currentTimeMillis()
                        showPaymentDialog = debt
                    }
                )
            } else {
                // DEBTS MAIN HUB
                DebtsMainContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSelectDebt = { d ->
                        activeDebtForDetails = d
                        viewModel.selectDebt(d.id)
                    },
                    onPayClick = { d ->
                        paymentAmount = d.minimumPayment.toInt().toString()
                        paymentNote = ""
                        paymentDate = System.currentTimeMillis()
                        showPaymentDialog = d
                    },
                    onAddDebtClick = {
                        title = ""
                        creditorName = ""
                        totalAmount = ""
                        interestRate = "0.0"
                        minimumPayment = ""
                        priority = "3"
                        notes = ""
                        showAddDebtDialog = true
                    }
                )
            }

            // --- DIALOGS ---

            // ADD DEBT DIALOG
            if (showAddDebtDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDebtDialog = false },
                    title = { Text("تسجيل التزام مالي / دين جديد", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("عنوان التزام الدين") },
                                placeholder = { Text("مثال: قرض السيارة، سلفة عائلية") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = creditorName,
                                onValueChange = { creditorName = it },
                                label = { Text("اسم الدائن / الجهة") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = totalAmount,
                                    onValueChange = { totalAmount = it },
                                    label = { Text("المبلغ (د.ج)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                                    modifier = Modifier.weight(1.5f)
                                )

                                OutlinedTextField(
                                    value = interestRate,
                                    onValueChange = { interestRate = it },
                                    label = { Text("النسبة %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = minimumPayment,
                                onValueChange = { minimumPayment = it },
                                label = { Text("القسط الشهري الأدنى (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Priority Selector
                            Text("أولوية السداد والاستعجال:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("1" to "طارئ", "3" to "متوسط", "5" to "مرن").forEach { (id, label) ->
                                    val isSelected = priority == id
                                    Button(
                                        onClick = { priority = id },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) ExpenseRed else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(2.dp)
                                    ) {
                                        Text(label, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val total = totalAmount.toDoubleOrNull() ?: 0.0
                                val minPay = minimumPayment.toDoubleOrNull() ?: 0.0
                                if (title.isBlank() || creditorName.isBlank()) {
                                    Toast.makeText(context, "الرجاء ملء اسم الدين والدائن", Toast.LENGTH_SHORT).show()
                                } else if (totalAmount.isBlank() || total <= 0.0) {
                                    Toast.makeText(context, "الرجاء إدخال مبلغ دين صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                } else if (minimumPayment.isBlank() || minPay <= 0.0) {
                                    Toast.makeText(context, "الرجاء إدخال قسط شهري صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addDebt(
                                        title = title.trim(),
                                        creditorName = creditorName.trim(),
                                        totalAmount = total,
                                        minimumPayment = minPay,
                                        paymentFrequency = "Monthly",
                                        linkedAccountId = selectedAccountId,
                                        priority = priority.toIntOrNull() ?: 3,
                                        notes = notes.ifBlank { null },
                                        color = color,
                                        interestRate = interestRate.toDoubleOrNull()
                                    )
                                    showAddDebtDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                        ) {
                            Text("حفظ الدين")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDebtDialog = false }) { Text("إلغاء") }
                    }
                )
            }

            // RECORD PAYMENT DIALOG
            showPaymentDialog?.let { debt ->
                AlertDialog(
                    onDismissRequest = { showPaymentDialog = null },
                    title = { Text("تسجيل سداد دفعة من الدين", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("الدين: ${debt.title}", fontWeight = FontWeight.Medium, color = ExpenseRed)
                            Text("المتبقي الكلي: ${debt.remainingAmount.toInt()} د.ج", style = MaterialTheme.typography.bodySmall)

                            OutlinedTextField(
                                value = paymentAmount,
                                onValueChange = { paymentAmount = it },
                                label = { Text("مبلغ الدفعة (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = paymentNote,
                                onValueChange = { paymentNote = it },
                                label = { Text("ملاحظة / رقم الإيصال") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // --- DATE SELECTOR CARD ---
                            Text("تاريخ دفعة السداد:", style = MaterialTheme.typography.labelSmall)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPaymentDatePicker = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DateRange, null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = FormatterUtils.formatDate(paymentDate),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Icon(Icons.Default.Edit, null, tint = TextGray, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (showPaymentDatePicker) {
                                val datePickerState = rememberDatePickerState(
                                    initialSelectedDateMillis = paymentDate
                                )
                                DatePickerDialog(
                                    onDismissRequest = { showPaymentDatePicker = false },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                datePickerState.selectedDateMillis?.let {
                                                    paymentDate = it
                                                }
                                                showPaymentDatePicker = false
                                            }
                                        ) {
                                            Text("موافق", color = ExpenseRed, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showPaymentDatePicker = false }) {
                                            Text("إلغاء", color = TextGray)
                                        }
                                    }
                                ) {
                                    DatePicker(
                                        state = datePickerState,
                                        colors = DatePickerDefaults.colors(
                                            selectedDayContainerColor = ExpenseRed,
                                            selectedDayContentColor = Color.White,
                                            todayContentColor = ExpenseRed,
                                            todayDateBorderColor = ExpenseRed
                                        )
                                    )
                                }
                            }

                            // Source account spinner
                            Text("الدفع من حساب:", style = MaterialTheme.typography.labelSmall)
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                val selectedAccountName = uiState.accounts.find { it.id == sourceAccountId }?.name ?: "اختر الحساب"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(selectedAccountName)
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    uiState.accounts.forEach { account ->
                                        DropdownMenuItem(
                                            text = { Text("${account.name} (رصيد: ${account.balance.toInt()} د.ج)") },
                                            onClick = {
                                                sourceAccountId = account.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = paymentAmount.toDoubleOrNull() ?: 0.0
                                if (paymentAmount.isBlank() || amount <= 0.0) {
                                    Toast.makeText(context, "الرجاء إدخال مبلغ دفع صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                } else if (sourceAccountId == null) {
                                    Toast.makeText(context, "الرجاء تحديد الحساب مصدر السداد", Toast.LENGTH_SHORT).show()
                                } else if (amount > debt.remainingAmount) {
                                    Toast.makeText(context, "المبلغ المدفوع أكبر من قيمة الدين المتبقية (${debt.remainingAmount.toInt()} د.ج)!", Toast.LENGTH_LONG).show()
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.makePayment(
                                        debtId = debt.id,
                                        accountId = sourceAccountId!!,
                                        amount = amount,
                                        paymentType = paymentType,
                                        note = paymentNote.ifBlank { null },
                                        date = paymentDate
                                    )
                                    showPaymentDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                        ) {
                            Text("تسجيل سداد")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPaymentDialog = null }) { Text("إلغاء") }
                    }
                )
            }
        }
        } // end PullToRefreshBox
    }
}

@Composable
fun DebtsMainContent(
    uiState: DebtUiState,
    viewModel: DebtViewModel,
    onSelectDebt: (Debt) -> Unit,
    onPayClick: (Debt) -> Unit,
    onAddDebtClick: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // SUMMARY METRICS HERO CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                val totalRemaining = uiState.debts.filter { !it.isClosed }.sumOf { it.remainingAmount }
                val totalMin = uiState.debts.filter { !it.isClosed }.sumOf { it.minimumPayment }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF141416), Color(0xFF381212))
                            )
                        )
                ) {
                    // Translucent decorative shapes
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-30).dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.03f))
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-20).dp, y = 30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.04f))
                    )

                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("إجمالي التزامات الديون القائمة", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = com.example.core.utils.FormatterUtils.convertNumerals("${String.format(Locale.getDefault(), "%,d", totalRemaining.toLong())} د.ج"),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("القسط الشهري الإجمالي", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${totalMin.toInt()} د.ج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("القضايا النشطة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${uiState.debts.count { !it.isClosed }} قضايا دين", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("الديون المغلقة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${uiState.debts.count { it.isClosed }} ذمة مبرأة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // STRATEGY COMPILER DISPLAY & BENCHMARK CELEBRATION
        if (uiState.strategyResults.isNotEmpty() && uiState.debts.any { !it.isClosed }) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مخطط تسوية الديون الاستراتيجي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    // Glassmorphic toggle buttons
                    Row(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(3.dp)
                    ) {
                        listOf("snowball" to "كرة الثلج", "avalanche" to "الانهيار").forEach { (id, label) ->
                            val isSelected = uiState.selectedStrategy == id
                            val bg = if (isSelected) Primary else Color.Transparent
                            val tc = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { viewModel.changeStrategy(id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = tc,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            item {
                val currentStratResult = uiState.strategyResults.find {
                    if (uiState.selectedStrategy == "snowball") it.strategyName.contains("الثلج")
                    else it.strategyName.contains("الانهيار")
                } ?: uiState.strategyResults.firstOrNull() ?: com.example.domain.model.DebtStrategyResult(
                    strategyName = "جدولة افتراضية",
                    durationInMonths = 0.0,
                    estimatedDebtFreeDate = System.currentTimeMillis(), 
                    monthlyPaymentNeeded = 0.0,
                    paymentScheduleSummary = "لا توجد تفاصيل حالياً."
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(currentStratResult.strategyName, fontWeight = FontWeight.Bold, color = Primary)
                            }
                            Box(
                                modifier = Modifier
                                    .background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("الحل الأمثل!", fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(currentStratResult.paymentScheduleSummary ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(com.example.core.utils.FormatterUtils.convertNumerals("الفترة المقدرة للتصفية: ${String.format(Locale.getDefault(), "%.1f", currentStratResult.durationInMonths)} شهر"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            Text("جاهز للتصفية الكاملة!", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    }
                }
            }
        }

        // MOTIVATIONAL ALERTS & INSIGHTS
        if (uiState.insights.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.insights.take(1).forEach { insightText ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                Text(com.example.core.utils.FormatterUtils.convertNumerals(insightText), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // DEBTS LIST SECTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("شروط وجداول الديون القائمة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = onAddDebtClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                ) {
                    Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسجيل دين", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (uiState.debts.isEmpty()) {
            item {
                EmptyStateView(
                    title = "لا توجد ديون مسجلة",
                    description = "مبروك! لا توجد التزامات مالية قائمة مسجلة بالبرنامج. يمكنك إضافة أي التزام لمراقبته وتسويته بذكاء."
                )
            }
        } else {
            val orderedDebts = if (uiState.selectedStrategy == "snowball") {
                uiState.debts.sortedBy { it.remainingAmount }
            } else {
                uiState.debts.sortedByDescending { it.interestRate ?: 0.0 }
            }

            items(orderedDebts, key = { it.id }) { debt ->
                val progress = if (debt.totalAmount > 0) ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).toFloat() else 1f
                val isUrgent = debt.priority == 1
                val isClosed = debt.isClosed

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDebt(debt) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
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
                                        .size(36.dp)
                                        .background(
                                            (if (isClosed) IncomeGreen else if (isUrgent) ExpenseRed else SavingsAmber).copy(alpha = 0.12f),
                                            CircleShape
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
                                    Text(debt.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(debt.creditorName, style = MaterialTheme.typography.labelSmall, color = TextGray)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isClosed) {
                                    Box(
                                        modifier = Modifier
                                            .background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("كامل السداد", fontSize = 9.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    if (isUrgent) {
                                        Box(
                                            modifier = Modifier
                                                .background(ExpenseRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("عاجل جداً", fontSize = 9.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Button(
                                        onClick = { onPayClick(debt) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("تسديد دفعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("المبلغ المتبقي للحل", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatterUtils.formatCurrency(debt.remainingAmount),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isClosed) IncomeGreen else ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("القسط / الفائدة", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                val interestStr = if (debt.interestRate != null) "${debt.interestRate}%" else "مرن"
                                Text("${debt.minimumPayment.toInt()} د.ج / $interestStr", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (isClosed) IncomeGreen else ExpenseRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebtDetailsContent(
    debt: Debt,
    uiState: DebtUiState,
    viewModel: DebtViewModel,
    onPayClick: () -> Unit
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = if (debt.isClosed) IncomeGreen else ExpenseRed,
                                strokeWidth = 6.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                        Column {
                            Text("المبلغ المتبقي حالياً للسداد", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatterUtils.formatCurrency(debt.remainingAmount),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (debt.isClosed) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Micro Card 1
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("الأصل", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(FormatterUtils.formatCurrency(debt.totalAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        // Micro Card 2
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("الجهة", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(debt.creditorName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                            }
                        }
                        // Micro Card 3
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("الأولوية", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                val pText = if (debt.priority == 1) "عاجل" else "عادي"
                                Text(pText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (debt.priority == 1) ExpenseRed else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (!debt.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Notes, null, tint = TextGray, modifier = Modifier.size(16.dp))
                                Text(debt.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }

        // PAY TRANSACTION ACTION TRIGGER
        if (!debt.isClosed) {
            item {
                Button(
                    onClick = onPayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CreditCard, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل سداد دفعة جديدة", fontWeight = FontWeight.Bold)
                }
            }
        }

        // STRATEGY ACTIONS (CLOSE, DELETE)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (!debt.isClosed) {
                        TextButton(
                            onClick = { viewModel.closeDebt(debt.id) },
                            colors = ButtonDefaults.textButtonColors(contentColor = IncomeGreen)
                        ) {
                            Icon(Icons.Default.CheckCircle, "قفل الدين", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعليم كمغلق", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(
                        onClick = { viewModel.deleteDebt(debt.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, "مسح", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح السجل المالي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // PAYMENT HISTORY LIST HEADER
        item {
            Text("سجل تسوية السداد المالي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (uiState.selectedDebtPayments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لم تسجل أي دفعات سداد لهذا الالتزام بعد.", textAlign = TextAlign.Center, color = TextGray)
                    }
                }
            }
        } else {
            items(uiState.selectedDebtPayments, key = { it.id }) { payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(IncomeGreen.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                val dateStr = sdf.format(Date(payment.paymentDate))
                                Text(payment.note ?: "تسديد قسط دين منتظم", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextGray)
                            }
                        }
                        Text(
                            text = "- ${payment.amount.toInt()} د.ج",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
            }
        }
    }
}
