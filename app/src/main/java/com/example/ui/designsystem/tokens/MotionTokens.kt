package com.example.ui.designsystem.tokens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object MotionTokens {
    // Animation Durations (ms)
    val DurationShort = 120
    val DurationMedium = 250
    val DurationLong = 400

    // Tween Specs
    fun <T> tweenShort() = tween<T>(durationMillis = DurationShort)
    fun <T> tweenMedium() = tween<T>(durationMillis = DurationMedium)
    fun <T> tweenLong() = tween<T>(durationMillis = DurationLong)

    // Spring Specs for physics-based fluid motions (elastic spring feeling like HeroUI)
    fun <T> springFluid(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> springResponsive(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> springBouncy(): SpringSpec<T> = spring(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMedium
    )
}
