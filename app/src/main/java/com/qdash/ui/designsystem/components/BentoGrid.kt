package com.qdash.ui.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Describes a single cell inside [ThreeColumnBentoGrid].
 * When [isWide] is true, the item spans two columns while keeping the exact row height
 * of a 1x1 square tile.
 */
data class BentoGridCell<T>(
    val value: T,
    val isWide: Boolean = false
)

/**
 * Renders rows of 1x1 square and 2x1 wide cells in a flush 3-column Bento grid.
 */
@Composable
fun <T> ThreeColumnBentoGrid(
    rows: List<List<BentoGridCell<T>>>,
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    minRowHeight: Dp = 84.dp,
    cellContent: @Composable (cellModifier: Modifier, cell: BentoGridCell<T>) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val rawSingleWidth = ((maxWidth - (spacing * 2)) / 3f).coerceAtLeast(0.dp)
        val rowHeight = rawSingleWidth.coerceAtLeast(minRowHeight)
        val wideWidth = (rawSingleWidth * 2f) + spacing

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    rowItems.forEach { cell ->
                        val cellModifier = if (cell.isWide) {
                            Modifier
                                .width(wideWidth)
                                .height(rowHeight)
                        } else {
                            Modifier
                                .weight(1f)
                                .height(rowHeight)
                        }
                        cellContent(cellModifier, cell)
                    }
                }
            }
        }
    }
}
