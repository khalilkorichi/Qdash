package com.qdash.domain.model

enum class RateDirection {
    UP,
    DOWN,
    STABLE
}

data class RateTrend(
    val direction: RateDirection,
    val changePercentage: Double // e.g. +0.8 or -0.5 (0.0 for STABLE)
)
