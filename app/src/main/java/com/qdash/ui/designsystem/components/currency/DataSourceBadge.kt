package com.qdash.ui.designsystem.components.currency

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Transparent source disclaimer badge displaying data source, timestamp, and manual refresh button.
 */
@Composable
fun DataSourceBadge(
    sourceText: String,
    lastUpdatedAt: Long?,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    useWesternNumerals: Boolean = true,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing) {
            infiniteRepeatable(tween(1000, easing = LinearEasing))
        } else {
            tween(300)
        },
        label = "refreshRotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Sm)
            .clip(ShapeTokens.Md)
            .background(ColorTokens.Primary.copy(alpha = 0.08f))
            .padding(horizontal = SpacingTokens.Md, vertical = SpacingTokens.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = ColorTokens.Primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sourceText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (lastUpdatedAt != null && lastUpdatedAt > 0L) {
                com.qdash.core.utils.FormatterUtils.useWesternNumerals = useWesternNumerals
                val formattedTime = com.qdash.core.utils.FormatterUtils.formatDateTime(lastUpdatedAt)
                Text(
                    text = "آخر تحديث: $formattedTime",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        IconButton(
            onClick = onRefreshClick,
            enabled = !isRefreshing,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "تحديث الأسعار",
                tint = ColorTokens.Primary,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation)
            )
        }
    }
}
