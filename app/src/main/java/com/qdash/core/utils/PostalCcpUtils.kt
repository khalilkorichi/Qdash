package com.qdash.core.utils

import java.util.Locale

/**
 * Official Algérie Poste CCP Clé, RIP Clé, RIP formatting, and input extraction utilities.
 */
object PostalCcpUtils {

    private const val ALGERIE_POSTE_RIP_PREFIX = "00799999"
    private const val MAX_CCP_DIGITS = 10

    data class ExtractedCcpInfo(
        val ccpNumber: String,
        val ccpKey: String,
        val ripKey: String,
        val fullRip: String,
        val isRipKeyValid: Boolean
    )

    val quickAlgerianPlaces: List<String> = listOf(
        "الجزائر",
        "وهران",
        "قسنطينة",
        "بومرداس",
        "البليدة",
        "سطيف",
        "تيزي وزو",
        "عنابة",
        "باتنة",
        "ورقلة"
    )

    /**
     * Calculates the official 2-digit Algérie Poste Clé CCP using right-to-left weights 4..13.
     */
    fun calculateCcpKey(ccpNumber: String): String? {
        val digits = normalizeValidCcpDigits(ccpNumber) ?: return null
        var sum = 0
        var weight = 4
        for (i in digits.length - 1 downTo 0) {
            val d = digits[i] - '0'
            sum += d * weight
            weight++
        }
        val key = sum % 100
        return String.format(Locale.US, "%02d", key)
    }

    /**
     * Calculates the official 2-digit Algérie Poste Clé RIP: 97 - ((ccp * 100 + 85) % 97).
     */
    fun calculateRipKey(ccpNumber: String): String? {
        val digits = normalizeValidCcpDigits(ccpNumber) ?: return null
        val ccpLong = digits.toLongOrNull() ?: return null
        val rem = (ccpLong * 100L + 85L) % 97L
        val ripKey = 97L - rem
        return String.format(Locale.US, "%02d", ripKey)
    }

    /**
     * Builds the full 20-digit Algérie Poste RIP ("00799999" + 10-digit zero-padded CCP + Clé RIP).
     */
    fun buildFullRip(ccpNumber: String): String? {
        val digits = normalizeValidCcpDigits(ccpNumber) ?: return null
        val trimmed = digits.trimStart('0')
        if (trimmed.isEmpty()) return null
        val ripKey = calculateRipKey(trimmed) ?: return null
        return "$ALGERIE_POSTE_RIP_PREFIX${trimmed.padStart(MAX_CCP_DIGITS, '0')}$ripKey"
    }

    /**
     * Parses a 20-digit RIP (starting with 00799999), a "CCP/Clé" / "CCP-Clé" / "CCP Clé" pair,
     * or a plain CCP account number and returns normalized account info.
     */
    fun extractFromInput(rawInput: String): ExtractedCcpInfo? {
        val normalized = FormatterUtils.convertNumerals(rawInput, useWestern = true).trim()
        if (normalized.isEmpty()) return null

        val allDigits = normalized.filter { it.isDigit() }
        if (allDigits.length == 20 && allDigits.startsWith(ALGERIE_POSTE_RIP_PREFIX)) {
            val middleCcp = allDigits.substring(8, 18).trimStart('0')
            if (middleCcp.isEmpty()) return null
            val providedRipKey = allDigits.substring(18, 20)
            val ccpKey = calculateCcpKey(middleCcp) ?: return null
            val expectedRipKey = calculateRipKey(middleCcp) ?: return null
            val fullRip = buildFullRip(middleCcp) ?: return null
            return ExtractedCcpInfo(
                ccpNumber = middleCcp,
                ccpKey = ccpKey,
                ripKey = expectedRipKey,
                fullRip = fullRip,
                isRipKeyValid = providedRipKey == expectedRipKey
            )
        }

        val pairMatch = CCP_KEY_PAIR_REGEX.matchEntire(normalized)
        val ccpCandidate = pairMatch?.groupValues?.get(1) ?: allDigits
        val validDigits = normalizeValidCcpDigits(ccpCandidate) ?: return null
        val trimmedCcp = validDigits.trimStart('0')
        if (trimmedCcp.isEmpty()) return null

        val ccpKey = calculateCcpKey(trimmedCcp) ?: return null
        val ripKey = calculateRipKey(trimmedCcp) ?: return null
        val fullRip = buildFullRip(trimmedCcp) ?: return null

        return ExtractedCcpInfo(
            ccpNumber = trimmedCcp,
            ccpKey = ccpKey,
            ripKey = ripKey,
            fullRip = fullRip,
            isRipKeyValid = true
        )
    }

    /**
     * Checks whether the provided 2-digit key matches the calculated Algérie Poste Clé CCP.
     */
    fun isValidCcpKey(ccpNumber: String, key: String): Boolean {
        val expected = calculateCcpKey(ccpNumber) ?: return false
        val normalizedKey = FormatterUtils.convertNumerals(key, useWestern = true).filter { it.isDigit() }
        return normalizedKey.length == 2 && normalizedKey == expected
    }

    private fun normalizeValidCcpDigits(ccpNumber: String): String? {
        val western = FormatterUtils.convertNumerals(ccpNumber, useWestern = true)
        val digits = western.filter { it.isDigit() }
        if (digits.isEmpty() || digits.length > MAX_CCP_DIGITS || digits.all { it == '0' }) {
            return null
        }
        return digits
    }

    private val CCP_KEY_PAIR_REGEX = Regex(
        """^(\d{1,10})(?:\s*[/\-]\s*|\s+(?:cl[eé]\s*)?)(\d{2})$""",
        RegexOption.IGNORE_CASE
    )
}
