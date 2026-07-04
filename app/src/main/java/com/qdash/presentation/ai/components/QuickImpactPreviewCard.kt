package com.qdash.presentation.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.QuickImpactPreviewState
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.theme.TextGray

@Composable
fun QuickImpactPreviewCard(
    state: QuickImpactPreviewState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFE9E9E6)), shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "⚡ نظرة سريعة على تأثير العملية",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            if (state.budgetLimit != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("التأثير على الميزانية المحددة:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("قبل: ${FormatterUtils.formatCurrency(state.budgetSpentBefore)}", fontSize = 10.sp)
                        Text("بعد: ${FormatterUtils.formatCurrency(state.budgetSpentAfter)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (state.budgetSpentAfter > state.budgetLimit) Color(0xFFEF4444) else Color.Unspecified)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (state.budgetSpentAfter / state.budgetLimit).toFloat().coerceIn(0f, 1f),
                        color = if (state.budgetSpentAfter > state.budgetLimit) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                    Text("الحد الأقصى: ${FormatterUtils.formatCurrency(state.budgetLimit)}", fontSize = 9.sp, color = TextGray)
                }
            }
            
            if (state.goalName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("الهدف: ${state.goalName}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("قبل: ${FormatterUtils.formatCurrency(state.goalSavedBefore)}", fontSize = 10.sp)
                        Text("بعد: ${FormatterUtils.formatCurrency(state.goalSavedAfter)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (state.goalSavedAfter / state.goalTarget).toFloat().coerceIn(0f, 1f),
                        color = Color(0xFF22C55E),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                    Text("الهدف الكلي: ${FormatterUtils.formatCurrency(state.goalTarget)}", fontSize = 9.sp, color = TextGray)
                }
            }
            
            if (state.debtName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("الدين: ${state.debtName}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المتبقي قبل: ${FormatterUtils.formatCurrency(state.debtRemainingBefore)}", fontSize = 10.sp)
                        Text("المتبقي بعد: ${FormatterUtils.formatCurrency(state.debtRemainingAfter)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (state.debtRemainingAfter < state.debtRemainingBefore) Color(0xFF22C55E) else Color(0xFFEF4444))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (state.debtRemainingAfter / state.debtTotal).toFloat().coerceIn(0f, 1f),
                        color = Color(0xFFEF4444),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
