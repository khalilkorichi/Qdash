package com.qdash

import com.qdash.core.utils.PostalAmountSuggester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostalAmountSuggesterTest {

    @Test
    fun suggestGridAmounts_blankReturnsDefaultNinePopularAmounts() {
        val result = PostalAmountSuggester.suggestGridAmounts("")
        assertEquals(9, result.size)
        assertEquals(PostalAmountSuggester.DEFAULT_GRID_AMOUNTS, result)
    }

    @Test
    fun suggestGridAmounts_prefixTwoReturnsNineAmountsStartingWithTwo() {
        val result = PostalAmountSuggester.suggestGridAmounts("2")
        assertEquals(9, result.size)
        assertTrue(
            "Expected all suggested amounts to start with '2', got $result",
            result.all { it.toString().startsWith("2") }
        )
        assertTrue("Expected 2000 in suggestions", 2_000L in result)
        assertTrue("Expected 20000 in suggestions", 20_000L in result)
    }

    @Test
    fun suggestGridAmounts_multiDigitAndEasternNumeralsMatchPrefix() {
        val result52 = PostalAmountSuggester.suggestGridAmounts("52")
        assertEquals(9, result52.size)
        assertTrue(result52.all { it.toString().startsWith("52") })
        assertTrue(5_200L in result52)

        val resultEastern = PostalAmountSuggester.suggestGridAmounts("٢")
        assertEquals(PostalAmountSuggester.suggestGridAmounts("2"), resultEastern)
    }

    @Test
    fun formatGridAmount_formatsWithPostalDotSeparators() {
        assertEquals("1.000", PostalAmountSuggester.formatGridAmount(1_000L))
        assertEquals("20.000", PostalAmountSuggester.formatGridAmount(20_000L))
        assertEquals("200.000", PostalAmountSuggester.formatGridAmount(200_000L))
    }
}
