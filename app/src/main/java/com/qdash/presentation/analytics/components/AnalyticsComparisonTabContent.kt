package com.qdash.presentation.analytics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.domain.model.CardAiContext
import com.qdash.presentation.analytics.AnalyticsUiState
import com.qdash.presentation.analytics.DashboardOverviewCard
import com.qdash.presentation.analytics.MonthComparisonCard
import com.qdash.presentation.analytics.MonthYearPickerDialog
import com.qdash.presentation.analytics.YearPickerDialog

/**
 * Content for Analytics Tab 1 (المقارنة — Comparison).
 * Contains: period switcher, date picker button, loading/empty states,
 * DashboardOverviewCard, and MonthComparisonCard.
 * Extracted from AnalyticsScreen to keep it under the SIZE-002 line limit.
 */
@Composable
fun AnalyticsComparisonTabContent(
    uiState: AnalyticsUiState,
    dashIncome: Double,
    dashExpenses: Double,
    periodStart: Long,
    periodEnd: Long,
    onPeriodChange: (String) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    onCompareMonthAChange: (month: Int, year: Int) -> Unit,
    onCompareMonthBChange: (month: Int, year: Int) -> Unit,
    onAiChatClick: (CardAiContext) -> Unit
) {
    // ── 1. Filter header & Period switcher ──
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Monthly vs Annually segmented pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)),
            modifier = Modifier.weight(1.1f)
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val periods = listOf("MONTHLY" to "شهرياً", "ANNUALLY" to "سنوياً")
                periods.forEach { (key, label) ->
                    val isSelected = uiState.dashboardPeriod == key
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "periodBg_$key"
                    )
                    val txt by animateColorAsState(
                        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        label = "periodTxt_$key"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(bg)
                            .clickable { onPeriodChange(key) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = txt)
                    }
                }
            }
        }

        // Date Selector Button
        var showDashboardDatePicker by remember { mutableStateOf(false) }
        val arabicMonths = remember {
            arrayOf("جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان", "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        }
        val pickerLabel = if (uiState.dashboardPeriod == "MONTHLY") {
            "${arabicMonths[uiState.dashboardMonth]} ${uiState.dashboardYear}"
        } else {
            "سنة ${uiState.dashboardYear}"
        }

        Button(
            onClick = { showDashboardDatePicker = true },
            modifier = Modifier.weight(1f).height(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(text = pickerLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showDashboardDatePicker) {
            if (uiState.dashboardPeriod == "MONTHLY") {
                MonthYearPickerDialog(
                    initialMonth = uiState.dashboardMonth,
                    initialYear = uiState.dashboardYear,
                    onDismiss = { showDashboardDatePicker = false },
                    onConfirm = { m, y ->
                        onMonthChange(m)
                        onYearChange(y)
                        showDashboardDatePicker = false
                    }
                )
            } else {
                YearPickerDialog(
                    initialYear = uiState.dashboardYear,
                    onDismiss = { showDashboardDatePicker = false },
                    onConfirm = { y ->
                        onYearChange(y)
                        showDashboardDatePicker = false
                    }
                )
            }
        }
    }

    // ── 2. Loading / Empty / Data states ──
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            }
        }
        uiState.isDatabaseEmpty -> {
            EmptyStateView(
                title = "لا تتوفر بيانات للمقارنة",
                description = "قم بتسجيل معاملاتك المالية أولاً لتتمكن من مقارنة الدخل والمصاريف بين الفترات المختلفة.",
                icon = Icons.Default.CompareArrows,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        else -> {
            DashboardOverviewCard(
                totalIncome = dashIncome,
                totalExpenses = dashExpenses,
                onAiChatClick = onAiChatClick,
                periodStart = periodStart,
                periodEnd = periodEnd
            )

            MonthComparisonCard(
                transactions = uiState.transactions,
                categories = uiState.categories,
                compareMonthA = uiState.compareMonthA,
                compareYearA = uiState.compareYearA,
                compareMonthB = uiState.compareMonthB,
                compareYearB = uiState.compareYearB,
                onMonthAChange = onCompareMonthAChange,
                onMonthBChange = onCompareMonthBChange,
                onAiChatClick = onAiChatClick,
                periodStart = periodStart,
                periodEnd = periodEnd
            )
        }
    }
}
