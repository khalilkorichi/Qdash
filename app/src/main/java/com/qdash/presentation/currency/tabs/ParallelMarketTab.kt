package com.qdash.presentation.currency.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.presentation.currency.CurrencyExchangeViewModel
import com.qdash.presentation.currency.OfficialRatesUiState
import com.qdash.ui.designsystem.components.AppLoadingState
import com.qdash.ui.designsystem.components.currency.DataSourceBadge
import com.qdash.ui.designsystem.components.currency.ParallelMarketRateCard
import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Parallel market tab displaying real-time scraped buy and sell rates from forexalgerie.com,
 * with trend indicators, source attribution badge, and manual refresh button.
 */
@Composable
fun ParallelMarketTab(
    viewModel: CurrencyExchangeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.officialRatesState.collectAsStateWithLifecycle()
    val converterState by viewModel.converterState.collectAsStateWithLifecycle()
    val useWesternNumerals by viewModel.useWesternNumerals.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadOfficialRates()
    }

    when (val s = state) {
        is OfficialRatesUiState.Idle,
        is OfficialRatesUiState.Loading -> {
            AppLoadingState(modifier = modifier.fillMaxSize())
        }

        is OfficialRatesUiState.Error,
        is OfficialRatesUiState.Success -> {
            val rates = (s as? OfficialRatesUiState.Success)?.rates ?: emptyList()
            val maxLastUpdated = rates.maxOfOrNull { it.lastUpdatedAt }?.takeIf { it > 0L }

            LazyColumn(modifier = modifier.fillMaxSize()) {
                item {
                    // Transparent Source & Refresh Badge
                    DataSourceBadge(
                        sourceText = "أسعار إرشادية (السوق الموازي)",
                        lastUpdatedAt = maxLastUpdated,
                        isRefreshing = converterState.isRefreshingParallel,
                        onRefreshClick = viewModel::refreshParallelRatesManually,
                        badgeIcon = Icons.Default.Storefront,
                        useWesternNumerals = useWesternNumerals
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                }

                items(rates, key = { it.currencyCode }) { rate ->
                    ParallelMarketRateCard(
                        rate = rate,
                        useWesternNumerals = useWesternNumerals
                    )
                }

                item { Spacer(Modifier.height(SpacingTokens.Giant)) }
            }
        }
    }
}
