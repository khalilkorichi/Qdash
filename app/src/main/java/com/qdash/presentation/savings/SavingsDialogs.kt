package com.qdash.presentation.savings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import com.qdash.domain.model.SavingGoal
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.theme.*

@Composable
fun AddSavingsGoalDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetAmount: Double, accountId: Long, color: String, strategy: String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var goalName by remember { mutableStateOf("") }
    var goalTarget by remember { mutableStateOf("") }
    var goalColor by remember { mutableStateOf("#4CAF50") }
    var goalStrategy by remember { mutableStateOf("manual") }
    var selectedAccountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء هدف ادخاري جديد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppInput(
                    value = goalName,
                    onValueChange = { goalName = it },
                    label = "اسم الهدف الادخاري",
                    placeholder = "مثال: سيارة جديدة، صندوق طوارئ"
                )

                AppInput(
                    value = goalTarget,
                    onValueChange = { goalTarget = it },
                    label = "المبلغ المستهدف (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                // Associated Account Selector
                Text("ربط الهدف بحساب تمويل أساسي:", style = MaterialTheme.typography.labelSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "اختر الحساب"
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
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccountId = account.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Savings Strategy
                Text("خطة الادخار المقترحة:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("manual", "يدوي", SavingsAmber),
                        Triple("monthly", "شهري", TransferBlue),
                        Triple("weekly", "أسبوعي", IncomeGreen)
                    ).forEach { (id, label, color) ->
                        val isSelected = goalStrategy == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) color
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { goalStrategy = id }
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

                // Accent Colors selection
                Text("اللون المميز للمشروع الادخاري:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0").forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(color, CircleShape)
                                .clip(CircleShape)
                                .clickable { goalColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (goalColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    val target = FormatterUtils.normalizeAmount(goalTarget).toDoubleOrNull() ?: 0.0
                    if (goalName.isBlank()) {
                        Toast.makeText(context, "الرجاء إدخال اسم الهدف الادخاري", Toast.LENGTH_SHORT).show()
                    } else if (goalTarget.isBlank() || target <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال مبلغ مستهدف صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else if (selectedAccountId == null) {
                        Toast.makeText(context, "الرجاء تحديد حساب التمويل الأساسي", Toast.LENGTH_SHORT).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(goalName.trim(), target, selectedAccountId!!, goalColor, goalStrategy)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("تأكيد الإنشاء", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun EditSavingsGoalDialog(
    goal: SavingGoal,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetAmount: Double, color: String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var goalName by remember { mutableStateOf(goal.name) }
    var goalTarget by remember { mutableStateOf(goal.targetAmount.toInt().toString()) }
    var goalColor by remember { mutableStateOf(goal.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل الهدف الادخاري", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppInput(
                    value = goalName,
                    onValueChange = { goalName = it },
                    label = "اسم الهدف الادخاري"
                )

                AppInput(
                    value = goalTarget,
                    onValueChange = { goalTarget = it },
                    label = "المبلغ المستهدف (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                // Accent colors Selection
                Text("تغيير اللون المميز:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0").forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(color, CircleShape)
                                .clickable { goalColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (goalColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    val target = FormatterUtils.normalizeAmount(goalTarget).toDoubleOrNull() ?: 0.0
                    if (goalName.isBlank()) {
                        Toast.makeText(context, "الرجاء إدخال اسم الهدف الادخاري", Toast.LENGTH_SHORT).show()
                    } else if (goalTarget.isBlank() || target <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال مبلغ مستهدف صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(goalName.trim(), target, goalColor)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("حفظ التغييرات", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
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
fun SavingsDepositDialog(
    goal: SavingGoal,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String?, accountId: Long, date: Long) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var contributionAmount by remember { mutableStateOf("") }
    var contributionNote by remember { mutableStateOf("") }
    var selectedContribAccountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
    var contributionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showContribDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تخصيص مدخرات وإيداع رصيد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الهدف: ${goal.name}", fontWeight = FontWeight.Medium, color = SavingsAmber)
                
                AppInput(
                    value = contributionAmount,
                    onValueChange = { contributionAmount = it },
                    label = "مبلغ الإيداع (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                AppInput(
                    value = contributionNote,
                    onValueChange = { contributionNote = it },
                    label = "ملاحظات (اختياري)",
                    placeholder = "مثال: من علاوة هذا الشهر"
                )

                // --- DATE SELECTOR CARD ---
                Text("تاريخ عملية الإيداع:", style = MaterialTheme.typography.labelSmall)
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.FLAT,
                    onClick = { showContribDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null, tint = SavingsAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = FormatterUtils.formatDate(contributionDate),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(Icons.Default.Edit, null, tint = TextGray, modifier = Modifier.size(16.dp))
                    }
                }

                if (showContribDatePicker) {
                    AppDatePickerDialog(
                        initialSelectedDateMillis = contributionDate,
                        onDismissRequest = { showContribDatePicker = false },
                        onDateSelected = { contributionDate = it },
                        confirmButtonColor = SavingsAmber
                    )
                }

                // Source account selector
                Text("حساب مصدر الأموال:", style = MaterialTheme.typography.labelSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == selectedContribAccountId }?.name ?: "اختر الحساب"
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
                                    selectedContribAccountId = account.id
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
                    val amount = FormatterUtils.normalizeAmount(contributionAmount).toDoubleOrNull() ?: 0.0
                    if (contributionAmount.isBlank() || amount <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال مبلغ إيداع صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else if (selectedContribAccountId == null) {
                        Toast.makeText(context, "الرجاء تحديد حساب مصدر الأموال", Toast.LENGTH_SHORT).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(amount, contributionNote.ifBlank { null }, selectedContribAccountId!!, contributionDate)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إيداع الآن", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
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
fun SavingsWithdrawDialog(
    goal: SavingGoal,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String?, accountId: Long, date: Long) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var contributionAmount by remember { mutableStateOf("") }
    var contributionNote by remember { mutableStateOf("") }
    var selectedContribAccountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
    var contributionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showContribDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سحب من حصالة الادخار", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الهدف: ${goal.name}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                Text("المبلغ المدخر المتوفر: ${goal.currentAmount.toInt()} د.ج", style = MaterialTheme.typography.bodySmall)

                AppInput(
                    value = contributionAmount,
                    onValueChange = { contributionAmount = it },
                    label = "مبلغ السحب (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                AppInput(
                    value = contributionNote,
                    onValueChange = { contributionNote = it },
                    label = "السبب والسحب إلى",
                    placeholder = "مثال: لتغطية ظرف طارئ"
                )

                // --- DATE SELECTOR CARD ---
                Text("تاريخ عملية السحب:", style = MaterialTheme.typography.labelSmall)
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.FLAT,
                    onClick = { showContribDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = FormatterUtils.formatDate(contributionDate),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(Icons.Default.Edit, null, tint = TextGray, modifier = Modifier.size(16.dp))
                    }
                }

                if (showContribDatePicker) {
                    AppDatePickerDialog(
                        initialSelectedDateMillis = contributionDate,
                        onDismissRequest = { showContribDatePicker = false },
                        onDateSelected = { contributionDate = it },
                        confirmButtonColor = MaterialTheme.colorScheme.error
                    )
                }

                // Target account selector
                Text("استقبال رصيد السحب في حساب:", style = MaterialTheme.typography.labelSmall)
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedAccountName = accounts.find { it.id == selectedContribAccountId }?.name ?: "اختر الحساب"
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
                                    selectedContribAccountId = account.id
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
                    val amount = FormatterUtils.normalizeAmount(contributionAmount).toDoubleOrNull() ?: 0.0
                    if (contributionAmount.isBlank() || amount <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال مبلغ سحب صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else if (selectedContribAccountId == null) {
                        Toast.makeText(context, "الرجاء تحديد الحساب المستقبل لرصيد السحب", Toast.LENGTH_SHORT).show()
                    } else if (amount > goal.currentAmount) {
                        Toast.makeText(context, "المبلغ المراد سحبه أكبر من الرصيد المتوفر في الحصالة (${goal.currentAmount.toInt()} د.ج)!", Toast.LENGTH_LONG).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(amount, contributionNote.ifBlank { null }, selectedContribAccountId!!, contributionDate)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.DANGER
            ) {
                Text("سحب الأموال", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
