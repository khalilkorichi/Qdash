package com.qdash.presentation.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.ui.components.TransactionItem
import com.qdash.presentation.home.TransactionListSkeleton
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.Category
import com.qdash.domain.model.Account

/**
 * Recent transactions list section embedded directly in the main LazyColumn as a LazyListScope extension.
 * Extracted from HomeScreen to keep it modular and below the 400 lines threshold.
 */
fun LazyListScope.homeRecentTransactionsSection(
    recentTransactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    isLoading: Boolean,
    onViewAllTransactionsClick: () -> Unit,
    primaryColor: Color
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "آخر العمليات والإنفاق",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "عرض الكل",
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor,
                modifier = Modifier
                    .clickable { onViewAllTransactionsClick() }
                    .padding(4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (isLoading) {
        item {
            TransactionListSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
        }
    } else if (recentTransactions.isEmpty()) {
        item {
            EmptyStateView(
                title = "لا توجد معاملات مضافة!",
                description = "اضغط على زر الإضافة العائم لإدراج أول مصروف لك اليوم.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    } else {
        val displayedTxs = recentTransactions.take(4)
        itemsIndexed(displayedTxs, key = { _, tx -> tx.id }) { index, tx ->
            val cat = categories.firstOrNull { it.id == tx.categoryId }
            val accName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: "غير معروف"
            val isLast = index == displayedTxs.lastIndex

            if (isLast) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TransactionItem(
                        transaction = tx,
                        category = cat,
                        accountName = accName,
                        onClick = onViewAllTransactionsClick,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                            .clickable { onViewAllTransactionsClick() }
                    )
                }
            } else {
                TransactionItem(
                    transaction = tx,
                    category = cat,
                    accountName = accName,
                    onClick = onViewAllTransactionsClick,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onViewAllTransactionsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عرض السجل الكامل للمعاملات",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
