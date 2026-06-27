package com.example.presentation.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.LowBalanceAlertState
import com.example.core.utils.FormatterUtils
import com.example.ui.theme.TextGray

@Composable
fun LowBalanceAlertCard(
    state: LowBalanceAlertState,
    messageId: String,
    editedLimit: Double?,
    onLimitFieldChange: (String, Double) -> Unit,
    onSaveLimitClick: (String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLimit = editedLimit ?: state.limit
    var inputVal by remember(messageId, currentLimit) { mutableStateOf(currentLimit.toLong().toString()) }

    Card(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "⚠️ تنبيه الرصيد المنخفض",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFFEF4444)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("تنبيه عند:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { raw ->
                        inputVal = raw.filter { it.isDigit() }
                        val d = inputVal.toDoubleOrNull() ?: 0.0
                        onLimitFieldChange(messageId, d)
                    },
                    modifier = Modifier.width(80.dp).height(46.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Text("دج", fontSize = 11.sp, color = TextGray)
                Button(
                    onClick = { onSaveLimitClick(messageId, inputVal.toDoubleOrNull() ?: 5000.0) },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("حفظ", fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            if (state.accountsUnderLimit.isEmpty()) {
                Text("كل الحسابات فوق الحد المضبوط. ✅", fontSize = 11.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
            } else {
                state.accountsUnderLimit.forEach { alert ->
                    val colorHex = alert.color.let { if (it.startsWith("#")) it else "#$it" }
                    val parsedColor = remember(alert.color) {
                        try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.Gray }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(parsedColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(alert.accountName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            FormatterUtils.formatCurrency(alert.currentBalance),
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
