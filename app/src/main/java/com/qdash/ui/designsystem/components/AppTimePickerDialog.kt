package com.qdash.ui.designsystem.components

import android.text.format.DateFormat
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class PresetTime(
    val hour: Int,
    val minute: Int,
    val label: String
)

/**
 * Custom Wheel-based Time Picker Dialog matching the requested mockup design.
 * Provides vertical scrollable wheels for hours, minutes, and AM/PM with snapping.
 * Includes a presets quick-selection row and confirmation button.
 *
 * @param initialHour Hour (0-23) to pre-select.
 * @param initialMinute Minute (0-59) to pre-select.
 * @param onDismissRequest Called when the user dismisses/cancels the picker.
 * @param onTimeSelected Called with (hour, minute) on confirmation.
 * @param is24Hour True for 24-hour format, false for 12-hour format with AM/PM.
 * @param confirmButtonColor Accent color for the confirmation button and active selections.
 */
@Composable
fun AppTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismissRequest: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    is24Hour: Boolean = DateFormat.is24HourFormat(LocalContext.current),
    confirmButtonColor: Color = MaterialTheme.colorScheme.primary
) {
    Dialog(onDismissRequest = onDismissRequest) {
        val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
        val dialogBgColor = if (isDark) ColorTokens.ElevatedSurfaceDark else ColorTokens.SurfaceLight
        val dividerColor = if (isDark) ColorTokens.DividerDark else ColorTokens.BorderLight
        val textPrimary = if (isDark) ColorTokens.TextPrimaryDark else ColorTokens.TextPrimaryLight
        val textSecondary = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight

        var selectedHour by rememberSaveable { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
        var selectedMinute by rememberSaveable { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

        val displayHour = remember(selectedHour) {
            if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
        }
        val isPm = remember(selectedHour) { selectedHour >= 12 }
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        Card(
            modifier = modifier.fillMaxWidth(),
            shape = ShapeTokens.Xl,
            colors = CardDefaults.cardColors(containerColor = dialogBgColor),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Title & Close button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRtl) "اختر الوقت" else "Time",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء",
                            tint = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scroll Selection Area
                val itemHeight = 44.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF9F9F9),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Gray highlighted bar in the center behind active items
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(itemHeight)
                            .background(
                                color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEEE),
                                shape = RoundedCornerShape(10.dp)
                            )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hoursList = if (is24Hour) {
                            (0..23).map { "%02d".format(it) }
                        } else {
                            (1..12).map { "%d".format(it) }
                        }
                        val minutesList = (0..59).map { "%02d".format(it) }
                        val amPmList = if (isRtl) listOf("ص", "م") else listOf("am", "pm")

                        val initialHourIndex = if (is24Hour) {
                            selectedHour
                        } else {
                            if (selectedHour == 0) 11 else if (selectedHour > 12) selectedHour - 13 else selectedHour - 1
                        }

                        // Hours Column
                        WheelColumn(
                            items = hoursList,
                            initialIndex = initialHourIndex,
                            onIndexSelected = { index ->
                                if (is24Hour) {
                                    selectedHour = index
                                } else {
                                    val hourVal = index + 1
                                    selectedHour = if (isPm) {
                                        if (hourVal == 12) 12 else hourVal + 12
                                    } else {
                                        if (hourVal == 12) 0 else hourVal
                                    }
                                }
                            },
                            isInfinite = true,
                            itemHeight = itemHeight,
                            modifier = Modifier.weight(1.2f)
                        )

                        // Separator ":"
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Minutes Column
                        WheelColumn(
                            items = minutesList,
                            initialIndex = selectedMinute,
                            onIndexSelected = { selectedMinute = it },
                            isInfinite = true,
                            itemHeight = itemHeight,
                            modifier = Modifier.weight(1.2f)
                        )

                        // AM/PM Column (Only for 12-hour format)
                        if (!is24Hour) {
                            Spacer(modifier = Modifier.width(8.dp))
                            WheelColumn(
                                items = amPmList,
                                initialIndex = if (isPm) 1 else 0,
                                onIndexSelected = { index ->
                                    val toPm = index == 1
                                    selectedHour = if (toPm) {
                                        if (displayHour == 12) 12 else displayHour + 12
                                    } else {
                                        if (displayHour == 12) 0 else displayHour
                                    }
                                },
                                isInfinite = false,
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Presets Section
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (isRtl) "الاختصارات" else "Presets",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = textSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val labelAm = if (isRtl) "ص" else "am"
                    val labelPm = if (isRtl) "م" else "pm"

                    val presets = listOf(
                        PresetTime(9, 0, if (is24Hour) "09:00" else "9 $labelAm"),
                        PresetTime(12, 0, if (is24Hour) "12:00" else "12 $labelPm"),
                        PresetTime(16, 0, if (is24Hour) "16:00" else "4 $labelPm"),
                        PresetTime(18, 0, if (is24Hour) "18:00" else "6 $labelPm")
                    )

                    presets.forEach { preset ->
                        val isSelected = selectedHour == preset.hour && selectedMinute == preset.minute

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) confirmButtonColor.copy(alpha = 0.12f) else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) confirmButtonColor else dividerColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedHour = preset.hour
                                    selectedMinute = preset.minute
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = FormatterUtils.convertNumerals(preset.label),
                                color = if (isSelected) confirmButtonColor else textSecondary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                // Confirm Action Button
                Spacer(modifier = Modifier.height(24.dp))
                TimePickerConfirmButton(
                    onClick = {
                        onTimeSelected(selectedHour, selectedMinute)
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = confirmButtonColor
                ) {
                    Text(
                        text = if (isRtl) "تأكيد" else "Done",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Scrollable column of items representing a scroll wheel.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isInfinite: Boolean = true,
    itemHeight: Dp = 44.dp
) {
    val coroutineScope = rememberCoroutineScope()
    val virtualIndex = if (isInfinite) (items.size * 500) + initialIndex else initialIndex
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = virtualIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    // Sync scroll state with external selection
    LaunchedEffect(lazyListState.firstVisibleItemIndex) {
        val realIndex = if (isInfinite) {
            lazyListState.firstVisibleItemIndex % items.size
        } else {
            lazyListState.firstVisibleItemIndex
        }
        onIndexSelected(realIndex.coerceIn(0, items.lastIndex))
    }

    // Scroll programmatically when initialIndex changes from presets
    LaunchedEffect(initialIndex) {
        val currentVirtualIndex = lazyListState.firstVisibleItemIndex
        val currentRealIndex = if (isInfinite) {
            currentVirtualIndex % items.size
        } else {
            currentVirtualIndex
        }
        if (currentRealIndex != initialIndex) {
            if (isInfinite) {
                val diff = initialIndex - currentRealIndex
                lazyListState.animateScrollToItem(currentVirtualIndex + diff)
            } else {
                lazyListState.animateScrollToItem(initialIndex)
            }
        }
    }

    Box(
        modifier = modifier.height(itemHeight * 5),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = lazyListState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * 2),
            modifier = Modifier.fillMaxWidth()
        ) {
            val totalItems = if (isInfinite) items.size * 1000 else items.size
            items(totalItems) { index ->
                val realIndex = if (isInfinite) index % items.size else index
                val isSelected = index == lazyListState.firstVisibleItemIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(index)
                            }
                        }
                        .graphicsLayer {
                            // Apply fading barrel distortion effect based on distance to center
                            val distance = abs(index - lazyListState.firstVisibleItemIndex)
                            alpha = when (distance) {
                                0 -> 1f
                                1 -> 0.45f
                                2 -> 0.15f
                                else -> 0f
                            }
                            scaleX = when (distance) {
                                0 -> 1.12f
                                1 -> 0.92f
                                2 -> 0.78f
                                else -> 0.7f
                            }
                            scaleY = scaleX
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[realIndex],
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = MotionTokens.springBouncy(),
        label = "time_confirm_scale"
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .height(50.dp),
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

