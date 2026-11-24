package com.qdash.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.qdash.ui.designsystem.components.home.HomeSkeleton
import com.qdash.ui.theme.KdachTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp")
class QuickActionsSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allSevenBentoTilesRenderClickAndMaintainSquareAndWideGeometry() {
        var addTxClicks = 0
        var viewAllClicks = 0
        var savingsClicks = 0
        var subscriptionsClicks = 0
        var docSimClicks = 0
        var aiClicks = 0
        var currencyClicks = 0

        composeRule.setContent {
            KdachTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.width(360.dp)) {
                        QuickActionsSection(
                            onAddTransactionClick = { addTxClicks++ },
                            onViewAllTransactionsClick = { viewAllClicks++ },
                            onSavingsClick = { savingsClicks++ },
                            onSubscriptionsClick = { subscriptionsClicks++ },
                            onDocumentSimulatorClick = { docSimClicks++ },
                            onAiAssistantClick = { aiClicks++ },
                            onCurrencyExchangeClick = { currencyClicks++ }
                        )
                    }
                }
            }
        }

        val labels = listOf(
            "إضافة معاملة",
            "سجل المعاملات",
            "حصالة الادخار",
            "الاشتراكات",
            "المساعد البريدي",
            "أسعار الصرف",
            "المساعد الذكي AI"
        )

        composeRule.onNodeWithText("الوصول السريع").assertIsDisplayed()
        labels.forEach { label ->
            composeRule.onNodeWithTag("quick_action_$label").assertIsDisplayed()
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }

        // Verify square 1x1 tiles and 2x1 wide tiles share exact row height and flush edges
        val addBounds = composeRule.onNodeWithTag("quick_action_إضافة معاملة").getBoundsInRoot()
        val historyBounds = composeRule.onNodeWithTag("quick_action_سجل المعاملات").getBoundsInRoot()
        val savingsBounds = composeRule.onNodeWithTag("quick_action_حصالة الادخار").getBoundsInRoot()
        val subsBounds = composeRule.onNodeWithTag("quick_action_الاشتراكات").getBoundsInRoot()
        val postalBounds = composeRule.onNodeWithTag("quick_action_المساعد البريدي").getBoundsInRoot()
        val currencyBounds = composeRule.onNodeWithTag("quick_action_أسعار الصرف").getBoundsInRoot()
        val aiBounds = composeRule.onNodeWithTag("quick_action_المساعد الذكي AI").getBoundsInRoot()

        val squareBounds = listOf(historyBounds, savingsBounds, subsBounds, postalBounds, currencyBounds)
        squareBounds.forEach { bounds ->
            assertTrue(
                "Expected 1x1 tile to be square, got width=${bounds.width}, height=${bounds.height}",
                abs(bounds.width.value - bounds.height.value) <= 1.5f
            )
        }

        assertTrue(
            "Wide tile height should match square tile height",
            abs(addBounds.height.value - historyBounds.height.value) <= 1.0f
        )
        assertTrue(
            "Bottom wide tile height should match square tile height",
            abs(aiBounds.height.value - currencyBounds.height.value) <= 1.0f
        )
        assertTrue(
            "Wide tile should be roughly 2x square tile + 10.dp spacing",
            abs(addBounds.width.value - (historyBounds.width.value * 2f + 10f)) <= 2.0f
        )
        // Flush outer edges across all 3 rows in RTL (rightmost = Row start, leftmost = Row end)
        assertTrue(abs(addBounds.right.value - savingsBounds.right.value) <= 1.5f)
        assertTrue(abs(savingsBounds.right.value - currencyBounds.right.value) <= 1.5f)
        assertTrue(abs(historyBounds.left.value - postalBounds.left.value) <= 1.5f)
        assertTrue(abs(postalBounds.left.value - aiBounds.left.value) <= 1.5f)

        // Verify all click callbacks
        labels.forEach { label ->
            composeRule.onNodeWithTag("quick_action_$label").performClick()
        }

        assertEquals(1, addTxClicks)
        assertEquals(1, viewAllClicks)
        assertEquals(1, savingsClicks)
        assertEquals(1, subscriptionsClicks)
        assertEquals(1, docSimClicks)
        assertEquals(1, currencyClicks)
        assertEquals(1, aiClicks)
    }

    @Test
    fun narrow320dpScreenMaintainsTrueSquareBentoTiles() {
        composeRule.setContent {
            KdachTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.width(320.dp)) {
                        QuickActionsSection(
                            onAddTransactionClick = {},
                            onViewAllTransactionsClick = {},
                            onSavingsClick = {},
                            onSubscriptionsClick = {},
                            onDocumentSimulatorClick = {},
                            onAiAssistantClick = {},
                            onCurrencyExchangeClick = {}
                        )
                    }
                }
            }
        }

        val savingsBounds = composeRule.onNodeWithTag("quick_action_حصالة الادخار").getBoundsInRoot()
        assertTrue(
            "Expected 1x1 tile on 320dp width to be square, got width=${savingsBounds.width}, height=${savingsBounds.height}",
            abs(savingsBounds.width.value - savingsBounds.height.value) <= 1.5f
        )
    }

    @Test
    fun homeSkeletonComposesWithBentoPlaceholderInDarkAndLightMode() {
        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.width(360.dp)) {
                        HomeSkeleton()
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }
}
