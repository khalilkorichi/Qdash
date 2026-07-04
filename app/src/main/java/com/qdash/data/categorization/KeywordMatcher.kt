package com.qdash.data.categorization

import java.util.Locale

class KeywordMatcher {
    fun normalize(text: String): String {
        return text.trim()
            .lowercase(Locale.getDefault())
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
    }

    fun containsKeyword(normalizedText: String, keyword: String): Boolean {
        val normKeyword = normalize(keyword)
        if (normKeyword.isEmpty() || normalizedText.isEmpty()) return false
        return normalizedText.contains(normKeyword)
    }
}
