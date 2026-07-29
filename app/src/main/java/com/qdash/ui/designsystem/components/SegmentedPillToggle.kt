package com.qdash.ui.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens

/**
 * Reusable 2-item Segmented Pill Toggle component matching Qdash Design Tokens.
 *
 * @param option1Label  Label for the first option (RTL: right side).
 * @param option2Label  Label for the second option (RTL: left side).
 * @param selectedIndex 0 for option1, 1 for option2.
 * @param onOptionSelected Callback when an option is tapped.
 * @param enabled       When false, the toggle is faded out and non-interactive.
 */
@Composable
fun SegmentedPillToggle(
    option1Label: String,
    option2Label: String,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) ColorTokens.CardDark else ColorTokens.CardLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeTokens.Full)
            .background(containerBg)
            .padding(SpacingTokens.Xs)
            .alpha(if (enabled) 1.0f else 0.45f),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Xs)
    ) {
        // Option 1 (RTL: Right side)
        PillItem(
            label = option1Label,
            isSelected = selectedIndex == 0,
            onClick = { if (enabled) onOptionSelected(0) },
            modifier = Modifier.weight(1f)
        )

        // Option 2 (RTL: Left side)
        PillItem(
            label = option2Label,
            isSelected = selectedIndex == 1,
            onClick = { if (enabled) onOptionSelected(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PillItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val targetTextColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(DurationMs),
        label = "pillBg"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(DurationMs),
        label = "pillText"
    )

    Box(
        modifier = modifier
            .clip(ShapeTokens.Full)
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(vertical = SpacingTokens.Sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = animatedTextColor
        )
    }
}

private const val DurationMs = 200
