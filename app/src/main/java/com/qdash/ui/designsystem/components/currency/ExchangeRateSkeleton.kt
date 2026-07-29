package com.qdash.ui.designsystem.components.currency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qdash.ui.designsystem.components.shimmerEffect
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Skeleton loading UI for the Currency Exchange section (Official & Parallel market tabs).
 * Mimics the exact layout of [DataSourceBadge] and currency rate card items with an animated shimmer effect.
 */
@Composable
fun ExchangeRateSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = SpacingTokens.Sm)
    ) {
        // ── 1. Header Badge Skeleton (DataSourceBadge) ───────────────────────
        Surface(
            modifier = Modifier
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
                // Icon box placeholder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .shimmerEffect(ShapeTokens.Sm)
                )
                Spacer(Modifier.width(SpacingTokens.Sm))
                Column(modifier = Modifier.weight(1f)) {
                    // Title placeholder
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(14.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    // Subtitle timestamp placeholder
                    Box(
                        modifier = Modifier
                            .width(170.dp)
                            .height(11.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
                // Refresh button placeholder
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .shimmerEffect(CircleShape)
                )
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        Spacer(Modifier.height(SpacingTokens.Xs))

        // ── 2. Card Items Skeletons ───────────────────────────────────────────
        repeat(itemCount) {
            ExchangeRateCardSkeleton()
        }
    }
}

@Composable
fun ExchangeRateCardSkeleton(
    modifier: Modifier = Modifier
) {
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
                // Flag emoji circle placeholder
                Box(
                    modifier = Modifier
                        .padding(end = SpacingTokens.Sm)
                        .size(28.dp)
                        .shimmerEffect(CircleShape)
                )
                Column {
                    // Currency Name placeholder
                    Box(
                        modifier = Modifier
                            .width(85.dp)
                            .height(15.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    // Currency Code placeholder
                    Box(
                        modifier = Modifier
                            .width(45.dp)
                            .height(11.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
            }

            // Buy / Sell rates with trend indicator placeholders
            Column(horizontalAlignment = Alignment.End) {
                // Buy rate row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Trend pill placeholder
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(16.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(SpacingTokens.Sm))
                    // Buy rate text placeholder
                    Box(
                        modifier = Modifier
                            .width(105.dp)
                            .height(16.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
                Spacer(Modifier.height(SpacingTokens.Sm))
                // Sell rate row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Trend pill placeholder
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(16.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(SpacingTokens.Sm))
                    // Sell rate text placeholder
                    Box(
                        modifier = Modifier
                            .width(105.dp)
                            .height(16.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
