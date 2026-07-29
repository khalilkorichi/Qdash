package com.qdash.presentation.currency.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.presentation.currency.CurrencyExchangeViewModel
import com.qdash.presentation.currency.OfficialRatesUiState
import com.qdash.ui.designsystem.components.AppLoadingState
import com.qdash.ui.designsystem.components.currency.ExchangeRateListItem
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            Column(
                modifier = modifier.fillMaxSize().padding(SpacingTokens.Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚠️ فشل تحميل الأسعار",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorTokens.Danger
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { viewModel.refreshOfficialRates() }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("إعادة المحاولة")
                }
            }
        }

        is OfficialRatesUiState.Success -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                // Header with last updated time + refresh button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "الأسعار الرسمية",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            s.lastUpdated?.let { ts ->
                                val sdf = SimpleDateFormat("dd MMM yyyy — HH:mm", Locale.US)
                                val rawDate = sdf.format(Date(ts))
                                val displayDate = com.qdash.core.utils.FormatterUtils.convertNumerals(rawDate, useWesternNumerals)
                                Text(
                                    text = "آخر تحديث: $displayDate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            } ?: Text(
                                text = "بيانات مخزنة محلياً",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTokens.Warning
                            )
                        }
                        IconButton(onClick = { viewModel.refreshOfficialRates() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "تحديث الأسعار",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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
