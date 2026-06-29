package com.example.ui.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.designsystem.tokens.ColorTokens
import com.example.ui.designsystem.tokens.ShapeTokens
import com.example.ui.designsystem.tokens.MotionTokens

enum class CardVariant {
    SOLID, FLAT, OUTLINED, INTERACTIVE
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.SOLID,
    shape: RoundedCornerShape = ShapeTokens.Md,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    borderStroke: BorderStroke? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // HeroUI Bouncy scale click anim
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = MotionTokens.springResponsive(),
        label = "card_scale"
    )

    val baseBgColor = backgroundColor ?: MaterialTheme.colorScheme.surfaceVariant
    val finalBgColor = when (variant) {
        CardVariant.FLAT -> baseBgColor.copy(alpha = 0.4f)
        CardVariant.OUTLINED -> Color.Transparent
        else -> baseBgColor
    }

    // Notion Design Principle: Clean flat surfaces, no heavy shadows
    val shadowElevation = 0.dp

    // Notion Design Principle: Thin 1dp outlines everywhere for that clean spreadsheet/document feel
    val finalBorderStroke = borderStroke ?: BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline
    )

    val finalModifier = modifier
        .scale(scale)
        .clip(shape)
        .background(finalBgColor)
        .border(finalBorderStroke.width, finalBorderStroke.brush, shape)
        .run {
            if (onClick != null) {
                clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else this
        }

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.TopStart,
        content = content
    )
}
