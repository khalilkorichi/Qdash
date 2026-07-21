package com.qdash.data.seeder

import com.qdash.ui.designsystem.tokens.CategoryIconRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgerianCategoriesSeederTest {

    @Test
    fun `test registered vector icon tokens validity`() {
        val marcheToken = CategoryIconRegistry.registeredIcons.find { it.key == "ic_cat_marche" }
        assertNotNull(marcheToken)
        assertTrue(marcheToken!!.isVectorDrawable)

        val flexyToken = CategoryIconRegistry.registeredIcons.find { it.key == "ic_cat_flexy" }
        assertNotNull(flexyToken)
        assertTrue(flexyToken!!.isVectorDrawable)

        val transportToken = CategoryIconRegistry.registeredIcons.find { it.key == "ic_cat_transport" }
        assertNotNull(transportToken)
        assertTrue(transportToken!!.isVectorDrawable)
    }

    @Test
    fun `test category icon registry returns valid drawable resource IDs`() {
        val marcheRes = CategoryIconRegistry.getDrawableResId("ic_cat_marche")
        assertNotNull(marcheRes)

        val flexyRes = CategoryIconRegistry.getDrawableResId("ic_cat_flexy")
        assertNotNull(flexyRes)

        val healthRes = CategoryIconRegistry.getDrawableResId("ic_cat_health")
        assertNotNull(healthRes)

        val defaultRes = CategoryIconRegistry.getDrawableResId("ic_cat_default")
        assertNotNull(defaultRes)

        val unknownFallbackRes = CategoryIconRegistry.getDrawableResId("unknown_custom_category")
        assertEquals(defaultRes, unknownFallbackRes)
    }
}
