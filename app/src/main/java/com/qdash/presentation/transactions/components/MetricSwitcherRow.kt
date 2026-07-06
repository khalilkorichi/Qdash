package com.qdash.presentation.transactions.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray
import com.qdash.ui.theme.TransferBlue

@Composable
fun MetricSwitcherRow(
    selectedMetricMode: String,
    onMetricModeChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var showHelpDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            val modes = listOf(
                "COUNT" to "العمليات",
                "EXPENSE" to "المصاريف",
                "INCOME" to "المداخيل",
                "CASHFLOW" to "صافي الحركة",
                "SCORE" to "النشاط"
            )

            items(modes) { (modeCode, label) ->
                val isModeSelected = selectedMetricMode == modeCode
                val modeColor = when (modeCode) {
                    "COUNT" -> primaryColor
                    "EXPENSE" -> ExpenseRed
                    "INCOME" -> IncomeGreen
                    "CASHFLOW" -> TransferBlue
                    "SCORE" -> SavingsAmber
                    else -> primaryColor
                }
                Surface(
                    onClick = { onMetricModeChanged(modeCode) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isModeSelected) modeColor else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold, fontSize = 11.sp
                        ),
                        color = if (isModeSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        IconButton(onClick = { showHelpDialog = true }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "معلومات المؤشرات",
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        }

        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "دليل المؤشرات المالية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            "• النشاط اليومي: يوضح عدد المعاملات والعمليات المسجلة كل يوم في هذا الشهر.",
                            "• المصاريف: حجم الإنفاقات المالية اليومية. الدوائر الحمراء الكبرى تدل على مصاريف ضخمة.",
                            "• المداخيل: وارداتك المالية اليومية. الدوائر الخضراء الكبرى تمثل أيام استلام الرواتب والمكاسب.",
                            "• صافي الحركة: يعرض الفارق بين الواردات والمصاريف اليومية.",
                            "• مؤشر السرعة: خوارزمية ذكية تدمج بين تكرار معاملاتك وحجم مبالغك المتداولة."
                        ).forEach { line ->
                            Text(text = line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Right)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("حسناً", fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                }
            )
        }
    }
}
