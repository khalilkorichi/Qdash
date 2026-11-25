package com.qdash.presentation.simulator.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.qdash.ui.designsystem.tokens.ColorTokens

/**
 * Two very translucent floating circular controls anchored at the physical bottom-right
 * corner of a postal document preview (Rotate/Fullscreen Zoom + Educational Guide).
 */
@Composable
fun TranslucentPreviewFloatingActions(
    isGuideActive: Boolean,
    onRotateExpandClick: () -> Unit,
    onToggleGuideClick: () -> Unit,
    expandButtonTestTag: String,
    guideButtonTestTag: String,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Educational Guide button next to the rotate/fullscreen button
            Surface(
                onClick = onToggleGuideClick,
                shape = CircleShape,
                color = if (isGuideActive) {
                    Color(0xFF0F172A).copy(alpha = 0.42f)
                } else {
                    Color(0xFF0F172A).copy(alpha = 0.22f)
                },
                border = BorderStroke(
                    width = 0.8.dp,
                    color = if (isGuideActive) {
                        ColorTokens.PosteYellow.copy(alpha = 0.70f)
                    } else {
                        Color.White.copy(alpha = 0.35f)
                    }
                ),
                modifier = Modifier
                    .size(34.dp)
                    .testTag(guideButtonTestTag)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "الدليل التعليمي",
                        tint = if (isGuideActive) {
                            ColorTokens.PosteYellow.copy(alpha = 0.92f)
                        } else {
                            Color.White.copy(alpha = 0.86f)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Landscape Rotate & Fullscreen Zoom button
            Surface(
                onClick = onRotateExpandClick,
                shape = CircleShape,
                color = Color(0xFF0F172A).copy(alpha = 0.22f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(34.dp)
                    .testTag(expandButtonTestTag)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "تدوير وتكبير بملء الشاشة",
                        tint = Color.White.copy(alpha = 0.86f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dynamic 2-finger pinch-to-zoom and pan gesture modifier that leaves single-finger
 * taps and parent vertical scrolling completely unobstructed at 1x scale.
 */
fun Modifier.twoFingerPinchZoom(
    getScale: () -> Float,
    getPan: () -> Offset,
    onTransform: (newScale: Float, newPan: Offset) -> Unit,
    minScale: Float = 1f,
    maxScale: Float = 3f
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val pressedCount = event.changes.count { it.pressed }
            val currentScale = getScale()
            if (pressedCount >= 2 || currentScale > 1.03f) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    val nextScale = (currentScale * zoomChange).coerceIn(minScale, maxScale)
                    val nextPan = if (nextScale > 1.02f) {
                        val currentPan = getPan()
                        val maxOffsetX = size.width * (nextScale - 1f) * 0.55f
                        val maxOffsetY = size.height * (nextScale - 1f) * 0.55f
                        Offset(
                            x = (currentPan.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (currentPan.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    } else {
                        Offset.Zero
                    }
                    onTransform(nextScale, nextPan)
                    event.changes.forEach { change ->
                        if (change.positionChanged()) change.consume()
                    }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
