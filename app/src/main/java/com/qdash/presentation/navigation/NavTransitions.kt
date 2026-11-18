package com.qdash.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer

private const val ENTER_DURATION = 220
private const val EXIT_DURATION = 180

/**
 * Centralised navigation transitions tuned for snappy RTL Arabic UI.
 * Forward push enters from the RTL leading edge (left, `-width/8`) while previous screen
 * slides toward the RTL trailing edge (right, `+width/8`).
 */
object NavTransitions {

    /** Forward navigation: content enters from the RTL leading edge (left). */
    val pushEnterRtl: EnterTransition = androidx.compose.animation.slideInHorizontally(
        initialOffsetX = { -it / 8 },
        animationSpec = tween(ENTER_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(ENTER_DURATION))

    /** Forward navigation: previous content exits toward the RTL trailing edge (right). */
    val pushExitRtl: ExitTransition = androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { it / 8 },
        animationSpec = tween(EXIT_DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(EXIT_DURATION))

    /** Back navigation (pop): content re-enters from the RTL trailing edge (right). */
    val popEnterRtl: EnterTransition = androidx.compose.animation.slideInHorizontally(
        initialOffsetX = { it / 8 },
        animationSpec = tween(ENTER_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(ENTER_DURATION))

    /** Back navigation (pop): content exits toward the RTL leading edge (left). */
    val popExitRtl: ExitTransition = androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { -it / 8 },
        animationSpec = tween(EXIT_DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(EXIT_DURATION))

    // Backwards-compatible aliases
    val enterFromRight: EnterTransition get() = pushEnterRtl
    val exitToRight: ExitTransition get() = pushExitRtl
    val popEnterFromLeft: EnterTransition get() = popEnterRtl
    val popExitToLeft: ExitTransition get() = popExitRtl

    val fadeEnter: EnterTransition = fadeIn(animationSpec = tween(160))
    val fadeExit: ExitTransition = fadeOut(animationSpec = tween(140))
}
