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
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val secondaryText = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight
    val formatter = DecimalFormat("#,##0.00")

    val buyText = rate.parallelBuyRate?.let { "${formatter.format(it)} دج" } ?: "—"
    val sellText = rate.parallelSellRate?.let { "${formatter.format(it)} دج" } ?: "—"

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
                    RateDirectionIndicator(trend = rate.parallelBuyTrend)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "شراء: $buyText",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ColorTokens.Success
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RateDirectionIndicator(trend = rate.parallelSellTrend)
                    Spacer(Modifier.width(4.dp))
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
