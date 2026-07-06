package com.qdash.presentation.debt.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.qdash.domain.model.Account
import com.qdash.domain.model.Debt
import com.qdash.domain.model.DebtType
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDebtBottomSheet(
    debt: Debt,
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
        interestRate: Double?,
        dueDate: Long?,
        debtType: DebtType
    ) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
    val borderColor = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight
    val fieldBgColor = if (isDark) ColorTokens.CardDark else ColorTokens.SurfaceLight

    // State pre-populated with debt values
    var title by remember { mutableStateOf(debt.title) }
    var creditorName by remember { mutableStateOf(debt.creditorName) }
    var totalAmount by remember { mutableStateOf(debt.totalAmount.toInt().toString()) }
    var interestRate by remember { mutableStateOf((debt.interestRate ?: 0.0).toString()) }
    var minimumPayment by remember { mutableStateOf(debt.minimumPayment.toInt().toString()) }
    var priority by remember { mutableStateOf(debt.priority.toString()) }
    var notes by remember { mutableStateOf(debt.notes ?: "") }
    var selectedColor by remember { mutableStateOf(debt.color) }
    var selectedAccountId by remember { mutableStateOf<Long?>(debt.linkedAccountId) }
    var dueDate by remember { mutableStateOf<Long?>(debt.dueDate) }
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "تعديل تفاصيل الالتزام المالي",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

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

                if (debt.debtType == DebtType.REGULAR) {
                    AppInput(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = "المبلغ (د.ج)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                    )

                    // Due Date Selector
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
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(if (isDark) ColorTokens.ElevatedSurfaceDark else MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون حساب مرتبط", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                            onClick = {
                                selectedAccountId = null
                                expanded = false
                            }
                        )
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (رصيد: ${account.balance.toInt()} د.ج)", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                onClick = {
                                    selectedAccountId = account.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Color Picker
                Text("اللون التعريفي التوضيحي:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { hexColor ->
                        val parsedColor = Color(android.graphics.Color.parseColor(hexColor))
                        val isSelected = selectedColor.uppercase() == hexColor.uppercase()
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hexColor }
                        )
                    }
                }

                AppInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "ملاحظات إضافية (اختياري)",
                    placeholder = "مثال: شروط الدفع، تفاصيل الضمان"
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.FLAT,
                        intent = ButtonIntent.PRIMARY
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }

                    AppButton(
                        onClick = {
                            val total = totalAmount.toDoubleOrNull() ?: 0.0
                            val minPay = minimumPayment.toDoubleOrNull() ?: 0.0
                            val interest = interestRate.toDoubleOrNull() ?: 0.0

                            if (title.isBlank() || creditorName.isBlank()) {
                                Toast.makeText(context, "الرجاء ملء اسم الدين والدائن", Toast.LENGTH_SHORT).show()
                            } else if (totalAmount.isBlank() || total <= 0.0) {
                                Toast.makeText(context, "الرجاء إدخال مبلغ دين صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                            } else if (debt.debtType == DebtType.INSTALLMENT && (minimumPayment.isBlank() || minPay <= 0.0)) {
                                Toast.makeText(context, "الرجاء إدخال قسط شهري صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                            } else {
                                onConfirm(
                                    title,
                                    creditorName,
                                    total,
                                    if (debt.debtType == DebtType.REGULAR) 0.0 else minPay,
                                    if (debt.debtType == DebtType.REGULAR) "NONE" else debt.paymentFrequency,
                                    selectedAccountId,
                                    if (debt.debtType == DebtType.REGULAR) 3 else (priority.toIntOrNull() ?: 3),
                                    notes.ifBlank { null },
                                    selectedColor,
                                    if (debt.debtType == DebtType.REGULAR) null else interest,
                                    dueDate,
                                    debt.debtType
                                )
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.DANGER
                    ) {
                        Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
