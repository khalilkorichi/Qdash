package com.qdash.presentation.simulator

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.PostalCcpUtils
import com.qdash.presentation.simulator.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/**
 * Interactive 3-page swipeable Postal Simulator hosting:
 * - Page 0: Postal Check ("الصك البريدي")
 * - Page 1: Chèque Secours ("شاك سوكور" / SFP 01)
 * - Page 2: Postal Accounts Manager ("الحساب البريدي")
 * Keeps preview locked to top, progress + hint strip beneath, compact collapsible guide,
 * unified popup quick-fill controls, Qdash-style bottom bar, and full-bleed rotated viewer.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentSimulatorScreen(
    viewModel: DocumentSimulatorViewModel,
    initialTab: SimulatorBottomTab = SimulatorBottomTab.CHEQUE,
    onOpenPostalProfiles: () -> Unit = {},
    onAddProfile: () -> Unit = {},
    onEditProfile: (Long) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = initialTab.ordinal.coerceIn(0, 2),
        pageCount = { 3 }
    )

    var isChequeZoomed by remember { mutableStateOf(false) }
    var isSfpZoomed by remember { mutableStateOf(false) }

    // Scroll-responsive bottom bar visibility matching AppShell
    var scrollDeltaAccumulator by remember { mutableFloatStateOf(0f) }
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f) {
                    scrollDeltaAccumulator += delta
                    if (scrollDeltaAccumulator < -100f) {
                        isBottomBarVisible = false
                    }
                } else if (delta > 10f) {
                    isBottomBarVisible = true
                    scrollDeltaAccumulator = 0f
                }
                return Offset.Zero
            }
        }
    }

    // Sync active pager page with ViewModel document state
    LaunchedEffect(pagerState.currentPage) {
        isBottomBarVisible = true
        scrollDeltaAccumulator = 0f
        when (pagerState.currentPage) {
            0 -> if (uiState.selectedDocType != DocumentType.CHEQUE) {
                viewModel.selectDocumentType(DocumentType.CHEQUE)
            }
            1 -> if (uiState.selectedDocType != DocumentType.SFP01) {
                viewModel.selectDocumentType(DocumentType.SFP01)
            }
            2 -> onOpenPostalProfiles()
        }
    }

    LaunchedEffect(uiState.quickFillFeedback) {
        val feedback = uiState.quickFillFeedback
        if (!feedback.isNullOrBlank()) {
            Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
            viewModel.clearQuickFillFeedback()
        }
    }

    val chequeAmountInWords = remember(uiState.chequeAmount, uiState.chequeWordsLanguage) {
        val amt = AmountConversionEngine.parseFlexibleAmount(uiState.chequeAmount)
        if (amt != null && amt > 0.0) {
            if (uiState.chequeWordsLanguage == WordsLanguage.FRENCH) {
                AmountConversionEngine.convertToFrenchWords(amt)
            } else {
                AmountConversionEngine.convertToArabicWords(amt, preferCentimes = false)
            }
        } else null
    }

    val sfpAmountInWords = remember(uiState.sfpAmount, uiState.sfpWordsLanguage) {
        val amt = AmountConversionEngine.parseFlexibleAmount(uiState.sfpAmount)
        if (amt != null && amt > 0.0) {
            if (uiState.sfpWordsLanguage == WordsLanguage.FRENCH) {
                AmountConversionEngine.convertToFrenchWords(amt)
            } else {
                AmountConversionEngine.convertToArabicWords(amt, preferCentimes = false)
            }
        } else null
    }

    val activeTab = SimulatorBottomTab.entries[pagerState.currentPage.coerceIn(0, 2)]
    val topBarTitle = when (activeTab) {
        SimulatorBottomTab.CHEQUE -> "الصك البريدي"
        SimulatorBottomTab.CHEQUE_SECOURS -> "شاك سوكور"
        SimulatorBottomTab.POSTAL_PROFILES -> "الحسابات البريدية"
    }
    val isActivePreviewZoomed = (activeTab == SimulatorBottomTab.CHEQUE && isChequeZoomed) ||
        (activeTab == SimulatorBottomTab.CHEQUE_SECOURS && isSfpZoomed)

    val bottomPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) 96.dp else 20.dp,
        animationSpec = tween(220),
        label = "simulator_bottom_padding"
    )
    val profilesListBottomPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) 164.dp else 88.dp,
        animationSpec = tween(220),
        label = "simulator_profiles_list_padding"
    )
    val profilesFabBottomPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) 92.dp else 16.dp,
        animationSpec = tween(220),
        label = "simulator_profiles_fab_padding"
    )

    if (uiState.isFullscreenViewerOpen) {
        val isFullscreenCheque = uiState.selectedDocType == DocumentType.CHEQUE
        FullscreenDocumentEditorOverlay(
            uiState = uiState,
            amountInWords = if (isFullscreenCheque) chequeAmountInWords else sfpAmountInWords,
            onDismiss = { viewModel.setFullscreenViewerOpen(false) },
            onToggleWordsLanguage = { viewModel.toggleWordsLanguage() },
            onFieldFocus = { viewModel.setFocusedField(it) },
            onSelectProfile = { profile, role -> viewModel.autofillFromProfile(profile, role) },
            onChequeAmountChange = { viewModel.updateChequeAmount(it) },
            onChequeBeneficiaryChange = { viewModel.updateChequeBeneficiary(it) },
            onChequeCcpChange = { viewModel.updateChequeCcp(it) },
            onChequeKeyChange = { viewModel.updateChequeKey(it) },
            onChequePlaceChange = { viewModel.updateChequePlace(it) },
            onChequeDateChange = { viewModel.updateChequeDate(it) },
            onChequeHolderChange = { viewModel.updateChequeAccountHolder(it) },
            onChequeMoiMemeClick = { viewModel.setChequeBeneficiaryMoiMeme() },
            onSfpCcpChange = { viewModel.updateSfpCcp(it) },
            onSfpKeyChange = { viewModel.updateSfpKey(it) },
            onSfpAmountChange = { viewModel.updateSfpAmount(it) },
            onSfpOperationChange = { viewModel.updateSfpOperation(it) },
            onSfpReleveOperationsChange = { viewModel.updateSfpReleveOperations(it) },
            onSfpAvecSoldeChange = { viewModel.updateSfpAvecSolde(it) },
            onSfpNotificationAvoirChange = { viewModel.updateSfpNotificationAvoir(it) },
            onSfpCommandeCarnetChange = { viewModel.updateSfpCommandeCarnet(it) },
            onSfpReleveDuChange = { viewModel.updateSfpReleveDu(it) },
            onSfpReleveAuChange = { viewModel.updateSfpReleveAu(it) },
            onSfpSenderNomChange = { viewModel.updateSfpSenderNom(it) },
            onSfpSenderPrenomChange = { viewModel.updateSfpSenderPrenom(it) },
            onSfpSenderAddressChange = { viewModel.updateSfpSenderAddress(it) },
            onSfpBeneficiaryNomChange = { viewModel.updateSfpBeneficiaryNom(it) },
            onSfpBeneficiaryPrenomChange = { viewModel.updateSfpBeneficiaryPrenom(it) },
            onSfpBeneficiaryAddressChange = { viewModel.updateSfpBeneficiaryAddress(it) },
            onSfpMotifChange = { viewModel.updateSfpMotif(it) },
            onSfpPlaceChange = { viewModel.updateSfpPlace(it) },
            onSfpDateChange = { viewModel.updateSfpDate(it) },
            onSfpIdDescriptionChange = { viewModel.updateSfpIdDescription(it) }
        )
    } else {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            topBar = {
                FinTrackTopBar(
                    title = topBarTitle,
                    showBackButton = true,
                    onBackClick = onBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isActivePreviewZoomed,
                    beyondViewportPageCount = 0,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("simulator_horizontal_pager")
                ) { page ->
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        .absoluteValue
                        .coerceIn(0f, 1f)
                    val transitionScale = lerp(0.955f, 1f, 1f - pageOffset)
                    val transitionAlpha = lerp(0.75f, 1f, 1f - pageOffset)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = transitionScale
                                scaleY = transitionScale
                                alpha = transitionAlpha
                            }
                    ) {
                        when (page) {
                            0, 1 -> {
                                val pageDocType = if (page == 0) DocumentType.CHEQUE else DocumentType.SFP01
                                SimulatorDocumentPage(
                                    pageDocType = pageDocType,
                                    uiState = uiState,
                                    chequeAmountInWords = chequeAmountInWords,
                                    sfpAmountInWords = sfpAmountInWords,
                                    bottomPadding = bottomPadding,
                                    isDark = isDark,
                                    viewModel = viewModel,
                                    onChequeZoomChanged = { isChequeZoomed = it },
                                    onSfpZoomChanged = { isSfpZoomed = it },
                                    onScrollResetTop = {
                                        isBottomBarVisible = true
                                        scrollDeltaAccumulator = 0f
                                    },
                                    onNavigateToProfilesPage = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                    }
                                )
                            }

                            else -> {
                                PostalProfilesPageContent(
                                    uiState = uiState,
                                    isDark = isDark,
                                    fabBottomPadding = profilesFabBottomPadding,
                                    listBottomPadding = profilesListBottomPadding,
                                    onAddProfile = onAddProfile,
                                    onEditProfile = onEditProfile,
                                    onToggleFavorite = { viewModel.toggleProfileFavorite(it) },
                                    onDeleteProfile = { viewModel.deletePostalProfile(it) }
                                )
                            }
                        }
                    }
                }

                // Floating Qdash-Style Bottom Navigation Bar synced with HorizontalPager
                SimulatorBottomBar(
                    selectedTab = activeTab,
                    isVisible = isBottomBarVisible,
                    onSelectTab = { tab ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = tab.ordinal,
                                animationSpec = tween(durationMillis = 320)
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun SimulatorDocumentPage(
    pageDocType: DocumentType,
    uiState: DocumentSimulatorUiState,
    chequeAmountInWords: String?,
    sfpAmountInWords: String?,
    bottomPadding: androidx.compose.ui.unit.Dp,
    isDark: Boolean,
    viewModel: DocumentSimulatorViewModel,
    onChequeZoomChanged: (Boolean) -> Unit,
    onSfpZoomChanged: (Boolean) -> Unit,
    onScrollResetTop: () -> Unit,
    onNavigateToProfilesPage: () -> Unit
) {
    val context = LocalContext.current
    val isCheque = pageDocType == DocumentType.CHEQUE
    val controlsScrollState = rememberScrollState()
    val pageUiState = remember(uiState, pageDocType) {
        if (uiState.selectedDocType == pageDocType) uiState else uiState.copy(selectedDocType = pageDocType)
    }

    var chequeZoomScale by remember { mutableFloatStateOf(1f) }
    var chequePanOffset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(chequeZoomScale) {
        onChequeZoomChanged(chequeZoomScale > 1.05f)
    }

    LaunchedEffect(controlsScrollState.value) {
        if (controlsScrollState.value <= 8) {
            onScrollResetTop()
        }
    }

    val isFrenchWords = (if (isCheque) uiState.chequeWordsLanguage else uiState.sfpWordsLanguage) == WordsLanguage.FRENCH
    val chequeRipCode = remember(uiState.chequeCcp) {
        PostalCcpUtils.buildFullRip(uiState.chequeCcp).orEmpty()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactHeight = maxHeight < 460.dp
        val adaptivePreviewHeight = if (isCompactHeight) {
            (maxHeight - 110.dp).coerceIn(168.dp, 224.dp)
        } else {
            264.dp
        }
        val chequeCardMaxWidth = if (isCompactHeight) 390.dp else 460.dp

        if (isCompactHeight) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(controlsScrollState)
                    .testTag(if (isCheque) "simulator_controls_scroll" else "sfp_controls_scroll")
                    .padding(bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocumentPreviewAndProgressSection(
                    isCheque = isCheque,
                    isCompactHeight = true,
                    isDark = isDark,
                    chequeCardMaxWidth = chequeCardMaxWidth,
                    adaptivePreviewHeight = adaptivePreviewHeight,
                    uiState = uiState,
                    pageUiState = pageUiState,
                    chequeAmountInWords = chequeAmountInWords,
                    sfpAmountInWords = sfpAmountInWords,
                    isFrenchWords = isFrenchWords,
                    chequeRipCode = chequeRipCode,
                    chequeZoomScale = chequeZoomScale,
                    chequePanOffset = chequePanOffset,
                    onChequeZoomTransform = { s, p ->
                        chequeZoomScale = s
                        chequePanOffset = p
                    },
                    onChequeDoubleTap = {
                        if (chequeZoomScale > 1.05f) {
                            chequeZoomScale = 1f
                            chequePanOffset = Offset.Zero
                        } else {
                            chequeZoomScale = 1.65f
                        }
                    },
                    onSfpZoomChanged = onSfpZoomChanged,
                    viewModel = viewModel
                )
                SimulatorQuickFillControlsBody(
                    uiState = uiState,
                    pageUiState = pageUiState,
                    viewModel = viewModel,
                    context = context,
                    onNavigateToProfilesPage = onNavigateToProfilesPage,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                DocumentPreviewAndProgressSection(
                    isCheque = isCheque,
                    isCompactHeight = false,
                    isDark = isDark,
                    chequeCardMaxWidth = chequeCardMaxWidth,
                    adaptivePreviewHeight = adaptivePreviewHeight,
                    uiState = uiState,
                    pageUiState = pageUiState,
                    chequeAmountInWords = chequeAmountInWords,
                    sfpAmountInWords = sfpAmountInWords,
                    isFrenchWords = isFrenchWords,
                    chequeRipCode = chequeRipCode,
                    chequeZoomScale = chequeZoomScale,
                    chequePanOffset = chequePanOffset,
                    onChequeZoomTransform = { s, p ->
                        chequeZoomScale = s
                        chequePanOffset = p
                    },
                    onChequeDoubleTap = {
                        if (chequeZoomScale > 1.05f) {
                            chequeZoomScale = 1f
                            chequePanOffset = Offset.Zero
                        } else {
                            chequeZoomScale = 1.65f
                        }
                    },
                    onSfpZoomChanged = onSfpZoomChanged,
                    viewModel = viewModel
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(controlsScrollState)
                            .testTag(if (isCheque) "simulator_controls_scroll" else "sfp_controls_scroll")
                            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = bottomPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SimulatorQuickFillControlsBody(
                            uiState = uiState,
                            pageUiState = pageUiState,
                            viewModel = viewModel,
                            context = context,
                            onNavigateToProfilesPage = onNavigateToProfilesPage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentPreviewAndProgressSection(
    isCheque: Boolean,
    isCompactHeight: Boolean,
    isDark: Boolean,
    chequeCardMaxWidth: androidx.compose.ui.unit.Dp,
    adaptivePreviewHeight: androidx.compose.ui.unit.Dp,
    uiState: DocumentSimulatorUiState,
    pageUiState: DocumentSimulatorUiState,
    chequeAmountInWords: String?,
    sfpAmountInWords: String?,
    isFrenchWords: Boolean,
    chequeRipCode: String,
    chequeZoomScale: Float,
    chequePanOffset: Offset,
    onChequeZoomTransform: (Float, Offset) -> Unit,
    onChequeDoubleTap: () -> Unit,
    onSfpZoomChanged: (Boolean) -> Unit,
    viewModel: DocumentSimulatorViewModel
) {
    if (isCheque) {
        val frameBorderColor = if (isDark) {
            ColorTokens.PosteYellow.copy(alpha = 0.40f)
        } else {
            ColorTokens.PosteChequeBorder.copy(alpha = 0.65f)
        }
        val frameBgColor = if (isDark) Color(0xFF18181B) else Color.White

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = chequeCardMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = frameBgColor,
                border = BorderStroke(1.dp, frameBorderColor),
                shadowElevation = if (isDark) 6.dp else 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isCompactHeight) Modifier.heightIn(max = adaptivePreviewHeight) else Modifier)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { onChequeDoubleTap() })
                        }
                        .twoFingerPinchZoom(
                            getScale = { chequeZoomScale },
                            getPan = { chequePanOffset },
                            onTransform = onChequeZoomTransform,
                            minScale = 1f,
                            maxScale = 2.8f
                        )
                ) {
                    Box(
                        modifier = if (isCompactHeight) {
                            Modifier.verticalScroll(rememberScrollState(), enabled = chequeZoomScale <= 1.05f)
                        } else {
                            Modifier
                        }
                    ) {
                        ChequeVisualView(
                            chequeAmount = uiState.chequeAmount,
                            chequeBeneficiary = uiState.chequeBeneficiary,
                            chequePlace = uiState.chequePlace,
                            chequeDate = uiState.chequeDate,
                            chequeCcp = uiState.chequeCcp,
                            chequeKey = uiState.chequeKey,
                            onFieldTap = { viewModel.setFocusedField(it) },
                            focusedField = uiState.focusedField,
                            chequeNumber = uiState.chequeNumber,
                            accountHolderName = uiState.chequeAccountHolder,
                            accountHolderAddress = uiState.chequeAccountAddress,
                            amountInWords = chequeAmountInWords,
                            isFrenchWords = isFrenchWords,
                            onToggleWordsLanguage = { viewModel.toggleWordsLanguage() },
                            ripCode = chequeRipCode,
                            modifier = Modifier.graphicsLayer(
                                scaleX = chequeZoomScale,
                                scaleY = chequeZoomScale,
                                translationX = chequePanOffset.x,
                                translationY = chequePanOffset.y
                            )
                        )
                    }

                    TranslucentPreviewFloatingActions(
                        isGuideActive = uiState.activePanel == SimulatorActivePanel.GUIDE,
                        onRotateExpandClick = { viewModel.setFullscreenViewerOpen(true) },
                        onToggleGuideClick = { viewModel.toggleActivePanel(SimulatorActivePanel.GUIDE) },
                        expandButtonTestTag = "cheque_rotate_expand_button",
                        guideButtonTestTag = "cheque_guide_button",
                        modifier = Modifier.align(AbsoluteAlignment.BottomRight)
                    )
                }
            }
        }
    } else {
        Sfp01ExpandablePreview(
            uiState = pageUiState,
            onCcpChange = { viewModel.updateSfpCcp(it) },
            onKeyChange = { viewModel.updateSfpKey(it) },
            onAmountChange = { viewModel.updateSfpAmount(it) },
            onSenderNomChange = { viewModel.updateSfpSenderNom(it) },
            onSenderPrenomChange = { viewModel.updateSfpSenderPrenom(it) },
            onSenderAddressChange = { viewModel.updateSfpSenderAddress(it) },
            onSenderPhoneChange = { viewModel.updateSfpSenderPhone(it) },
            onBeneficiaryNomChange = { viewModel.updateSfpBeneficiaryNom(it) },
            onBeneficiaryPrenomChange = { viewModel.updateSfpBeneficiaryPrenom(it) },
            onBeneficiaryAddressChange = { viewModel.updateSfpBeneficiaryAddress(it) },
            onBeneficiaryPhoneChange = { viewModel.updateSfpBeneficiaryPhone(it) },
            onPlaceChange = { viewModel.updateSfpPlace(it) },
            onDateChange = { viewModel.updateSfpDate(it) },
            onIdDescriptionChange = { viewModel.updateSfpIdDescription(it) },
            onOperationChange = { viewModel.updateSfpOperation(it) },
            onJustificatifCcpChange = { viewModel.updateSfpJustificatifCcp(it) },
            onAvisCreditChange = { viewModel.updateSfpAvisCredit(it) },
            onCarnetChequesChange = { viewModel.updateSfpCarnetCheques(it) },
            onCodeConfidentielChange = { viewModel.updateSfpCodeConfidentiel(it) },
            onRipChange = { viewModel.updateSfpRip(it) },
            focusedField = uiState.focusedField,
            onFieldFocus = { viewModel.setFocusedField(it) },
            onMotifChange = { viewModel.updateSfpMotif(it) },
            sfpReleveOperations = uiState.sfpReleveOperations,
            onReleveOperationsChange = { viewModel.updateSfpReleveOperations(it) },
            sfpAvecSolde = uiState.sfpAvecSolde,
            onAvecSoldeChange = { viewModel.updateSfpAvecSolde(it) },
            sfpNotificationAvoir = uiState.sfpNotificationAvoir,
            onNotificationAvoirChange = { viewModel.updateSfpNotificationAvoir(it) },
            sfpCommandeCarnet = uiState.sfpCommandeCarnet,
            onCommandeCarnetChange = { viewModel.updateSfpCommandeCarnet(it) },
            sfpReleveDu = uiState.sfpReleveDu,
            onReleveDuChange = { viewModel.updateSfpReleveDu(it) },
            sfpReleveAu = uiState.sfpReleveAu,
            onReleveAuChange = { viewModel.updateSfpReleveAu(it) },
            sfpPiType = uiState.sfpPiType,
            sfpPiNumero = uiState.sfpPiNumero,
            sfpPiDelivreLe = uiState.sfpPiDelivreLe,
            sfpPiPar = uiState.sfpPiPar,
            amountInWords = sfpAmountInWords,
            isFrenchWords = isFrenchWords,
            onToggleWordsLanguage = { viewModel.toggleWordsLanguage() },
            isPreviewReadOnly = true,
            isGuideActive = uiState.activePanel == SimulatorActivePanel.GUIDE,
            onRotateExpandClick = { viewModel.setFullscreenViewerOpen(true) },
            onToggleGuideClick = { viewModel.toggleActivePanel(SimulatorActivePanel.GUIDE) },
            onZoomChanged = onSfpZoomChanged,
            previewHeight = adaptivePreviewHeight,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )
    }

    DocumentCompletionProgressStrip(
        completionPercentage = uiState.completionPercentage,
        missingFieldLabels = uiState.missingFieldLabels,
        isDark = isDark,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
    )

    AnimatedVisibility(
        visible = uiState.activePanel == SimulatorActivePanel.GUIDE,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                EducationalGuidePanel(
                    uiState = pageUiState,
                    onStartGuide = { viewModel.startGuide() },
                    onPrevStep = { viewModel.prevGuideStep() },
                    onNextStep = { viewModel.nextGuideStep() },
                    onStopGuide = {
                        viewModel.stopGuide()
                        viewModel.toggleActivePanel(SimulatorActivePanel.NONE)
                    }
                )
            }
        }
    }
}

@Composable
private fun SimulatorQuickFillControlsBody(
    uiState: DocumentSimulatorUiState,
    pageUiState: DocumentSimulatorUiState,
    viewModel: DocumentSimulatorViewModel,
    context: android.content.Context,
    onNavigateToProfilesPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuickFillControlsPanel(
        uiState = pageUiState,
        onPullMyAccountClick = {
            if (!viewModel.applyPrimarySelfProfile()) {
                onNavigateToProfilesPage()
            }
        },
        onManageProfilesClick = onNavigateToProfilesPage,
        onSelectProfileDirect = { profile, role ->
            viewModel.autofillFromProfile(profile, role)
            Toast.makeText(context, "تمت التعبئة من ${profile.profileName}", Toast.LENGTH_SHORT).show()
        },
        onMoiMemeClick = {
            viewModel.setChequeBeneficiaryMoiMeme()
            if (uiState.chequeCcp.isBlank()) {
                viewModel.applyPrimarySelfProfile()
            }
        },
        onToggleWordsLanguage = { viewModel.toggleWordsLanguage() },
        onToggleManualEditMode = { viewModel.toggleManualEditMode() },
        onClearAmountClick = { viewModel.selectPresetAmount(0) },
        onChequeAmountChange = { viewModel.updateChequeAmount(it) },
        onChequeBeneficiaryChange = { viewModel.updateChequeBeneficiary(it) },
        onChequeCcpChange = { viewModel.updateChequeCcp(it) },
        onChequeKeyChange = { viewModel.updateChequeKey(it) },
        onChequeAccountHolderChange = { viewModel.updateChequeAccountHolder(it) },
        onChequeAccountAddressChange = { viewModel.updateChequeAccountAddress(it) },
        onChequePlaceChange = { viewModel.updateChequePlace(it) },
        onChequeDateChange = { viewModel.updateChequeDate(it) },
        onSfpOperationChange = { viewModel.updateSfpOperation(it) },
        onSfpReleveOperationsChange = { viewModel.updateSfpReleveOperations(it) },
        onSfpAvecSoldeChange = { viewModel.updateSfpAvecSolde(it) },
        onSfpNotificationAvoirChange = { viewModel.updateSfpNotificationAvoir(it) },
        onSfpCommandeCarnetChange = { viewModel.updateSfpCommandeCarnet(it) },
        onSfpReleveDuChange = { viewModel.updateSfpReleveDu(it) },
        onSfpReleveAuChange = { viewModel.updateSfpReleveAu(it) },
        onSfpAmountChange = { viewModel.updateSfpAmount(it) },
        onSfpCcpChange = { viewModel.updateSfpCcp(it) },
        onSfpKeyChange = { viewModel.updateSfpKey(it) },
        onSfpSenderNomChange = { viewModel.updateSfpSenderNom(it) },
        onSfpSenderPrenomChange = { viewModel.updateSfpSenderPrenom(it) },
        onSfpSenderAddressChange = { viewModel.updateSfpSenderAddress(it) },
        onSfpBeneficiaryNomChange = { viewModel.updateSfpBeneficiaryNom(it) },
        onSfpBeneficiaryPrenomChange = { viewModel.updateSfpBeneficiaryPrenom(it) },
        onSfpBeneficiaryAddressChange = { viewModel.updateSfpBeneficiaryAddress(it) },
        onSfpMotifChange = { viewModel.updateSfpMotif(it) },
        onSfpPlaceChange = { viewModel.updateSfpPlace(it) },
        onSfpDateChange = { viewModel.updateSfpDate(it) },
        onSfpPiTypeChange = { viewModel.updateSfpPiType(it) },
        onSfpPiNumeroChange = { viewModel.updateSfpPiNumero(it) },
        onSfpPiDelivreLeChange = { viewModel.updateSfpPiDelivreLe(it) },
        onSfpPiParChange = { viewModel.updateSfpPiPar(it) },
        onSfpIdDescriptionChange = { viewModel.updateSfpIdDescription(it) },
        modifier = modifier
    )
}

@Composable
private fun DocumentCompletionProgressStrip(
    completionPercentage: Float,
    missingFieldLabels: List<String>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = completionPercentage.coerceIn(0f, 1f),
        animationSpec = tween(250),
        label = "doc_completion_progress"
    )
    val isReady = completionPercentage >= 0.999f && missingFieldLabels.isEmpty()
    val progressColor = when {
        isReady -> if (isDark) ColorTokens.SuccessDark else ColorTokens.Success
        isDark -> ColorTokens.PosteYellow
        else -> ColorTokens.PosteBlue
    }

    val missingHint = when {
        isReady -> "جاهز للتعبئة"
        missingFieldLabels.size <= 3 -> "الناقص: ${missingFieldLabels.joinToString("، ")}"
        else -> "الناقص: ${missingFieldLabels.take(2).joinToString("، ")} (+${missingFieldLabels.size - 2})"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("document_progress_strip"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = missingHint,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isReady) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isReady) progressColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("document_progress_missing_text")
            )
            Text(
                text = "\u2066${(completionPercentage * 100).toInt()}%\u2069",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = progressColor,
                maxLines = 1
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    }
}
