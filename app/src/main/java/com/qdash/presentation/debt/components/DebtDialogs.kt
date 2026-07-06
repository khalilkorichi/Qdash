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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.components.AppDatePickerDialog
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.Debt
import com.qdash.domain.model.DebtPaymentType
import com.qdash.domain.model.DebtType
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(
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
        interestRate: Double?,
        dueDate: Long?,
        debtType: DebtType
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
    var debtType by remember { mutableStateOf(DebtType.REGULAR) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && selectedAccountId == null) {
            selectedAccountId = accounts.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("تسجيل التزام مالي / دين جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
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
                }

                Text("الحساب المرتبط (اختياري):", style = MaterialTheme.typography.labelSmall)
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
                        onConfirm(
                            title.trim(),
                            creditorName.trim(),
                            total,
                            if (debtType == DebtType.REGULAR) 0.0 else minPay,
                            "Monthly",
                            selectedAccountId,
                            if (debtType == DebtType.REGULAR) 3 else (priority.toIntOrNull() ?: 3),
                            notes.ifBlank { null },
                            color,
                            if (debtType == DebtType.REGULAR) null else interestRate.toDoubleOrNull(),
                            dueDate,
                            debtType
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
fun RecordPaymentDialog(
    debt: Debt,
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

    var paymentAmount by remember { mutableStateOf(
        if (debt.debtType == DebtType.REGULAR) debt.remainingAmount.toInt().toString() else debt.minimumPayment.toInt().toString()
    ) }
    val paymentType by remember { mutableStateOf(DebtPaymentType.MANUAL) }
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
                intent = ButtonIntent.DANGER
            ) {
                Text("تسجيل سداد", color = Color.White, fontWeight = FontWeight.Bold)
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
