package com.qdash.ui.designsystem.components.transactions

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
 * Skeleton loading UI for the Transactions list screen.
 * Includes shimmer placeholders for Filter Chips, Search Input, Date Header, and Transaction Item Cards.
 */
@Composable
fun TransactionsSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.Md)
    ) {
        // ── 1. Search Bar Skeleton ───────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = ShapeTokens.Lg,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = SpacingTokens.Md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .shimmerEffect(CircleShape)
                )
                Spacer(Modifier.width(SpacingTokens.Md))
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
        }

        // ── 2. Filter Chips Row Skeleton ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Sm)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(75.dp)
                        .height(34.dp)
                        .shimmerEffect(CircleShape)
                )
            }
        }

        Spacer(Modifier.height(SpacingTokens.Xs))

        // ── 3. Date Group Header Skeleton ────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(14.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(12.dp)
                    .shimmerEffect(RoundedCornerShape(3.dp))
            )
        }

        // ── 4. Transaction Item Cards Skeletons ──────────────────────────────
        repeat(5) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
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
                                .size(40.dp)
                                .shimmerEffect(CircleShape)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(14.dp)
                                    .shimmerEffect(RoundedCornerShape(4.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .width(75.dp)
                                    .height(10.dp)
                                    .shimmerEffect(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(85.dp)
                            .height(16.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
