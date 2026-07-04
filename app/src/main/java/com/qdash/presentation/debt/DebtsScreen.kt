package com.qdash.presentation.debt

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.Debt
import com.qdash.domain.model.DebtPayment
import com.qdash.domain.model.DebtPaymentType
import com.qdash.domain.model.DebtType
import com.qdash.presentation.debt.components.EditDebtBottomSheet
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
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
    var priority by remember { mutableStateOf("3") }
    var notes by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#EF4444") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var debtType by remember { mutableStateOf(DebtType.REGULAR) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    // Payment inputs
    var paymentAmount by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf(DebtPaymentType.MANUAL) }
    var paymentNote by remember { mutableStateOf("") }
    var sourceAccountId by remember { mutableStateOf<Long?>(null) }
    var paymentDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }

    // Edit, Delete, Forgive states
    var showEditDebtBottomSheet by remember { mutableStateOf<Debt?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Debt?>(null) }
    var showForgiveConfirmDialog by remember { mutableStateOf<Debt?>(null) }

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
                        paymentAmount = if (debt.debtType == DebtType.REGULAR) debt.remainingAmount.toInt().toString() else debt.minimumPayment.toInt().toString()
                        paymentNote = ""
                        paymentDate = System.currentTimeMillis()
                        showPaymentDialog = debt
                    },
                    onEditClick = { d -> showEditDebtBottomSheet = d },
                    onDeleteClick = { d -> showDeleteConfirmDialog = d },
                    onForgiveClick = { d -> showForgiveConfirmDialog = d }
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
                        paymentAmount = if (d.debtType == DebtType.REGULAR) d.remainingAmount.toInt().toString() else d.minimumPayment.toInt().toString()
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
                        debtType = DebtType.REGULAR
                        dueDate = null
                        showAddDebtDialog = true
                    },
                    onEditClick = { d -> showEditDebtBottomSheet = d },
                    onDeleteClick = { d -> showDeleteConfirmDialog = d },
                    onForgiveClick = { d -> showForgiveConfirmDialog = d }
                )
            }

            // --- DIALOGS ---

            // ADD DEBT DIALOG
            if (showAddDebtDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDebtDialog = false },
                    title = { Text("تسجيل التزام مالي / دين جديد", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            // Debt Type Selector
                            Text("نوع الالتزام:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(DebtType.REGULAR to "دين عادي", DebtType.INSTALLMENT to "دين مقسط").forEach { (type, label) ->
                                    val isSelected = debtType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) ExpenseRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, if (isSelected) ExpenseRed else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { debtType = type }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = label, fontWeight = FontWeight.Bold, color = if (isSelected) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }
                            }

                            AppInput(
                                value = title,
                                onValueChange = { title = it },
                                label = "عنوان التزام الدين",
                                placeholder = "مثال: قرض السيارة، سلفة عائلية"
                            )

                            AppInput(
                                value = creditorName,
                                onValueChange = { creditorName = it },
                                label = "اسم الدائن / الجهة"
                            )

                            if (debtType == DebtType.REGULAR) {
                                AppInput(
                                    value = totalAmount,
                                    onValueChange = { totalAmount = it },
                                    label = "المبلغ (د.ج)",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                                )

                                // Date Selector
                                Text("تاريخ الاستحقاق (اختياري):", style = MaterialTheme.typography.labelSmall)
                                AppCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = CardVariant.FLAT,
                                    onClick = { showDueDatePicker = true }
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
                                                text = if (dueDate != null) FormatterUtils.formatDate(dueDate!!) else "بدون تاريخ محدد",
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (dueDate != null) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = TextGray,
                                                modifier = Modifier.size(16.dp).clickable { dueDate = null }
                                            )
                                        } else {
                                            Icon(Icons.Default.Edit, null, tint = TextGray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                if (showDueDatePicker) {
                                    AppDatePickerDialog(
                                        initialSelectedDateMillis = dueDate ?: System.currentTimeMillis(),
                                        onDismissRequest = { showDueDatePicker = false },
                                        onDateSelected = { 
                                            dueDate = it
                                            showDueDatePicker = false
                                        },
                                        confirmButtonColor = ExpenseRed
                                    )
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AppInput(
                                        value = totalAmount,
                                        onValueChange = { totalAmount = it },
                                        label = "المبلغ (د.ج)",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
                                        modifier = Modifier.weight(1.5f)
                                    )

                                    AppInput(
                                        value = interestRate,
                                        onValueChange = { interestRate = it },
                                        label = "النسبة %",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                AppInput(
                                    value = minimumPayment,
                                    onValueChange = { minimumPayment = it },
                                    label = "القسط الشهري الأدنى (د.ج)",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                                )

                                // Priority Selector
                                Text("أولوية السداد والاستعجال:", style = MaterialTheme.typography.labelSmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf("1" to "طارئ", "3" to "متوسط", "5" to "مرن").forEach { (id, label) ->
                                        val isSelected = priority == id
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) ExpenseRed
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable { priority = id }
                                                .padding(vertical = 8.dp),
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

                            // Linked Account Selector
                            Text("الحساب المرتبط (اختياري):", style = MaterialTheme.typography.labelSmall)
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                val selectedAccountName = uiState.accounts.find { it.id == selectedAccountId }?.name ?: "بدون حساب مرتبط"
                                AppCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = CardVariant.FLAT,
                                    onClick = { expanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedAccountName)
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("بدون حساب مرتبط") },
                                        onClick = {
                                            selectedAccountId = null
                                            expanded = false
                                        }
                                    )
                                    uiState.accounts.forEach { account ->
                                        DropdownMenuItem(
                                            text = { Text("${account.name} (رصيد: ${account.balance.toInt()} د.ج)") },
                                            onClick = {
                                                selectedAccountId = account.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            AppInput(
                                value = notes,
                                onValueChange = { notes = it },
                                label = "ملاحظات إضافية (اختياري)",
                                placeholder = "مثال: شروط الدفع، تفاصيل الضمان"
                            )
                        }
                    },
                    confirmButton = {
                        AppButton(
                            onClick = {
                                val total = totalAmount.toDoubleOrNull() ?: 0.0
                                val minPay = minimumPayment.toDoubleOrNull() ?: 0.0
                                if (title.isBlank() || creditorName.isBlank()) {
                                    Toast.makeText(context, "الرجاء ملء اسم الدين والدائن", Toast.LENGTH_SHORT).show()
                                } else if (totalAmount.isBlank() || total <= 0.0) {
                                    Toast.makeText(context, "الرجاء إدخال مبلغ دين صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                } else if (debtType == DebtType.INSTALLMENT && (minimumPayment.isBlank() || minPay <= 0.0)) {
                                    Toast.makeText(context, "الرجاء إدخال قسط شهري صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addDebt(
                                        title = title.trim(),
                                        creditorName = creditorName.trim(),
                                        totalAmount = total,
                                        minimumPayment = if (debtType == DebtType.REGULAR) 0.0 else minPay,
                                        paymentFrequency = "Monthly",
                                        linkedAccountId = selectedAccountId,
                                        priority = if (debtType == DebtType.REGULAR) 3 else (priority.toIntOrNull() ?: 3),
                                        notes = notes.ifBlank { null },
                                        color = color,
                                        interestRate = if (debtType == DebtType.REGULAR) null else interestRate.toDoubleOrNull(),
                                        dueDate = dueDate,
                                        debtType = debtType
                                    )
                                    showAddDebtDialog = false
                                }
                            },
                            variant = ButtonVariant.SOLID,
                            intent = ButtonIntent.DANGER
                        ) {
                            Text("حفظ الدين", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        AppButton(
                            onClick = { showAddDebtDialog = false },
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.PRIMARY
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
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

                            AppInput(
                                value = paymentAmount,
                                onValueChange = { paymentAmount = it },
                                label = "مبلغ الدفعة (د.ج)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                            )

                            AppInput(
                                value = paymentNote,
                                onValueChange = { paymentNote = it },
                                label = "ملاحظة / رقم الإيصال"
                            )

                            // --- DATE SELECTOR CARD ---
                            Text("تاريخ دفعة السداد:", style = MaterialTheme.typography.labelSmall)
                            AppCard(
                                modifier = Modifier.fillMaxWidth(),
                                variant = CardVariant.FLAT,
                                onClick = { showPaymentDatePicker = true }
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
                                AppDatePickerDialog(
                                    initialSelectedDateMillis = paymentDate,
                                    onDismissRequest = { showPaymentDatePicker = false },
                                    onDateSelected = { paymentDate = it },
                                    confirmButtonColor = ExpenseRed
                                )
                            }

                            // Source account spinner
                            Text("الدفع من حساب:", style = MaterialTheme.typography.labelSmall)
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                val selectedAccountName = uiState.accounts.find { it.id == sourceAccountId }?.name ?: "اختر الحساب"
                                AppCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = CardVariant.FLAT,
                                    onClick = { expanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
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
                        AppButton(
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
                            variant = ButtonVariant.SOLID,
                            intent = ButtonIntent.DANGER
                        ) {
                            Text("تسجيل سداد", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        AppButton(
                            onClick = { showPaymentDialog = null },
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.PRIMARY
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
            // EDIT DEBT BOTTOM SHEET
            showEditDebtBottomSheet?.let { targetDebt ->
                EditDebtBottomSheet(
                    debt = targetDebt,
                    accounts = uiState.accounts,
                    onDismissRequest = { showEditDebtBottomSheet = null },
                    onConfirm = { title, creditorName, totalAmount, minimumPayment, paymentFrequency, linkedAccountId, priority, notes, color, interestRate, dueDate ->
                        viewModel.updateDebtDetails(
                            debtId = targetDebt.id,
                            title = title,
                            creditorName = creditorName,
                            totalAmount = totalAmount,
                            minimumPayment = minimumPayment,
                            paymentFrequency = paymentFrequency,
                            linkedAccountId = linkedAccountId,
                            priority = priority,
                            notes = notes,
                            color = color,
                            interestRate = interestRate,
                            dueDate = dueDate,
                            onSuccess = {
                                showEditDebtBottomSheet = null
                                Toast.makeText(context, "تم تحديث الدين بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }

            // DELETE DEBT CONFIRM DIALOG
            showDeleteConfirmDialog?.let { targetDebt ->
                AppDialog(
                    onDismissRequest = { showDeleteConfirmDialog = null },
                    title = "حذف السجل المالي للدين",
                    text = "هل أنت متأكد من حذف دين '${targetDebt.title}' نهائياً؟ سيؤدي ذلك أيضاً إلى حذف جميع دفعات السداد المسجلة المرتبطة به وإلغاء تأثيرها على رصيد محفظتك المالية (حذف المعاملات). لا يمكن التراجع عن هذا الإجراء.",
                    confirmButtonText = "تأكيد الحذف",
                    onConfirm = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteDebt(targetDebt.id)
                        showDeleteConfirmDialog = null
                        if (activeDebtForDetails?.id == targetDebt.id) {
                            activeDebtForDetails = null
                        }
                        Toast.makeText(context, "تم حذف الدين وإلغاء دفعاته بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    dismissButtonText = "إلغاء",
                    onDismiss = { showDeleteConfirmDialog = null },
                    isDestructive = true
                )
            }

            // FORGIVE DEBT CONFIRM DIALOG
            showForgiveConfirmDialog?.let { targetDebt ->
                AppDialog(
                    onDismissRequest = { showForgiveConfirmDialog = null },
                    title = "الإعفاء من الدين",
                    text = "هل أنت متأكد من الإعفاء من المتبقي لدين '${targetDebt.title}'؟ سيتم تصفير المبلغ المتبقي وتعيين الدين كمغلق دون خصم أي مبلغ من رصيدك المالي أو إنشاء معاملة سداد.",
                    confirmButtonText = "تأكيد الإعفاء",
                    onConfirm = {
                        viewModel.forgiveDebt(
                            debtId = targetDebt.id,
                            onSuccess = {
                                showForgiveConfirmDialog = null
                                if (activeDebtForDetails?.id == targetDebt.id) {
                                    activeDebtForDetails = uiState.debts.find { it.id == targetDebt.id }
                                }
                                Toast.makeText(context, "تم الإعفاء من الدين وتصفيره", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    dismissButtonText = "إلغاء",
                    onDismiss = { showForgiveConfirmDialog = null }
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
    onAddDebtClick: () -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit,
    onForgiveClick: (Debt) -> Unit
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
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xxl,
                backgroundColor = Color.Transparent
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
                            text = com.qdash.core.utils.FormatterUtils.convertNumerals("${String.format(Locale.getDefault(), "%,d", totalRemaining.toLong())} د.ج"),
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
                } ?: uiState.strategyResults.firstOrNull() ?: com.qdash.domain.model.DebtStrategyResult(
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
                            Text(com.qdash.core.utils.FormatterUtils.convertNumerals("الفترة المقدرة للتصفية: ${String.format(Locale.getDefault(), "%.1f", currentStratResult.durationInMonths)} شهر"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
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
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = CardVariant.SOLID,
                            shape = ShapeTokens.Lg,
                            backgroundColor = ExpenseRed.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                Text(com.qdash.core.utils.FormatterUtils.convertNumerals(insightText), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
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

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Xl,
                    onClick = { onSelectDebt(debt) },
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(debt.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        val typeLabel = if (debt.debtType == DebtType.REGULAR) "عادي" else "مقسط"
                                        val typeColor = if (debt.debtType == DebtType.REGULAR) SavingsAmber else ExpenseRed
                                        Box(
                                            modifier = Modifier
                                                .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(typeLabel, fontSize = 8.sp, color = typeColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ExpenseRed)
                                            .clickable { onPayClick(debt) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("تسديد دفعة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "خيارات الدين",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(if (isDark) ColorTokens.ElevatedSurfaceDark else MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("تعديل تفاصيل الدين", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                menuExpanded = false
                                                onEditClick(debt)
                                            }
                                        )
                                        if (!isClosed) {
                                            DropdownMenuItem(
                                                text = { Text("إعفاء من المتبقي", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                                leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onForgiveClick(debt)
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("حذف السجل المالي", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth(), color = ExpenseRed) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                menuExpanded = false
                                                onDeleteClick(debt)
                                            }
                                        )
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
    onPayClick: () -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit,
    onForgiveClick: (Debt) -> Unit
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
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xxl,
                backgroundColor = MaterialTheme.colorScheme.surface
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
                        AppCard(
                            modifier = Modifier.weight(1f),
                            variant = CardVariant.FLAT,
                            shape = ShapeTokens.Lg,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("الأصل", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(FormatterUtils.formatCurrency(debt.totalAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        // Micro Card 2
                        AppCard(
                            modifier = Modifier.weight(1f),
                            variant = CardVariant.FLAT,
                            shape = ShapeTokens.Lg,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("الجهة", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(debt.creditorName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                            }
                        }
                        // Micro Card 3
                        AppCard(
                            modifier = Modifier.weight(1f),
                            variant = CardVariant.FLAT,
                            shape = ShapeTokens.Lg,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("الأولوية", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                val pText = if (debt.priority == 1) "عاجل" else "عادي"
                                Text(pText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (debt.priority == 1) ExpenseRed else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (debt.dueDate != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Event, null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            Text("تاريخ الاستحقاق: ${FormatterUtils.formatDate(debt.dueDate)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }
                    }

                    debt.linkedAccountId?.let { accId ->
                        val accName = uiState.accounts.find { it.id == accId }?.name
                        if (accName != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = TextGray, modifier = Modifier.size(16.dp))
                                Text("الحساب المرتبط: $accName", style = MaterialTheme.typography.bodySmall, color = TextGray)
                            }
                        }
                    }

                    if (!debt.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = CardVariant.FLAT,
                            shape = ShapeTokens.Lg,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
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
                AppButton(
                    onClick = onPayClick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.DANGER,
                    shape = ShapeTokens.Lg,
                    leadingIcon = { Icon(Icons.Default.CreditCard, null) }
                ) {
                    Text("تسجيل سداد دفعة جديدة", fontWeight = FontWeight.Bold)
                }
            }
        }

        // STRATEGY ACTIONS (CLOSE, DELETE)
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
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!debt.isClosed) {
                        AppButton(
                            onClick = { viewModel.closeDebt(debt.id) },
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.SUCCESS,
                            leadingIcon = { Icon(Icons.Default.CheckCircle, "إغلاق", modifier = Modifier.size(16.dp)) }
                        ) {
                            Text("إغلاق", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        AppButton(
                            onClick = { onForgiveClick(debt) },
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.SUCCESS,
                            leadingIcon = { Icon(Icons.Default.CardGiftcard, "إعفاء", modifier = Modifier.size(16.dp)) }
                        ) {
                            Text("إعفاء", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

        // PAYMENT HISTORY LIST HEADER
        item {
            Text("سجل تسوية السداد المالي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (uiState.selectedDebtPayments.isEmpty()) {
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Lg,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لم تسجل أي دفعات سداد لهذا الالتزام بعد.", textAlign = TextAlign.Center, color = TextGray)
                    }
                }
            }
        } else {
            items(uiState.selectedDebtPayments, key = { it.id }) { payment ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Lg,
                    backgroundColor = MaterialTheme.colorScheme.surface
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
