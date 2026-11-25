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
    fun darkThemeRtl_checkAndChequeSecours_smart3x3AmountDropdown_unifiedDropdowns_andBottomBar() {
        val viewModel = createViewModel()
        var openedProfilesTab = false

        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 360.dp, height = 850.dp)) {
                        DocumentSimulatorScreen(
                            viewModel = viewModel,
                            onOpenPostalProfiles = { openedProfilesTab = true },
                            onBack = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        // 1. Removed top controls & cheat sheet & place/date/key/rip quick buttons
        assertTrue(composeRule.onAllNodesWithTag("doc_switcher_container").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("toolbar_review_button").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("btn_ready_amounts").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("btn_auto_calc_key").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("btn_paste_rip").fetchSemanticsNodes().isEmpty())

        // 2. Inline progress strip + missing hint directly below preview
        composeRule.onNodeWithTag("document_progress_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("document_progress_missing_text").assertIsDisplayed()

        // 3. Physical LTR lock on Check + bottom-right translucent rotate & guide buttons
        val payableBounds = composeRule.onNodeWithTag("cheque_payable_box").getBoundsInRoot()
        val amountBoxBounds = composeRule.onNodeWithTag("cheque_amount_box").getBoundsInRoot()
        assertTrue("Check must stay LTR in RTL", payableBounds.right.value <= amountBoxBounds.left.value + 1f)

        val rotateBtnBounds = composeRule.onNodeWithTag("cheque_rotate_expand_button").getBoundsInRoot()
        val guideBtnBounds = composeRule.onNodeWithTag("cheque_guide_button").getBoundsInRoot()
        assertTrue("Guide button must sit to the left of rotate button", guideBtnBounds.right.value <= rotateBtnBounds.left.value + 0.5f)
        assertTrue("Rotate button must be in right half", rotateBtnBounds.left.value > 180f)

        // 4. Smart 3x3 Amount Dropdown with live prefix filtering
        composeRule.onNodeWithTag("amount_dropdown_trigger").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("amount_suggestions_grid").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("amount_grid_item_1000").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("amount_grid_item_200000").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("amount_dropdown_input").performScrollTo().performTextInput("2")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("amount_grid_item_2000").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("amount_grid_item_20000").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("amount_grid_item_2000").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithText("ألفا دينار جزائري", substring = true).fetchSemanticsNodes().isNotEmpty())

        // 5. Scrolling up brings back the floating bottom bar, switching cleanly to "شاك سوكور"
        composeRule.onNodeWithTag("simulator_controls_scroll").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("simulator_bottom_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_tab_cheque_secours").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sfp01_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("sfp01_form_grid").assertIsDisplayed()

        val opTriggerBounds = composeRule.onNodeWithTag("sfp_operation_dropdown_trigger").performScrollTo().getBoundsInRoot()
        val motifTriggerBounds = composeRule.onNodeWithTag("sfp_motif_dropdown_trigger").performScrollTo().getBoundsInRoot()
        assertTrue(
            "Operation & Motif dropdowns must sit side-by-side in one row",
            abs(opTriggerBounds.top.value - motifTriggerBounds.top.value) <= 2f &&
                motifTriggerBounds.right.value <= opTriggerBounds.left.value + 1f
        )

        composeRule.onNodeWithTag("sfp_operation_dropdown_trigger").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sfp_unified_dropdown_panel").assertIsDisplayed()

        composeRule.onNodeWithTag("sfp_motif_dropdown_trigger").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sfp_unified_dropdown_panel").assertIsDisplayed()

        // 6. Fullscreen viewer opens and third bottom tab triggers Postal Profiles
        composeRule.onNodeWithTag("sfp01_expand_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sfp01_fullscreen_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("fullscreen_close_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("bottom_tab_postal_profiles").performClick()
        assertTrue(openedProfilesTab)
    }

    @Test
    fun fullscreenCheckViewer_floatingOppositeEditor_movesOppositeToTappedField() {
        val viewModel = createViewModel()

        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 360.dp, height = 850.dp)) {
                        DocumentSimulatorScreen(
                            viewModel = viewModel,
                            onOpenPostalProfiles = {},
                            onBack = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cheque_rotate_expand_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cheque_fullscreen_dialog").assertIsDisplayed()

        // Tap top-half Amount box inside fullscreen dialog -> floating editor appears in bottom half
        composeRule.onNode(
            hasTestTag("cheque_amount_box").and(hasAnyAncestor(hasTestTag("cheque_fullscreen_dialog")))
        ).performClick()
        composeRule.waitForIdle()
        val bottomEditorBounds = composeRule.onNodeWithTag("floating_opposite_editor").getBoundsInRoot()
        assertTrue("Top field editor should appear in bottom half: $bottomEditorBounds", bottomEditorBounds.top.value > 350f)

        // Tap lower-half Beneficiary line inside fullscreen dialog -> floating editor moves to top half
        composeRule.onNode(
            hasTestTag("cheque_beneficiary_line").and(hasAnyAncestor(hasTestTag("cheque_fullscreen_dialog")))
        ).performClick()
        composeRule.waitForIdle()
        val topEditorBounds = composeRule.onNodeWithTag("floating_opposite_editor").getBoundsInRoot()
        assertTrue(
            "Lower field editor should appear above top-field editor: top=$topEditorBounds bottom=$bottomEditorBounds",
            topEditorBounds.top.value < bottomEditorBounds.top.value
        )
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
