package com.example

import com.example.core.utils.CalculatorParser
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorParserTest {

    @Test
    fun testBasicArithmetic() {
        assertEquals(150.0, CalculatorParser.evaluate("100 + 50"), 0.001)
        assertEquals(50.0, CalculatorParser.evaluate("100 - 50"), 0.001)
    }

    @Test
    fun testMultiplicationAndDivision() {
        assertEquals(500.0, CalculatorParser.evaluate("100 × 5"), 0.001)
        assertEquals(20.0, CalculatorParser.evaluate("100 ÷ 5"), 0.001)
    }

    @Test
    fun testPrecedence() {
        assertEquals(200.0, CalculatorParser.evaluate("100 + 50 × 2"), 0.001)
        assertEquals(90.0, CalculatorParser.evaluate("100 - 50 ÷ 5"), 0.001)
    }

    @Test
    fun testSpacesAndEmpty() {
        assertEquals(150.0, CalculatorParser.evaluate(" 100  +  50  "), 0.001)
        assertEquals(0.0, CalculatorParser.evaluate(""), 0.001)
        assertEquals(0.0, CalculatorParser.evaluate("   "), 0.001)
    }

    @Test
    fun testNumberToArabicWordsDZ() {
        // Test case: 100.50
        val p1 = numberToArabicWordsDZ(100.50)
        assertEquals("مائة دينار وخمسون سنتيم", p1.first)
        assertEquals("عشرة آلاف وخمسون سنتيم", p1.second)

        // Test case: 2.02
        val p2 = numberToArabicWordsDZ(2.02)
        assertEquals("ديناران وسنتيمان", p2.first)
        assertEquals("مئتان واثنان سنتيم", p2.second)

        // Test case: 1.0
        val p3 = numberToArabicWordsDZ(1.0)
        assertEquals("دينار واحد", p3.first)
        assertEquals("مائة سنتيم", p3.second)

        // Test case: 0.05
        val p4 = numberToArabicWordsDZ(0.05)
        assertEquals("خمسة سنتيمات", p4.first)
        assertEquals("خمسة سنتيمات", p4.second)

        // Test case: 0.0
        val p5 = numberToArabicWordsDZ(0.0)
        assertEquals("", p5.first)
        assertEquals("", p5.second)

        // Test case: 25000.75
        val p6 = numberToArabicWordsDZ(25000.75)
        assertEquals("خمسة وعشرون ألف دينار وخمسة وسبعون سنتيم", p6.first)
        assertEquals("مليونان وخمسمائة ألف وخمسة وسبعون سنتيم", p6.second)
    }

    // Copy of helper functions to test them locally
    private fun numberToArabicWordsDZ(amount: Double): Pair<String, String> {
        val totalCentimes = Math.round(amount * 100.0)
        if (totalCentimes <= 0) return Pair("", "")
        
        val dinars = totalCentimes / 100
        val cents = totalCentimes % 100
        
        // Dinar representation
        val dinarText = when {
            dinars > 0 && cents > 0 -> {
                val dinarPart = when (dinars) {
                    1L -> "دينار واحد"
                    2L -> "ديناران"
                    in 3..10 -> "${convertWholeNumber(dinars)} دنانير"
                    else -> "${convertWholeNumber(dinars)} دينار"
                }
                val centsPart = when (cents) {
                    1L -> "سنتيم واحد"
                    2L -> "سنتيمان"
                    in 3..10 -> "${convertWholeNumber(cents)} سنتيمات"
                    else -> "${convertWholeNumber(cents)} سنتيم"
                }
                "$dinarPart و$centsPart"
            }
            dinars > 0 -> {
                when (dinars) {
                    1L -> "دينار واحد"
                    2L -> "ديناران"
                    in 3..10 -> "${convertWholeNumber(dinars)} دنانير"
                    else -> "${convertWholeNumber(dinars)} دينار"
                }
            }
            cents > 0 -> {
                when (cents) {
                    1L -> "سنتيم واحد"
                    2L -> "سنتيمان"
                    in 3..10 -> "${convertWholeNumber(cents)} سنتيمات"
                    else -> "${convertWholeNumber(cents)} سنتيم"
                }
            }
            else -> ""
        }
        
        // Centime representation (Algerian colloquial style)
        val centimeText = when (totalCentimes) {
            1L -> "سنتيم واحد"
            2L -> "سنتيمان"
            in 3..10 -> "${convertWholeNumber(totalCentimes)} سنتيمات"
            else -> "${convertWholeNumber(totalCentimes)} سنتيم"
        }
        
        return Pair(dinarText, centimeText)
    }

    private fun convertWholeNumber(n: Long): String {
        if (n == 0L) return "صفر"
        if (n == 1L) return "واحد"
        if (n == 2L) return "اثنان"

        val ones = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
        val teens = arrayOf("عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر")
        val tens = arrayOf("", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
        val hundreds = arrayOf("", "مائة", "مئتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")

        val parts = mutableListOf<String>()
        var remaining = n

        // Billions
        if (remaining >= 1_000_000_000) {
            val b = remaining / 1_000_000_000
            remaining %= 1_000_000_000
            parts.add(when {
                b == 1L -> "مليار"
                b == 2L -> "ملياران"
                b in 3..10 -> "${convertSmall(b)} مليارات"
                else -> "${convertSmall(b)} مليار"
            })
        }

        // Millions
        if (remaining >= 1_000_000) {
            val m = remaining / 1_000_000
            remaining %= 1_000_000
            parts.add(when {
                m == 1L -> "مليون"
                m == 2L -> "مليونان"
                m in 3..10 -> "${convertSmall(m)} ملايين"
                else -> "${convertSmall(m)} مليون"
            })
        }

        // Thousands
        if (remaining >= 1000) {
            val t = remaining / 1000
            remaining %= 1000
            parts.add(when {
                t == 1L -> "ألف"
                t == 2L -> "ألفان"
                t in 3..10 -> "${convertSmall(t)} آلاف"
                else -> "${convertSmall(t)} ألف"
            })
        }

        // Hundreds
        if (remaining >= 100) {
            val h = (remaining / 100).toInt()
            remaining %= 100
            parts.add(hundreds[h])
        }

        // Tens and ones
        if (remaining > 0) {
            if (remaining in 10..19) {
                parts.add(teens[(remaining - 10).toInt()])
            } else {
                val o = (remaining % 10).toInt()
                val t = (remaining / 10).toInt()
                if (o > 0 && t > 0) {
                    parts.add("${ones[o]} و${tens[t]}")
                } else if (t > 0) {
                    parts.add(tens[t])
                } else if (o > 0) {
                    parts.add(ones[o])
                }
            }
        }

        return parts.joinToString(" و")
    }

    private fun convertSmall(n: Long): String {
        val ones = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
        val teens = arrayOf("عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر")
        val tens = arrayOf("", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
        val hundreds = arrayOf("", "مائة", "مئتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")
        
        if (n == 0L) return "صفر"
        val parts = mutableListOf<String>()
        var r = n
        if (r >= 100) { parts.add(hundreds[(r/100).toInt()]); r %= 100 }
        if (r in 10..19) { parts.add(teens[(r-10).toInt()]); r = 0 }
        if (r > 0) {
            val o = (r % 10).toInt()
            val t = (r / 10).toInt()
            if (o > 0 && t > 0) parts.add("${ones[o]} و${tens[t]}")
            else if (t > 0) parts.add(tens[t])
            else if (o > 0) parts.add(ones[o])
        }
        return parts.joinToString(" و")
    }
}
