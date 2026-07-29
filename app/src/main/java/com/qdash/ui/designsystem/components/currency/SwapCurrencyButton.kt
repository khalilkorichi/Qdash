package com.qdash.ui.designsystem.components.currency

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens

/**
 * Animated swap button for the currency converter.
 * On press: scales down (bounce) and rotates 180°.
 * Uses Material Icons SwapVert (vertical arrows matching top/bottom input fields).
 */
@Composable
fun SwapCurrencyButton(
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationState by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationState,
        animationSpec = MotionTokens.tweenMedium(),
        label = "swap_rotation"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = MotionTokens.springResponsive(),
        label = "swap_scale"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .scale(scale)
            .clip(ShapeTokens.Full)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    rotationState += 180f
                    onSwap()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SwapVert,
            contentDescription = "تبديل العملتين",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation)
        )
    }
}
