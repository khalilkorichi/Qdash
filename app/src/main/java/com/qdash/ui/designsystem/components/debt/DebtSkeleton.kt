package com.qdash.ui.designsystem.components.debt

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
 * Skeleton loading UI for the Debts & Loans Management Screen.
 * Includes shimmer placeholders for Debt Summary Cards and Debtor/Creditor Person Cards.
 */
@Composable
fun DebtSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.Lg)
    ) {
        // ── 1. Summary Owed / Debt Metric Cards Skeleton ──────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Md)
        ) {
            repeat(2) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    shape = ShapeTokens.Lg,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(SpacingTokens.Md),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .shimmerEffect(RoundedCornerShape(3.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(22.dp)
                                .shimmerEffect(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        // ── 2. Add Debt Action Bar Skeleton ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Md)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .shimmerEffect(ShapeTokens.Md)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .shimmerEffect(ShapeTokens.Md)
            )
        }

        // ── 3. Debts List Items Skeleton ─────────────────────────────────────
        repeat(4) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
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
                                .size(44.dp)
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
                                    .width(70.dp)
                                    .height(10.dp)
                                    .shimmerEffect(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(16.dp)
                                .shimmerEffect(RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(12.dp)
                                .shimmerEffect(ShapeTokens.Sm)
                        )
                    }
                }
            }
        }
    }
}
