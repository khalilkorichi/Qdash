package com.qdash

import com.qdash.core.utils.PostalCcpUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostalCcpUtilsTest {

    @Test
    fun calculateCcpKey_returnsOfficialTwoDigitKey() {
        assertEquals("98", PostalCcpUtils.calculateCcpKey("42339121"))
        assertEquals("98", PostalCcpUtils.calculateCcpKey("0042339121"))
        assertEquals("04", PostalCcpUtils.calculateCcpKey("1"))
        assertEquals("06", PostalCcpUtils.calculateCcpKey("100"))
        assertEquals("98", PostalCcpUtils.calculateCcpKey("٤٢٣٣٩١٢١"))
    }

    @Test
    fun calculateCcpKey_rejectsInvalidInputs() {
        assertNull(PostalCcpUtils.calculateCcpKey(""))
        assertNull(PostalCcpUtils.calculateCcpKey("   "))
        assertNull(PostalCcpUtils.calculateCcpKey("000000"))
        assertNull(PostalCcpUtils.calculateCcpKey("12345678901"))
    }

    @Test
    fun calculateRipKey_and_buildFullRip_matchOfficialVector() {
        assertEquals("75", PostalCcpUtils.calculateRipKey("42339121"))
        assertEquals("00799999004233912175", PostalCcpUtils.buildFullRip("42339121"))
        assertEquals("00799999004233912175", PostalCcpUtils.buildFullRip("0042339121"))
        assertNull(PostalCcpUtils.calculateRipKey(""))
        assertNull(PostalCcpUtils.buildFullRip("0000"))
        assertNull(PostalCcpUtils.buildFullRip("12345678901"))
    }

    @Test
    fun extractFromInput_parsesValidAndInvalidRip() {
        val valid = PostalCcpUtils.extractFromInput("00799999004233912175")
        assertNotNull(valid)
        assertEquals("42339121", valid!!.ccpNumber)
        assertEquals("98", valid.ccpKey)
        assertEquals("75", valid.ripKey)
        assertEquals("00799999004233912175", valid.fullRip)
        assertTrue(valid.isRipKeyValid)

        val invalidKeyRip = PostalCcpUtils.extractFromInput("00799999004233912199")
        assertNotNull(invalidKeyRip)
        assertEquals("42339121", invalidKeyRip!!.ccpNumber)
        assertEquals("98", invalidKeyRip.ccpKey)
        assertEquals("75", invalidKeyRip.ripKey)
        assertFalse(invalidKeyRip.isRipKeyValid)
    }

    @Test
    fun extractFromInput_parsesDelimitedPairsAndEasternNumerals() {
        val slash = PostalCcpUtils.extractFromInput("42339121/98")
        assertNotNull(slash)
        assertEquals("42339121", slash!!.ccpNumber)
        assertEquals("98", slash.ccpKey)
        assertTrue(slash.isRipKeyValid)

        val dash = PostalCcpUtils.extractFromInput("42339121-98")
        assertEquals("42339121", dash?.ccpNumber)

        val arabicDigits = PostalCcpUtils.extractFromInput("٤٢٣٣٩١٢١/٩٨")
        assertNotNull(arabicDigits)
        assertEquals("42339121", arabicDigits!!.ccpNumber)
        assertEquals("98", arabicDigits.ccpKey)

        assertNull(PostalCcpUtils.extractFromInput(""))
        assertNull(PostalCcpUtils.extractFromInput("abc-xyz"))
    }

    @Test
    fun isValidCcpKey_validatesExactTwoDigitKey() {
        assertTrue(PostalCcpUtils.isValidCcpKey("42339121", "98"))
        assertTrue(PostalCcpUtils.isValidCcpKey("42339121", "٩٨"))
        assertTrue(PostalCcpUtils.isValidCcpKey("1", "04"))
        assertFalse(PostalCcpUtils.isValidCcpKey("1", "4"))
        assertFalse(PostalCcpUtils.isValidCcpKey("42339121", "12"))
        assertFalse(PostalCcpUtils.isValidCcpKey("", "98"))
    }
}
