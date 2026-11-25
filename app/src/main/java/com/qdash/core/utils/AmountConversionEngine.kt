package com.qdash.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Unified Single Source of Truth for Number-to-Words (Tafqeet) and Postal Amount Formatting.
 * Centrally located in core/utils for reuse across:
 *  1. Document Simulator / Postal Profiles
 *  2. Add Transaction Screen
 *  3. Currency Exchange Converter
 */
object AmountConversionEngine {

    /**
     * Parses flexible user amount strings supporting Western & Eastern Arabic digits,
     * postal format ("20.000,00"), French decimal comma ("5000,50"), and standard ("5000.50").
     */
    fun parseFlexibleAmount(raw: String): Double? {
        if (raw.isBlank()) return null
        val western = FormatterUtils.convertNumerals(raw, useWestern = true)
            .replace(" ", "")
            .replace("\u00A0", "")
            .trim()
        if (western.isEmpty()) return null

        val normalized = when {
            western.contains('.') && western.contains(',') -> {
                if (western.lastIndexOf(',') > western.lastIndexOf('.')) {
                    // Postal/European: 20.000,50 -> 20000.50
                    western.replace(".", "").replace(',', '.')
                } else {
                    // US: 20,000.50 -> 20000.50
                    western.replace(",", "")
                }
            }
            western.contains(',') -> {
                val parts = western.split(',')
                // Single comma with 1-2 trailing digits = decimal separator (e.g. "5000,00" or "12,5")
                if (parts.size == 2 && parts[1].length in 1..2) {
                    "${parts[0]}.${parts[1]}"
                } else {
                    western.replace(",", "")
                }
            }
            else -> western
        }
        return normalized.toDoubleOrNull()
    }

    /**
     * Format number to standard Algerian post layout (e.g. 20000 -> "20.000,00")
     */
    fun formatAmountToPostal(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,##0.00", symbols)
        return formatter.format(amount)
    }

    /**
     * Convert Dinar amount to official written Arabic words (Tafqeet)
     */
    /**
     * Convert Dinar amount to written Arabic words in Centimes (default) or official Dinars.
     */
    fun convertToArabicWords(amount: Double, preferCentimes: Boolean = true): String {
        if (preferCentimes) {
            return getAlgerianColloquialWords(amount)
        }

        val dinars = amount.toLong()
        val centimes = ((amount - dinars) * 100 + 0.5).toInt()

        if (dinars == 0L && centimes == 0) {
            return "صفر دينار جزائري"
        }

        val dinarPart = if (dinars > 0) {
            var words = convertNumberToWords(dinars)
            if (words.endsWith("مائتان")) {
                words = words.substring(0, words.length - 1)
            } else if (words.endsWith("ألفان")) {
                words = words.substring(0, words.length - 1)
            } else if (words.endsWith("مليونان")) {
                words = words.substring(0, words.length - 1)
            } else if (words.endsWith("ملياران")) {
                words = words.substring(0, words.length - 1)
            }
            
            val mod100 = dinars % 100L
            val suffix = when {
                dinars == 2L -> "ديناران جزائريان"
                dinars in 3L..10L -> "دنانير جزائرية"
                mod100 in 11L..99L -> "ديناراً جزائرياً"
                else -> "دينار جزائري"
            }
            if (dinars == 2L) suffix else "$words $suffix"
        } else ""

        val centimePart = if (centimes > 0) {
            "${convertNumberToWords(centimes.toLong())} سنتيم"
        } else ""

        return when {
            dinarPart.isNotEmpty() && centimePart.isNotEmpty() -> "$dinarPart و $centimePart"
            dinarPart.isNotEmpty() -> dinarPart
            else -> centimePart
        }
    }

    /**
     * Convert Centimes amount (Long) directly to official written Arabic words by converting to Dinars (/100) with 100% precision.
     */
    fun convertCentimesToArabicWords(amountInCentimes: Long): String {
        val dinars = amountInCentimes / 100
        val remainingCentimes = amountInCentimes % 100
        return if (remainingCentimes == 0L) {
            convertToArabicWords(dinars.toDouble(), preferCentimes = false)
        } else {
            val doubleDinars = amountInCentimes.toDouble() / 100.0
            convertToArabicWords(doubleDinars, preferCentimes = false)
        }
    }

    /**
     * Convert Centimes amount (Long) directly to Algerian colloquial words.
     */
    fun convertCentimesToColloquialWords(amountInCentimes: Long): String {
        val dinars = amountInCentimes / 100.0
        return getAlgerianColloquialWords(dinars)
    }

