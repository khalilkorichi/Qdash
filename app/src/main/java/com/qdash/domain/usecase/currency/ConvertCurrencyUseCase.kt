package com.qdash.domain.usecase.currency

import com.qdash.domain.model.ExchangeRate
import com.qdash.domain.model.MarketType
import com.qdash.domain.model.TradeDirection
import kotlin.math.roundToLong

/**
 * Deterministic, testable currency conversion logic.
 * Contains zero side effects — no repository calls, no coroutines.
 */
class ConvertCurrencyUseCase {

    enum class RateType { OFFICIAL_BUY, OFFICIAL_SELL, PARALLEL_BUY, PARALLEL_SELL, MANUAL }

    operator fun invoke(
        amount: Double,
        fromCode: String,
        toCode: String,
        rates: List<ExchangeRate>,
        marketType: MarketType = MarketType.OFFICIAL,
        tradeDirection: TradeDirection = TradeDirection.BUY,
        rateType: RateType? = null,
        manualRate: Double? = null
    ): Double {
        if (amount <= 0.0) return 0.0
        if (fromCode == toCode) return amount

        val resolvedRateType = when {
            rateType == RateType.MANUAL || (manualRate != null && manualRate > 0.0) -> RateType.MANUAL
            rateType != null -> rateType
            marketType == MarketType.OFFICIAL && tradeDirection == TradeDirection.BUY -> RateType.OFFICIAL_BUY
            marketType == MarketType.OFFICIAL && tradeDirection == TradeDirection.SELL -> RateType.OFFICIAL_SELL
            marketType == MarketType.PARALLEL && tradeDirection == TradeDirection.BUY -> RateType.PARALLEL_BUY
            marketType == MarketType.PARALLEL && tradeDirection == TradeDirection.SELL -> RateType.PARALLEL_SELL
            else -> RateType.OFFICIAL_BUY
        }

        // Shortcut: DZD → Foreign
        if (fromCode == "DZD") {
            val toRate = rates.find { it.currencyCode == toCode }
                ?: return 0.0
            val divisor = selectRate(toRate, resolvedRateType, manualRate)
            return if (divisor > 0.0) (amount / divisor).roundTo(4) else 0.0
        }

        // Shortcut: Foreign → DZD
        if (toCode == "DZD") {
            val fromRate = rates.find { it.currencyCode == fromCode }
                ?: return 0.0
            val multiplier = selectRate(fromRate, resolvedRateType, manualRate)
            return (amount * multiplier).roundTo(2)
        }

        // General: Foreign → DZD → Foreign (pivot)
        val fromRate = rates.find { it.currencyCode == fromCode } ?: return 0.0
        val toRate = rates.find { it.currencyCode == toCode } ?: return 0.0
        val inDzd = amount * selectRate(fromRate, resolvedRateType, manualRate)
        val divisor = selectRate(toRate, resolvedRateType, manualRate)
        return if (divisor > 0.0) (inDzd / divisor).roundTo(4) else 0.0
    }

    private fun selectRate(rate: ExchangeRate, rateType: RateType, manualRate: Double?): Double =
        when (rateType) {
            RateType.OFFICIAL_BUY -> rate.officialBuyRate
            RateType.OFFICIAL_SELL -> rate.officialSellRate
            RateType.PARALLEL_BUY -> rate.parallelBuyRate ?: rate.officialBuyRate
            RateType.PARALLEL_SELL -> rate.parallelSellRate ?: rate.officialSellRate
            RateType.MANUAL -> manualRate ?: rate.officialBuyRate
        }

    private fun Double.roundTo(decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return (this * factor).roundToLong() / factor
    }
}
