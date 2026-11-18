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

            val distinct = parseDocumentRates(doc, "table tr, article, li, div")
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

            val distinct = parseDocumentRates(doc, "table tr, div.currency-row, div.rate-card, li, div")
            if (distinct.isNotEmpty()) {
                Result.success(distinct)
            } else {
                Result.failure(Exception("forexalgerie returned empty rates"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun parseDocumentRates(doc: Document, selector: String): List<ScrapedRate> {
        val rates = mutableListOf<ScrapedRate>()
        // Sort compact/leaf elements first and cap text length to 160 chars so outer wrapper
        // <div>s containing the entire page never shadow individual currency blocks.
        val candidates = doc.select(selector)
            .map { it to it.text().trim() }
            .filter { (_, text) -> text.isNotEmpty() && text.length <= MAX_BLOCK_TEXT_LENGTH }
            .sortedWith(compareBy({ (el, _) -> el.children().size }, { (_, text) -> text.length }))

        for ((_, text) in candidates) {
            val matchedCodes = detectAllCurrencyCodes(text)
            if (matchedCodes.size != 1) continue
            val code = matchedCodes.first()
            if (rates.any { it.currencyCode == code }) continue

            val numbers = extractNumbers(text)
            if (numbers.size >= 2) {
                val buy = minOf(numbers[0], numbers[1])
                val sell = maxOf(numbers[0], numbers[1])
                if (buy in MIN_PLAUSIBLE_DZD..MAX_PLAUSIBLE_DZD &&
                    sell in MIN_PLAUSIBLE_DZD..MAX_PLAUSIBLE_DZD &&
                    sell >= buy
                ) {
                    rates.add(ScrapedRate(currencyCode = code, buyRate = buy, sellRate = sell))
                }
            }
        }
        return rates.distinctBy { it.currencyCode }
    }

    private fun detectAllCurrencyCodes(text: String): Set<String> {
        val uppercase = text.uppercase()
        val found = mutableSetOf<String>()
        if (uppercase.contains("EUR") || uppercase.contains("EURO") || uppercase.contains("يورو")) found.add("EUR")
        if (uppercase.contains("USD") || uppercase.contains("DOLLAR") || uppercase.contains("دولار")) found.add("USD")
        if (uppercase.contains("GBP") || uppercase.contains("STERLING") || uppercase.contains("LIVRE STERLING") || uppercase.contains("جنيه")) found.add("GBP")
        if (uppercase.contains("CAD") || uppercase.contains("CANADIEN") || uppercase.contains("كندي")) found.add("CAD")
        if (uppercase.contains("SAR") || uppercase.contains("SAOUDIEN") || uppercase.contains("RIYAL") || uppercase.contains("ريال")) found.add("SAR")
        if (uppercase.contains("AED") || uppercase.contains("EMIRATI") || uppercase.contains("DIRHAM EMIRATI") || uppercase.contains("درهم إماراتي")) found.add("AED")
        if (uppercase.contains("TND") || uppercase.contains("TUNISIEN") || uppercase.contains("DINAR TUNISIEN") || uppercase.contains("تونسي")) found.add("TND")
        return found
    }

    private fun extractNumbers(text: String): List<Double> {
        val normalized = text
            .replace('\u00A0', ' ')
            .replace('٫', '.')
            .replace(Regex("""(\d),(\d)"""), "$1.$2")
        val regex = Regex("""(?<![.\d])\d{2,3}(?:\.\d{1,2})?(?![.\d])""")
        return regex.findAll(normalized)
            .mapNotNull { it.value.toDoubleOrNull() }
            .filter { it in MIN_PLAUSIBLE_DZD..MAX_PLAUSIBLE_DZD }
            .toList()
    }

    companion object {
        private const val DEVISEDZ_URL = "https://devisedz.com/"
        private const val FOREX_ALGERIE_URL = "https://forexalgerie.com/"
        private const val TIMEOUT_MS = 10000
        private const val MAX_BLOCK_TEXT_LENGTH = 160
        private const val MIN_PLAUSIBLE_DZD = 20.0
        private const val MAX_PLAUSIBLE_DZD = 600.0
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
