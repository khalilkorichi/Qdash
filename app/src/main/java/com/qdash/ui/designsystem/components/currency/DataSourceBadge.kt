package com.qdash.ui.designsystem.components.currency

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Unified market header badge displaying data source name, timestamp, and manual refresh button.
 */
@Composable
fun DataSourceBadge(
    sourceText: String,
    lastUpdatedAt: Long?,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    badgeIcon: ImageVector = Icons.Default.Info,
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Sm),
        shape = ShapeTokens.Md,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SpacingTokens.Md, vertical = SpacingTokens.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = ShapeTokens.Sm,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(SpacingTokens.Sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sourceText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitleText = if (lastUpdatedAt != null && lastUpdatedAt > 0L) {
                    com.qdash.core.utils.FormatterUtils.useWesternNumerals = useWesternNumerals
                    "آخر تحديث: ${com.qdash.core.utils.FormatterUtils.formatDateTime(lastUpdatedAt)}"
                } else {
                    "بيانات مخزنة محلياً"
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRefreshClick,
                enabled = !isRefreshing,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "تحديث الأسعار",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

