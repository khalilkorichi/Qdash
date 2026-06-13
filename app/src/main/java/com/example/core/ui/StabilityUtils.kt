package com.example.core.ui

import androidx.compose.runtime.Immutable

/**
 * A stable wrapper for [List] in Jetpack Compose to prevent unnecessary recompositions.
 */
@Immutable
data class StableList<T>(val items: List<T>)

/**
 * Extension helper to wrap any [List] into a [StableList].
 */
fun <T> List<T>.asStable(): StableList<T> = StableList(this)

/**
 * A unified resource wrapper for UI states containing loading, success, and error outcomes.
 */
sealed interface UIResource<out T> {
    object Loading : UIResource<Nothing>
    data class Success<out T>(val data: T) : UIResource<T>
    data class Error(val errorMessage: String, val exception: Throwable? = null) : UIResource<Nothing>
}
