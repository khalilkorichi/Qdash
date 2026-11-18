package com.qdash.presentation.debt.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.core.utils.ThousandsSeparatorTransformation
import com.qdash.domain.model.Account
import com.qdash.domain.model.DebtDirection
import com.qdash.domain.model.InstallmentDebt
import com.qdash.domain.model.RegularDebt
import com.qdash.presentation.components.safeParseColor
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRegularDebtBottomSheet(
    debt: RegularDebt,
    accounts: List<Account>,
    onConfirm: (
        title: String,
        creditorName: String,
        totalAmount: Double,
        linkedAccountId: Long?,
        notes: String?,
        color: String,
        dueDate: Long?
    ) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val isLent = debt.direction == DebtDirection.OWED_TO_ME
    val requiresAccount = isLent && debt.initialTransactionId != null
    val accentColor = if (isLent) IncomeGreen else MaterialTheme.colorScheme.primary

    var title by remember(debt.id) { mutableStateOf(debt.title) }
    var creditorName by remember(debt.id) { mutableStateOf(debt.creditorName) }
    var totalAmount by remember(debt.id) { mutableStateOf(debt.totalAmount.toInt().toString()) }
    var notes by remember(debt.id) { mutableStateOf(debt.notes ?: "") }
    var selectedColor by remember(debt.id) { mutableStateOf(debt.color) }
    var selectedAccountId by remember(debt.id) { mutableStateOf<Long?>(debt.linkedAccountId) }
    var dueDate by remember(debt.id) { mutableStateOf<Long?>(debt.dueDate) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    val colorOptions = listOf("#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6", "#EC4899")

    AppBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isLent) "تعديل تفاصيل السلفة" else "تعديل تفاصيل الالتزام المرن",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right
                )

                AppInput(
                    value = title,
                    onValueChange = { title = it },
                    label = if (isLent) "عنوان السلفة" else "عنوان الدين"
                )

                AppInput(
                    value = creditorName,
                    onValueChange = { creditorName = it },
                    label = if (isLent) "اسم المدين" else "اسم الدائن / الشخص"
                )

                AppInput(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = if (isLent) "مبلغ السلفة الإجمالي (د.ج)" else "المبلغ المطلوب (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation()
                )

                Text(
                    text = if (isLent) "موعد الاسترداد المتوقع (اختياري):" else "تاريخ الاستحقاق النهائي (اختياري):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.FLAT,
                    onClick = { showDueDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dueDate != null) FormatterUtils.formatDate(dueDate!!) else "بدون تاريخ محدد",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (dueDate != null) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح التاريخ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { dueDate = null }
                            )
                        } else {
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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
                        confirmButtonColor = accentColor
                    )
                }

                Text(
                    text = if (isLent) "المحفظة المخصوم منها مبلغ السلفة:" else "الحساب المرتبط بالسداد (اختياري):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "بدون حساب مرتبط"
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = CardVariant.FLAT,
                        onClick = { expanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedAccountName, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (!requiresAccount) {
                            DropdownMenuItem(
                                text = { Text("بدون حساب مرتبط") },
                                onClick = {
                                    selectedAccountId = null
                                    expanded = false
                                }
                            )
                        }
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (رصيد: ${FormatterUtils.formatCurrency(account.balance)})") },
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
                    placeholder = if (isLent) "تفاصيل الاتفاق أو طريقة الاسترداد" else "مثال: شروط الاتفاق أو الموعد"
                )

                Text(
                    text = "اللون التمييزي:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(colorOptions) { colorHex ->
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(safeParseColor(colorHex, accentColor))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AppButton(
                    onClick = {
                        val amount = totalAmount.toDoubleOrNull() ?: 0.0
                        if (title.isBlank() || creditorName.isBlank()) {
                            val errMsg = if (isLent) "الرجاء ملء عنوان السلفة واسم المدين" else "الرجاء ملء عنوان الدين واسم الدائن"
                            Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                        } else if (totalAmount.isBlank() || amount <= 0.0) {
                            Toast.makeText(context, "الرجاء إدخال مبلغ صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                        } else {
                            onConfirm(
                                title.trim(),
                                creditorName.trim(),
                                amount,
                                selectedAccountId,
                                notes.ifBlank { null },
                                selectedColor,
                                dueDate
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.SOLID,
                    intent = if (isLent) ButtonIntent.SUCCESS else ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg
                ) {
                    Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInstallmentDebtBottomSheet(
    debt: InstallmentDebt,
    accounts: List<Account>,
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
    ) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current

    var title by remember(debt.id) { mutableStateOf(debt.title) }
    var creditorName by remember(debt.id) { mutableStateOf(debt.creditorName) }
    var totalAmount by remember(debt.id) { mutableStateOf(debt.totalAmount.toInt().toString()) }
    var interestRate by remember(debt.id) { mutableStateOf(debt.interestRate.toString()) }
    var minimumPayment by remember(debt.id) { mutableStateOf(debt.minimumPayment.toInt().toString()) }
    var priority by remember(debt.id) { mutableStateOf(debt.priority.toString()) }
    var notes by remember(debt.id) { mutableStateOf(debt.notes ?: "") }
    var selectedColor by remember(debt.id) { mutableStateOf(debt.color) }
    var selectedAccountId by remember(debt.id) { mutableStateOf<Long?>(debt.linkedAccountId) }
    var dueDate by remember(debt.id) { mutableStateOf<Long?>(debt.dueDate) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    val colorOptions = listOf("#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6", "#EC4899")

    AppBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "تعديل تفاصيل القرض المقسط",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right
                )

                AppInput(
                    value = title,
                    onValueChange = { title = it },
                    label = "عنوان القرض"
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
                        visualTransformation = ThousandsSeparatorTransformation(),
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
                    visualTransformation = ThousandsSeparatorTransformation()
                )

                Text(
                    text = "تاريخ القسط القادم (اختياري):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.FLAT,
                    onClick = { showDueDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dueDate != null) FormatterUtils.formatDate(dueDate!!) else "اختر التاريخ",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (dueDate != null) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح التاريخ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { dueDate = null }
                            )
                        } else {
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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
                        }
                    )
                }

                Text(
                    text = "أولوية السداد:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { priority = id }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Text(
                    text = "الحساب البنكي المرتبط (اختياري):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "بدون حساب مرتبط"
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = CardVariant.FLAT,
                        onClick = { expanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedAccountName, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                text = { Text("${account.name} (رصيد: ${FormatterUtils.formatCurrency(account.balance)})") },
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
                    placeholder = "مثال: معدل الفائدة، شروط السداد المبكر"
                )

                Text(
                    text = "اللون التمييزي:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(colorOptions) { colorHex ->
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(safeParseColor(colorHex, ExpenseRed))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AppButton(
                    onClick = {
                        val amount = totalAmount.toDoubleOrNull() ?: 0.0
                        val minPay = minimumPayment.toDoubleOrNull() ?: 0.0
                        val interest = interestRate.toDoubleOrNull() ?: 0.0
                        if (title.isBlank() || creditorName.isBlank()) {
                            Toast.makeText(context, "الرجاء ملء عنوان القرض واسم الدائن", Toast.LENGTH_SHORT).show()
                        } else if (totalAmount.isBlank() || amount <= 0.0) {
                            Toast.makeText(context, "الرجاء إدخال مبلغ صالح", Toast.LENGTH_SHORT).show()
                        } else if (minimumPayment.isBlank() || minPay <= 0.0) {
                            Toast.makeText(context, "الرجاء إدخال قسط شهري صالح", Toast.LENGTH_SHORT).show()
                        } else {
                            onConfirm(
                                title.trim(),
                                creditorName.trim(),
                                amount,
                                minPay,
                                debt.paymentFrequency.ifBlank { "MONTHLY" },
                                selectedAccountId,
                                priority.toIntOrNull() ?: 3,
                                notes.ifBlank { null },
                                selectedColor,
                                interest,
                                dueDate
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg
                ) {
                    Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
