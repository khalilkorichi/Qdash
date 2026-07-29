package com.qdash.data.remote.scraper

/**
 * Parsed exchange rate extracted from parallel market sources.
 *
 * @param currencyCode ISO 4217 code ("USD", "EUR", …)
 * @param buyRate      Parallel buy rate in DZD (Achat)
 * @param sellRate     Parallel sell rate in DZD (Vente)
 */
data class ScrapedRate(
    val currencyCode: String,
    val buyRate: Double,
    val sellRate: Double
)
