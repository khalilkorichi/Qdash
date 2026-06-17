package com.example.core.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatterUtils {
    var hideDecimals: Boolean = true
    var useWesternNumerals: Boolean = true

    fun convertNumerals(input: String): String {
        if (useWesternNumerals) {
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
                    else -> char
                }
            }.joinToString("")
        }
    }

    private val decimalFormatWithDecimals = DecimalFormat("#,##0.00")
    private val decimalFormatWithoutDecimals = DecimalFormat("#,##0")
    
    fun formatCurrency(amount: Double): String {
        val format = if (hideDecimals) decimalFormatWithoutDecimals else decimalFormatWithDecimals
        return convertNumerals("${format.format(amount)} دج")
    }

    fun formatColloquialAlgerian(amount: Double): String? {
        val absAmount = kotlin.math.abs(amount)
        if (absAmount < 1.0) return null
        
        val centimes = absAmount * 100.0
        val text = when {
            centimes >= 1_000_000_000.0 -> {
                val billions = centimes / 1_000_000_000.0
                if (billions % 1.0 == 0.0) {
                    when (billions) {
                        1.0 -> "مليار سنتيم"
                        2.0 -> "مليارين سنتيم"
                        in 3.0..10.0 -> "${billions.toInt()} ملايير سنتيم"
                        else -> "${billions.toInt()} مليار سنتيم"
                    }
                } else {
                    "${String.format(Locale.US, "%.1f", billions)} مليار سنتيم"
                }
            }
            centimes >= 1_000_000.0 -> {
                val millions = centimes / 1_000_000.0
                if (millions % 1.0 == 0.0) {
                    when (millions) {
                        1.0 -> "مليون سنتيم"
                        2.0 -> "مليونين سنتيم"
                        in 3.0..10.0 -> "${millions.toInt()} ملاين سنتيم"
                        else -> "${millions.toInt()} مليون سنتيم"
                    }
                } else {
                    val formatted = String.format(Locale.US, "%.2f", millions)
                        .replace(Regex("\\.?0+$"), "")
                    "$formatted مليون سنتيم"
                }
            }
            centimes >= 1000.0 -> {
                val thousands = centimes / 1000.0
                when (thousands) {
                    1.0 -> "ألف سنتيم"
                    2.0 -> "ألفين سنتيم"
                    in 3.0..10.0 -> "${thousands.toInt()} آلاف سنتيم"
                    else -> "${thousands.toInt()} ألف سنتيم"
                }
            }
            else -> {
                "${centimes.toInt()} سنتيم"
            }
        }
        val result = if (amount < 0) "ناقص $text" else text
        return convertNumerals(result)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        return convertNumerals(sdf.format(Date(timestamp)))
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale("ar"))
        return convertNumerals(sdf.format(Date(timestamp)))
    }

    fun formatDateToMonthYear(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("ar"))
        return convertNumerals(sdf.format(Date(timestamp)))
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
