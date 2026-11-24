package com.qdash.ui.designsystem.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qdash.ui.designsystem.components.BentoGridCell
import com.qdash.ui.designsystem.components.ThreeColumnBentoGrid
import com.qdash.ui.designsystem.components.shimmerEffect
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Skeleton loading UI for the Home screen.
 * Shimmer-animated placeholders matching the Balance Hero Card, Quick Actions Bento Grid,
 * and Recent Transactions list.
 */
@Composable
fun HomeSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = false)
            .padding(horizontal = SpacingTokens.Lg, vertical = SpacingTokens.Md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.Lg)
    ) {
        // ── 1. Hero Balance Card Skeleton ────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = ShapeTokens.Lg,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(SpacingTokens.Lg),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
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
                            .size(32.dp)
                            .shimmerEffect(CircleShape)
                    )
                }

                // Balance Number bar
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(32.dp)
                        .shimmerEffect(RoundedCornerShape(6.dp))
                )

                // Income / Expense summary pills row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(36.dp)
                            .shimmerEffect(ShapeTokens.Sm)
                    )
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(36.dp)
                            .shimmerEffect(ShapeTokens.Sm)
                    )
                }
            }
        }

        // ── 2. Quick Actions Bento Grid Skeleton ──────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(16.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
            val tileShape = RoundedCornerShape(20.dp)
            val bentoSkeletonRows = listOf(
                listOf(BentoGridCell(Unit, isWide = true), BentoGridCell(Unit)),
                listOf(BentoGridCell(Unit), BentoGridCell(Unit), BentoGridCell(Unit)),
                listOf(BentoGridCell(Unit), BentoGridCell(Unit, isWide = true))
            )
            ThreeColumnBentoGrid(rows = bentoSkeletonRows) { cellModifier, _ ->
                Box(modifier = cellModifier.shimmerEffect(tileShape))
            }
        }

        // ── 3. Recent Transactions Section Header Skeleton ──────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(18.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(14.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
        }

        // ── 4. Recent Transactions List Skeletons ───────────────────────────
        repeat(4) {
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
                        // Category Icon Circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shimmerEffect(CircleShape)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Title bar
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(14.dp)
                                    .shimmerEffect(RoundedCornerShape(4.dp))
                            )
                            // Date bar
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(10.dp)
                                    .shimmerEffect(RoundedCornerShape(3.dp))
                            )
                        }
                    }

                    // Amount bar
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
