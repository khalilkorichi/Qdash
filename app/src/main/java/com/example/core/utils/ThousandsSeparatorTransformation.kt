package com.example.core.utils

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString

class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val parts = originalText.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else null

        val formattedInteger = try {
            val longVal = integerPart.toLongOrNull()
            if (longVal != null) {
                java.text.DecimalFormat("#,###").format(longVal)
            } else {
                integerPart
            }
        } catch (e: Exception) {
            integerPart
        }

        val transformedString = buildString {
            append(formattedInteger)
            if (decimalPart != null) {
                append(".")
                append(decimalPart)
            } else if (originalText.endsWith(".")) {
                append(".")
            }
        }

        // Custom offset mapping to map cursor position correctly
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val realOffset = offset.coerceAtMost(originalText.length)
                
                val subOriginal = originalText.substring(0, realOffset)
                val subOriginalParts = subOriginal.split(".")
                val subIntPart = subOriginalParts[0]
                val subDecPart = if (subOriginalParts.size > 1) subOriginalParts[1] else null
                
                val subFormattedInt = try {
                    val longVal = subIntPart.toLongOrNull()
                    if (longVal != null) {
                        java.text.DecimalFormat("#,###").format(longVal)
                    } else {
                        subIntPart
                    }
                } catch (e: Exception) {
                    subIntPart
                }
                
                val subTransformed = buildString {
                    append(subFormattedInt)
                    if (subDecPart != null) {
                        append(".")
                        append(subDecPart)
                    } else if (subOriginal.endsWith(".")) {
                        append(".")
                    }
                }
                return subTransformed.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val realOffset = offset.coerceAtMost(transformedString.length)
                
                var nonCommaCount = 0
                for (i in 0 until realOffset) {
                    if (transformedString[i] != ',') {
                        nonCommaCount++
                    }
                }
                return nonCommaCount.coerceAtMost(originalText.length)
            }
        }

        return TransformedText(
            text = AnnotatedString(transformedString),
            offsetMapping = offsetMapping
        )
    }
}
