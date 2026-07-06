package com.qdash.presentation.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.presentation.home.QuickActionTile
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TransferBlue

@Composable
fun QuickActionsSection(
    onAddTransactionClick: () -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onSavingsClick: () -> Unit,
    onSubscriptionsClick: () -> Unit,
    onDocumentSimulatorClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "الوصول السريع",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Payments,
                label = "إضافة معاملة",
                iconBg = Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.25f), primary.copy(alpha = 0.08f))
                ),
                iconColor = primary,
                onClick = onAddTransactionClick
            )
            QuickActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ReceiptLong,
                label = "سجل المعاملات",
                iconBg = Brush.linearGradient(
                    listOf(TransferBlue.copy(alpha = 0.25f), TransferBlue.copy(alpha = 0.08f))
                ),
                iconColor = TransferBlue,
                onClick = onViewAllTransactionsClick
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Savings,
                label = "حصالة الادخار",
                iconBg = Brush.linearGradient(
                    listOf(SavingsAmber.copy(alpha = 0.25f), SavingsAmber.copy(alpha = 0.08f))
                ),
                iconColor = SavingsAmber,
                onClick = onSavingsClick
            )
            QuickActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CardMembership,
                label = "الاشتراكات",
                iconBg = Brush.linearGradient(
                    listOf(ExpenseRed.copy(alpha = 0.20f), ExpenseRed.copy(alpha = 0.06f))
                ),
                iconColor = ExpenseRed,
                onClick = onSubscriptionsClick
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Description,
                label = "المساعد البريدي",
                iconBg = Brush.linearGradient(
                    listOf(IncomeGreen.copy(alpha = 0.25f), IncomeGreen.copy(alpha = 0.08f))
                ),
                iconColor = IncomeGreen,
                onClick = onDocumentSimulatorClick
            )
            QuickActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Android,
                label = "المساعد الذكي AI",
                iconBg = Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.25f), primary.copy(alpha = 0.08f))
                ),
                iconColor = primary,
                onClick = onAiAssistantClick
            )
        }
    }
}
