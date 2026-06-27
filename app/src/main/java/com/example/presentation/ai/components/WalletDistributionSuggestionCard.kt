package com.example.presentation.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.WalletDistributionSuggestion
import com.example.core.utils.FormatterUtils
import com.example.ui.theme.TextGray

@Composable
fun WalletDistributionSuggestionCard(
    suggestion: WalletDistributionSuggestion,
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
                text = "💡 مقترح توزيع أرصدة المحفظة",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "توزيع متوازن ومقترح للأرصدة الكلية بالتساوي بين الحسابات الفعالة لتخفيف مخاطر السيولة.",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(10.dp))
            suggestion.items.forEach { item ->
                val colorHex = item.color.let { if (it.startsWith("#")) it else "#$it" }
                val parsedColor = remember(item.color) {
                    try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.Gray }
                }
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.accountName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${FormatterUtils.formatCurrency(item.currentBalance)} / ${FormatterUtils.formatCurrency(item.suggestedBalance)}",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (item.suggestedPercentage / 100f).toFloat(),
                        color = parsedColor,
                        trackColor = parsedColor.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
