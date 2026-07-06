package com.qdash.presentation.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.AccountCard
import com.qdash.presentation.home.AccountsRowSkeleton
import com.qdash.domain.model.Account
import com.qdash.ui.theme.TextGray

@Composable
fun HomeAccountsSection(
    accounts: List<Account>,
    accountBalancesVisibility: Map<Long, Boolean>,
    isLoading: Boolean,
    onToggleBalanceVisibility: (Long) -> Unit,
    onAccountClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "حساباتي المالية",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (isLoading) {
            AccountsRowSkeleton()
        } else if (accounts.isEmpty()) {
            Text(
                text = "لا توجد حسابات مضافة حالياً.",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(accounts, key = { it.id }) { acc ->
                    AccountCard(
                        account = acc,
                        showBalance = accountBalancesVisibility[acc.id] ?: true,
                        onToggleBalanceVisibility = { onToggleBalanceVisibility(acc.id) },
                        onClick = { onAccountClick(acc.id) }
                    )
                }
            }
        }
    }
}
