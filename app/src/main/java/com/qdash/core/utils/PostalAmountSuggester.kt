package com.qdash.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Provides reactive 3x3 grid amount suggestions for Algerian postal documents.
 * Defaults to 9 popular Dinar denominations and dynamically suggests 9 prefix-matching
 * amounts as the user types.
 */
object PostalAmountSuggester {

    val DEFAULT_GRID_AMOUNTS: List<Long> = listOf(
        1_000L,
        2_000L,
        5_000L,
        10_000L,
        20_000L,
        30_000L,
        50_000L,
        100_000L,
        200_000L
    )

    private val CURATED_POSTAL_AMOUNTS: List<Long> = listOf(
        100L, 150L, 200L, 250L, 300L, 350L, 400L, 450L, 500L,
        600L, 650L, 700L, 750L, 800L, 850L, 900L, 950L,
        1_000L, 1_200L, 1_500L, 1_800L, 2_000L, 2_200L, 2_500L, 2_800L,
        3_000L, 3_500L, 4_000L, 4_500L, 5_000L, 5_200L, 5_500L,
        6_000L, 6_500L, 7_000L, 7_500L, 8_000L, 8_500L, 9_000L, 9_500L,
        10_000L, 12_000L, 14_000L, 15_000L, 16_000L, 18_000L,
        20_000L, 22_000L, 24_000L, 25_000L, 28_000L,
        30_000L, 32_000L, 35_000L, 38_000L, 40_000L, 45_000L,
        50_000L, 55_000L, 60_000L, 65_000L, 70_000L, 75_000L,
        80_000L, 85_000L, 90_000L, 95_000L,
        100_000L, 120_000L, 150_000L, 180_000L, 200_000L, 250_000L,
        300_000L, 350_000L, 400_000L, 450_000L, 500_000L,
        600_000L, 700_000L, 750_000L, 800_000L, 900_000L,
        1_000_000L, 1_500_000L, 2_000_000L, 2_500_000L, 3_000_000L,
        4_000_000L, 5_000_000L, 6_000_000L, 7_000_000L, 8_000_000L, 9_000_000L
    )

    private const val GRID_COUNT = 9
    private const val MAX_SUGGESTED_DINARS = 99_000_000L

    /**
     * Returns exactly 9 suggested amounts for the 3x3 grid.
     * Filters by digit prefix when [rawQuery] is non-empty, or returns [DEFAULT_GRID_AMOUNTS] when blank.
     */
    fun suggestGridAmounts(rawQuery: String): List<Long> {
        val western = FormatterUtils.convertNumerals(rawQuery, useWestern = true)
        val integerPart = western.substringBefore('.').substringBefore(',')
        val digits = integerPart.filter { it.isDigit() }.trimStart('0')

        if (digits.isEmpty()) {
            return DEFAULT_GRID_AMOUNTS
        }

        val suggestions = linkedSetOf<Long>()

        // 1. Curated popular postal amounts matching the typed prefix
        CURATED_POSTAL_AMOUNTS.forEach { candidate ->
            if (candidate.toString().startsWith(digits)) {
                suggestions.add(candidate)
            }
        }

        // 2. Dynamic scaled powers of 10 and round increments starting with the typed prefix
        val base = digits.toLongOrNull()
        if (base != null && base > 0L) {
            val scales = longArrayOf(100L, 1_000L, 10_000L, 100_000L, 10L, 1_000_000L, 1L)
            scales.forEach { scale ->
                if (base <= MAX_SUGGESTED_DINARS / scale) {
                    val value = base * scale
                    if (value >= 100L && value.toString().startsWith(digits)) {
                        suggestions.add(value)
                    }
                }
            }

            // Half-step completions like 2 -> 2500, 25000 or 5 -> 5500, 55000
            val halfExtensions = intArrayOf(5, 2, 1, 3, 4, 6, 8)
            val extScales = longArrayOf(100L, 1_000L, 10_000L, 10L)
            for (ext in halfExtensions) {
                if (suggestions.size >= GRID_COUNT) break
                val extended = base * 10L + ext
                for (scale in extScales) {
                    if (extended <= MAX_SUGGESTED_DINARS / scale) {
                        val value = extended * scale
                        if (value >= 100L && value.toString().startsWith(digits)) {
                            suggestions.add(value)
                        }
                    }
                }
            }
        }

        // Fallback if very long prefix (>7 digits) left fewer than 9 items
        if (suggestions.size < GRID_COUNT && base != null && base > 0L) {
            if (base <= MAX_SUGGESTED_DINARS) {
                suggestions.add(base)
            }
            var suffix = 0
            while (suggestions.size < GRID_COUNT && suffix < 100) {
                val candidate = if (base <= MAX_SUGGESTED_DINARS / 10L) base * 10L + suffix else base + suffix
                if (candidate in 1L..MAX_SUGGESTED_DINARS && candidate.toString().startsWith(digits)) {
                    suggestions.add(candidate)
                }
                suffix++
            }
        }

        val ordered = suggestions.sorted().take(GRID_COUNT).toMutableList()
        if (ordered.size < GRID_COUNT) {
            DEFAULT_GRID_AMOUNTS.forEach { fallback ->
                if (ordered.size < GRID_COUNT && fallback !in ordered) {
                    ordered.add(fallback)
                }
            }
        }
        return ordered
    }

    /**
     * Formats an integer amount with dot thousand separators for Algerian postal display (e.g. "20.000").
     */
    fun formatGridAmount(amount: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
        }
        return DecimalFormat("#,##0", symbols).format(amount)
    }
}
