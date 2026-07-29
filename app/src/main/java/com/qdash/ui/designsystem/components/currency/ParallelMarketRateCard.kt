package com.qdash.ui.designsystem.components.currency

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.ExchangeRate
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens
import java.text.DecimalFormat

/**
 * Currency item for Parallel Market Tab displaying independent buy and sell rates with trend indicators.
 */
@Composable
fun ParallelMarketRateCard(
    rate: ExchangeRate,
    useWesternNumerals: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val secondaryText = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight
    val formatter = DecimalFormat("#,##0.00")

    val formatVal: (Double?) -> String = { rateVal ->
        if (rateVal == null) "—" else {
            val s = "${formatter.format(rateVal)} دج"
            if (useWesternNumerals) s else com.qdash.core.utils.FormatterUtils.convertNumerals(s)
        }
    }
    val buyText = formatVal(rate.parallelBuyRate)
    val sellText = formatVal(rate.parallelSellRate)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag + Name + Code
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = rate.countryFlagEmoji,
                    fontSize = 26.sp,
                    modifier = Modifier.padding(end = SpacingTokens.Sm)
                )
                Column {
                    Text(
                        text = rate.currencyName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${rate.currencyCode} (سوق موازي)",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText
                    )
                }
            }

            // Buy / Sell rates with independent trend indicators
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RateDirectionIndicator(trend = rate.parallelBuyTrend, useWesternNumerals = useWesternNumerals)
                    Spacer(Modifier.width(SpacingTokens.Xs))
                    Text(
                        text = "شراء: $buyText",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ColorTokens.Success
                    )
                }
                Spacer(Modifier.height(SpacingTokens.Xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RateDirectionIndicator(trend = rate.parallelSellTrend, useWesternNumerals = useWesternNumerals)
                    Spacer(Modifier.width(SpacingTokens.Xs))
                    Text(
                        text = "بيع: $sellText",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = SpacingTokens.Lg),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}