    /**
     * Map Dinar amount to Algerian colloquial equivalent representation in Centimes (100% Arabic letters).
     */
    fun getAlgerianColloquialWords(amount: Double): String {
        val dinars = amount.toLong()
        if (dinars <= 0) return "صفر سنتيم"

        return when {
            dinars == 10L -> "ألف سنتيم"
            dinars == 20L -> "ألفين سنتيم"
            dinars == 50L -> "خمس لاف سنتيم"
            dinars == 100L -> "عشر لاف سنتيم"
            dinars == 200L -> "عشرين ألف سنتيم"
            dinars == 500L -> "خمسين ألف سنتيم"
            dinars == 1000L -> "مية ألف سنتيم"
            dinars == 2000L -> "ميتين ألف سنتيم"
            dinars == 5000L -> "خمس مية ألف سنتيم"
            dinars == 10000L -> "مليون سنتيم"
            dinars == 15000L -> "مليون ونص سنتيم"
            dinars == 20000L -> "زوج ملايين سنتيم"
            dinars == 30000L -> "ثلاثة ملايين سنتيم"
            dinars == 40000L -> "أربعة ملايين سنتيم"
            dinars == 50000L -> "خمس ملايين سنتيم"
            dinars == 100000L -> "عشر ملايين سنتيم"
            dinars == 200000L -> "عشرين مليون سنتيم"
            dinars == 500000L -> "خمسين مليون سنتيم"
            dinars == 1000000L -> "مية مليون سنتيم"
            dinars == 2000000L -> "ميتين مليون سنتيم"
            dinars == 5000000L -> "خمس مية مليون سنتيم"
            dinars == 10000000L -> "مليار سنتيم"
            else -> {
                if (dinars >= 10000L) {
                    val millions = dinars / 10000L
                    val remainder = dinars % 10000L
                    val millionsText = convertNumberToWords(millions)
                    val millionWord = when {
                        millions == 1L -> "مليون"
                        millions == 2L -> "زوج ملايين"
                        millions in 3..10 -> "$millionsText ملايين"
                        else -> "$millionsText مليون"
                    }
                    if (remainder == 0L) {
                        "$millionWord سنتيم"
                    } else {
                        val remainderText = getAlgerianColloquialWords(remainder.toDouble())
                        "$millionWord و $remainderText"
                    }
                } else {
                    val centimes = dinars * 100
                    "${convertNumberToWords(centimes)} سنتيم"
                }
            }
        }
    }

    private val units = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    private val tens = arrayOf("", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    private val hundreds = arrayOf("", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")

    private fun convertNumberToWords(number: Long): String {
        if (number == 0L) return ""
        
        if (number < 10) return units[number.toInt()]
        if (number < 20) {
            return when (number) {
                10L -> "عشرة"
                11L -> "أحد عشر"
                12L -> "اثنا عشر"
                else -> "${units[(number % 10).toInt()]} عشر"
            }
        }
        if (number < 100) {
            val tenIndex = (number / 10).toInt()
            val unitValue = (number % 10).toInt()
            return if (unitValue == 0) tens[tenIndex] else "${units[unitValue]} و ${tens[tenIndex]}"
        }
        if (number < 1000) {
            val hundredIndex = (number / 100).toInt()
            val remainder = number % 100
            val hundredWord = hundreds[hundredIndex]
            return if (remainder == 0L) hundredWord else "$hundredWord و ${convertNumberToWords(remainder)}"
        }
        if (number < 1000000) {
            val thousands = number / 1000
            val remainder = number % 1000
            val thousandWord = when {
                thousands == 1L -> "ألف"
                thousands == 2L -> "ألفان"
                thousands in 3..10 -> "${convertNumberToWords(thousands)} آلاف"
                else -> {
                    var tw = convertNumberToWords(thousands)
                    if (tw.endsWith("مائتان")) {
                        tw = tw.substring(0, tw.length - 1)
                    }
                    "$tw ألف"
                }
            }
            return if (remainder == 0L) thousandWord else "$thousandWord و ${convertNumberToWords(remainder)}"
        }
        if (number < 1000000000L) {
            val millions = number / 1000000
            val remainder = number % 1000000
            val millionWord = when {
                millions == 1L -> "مليون"
                millions == 2L -> "مليونان"
                millions in 3..10 -> "${convertNumberToWords(millions)} ملايين"
                else -> "${convertNumberToWords(millions)} مليون"
            }
            return if (remainder == 0L) millionWord else "$millionWord و ${convertNumberToWords(remainder)}"
        }
        
        val billions = number / 1000000000L
        val remainder = number % 1000000000L
        val billionWord = when {
            billions == 1L -> "مليار"
            billions == 2L -> "ملياران"
            billions in 3..10 -> "${convertNumberToWords(billions)} مليارات"
            else -> "${convertNumberToWords(billions)} مليار"
        }
        return if (remainder == 0L) billionWord else "$billionWord و ${convertNumberToWords(remainder)}"
    }

    /**
     * Converts a Dinar amount to official Algerian postal French words (e.g. 5200.0 -> "Cinq mille deux cents dinars").
     */
    fun convertToFrenchWords(amount: Double): String {
        val safeAmount = if (amount.isFinite() && amount > 0.0) amount else 0.0
        val totalCentimes = kotlin.math.round(safeAmount * 100.0).toLong()
        val dinars = totalCentimes / 100L
        val centimes = (totalCentimes % 100L).toInt()

        if (dinars == 0L && centimes == 0) {
            return "Zéro dinar"
        }

        val dinarPart = if (dinars > 0L) {
            val words = convertFrenchNumber(dinars)
            val currencyUnit = when {
                dinars == 1L -> "dinar"
                dinars >= 1_000_000L && dinars % 1_000_000L == 0L -> "de dinars"
                else -> "dinars"
            }
            "$words $currencyUnit"
        } else ""

        val centimePart = if (centimes > 0) {
            val words = convertFrenchBelow100(centimes, allowFinalPluralS = true)
            val unit = if (centimes == 1) "centime" else "centimes"
            "$words $unit"
        } else ""

        val combined = when {
            dinarPart.isNotEmpty() && centimePart.isNotEmpty() -> "$dinarPart et $centimePart"
            dinarPart.isNotEmpty() -> dinarPart
            else -> centimePart
        }

        return combined.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.FRENCH) else ch.toString()
        }
    }

