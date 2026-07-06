package com.qdash.presentation.transactions

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Pure functions to sanitize the amount input and handle custom numpad key inputs.
 * Extracted from AddTransactionScreen.kt to keep the screen file modular.
 */

fun sanitizeAmountTextFieldValue(value: TextFieldValue): TextFieldValue {
    fun normalize(input: String): String {
        val operators = setOf('+', '-', '×', '÷')
        val output = StringBuilder()
        var hasDecimalInToken = false
        var decimalPlaces = 0

        fun appendOperator(operator: Char) {
            while (output.endsWith(" ")) output.deleteCharAt(output.lastIndex)
            val last = output.lastOrNull()
            if (output.isEmpty()) {
                output.append('0')
            } else if (last != null && operators.contains(last)) {
                output.deleteCharAt(output.lastIndex)
                while (output.endsWith(" ")) output.deleteCharAt(output.lastIndex)
            }
            output.append(' ').append(operator).append(' ')
            hasDecimalInToken = false
            decimalPlaces = 0
        }

        input.forEach { char ->
            when (char) {
                in '0'..'9' -> {
                    if (!hasDecimalInToken || decimalPlaces < 2) {
                        output.append(char)
                        if (hasDecimalInToken) decimalPlaces++
                    }
                }
                '٠' -> output.append('0')
                '١' -> output.append('1')
                '٢' -> output.append('2')
                '٣' -> output.append('3')
                '٤' -> output.append('4')
                '٥' -> output.append('5')
                '٦' -> output.append('6')
                '٧' -> output.append('7')
                '٨' -> output.append('8')
                '٩' -> output.append('9')
                '.', '٫' -> {
                    if (!hasDecimalInToken) {
                        val tokenStart = output.isEmpty() || output.endsWith(" ")
                        if (tokenStart) output.append('0')
                        output.append('.')
                        hasDecimalInToken = true
                        decimalPlaces = 0
                    }
                }
                '+', '-' -> appendOperator(char)
                '×', '*' -> appendOperator('×')
                '÷', '/' -> appendOperator('÷')
                ',', '٬', ' ', '\u00A0', '\u202F' -> Unit
            }
        }

        val normalized = output.toString()
        if (normalized.isBlank()) return "0"

        return normalized.split(' ').joinToString(" ") { token ->
            when {
                token.length == 1 && operators.contains(token.first()) -> token
                token.contains('.') -> {
                    val parts = token.split('.', limit = 2)
                    val integerPart = parts[0].trimStart('0').ifEmpty { "0" }
                    val decimalPart = parts.getOrNull(1).orEmpty().take(2)
                    "$integerPart.$decimalPart"
                }
                token.isNotEmpty() -> token.trimStart('0').ifEmpty { "0" }
                else -> token
            }
        }
    }

    val sanitizedText = normalize(value.text)
    val selectionStart = normalize(value.text.take(value.selection.min)).length.coerceIn(0, sanitizedText.length)
    val selectionEnd = normalize(value.text.take(value.selection.max)).length.coerceIn(0, sanitizedText.length)
    return TextFieldValue(sanitizedText, selection = TextRange(selectionStart, selectionEnd))
}

