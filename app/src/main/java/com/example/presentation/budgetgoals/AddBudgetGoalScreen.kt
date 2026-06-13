package com.example.presentation.budgetgoals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.BudgetType
import com.example.presentation.components.getIconByName
import com.example.ui.theme.Primary
import com.example.ui.theme.TextGray
import java.util.Calendar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetGoalScreen(
    viewModel: BudgetGoalsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    var title by remember { mutableStateOf("") }
    var selectedBudgetType by remember { mutableStateOf(BudgetType.GLOBAL) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var amountLimit by remember { mutableStateOf("") }
    var alertThresholdPercent by remember { mutableStateOf(80f) }

    // Selected color and icon defaults
    var selectedColor by remember { mutableStateOf("#6C63FF") }
    var selectedIcon by remember { mutableStateOf("monetization_on") }

    val scrollState = rememberScrollState()

    // Presets
    val colors = listOf("#6C63FF", "#22C55E", "#EF4444", "#F59E0B", "#3B82F6", "#EE5F5B", "#A770EF", "#00F2FE")
    val icons = listOf("person", "groups", "home", "restaurant", "directions_car", "receipt_long", "shopping_bag", "medical_services")

    // Handle Quick Presets Action
    val applyPreset: (String, BudgetType, String, String, String) -> Unit = { pTitle, pType, pColor, pIcon, pAmount ->
        title = pTitle
        selectedBudgetType = pType
        selectedColor = pColor
        selectedIcon = pIcon
        amountLimit = pAmount
        if (pType == BudgetType.CATEGORY) {
            val matchedCategory = uiState.categories.find { it.name.trim() == "طعام" || it.name.trim() == "مواصلات" }
            selectedCategoryId = matchedCategory?.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إنشاء ميزانية تتبع") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Presets Layout
            Text(text = "قوالب ميزانية سريعة", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            applyPreset("ميزانية الطعام الشهرية", BudgetType.CATEGORY, "#F59E0B", "restaurant", "30000")
                        },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "طعام شهري", style = MaterialTheme.typography.labelMedium)
                        Text(text = "30,000 د.ج", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            applyPreset("تجهيز الغرفة المؤقت", BudgetType.CUSTOM, "#6C63FF", "home", "15000")
                        },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "منزل مؤقت", style = MaterialTheme.typography.labelMedium)
                        Text(text = "15,000 د.ج", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            applyPreset("ميزانية الإنفاق العامة", BudgetType.GLOBAL, "#22C55E", "person", "60000")
                        },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "إنفاق عام", style = MaterialTheme.typography.labelMedium)
                        Text(text = "60,000 د.ج", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                }
            }

            HorizontalDivider()

            // Form Fields
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الميزانية") },
                placeholder = { Text("مثال: ميزانية البقالة الشهرية") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Budget Type Row Selector
            Text(text = "نوع الميزانية", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetType.values().forEach { type ->
                    val isSelected = selectedBudgetType == type
                    val label = when (type) {
                        BudgetType.CATEGORY -> "تصنيف محدد"
                        BudgetType.CUSTOM -> "مخصصة مؤقتة"
                        BudgetType.GLOBAL -> "إنفاق إجمالي"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                selectedBudgetType = type
                                if (type != BudgetType.CATEGORY) selectedCategoryId = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Category Picker
            if (selectedBudgetType == BudgetType.CATEGORY) {
                Text(text = "اختر التصنيف المستهدف", style = MaterialTheme.typography.titleSmall)
                var expanded by remember { mutableStateOf(false) }
                val selectedCategory = uiState.categories.find { it.id == selectedCategoryId }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expanded = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = Primary)
                            Text(
                                text = selectedCategory?.name ?: "انقر لاختيار الفئة المستهدفة",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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

            // Amount limit
            OutlinedTextField(
                value = amountLimit,
                onValueChange = { amountLimit = it },
                label = { Text("المبلغ الأقصى للميزانية (د.ج)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Alert Threshold Percent
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "حد تنبيه الإنفاق الذكي", style = MaterialTheme.typography.titleSmall)
                    Text(text = "${alertThresholdPercent.toInt()}% من الإجمالي", style = MaterialTheme.typography.bodyMedium, color = Primary)
                }
                Slider(
                    value = alertThresholdPercent,
                    onValueChange = { alertThresholdPercent = it },
                    valueRange = 50f..100f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Primary,
                        thumbColor = Primary
                    )
                )
            }

            // Color Selector
            Text(text = "اختر لون الرمز", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colors.forEach { hex ->
                    val colorHex = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorHex)
                            .clickable { selectedColor = hex }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == hex) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Icon Selector
            Text(text = "اختر الرمز المرئي", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                icons.forEach { iconName ->
                    val isSelected = selectedIcon == iconName
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedIcon = iconName },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(iconName),
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val limitDouble = amountLimit.toDoubleOrNull() ?: 0.0
                    if (title.isBlank()) {
                        Toast.makeText(context, "الرجاء إدخال عنوان الميزانية", Toast.LENGTH_SHORT).show()
                    } else if (amountLimit.isBlank() || limitDouble <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال مبلغ ميزانية صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                    } else if (selectedBudgetType == BudgetType.CATEGORY && selectedCategoryId == null) {
                        Toast.makeText(context, "الرجاء اختيار تصنيف مستهدف", Toast.LENGTH_SHORT).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val calendar = Calendar.getInstance()
                        val startDate = calendar.timeInMillis
                        
                        // Set standard durations based on type
                        if (title.contains("أسبوع") || title.contains("اسبوع") || title.contains("Weekly")) {
                            calendar.add(Calendar.DAY_OF_YEAR, 7)
                        } else {
                            calendar.add(Calendar.MONTH, 1) // default to Monthly
                        }
                        val endDate = calendar.timeInMillis

                        viewModel.addBudgetGoal(
                            title = title.trim(),
                            linkedCategoryId = selectedCategoryId,
                            budgetType = selectedBudgetType,
                            amountLimit = limitDouble,
                            startDate = startDate,
                            endDate = endDate,
                            alertThresholdPercent = alertThresholdPercent.toInt(),
                            color = selectedColor,
                            icon = selectedIcon
                        )
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "حفظ الميزانية الذكية", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}
