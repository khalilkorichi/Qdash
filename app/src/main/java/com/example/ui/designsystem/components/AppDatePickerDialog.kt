package com.example.ui.designsystem.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
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
import com.example.core.utils.FormatterUtils
import com.example.ui.designsystem.tokens.ColorTokens
import com.example.ui.designsystem.tokens.ShapeTokens
import com.example.ui.designsystem.tokens.MotionTokens
import java.util.*

@Composable
fun AppDatePickerDialog(
    initialSelectedDateMillis: Long?,
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit,
    confirmButtonColor: Color = MaterialTheme.colorScheme.primary
) {
    Dialog(onDismissRequest = onDismissRequest) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
            val transition = rememberTransition(transitionState, label = "datepicker_entrance")

            val alpha by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 180, easing = LinearOutSlowInEasing) },
                label = "datepicker_alpha"
            ) { state ->
                if (state) 1f else 0f
            }

            val translateY by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 180, easing = FastOutSlowInEasing) },
                label = "datepicker_translateY"
            ) { state ->
                if (state) 0f else 25f
            }

            val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
            val dialogBgColor = if (isDark) ColorTokens.ElevatedSurfaceDark else ColorTokens.SurfaceLight
            val dividerColor = if (isDark) ColorTokens.DividerDark else ColorTokens.BorderLight

            var selectedDate by remember { mutableStateOf(initialSelectedDateMillis ?: System.currentTimeMillis()) }
            
            // Calendar visible month and year state
            var visibleMonth by remember {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                mutableStateOf(cal.get(Calendar.MONTH))
            }
            var visibleYear by remember {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                mutableStateOf(cal.get(Calendar.YEAR))
            }

            var isYearMonthPickerVisible by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        alpha = alpha,
                        translationY = translateY
                    ),
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
                    // Header: Selected Date Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "اختر التاريخ",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatterUtils.formatDate(selectedDate),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isDark) ColorTokens.TextPrimaryDark else ColorTokens.TextPrimaryLight
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = confirmButtonColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val arabicMonths = remember {
                        arrayOf(
                            "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
                            "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
                        )
                    }

                    // Month & Year Selector Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isYearMonthPickerVisible) {
                                    // Decrement year
                                    visibleYear--
                                } else {
                                    // Previous month
                                    if (visibleMonth == 0) {
                                        visibleMonth = 11
                                        visibleYear--
                                    } else {
                                        visibleMonth--
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "السابق",
                                tint = if (isDark) ColorTokens.TextPrimaryDark else ColorTokens.TextPrimaryLight
                            )
                        }

                        // Display text (Clickable to toggle quick Year/Month Selector)
                        Text(
                            text = if (isYearMonthPickerVisible) {
                                FormatterUtils.convertNumerals(visibleYear.toString())
                            } else {
                                "${arabicMonths[visibleMonth]} ${FormatterUtils.convertNumerals(visibleYear.toString())}"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = confirmButtonColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isYearMonthPickerVisible = !isYearMonthPickerVisible }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        IconButton(
                            onClick = {
                                if (isYearMonthPickerVisible) {
                                    // Increment year
                                    visibleYear++
                                } else {
                                    // Next month
                                    if (visibleMonth == 11) {
                                        visibleMonth = 0
                                        visibleYear++
                                    } else {
                                        visibleMonth++
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "التالي",
                                tint = if (isDark) ColorTokens.TextPrimaryDark else ColorTokens.TextPrimaryLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isYearMonthPickerVisible) {
                        // Quick Year/Month Picker Grid
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "اختر الشهر:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(12) { monthIdx ->
                                    val isSelected = visibleMonth == monthIdx
                                    val cellBg = if (isSelected) confirmButtonColor else dividerColor.copy(alpha = 0.2f)
                                    val cellTextColor = if (isSelected) Color.White else (if (isDark) ColorTokens.TextPrimaryDark else ColorTokens.TextPrimaryLight)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cellBg)
                                            .clickable {
                                                visibleMonth = monthIdx
                                                isYearMonthPickerVisible = false
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = arabicMonths[monthIdx],
                                            color = cellTextColor,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Main Calendar View
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            // Weekdays Header (Full Arabic Weekday Names)
                            val weekdayNames = remember {
                                arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                weekdayNames.forEach { dayName ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayName,
                                            color = if (isDark) ColorTokens.TextMutedDark else ColorTokens.TextSecondaryLight,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Generate days for 6x7 grid (42 days)
                            val calendarDays = remember(visibleMonth, visibleYear) {
                                getCalendarDays(visibleMonth, visibleYear)
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(7),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(calendarDays) { day ->
                                    val isCurrentMonth = day.monthOffset == 0
                                    val isSelected = isSameDay(day.timestamp, selectedDate)
                                    val isToday = isSameDay(day.timestamp, System.currentTimeMillis())

                                    val cellBg = when {
                                        isSelected -> confirmButtonColor
                                        else -> Color.Transparent
                                    }

                                    val cellTextColor = when {
                                        isSelected -> Color.White
                                        isCurrentMonth -> if (isDark) ColorTokens.TextPrimaryDark else ColorTokens.TextPrimaryLight
                                        else -> if (isDark) ColorTokens.TextMutedDark.copy(alpha = 0.5f) else ColorTokens.TextSecondaryLight.copy(alpha = 0.4f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(cellBg)
                                            .run {
                                                if (isCurrentMonth) {
                                                    clickable {
                                                        selectedDate = day.timestamp
                                                    }
                                                } else {
                                                    clickable {
                                                        // Navigate to previous/next month
                                                        if (day.monthOffset == -1) {
                                                            if (visibleMonth == 0) {
                                                                visibleMonth = 11
                                                                visibleYear--
                                                            } else {
                                                                visibleMonth--
                                                            }
                                                        } else {
                                                            if (visibleMonth == 11) {
                                                                visibleMonth = 0
                                                                visibleYear++
                                                            } else {
                                                                visibleMonth++
                                                            }
                                                        }
                                                        selectedDate = day.timestamp
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = FormatterUtils.convertNumerals(day.dayOfMonth.toString()),
                                                color = cellTextColor,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                fontSize = 13.sp
                                            )
                                            if (isToday && !isSelected) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(confirmButtonColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Footer Buttons
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

                        DialogButton(
                            onClick = {
                                onDateSelected(selectedDate)
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

// Calendar Calculation helper models and functions
private data class CalendarDay(
    val dayOfMonth: Int,
    val monthOffset: Int, // -1 for previous month, 0 for current, 1 for next month
    val timestamp: Long
)

private fun getCalendarDays(month: Int, year: Int): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    
    // Calendar instance set to the 1st of the visible month
    val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 12) // Midday to prevent timezone transitions
    }
    
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ..., 7 = Saturday
    
    // Grid starts on Sunday (offset is firstDayOfWeek - 1)
    val prevMonthDaysToShow = firstDayOfWeek - 1
    
    val targetCal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
    }

    // Go back to the grid start date
    cal.add(Calendar.DAY_OF_MONTH, -prevMonthDaysToShow)
    
    for (i in 0 until 42) {
        val calYear = cal.get(Calendar.YEAR)
        val calMonth = cal.get(Calendar.MONTH)
        
        val offset = when {
            calYear < targetCal.get(Calendar.YEAR) -> -1
            calYear > targetCal.get(Calendar.YEAR) -> 1
            calMonth < targetCal.get(Calendar.MONTH) -> -1
            calMonth > targetCal.get(Calendar.MONTH) -> 1
            else -> 0
        }
        
        days.add(
            CalendarDay(
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                monthOffset = offset,
                timestamp = cal.timeInMillis
            )
        )
        
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    
    return days
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun DialogButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = MotionTokens.springBouncy(),
        label = "dialog_button_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .height(48.dp),
        shape = ShapeTokens.Md,
        color = backgroundColor,
        contentColor = contentColor,
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
