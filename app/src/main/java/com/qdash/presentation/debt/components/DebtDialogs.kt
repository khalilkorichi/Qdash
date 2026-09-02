package com.qdash.presentation.debt.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.Debt
import com.qdash.domain.model.DebtPaymentType
import com.qdash.domain.model.InstallmentDebt
import com.qdash.domain.model.RegularDebt
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRegularDebtDialog(
    accounts: List<Account>,
    onDismissRequest: () -> Unit,
    onConfirm: (
        title: String,
        creditorName: String,
        totalAmount: Double,
        linkedAccountId: Long?,
        notes: String?,
        color: String,
        dueDate: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var title by remember { mutableStateOf("") }
    var creditorName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#EF4444") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && selectedAccountId == null) {
            selectedAccountId = accounts.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("تسجيل دين / التزام مالي عادي", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                AppInput(
                    value = title,
                    onValueChange = { title = it },
                    label = "عنوان الدين",
                    placeholder = "مثال: سلفة عائلية، قرض شخصي مرن"
                )

                AppInput(
                    value = creditorName,
                    onValueChange = { creditorName = it },
                    label = "اسم الدائن / الشخص"
                )

                AppInput(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = "المبلغ المطلوب سداده (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                Text("تاريخ الاستحقاق النهائي (اختياري):", style = MaterialTheme.typography.labelSmall)
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

                Text("الحساب المرتبط بالخصم (اختياري):", style = MaterialTheme.typography.labelSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "بدون حساب مرتبط"
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
                        accounts.forEach { account ->
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
                    placeholder = "أضف تفاصيل الاتفاق أو الموعد"
                )
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    val total = totalAmount.toDoubleOrNull() ?: 0.0
                    if (title.isBlank() || creditorName.isBlank()) {
                        Toast.makeText(context, "الرجاء ملء اسم الدين والدائن", Toast.LENGTH_SHORT).show()
                    } else if (totalAmount.isBlank() || total <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال مبلغ دين صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(
                            title.trim(),
                            creditorName.trim(),
                            total,
                            selectedAccountId,
                            notes.ifBlank { null },
                            color,
                            dueDate
                        )
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
                onClick = onDismissRequest,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInstallmentDebtDialog(
    accounts: List<Account>,
    onDismissRequest: () -> Unit,
    onConfirm: (
        title: String,
        creditorName: String,
        totalAmount: Double,
        minimumPayment: Double,
        paymentFrequency: String,
        linkedAccountId: Long?,
        priority: Int,
        notes: String?,
        color: String,
        interestRate: Double,
        dueDate: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var title by remember { mutableStateOf("") }
    var creditorName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("0.0") }
    var minimumPayment by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("3") }
    var notes by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#EF4444") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && selectedAccountId == null) {
            selectedAccountId = accounts.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("تسجيل قرض / التزام مقسط جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                AppInput(
                    value = title,
                    onValueChange = { title = it },
                    label = "عنوان القرض / الالتزام",
                    placeholder = "مثال: قرض السيارة، تمويل شراء منزل"
                )

                AppInput(
                    value = creditorName,
                    onValueChange = { creditorName = it },
                    label = "اسم الدائن / البنك"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppInput(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = "المبلغ الإجمالي (د.ج)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
                        modifier = Modifier.weight(1.5f)
                    )

                    AppInput(
                        value = interestRate,
                        onValueChange = { interestRate = it },
                        label = "نسبة الفائدة %",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                AppInput(
                    value = minimumPayment,
                    onValueChange = { minimumPayment = it },
                    label = "القسط الشهري المطلوب (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                Text("تاريخ أول قسط / البداية:", style = MaterialTheme.typography.labelSmall)
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
                                text = if (dueDate != null) FormatterUtils.formatDate(dueDate!!) else "اختر التاريخ",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(Icons.Default.Edit, null, tint = TextGray, modifier = Modifier.size(16.dp))
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

                Text("أولوية الاستعجال:", style = MaterialTheme.typography.labelSmall)
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
                                .background(if (isSelected) ExpenseRed else MaterialTheme.colorScheme.surfaceVariant)
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

                Text("الحساب البنكي المرتبط (اختياري):", style = MaterialTheme.typography.labelSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "بدون حساب مرتبط"
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
                        accounts.forEach { account ->
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
                    label = "شروط الضمان / تفاصيل القرض (اختياري)",
                    placeholder = "مثال: معدل الفائدة المتغير، جدول الإهلاك"
                )
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    val total = totalAmount.toDoubleOrNull() ?: 0.0
                    val interest = interestRate.toDoubleOrNull() ?: 0.0
                    val minPay = minimumPayment.toDoubleOrNull() ?: 0.0
                    if (title.isBlank() || creditorName.isBlank()) {
                        Toast.makeText(context, "الرجاء ملء اسم الدين والدائن", Toast.LENGTH_SHORT).show()
                    } else if (totalAmount.isBlank() || total <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال مبلغ دين صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else if (minimumPayment.isBlank() || minPay <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال قسط شهري صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(
                            title.trim(),
                            creditorName.trim(),
                            total,
                            minPay,
                            "Monthly",
                            selectedAccountId,
                            priority.toIntOrNull() ?: 3,
                            notes.ifBlank { null },
                            color,
                            interest,
                            dueDate
                        )
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.DANGER
            ) {
                Text("حفظ القرض", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismissRequest,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordRegularPaymentDialog(
    debt: RegularDebt,
    accounts: List<Account>,
    onDismissRequest: () -> Unit,
    onConfirm: (
        amount: Double,
        note: String?,
        date: Long,
        accountId: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var paymentAmount by remember { mutableStateOf(debt.remainingAmount.toInt().toString()) }
    var paymentNote by remember { mutableStateOf("") }
    var sourceAccountId by remember { mutableStateOf<Long?>(null) }
    var paymentDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && sourceAccountId == null) {
            sourceAccountId = accounts.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("تسديد سلفة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الدين: ${debt.title}", fontWeight = FontWeight.Medium, color = ExpenseRed)
                Text("المتبقي الكلي: ${debt.remainingAmount.toInt()} د.ج", style = MaterialTheme.typography.bodySmall)

                AppInput(
                    value = paymentAmount,
                    onValueChange = { paymentAmount = it },
                    label = "مبلغ السداد المدفوع (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                AppInput(
                    value = paymentNote,
                    onValueChange = { paymentNote = it },
                    label = "ملاحظة / رقم الإيصال"
                )

                Text("تاريخ عملية السداد:", style = MaterialTheme.typography.labelSmall)
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

                Text("الخصم من حساب:", style = MaterialTheme.typography.labelSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == sourceAccountId }?.name ?: "اختر الحساب"
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
                        accounts.forEach { account ->
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
                        onConfirm(amount, paymentNote.ifBlank { null }, paymentDate, sourceAccountId!!)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.DANGER
            ) {
                Text("تسجيل السداد", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismissRequest,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordInstallmentPaymentDialog(
    debt: InstallmentDebt,
    accounts: List<Account>,
    onDismissRequest: () -> Unit,
    onConfirm: (
        amount: Double,
        paymentType: DebtPaymentType,
        note: String?,
        date: Long,
        accountId: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var paymentAmount by remember { mutableStateOf(debt.minimumPayment.toInt().toString()) }
    var paymentType by remember { mutableStateOf(DebtPaymentType.MINIMUM) }
    var paymentNote by remember { mutableStateOf("") }
    var sourceAccountId by remember { mutableStateOf<Long?>(null) }
    var paymentDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && sourceAccountId == null) {
            sourceAccountId = accounts.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("تسديد قسط", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("الدين: ${debt.title}", fontWeight = FontWeight.Medium, color = ExpenseRed)
                Text("المتبقي الكلي: ${debt.remainingAmount.toInt()} د.ج | القسط الأساسي: ${debt.minimumPayment.toInt()} د.ج", style = MaterialTheme.typography.bodySmall)

                Text("نوع السداد / الدفعة:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(DebtPaymentType.MINIMUM to "القسط الشهري الأساسي", DebtPaymentType.EXTRA to "سداد مبكر / إضافي").forEach { (type, label) ->
                        val isSelected = paymentType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ExpenseRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) ExpenseRed else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { 
                                    paymentType = type 
                                    if (type == DebtPaymentType.MINIMUM) {
                                        paymentAmount = debt.minimumPayment.toInt().toString()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontWeight = FontWeight.Bold, color = if (isSelected) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }

                AppInput(
                    value = paymentAmount,
                    onValueChange = { paymentAmount = it },
                    label = "مبلغ القسط (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                AppInput(
                    value = paymentNote,
                    onValueChange = { paymentNote = it },
                    label = "ملاحظة / رقم الإيصال"
                )

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

                Text("الدفع من حساب:", style = MaterialTheme.typography.labelSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == sourceAccountId }?.name ?: "اختر الحساب"
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
                        accounts.forEach { account ->
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
                        onConfirm(amount, paymentType, paymentNote.ifBlank { null }, paymentDate, sourceAccountId!!)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.SUCCESS
            ) {
                Text("تسديد", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismissRequest,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLentDebtDialog(
    accounts: List<Account>,
    onDismissRequest: () -> Unit,
    onConfirm: (
        title: String,
        debtorName: String,
        totalAmount: Double,
        linkedAccountId: Long,
        dueDate: Long?,
        notes: String?,
        color: String
    ) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var title by remember { mutableStateOf("") }
    var debtorName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#10B981") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && selectedAccountId == null) {
            selectedAccountId = accounts.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("تسجيل سلفة للغير (دين لك)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "سيتم خصم مبلغ السلفة تلقائياً من رصيد المحفظة المختارة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                AppInput(
                    value = title,
                    onValueChange = { title = it },
                    label = "عنوان السلفة / الغرض",
                    placeholder = "مثال: مساعدة شراء هاتف، سلفة طارئة"
                )

                AppInput(
                    value = debtorName,
                    onValueChange = { debtorName = it },
                    label = "اسم المستلف / المدين",
                    placeholder = "مثال: الصديق أحمد، الأخ محمد"
                )

                AppInput(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = "مبلغ السلفة (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                Text("المحفظة المخصوم منها المبلغ (إلزامي):", style = MaterialTheme.typography.labelSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccount = accounts.find { it.id == selectedAccountId }
                    val selectedAccountName = if (selectedAccount != null) {
                        "${selectedAccount.name} (رصيد: ${selectedAccount.balance.toInt()} د.ج)"
                    } else "اختر المحفظة"

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
                        accounts.forEach { account ->
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

                Text("موعد الاسترداد المتوقع (اختياري):", style = MaterialTheme.typography.labelSmall)
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
                            Icon(Icons.Default.DateRange, null, tint = com.qdash.ui.theme.IncomeGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dueDate != null) FormatterUtils.formatDate(dueDate!!) else "بدون موعد محدد",
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
                        confirmButtonColor = com.qdash.ui.theme.IncomeGreen
                    )
                }

                AppInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "ملاحظات إضافية (اختياري)",
                    placeholder = "تفاصيل الاتفاق أو طريقة الاسترداد"
                )
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    val total = totalAmount.toDoubleOrNull() ?: 0.0
                    val chosenAcc = accounts.find { it.id == selectedAccountId }
                    if (title.isBlank() || debtorName.isBlank()) {
                        Toast.makeText(context, "الرجاء ملء عنوان السلفة واسم المستلف", Toast.LENGTH_SHORT).show()
                    } else if (totalAmount.isBlank() || total <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال مبلغ سلفة صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else if (selectedAccountId == null) {
                        Toast.makeText(context, "الرجاء اختيار المحفظة المخصوم منها", Toast.LENGTH_SHORT).show()
                    } else if (chosenAcc != null && chosenAcc.balance < total) {
                        Toast.makeText(context, "تنبيه: رصيد المحفظة (${chosenAcc.balance.toInt()} د.ج) أقل من مبلغ السلفة!", Toast.LENGTH_LONG).show()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(title.trim(), debtorName.trim(), total, selectedAccountId!!, dueDate, notes.ifBlank { null }, color)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(title.trim(), debtorName.trim(), total, selectedAccountId!!, dueDate, notes.ifBlank { null }, color)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.SUCCESS
            ) {
                Text("تسجيل وخصم من المحفظة", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismissRequest,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordLentRepaymentDialog(
    debt: Debt,
    accounts: List<Account>,
    onDismissRequest: () -> Unit,
    onConfirm: (amount: Double, note: String?, date: Long, targetAccountId: Long) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var paymentAmount by remember { mutableStateOf("") }
    var paymentNote by remember { mutableStateOf("") }
    var paymentDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var targetAccountId by remember { mutableStateOf<Long?>(debt.linkedAccountId ?: accounts.firstOrNull()?.id) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("تحصيل استرداد سلفة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("السلفة: ${debt.title} (المدين: ${debt.creditorName})", fontWeight = FontWeight.Medium, color = com.qdash.ui.theme.IncomeGreen)
                Text("المتبقي للاسترداد: ${debt.remainingAmount.toInt()} د.ج", style = MaterialTheme.typography.bodySmall)

                AppInput(
                    value = paymentAmount,
                    onValueChange = { paymentAmount = it },
                    label = "المبلغ المسترد (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                AppInput(
                    value = paymentNote,
                    onValueChange = { paymentNote = it },
                    label = "ملاحظة / تفاصيل الاستلام"
                )

                Text("تاريخ الاستلام:", style = MaterialTheme.typography.labelSmall)
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
                            Icon(Icons.Default.DateRange, null, tint = com.qdash.ui.theme.IncomeGreen, modifier = Modifier.size(20.dp))
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
                        confirmButtonColor = com.qdash.ui.theme.IncomeGreen
                    )
                }

                Text("إيداع المبلغ في محفظة:", style = MaterialTheme.typography.labelSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == targetAccountId }?.name ?: "اختر المحفظة"
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
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (رصيد: ${account.balance.toInt()} د.ج)") },
                                onClick = {
                                    targetAccountId = account.id
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
                        Toast.makeText(context, "الرجاء إدخال مبلغ استرداد صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else if (targetAccountId == null) {
                        Toast.makeText(context, "الرجاء تحديد المحفظة المستلمة للمبلغ", Toast.LENGTH_SHORT).show()
                    } else if (amount > debt.remainingAmount) {
                        Toast.makeText(context, "المبلغ المسترد أكبر من قيمة السلفة المتبقية (${debt.remainingAmount.toInt()} د.ج)!", Toast.LENGTH_LONG).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(amount, paymentNote.ifBlank { null }, paymentDate, targetAccountId!!)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.SUCCESS
            ) {
                Text("تأكيد الإيداع في المحفظة", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismissRequest,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
