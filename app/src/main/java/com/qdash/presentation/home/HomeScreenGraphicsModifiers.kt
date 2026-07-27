package com.qdash.presentation.home

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a blur effect efficiently based on API level.
 *
 * - **API 31+ (Android 12+):** Uses [Modifier.blur] which Compose routes through
 *   `RenderEffect` on the RenderThread — no main-thread cost per frame.
 * - **API 24–30:** Also uses [Modifier.blur] (backed by software rendering on older
 *   devices, but only triggers when blurRadius > 0, i.e. showBalanceDetails = true,
 *   which is a rare user-initiated action, not a per-frame animation).
 *
 * If performance is unacceptable on older devices in practice, change the
 * API < 31 branch to `this` (no blur) as documented in the implementation plan.
 *
 * Usage:
 * ```kotlin
 * Modifier.conditionalBlur(blurRadius)
 * ```
 */
fun Modifier.conditionalBlur(radius: Dp): Modifier {
    return if (radius > 0.dp) {
        this.blur(radius)
    } else {
        this
    }
}
