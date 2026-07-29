package com.qdash.presentation.currency.tabs

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.presentation.currency.CurrencyExchangeViewModel
import com.qdash.ui.designsystem.components.currency.CurrencyConverterCard
import com.qdash.ui.designsystem.components.currency.ManualRateInputField
import com.qdash.ui.designsystem.components.currency.MarketAndDirectionSelector
import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Currency converter tab.
 * Includes double selection row (MarketType & TradeDirection) directly above the converter card.
 */
@Composable
fun ConverterTab(
    viewModel: CurrencyExchangeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.converterState.collectAsStateWithLifecycle()
    val useWesternNumerals by viewModel.useWesternNumerals.collectAsStateWithLifecycle()
    var showManualSection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadConverterRates()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SpacingTokens.Lg),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.Md)
    ) {
        // ── 2-Row MarketType & TradeDirection selector ───────────────────────
        MarketAndDirectionSelector(
            selectedMarketType = state.selectedMarketType,
            selectedTradeDirection = state.selectedTradeDirection,
            onMarketTypeSelected = viewModel::onMarketTypeSelected,
            onTradeDirectionSelected = viewModel::onTradeDirectionSelected,
            enabled = !state.useManualRate
        )

        // ── Converter card ────────────────────────────────────────────────────
        CurrencyConverterCard(
            amount = state.amount,
            fromCurrency = state.fromCurrency,
            toCurrency = state.toCurrency,
            result = state.result,
            availableCurrencies = state.availableRates,
            onAmountChange = viewModel::onAmountChange,
            onFromCurrencyChange = viewModel::onFromCurrencyChange,
            onToCurrencyChange = viewModel::onToCurrencyChange,
            onSwap = viewModel::swapCurrencies,
            useWesternNumerals = useWesternNumerals
        )

        // ── Arabic words (Centimes explanation) ────────────────────────────────
        val wordsText = state.amountInWords?.let {
            if (useWesternNumerals) it else com.qdash.core.utils.FormatterUtils.convertNumerals(it)
        } ?: ""

        AnimatedVisibility(
            visible = wordsText.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SpacingTokens.Xs),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = com.qdash.ui.designsystem.tokens.ShapeTokens.Lg,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = wordsText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal,
                            textAlign = TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Md)
                    )
                }
            }
        }

        // ── Manual rate toggle ────────────────────────────────────────────────
        TextButton(
            onClick = { showManualSection = !showManualSection },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (showManualSection) "إخفاء سعر الصرف اليدوي" else "إدخال سعر صرف يدوي مؤقت",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // ── Manual rate input ─────────────────────────────────────────────────
        if (showManualSection) {
            ManualRateInputField(
                label = "سعر الصرف اليدوي المؤقت (دينار مقابل 1 وحدة)",
                value = state.manualRate,
                onValueChange = viewModel::onManualRateChange
            )
        }
    }
}
