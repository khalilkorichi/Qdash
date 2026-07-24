package com.qdash.data

import com.qdash.data.ai.providers.parseGeminiModelConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiProviderTest {

    @Test
    fun testParseGeminiModelConfig_High() {
        val (model, budget) = parseGeminiModelConfig("gemini-3.6-flash-high")
        assertEquals("gemini-3.6-flash", model)
        assertEquals(8192, budget)
    }

    @Test
    fun testParseGeminiModelConfig_Medium() {
        val (model, budget) = parseGeminiModelConfig("gemini-3.6-flash-medium")
        assertEquals("gemini-3.6-flash", model)
        assertEquals(4096, budget)
    }

    @Test
    fun testParseGeminiModelConfig_Low() {
        val (model, budget) = parseGeminiModelConfig("gemini-3.6-flash-low")
        assertEquals("gemini-3.6-flash", model)
        assertEquals(1024, budget)
    }

    @Test
    fun testParseGeminiModelConfig_Lite() {
        val (model, budget) = parseGeminiModelConfig("gemini-3.5-flash-lite")
        assertEquals("gemini-3.5-flash-lite", model)
        assertNull(budget)
    }
}
