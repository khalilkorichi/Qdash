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
 * Single card displaying one currency's official exchange rate with buy/sell trend indicators.
 */
@Composable
fun ExchangeRateListItem(
    rate: ExchangeRate,
    useWesternNumerals: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val secondaryText = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight
    val formatter = DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(java.util.Locale.US))
    val formatRate: (Double) -> String = { rateVal ->
        val raw = formatter.format(rateVal)
        com.qdash.core.utils.FormatterUtils.convertNumerals(raw, useWesternNumerals)
    }

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
            // Flag + Name + Code (RTL: appears on the right)
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
                        text = rate.currencyCode,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = secondaryText
                    )
                }
            }

            // Buy / Sell rates with trend indicators (widened spacing & larger numbers)
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RateDirectionIndicator(trend = rate.officialBuyTrend, useWesternNumerals = useWesternNumerals)
                    Spacer(Modifier.width(SpacingTokens.Sm))
                    Text(
                        text = "شراء: ${formatRate(rate.officialBuyRate)} دج",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ColorTokens.Success
                    )
                }
                Spacer(Modifier.height(SpacingTokens.Sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RateDirectionIndicator(trend = rate.officialSellTrend, useWesternNumerals = useWesternNumerals)
                    Spacer(Modifier.width(SpacingTokens.Sm))
                    Text(
                        text = "بيع: ${formatRate(rate.officialSellRate)} دج",
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
