package com.qdash.domain.usecase.currency

import com.qdash.domain.model.RateDirection
import com.qdash.domain.model.RateTrend
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Pure, deterministic calculation of rate trend direction and percentage change.
 * Zero side-effects.
 */
class CalculateRateTrendUseCase {

    operator fun invoke(currentRate: Double, previousRate: Double?): RateTrend {
        if (previousRate == null || previousRate <= 0.0 || abs(currentRate - previousRate) < 0.0001) {
            return RateTrend(RateDirection.STABLE, 0.0)
        }

        val percentage = ((currentRate - previousRate) / previousRate * 100).roundTo(1)
        return when {
            percentage > 0.0 -> RateTrend(RateDirection.UP, percentage)
            percentage < 0.0 -> RateTrend(RateDirection.DOWN, percentage)
            else -> RateTrend(RateDirection.STABLE, 0.0)
        }
    }

    private fun Double.roundTo(decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return (this * factor).roundToLong() / factor
    }
}
