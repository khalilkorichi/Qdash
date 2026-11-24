package com.qdash.ui.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qdash.ui.designsystem.tokens.MotionTokens

/**
 * Circular action button built without Material3 IconButton's implicit 48dp touch-target
 * expansion so adjacent buttons and field text respect exact layout spacing and never overlap.
 */
@Composable
fun AppCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    iconSize: Dp = 17.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    borderColor: Color = Color.Transparent,
    enabled: Boolean = true
) {
    AppCircleButton(
        onClick = onClick,
        modifier = modifier,
        size = size,
        containerColor = containerColor,
        borderColor = borderColor,
        enabled = enabled,
        contentDescription = contentDescription
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Generic circular interactive container with exact layout & touch bounds, bounded ripple,
 * press-scale feedback, and accessibility semantics. Useful when the child includes a badge.
 */
@Composable
fun AppCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    borderColor: Color = Color.Transparent,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        animationSpec = MotionTokens.springResponsive(),
        label = "circle_btn_scale"
    )

    val shapeModifier = if (borderColor != Color.Transparent) {
        Modifier
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape)
    } else {
        Modifier
            .clip(CircleShape)
            .background(containerColor)
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(shapeModifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}