fun handleNumpadKey(value: TextFieldValue, key: String): TextFieldValue {
    val operators = setOf("+", "-", "×", "÷")
    val text = value.text
    val selection = value.selection
    val start = selection.min
    val end = selection.max

    fun insertAtCursor(insertText: String): TextFieldValue {
        val newText = text.substring(0, start) + insertText + text.substring(end)
        val newCursor = start + insertText.length
        return TextFieldValue(newText, selection = TextRange(newCursor))
    }

    fun isValidDecimalPlaces(t: String): Boolean {
        val tokens = t.split(" ")
        for (token in tokens) {
            val dotIndex = token.indexOf('.')
            if (dotIndex != -1) {
                if (token.length - 1 - dotIndex > 2) {
                    return false
                }
            }
        }
        return true
    }

    fun getTokenAtIndex(t: String, index: Int): String {
        var cumulativeLength = 0
        val tokens = t.split(" ")
        for (token in tokens) {
            val tokenLength = token.length
            if (index >= cumulativeLength && index <= cumulativeLength + tokenLength) {
                return token
            }
            cumulativeLength += tokenLength + 1
        }
        return tokens.lastOrNull() ?: ""
    }

    return when (key) {
        "⌫" -> {
            if (!selection.collapsed) {
                val newText = text.substring(0, start) + text.substring(end)
                val finalVal = if (newText.isEmpty()) "0" else newText
                val newCursor = if (newText.isEmpty()) 1 else start
                TextFieldValue(finalVal, selection = TextRange(newCursor))
            } else {
                if (start == 0) value
                else {
                    val beforeCursor = text.substring(0, start)
                    if (beforeCursor.endsWith(" ") && beforeCursor.length >= 3) {
                        val opChar = beforeCursor[beforeCursor.length - 2].toString()
                        val spaceBefore = beforeCursor[beforeCursor.length - 3]
                        if (operators.contains(opChar) && spaceBefore == ' ') {
                            val newText = text.substring(0, start - 3) + text.substring(start)
                            val finalVal = if (newText.isEmpty()) "0" else newText
                            val newCursor = if (newText.isEmpty()) 1 else (start - 3)
                            return TextFieldValue(finalVal, selection = TextRange(newCursor))
                        }
                    }
                    val newText = text.substring(0, start - 1) + text.substring(start)
                    val finalVal = if (newText.isEmpty()) "0" else newText
                    val newCursor = if (newText.isEmpty()) 1 else (start - 1)
                    TextFieldValue(finalVal, selection = TextRange(newCursor))
                }
            }
        }
        "C" -> TextFieldValue("0", selection = TextRange(1))
        "00" -> {
            if (text == "0" || text.isEmpty()) {
                TextFieldValue("0", selection = TextRange(1))
            } else {
                val beforeCursor = text.substring(0, start)
                val trimmedBefore = beforeCursor.trimEnd()
                val isAfterOperator = trimmedBefore.isNotEmpty() && operators.contains(trimmedBefore.last().toString())
                val textToInsert = if (isAfterOperator) "0" else "00"
                
                val result = insertAtCursor(textToInsert)
                if (isValidDecimalPlaces(result.text)) result else value
            }
        }
        "+", "-", "×", "÷" -> {
            if (text.isEmpty()) {
                TextFieldValue("0", selection = TextRange(1))
            } else {
                val beforeCursor = text.substring(0, start)
                val trimmedBefore = beforeCursor.trimEnd()
                if (trimmedBefore.isNotEmpty() && operators.contains(trimmedBefore.last().toString())) {
                    val opIndex = trimmedBefore.length - 1
                    val textBeforeOp = text.substring(0, opIndex)
                    val textAfterOp = text.substring(opIndex + 1)
                    val cleanAfterOp = if (textAfterOp.startsWith(" ")) textAfterOp.substring(1) else textAfterOp
                    val newText = textBeforeOp + "$key " + cleanAfterOp
                    val newCursor = textBeforeOp.length + "$key ".length
                    TextFieldValue(newText, selection = TextRange(newCursor))
                } else {
                    val afterCursor = text.substring(end)
                    val cleanBefore = beforeCursor.trimEnd()
                    val cleanAfter = afterCursor.trimStart()
                    val prefix = if (cleanBefore.isEmpty()) "0" else cleanBefore
                    val newText = prefix + " $key " + cleanAfter
                    val newCursor = prefix.length + " $key ".length
                    TextFieldValue(newText, selection = TextRange(newCursor))
                }
            }
        }
        "." -> {
            val currentToken = getTokenAtIndex(text, start)
            if (currentToken.contains(".") || operators.contains(currentToken)) {
                value
            } else if (currentToken.isEmpty()) {
                insertAtCursor("0.")
            } else {
                insertAtCursor(".")
            }
        }
        "=" -> {
            val eval = com.qdash.core.utils.CalculatorParser.evaluate(text)
            val evalResult = if (eval % 1 == 0.0) {
                eval.toInt().toString()
            } else {
                "%.2f".format(eval).replace(",", ".")
            }
            TextFieldValue(evalResult, selection = TextRange(evalResult.length))
        }
        else -> { // Digit keys "0" - "9"
            if (text == "0") {
                TextFieldValue(key, selection = TextRange(1))
            } else {
                val currentToken = getTokenAtIndex(text, start)
                if (currentToken == "0") {
                    val zeroIndex = if (start > 0 && text[start - 1] == '0') start - 1 else start
                    val newText = text.substring(0, zeroIndex) + key + text.substring(zeroIndex + 1)
                    val newCursor = zeroIndex + 1
                    val result = TextFieldValue(newText, selection = TextRange(newCursor))
                    if (isValidDecimalPlaces(result.text)) result else value
                } else {
                    val result = insertAtCursor(key)
                    if (isValidDecimalPlaces(result.text)) result else value
                }
            }
        }
    }
}
