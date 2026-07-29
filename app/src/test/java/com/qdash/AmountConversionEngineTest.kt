package com.qdash

import com.qdash.core.utils.AmountConversionEngine
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
    fun testConvertToArabicWords_OfficialDinars() {
        assertEquals("عشرة دنانير جزائرية", AmountConversionEngine.convertToArabicWords(10.0, preferCentimes = false))
        assertEquals("عشرون ديناراً جزائرياً", AmountConversionEngine.convertToArabicWords(20.0, preferCentimes = false))
        assertEquals("خمسون ديناراً جزائرياً", AmountConversionEngine.convertToArabicWords(50.0, preferCentimes = false))
        assertEquals("مائة دينار جزائري", AmountConversionEngine.convertToArabicWords(100.0, preferCentimes = false))
        assertEquals("مائتا دينار جزائري", AmountConversionEngine.convertToArabicWords(200.0, preferCentimes = false))
        assertEquals("خمسمائة دينار جزائري", AmountConversionEngine.convertToArabicWords(500.0, preferCentimes = false))
        assertEquals("ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(1000.0, preferCentimes = false))
        assertEquals("ألفا دينار جزائري", AmountConversionEngine.convertToArabicWords(2000.0, preferCentimes = false))
        assertEquals("عشرة آلاف دينار جزائري", AmountConversionEngine.convertToArabicWords(10000.0, preferCentimes = false))
        assertEquals("خمسة عشر ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(15000.0, preferCentimes = false))
        assertEquals("عشرون ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(20000.0, preferCentimes = false))
        assertEquals("خمسون ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(50000.0, preferCentimes = false))
        assertEquals("مائة ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(100000.0, preferCentimes = false))
        assertEquals("مائتا ألف دينار جزائري", AmountConversionEngine.convertToArabicWords(200000.0, preferCentimes = false))
        assertEquals("مليون دينار جزائري", AmountConversionEngine.convertToArabicWords(1000000.0, preferCentimes = false))
    }

    @Test
    fun testConvertToArabicWords_DefaultCentimes() {
        assertEquals("مية ألف سنتيم", AmountConversionEngine.convertToArabicWords(1000.0))
        assertEquals("زوج ملايين سنتيم", AmountConversionEngine.convertToArabicWords(20000.0))
    }

    @Test
    fun testGetAlgerianColloquialWords_UserTable() {
        assertEquals("ألف سنتيم", AmountConversionEngine.getAlgerianColloquialWords(10.0))
        assertEquals("ألفين سنتيم", AmountConversionEngine.getAlgerianColloquialWords(20.0))
        assertEquals("خمس لاف سنتيم", AmountConversionEngine.getAlgerianColloquialWords(50.0))
        assertEquals("عشر لاف سنتيم", AmountConversionEngine.getAlgerianColloquialWords(100.0))
        assertEquals("عشرين ألف سنتيم", AmountConversionEngine.getAlgerianColloquialWords(200.0))
        assertEquals("خمسين ألف سنتيم", AmountConversionEngine.getAlgerianColloquialWords(500.0))
        assertEquals("مية ألف سنتيم", AmountConversionEngine.getAlgerianColloquialWords(1000.0))
        assertEquals("ميتين ألف سنتيم", AmountConversionEngine.getAlgerianColloquialWords(2000.0))
        assertEquals("مليون سنتيم", AmountConversionEngine.getAlgerianColloquialWords(10000.0))
        assertEquals("زوج ملايين سنتيم", AmountConversionEngine.getAlgerianColloquialWords(20000.0))
        assertEquals("خمس ملايين سنتيم", AmountConversionEngine.getAlgerianColloquialWords(50000.0))
        assertEquals("عشر ملايين سنتيم", AmountConversionEngine.getAlgerianColloquialWords(100000.0))
        assertEquals("عشرين مليون سنتيم", AmountConversionEngine.getAlgerianColloquialWords(200000.0))
        assertEquals("مية مليون سنتيم", AmountConversionEngine.getAlgerianColloquialWords(1000000.0))
        assertEquals("مليار سنتيم", AmountConversionEngine.getAlgerianColloquialWords(10000000.0))
    }

    @Test
    fun testCentimesConversion() {
        // 1.000 Centimes = 10 DZD -> "ألف سنتيم"
        assertEquals("ألف سنتيم", AmountConversionEngine.convertCentimesToColloquialWords(1000L))
        // 100.000 Centimes = 1.000 DZD -> "ألف دينار جزائري"
        assertEquals("ألف دينار جزائري", AmountConversionEngine.convertCentimesToArabicWords(100000L))
        // 200.000 Centimes = 2.000 DZD -> "ألفا دينار جزائري"
        assertEquals("ألفا دينار جزائري", AmountConversionEngine.convertCentimesToArabicWords(200000L))
        // 2.000.000 Centimes = 20.000 DZD -> "عشرون ألف دينار جزائري"
        assertEquals("عشرون ألف دينار جزائري", AmountConversionEngine.convertCentimesToArabicWords(2000000L))
        assertEquals("زوج ملايين سنتيم", AmountConversionEngine.convertCentimesToColloquialWords(2000000L))
    }
}


