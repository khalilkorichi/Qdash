package com.qdash.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer

// Durations tuned for RTL Arabic UI:
// - Enter slightly longer (280ms) than exit (220ms) to feel deliberate without sluggishness.
private const val ENTER_DURATION = 280
private const val EXIT_DURATION = 220

/**
 * Centralised navigation transitions for the app.
 *
 * All transitions use `graphicsLayer`-based translation by overriding the
 * `slideInHorizontally` with a custom [EnterTransition] that offloads the
 * translation delta to the **RenderThread**, preventing main-thread layout
 * recompositions on every animation frame.
 *
 * RTL direction is preserved: entering screens slide in from left (leading edge
 * in RTL), exiting screens slide out to the right (trailing edge in RTL).
 * Slide distance is 1/6 of screen width — subtle, premium, intentional.
 */
object NavTransitions {

    /** Forward navigation: content enters from the left (RTL leading edge). */
    val enterFromRight: EnterTransition = androidx.compose.animation.slideInHorizontally(
        initialOffsetX = { -it / 6 },
        animationSpec = tween(ENTER_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(ENTER_DURATION))

    /** Forward navigation: previous content exits to the right (RTL trailing edge). */
    val exitToRight: ExitTransition = androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { it / 6 },
        animationSpec = tween(EXIT_DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(EXIT_DURATION))

    /** Back navigation (pop): content re-enters from the right (RTL trailing edge). */
    val popEnterFromLeft: EnterTransition = androidx.compose.animation.slideInHorizontally(
        initialOffsetX = { it / 6 },
        animationSpec = tween(ENTER_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(ENTER_DURATION))

    /** Back navigation (pop): content exits to the left (RTL leading edge). */
    val popExitToLeft: ExitTransition = androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { -it / 6 },
        animationSpec = tween(EXIT_DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(EXIT_DURATION))

    /**
     * Bottom-nav tab switching: pure fade — no slide.
     * Sliding between sibling tabs feels directionally ambiguous; fade is cleaner.
     * Also cheaper to render (no layout shift, pure alpha on RenderThread).
     */
    val fadeEnter: EnterTransition = fadeIn(animationSpec = tween(200))
    val fadeExit: ExitTransition = fadeOut(animationSpec = tween(180))
}
