package com.qdash.presentation.simulator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.domain.repository.PostalProfileRepository
import com.qdash.ui.theme.KdachTheme
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
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
    fun entryScreen_bentoGridLayout_rendersWideRectangleAndTwoSideBySideSquares() {
        val viewModel = createViewModel()
        var selectedDoc: DocumentType? = null
        var managedProfiles = false

        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 360.dp, height = 800.dp)) {
                        DocumentSimulatorEntryScreen(
                            viewModel = viewModel,
                            onSelectDocType = { selectedDoc = it },
                            onManageProfiles = { managedProfiles = true },
                            onBack = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val profilesTile = composeRule.onNodeWithTag("entry_bento_profiles").getBoundsInRoot()
        val chequeTile = composeRule.onNodeWithTag("entry_bento_cheque").getBoundsInRoot()
        val secoursTile = composeRule.onNodeWithTag("entry_bento_cheque_secours").getBoundsInRoot()

        assertTrue("Wide tile must be wider than square tiles", profilesTile.width.value > chequeTile.width.value * 1.5f)
        assertTrue("Square tiles must match width", abs(chequeTile.width.value - secoursTile.width.value) <= 1f)
        assertTrue("Square tiles must be approximately square", abs(chequeTile.width.value - chequeTile.height.value) <= 2f)
        assertTrue("Cheque & Secours must sit side-by-side", secoursTile.right.value <= chequeTile.left.value + 1f)

        composeRule.onNodeWithText("شاك سوكور").assertIsDisplayed().performClick()
        assertEquals(DocumentType.SFP01, selectedDoc)

        composeRule.onNodeWithTag("entry_bento_profiles").performClick()
        assertTrue(managedProfiles)
    }

    @Test
    fun darkThemeRtl_checkAndChequeSecours_popups_unifiedIdentity_compactGuide_andSwipePager() {
        val viewModel = createViewModel()

        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 360.dp, height = 850.dp)) {
                        DocumentSimulatorScreen(
                            viewModel = viewModel,
                            onAddProfile = {},
                            onEditProfile = {},
                            onBack = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        // 1. Removed scattered controls & cheat sheet & chequeNumber editable input
        assertTrue(composeRule.onAllNodesWithTag("doc_switcher_container").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("toolbar_review_button").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("btn_ready_amounts").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("btn_auto_calc_key").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("btn_paste_rip").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("btn_pull_my_account").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("chip_moi_meme").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("3245101", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("XXXX", substring = true).fetchSemanticsNodes().isNotEmpty())

        // 2. Inline progress strip + missing hint directly below preview
        composeRule.onNodeWithTag("cheque_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("document_progress_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("document_progress_missing_text").assertIsDisplayed()

        // 3. Physical LTR lock on Check + bottom-right translucent rotate & compact guide toggle
        val payableBounds = composeRule.onNodeWithTag("cheque_payable_box").getBoundsInRoot()
        val amountBoxBounds = composeRule.onNodeWithTag("cheque_amount_box").getBoundsInRoot()
        assertTrue("Check must stay LTR in RTL", payableBounds.right.value <= amountBoxBounds.left.value + 1f)

        val rotateBtnBounds = composeRule.onNodeWithTag("cheque_rotate_expand_button").getBoundsInRoot()
        val guideBtnBounds = composeRule.onNodeWithTag("cheque_guide_button").getBoundsInRoot()
        assertTrue("Guide button must sit to the left of rotate button", guideBtnBounds.right.value <= rotateBtnBounds.left.value + 0.5f)
        assertTrue("Rotate button must be in right half", rotateBtnBounds.left.value > 180f)

        composeRule.onNodeWithTag("cheque_guide_button").performClick()
        composeRule.waitForIdle()
        val guideStartBounds = composeRule.onNodeWithTag("educational_guide_panel").getBoundsInRoot()
        assertTrue("Guide start strip should be vertically compact: $guideStartBounds", guideStartBounds.height.value <= 120f)
        composeRule.onNodeWithTag("btn_start_guide").performClick()
        composeRule.waitForIdle()
        val guideActiveBounds = composeRule.onNodeWithTag("educational_guide_panel").getBoundsInRoot()
        assertTrue("Active guide card should stay compact: $guideActiveBounds", guideActiveBounds.height.value <= 165f)
        composeRule.onNodeWithTag("btn_stop_guide").performClick()
        composeRule.waitForIdle()

        // 4. Single Unified Account/Beneficiary/Moi-même selector popup
        composeRule.onNodeWithTag("unified_identity_trigger").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("unified_identity_popup").assertIsDisplayed()
        composeRule.onNodeWithTag("unified_action_my_ccp").assertIsDisplayed()
        composeRule.onNodeWithTag("unified_action_moi_meme").assertIsDisplayed()
        composeRule.onNodeWithTag("unified_profile_item_1").performClick()
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithText("42339121", substring = true).fetchSemanticsNodes().isNotEmpty())

        // 5. Smart 3x3 Amount Dropdown opens as a Popup Dialog with live prefix filtering
        composeRule.onNodeWithTag("amount_dropdown_trigger").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNode(
            hasTestTag("amount_dropdown_panel").and(hasAnyAncestor(isDialog()))
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("amount_suggestions_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("amount_grid_item_1000").assertIsDisplayed()
        composeRule.onNodeWithTag("amount_grid_item_200000").assertIsDisplayed()

        composeRule.onNodeWithTag("amount_dropdown_input").performTextInput("2")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("amount_grid_item_2000").assertIsDisplayed()
        composeRule.onNodeWithTag("amount_grid_item_20000").assertIsDisplayed()
        composeRule.onNodeWithTag("amount_grid_item_2000").performClick()
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithText("ألفا دينار جزائري", substring = true).fetchSemanticsNodes().isNotEmpty())

        // 6. Horizontal swipe transitions smoothly from Check -> Chèque Secours -> Postal Profiles
        composeRule.onNodeWithTag("simulator_controls_scroll").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("simulator_bottom_bar").assertIsDisplayed()

        composeRule.onNodeWithTag("simulator_horizontal_pager").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sfp01_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("sfp01_form_grid").assertIsDisplayed()

        // Operation/Services and Motif side-by-side triggers open Popup Dialogs
        val opTriggerBounds = composeRule.onNodeWithTag("sfp_operation_dropdown_trigger").performScrollTo().getBoundsInRoot()
        val motifTriggerBounds = composeRule.onNodeWithTag("sfp_motif_dropdown_trigger").performScrollTo().getBoundsInRoot()
        assertTrue(
            "Operation & Motif dropdowns must sit side-by-side in one row",
            abs(opTriggerBounds.top.value - motifTriggerBounds.top.value) <= 2f &&
                motifTriggerBounds.right.value <= opTriggerBounds.left.value + 1f
        )

        composeRule.onNodeWithTag("sfp_operation_dropdown_trigger").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(
            hasTestTag("sfp_unified_dropdown_panel").and(hasAnyAncestor(isDialog()))
        ).assertIsDisplayed()
        composeRule.onNodeWithText("حفظ وإغلاق").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sfp_motif_dropdown_trigger").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(
            hasTestTag("sfp_unified_dropdown_panel").and(hasAnyAncestor(isDialog()))
        ).assertIsDisplayed()
        composeRule.onNodeWithText("حفظ وإغلاق").performClick()
        composeRule.waitForIdle()

        // Swipe once more to Page 2 (Postal Profiles)
        composeRule.onNodeWithTag("simulator_horizontal_pager").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("postal_profiles_list").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_add_postal_profile").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w720dp-h380dp")
    fun fullscreenLandscapeViewers_fillWholeLandscapeScreen_andMoveEditorOpposite() {
        val viewModel = createViewModel()

        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 720.dp, height = 380.dp)) {
                        DocumentSimulatorScreen(
                            viewModel = viewModel,
                            onAddProfile = {},
                            onEditProfile = {},
                            onBack = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cheque_rotate_expand_button").performClick()
        composeRule.waitForIdle()
        // Must fill entire rotated landscape screen without portrait pillarbox clipping
        val checkFullscreenBounds = composeRule.onNodeWithTag("cheque_fullscreen_dialog").getBoundsInRoot()
        assertTrue("Fullscreen Check must fill landscape width: $checkFullscreenBounds", checkFullscreenBounds.width.value >= 700f)
        assertTrue("Fullscreen Check must fill landscape height: $checkFullscreenBounds", checkFullscreenBounds.height.value >= 360f)

        val checkCardBounds = composeRule.onNodeWithTag("cheque_preview_card").getBoundsInRoot()
        assertTrue("Check preview inside landscape fullscreen must expand wide: $checkCardBounds", checkCardBounds.width.value >= 560f)
        assertTrue("Check card must fit vertically without clipping top: $checkCardBounds", checkCardBounds.top.value >= 0f && checkCardBounds.bottom.value <= 380f)

        // Pre-printed XXXX number is read-only (tapping it must not open any editor)
        composeRule.onNodeWithTag("cheque_number_field").performClick()
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithTag("floating_opposite_editor").fetchSemanticsNodes().isEmpty())

        // Tap top-half Amount box -> floating editor appears in bottom half
        composeRule.onNodeWithTag("cheque_amount_box").performClick()
        composeRule.waitForIdle()
        val bottomEditorBounds = composeRule.onNodeWithTag("floating_opposite_editor").getBoundsInRoot()
        assertTrue("Top field editor should appear in bottom half: $bottomEditorBounds", bottomEditorBounds.top.value > 150f)

        // Tap lower-half Beneficiary line -> floating editor moves to top half
        composeRule.onNodeWithTag("cheque_beneficiary_line").performClick()
        composeRule.waitForIdle()
        val topEditorBounds = composeRule.onNodeWithTag("floating_opposite_editor").getBoundsInRoot()
        assertTrue(
            "Lower field editor should appear above top-field editor: top=$topEditorBounds bottom=$bottomEditorBounds",
            topEditorBounds.top.value < bottomEditorBounds.top.value
        )

        // Close Check fullscreen, switch to Chèque Secours, and verify landscape fullscreen SFP 01 width
        composeRule.onNodeWithTag("fullscreen_close_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_tab_cheque_secours").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sfp01_expand_button").performClick()
        composeRule.waitForIdle()

        val sfpFullscreenBounds = composeRule.onNodeWithTag("sfp01_fullscreen_dialog").getBoundsInRoot()
        assertTrue("Fullscreen SFP01 must fill landscape width: $sfpFullscreenBounds", sfpFullscreenBounds.width.value >= 700f)
        val sfpFormBounds = composeRule.onNodeWithTag("sfp01_form_grid").getBoundsInRoot()
        assertTrue("SFP01 form in landscape fullscreen must expand wide: $sfpFormBounds", sfpFormBounds.width.value >= 600f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w320dp-h720dp")
    fun compact320dp_bottomBarAndProfilesScreenRenderWithoutOverlap() {
        val viewModel = createViewModel()

        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 320.dp, height = 720.dp)) {
                        PostalProfilesScreen(
                            viewModel = viewModel,
                            onAddProfile = {},
                            onEditProfile = {},
                            onSelectDocType = {},
                            onBack = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        val tabCheque = composeRule.onNodeWithTag("bottom_tab_cheque").getBoundsInRoot()
        val tabSecours = composeRule.onNodeWithTag("bottom_tab_cheque_secours").getBoundsInRoot()
        val tabProfiles = composeRule.onNodeWithTag("bottom_tab_postal_profiles").getBoundsInRoot()

        assertTrue(tabProfiles.left.value >= 0f && tabCheque.right.value <= 320.5f)
        assertTrue("Bottom tabs must not overlap in 320dp RTL", tabSecours.right.value <= tabCheque.left.value + 1f)
        assertTrue("Bottom tabs must not overlap in 320dp RTL", tabProfiles.right.value <= tabSecours.left.value + 1f)
        composeRule.onNodeWithTag("postal_profiles_list").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_add_postal_profile").assertIsDisplayed()
    }
}
