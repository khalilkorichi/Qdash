package com.qdash.presentation.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.presentation.home.QuickActionTile
import com.qdash.ui.designsystem.components.BentoGridCell
import com.qdash.ui.designsystem.components.ThreeColumnBentoGrid
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TransferBlue

private data class QuickActionBentoItem(
    val icon: ImageVector,
    val label: String,
    val iconBg: Brush,
    val iconColor: Color,
    val onClick: () -> Unit
)

private val CurrencyGold = Color(0xFFD4A017)

@Composable
fun QuickActionsSection(
    onAddTransactionClick: () -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onSavingsClick: () -> Unit,
    onSubscriptionsClick: () -> Unit,
    onDocumentSimulatorClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onCurrencyExchangeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val bentoRows = remember(
        primary,
        onAddTransactionClick,
        onViewAllTransactionsClick,
        onSavingsClick,
        onSubscriptionsClick,
        onDocumentSimulatorClick,
        onCurrencyExchangeClick,
        onAiAssistantClick
    ) {
        val primaryBg = Brush.linearGradient(
            listOf(primary.copy(alpha = 0.25f), primary.copy(alpha = 0.08f))
        )
        listOf(
            listOf(
                BentoGridCell(
                    value = QuickActionBentoItem(
                        icon = Icons.Default.Payments,
                        label = "إضافة معاملة",
                        iconBg = primaryBg,
                        iconColor = primary,
                        onClick = onAddTransactionClick
                    ),
                    isWide = true
                ),
                BentoGridCell(
                    value = QuickActionBentoItem(
                        icon = Icons.Default.ReceiptLong,
                        label = "سجل المعاملات",
                        iconBg = Brush.linearGradient(
                            listOf(TransferBlue.copy(alpha = 0.25f), TransferBlue.copy(alpha = 0.08f))
                        ),
                        iconColor = TransferBlue,
                        onClick = onViewAllTransactionsClick
                    )
                )
            ),
            listOf(
                BentoGridCell(
                    value = QuickActionBentoItem(
                        icon = Icons.Default.Savings,
                        label = "حصالة الادخار",
                        iconBg = Brush.linearGradient(
                            listOf(SavingsAmber.copy(alpha = 0.25f), SavingsAmber.copy(alpha = 0.08f))
                        ),
                        iconColor = SavingsAmber,
                        onClick = onSavingsClick
                    )
                ),
                BentoGridCell(
                    value = QuickActionBentoItem(
                        icon = Icons.Default.CardMembership,
                        label = "الاشتراكات",
                        iconBg = Brush.linearGradient(
                            listOf(ExpenseRed.copy(alpha = 0.20f), ExpenseRed.copy(alpha = 0.06f))
                        ),
                        iconColor = ExpenseRed,
                        onClick = onSubscriptionsClick
                    )
                ),
                BentoGridCell(
                    value = QuickActionBentoItem(
                        icon = Icons.Default.Description,
                        label = "المساعد البريدي",
                        iconBg = Brush.linearGradient(
                            listOf(IncomeGreen.copy(alpha = 0.25f), IncomeGreen.copy(alpha = 0.08f))
                        ),
                        iconColor = IncomeGreen,
                        onClick = onDocumentSimulatorClick
                    )
                )
            ),
            listOf(
                BentoGridCell(
                    value = QuickActionBentoItem(
                        icon = Icons.Default.CurrencyExchange,
                        label = "أسعار الصرف",
                        iconBg = Brush.linearGradient(
                            listOf(CurrencyGold.copy(alpha = 0.28f), CurrencyGold.copy(alpha = 0.08f))
                        ),
                        iconColor = CurrencyGold,
                        onClick = onCurrencyExchangeClick
                    )
                ),
                BentoGridCell(
                    value = QuickActionBentoItem(
                        icon = Icons.Default.Android,
                        label = "المساعد الذكي AI",
                        iconBg = primaryBg,
                        iconColor = primary,
                        onClick = onAiAssistantClick
                    ),
                    isWide = true
                )
            )
        )
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "الوصول السريع",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        ThreeColumnBentoGrid(rows = bentoRows) { cellModifier, cell ->
            val item = cell.value
            QuickActionTile(
                modifier = cellModifier,
                icon = item.icon,
                label = item.label,
                iconBg = item.iconBg,
                iconColor = item.iconColor,
                isWide = cell.isWide,
                onClick = item.onClick
            )
        }
    }
}
