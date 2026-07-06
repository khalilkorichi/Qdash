package com.qdash.presentation.analytics.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.domain.model.CardAiContext
import com.qdash.presentation.analytics.AnalyticsUiState
import com.qdash.presentation.analytics.EmergencyFundCard
import com.qdash.presentation.analytics.SalaryCycleCard
import com.qdash.presentation.analytics.SavingsChallengesSection
import com.qdash.presentation.analytics.WeekendWeekdayCard
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.ui.theme.*

/**
 * Content for Analytics Tab 2 (التحليلات — Insights).
 * Contains: EmergencyFundCard, SalaryCycleCard, WeekendWeekdayCard, empty states.
 * Extracted from AnalyticsScreen to keep it under the SIZE-002 line limit.
 */
@Composable
fun AnalyticsInsightsTabContent(
    uiState: AnalyticsUiState,
    periodStart: Long,
    periodEnd: Long,
    onAiChatClick: (CardAiContext) -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        }
        uiState.isDatabaseEmpty -> {
            EmptyStateView(
                title = "التحليلات الذكية هادئة حالياً",
                description = "أضف بعض المعاملات والمصاريف اليومية لتوليد تحليلات ذكية حول عادات الإنفاق وصندوق الطوارئ الخاص بك.",
                icon = Icons.Default.PieChart,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        else -> {
            EmergencyFundCard(
                uiState = uiState,
                onAiChatClick = onAiChatClick,
                periodStart = periodStart,
                periodEnd = periodEnd
            )
            if (uiState.selectedPeriod == "MONTH" && uiState.spendingsByCategory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                SalaryCycleCard(
                    uiState = uiState,
                    onAiChatClick = onAiChatClick,
                    periodStart = periodStart,
                    periodEnd = periodEnd
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            WeekendWeekdayCard(
                uiState = uiState,
                onAiChatClick = onAiChatClick,
                periodStart = periodStart,
                periodEnd = periodEnd
            )
            if (uiState.spendingsByCategory.isEmpty()) {
                AppCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Xxl,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Text(
                            text = "لا توجد مصاريف مسجلة في هذه الفترة الزمنية لتوليد تحليلات الراتب ونهاية الأسبوع.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "جرب اختيار فترة زمنية أخرى أو إضافة معاملات إنفاق جديدة.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Content for Analytics Tab 3 (الادخار — Savings).
 * Contains: SavingsChallengesSection + empty states.
 * Extracted from AnalyticsScreen to keep it under the SIZE-002 line limit.
 */
@Composable
fun AnalyticsSavingsTabContent(
    uiState: AnalyticsUiState
) {
    Spacer(modifier = Modifier.height(16.dp))
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        }
        uiState.isDatabaseEmpty -> {
            EmptyStateView(
                title = "لا تتوفر تحديات ادخار حالياً",
                description = "سجل معاملاتك المعتادة لتفعيل تحديات الادخار المخصصة ومساعدتك على توفير المال.",
                icon = Icons.Default.Savings,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        uiState.spendingsByCategory.isNotEmpty() -> {
            SavingsChallengesSection()
        }
        else -> {
            EmptyStateView(
                title = "لا توجد بيانات إنفاق في هذه الفترة",
                description = "جرب تغيير الفترة الزمنية أو أضف معاملات جديدة لتفعيل تحديات الادخار.",
                icon = Icons.Default.Savings,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}
