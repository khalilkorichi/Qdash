package com.qdash.presentation.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.R
import com.qdash.domain.model.AccountType
import com.qdash.domain.model.SelectedAccountDetailsState
import com.qdash.domain.model.TransactionType
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.theme.TextGray

@Composable
fun SelectedAccountDetailsCard(
    state: SelectedAccountDetailsState,
    modifier: Modifier = Modifier
) {
    val accColorHex = state.account.color.let { if (it.startsWith("#")) it else "#$it" }
    val parsedColor = remember(state.account.color) {
        try { Color(android.graphics.Color.parseColor(accColorHex)) } catch (_: Exception) { Color.Gray }
    }
    
    Card(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .border(BorderStroke(1.dp, parsedColor.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Account icon — show logo for BARIDIMOB, mapped icon for others
                if (state.account.type == AccountType.BARIDIMOB) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_baridimob),
                        contentDescription = "بريدي موب",
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = accountIconVector(state.account.icon, state.account.type),
                        contentDescription = state.account.name,
                        tint = parsedColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(state.account.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("الرصيد الحالي: ${FormatterUtils.formatCurrency(state.account.balance)}", fontSize = 11.sp, color = TextGray)
                }
            }
            
            if (state.recentTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("المعاملات الأخيرة:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                Spacer(modifier = Modifier.height(4.dp))
                state.recentTransactions.forEach { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tx.note ?: "عملية مالية", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(
                            text = "${if (tx.type == TransactionType.EXPENSE) "-" else "+"}${FormatterUtils.formatCurrency(tx.amount)}",
                            fontSize = 10.sp,
                            color = if (tx.type == TransactionType.EXPENSE) Color(0xFFEF4444) else Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (state.activeGoals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("الأهداف الادخارية النشطة:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                Spacer(modifier = Modifier.height(4.dp))
                state.activeGoals.forEach { goal ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(goal.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${FormatterUtils.formatCurrency(goal.currentAmount)} / ${FormatterUtils.formatCurrency(goal.targetAmount)}", fontSize = 9.sp, color = TextGray)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val progress = if (goal.targetAmount > 0.0) goal.currentAmount / goal.targetAmount else 0.0
                        LinearProgressIndicator(
                            progress = progress.toFloat(),
                            color = parsedColor,
                            trackColor = parsedColor.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

private fun accountIconVector(icon: String, type: AccountType): ImageVector = when (type) {
    AccountType.BANK       -> Icons.Default.AccountBalance
    AccountType.CCP        -> Icons.Default.CreditCard
    AccountType.BARIDIMOB  -> Icons.Default.PhoneAndroid // fallback; logo image shown above
    AccountType.CASH       -> Icons.Default.Payments
    AccountType.SAVINGS    -> Icons.Default.Savings
    AccountType.WALLET     -> Icons.Default.AccountBalanceWallet
    AccountType.OTHER      -> Icons.Default.MonetizationOn
}

