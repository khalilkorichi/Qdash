package com.qdash.presentation.ai.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

    Box(
        modifier = modifier
            .size(76.dp),
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
                )
                .clickable { onClick() },
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
