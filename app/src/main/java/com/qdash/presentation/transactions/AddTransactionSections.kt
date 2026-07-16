package com.qdash.presentation.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.model.TransactionType
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.core.utils.FormatterUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(
    selectedTags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    typeAccentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SectionLabel(text = "الوسوم (Tags)")
        Spacer(modifier = Modifier.height(4.dp))
        
        if (selectedTags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                selectedTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeAccentColor.copy(alpha = 0.12f))
                            .border(
                                width = 0.5.dp,
                                color = typeAccentColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onTagsChanged(selectedTags - tag) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = typeAccentColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "حذف",
                                tint = typeAccentColor,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val presetTags = listOf("غذاء", "بنزين", "فاتورة", "سفر", "هدايا", "صحة", "ملابس", "إنترنت")
        val availablePresets = presetTags.filter { it !in selectedTags }
        
        Text(
            text = "وسوم مقترحة:",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            availablePresets.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { onTagsChanged(selectedTags + tag) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            var showCustomTagInput by remember { mutableStateOf(false) }
            var customTagText by remember { mutableStateOf("") }
            
            if (showCustomTagInput) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicTextField(
                        value = customTagText,
                        onValueChange = { customTagText = it },
                        textStyle = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .width(80.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            val trimmed = customTagText.trim().replace("#", "")
                            if (trimmed.isNotEmpty() && trimmed !in selectedTags) {
                                onTagsChanged(selectedTags + trimmed)
                            }
                            customTagText = ""
                            showCustomTagInput = false
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "إضافة",
                            tint = typeAccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            customTagText = ""
                            showCustomTagInput = false
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء",
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(typeAccentColor.copy(alpha = 0.08f))
                        .clickable { showCustomTagInput = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ وسم مخصص",
                        style = MaterialTheme.typography.labelSmall,
                        color = typeAccentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetWarningCard(
    activeBudget: BudgetGoal?,
    inputAmount: Double,
    modifier: Modifier = Modifier
) {
    activeBudget?.let { budget ->
        if (inputAmount > 0.0) {
            val newSpent = budget.spentAmount + inputAmount
            val remaining = budget.amountLimit - budget.spentAmount
            val isExceeded = newSpent > budget.amountLimit
            val isWarning = !isExceeded && newSpent >= (budget.amountLimit * 0.90)
            
            if (isExceeded || isWarning) {
                val remainingFormatted = FormatterUtils.formatCurrency(remaining.coerceAtLeast(0.0))
                val limitFormatted = FormatterUtils.formatCurrency(budget.amountLimit)
                Card(
                    modifier = modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExceeded) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        } else {
                            SavingsAmber.copy(alpha = 0.15f)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isExceeded) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else SavingsAmber.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isExceeded) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isExceeded) MaterialTheme.colorScheme.error else SavingsAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (isExceeded) "تنبيه: هذا المبلغ يتجاوز الميزانية المحددة للفئة!" else "انتبه: الميزانية المحددة لهذه الفئة أوشكت على النفاد!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isExceeded) {
                                    "ميزانية الفئة: $limitFormatted (تجاوز بقيمة: ${FormatterUtils.formatCurrency(newSpent - budget.amountLimit)})"
                                } else {
                                    "المتبقي في الميزانية: $remainingFormatted من إجمالي $limitFormatted"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDateSelector(
    transactionDate: Long,
    onDateChanged: (Long) -> Unit,
    typeAccentColor: Color,
    // Precise execution timestamp — null for legacy records or when user hasn't set a time yet.
    occurredAt: Long? = null,
    onOccurredAtChanged: ((Long?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Extract hour/minute from occurredAt (or current time as default for the picker)
    val cal = remember(occurredAt) {
        java.util.Calendar.getInstance().apply {
            timeInMillis = occurredAt ?: System.currentTimeMillis()
        }
    }
    val pickerHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val pickerMinute = cal.get(java.util.Calendar.MINUTE)

    Column(modifier = modifier) {
        SectionLabel(text = "تاريخ العملية")
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { showDatePicker = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = typeAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = FormatterUtils.formatDate(transactionDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "تعديل التاريخ",
                    tint = TextGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val todayStart = remember { getStartOfDay(System.currentTimeMillis()) }
            val yesterdayStart = remember { todayStart - 24 * 60 * 60 * 1000L }
            val isToday = remember(transactionDate) { getStartOfDay(transactionDate) == todayStart }
            val isYesterday = remember(transactionDate) { getStartOfDay(transactionDate) == yesterdayStart }
            val isOther = !isToday && !isYesterday

            QuickDateButton(
                text = "اليوم",
                isSelected = isToday,
                accentColor = typeAccentColor,
                // Preserve day of transactionDate but keep time in occurredAt unchanged
                onClick = { onDateChanged(mergeDateTime(System.currentTimeMillis(), occurredAt)) }
            )

            QuickDateButton(
                text = "البارحة",
                isSelected = isYesterday,
                accentColor = typeAccentColor,
                onClick = { onDateChanged(mergeDateTime(System.currentTimeMillis() - 24 * 60 * 60 * 1000L, occurredAt)) }
            )

            QuickDateButton(
                text = "تاريخ آخر...",
                isSelected = isOther,
                accentColor = typeAccentColor,
                onClick = { showDatePicker = true }
            )
        }

        // Time picker section (only shown if callback provided)
        if (onOccurredAtChanged != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp))
                    .clickable { showTimePicker = true }
                    .testTag("time_selector"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (occurredAt != null)
                        typeAccentColor.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (occurredAt != null) typeAccentColor else TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (occurredAt != null)
                                FormatterUtils.formatTime(occurredAt)
                            else
                                "إضافة وقت (اختياري)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (occurredAt != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                TextGray,
                            fontWeight = if (occurredAt != null) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (occurredAt != null) {
                            // Allow removing the time
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إزالة الوقت",
                                tint = TextGray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onOccurredAtChanged(null) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الوقت",
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialSelectedDateMillis = transactionDate,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                // Preserve time component from occurredAt when date changes
                onDateChanged(mergeDateTime(selectedDate, occurredAt))
            },
            confirmButtonColor = typeAccentColor
        )
    }

    if (showTimePicker && onOccurredAtChanged != null) {
        AppTimePickerDialog(
            initialHour = pickerHour,
            initialMinute = pickerMinute,
            onDismissRequest = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                // Merge selected time into transactionDate to produce occurredAt epoch
                val newOccurredAt = buildOccurredAt(transactionDate, hour, minute)
                onOccurredAtChanged(newOccurredAt)
            },
            confirmButtonColor = typeAccentColor
        )
    }
}

/**
 * Merges the day from [dateMills] with the time-of-day from [timeMills].
 * If [timeMills] is null, returns [dateMills] unchanged.
 * Used to update transactionDate quick-buttons without resetting the user's chosen time.
 */
private fun mergeDateTime(dateMills: Long, timeMills: Long?): Long {
    if (timeMills == null) return dateMills
    val dateCal = java.util.Calendar.getInstance().apply { timeInMillis = dateMills }
    val timeCal = java.util.Calendar.getInstance().apply { timeInMillis = timeMills }
    dateCal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
    dateCal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
    dateCal.set(java.util.Calendar.SECOND, 0)
    dateCal.set(java.util.Calendar.MILLISECOND, 0)
    return dateCal.timeInMillis
}

/**
 * Produces an occurredAt epoch by combining the date component of [dateMills] with
 * the explicit [hour] and [minute] chosen by the user via the time picker.
 */
private fun buildOccurredAt(dateMills: Long, hour: Int, minute: Int): Long {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = dateMills }
    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
    cal.set(java.util.Calendar.MINUTE, minute)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}



@Composable
fun RecurringOptionsSection(
    isRecurring: Boolean,
    onRecurringChanged: (Boolean) -> Unit,
    recurringPeriod: String,
    onPeriodChanged: (String) -> Unit,
    type: TransactionType,
    isSalaryAutomation: Boolean,
    onSalaryAutomationChanged: (Boolean) -> Unit,
    typeAccentColor: Color,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = if (isRecurring) primaryColor else TextGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "عملية متكررة دورياً؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
            }
            Switch(
                checked = isRecurring,
                onCheckedChange = onRecurringChanged,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = primaryColor,
                    checkedThumbColor = Color.White
                )
            )
        }

        if (type == TransactionType.INCOME) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isSalaryAutomation) primaryColor else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل كراتب تلقائي (أتمتة الراتب)؟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = isSalaryAutomation,
                    onCheckedChange = onSalaryAutomationChanged,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = primaryColor,
                        checkedThumbColor = Color.White
                    )
                )
            }
        }

        AnimatedVisibility(visible = isRecurring) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = "وتيرة التكرار",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "DAILY" to "يومياً",
                        "WEEKLY" to "أسبوعياً",
                        "MONTHLY" to "شهرياً",
                        "YEARLY" to "سنوياً"
                    ).forEach { (period, label) ->
                        val isSelected = recurringPeriod == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) typeAccentColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { onPeriodChanged(period) }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) typeAccentColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) typeAccentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getStartOfDay(millis: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
