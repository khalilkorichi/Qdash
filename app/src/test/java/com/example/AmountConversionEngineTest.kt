package com.example

import com.example.domain.usecase.simulator.AmountConversionEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountConversionEngineTest {

    @Test
    fun testFormatAmountToPostal() {
        assertEquals("20.000,00", AmountConversionEngine.formatAmountToPostal(20000.0))
        assertEquals("1.000,00", AmountConversionEngine.formatAmountToPostal(1000.0))
        assertEquals("500,50", AmountConversionEngine.formatAmountToPostal(500.5))
        assertEquals("1.234.567,89", AmountConversionEngine.formatAmountToPostal(1234567.89))
    }

    @Test
    fun testConvertToArabicWords() {
        assertEquals("ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(1000.0))
        assertEquals("ألفا دينار جزائري", AmountConversionEngine.convertToArabicWords(2000.0))
        assertEquals("خمسة آلاف دينار جزائري", AmountConversionEngine.convertToArabicWords(5000.0))
        assertEquals("عشرة آلاف دينار جزائري", AmountConversionEngine.convertToArabicWords(10000.0))
        assertEquals("عشرون ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(20000.0))
        assertEquals("خمسون ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(50000.0))
        assertEquals("مائة ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(100000.0))
        assertEquals("مليون دينار جزائري", AmountConversionEngine.convertToArabicWords(1000000.0))
    }

    @Test
    fun testGetAlgerianColloquialWords() {
        assertEquals("مية ألف سنتيم (1.000 دج)", AmountConversionEngine.getAlgerianColloquialWords(1000.0))
        assertEquals("ميتين ألف سنتيم (2.000 دج)", AmountConversionEngine.getAlgerianColloquialWords(2000.0))
        assertEquals("مليون سنتيم (10.000 دج)", AmountConversionEngine.getAlgerianColloquialWords(10000.0))
        assertEquals("زوج ملايين سنتيم (20.000 دج)", AmountConversionEngine.getAlgerianColloquialWords(20000.0))
        assertEquals("خمس ملايين سنتيم (50.000 دج)", AmountConversionEngine.getAlgerianColloquialWords(50000.0))
        assertEquals("عشر ملايين سنتيم (100.000 دج)", AmountConversionEngine.getAlgerianColloquialWords(100000.0))
        assertEquals("مليار سنتيم (10.000.000 دج)", AmountConversionEngine.getAlgerianColloquialWords(10000000.0))
    }
}
