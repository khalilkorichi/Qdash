package com.qdash.presentation.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.AccountType
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.model.CategoryType
import com.qdash.domain.model.SuggestionSource
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.analytics.CategoryShare
import com.qdash.presentation.analytics.components.CategoryColorPickerDialog
import com.qdash.presentation.transactions.components.UnifiedCategorySelectorCard
import com.qdash.ui.theme.KdachTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h850dp")
class ButtonOverlapAndCurrencyUiTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun categorySuggestionCardButtonsDoNotOverlapInRtl() {
        val parentCat = Category(
            id = 1L,
            name = "عائلي",
            type = CategoryType.EXPENSE,
            icon = "family",
            color = "#2E7D32"
        )
        val subCat = Category(
            id = 2L,
            name = "بقالة",
            type = CategoryType.EXPENSE,
            icon = "shopping_cart",
            color = "#2E7D32",
            parentId = 1L
        )
        val suggestion = CategorySuggestion(
            suggestedCategoryId = 2L,
            suggestionSource = SuggestionSource.HISTORY,
            confidenceScore = 0.94f
        )

        composeRule.setContent {
            KdachTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 400.dp, height = 850.dp)) {
                        UnifiedCategorySelectorCard(
                            categories = listOf(parentCat, subCat),
                            transactions = emptyList(),
                            type = TransactionType.EXPENSE,
                            selectedCategoryId = null,
                            subcategoryId = null,
                            typeAccentColor = Color(0xFFE53935),
                            smartSortEnabled = false,
                            suggestion = suggestion,
                            matchedCategory = subCat,
                            isSuggestingCategory = false,
                            onToggleSmartSort = {},
                            onCategorySelected = { _, _ -> },
                            onAddMainCategory = {},
                            onAddSubCategory = {},
                            onAiSuggest = {},
                            onAcceptSuggestion = {},
                            onAcceptAndCreateSuggestion = {},
                            onDismissSuggestion = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val acceptBounds = composeRule.onNodeWithTag("accept_suggestion_button").getBoundsInRoot()
        val dismissBounds = composeRule.onNodeWithTag("dismiss_suggestion_button").getBoundsInRoot()

        assertTrue(
            "Expected 32dp dismiss circle button, got ${dismissBounds.width}x${dismissBounds.height}",
            abs(dismissBounds.width.value - 32f) <= 0.5f && abs(dismissBounds.height.value - 32f) <= 0.5f
        )

        // In RTL, "تطبيق" (first in Row) is to the right of "X" (second in Row)
        val horizontalGapDp = acceptBounds.left.value - dismissBounds.right.value
        assertTrue(
            "Suggestion accept and dismiss buttons must have >= 7.5dp spacing; got ${horizontalGapDp}dp",
            horizontalGapDp >= 7.5f
        )
    }

    @Test
    fun transactionDateSelectorClearButtonFitsInsideAndClearsTimeInRtl() {
        var latestOccurredAt: Long? = 1795550400000L

        composeRule.setContent {
            KdachTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var occurredAt by remember { mutableStateOf<Long?>(1795550400000L) }
                    Box(modifier = Modifier.size(width = 400.dp, height = 300.dp)) {
                        TransactionDateSelector(
                            transactionDate = 1795550400000L,
                            onDateChanged = {},
                            typeAccentColor = Color(0xFFE53935),
                            occurredAt = occurredAt,
                            onOccurredAtChanged = { updated ->
                                occurredAt = updated
                                latestOccurredAt = updated
                            }
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val timeBounds = composeRule.onNodeWithTag("time_selector").getBoundsInRoot()
        val clearBounds = composeRule.onNodeWithTag("clear_time_button").getBoundsInRoot()

        assertTrue(
            "Expected 28dp clear_time_button circle, got ${clearBounds.width}x${clearBounds.height}",
            abs(clearBounds.width.value - 28f) <= 0.5f && abs(clearBounds.height.value - 28f) <= 0.5f
        )
        assertTrue(
            "clear_time_button should stay inside time_selector horizontal bounds",
            clearBounds.left.value >= timeBounds.left.value - 0.5f &&
                clearBounds.right.value <= timeBounds.right.value + 0.5f
        )

        composeRule.onNodeWithTag("clear_time_button").performClick()
        composeRule.waitForIdle()
        assertNull(latestOccurredAt)
    }

    @Test
    fun categoryDetailsModalDisplaysSingleDzdSuffixAndNonOverlappingCirclesInRtl() {
        val parentCat = Category(
            id = 1L,
            name = "عائلي",
            type = CategoryType.EXPENSE,
            icon = "family",
            color = "#2E7D32"
        )
        val subCat = Category(
            id = 2L,
            name = "بقالة",
            type = CategoryType.EXPENSE,
            icon = "shopping_cart",
            color = "#2E7D32",
            parentId = 1L
        )
        val account = Account(
            id = 10L,
            name = "كاش",
            type = AccountType.CASH,
            balance = 5000.0,
            currency = "DZD",
            color = "#4CAF50",
            icon = "wallet"
        )
        val txs = listOf(
            Transaction(
                id = 100L,
                amount = 1500.0,
                type = TransactionType.EXPENSE,
                categoryId = 2L,
                accountId = 10L,
                note = "مصاريف منزلية",
                date = 1795550400000L,
                occurredAt = 1795550400000L
            ),
            Transaction(
                id = 101L,
                amount = 500.0,
                type = TransactionType.EXPENSE,
                categoryId = 2L,
                accountId = 10L,
                note = "خبز",
                date = 1795540400000L,
                occurredAt = 1795540400000L
            )
        )
        val share = CategoryShare(
            categoryId = 2L,
            categoryName = "بقالة",
            amount = 2000.0,
            percentage = 100f,
            color = "#2E7D32",
            transactionCount = 2
        )

        composeRule.setContent {
            KdachTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 400.dp, height = 850.dp)) {
                        CategoryColorPickerDialog(
                            category = share,
                            categoryTxs = txs,
                            allCategories = listOf(parentCat, subCat),
                            accounts = listOf(account),
                            periodLabel = "الكل",
                            onDismiss = {},
                            onColorChange = { _, _ -> },
                            onMoveCategory = { _, _ -> }
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val formattedNodes = composeRule.onAllNodesWithText("1,500 دج").fetchSemanticsNodes()
        assertTrue("Expected formatted transaction amount '1,500 دج' to be shown", formattedNodes.isNotEmpty())
        composeRule.onAllNodesWithText("DZD", substring = true).assertCountEquals(0)

        val moveBounds = composeRule.onNodeWithContentDescription("نقل وتنظيم الفئة").getBoundsInRoot()
        val colorBounds = composeRule.onNodeWithContentDescription("تعديل اللون").getBoundsInRoot()
        val closeBounds = composeRule.onNodeWithContentDescription("إغلاق").getBoundsInRoot()

        assertTrue(
            "Expected 34dp header action circles, got ${moveBounds.width}x${moveBounds.height}",
            abs(moveBounds.width.value - 34f) <= 0.5f &&
                abs(colorBounds.width.value - 34f) <= 0.5f &&
                abs(closeBounds.width.value - 34f) <= 0.5f
        )

        // In RTL: move -> color -> close from right to left
        val gapMoveToColor = moveBounds.left.value - colorBounds.right.value
        val gapColorToClose = colorBounds.left.value - closeBounds.right.value
        assertEquals(8f, gapMoveToColor, 0.75f)
        assertEquals(8f, gapColorToClose, 0.75f)
    }
}
