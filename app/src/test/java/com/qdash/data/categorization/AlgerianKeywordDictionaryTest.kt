package com.qdash.data.categorization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AlgerianKeywordDictionaryTest {

    private val matcher = KeywordMatcher()

    @Test
    fun `test Flexy keyword matches bills internet subcategory`() {
        val normalized = matcher.normalize("فليكسي موبيليس 500")
        val match = AlgerianKeywordDictionary.findMatchingCategory(normalized)
        assertNotNull(match)
        assertEquals("فواتير", match?.categoryName)
        assertEquals("إنترنت", match?.subcategoryName)
    }

    @Test
    fun `test Khodra keyword matches food grocery subcategory`() {
        val normalized = matcher.normalize("خضرة وفواكه من السوق")
        val match = AlgerianKeywordDictionary.findMatchingCategory(normalized)
        assertNotNull(match)
        assertEquals("طعام", match?.categoryName)
        assertEquals("بقالة", match?.subcategoryName)
    }

    @Test
    fun `test Transport keyword matches transport taxi subcategory`() {
        val normalized = matcher.normalize("حافلة المدينة")
        val match = AlgerianKeywordDictionary.findMatchingCategory(normalized)
        assertNotNull(match)
        assertEquals("مواصلات", match?.categoryName)
        assertEquals("تاكسي", match?.subcategoryName)
    }

    @Test
    fun `test Fast Food keyword matches food restaurants subcategory`() {
        val normalized = matcher.normalize("محاجب سخونين")
        val match = AlgerianKeywordDictionary.findMatchingCategory(normalized)
        assertNotNull(match)
        assertEquals("طعام", match?.categoryName)
        assertEquals("مطاعم", match?.subcategoryName)
    }

    @Test
    fun `test Hardware Droguerie keyword matches household hardware subcategory`() {
        val normalized = matcher.normalize("دروغري وخردوات")
        val match = AlgerianKeywordDictionary.findMatchingCategory(normalized)
        assertNotNull(match)
        assertEquals("منزلي", match?.categoryName)
        assertEquals("خردوات وعتاد", match?.subcategoryName)
    }

    @Test
    fun `test empty string returns null`() {
        val match = AlgerianKeywordDictionary.findMatchingCategory("")
        assertNull(match)
    }
}
