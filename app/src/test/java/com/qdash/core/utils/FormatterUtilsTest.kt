package com.qdash.core.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormatterUtilsTest {

    private var prevHideDecimals = true
    private var prevWesternNumerals = true

    @Before
    fun setUp() {
        prevHideDecimals = FormatterUtils.hideDecimals
        prevWesternNumerals = FormatterUtils.useWesternNumerals
        FormatterUtils.hideDecimals = true
        FormatterUtils.useWesternNumerals = true
    }

    @After
    fun tearDown() {
        FormatterUtils.hideDecimals = prevHideDecimals
        FormatterUtils.useWesternNumerals = prevWesternNumerals
    }

    @Test
    fun formatCurrency_defaultsToSingleDzdArabicUnit() {
        assertEquals("1,500 دج", FormatterUtils.formatCurrency(1500.0))
        assertEquals("1,500 دج", FormatterUtils.formatCurrency(1500.0, currencyCode = "DZD"))
        assertEquals("1,500 دج", FormatterUtils.formatCurrency(1500.0, currencyCode = "د.ج"))
    }

    @Test
    fun formatCurrency_stripsDuplicateCurrencyPrefixesSuchAsDzdOrDa() {
        val positionalDzd = FormatterUtils.formatCurrency(1500.0, "DZD")
        assertEquals("1,500 دج", positionalDzd)
        assertFalse(positionalDzd.contains("DZD"))

        val explicitPrefix = FormatterUtils.formatCurrency(1500.0, prefix = "DA", currencyCode = "د.ج")
        assertEquals("1,500 دج", explicitPrefix)
        assertFalse(explicitPrefix.contains("DA"))
    }

    @Test
    fun formatCurrency_preservesSignPrefixWithoutCurrencyDuplication() {
        val signed = FormatterUtils.formatCurrency(1500.0, prefix = "+", currencyCode = "DZD")
        assertTrue("Expected +1,500 دج in '$signed'", signed.contains("+1,500 دج"))
        assertFalse(signed.contains("DZD"))
    }

    @Test
    fun formatCurrency_formatsDecimalsWhenEnabledWithoutDzdDuplication() {
        FormatterUtils.hideDecimals = false
        val formatted = FormatterUtils.formatCurrency(1500.5, currencyCode = "DZD")
        assertEquals("1,500.50 دج", formatted)
        assertFalse(formatted.contains("DZD"))
    }

    @Test
    fun currencySymbol_normalizesAllDinarVariantsToDzdArabicAbbreviation() {
        listOf("DZD", "dzd", "DA", "د.ج", "دج", "").forEach { code ->
            assertEquals("دج", FormatterUtils.currencySymbol(code))
        }
    }
}
