package com.qdash.domain.model

/**
 * Pure domain model — no Android/Room dependencies.
 * Includes calculated trends (UP/DOWN/STABLE) for official and parallel buy/sell rates.
 */
data class ExchangeRate(
    val currencyCode: String,
    val currencyName: String,          // Arabic name: "دولار أمريكي"
    val countryFlagEmoji: String,
    val officialBuyRate: Double,       // DZD you get when you sell 1 foreign unit to the bank
    val officialSellRate: Double,      // DZD you pay to buy 1 foreign unit from the bank
    val officialBuyTrend: RateTrend = RateTrend(RateDirection.STABLE, 0.0),
    val officialSellTrend: RateTrend = RateTrend(RateDirection.STABLE, 0.0),
    val parallelBuyRate: Double? = null,
    val parallelSellRate: Double? = null,
    val parallelBuyTrend: RateTrend? = null,
    val parallelSellTrend: RateTrend? = null,
    val lastUpdatedAt: Long = 0L
)

/** Supported currencies — order matches RTL display order in the UI. */
val SUPPORTED_CURRENCIES = listOf("USD", "EUR", "GBP", "CAD", "SAR", "AED", "TND")

/** Maps ISO code → Arabic currency name. */
val CURRENCY_ARABIC_NAMES = mapOf(
    "USD" to "دولار أمريكي",
    "EUR" to "يورو",
    "GBP" to "جنيه إسترليني",
    "CAD" to "دولار كندي",
    "SAR" to "ريال سعودي",
    "AED" to "درهم إماراتي",
    "TND" to "دينار تونسي",
    "DZD" to "دينار جزائري"
)

/** Default seed rates — official rates initialized only. Parallel rates fetched live from forexalgerie.com */
val DEFAULT_EXCHANGE_RATES = listOf(
    ExchangeRate("USD", "دولار أمريكي", "🇺🇸", 137.50, 137.80, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), null, null, null, null, 0L),
    ExchangeRate("EUR", "يورو", "🇪🇺", 148.20, 148.60, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), null, null, null, null, 0L),
    ExchangeRate("GBP", "جنيه إسترليني", "🇬🇧", 172.30, 172.80, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), null, null, null, null, 0L),
    ExchangeRate("CAD", "دولار كندي", "🇨🇦", 99.50, 99.90, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), null, null, null, null, 0L),
    ExchangeRate("SAR", "ريال سعودي", "🇸🇦", 36.60, 36.80, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), null, null, null, null, 0L),
    ExchangeRate("AED", "درهم إماراتي", "🇦🇪", 37.45, 37.65, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), null, null, null, null, 0L),
    ExchangeRate("TND", "دينار تونسي", "🇹🇳", 43.20, 43.50, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), null, null, null, null, 0L)
)
