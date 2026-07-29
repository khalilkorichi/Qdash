package com.qdash.ui.designsystem.components.currency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.RateDirection
import com.qdash.domain.model.RateTrend
import com.qdash.ui.designsystem.tokens.ColorTokens
import java.util.Locale

/**
 * Visual rectangular indicator pill displaying rate movement (UP, DOWN, STABLE)
 * with matching background tint, stroke border, icon, and percentage change.
 */
@Composable
fun RateDirectionIndicator(
    trend: RateTrend?,
    useWesternNumerals: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (trend == null) return

    val (bgColor, borderColor, contentColor, icon) = when (trend.direction) {
        RateDirection.UP -> Tuple4(
            ColorTokens.Success.copy(alpha = 0.12f),
            ColorTokens.Success.copy(alpha = 0.35f),
            ColorTokens.Success,
            Icons.AutoMirrored.Filled.TrendingUp
        )
        RateDirection.DOWN -> Tuple4(
            ColorTokens.Danger.copy(alpha = 0.12f),
            ColorTokens.Danger.copy(alpha = 0.35f),
            ColorTokens.Danger,
            Icons.AutoMirrored.Filled.TrendingDown
        )
        RateDirection.STABLE -> Tuple4(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Remove
        )
    }

    val percentageText = if (trend.changePercentage != 0.0) {
        val prefix = if (trend.changePercentage > 0) "+" else ""
        val rawPercentage = String.format(Locale.US, "%.1f", trend.changePercentage)
        val formattedPercentage = com.qdash.core.utils.FormatterUtils.convertNumerals(rawPercentage, useWesternNumerals)
        "$prefix$formattedPercentage%"
    } else {
        "0.0%"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = percentageText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val a: A,
    val b: B,
    val c: C,
    val d: D
)

