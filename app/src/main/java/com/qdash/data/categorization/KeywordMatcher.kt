package com.qdash.data.categorization

import java.util.Locale

class KeywordMatcher {

    private val arabicPrefixes = listOf("وال", "بال", "لال", "فال", "كال", "لل", "ال", "و", "ف", "ب", "ل", "ك")
    private val diacriticsRegex = Regex("[\\u064B-\\u065F\\u0670\\u0640]")
    private val tokenSplitRegex = Regex("[\\s,.;:/\\\\|()\\[\\]{}_+\\-!؟?«»\"']+")

    fun normalize(text: String): String {
        return text.trim()
            .lowercase(Locale.ROOT)
            .replace(diacriticsRegex, "")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ٱ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("à", "a")
            .replace("ç", "c")
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Returns a positive specificity score when [keyword] matches [normalizedText], or 0 when unmatched.
     * Prevents short-token false positives (e.g. "ماء" inside "أسماء", "كار" inside "كارطة")
     * while supporting Arabic clitic prefixes ("الـ", "بـ", "لـ", "بالـ") and multi-word phrases.
     */
    fun scoreKeywordMatch(normalizedText: String, keyword: String): Int {
        val normKeyword = normalize(keyword)
        if (normKeyword.isEmpty() || normalizedText.isEmpty()) return 0

        if (normalizedText == normKeyword) {
            return 1000 + normKeyword.length * 10
        }

        // Multi-word phrase match
        if (normKeyword.contains(' ')) {
            val idx = normalizedText.indexOf(normKeyword)
            if (idx >= 0) {
                val validStart = idx == 0 || !normalizedText[idx - 1].isLetterOrDigit()
                val endIdx = idx + normKeyword.length
                val validEnd = endIdx == normalizedText.length || !normalizedText[endIdx].isLetterOrDigit()
                if (validStart && validEnd) {
                    return 600 + normKeyword.length * 10
                }
                if (normKeyword.length >= 6) {
                    return 450 + normKeyword.length * 8
                }
            }
            return 0
        }

        val tokens = normalizedText.split(tokenSplitRegex).filter { it.isNotEmpty() }
        for (token in tokens) {
            if (token == normKeyword) {
                return 400 + normKeyword.length * 10
            }
            // Match with standard Arabic attached prefixes (e.g. "بالذهبية", "للمدرسة", "والخبز")
            for (prefix in arabicPrefixes) {
                if (token.length > prefix.length + 1 && token.startsWith(prefix) && token.removePrefix(prefix) == normKeyword) {
                    return 360 + normKeyword.length * 10
                }
            }
            // Safe stem match only for sufficiently long roots (>= 5 chars)
            if (normKeyword.length >= 5 && token.contains(normKeyword)) {
                return 250 + normKeyword.length * 8
            }
        }

        return 0
    }

    fun containsKeyword(normalizedText: String, keyword: String): Boolean {
        return scoreKeywordMatch(normalizedText, keyword) > 0
    }
}
