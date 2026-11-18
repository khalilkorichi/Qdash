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
        assertEquals("فليكسي ورصيد هاتف", match?.seededSubcategoryTarget)
    }

    @Test
    fun `test Khodra keyword matches food grocery subcategory`() {
        val normalized = matcher.normalize("خضرة وفواكه من السوق")
        val match = AlgerianKeywordDictionary.findMatchingCategory(normalized)
        assertNotNull(match)
        assertEquals("طعام", match?.categoryName)
        assertEquals("بقالة", match?.subcategoryName)
        assertEquals("مارشي وخضار", match?.seededSubcategoryTarget)
    }

    @Test
    fun `test Transport keyword matches transport taxi subcategory`() {
        val normalized = matcher.normalize("حافلة المدينة")
        val match = AlgerianKeywordDictionary.findMatchingCategory(normalized)
        assertNotNull(match)
        assertEquals("مواصلات", match?.categoryName)
        assertEquals("تاكسي", match?.subcategoryName)
        assertEquals("حافلة وترامواي", match?.seededSubcategoryTarget)
    }

    @Test
    fun `test Fast Food keyword matches food restaurants subcategory`() {
        val normalized = matcher.normalize("محاجب سخونين")
        val match = AlgerianKeywordDictionary.findMatchingCategory(normalized)
        assertNotNull(match)
        assertEquals("طعام", match?.categoryName)
        assertEquals("مطاعم", match?.subcategoryName)
        assertEquals("أكل سريع وبلاطو", match?.seededSubcategoryTarget)
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
    fun `test transactionType strictly separates rent expense vs rental income`() {
        val normRent = matcher.normalize("كرية المحل")
        val expenseMatch = AlgerianKeywordDictionary.findMatchingCategory(normRent, "EXPENSE")
        val incomeMatch = AlgerianKeywordDictionary.findMatchingCategory(normRent, "INCOME")

        assertNotNull(expenseMatch)
        assertEquals("EXPENSE", expenseMatch?.transactionType)
        assertEquals("منزلي وبناء", expenseMatch?.seededCategoryTarget)
        assertEquals("إيجار", expenseMatch?.seededSubcategoryTarget)

        assertNotNull(incomeMatch)
        assertEquals("INCOME", incomeMatch?.transactionType)
        assertEquals("إيجار واستثمار", incomeMatch?.seededCategoryTarget)
        assertEquals("مدخول كراء عقار أو محل", incomeMatch?.seededSubcategoryTarget)
    }

    @Test
    fun `test Algerian Darja income terms match exact income subcategories`() {
        val primeMatch = AlgerianKeywordDictionary.findMatchingCategory(
            matcher.normalize("بريم المردودية"),
            "INCOME"
        )
        assertEquals("راتب", primeMatch?.seededCategoryTarget)
        assertEquals("مردودية ومنح العمل (Prime / Rappel)", primeMatch?.seededSubcategoryTarget)

        val chomageMatch = AlgerianKeywordDictionary.findMatchingCategory(
            matcher.normalize("منحة البطالة"),
            "INCOME"
        )
        assertEquals("منح وتعويضات", chomageMatch?.seededCategoryTarget)
        assertEquals("منحة البطالة ومنحة جامعية", chomageMatch?.seededSubcategoryTarget)

        val ouedknissMatch = AlgerianKeywordDictionary.findMatchingCategory(
            matcher.normalize("بعت هاتف في واد كنيس"),
            "INCOME"
        )
        assertEquals("مبيعات", ouedknissMatch?.seededCategoryTarget)
        assertEquals("بيع مستعمل (Ouedkniss / Marketplace)", ouedknissMatch?.seededSubcategoryTarget)
    }

    @Test
    fun `test short token boundary prevents false positive substring match`() {
        val unrelated = matcher.normalize("أسماء صافية")
        val match = AlgerianKeywordDictionary.findMatchingCategory(unrelated, "EXPENSE")
        assertNull(match)
    }

    @Test
    fun `test empty string returns null`() {
        val match = AlgerianKeywordDictionary.findMatchingCategory("")
        assertNull(match)
    }
}
