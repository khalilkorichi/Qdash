package com.qdash.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatterUtils {
    @Volatile var hideDecimals: Boolean = true
    @Volatile var useWesternNumerals: Boolean = true
    @Volatile var useAlgerianMonths: Boolean = true

    val algerianMonths = arrayOf(
        "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
        "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    val standardArabicMonths = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun getMonthName(monthIndex: Int, algerian: Boolean = useAlgerianMonths): String {
        val idx = monthIndex.coerceIn(0, 11)
        return if (algerian) algerianMonths[idx] else standardArabicMonths[idx]
    }

    fun getMonthNames(algerian: Boolean = useAlgerianMonths): Array<String> {
        return if (algerian) algerianMonths else standardArabicMonths
    }

    fun getMonthNamesList(algerian: Boolean = useAlgerianMonths): List<String> {
        return getMonthNames(algerian).toList()
    }

    fun convertNumerals(input: String, useWestern: Boolean = useWesternNumerals): String {
        if (useWestern) {
            return input.map { char ->
                when (char) {
                    '٠' -> '0'
                    '١' -> '1'
                    '٢' -> '2'
                    '٣' -> '3'
                    '٤' -> '4'
                    '٥' -> '5'
                    '٦' -> '6'
                    '٧' -> '7'
                    '٨' -> '8'
                    '٩' -> '9'
                    '٫' -> '.'
                    '٬' -> ','
                    else -> char
                }
            }.joinToString("")
        } else {
            return input.map { char ->
                when (char) {
                    '0' -> '٠'
                    '1' -> '١'
                    '2' -> '٢'
                    '3' -> '٣'
                    '4' -> '٤'
                    '5' -> '٥'
                    '6' -> '٦'
                    '7' -> '٧'
                    '8' -> '٨'
                    '9' -> '٩'
                    '.' -> '٫'
                    ',' -> '٬'
                    else -> char
                }
            }.joinToString("")
        }
    }

    private val decimalFormatWithDecimals = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    }
    private val decimalFormatWithoutDecimals = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    }

    fun currencySymbol(currencyCode: String): String = when (currencyCode.trim().uppercase(Locale.US)) {
        "DZD", "DA", "دج", "د.ج", "" -> "دج"
        "EUR" -> "€"
        "USD" -> "$"
        "GBP" -> "£"
        "CAD" -> "CA$"
        "SAR" -> "ر.س"
        "AED" -> "د.إ"
        "TND" -> "د.ت"
        else -> currencyCode.trim().uppercase(Locale.US)
    }

    fun formatCurrency(amount: Double, prefix: String = "", currencyCode: String = "DZD"): String {
        val format = if (hideDecimals) decimalFormatWithoutDecimals.get()!! else decimalFormatWithDecimals.get()!!
        val formattedNumber = format.format(amount)
        val unit = currencySymbol(currencyCode)
        val safePrefix = if (prefix.trim().uppercase(Locale.US) in setOf("DZD", "DA", "دج", "د.ج")) "" else prefix
        val result = if (safePrefix.isNotEmpty()) "\u200E$safePrefix$formattedNumber $unit" else "$formattedNumber $unit"
        return convertNumerals(result)
    }

    fun formatColloquialAlgerian(amount: Double): String? {
        val absAmount = kotlin.math.abs(amount)
        if (absAmount < 1.0) return null
        val words = AmountConversionEngine.getAlgerianColloquialWords(absAmount)
        return if (amount < 0) "ناقص $words" else words
    }

    fun formatDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthStr = getMonthName(cal.get(Calendar.MONTH))
        val year = cal.get(Calendar.YEAR)
        return convertNumerals("$day $monthStr $year")
    }

    fun formatShortDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthStr = getMonthName(cal.get(Calendar.MONTH))
        return convertNumerals("$day $monthStr")
    }

    fun formatDateToMonthYear(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val monthStr = getMonthName(cal.get(Calendar.MONTH))
        val year = cal.get(Calendar.YEAR)
        return convertNumerals("$monthStr $year")
    }

    /**
     * Formats epoch milliseconds as local-timezone time string (HH:mm).
     * Only call this when occurredAt is non-null — never fabricate times for legacy records.
     */
    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        return convertNumerals(sdf.format(Date(timestamp)))
    }

    /**
     * Formats epoch milliseconds as full date+time string (e.g. "16 يوليو 2026 — 14:35").
     * Only call this when occurredAt is non-null.
     */
    fun formatDateTime(timestamp: Long): String {
        val datePart = formatDate(timestamp)
        val timePart = formatTime(timestamp)
        return "$datePart — $timePart"
    }


    fun formatFileSize(size: Long): String {
        if (size <= 0) return convertNumerals("0 ب")
        val units = arrayOf("ب", "ك.ب", "م.ب", "ج.ب", "ت.ب")
        val digitGroups = (kotlin.math.log10(size.toDouble()) / kotlin.math.log10(1024.0))
            .toInt()
            .coerceIn(0, units.lastIndex)
        val num = size / Math.pow(1024.0, digitGroups.toDouble())
        val formatted = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.US)).format(num)
        return convertNumerals("$formatted ${units[digitGroups]}")
    }

    fun normalizeAmount(input: String): String {
        return input.map { char ->
            when (char) {
                '٠' -> '0'
                '١' -> '1'
                '٢' -> '2'
                '٣' -> '3'
                '٤' -> '4'
                '٥' -> '5'
                '٦' -> '6'
                '٧' -> '7'
                '٨' -> '8'
                '٩' -> '9'
                '٫' -> '.'
                '٬' -> ','
                else -> char
            }
        }.joinToString("").replace(",", "").replace(" ", "").trim()
    }
}
