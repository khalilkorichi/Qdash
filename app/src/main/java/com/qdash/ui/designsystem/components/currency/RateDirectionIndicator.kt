package com.qdash.ui.designsystem.components.currency

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.RateDirection
import com.qdash.domain.model.RateTrend
import com.qdash.ui.designsystem.tokens.ColorTokens
import java.util.Locale

/**
 * Visual indicator displaying rate movement (UP, DOWN, STABLE) with percentage change.
 * Uses AutoMirrored Material Icons and ColorTokens (Success / Danger / TextMuted).
 */
@Composable
fun RateDirectionIndicator(
    trend: RateTrend?,
    useWesternNumerals: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (trend == null) return

    val isDark = isSystemInDarkTheme()
    val (color, icon) = when (trend.direction) {
        RateDirection.UP -> Pair(
            ColorTokens.Success,
            Icons.AutoMirrored.Filled.TrendingUp
        )
        RateDirection.DOWN -> Pair(
            ColorTokens.Danger,
            Icons.AutoMirrored.Filled.TrendingDown
        )
        RateDirection.STABLE -> Pair(
            if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight,
            Icons.Default.Remove
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        if (trend.direction != RateDirection.STABLE && trend.changePercentage != 0.0) {
            val prefix = if (trend.changePercentage > 0) "+" else ""
            val rawPercentage = String.format(Locale.US, "%.1f", trend.changePercentage)
            val formattedPercentage = com.qdash.core.utils.FormatterUtils.convertNumerals(rawPercentage, useWesternNumerals)
            Text(
                text = "$prefix$formattedPercentage%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}
