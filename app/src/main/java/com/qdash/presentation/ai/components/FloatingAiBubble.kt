package com.qdash.presentation.ai.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FloatingAiBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // One-time startup scale and alpha animatables
    val scale = remember { Animatable(1.0f) }
    val alpha = remember { Animatable(0.7f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1.7f,
                animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
            )
        }
    }

    // Docking & interaction state
    var isDocked by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }
    var interactionTrigger by remember { mutableIntStateOf(0) }

    // 2-second inactivity timer to dock
    LaunchedEffect(isDocked, interactionTrigger) {
        if (!isDocked) {
            delay(2000)
            isDocked = true
        }
    }

    // Spaced periodic tooltip: 3 seconds visible, 57 seconds hidden (total 1 minute cycle)
    LaunchedEffect(isDocked) {
        if (isDocked) {
            while (true) {
                delay(57000)
                showTooltip = true
                delay(3000)
                showTooltip = false
            }
        } else {
            showTooltip = false
        }
    }

    // Determine direction-aware offset (RTL End is left, LTR End is right)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val xOffset by animateDpAsState(
        targetValue = if (isRtl) {
            if (isDocked) 72.dp else 0.dp
        } else {
            if (isDocked) (-72).dp else 0.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ai_bubble_x_offset"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.offset(x = xOffset)
    ) {
        // Speech Bubble Tooltip (shows only when docked)
        AnimatedVisibility(
            visible = showTooltip && isDocked,
            enter = fadeIn(animationSpec = tween(400)) + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut(animationSpec = tween(400)) + shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shadowElevation = 6.dp,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "أنا مساعد قداش في خدمتك",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }

        // Main AI FAB Sphere
        Box(
            modifier = Modifier
                .size(76.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isDocked) {
                        isDocked = false
                        interactionTrigger++
                    } else {
                        onClick()
                        interactionTrigger++ // reset timer on click
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // One-time pulsing glowing ring behind the FAB
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale.value)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha.value),
                        shape = CircleShape
                    )
            )

            // Main AI FAB (Glossy Premium sphere styling)
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(elevation = 10.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
                        CircleShape
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6), // Premium Violet
                                Color(0xFF3B82F6), // Deep Royal Blue
                                Color(0xFF10B981)  // Emerald Teal
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "الذكاء الاصطناعي",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
