package com.qdash.data.remote.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Real-time scraper for Algerian parallel market (Square Port Said) exchange rates.
 * Primary source: forexalgerie.com
 * Resilient secondary source: devisedz.com
 *
 * Safe execution: 10s timeout, non-blocking, exception-wrapped in Result<List<ScrapedRate>>.
 */
class ParallelRateScraper {

    suspend fun scrapeRates(): Result<List<ScrapedRate>> = withContext(Dispatchers.IO) {
        // Try Primary Source: devisedz.com / forexalgerie.com
        val primaryResult = scrapeFromDeviseDz()
        if (primaryResult.isSuccess && primaryResult.getOrNull()?.isNotEmpty() == true) {
            return@withContext primaryResult
        }

        // Try Secondary Source: forexalgerie.com
        val secondaryResult = scrapeFromForexAlgerie()
        if (secondaryResult.isSuccess && secondaryResult.getOrNull()?.isNotEmpty() == true) {
            return@withContext secondaryResult
        }

        Result.failure(Exception("تعذر استخراج بيانات السوق الموازي من المصادر المتاحة"))
    }

    private fun scrapeFromDeviseDz(): Result<List<ScrapedRate>> {
        return try {
            val doc = Jsoup.connect(DEVISEDZ_URL)
                .timeout(TIMEOUT_MS)
                .userAgent(USER_AGENT)
                .get()

            val rates = mutableListOf<ScrapedRate>()
            val divs = doc.select("div")

            for (div in divs) {
                val text = div.text()
                val code = detectCurrencyCode(text) ?: continue

                val numbers = extractNumbers(text)
                if (numbers.size >= 2) {
                    val buy = numbers[0]
                    val sell = numbers[1]
                    if (buy in 10.0..1000.0 && sell in 10.0..1000.0) {
                        rates.add(ScrapedRate(currencyCode = code, buyRate = buy, sellRate = sell))
                    }
                }
            }

            val distinct = rates.distinctBy { it.currencyCode }
            if (distinct.isNotEmpty()) {
                Result.success(distinct)
            } else {
                Result.failure(Exception("devisedz returned empty rates"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scrapeFromForexAlgerie(): Result<List<ScrapedRate>> {
        return try {
            val doc = Jsoup.connect(FOREX_ALGERIE_URL)
                .timeout(TIMEOUT_MS)
                .userAgent(USER_AGENT)
                .get()

            val rates = mutableListOf<ScrapedRate>()
            val rows = doc.select("table tr, div.currency-row, div.rate-card")

            for (row in rows) {
                val text = row.text()
                val code = detectCurrencyCode(text) ?: continue

                val numbers = extractNumbers(text)
                if (numbers.size >= 2) {
                    val buy = numbers[0]
                    val sell = numbers[1]
                    if (buy in 10.0..1000.0 && sell in 10.0..1000.0) {
                        rates.add(ScrapedRate(currencyCode = code, buyRate = buy, sellRate = sell))
                    }
                }
            }

            val distinct = rates.distinctBy { it.currencyCode }
            if (distinct.isNotEmpty()) {
                Result.success(distinct)
            } else {
                Result.failure(Exception("forexalgerie returned empty rates"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun detectCurrencyCode(text: String): String? {
        val uppercase = text.uppercase()
        return when {
            uppercase.contains("EUR") || uppercase.contains("EURO") || uppercase.contains("يورو") -> "EUR"
            uppercase.contains("USD") || uppercase.contains("DOLLAR") || uppercase.contains("دولار") -> "USD"
            uppercase.contains("GBP") || uppercase.contains("STERLING") || uppercase.contains("LIVRE STERLING") || uppercase.contains("جنيه") -> "GBP"
            uppercase.contains("CAD") || uppercase.contains("CANADIEN") || uppercase.contains("كندي") -> "CAD"
            uppercase.contains("SAR") || uppercase.contains("SAOUDIEN") || uppercase.contains("RIYAL") || uppercase.contains("ريال") -> "SAR"
            uppercase.contains("AED") || uppercase.contains("EMIRATI") || uppercase.contains("DIRHAM EMIRATI") || uppercase.contains("درهم إماراتي") -> "AED"
            uppercase.contains("TND") || uppercase.contains("TUNISIEN") || uppercase.contains("DINAR TUNISIEN") || uppercase.contains("تونسي") -> "TND"
            else -> null
        }
    }

    private fun extractNumbers(text: String): List<Double> {
        val regex = Regex("""\d{2,3}(?:\.\d{1,2})?""")
        return regex.findAll(text)
            .map { it.value.toDoubleOrNull() }
            .filterNotNull()
            .filter { it in 10.0..1000.0 }
            .toList()
    }

    companion object {
        private const val DEVISEDZ_URL = "https://devisedz.com/"
        private const val FOREX_ALGERIE_URL = "https://forexalgerie.com/"
        private const val TIMEOUT_MS = 10000
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
