package com.example.core.utils

object CalculatorParser {

    /**
     * Evaluates a basic mathematical expression containing numbers and basic operators (+, -, ×, ÷).
     * Respects operator precedence (multiplication and division first, then addition and subtraction).
     */
    fun evaluate(expression: String): Double {
        try {
            // Clean the expression
            val cleaned = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace(" ", "")
            if (cleaned.isEmpty()) return 0.0

            // We tokenize the expression into numbers and operators (+, -)
            val parts = mutableListOf<String>()
            var currentNum = StringBuilder()
            
            var i = 0
            while (i < cleaned.length) {
                val char = cleaned[i]
                if (char == '+' || char == '-') {
                    if (currentNum.isNotEmpty()) {
                        parts.add(currentNum.toString())
                        currentNum = StringBuilder()
                    }
                    parts.add(char.toString())
                } else {
                    currentNum.append(char)
                }
                i++
            }
            if (currentNum.isNotEmpty()) {
                parts.add(currentNum.toString())
            }

            // Now evaluate multiplication and division in terms separated by + or -
            val termResults = mutableListOf<Double>()
            val operators = mutableListOf<Char>()

            var index = 0
            while (index < parts.size) {
                val part = parts[index]
                if (part == "+" || part == "-") {
                    operators.add(part[0])
                } else {
                    termResults.add(evaluateMulDiv(part))
                }
                index++
            }

            // Now evaluate addition and subtraction left-to-right
            var result = termResults.firstOrNull() ?: 0.0
            for (j in 0 until operators.size) {
                val op = operators[j]
                val nextVal = termResults.getOrNull(j + 1) ?: 0.0
                if (op == '+') {
                    result += nextVal
                } else if (op == '-') {
                    result -= nextVal
                }
            }
            return result
        } catch (e: Exception) {
            return 0.0
        }
    }

    private fun evaluateMulDiv(term: String): Double {
        val numbers = mutableListOf<Double>()
        val ops = mutableListOf<Char>()
        var currentNum = StringBuilder()
        
        for (char in term) {
            if (char == '*' || char == '/') {
                if (currentNum.isNotEmpty()) {
                    numbers.add(currentNum.toString().toDoubleOrNull() ?: 0.0)
                    currentNum = StringBuilder()
                }
                ops.add(char)
            } else {
                currentNum.append(char)
            }
        }
        if (currentNum.isNotEmpty()) {
            numbers.add(currentNum.toString().toDoubleOrNull() ?: 0.0)
        }

        var res = numbers.firstOrNull() ?: 0.0
        for (i in 0 until ops.size) {
            val op = ops[i]
            val nextVal = numbers.getOrNull(i + 1) ?: 1.0
            if (op == '*') {
                res *= nextVal
            } else if (op == '/') {
                res = if (nextVal != 0.0) res / nextVal else 0.0
            }
        }
        return res
    }
}
