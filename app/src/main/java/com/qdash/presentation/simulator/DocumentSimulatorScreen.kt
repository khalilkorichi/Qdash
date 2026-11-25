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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.PostalCcpUtils
import com.qdash.domain.model.PostalProfileRole
import com.qdash.presentation.simulator.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentSimulatorScreen(
    viewModel: DocumentSimulatorViewModel,
    onOpenPostalProfiles: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var showProfilePicker by remember { mutableStateOf(false) }
    var selectedRoleForAutofill by remember { mutableStateOf<PostalProfileRole?>(null) }

    // 2-finger dynamic pinch-to-zoom & pan state for inline Check preview
    var zoomScale by remember(uiState.selectedDocType) { mutableFloatStateOf(1f) }
    var panOffset by remember(uiState.selectedDocType) { mutableStateOf(Offset.Zero) }

    // Bottom navigation bar scroll-hide/show behavior matching AppShell
    val controlsScrollState = rememberScrollState()
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

    LaunchedEffect(controlsScrollState.value) {
        if (controlsScrollState.value <= 8) {
            isBottomBarVisible = true
            scrollDeltaAccumulator = 0f
        }
    }

    LaunchedEffect(uiState.selectedDocType) {
        isBottomBarVisible = true
        scrollDeltaAccumulator = 0f
    }

    LaunchedEffect(uiState.quickFillFeedback) {
        val feedback = uiState.quickFillFeedback
        if (!feedback.isNullOrBlank()) {
            Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
            viewModel.clearQuickFillFeedback()
        }
    }

    val isCheque = uiState.selectedDocType == DocumentType.CHEQUE
    val activeWordsLanguage = if (isCheque) uiState.chequeWordsLanguage else uiState.sfpWordsLanguage
    val isFrenchWords = activeWordsLanguage == WordsLanguage.FRENCH

    val chequeAmountInWords = remember(uiState.chequeAmount, uiState.chequeWordsLanguage) {
        val amt = AmountConversionEngine.parseFlexibleAmount(uiState.chequeAmount)
        if (amt != null && amt > 0.0) {
            if (uiState.chequeWordsLanguage == WordsLanguage.FRENCH) {
                AmountConversionEngine.convertToFrenchWords(amt)
            } else {
                AmountConversionEngine.convertToArabicWords(amt, preferCentimes = false)
            }
        } else {
            null
        }
    }

    val sfpAmountInWords = remember(uiState.sfpAmount, uiState.sfpWordsLanguage) {
        val amt = AmountConversionEngine.parseFlexibleAmount(uiState.sfpAmount)
        if (amt != null && amt > 0.0) {
            if (uiState.sfpWordsLanguage == WordsLanguage.FRENCH) {
                AmountConversionEngine.convertToFrenchWords(amt)
            } else {
                AmountConversionEngine.convertToArabicWords(amt, preferCentimes = false)
            }
        } else {
            null
        }
    }

    val chequeRipCode = remember(uiState.chequeCcp) {
        PostalCcpUtils.buildFullRip(uiState.chequeCcp).orEmpty()
    }

    val bottomPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) 96.dp else 20.dp,
        animationSpec = tween(220),
        label = "simulator_bottom_padding"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        topBar = {
            FinTrackTopBar(
                title = if (isCheque) "الصك البريدي" else "شاك سوكور",
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- 1. Document Preview Directly at Top ---
                if (isCheque) {
                    val frameBorderColor = if (isDark) {
                        ColorTokens.PosteYellow.copy(alpha = 0.40f)
                    } else {
                        ColorTokens.PosteChequeBorder.copy(alpha = 0.65f)
                    }
                    val frameBgColor = if (isDark) Color(0xFF18181B) else Color.White

                    Surface(
                        modifier = Modifier
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
                                .padding(4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (zoomScale > 1.05f) {
                                                zoomScale = 1f
                                                panOffset = Offset.Zero
                                            } else {
                                                zoomScale = 1.65f
                                            }
                                        }
                                    )
                                }
                                .twoFingerPinchZoom(
                                    getScale = { zoomScale },
                                    getPan = { panOffset },
                                    onTransform = { newScale, newPan ->
                                        zoomScale = newScale
                                        panOffset = newPan
                                    },
                                    minScale = 1f,
                                    maxScale = 2.8f
                                )
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
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                )
                            )

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
                } else {
                    Sfp01ExpandablePreview(
                        uiState = uiState,
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
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                // --- 2. Compact Inline Progress Bar + Missing Field Hint ---
                DocumentCompletionProgressStrip(
                    completionPercentage = uiState.completionPercentage,
                    missingFieldLabels = uiState.missingFieldLabels,
                    isDark = isDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // --- 3. Collapsible Educational Guide Panel (toggled from floating preview icon) ---
                AnimatedVisibility(
                    visible = uiState.activePanel == SimulatorActivePanel.GUIDE,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                EducationalGuidePanel(
                                    uiState = uiState,
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

                // --- 4. Scrollable Quick-Fill Controls Area ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(controlsScrollState)
                            .testTag("simulator_controls_scroll")
                            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = bottomPadding),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        QuickFillControlsPanel(
                            uiState = uiState,
                            onPullMyAccountClick = {
                                if (!viewModel.applyPrimarySelfProfile()) {
                                    selectedRoleForAutofill = PostalProfileRole.SELF
                                    showProfilePicker = true
                                }
                            },
                            onOpenProfilePicker = { role ->
                                selectedRoleForAutofill = role
                                showProfilePicker = true
                            },
                            onSelectProfileDirect = { profile, role ->
                                viewModel.autofillFromProfile(profile, role)
                                Toast.makeText(context, "تمت التعبئة من ${profile.profileName}", Toast.LENGTH_SHORT).show()
                            },
                            onMoiMemeClick = { viewModel.setChequeBeneficiaryMoiMeme() },
                            onToggleWordsLanguage = { viewModel.toggleWordsLanguage() },
                            onToggleManualEditMode = { viewModel.toggleManualEditMode() },
                            onClearAmountClick = { viewModel.selectPresetAmount(0) },
                            onChequeNumberChange = { viewModel.updateChequeNumber(it) },
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
                            onSfpIdDescriptionChange = { viewModel.updateSfpIdDescription(it) }
                        )
                    }
                }
            }

            // --- 5. Floating Qdash-Style Bottom Navigation Bar ---
            SimulatorBottomBar(
                selectedTab = if (isCheque) SimulatorBottomTab.CHEQUE else SimulatorBottomTab.CHEQUE_SECOURS,
                isVisible = isBottomBarVisible && !uiState.isFullscreenViewerOpen,
                onSelectTab = { tab ->
                    when (tab) {
                        SimulatorBottomTab.CHEQUE -> viewModel.selectDocumentType(DocumentType.CHEQUE)
                        SimulatorBottomTab.CHEQUE_SECOURS -> viewModel.selectDocumentType(DocumentType.SFP01)
                        SimulatorBottomTab.POSTAL_PROFILES -> onOpenPostalProfiles()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (uiState.isFullscreenViewerOpen) {
        FullscreenDocumentEditorDialog(
            uiState = uiState,
            amountInWords = if (isCheque) chequeAmountInWords else sfpAmountInWords,
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
            onChequeNumberChange = { viewModel.updateChequeNumber(it) },
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
    }

    if (showProfilePicker) {
        AutofillProfileBottomSheet(
            profiles = uiState.savedProfiles,
            onDismiss = { showProfilePicker = false },
            onProfileSelected = { profile ->
                selectedRoleForAutofill?.let { role ->
                    viewModel.autofillFromProfile(profile, role)
                    Toast.makeText(context, "تم التعبئة التلقائية!", Toast.LENGTH_SHORT).show()
                }
                showProfilePicker = false
            }
        )
    }
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
