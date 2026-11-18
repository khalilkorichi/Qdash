package com.qdash.presentation.transactions.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.components.AppDatePickerDialog
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.theme.ExpenseRed
import java.util.Calendar

private fun startOfDayMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun endOfDayMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFiltersBottomSheet(
    filterMinAmount: Double?,
    filterStartDate: Long?,
    filterEndDate: Long?,
    onApplyFilters: (minAmount: Double?, startDate: Long?, endDate: Long?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempMinAmount by remember { mutableStateOf(filterMinAmount?.toString() ?: "") }
    var tempStartDate by remember { mutableStateOf(filterStartDate) }
    var tempEndDate by remember { mutableStateOf(filterEndDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = filterSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "تصفية متقدمة للمعاملات",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            // 1. Min Amount Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "مبالغ أكبر من أو تساوي (دج):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tempMinAmount,
                    onValueChange = { tempMinAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("مثال: 1000", textAlign = TextAlign.Start) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 2. Date Range Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "النطاق الزمني:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Date Button (First in RTL = Right)
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = tempStartDate?.let { FormatterUtils.formatDate(it) } ?: "تاريخ البداية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // End Date Button (Second in RTL = Left)
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = tempEndDate?.let { FormatterUtils.formatDate(it) } ?: "تاريخ النهاية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reset Button
                OutlinedButton(
                    onClick = {
                        tempMinAmount = ""
                        tempStartDate = null
                        tempEndDate = null
                        onClearFilters()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("إعادة تعيين", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }

                // Apply Button
                Button(
                    onClick = {
                        val minVal = AmountConversionEngine.parseFlexibleAmount(tempMinAmount)
                        val rawStart = tempStartDate
                        val rawEnd = tempEndDate
                        val (orderedStart, orderedEnd) = if (rawStart != null && rawEnd != null && rawStart > rawEnd) {
                            Pair(rawEnd, rawStart)
                        } else {
                            Pair(rawStart, rawEnd)
                        }
                        val normalizedStart = orderedStart?.let { startOfDayMillis(it) }
                        val normalizedEnd = orderedEnd?.let { endOfDayMillis(it) }
                        onApplyFilters(minVal, normalizedStart, normalizedEnd)
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("تطبيق الفلتر", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // DatePickers Dialogs
    if (showStartDatePicker) {
        AppDatePickerDialog(
            initialSelectedDateMillis = tempStartDate,
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { tempStartDate = it },
            confirmButtonColor = primaryColor
        )
    }

    if (showEndDatePicker) {
        AppDatePickerDialog(
            initialSelectedDateMillis = tempEndDate,
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { tempEndDate = it },
            confirmButtonColor = primaryColor
        )
    }
}
