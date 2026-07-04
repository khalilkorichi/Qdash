package com.qdash.presentation.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.RecentActivitySummary
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.theme.TextGray

@Composable
fun RecentActivitySummaryCard(
    summary: RecentActivitySummary,
    messageId: String,
    onDuplicateClick: ((Transaction) -> Unit)?,
    onEditClick: ((String, Transaction) -> Unit)?,
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
                text = "📊 آخر الحركات المالية",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (summary.transactions.isEmpty()) {
                Text("لا توجد حركات مالية مؤخراً.", fontSize = 11.sp, color = TextGray)
            } else {
                summary.transactions.forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val typeEmoji = when (tx.type) {
                                TransactionType.EXPENSE -> "🔴"
                                TransactionType.INCOME -> "🟢"
                                TransactionType.TRANSFER -> "🔵"
                            }
                            Text(
                                text = "$typeEmoji ${tx.note ?: "عملية مالية"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = FormatterUtils.formatCurrency(tx.amount),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onEditClick?.invoke(messageId, tx) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تعديل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDuplicateClick?.invoke(tx) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تكرار",
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}
