package com.qdash.ui.designsystem.components.currency

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.MarketType
import com.qdash.domain.model.TradeDirection
import com.qdash.ui.designsystem.components.SegmentedPillToggle
import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Double selection row component for the Currency Converter tab.
 *
 * Row 1: MarketType (PARALLEL | OFFICIAL)
 * Row 2: TradeDirection (BUY | SELL)
 */
@Composable
fun MarketAndDirectionSelector(
    selectedMarketType: MarketType,
    selectedTradeDirection: TradeDirection,
    onMarketTypeSelected: (MarketType) -> Unit,
    onTradeDirectionSelected: (TradeDirection) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.Sm)
    ) {
        // Row 1: MarketType (RTL: PARALLEL on right, OFFICIAL on left)
        SegmentedPillToggle(
            option1Label = MarketType.PARALLEL.labelArabic,
            option2Label = MarketType.OFFICIAL.labelArabic,
            selectedIndex = if (selectedMarketType == MarketType.PARALLEL) 0 else 1,
            onOptionSelected = { index ->
                val selected = if (index == 0) MarketType.PARALLEL else MarketType.OFFICIAL
                onMarketTypeSelected(selected)
            },
            enabled = enabled
        )

        // Row 2: TradeDirection (RTL: BUY on right, SELL on left)
        SegmentedPillToggle(
            option1Label = TradeDirection.BUY.labelArabic,
            option2Label = TradeDirection.SELL.labelArabic,
            selectedIndex = if (selectedTradeDirection == TradeDirection.BUY) 0 else 1,
            onOptionSelected = { index ->
                val selected = if (index == 0) TradeDirection.BUY else TradeDirection.SELL
                onTradeDirectionSelected(selected)
            },
            enabled = enabled
        )
    }
}
