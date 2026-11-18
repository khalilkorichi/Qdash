package com.qdash.presentation.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun CustomColorSliderDialog(
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (hex: String) -> Unit
) {
    var redVal by remember {
        mutableStateOf(
            (try { android.graphics.Color.red(android.graphics.Color.parseColor(initialColor)) } catch (_: Exception) { 108 }).toFloat() / 255f
        )
    }
    var greenVal by remember {
        mutableStateOf(
            (try { android.graphics.Color.green(android.graphics.Color.parseColor(initialColor)) } catch (_: Exception) { 99 }).toFloat() / 255f
        )
    }
    var blueVal by remember {
        mutableStateOf(
            (try { android.graphics.Color.blue(android.graphics.Color.parseColor(initialColor)) } catch (_: Exception) { 255 }).toFloat() / 255f
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
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(previewColor)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                )
                Text(
                    text = hexString,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(value * 255).toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor.copy(alpha = 0.55f)
            )
        )
    }
}
