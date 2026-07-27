package com.qdash.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.StableList
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.components.getIconByName
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TextGray

/**
 * Renders a list of [Transaction]s inside an account card using [LazyColumn]
 * with [key] and [contentType] instead of a `forEach` loop in a [Column].
 *
 * Benefits:
 * - Only visible rows are composed (virtual list recycling).
 * - `key` ensures Compose can identify and skip unchanged items.
 * - `contentType` groups similar items for maximum composition reuse.
 * - [userScrollEnabled] = false because this is nested inside [AccountsScreen]'s parent LazyColumn.
 * - [heightIn] bounds prevent infinite height in the parent scroll.
 */
@Composable
fun AccountBoxTransactionList(
    filteredTransactions: List<Transaction>,
    categories: StableList<Category>,
    accountId: Long,
    accentColor: Color,
    emptyText: String = "لا توجد عمليات مسجلة لهذا الحساب حالياً.",
    modifier: Modifier = Modifier
) {
    if (filteredTransactions.isEmpty()) {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray.copy(alpha = 0.8f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = filteredTransactions,
            key = { tx -> tx.id },
            contentType = { "account_tx_row" }
        ) { tx ->
            AccountTransactionRow(
                transaction = tx,
                category = categories.items.firstOrNull { it.id == tx.categoryId },
                accountId = accountId,
                accentColor = accentColor
            )
        }
    }
}

/**
 * A single transaction row inside an account card.
 * Extracted as a separate composable so the Compose compiler can independently
 * skip it when its inputs are stable and unchanged between recompositions.
 */
@Composable
private fun AccountTransactionRow(
    transaction: Transaction,
    category: Category?,
    accountId: Long,
    accentColor: Color
) {
    val isIncoming = transaction.type == TransactionType.INCOME ||
            (transaction.type == TransactionType.TRANSFER && transaction.toAccountId == accountId)
    val amountColor = if (isIncoming) IncomeGreen else ExpenseRed
    val amountPrefix = if (isIncoming) "+" else "-"

    val catColor = remember(category?.color) {
        runCatching {
            Color(android.graphics.Color.parseColor(category?.color ?: "#6C63FF"))
        }.getOrElse { accentColor }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(catColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(category?.icon ?: "receipt_long"),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = transaction.note ?: category?.name ?: "عملية مالية",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FormatterUtils.formatDate(transaction.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = FormatterUtils.formatCurrency(transaction.amount, amountPrefix),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = amountColor
        )
    }
}
