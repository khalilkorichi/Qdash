package com.qdash.presentation.budgetgoals

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.FormatterUtils
import com.qdash.core.utils.ThousandsSeparatorTransformation
import com.qdash.domain.model.BudgetType
import com.qdash.presentation.components.getIconByName
import com.qdash.presentation.components.safeParseColor
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.AppDatePickerDialog
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.tokens.ShapeTokens
import java.util.Calendar

enum class BudgetPeriodPreset(val label: String) {
    MONTHLY("شهري"),
    WEEKLY("أسبوعي"),
    CUSTOM("مخصص")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetGoalScreen(
    viewModel: BudgetGoalsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var title by remember { mutableStateOf("") }
    var selectedBudgetType by remember { mutableStateOf(BudgetType.GLOBAL) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var amountLimit by remember { mutableStateOf("") }
    var alertThresholdPercent by remember { mutableFloatStateOf(80f) }

    // Period selector state
    var selectedPeriod by remember { mutableStateOf(BudgetPeriodPreset.MONTHLY) }
    var customStartDate by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }
    var customEndDate by remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }
        mutableLongStateOf(cal.timeInMillis)
    }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Selected color and icon defaults
    var selectedColor by remember { mutableStateOf("#6C63FF") }
    var selectedIcon by remember { mutableStateOf("monetization_on") }

    val scrollState = rememberScrollState()

    // Presets
    val colors = listOf("#6C63FF", "#22C55E", "#EF4444", "#F59E0B", "#3B82F6", "#EE5F5B", "#A770EF", "#00B4D8")
    val icons = listOf("person", "groups", "home", "restaurant", "directions_car", "receipt_long", "shopping_bag", "medical_services")

    val applyPreset: (String, BudgetType, String, String, String, BudgetPeriodPreset, String?) -> Unit =
        { pTitle, pType, pColor, pIcon, pAmount, pPeriod, keyword ->
            title = pTitle
            selectedBudgetType = pType
            selectedColor = pColor
            selectedIcon = pIcon
            amountLimit = pAmount
            selectedPeriod = pPeriod
            if (pType == BudgetType.CATEGORY) {
                val expenseCategories = uiState.categories.filter { it.type.name == "EXPENSE" }
                val matchedCategory = (if (keyword != null) {
                    expenseCategories.find { it.name.contains(keyword) }
                } else null)
                    ?: expenseCategories.find { it.name.contains("طعام") || it.name.contains("مواصلات") }
                    ?: expenseCategories.firstOrNull()
                selectedCategoryId = matchedCategory?.id
            } else {
                selectedCategoryId = null
            }
        }

    Scaffold(
        topBar = {
            FinTrackTopBar(
                title = "إنشاء ميزانية تتبع",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Presets Layout (LazyRow so narrow screens never clip)
            Text(
                text = "قوالب ميزانية سريعة",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val presets = listOf(
                    PresetItem("طعام شهري", "30,000 د.ج", "ميزانية الطعام الشهرية", BudgetType.CATEGORY, "#F59E0B", "restaurant", "30000", BudgetPeriodPreset.MONTHLY, "طعام"),
                    PresetItem("مواصلات أسبوعية", "5,000 د.ج", "ميزانية المواصلات الأسبوعية", BudgetType.CATEGORY, "#3B82F6", "directions_car", "5000", BudgetPeriodPreset.WEEKLY, "مواصلات"),
                    PresetItem("منزل مؤقت", "15,000 د.ج", "تجهيز المنزل المؤقت", BudgetType.CUSTOM, "#6C63FF", "home", "15000", BudgetPeriodPreset.MONTHLY, null),
                    PresetItem("إنفاق عام", "60,000 د.ج", "ميزانية الإنفاق العامة", BudgetType.GLOBAL, "#22C55E", "person", "60000", BudgetPeriodPreset.MONTHLY, null)
                )
                items(presets) { preset ->
                    OutlinedCard(
                        modifier = Modifier
                            .widthIn(min = 120.dp)
                            .clickable {
                                applyPreset(
                                    preset.goalTitle,
                                    preset.type,
                                    preset.color,
                                    preset.icon,
                                    preset.amount,
                                    preset.period,
                                    preset.keyword
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.chipTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = preset.chipAmount,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الميزانية") },
                placeholder = { Text("مثال: ميزانية البقالة الشهرية") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Budget Type Row Selector
            Text(text = "نوع الميزانية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetType.entries.forEach { type ->
                    val isSelected = selectedBudgetType == type
                    val label = when (type) {
                        BudgetType.CATEGORY -> "تصنيف محدد"
                        BudgetType.CUSTOM -> "مخصصة"
                        BudgetType.GLOBAL -> "إنفاق إجمالي"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                selectedBudgetType = type
                                if (type != BudgetType.CATEGORY) selectedCategoryId = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Category Picker
            if (selectedBudgetType == BudgetType.CATEGORY) {
                Text(text = "اختر التصنيف المستهدف", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                var expanded by remember { mutableStateOf(false) }
                val selectedCategory = uiState.categories.find { it.id == selectedCategoryId }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = primary)
                        Text(
                            text = selectedCategory?.name ?: "انقر لاختيار الفئة المستهدفة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        uiState.categories.filter { it.type.name == "EXPENSE" }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Period Selector (شهري / أسبوعي / مخصص)
            Text(text = "الفترة الزمنية للميزانية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetPeriodPreset.entries.forEach { preset ->
                    val isSelected = selectedPeriod == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedPeriod = preset }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            if (selectedPeriod == BudgetPeriodPreset.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppCard(
                        modifier = Modifier.weight(1f),
                        variant = CardVariant.FLAT,
                        shape = ShapeTokens.Md,
                        onClick = { showStartDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                            Column {
                                Text("تاريخ البداية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FormatterUtils.formatDate(customStartDate), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    AppCard(
                        modifier = Modifier.weight(1f),
                        variant = CardVariant.FLAT,
                        shape = ShapeTokens.Md,
                        onClick = { showEndDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                            Column {
                                Text("تاريخ النهاية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FormatterUtils.formatDate(customEndDate), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showStartDatePicker) {
                    AppDatePickerDialog(
                        initialSelectedDateMillis = customStartDate,
                        onDismissRequest = { showStartDatePicker = false },
                        onDateSelected = { picked ->
                            val startCal = Calendar.getInstance().apply {
                                timeInMillis = picked
                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }
                            customStartDate = startCal.timeInMillis
                            if (customEndDate <= customStartDate) {
                                val nextCal = (startCal.clone() as Calendar).apply {
                                    add(Calendar.DAY_OF_YEAR, 7)
                                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                                }
                                customEndDate = nextCal.timeInMillis
                            }
                        }
                    )
                }

                if (showEndDatePicker) {
                    AppDatePickerDialog(
                        initialSelectedDateMillis = customEndDate,
                        onDismissRequest = { showEndDatePicker = false },
                        onDateSelected = { picked ->
                            val endCal = Calendar.getInstance().apply {
                                timeInMillis = picked
                                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                            }
                            customEndDate = endCal.timeInMillis
                        }
                    )
                }
            }

            // Amount limit
            OutlinedTextField(
                value = amountLimit,
                onValueChange = { amountLimit = it },
                label = { Text("المبلغ الأقصى للميزانية (د.ج)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandsSeparatorTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Alert Threshold Percent
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "حد تنبيه الإنفاق الذكي", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = "${alertThresholdPercent.toInt()}% من الإجمالي", style = MaterialTheme.typography.bodyMedium, color = primary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = alertThresholdPercent,
                    onValueChange = { alertThresholdPercent = it },
                    valueRange = 50f..100f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        activeTrackColor = primary,
                        thumbColor = primary
                    )
                )
            }

            // Color Selector (LazyRow so 360dp screens never clip)
            Text(text = "اختر لون الرمز", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(colors) { hex ->
                    val colorVal = safeParseColor(hex, primary)
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorVal)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Icon Selector (LazyRow so 360dp screens never clip)
            Text(text = "اختر الرمز المرئي", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(icons) { iconName ->
                    val isSelected = selectedIcon == iconName
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { selectedIcon = iconName },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(iconName),
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val limitDouble = amountLimit.toDoubleOrNull() ?: 0.0
                    val nowCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val (resolvedStart, resolvedEnd) = when (selectedPeriod) {
                        BudgetPeriodPreset.WEEKLY -> {
                            val start = nowCal.timeInMillis
                            val endCal = (nowCal.clone() as Calendar).apply {
                                add(Calendar.DAY_OF_YEAR, 7)
                                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                            }
                            start to endCal.timeInMillis
                        }
                        BudgetPeriodPreset.MONTHLY -> {
                            val start = nowCal.timeInMillis
                            val endCal = (nowCal.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                            }
                            start to endCal.timeInMillis
                        }
                        BudgetPeriodPreset.CUSTOM -> customStartDate to customEndDate
                    }

                    when {
                        title.isBlank() -> {
                            Toast.makeText(context, "الرجاء إدخال عنوان الميزانية", Toast.LENGTH_SHORT).show()
                        }
                        amountLimit.isBlank() || limitDouble <= 0.0 -> {
                            Toast.makeText(context, "الرجاء إدخال مبلغ ميزانية صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                        }
                        selectedBudgetType == BudgetType.CATEGORY && selectedCategoryId == null -> {
                            Toast.makeText(context, "الرجاء اختيار تصنيف مستهدف", Toast.LENGTH_SHORT).show()
                        }
                        resolvedEnd <= resolvedStart -> {
                            Toast.makeText(context, "تاريخ النهاية يجب أن يكون بعد تاريخ البداية", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addBudgetGoal(
                                title = title.trim(),
                                linkedCategoryId = selectedCategoryId,
                                budgetType = selectedBudgetType,
                                amountLimit = limitDouble,
                                startDate = resolvedStart,
                                endDate = resolvedEnd,
                                alertThresholdPercent = alertThresholdPercent.toInt(),
                                color = selectedColor,
                                icon = selectedIcon
                            )
                            onBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "حفظ الميزانية الذكية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class PresetItem(
    val chipTitle: String,
    val chipAmount: String,
    val goalTitle: String,
    val type: BudgetType,
    val color: String,
    val icon: String,
    val amount: String,
    val period: BudgetPeriodPreset,
    val keyword: String?
)
