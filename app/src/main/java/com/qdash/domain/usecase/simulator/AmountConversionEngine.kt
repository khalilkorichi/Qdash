package com.qdash.domain.usecase.simulator

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object AmountConversionEngine {

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
    fun convertToArabicWords(amount: Double): String {
        val dinars = amount.toLong()
        val centimes = ((amount - dinars) * 100 + 0.5).toInt()

        if (dinars == 0L && centimes == 0) {
            return "صفر دينار جزائري"
        }

        val dinarPart = if (dinars > 0) {
            var words = convertNumberToWords(dinars)
            if (words.endsWith("ألفان")) {
                words = words.substring(0, words.length - 1)
            } else if (words.endsWith("مليونان")) {
                words = words.substring(0, words.length - 1)
            } else if (words.endsWith("ملياران")) {
                words = words.substring(0, words.length - 1)
            }
            "$words دينار جزائري"
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
     * Map Dinar amount to Algerian colloquial equivalent representation (in Centimes)
     */
    fun getAlgerianColloquialWords(amount: Double): String {
        val dinars = amount.toLong()
        if (dinars <= 0) return "صفر سنتيم"

        return when {
            dinars == 10L -> "ألف سنتيم (10 دج)"
            dinars == 20L -> "ألفين سنتيم (20 دج)"
            dinars == 50L -> "خمسة آلاف سنتيم (50 دج)"
            dinars == 100L -> "عشرة آلاف سنتيم (100 دج)"
            dinars == 200L -> "عشرين ألف سنتيم (200 دج)"
            dinars == 500L -> "خمسين ألف سنتيم (500 دج)"
            dinars == 1000L -> "مية ألف سنتيم (1.000 دج)"
            dinars == 2000L -> "ميتين ألف سنتيم (2.000 دج)"
            dinars == 5000L -> "خمس مية ألف سنتيم (5.000 دج)"
            dinars == 10000L -> "مليون سنتيم (10.000 دج)"
            dinars == 15000L -> "مليون ونص سنتيم (15.000 دج)"
            dinars == 20000L -> "زوج ملايين سنتيم (20.000 دج)"
            dinars == 30000L -> "تلاتة ملايين سنتيم (30.000 دج)"
            dinars == 40000L -> "أربعة ملايين سنتيم (40.000 دج)"
            dinars == 50000L -> "خمس ملايين سنتيم (50.000 دج)"
            dinars == 100000L -> "عشر ملايين سنتيم (100.000 دج)"
            dinars == 200000L -> "عشرين مليون سنتيم (200.000 دج)"
            dinars == 500000L -> "خمسين مليون سنتيم (500.000 دج)"
            dinars == 1000000L -> "مية مليون سنتيم (1.000.000 دج)"
            dinars == 2000000L -> "ميتين مليون سنتيم (2.000.000 دج)"
            dinars == 5000000L -> "خمس مية مليون (نصف مليار) سنتيم (5.000.000 دج)"
            dinars == 10000000L -> "مليار سنتيم (10.000.000 دج)"
            else -> {
                if (dinars >= 10000L) {
                    val millions = dinars / 10000L
                    val remainder = dinars % 10000L
                    val millionWord = when {
                        millions == 1L -> "مليون"
                        millions == 2L -> "مليونين"
                        millions in 3..10 -> "$millions ملايين"
                        else -> "$millions مليون"
                    }
                    if (remainder == 0L) {
                        "$millionWord سنتيم"
                    } else {
                        val remainderDinars = remainder
                        val remainderCentimesText = getAlgerianColloquialWords(remainderDinars.toDouble())
                        "$millionWord و $remainderCentimesText"
                    }
                } else {
                    val centimes = dinars * 100
                    val formatter = DecimalFormat("#,###")
                    val formattedCentimes = formatter.format(centimes).replace(",", ".")
                    "$formattedCentimes سنتيم"
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
                else -> "${convertNumberToWords(thousands)} ألف"
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
}
