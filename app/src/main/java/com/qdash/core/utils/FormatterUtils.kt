package com.qdash.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatterUtils {
    var hideDecimals: Boolean = true
    var useWesternNumerals: Boolean = true
    var useAlgerianMonths: Boolean = true

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

    private val usSymbols = DecimalFormatSymbols(Locale.US)
    private val decimalFormatWithDecimals = DecimalFormat("#,##0.00", usSymbols)
    private val decimalFormatWithoutDecimals = DecimalFormat("#,##0", usSymbols)
    
    fun formatCurrency(amount: Double, prefix: String = ""): String {
        val format = if (hideDecimals) decimalFormatWithoutDecimals else decimalFormatWithDecimals
        val formattedNumber = format.format(amount)
        val result = if (prefix.isNotEmpty()) "\u200E$prefix$formattedNumber دج" else "$formattedNumber دج"
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
        if (size <= 0) return "0 ب"
        val units = arrayOf("ب", "ك.ب", "م.ب", "ج.ب")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.toDouble())).toInt()
        val num = size / Math.pow(1024.toDouble(), digitGroups.toDouble())
        val formatted = java.text.DecimalFormat("#,##0.#").format(num)
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
                else -> char
            }
        }.joinToString("").replace(",", "").replace(" ", "").trim()
    }
}