    private val frenchUnits = arrayOf(
        "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
        "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
        "dix-sept", "dix-huit", "dix-neuf"
    )
    private val frenchTens = arrayOf(
        "", "dix", "vingt", "trente", "quarante", "cinquante", "soixante"
    )

    private fun convertFrenchNumber(number: Long): String {
        if (number == 0L) return "zéro"

        val parts = mutableListOf<String>()
        val billions = number / 1_000_000_000L
        val millions = (number % 1_000_000_000L) / 1_000_000L
        val thousands = (number % 1_000_000L) / 1_000L
        val remainder = (number % 1_000L).toInt()

        if (billions > 0L) {
            parts.add(
                when {
                    billions == 1L -> "un milliard"
                    billions < 1_000L -> "${convertFrenchBelow1000(billions.toInt(), allowFinalPluralS = true)} milliards"
                    else -> "${convertFrenchNumber(billions)} milliards"
                }
            )
        }
        if (millions > 0L) {
            parts.add(
                if (millions == 1L) "un million"
                else "${convertFrenchBelow1000(millions.toInt(), allowFinalPluralS = true)} millions"
            )
        }
        if (thousands > 0L) {
            parts.add(
                if (thousands == 1L) "mille"
                else "${convertFrenchBelow1000(thousands.toInt(), allowFinalPluralS = false)} mille"
            )
        }
        if (remainder > 0) {
            parts.add(convertFrenchBelow1000(remainder, allowFinalPluralS = true))
        }

        return parts.joinToString(" ")
    }

    private fun convertFrenchBelow1000(number: Int, allowFinalPluralS: Boolean): String {
        val hundreds = number / 100
        val remainder = number % 100
        if (hundreds == 0) {
            return convertFrenchBelow100(remainder, allowFinalPluralS)
        }
        val hundredBase = if (hundreds == 1) {
            "cent"
        } else {
            val pluralS = if (remainder == 0 && allowFinalPluralS) "s" else ""
            "${frenchUnits[hundreds]} cent$pluralS"
        }
        return if (remainder == 0) {
            hundredBase
        } else {
            "$hundredBase ${convertFrenchBelow100(remainder, allowFinalPluralS)}"
        }
    }

    private fun convertFrenchBelow100(number: Int, allowFinalPluralS: Boolean): String {
        if (number <= 0) return ""
        if (number < 20) return frenchUnits[number]

        if (number < 70) {
            val ten = number / 10
            val unit = number % 10
            return when (unit) {
                0 -> frenchTens[ten]
                1 -> "${frenchTens[ten]} et un"
                else -> "${frenchTens[ten]}-${frenchUnits[unit]}"
            }
        }
        if (number < 80) {
            val sub = number - 60
            return if (sub == 11) "soixante et onze" else "soixante-${frenchUnits[sub]}"
        }
        val sub = number - 80
        return if (sub == 0) {
            if (allowFinalPluralS) "quatre-vingts" else "quatre-vingt"
        } else {
            "quatre-vingt-${frenchUnits[sub]}"
        }
    }
}
