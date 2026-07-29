package com.qdash.ui.designsystem.components.currency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.ExchangeRate
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
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
    val formatter = DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(java.util.Locale.US))

    val formatVal: (Double?) -> String = { rateVal ->
        if (rateVal == null) "—" else {
            val s = "${formatter.format(rateVal)} دج"
            com.qdash.core.utils.FormatterUtils.convertNumerals(s, useWesternNumerals)
        }
    }
    val buyText = formatVal(rate.parallelBuyRate)
    val sellText = formatVal(rate.parallelSellRate)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Xxs),
        shape = ShapeTokens.Md,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
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
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = SpacingTokens.Sm)
                )
                Column {
                    Text(
                        text = rate.currencyName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${rate.currencyCode} (سوق موازي)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = secondaryText
                    )
                }
            }

            // Buy / Sell rates with independent trend indicators (widened spacing & larger numbers)
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RateDirectionIndicator(trend = rate.parallelBuyTrend, useWesternNumerals = useWesternNumerals)
                    Spacer(Modifier.width(SpacingTokens.Sm))
                    Text(
                        text = "شراء: $buyText",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ColorTokens.Success
                    )
                }
                Spacer(Modifier.height(SpacingTokens.Sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RateDirectionIndicator(trend = rate.parallelSellTrend, useWesternNumerals = useWesternNumerals)
                    Spacer(Modifier.width(SpacingTokens.Sm))
                    Text(
                        text = "بيع: $sellText",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
