package com.qdash.presentation.analytics.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.TransactionType
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.ui.theme.*
import com.qdash.presentation.analytics.CategoryShare

/**
 * Dialog that shows category transaction history and a color picker.
 * Extracted from AnalyticsScreen to keep it under the SIZE-002 line limit.
 */
@Composable
fun CategoryColorPickerDialog(
    category: CategoryShare,
    categoryTxs: List<com.qdash.domain.model.Transaction>,
    onDismiss: (newColor: String) -> Unit,
    onColorChange: (categoryId: Long, color: String) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedColor by remember(category.categoryId) { mutableStateOf(category.color) }
    var showCustomColorDialog by remember { mutableStateOf(false) }

    val parsedSelectedColor = remember(selectedColor) {
        try { Color(android.graphics.Color.parseColor(selectedColor)) }
        catch (e: Exception) { null }
    }

    // Preset colors — defined once, never recreated
    val colorPresets = remember {
        listOf(
            "#6C63FF", "#22C55E", "#EF4444", "#3B82F6", "#F59E0B",
            "#EC4899", "#8B5CF6", "#06B6D4", "#10B981", "#F97316"
        )
    }
    val parsedPresetColors = remember {
        colorPresets.map { hex ->
            try { Color(android.graphics.Color.parseColor(hex)) }
            catch (e: Exception) { Color.Gray }
        }
    }
    val rainbowBrush = remember {
        Brush.sweepGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green,
                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
        )
    }
    val dateFormatter = remember { java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US) }

    if (showCustomColorDialog) {
        CustomColorSliderDialog(
            initialColor = selectedColor,
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { hex ->
                selectedColor = hex
                showCustomColorDialog = false
            }
        )
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = {
        if (selectedColor != category.color) {
            onColorChange(category.categoryId, selectedColor)
        }
        onDismiss(selectedColor)
    }) {
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            variant = CardVariant.SOLID,
            shape = ShapeTokens.Xl,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "سجل معاملات: ${category.categoryName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    (parsedSelectedColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.2f)
                                )
                                .clickable { showColorPicker = !showColorPicker },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "تعديل اللون",
                                tint = parsedSelectedColor ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showColorPicker,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(100))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "اختر لوناً للفئة:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextGray
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Row 1: First 5 preset colors
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colorPresets.take(5).forEachIndexed { idx, hex ->
                                    val colorVal = parsedPresetColors[idx]
                                    val isCurrent = selectedColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colorVal)
                                            .border(
                                                width = if (isCurrent) 2.5.dp else 0.dp,
                                                color = if (isCurrent) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = hex }
                                    )
                                }
                            }

                            // Row 2: Next 5 preset colors + Custom color button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colorPresets.drop(5).forEachIndexed { relIdx, hex ->
                                    val colorVal = parsedPresetColors[relIdx + 5]
                                    val isCurrent = selectedColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colorVal)
                                            .border(
                                                width = if (isCurrent) 2.5.dp else 0.dp,
                                                color = if (isCurrent) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = hex }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(rainbowBrush)
                                        .clickable { showCustomColorDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "لون مخصص",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                if (categoryTxs.isEmpty()) {
                    Text(
                        text = "لا توجد معاملات لهذه الفئة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        categoryTxs.take(4).forEach { tx ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.note ?: "بدون ملاحظة",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val dateStr = FormatterUtils.convertNumerals(
                                        dateFormatter.format(java.util.Date(tx.date))
                                    )
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                }
                                Text(
                                    text = FormatterUtils.formatCurrency(tx.amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                        if (categoryTxs.size > 4) {
                            Text(
                                text = "+ وأكثر بـ ${categoryTxs.size - 4} عمليات",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * RGB slider dialog for picking a custom category color.
 */
@Composable
private fun CustomColorSliderDialog(
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (hex: String) -> Unit
) {
    var redVal by remember {
        mutableStateOf(
            (try { android.graphics.Color.red(android.graphics.Color.parseColor(initialColor)) } catch (e: Exception) { 108 }).toFloat() / 255f
        )
    }
    var greenVal by remember {
        mutableStateOf(
            (try { android.graphics.Color.green(android.graphics.Color.parseColor(initialColor)) } catch (e: Exception) { 99 }).toFloat() / 255f
        )
    }
    var blueVal by remember {
        mutableStateOf(
            (try { android.graphics.Color.blue(android.graphics.Color.parseColor(initialColor)) } catch (e: Exception) { 255 }).toFloat() / 255f
        )
    }

    val previewColor = Color(redVal, greenVal, blueVal)
    val hexString = String.format("#%02X%02X%02X", (redVal * 255).toInt(), (greenVal * 255).toInt(), (blueVal * 255).toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر لوناً مخصصاً للفئة", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(previewColor)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                )
                Text(
                    text = hexString,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorSliderRow(label = "الأحمر", value = redVal, onChange = { redVal = it }, trackColor = Color.Red)
                    ColorSliderRow(label = "الأخضر", value = greenVal, onChange = { greenVal = it }, trackColor = Color.Green)
                    ColorSliderRow(label = "الأزرق", value = blueVal, onChange = { blueVal = it }, trackColor = Color.Blue)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(hexString) }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun ColorSliderRow(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    trackColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextGray)
            Text("${(value * 255).toInt()}", style = MaterialTheme.typography.labelSmall, color = TextGray)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor.copy(alpha = 0.5f)
            )
        )
    }
}
