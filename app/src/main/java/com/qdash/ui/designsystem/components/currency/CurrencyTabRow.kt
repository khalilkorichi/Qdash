package com.qdash.ui.designsystem.components.currency

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import androidx.compose.foundation.isSystemInDarkTheme

import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Pill-style tab row for the currency exchange screen.
 * RTL-aware: tabs are ordered right-to-left [Parallel | Converter | Official].
 *
 * @param tabs      List of tab labels (RTL order — index 0 = rightmost).
 * @param selected  Index of the currently selected tab.
 * @param onTabSelected Callback with the tapped tab index.
 */
@Composable
fun CurrencyTabRow(
    tabs: List<String>,
    selected: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val activeTextColor = MaterialTheme.colorScheme.onPrimary
    val inactiveTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeTokens.Full)
            .background(containerColor)
            .padding(SpacingTokens.Xs),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Xxs)
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = index == selected
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(ShapeTokens.Full)
                    .background(
                        if (isSelected) activeColor else Color.Transparent,
                        ShapeTokens.Full
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onTabSelected(index) }
                    )
                    .padding(vertical = SpacingTokens.Sm, horizontal = SpacingTokens.Xs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) activeTextColor else inactiveTextColor,
                    maxLines = 1
                )
            }
        }
    }
}

