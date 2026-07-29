package com.qdash.ui.designsystem.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Skeleton loading UI for the Analytics & Reports Screen.
 * Includes shimmer placeholders for Date Navigator, Summary Cards, Chart Panel, and Insights List.
 */
@Composable
fun AnalyticsSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.Lg)
    ) {
        // ── 1. Date Navigator Skeleton ──────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = ShapeTokens.Md,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.Md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .shimmerEffect(CircleShape)
                )
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(16.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .shimmerEffect(CircleShape)
                )
            }
        }

        // ── 2. Metric Summary Cards Skeletons ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Md)
        ) {
            repeat(2) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp),
                    shape = ShapeTokens.Md,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(SpacingTokens.Md),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(12.dp)
                                .shimmerEffect(RoundedCornerShape(3.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(20.dp)
                                .shimmerEffect(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        // ── 3. Interactive Chart Card Skeleton ──────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = ShapeTokens.Lg,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SpacingTokens.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Donut circle placeholder
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .shimmerEffect(CircleShape)
                )
                Spacer(Modifier.height(SpacingTokens.Md))
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(14.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
        }

        // ── 4. Categories List Skeletons ──────────────────────────────────────
        repeat(3) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = ShapeTokens.Md,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .shimmerEffect(CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(14.dp)
                                .shimmerEffect(RoundedCornerShape(4.dp))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(16.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
