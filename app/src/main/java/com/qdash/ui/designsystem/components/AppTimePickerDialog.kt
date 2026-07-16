package com.qdash.ui.designsystem.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import java.util.*

/**
 * RTL-native time picker dialog matching the app design system.
 * Only used when user explicitly opts to set a precise time (occurredAt).
 *
 * @param initialHour  Hour (0-23) to pre-select. Usually from occurredAt or Calendar.HOUR_OF_DAY.
 * @param initialMinute Minute (0-59) to pre-select.
 * @param onDismissRequest Called when the user dismisses without confirming.
 * @param onTimeSelected  Called with (hour, minute) on confirm.
 * @param confirmButtonColor Accent color for the confirm button and selected cells.
 */
@Composable
fun AppTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismissRequest: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    confirmButtonColor: Color = MaterialTheme.colorScheme.primary
) {
    Dialog(onDismissRequest = onDismissRequest) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
            val transition = rememberTransition(transitionState, label = "timepicker_entrance")

            val alpha by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 180, easing = LinearOutSlowInEasing) },
                label = "timepicker_alpha"
            ) { if (it) 1f else 0f }

            val translateY by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 180, easing = FastOutSlowInEasing) },
                label = "timepicker_translateY"
            ) { if (it) 0f else 25f }

            val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
            val dialogBgColor = if (isDark) ColorTokens.ElevatedSurfaceDark else ColorTokens.SurfaceLight
            val dividerColor = if (isDark) ColorTokens.DividerDark else ColorTokens.BorderLight
            val textPrimary = if (isDark) ColorTokens.TextPrimaryDark else ColorTokens.TextPrimaryLight
            val textSecondary = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight

            var selectedHour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
            var selectedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = alpha, translationY = translateY),
                shape = ShapeTokens.Xl,
                colors = CardDefaults.cardColors(containerColor = dialogBgColor),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "اختر الوقت",
                                style = MaterialTheme.typography.labelMedium,
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatterUtils.convertNumerals(
                                    "%02d:%02d".format(selectedHour, selectedMinute)
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = textPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = confirmButtonColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Hours + Minutes pickers side by side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hours (0-23)
                        TimeColumn(
                            label = "الساعة",
                            items = (0..23).toList(),
                            selected = selectedHour,
                            onSelected = { selectedHour = it },
                            accentColor = confirmButtonColor,
                            isDark = isDark,
                            textPrimary = textPrimary,
                            dividerColor = dividerColor,
                            modifier = Modifier.weight(1f)
                        )

                        // Separator
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = confirmButtonColor,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        // Minutes — multiples of 5 for easy picking; fine-tune via +1/-1 arrows
                        TimeColumn(
                            label = "الدقيقة",
                            items = (0..59).toList(),
                            selected = selectedMinute,
                            onSelected = { selectedMinute = it },
                            accentColor = confirmButtonColor,
                            isDark = isDark,
                            textPrimary = textPrimary,
                            dividerColor = dividerColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Footer buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.weight(1f),
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.PRIMARY
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        TimePickerConfirmButton(
                            onClick = {
                                onTimeSelected(selectedHour, selectedMinute)
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(1f),
                            backgroundColor = confirmButtonColor
                        ) {
                            Text("تأكيد", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Scrollable column of time values. Each value is a tappable chip.
 * Renders as a vertical grid of fixed-height rows, showing 5 at a time.
 */
@Composable
private fun TimeColumn(
    label: String,
    items: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    accentColor: Color,
    isDark: Boolean,
    textPrimary: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    val textMuted = if (isDark) ColorTokens.TextMutedDark else ColorTokens.TextSecondaryLight

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Quick-pick grid: 6 columns, rows as needed
        val rowsOf6 = items.chunked(6)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rowsOf6.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEach { value ->
                        val isSelected = value == selected
                        val cellBg = if (isSelected) accentColor else dividerColor.copy(alpha = 0.15f)
                        val cellTextColor = if (isSelected) Color.White else textPrimary
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg)
                                .clickable { onSelected(value) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = FormatterUtils.convertNumerals("%02d".format(value)),
                                color = cellTextColor,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // Fill remaining slots in last row
                    repeat(6 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePickerConfirmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = MotionTokens.springBouncy(),
        label = "time_confirm_scale"
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .height(48.dp),
        shape = ShapeTokens.Md,
        color = backgroundColor,
        contentColor = Color.White,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
