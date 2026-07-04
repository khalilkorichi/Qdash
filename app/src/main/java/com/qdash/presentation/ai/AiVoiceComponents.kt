package com.qdash.presentation.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.AiVoiceState
import androidx.compose.runtime.withFrameMillis

@Composable
fun AnimatedVoiceOrb(
    state: AiVoiceState,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val accentPink = Color(0xFFEC4899)

    val isListening = state is AiVoiceState.Listening
    val isProcessing = state is AiVoiceState.Processing
    val isError = state is AiVoiceState.Error

    // Animate amplitude based on state
    val targetAmplitude = when {
        isListening -> 1f
        isProcessing -> 0.5f
        isError -> 0.3f
        else -> 0.4f
    }
    val amplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "wave_amplitude"
    )

    // Animate speed based on state
    val targetSpeed = when {
        isListening -> 2.5f
        isProcessing -> 0.8f
        else -> 1.2f
    }
    val speed by animateFloatAsState(
        targetValue = targetSpeed,
        animationSpec = tween(400),
        label = "wave_speed"
    )

    // Continuous phase accumulation — never restarts, never glitches
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(speed) {
        var lastTime = 0L
        while (true) {
            withFrameMillis { timeMs ->
                if (lastTime != 0L) {
                    val dt = (timeMs - lastTime) / 1000f
                    // Limit dt to avoid spikes (e.g. if the app goes to background and resumes)
                    if (dt > 0f && dt < 1f) {
                        phase.floatValue += dt * speed
                    }
                }
                lastTime = timeMs
            }
        }
    }

    val wave1Color = if (isError) error else primary
    val wave2Color = if (isError) error.copy(alpha = 0.6f) else accentPink

    Canvas(
        modifier = modifier
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val centerY = h / 2f
        val maxAmp = h * 0.3f * amplitude
        val strokeW = 3.dp.toPx()
        val pi = Math.PI.toFloat()
        val step = 2
        val currentPhase = phase.floatValue

        // Wave 1: Primary color sine wave (3 full cycles across width)
        val path1 = androidx.compose.ui.graphics.Path()
        path1.moveTo(0f, centerY)
        var x = 0
        while (x <= w.toInt()) {
            val xf = x.toFloat()
            val normalized = xf / w
            val y = centerY + kotlin.math.sin(normalized * 3f * 2f * pi + currentPhase) * maxAmp
            path1.lineTo(xf, y)
            x += step
        }
        drawPath(
            path = path1,
            color = wave1Color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Wave 2: Pink wave, phase shifted (3 full cycles)
        val path2 = androidx.compose.ui.graphics.Path()
        path2.moveTo(0f, centerY)
        x = 0
        while (x <= w.toInt()) {
            val xf = x.toFloat()
            val normalized = xf / w
            val y = centerY + kotlin.math.sin(normalized * 3f * 2f * pi + currentPhase + pi * 0.65f) * maxAmp * 0.8f
            path2.lineTo(xf, y)
            x += step
        }
        drawPath(
            path = path2,
            color = wave2Color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MicrophoneButton(
    state: AiVoiceState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = state is AiVoiceState.Listening
    val isProcessing = state is AiVoiceState.Processing
    val containerColor = when {
        isListening -> MaterialTheme.colorScheme.error
        isProcessing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        onClick = { if (isListening) onStopRecording() else onStartRecording() },
        enabled = !isProcessing,
        shape = CircleShape,
        color = containerColor,
        modifier = modifier
            .size(76.dp)
            .shadow(14.dp, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isListening) "إيقاف التسجيل" else "بدء التسجيل",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

@Composable
fun TranscribedTextDisplay(
    text: String,
    state: AiVoiceState,
    modifier: Modifier = Modifier
) {
    val displayText = when {
        text.isNotBlank() -> text
        state is AiVoiceState.Error -> state.message
        else -> "انقر على الميكروفون وتحدث بالعربية"
    }
    val displayColor = if (state is AiVoiceState.Error) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    AnimatedContent(
        targetState = displayText,
        transitionSpec = { fadeIn() + slideInVertically { it / 3 } togetherWith fadeOut() },
        label = "voice_text",
        modifier = modifier
    ) { targetText ->
        Text(
            text = targetText,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                textDirection = TextDirection.Rtl,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            color = displayColor,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                    MaterialTheme.shapes.extraLarge
                )
                .padding(horizontal = 22.dp, vertical = 20.dp)
        )
    }
}
