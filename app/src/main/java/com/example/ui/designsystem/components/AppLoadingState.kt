package com.example.ui.designsystem.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.designsystem.tokens.ColorTokens
import com.example.ui.designsystem.tokens.ShapeTokens

@Composable
fun AppLoadingState(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val indicatorColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = indicatorColor,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp)
        )
    }
}@Composable
fun AppSkeleton(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = ShapeTokens.Md
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shimmerEffect(shape)
    )
}

fun Modifier.shimmerEffect(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
): Modifier = composed {
    val context = androidx.compose.ui.platform.LocalContext.current
    val powerManager = remember(context) { 
        context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager 
    }
    val isPowerSaveMode = remember(powerManager) { 
        powerManager?.isPowerSaveMode ?: false 
    }
    val isDark = MaterialTheme.colorScheme.background != com.example.ui.designsystem.tokens.ColorTokens.BackgroundLight
    
    // Power-saving mode mitigation: halt animation cycles entirely
    if (isPowerSaveMode) {
        val staticColor = if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
        return@composed this.background(color = staticColor, shape = shape)
    }

    val baseColor = if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
    val highlightColor = if (isDark) Color(0xFF4B5563) else Color(0xFFF3F4F6)

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val layoutDirection = LocalLayoutDirection.current

    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = if (layoutDirection == LayoutDirection.Rtl) {
            Offset(x = 2000f - translateAnimation.value, y = 0f)
        } else {
            Offset(x = translateAnimation.value - 1000f, y = 0f)
        },
        end = if (layoutDirection == LayoutDirection.Rtl) {
            Offset(x = 1000f - translateAnimation.value, y = 1000f)
        } else {
            Offset(x = translateAnimation.value, y = 1000f)
        }
    )

    this.background(brush = shimmerBrush, shape = shape)
}
