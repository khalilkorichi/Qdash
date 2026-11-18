package com.qdash.presentation.currency.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.presentation.currency.CurrencyExchangeViewModel
import com.qdash.presentation.currency.OfficialRatesUiState
import com.qdash.ui.designsystem.components.currency.DataSourceBadge
import com.qdash.ui.designsystem.components.currency.ExchangeRateSkeleton
import com.qdash.ui.designsystem.components.currency.ParallelMarketRateCard
import com.qdash.ui.designsystem.tokens.ShapeTokens
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
            ExchangeRateSkeleton(modifier = modifier)
        }

        is OfficialRatesUiState.Error,
        is OfficialRatesUiState.Success -> {
            val rates = (s as? OfficialRatesUiState.Success)?.rates ?: emptyList()
            val maxLastUpdated = rates.maxOfOrNull { it.lastUpdatedAt }?.takeIf { it > 0L }
            val allParallelNull = rates.isNotEmpty() && rates.all { it.parallelBuyRate == null }

            LazyColumn(modifier = modifier.fillMaxSize()) {
                item {
                    // Transparent Source & Refresh Badge
                    DataSourceBadge(
                        sourceText = "أسعار إرشادية (السوق الموازي)",
                        lastUpdatedAt = if (allParallelNull) null else maxLastUpdated,
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

                if (allParallelNull && !converterState.isRefreshingParallel) {
                    item {
                        Surface(
                            onClick = viewModel::refreshParallelRatesManually,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Sm),
                            shape = ShapeTokens.Md,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(SpacingTokens.Md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "لم يتم تحميل أسعار السوق الموازي بعد. اضغط لإعادة المحاولة.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
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
