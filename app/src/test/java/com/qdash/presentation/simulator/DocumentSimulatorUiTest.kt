package com.qdash.presentation.simulator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.domain.repository.PostalProfileRepository
import com.qdash.ui.theme.KdachTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h850dp")
class DocumentSimulatorUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun createViewModel(): DocumentSimulatorViewModel {
        val sampleProfile = PostalProfile(
            id = 1L,
            profileName = "حسابي الرئيسي",
            firstName = "خليل",
            lastName = "قريشي",
            fullName = "قريشي خليل",
            accountNumber = "42339121",
            accountKey = "98",
            address = "الجزائر الوسطى",
            city = "الجزائر",
            defaultRole = PostalProfileRole.SELF,
            isFavorite = true
        )
        val fakeRepo = object : PostalProfileRepository {
            override fun getAllProfiles(): Flow<List<PostalProfile>> = flowOf(listOf(sampleProfile))
            override fun getFavoriteProfiles(): Flow<List<PostalProfile>> = flowOf(listOf(sampleProfile))
            override fun getProfilesByRole(role: PostalProfileRole): Flow<List<PostalProfile>> = flowOf(listOf(sampleProfile))
            override suspend fun getProfileById(id: Long): PostalProfile? = sampleProfile
            override suspend fun insertProfile(profile: PostalProfile): Long = 1L
            override suspend fun updateProfile(profile: PostalProfile) {}
            override suspend fun deleteProfile(profile: PostalProfile) {}
            override suspend fun deleteProfileById(id: Long) {}
            override suspend fun getProfilesCount(): Int = 1
        }
        return DocumentSimulatorViewModel(fakeRepo)
    }

    @Test
    fun darkThemeRtl_checkAndSfp01_layoutQuickFillLanguageManualAndExpandDialog() {
        val viewModel = createViewModel()

        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 360.dp, height = 850.dp)) {
                        DocumentSimulatorScreen(
                            viewModel = viewModel,
                            onBack = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        // 1. Assert no overlapping toolbar buttons on 360dp width in RTL
        val switcherBounds = composeRule.onNodeWithTag("doc_switcher_container").getBoundsInRoot()
        val langBounds = composeRule.onNodeWithTag("toolbar_lang_toggle").getBoundsInRoot()
        val zoomBounds = composeRule.onNodeWithTag("toolbar_zoom_button").getBoundsInRoot()
        val guideBounds = composeRule.onNodeWithTag("toolbar_guide_button").getBoundsInRoot()
        val reviewBounds = composeRule.onNodeWithTag("toolbar_review_button").getBoundsInRoot()

        val toolbarControls = listOf(switcherBounds, langBounds, zoomBounds, guideBounds, reviewBounds)
        toolbarControls.forEach { bounds ->
            assertTrue("Expected positive toolbar item width, got ${bounds.width}", bounds.width.value > 0f)
            assertTrue("Control exceeds 360dp root bounds: $bounds", bounds.left.value >= 0f && bounds.right.value <= 360.5f)
        }
        // In RTL: switcher is on the right, trailing icons sit cleanly to its left: review -> guide -> zoom -> lang -> switcher
        assertTrue("Switcher overlaps lang toggle", langBounds.right.value <= switcherBounds.left.value + 0.5f)
        assertTrue("Lang toggle overlaps zoom", zoomBounds.right.value <= langBounds.left.value + 0.5f)
        assertTrue("Zoom overlaps guide", guideBounds.right.value <= zoomBounds.left.value + 0.5f)
        assertTrue("Guide overlaps review", reviewBounds.right.value <= guideBounds.left.value + 0.5f)

        // 2. Physical LTR layout lock on Postal Check even under RTL: Payable à (left) < Amount Box (right)
        val payableBounds = composeRule.onNodeWithTag("cheque_payable_box").getBoundsInRoot()
        val amountBoxBounds = composeRule.onNodeWithTag("cheque_amount_box").getBoundsInRoot()
        assertTrue(
            "Check must stay physically LTR in RTL: payable=$payableBounds, amount=$amountBoxBounds",
            payableBounds.right.value <= amountBoxBounds.left.value + 1f
        )

        // 3. Preset chip preset_amount_5000 updates live words WITHOUT opening manual inputs
        assertTrue(
            "Manual edit section should be hidden by default",
            composeRule.onAllNodesWithText("التعديل اليدوي الكامل").fetchSemanticsNodes().isEmpty()
        )

        composeRule.onNodeWithTag("preset_amount_5000").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(
            "Expected Arabic words for 5000 DA on screen",
            composeRule.onAllNodesWithText("خمسة آلاف دينار جزائري", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "Manual edit section must remain closed after preset click",
            composeRule.onAllNodesWithText("التعديل اليدوي الكامل").fetchSemanticsNodes().isEmpty()
        )

        // 4. toggle_words_lang switches Arabic -> French -> Arabic
        composeRule.onNodeWithTag("toggle_words_lang").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(
            "Expected French words for 5000 DA after toggle",
            composeRule.onAllNodesWithText("Cinq mille dinars", substring = true).fetchSemanticsNodes().isNotEmpty()
        )

        composeRule.onNodeWithTag("toggle_words_lang").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(
            "Expected Arabic words again after toggling back",
            composeRule.onAllNodesWithText("خمسة آلاف دينار جزائري", substring = true).fetchSemanticsNodes().isNotEmpty()
        )

        // 5. toggle_manual_edit reveals manual inputs
        composeRule.onNodeWithTag("toggle_manual_edit").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("manual_edit_section").performScrollTo().assertIsDisplayed()

        // 6. Switch to SFP 01, verify physical LTR lock and fullscreen expand dialog
        composeRule.onNodeWithTag("doc_switcher_sfp01").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sfp01_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("sfp01_form_grid").assertIsDisplayed()

        val sfpLeftHeader = composeRule.onNodeWithText("Opérations financières postales").getBoundsInRoot()
        val sfpRightHeader = composeRule.onNodeWithText("العمليات المالية البريدية").getBoundsInRoot()
        assertTrue(
            "SFP 01 must stay physically LTR in RTL: french=$sfpLeftHeader, arabic=$sfpRightHeader",
            sfpLeftHeader.right.value <= sfpRightHeader.left.value + 1f
        )

        composeRule.onNodeWithTag("sfp01_expand_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sfp01_fullscreen_dialog").assertIsDisplayed()
    }

    @Test
    fun compact320dpRtl_toolbarDoesNotOverlap_andEntryAndPanelsRenderCleanly() {
        val viewModel = createViewModel()

        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 320.dp, height = 720.dp)) {
                        DocumentSimulatorScreen(
                            viewModel = viewModel,
                            onBack = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val switcherBounds = composeRule.onNodeWithTag("doc_switcher_container").getBoundsInRoot()
        val langBounds = composeRule.onNodeWithTag("toolbar_lang_toggle").getBoundsInRoot()
        val zoomBounds = composeRule.onNodeWithTag("toolbar_zoom_button").getBoundsInRoot()
        val guideBounds = composeRule.onNodeWithTag("toolbar_guide_button").getBoundsInRoot()
        val reviewBounds = composeRule.onNodeWithTag("toolbar_review_button").getBoundsInRoot()

        listOf(switcherBounds, langBounds, zoomBounds, guideBounds, reviewBounds).forEach { bounds ->
            assertTrue("Control exceeds 320dp width: $bounds", bounds.left.value >= 0f && bounds.right.value <= 320.5f)
        }
        assertTrue("320dp: Switcher overlaps lang toggle", langBounds.right.value <= switcherBounds.left.value + 0.5f)
        assertTrue("320dp: Lang toggle overlaps zoom", zoomBounds.right.value <= langBounds.left.value + 0.5f)
        assertTrue("320dp: Zoom overlaps guide", guideBounds.right.value <= zoomBounds.left.value + 0.5f)
        assertTrue("320dp: Guide overlaps review", reviewBounds.right.value <= guideBounds.left.value + 0.5f)

        // Open Review checklist panel and verify errors card renders in Dark Theme RTL
        composeRule.onNodeWithTag("toolbar_review_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("validation_checklist_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("validation_errors_card").assertIsDisplayed()
    }
}
