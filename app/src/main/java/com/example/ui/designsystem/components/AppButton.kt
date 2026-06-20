package com.example.ui.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.tokens.ColorTokens
import com.example.ui.designsystem.tokens.ShapeTokens
import com.example.ui.designsystem.tokens.MotionTokens

enum class ButtonVariant {
    SOLID, BORDERED, FLAT, LIGHT
}

enum class ButtonIntent {
    PRIMARY, SUCCESS, DANGER, WARNING, INFO
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.SOLID,
    intent: ButtonIntent = ButtonIntent.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    shape: RoundedCornerShape = ShapeTokens.Md,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale on press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = MotionTokens.springBouncy(),
        label = "button_scale"
    )

    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight

    // Colors mapping based on variant and intent
    val intentColor = when (intent) {
        ButtonIntent.PRIMARY -> MaterialTheme.colorScheme.primary
        ButtonIntent.SUCCESS -> if (isDark) ColorTokens.SuccessDark else ColorTokens.Success
        ButtonIntent.DANGER -> MaterialTheme.colorScheme.error
        ButtonIntent.WARNING -> if (isDark) ColorTokens.WarningDark else ColorTokens.Warning
        ButtonIntent.INFO -> if (isDark) ColorTokens.InfoDark else ColorTokens.Info
    }

    val containerColor = when (variant) {
        ButtonVariant.SOLID -> intentColor
        ButtonVariant.FLAT -> intentColor.copy(alpha = 0.12f)
        ButtonVariant.BORDERED, ButtonVariant.LIGHT -> Color.Transparent
    }

    val contentColor = when (variant) {
        ButtonVariant.SOLID -> if (intent == ButtonIntent.PRIMARY) MaterialTheme.colorScheme.onPrimary else Color.White
        ButtonVariant.FLAT -> intentColor
        ButtonVariant.BORDERED -> intentColor
        ButtonVariant.LIGHT -> intentColor
    }

    val disabledContainerColor = when (variant) {
        ButtonVariant.SOLID -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        ButtonVariant.FLAT -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        ButtonVariant.BORDERED, ButtonVariant.LIGHT -> Color.Transparent
    }

    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    val finalContainerColor = if (enabled) containerColor else disabledContainerColor
    val finalContentColor = if (enabled) contentColor else disabledContentColor

    val border = if (variant == ButtonVariant.BORDERED && enabled) {
        BorderStroke(1.5.dp, intentColor)
    } else if (variant == ButtonVariant.BORDERED && !enabled) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    } else {
        null
    }

    Surface(
        onClick = { if (enabled && !isLoading) onClick() },
        enabled = enabled && !isLoading,
        modifier = modifier
            .scale(scale)
            .height(48.dp),
        shape = shape,
        color = finalContainerColor,
        contentColor = finalContentColor,
        border = border,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else if (leadingIcon != null) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    leadingIcon()
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                content()
            }

            if (trailingIcon != null && !isLoading) {
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    trailingIcon()
                }
            }
        }
    }
}
