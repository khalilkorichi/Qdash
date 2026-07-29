package com.qdash.presentation.currency.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.presentation.currency.CurrencyExchangeViewModel
import com.qdash.presentation.currency.OfficialRatesUiState
import com.qdash.ui.designsystem.components.AppLoadingState
import com.qdash.ui.designsystem.components.currency.DataSourceBadge
import com.qdash.ui.designsystem.components.currency.ExchangeRateListItem
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Tab displaying official exchange rates from the central bank / API.
 * Lazy-loaded: calls [viewModel.loadOfficialRates()] on first composition.
 */
@Composable
fun OfficialMarketTab(
    viewModel: CurrencyExchangeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.officialRatesState.collectAsStateWithLifecycle()
    val useWesternNumerals by viewModel.useWesternNumerals.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadOfficialRates()
    }

    when (val s = state) {
        is OfficialRatesUiState.Idle,
        is OfficialRatesUiState.Loading -> {
            AppLoadingState(modifier = modifier.fillMaxSize())
        }

        is OfficialRatesUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        is OfficialRatesUiState.Success -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                // Header with last updated time + refresh button
                item {
                    DataSourceBadge(
                        sourceText = "أسعار البنك المركزي الرسمي",
                        lastUpdatedAt = s.lastUpdated,
                        isRefreshing = false,
                        onRefreshClick = { viewModel.refreshOfficialRates() },
                        badgeIcon = Icons.Default.AccountBalance,
                        useWesternNumerals = useWesternNumerals
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                }

                items(s.rates, key = { it.currencyCode }) { rate ->
                    ExchangeRateListItem(
                        rate = rate,
                        useWesternNumerals = useWesternNumerals
                    )
                }

                item { Spacer(Modifier.height(SpacingTokens.Giant)) }
            }
        }
    }
}
