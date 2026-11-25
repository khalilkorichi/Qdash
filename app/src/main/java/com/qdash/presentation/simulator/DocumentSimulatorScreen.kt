package com.qdash.presentation.simulator

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var showProfilePicker by remember { mutableStateOf(false) }
    var selectedRoleForAutofill by remember { mutableStateOf<PostalProfileRole?>(null) }
    var isSfpFullscreen by remember(uiState.selectedDocType) { mutableStateOf(false) }

    // Pinch-to-zoom & quick zoom toggle state for check preview
    var zoomScale by remember(uiState.selectedDocType) { mutableFloatStateOf(1f) }
    var panOffset by remember(uiState.selectedDocType) { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (zoomScale * zoomChange).coerceIn(1f, 2.5f)
        zoomScale = nextScale
        if (nextScale > 1.02f) {
            panOffset += panChange
        } else {
            panOffset = Offset.Zero
        }
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FinTrackTopBar(
                title = if (isCheque) "محاكي الصك البريدي" else "محاكي استمارة SFP 01",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // --- 1. Upper Toolbar (compact for 320dp/360dp RTL widths) ---
            SimulatorToolbar(
                isCheque = isCheque,
                isFrenchWords = isFrenchWords,
                isZoomed = if (isCheque) zoomScale > 1.05f else isSfpFullscreen,
                activePanel = uiState.activePanel,
                errorCount = uiState.errors.size,
                isDark = isDark,
                onSelectDocType = { viewModel.selectDocumentType(it) },
                onToggleWordsLanguage = { viewModel.toggleWordsLanguage() },
                onToggleZoom = {
                    if (isCheque) {
                        if (zoomScale > 1.05f) {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        } else {
                            zoomScale = 1.6f
                        }
                    } else {
                        isSfpFullscreen = !isSfpFullscreen
                    }
                },
                onToggleGuide = { viewModel.toggleActivePanel(SimulatorActivePanel.GUIDE) },
                onToggleReview = { viewModel.toggleActivePanel(SimulatorActivePanel.REVIEW) }
            )

            // --- 2. Collapsible Active Panel (Guide / Review) ---
            AnimatedVisibility(
                visible = uiState.activePanel != SimulatorActivePanel.NONE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            when (uiState.activePanel) {
                                SimulatorActivePanel.GUIDE -> {
                                    EducationalGuidePanel(
                                        uiState = uiState,
                                        onStartGuide = { viewModel.startGuide() },
                                        onPrevStep = { viewModel.prevGuideStep() },
                                        onNextStep = { viewModel.nextGuideStep() },
                                        onStopGuide = { viewModel.stopGuide() }
                                    )
                                }
                                SimulatorActivePanel.REVIEW -> {
                                    ValidationChecklistPanel(uiState = uiState)
                                }
                                SimulatorActivePanel.NONE -> Unit
                            }
                        }
                    }
                }
            }

            // --- 3. Document Preview Area ---
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
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            .transformable(
                                state = transformableState,
                                canPan = { zoomScale > 1.02f }
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
                    expanded = isSfpFullscreen,
                    onExpandedChange = { isSfpFullscreen = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- 4. Scrollable Quick-Fill Controls Area ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        onPresetAmountClick = { viewModel.selectPresetAmount(it) },
                        onIncrementAmountClick = { viewModel.incrementAmount(it) },
                        onClearAmountClick = { viewModel.selectPresetAmount(0) },
                        onOpenReadyAmountsClick = { viewModel.toggleReadyAmounts(true) },
                        onQuickPlaceClick = { viewModel.selectQuickPlace(it) },
                        onTodayDateClick = { viewModel.setTodayDate() },
                        onAutoCalcKeyClick = { viewModel.computeCcpKeyFromCurrent() },
                        onExtractRipInput = { viewModel.extractFromRipPaste(it) },
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
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
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

    if (uiState.showReadyAmounts) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleReadyAmounts(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "المبالغ الجاهزة (التعبئة السريعة)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                AmountCheatSheetPanel(
                    onAmountSelected = { amount ->
                        if (isCheque) {
                            viewModel.updateChequeAmount(amount)
                        } else {
                            viewModel.updateSfpAmount(amount)
                        }
                        viewModel.toggleReadyAmounts(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun SimulatorToolbar(
    isCheque: Boolean,
    isFrenchWords: Boolean,
    isZoomed: Boolean,
    activePanel: SimulatorActivePanel,
    errorCount: Int,
    isDark: Boolean,
    onSelectDocType: (DocumentType) -> Unit,
    onToggleWordsLanguage: () -> Unit,
    onToggleZoom: () -> Unit,
    onToggleGuide: () -> Unit,
    onToggleReview: () -> Unit
) {
    val activeAccent = if (isDark) ColorTokens.PosteYellow else MaterialTheme.colorScheme.primary
    val activeTint = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simulator_toolbar")
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document switcher: صك | استمارة
        Row(
            modifier = Modifier
                .testTag("doc_switcher_container")
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { onSelectDocType(DocumentType.CHEQUE) },
                color = if (isCheque) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("doc_switcher_cheque")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text(
                        text = "صك",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isCheque) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Surface(
                onClick = { onSelectDocType(DocumentType.SFP01) },
                color = if (!isCheque) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("doc_switcher_sfp01")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text(
                        text = "استمارة",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (!isCheque) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Trailing action buttons: Lang | Zoom | Guide | Review
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onToggleWordsLanguage,
                color = if (isFrenchWords) activeAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isFrenchWords) activeAccent else MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("toolbar_lang_toggle")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFrenchWords) "FR" else "AR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isFrenchWords) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            ToolbarIconButton(
                isActive = isZoomed,
                activeColor = activeTint,
                testTag = "toolbar_zoom_button",
                onClick = onToggleZoom
            ) {
                Icon(
                    imageVector = if (isZoomed) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                    contentDescription = if (isZoomed) "تصغير المعاينة" else "تكبير المعاينة",
                    tint = if (isZoomed) activeTint else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            val isGuideActive = activePanel == SimulatorActivePanel.GUIDE
            ToolbarIconButton(
                isActive = isGuideActive,
                activeColor = activeTint,
                testTag = "toolbar_guide_button",
                onClick = onToggleGuide
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "دليل خطوة بخطوة",
                    tint = if (isGuideActive) activeTint else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            val isReviewActive = activePanel == SimulatorActivePanel.REVIEW
            BadgedBox(
                badge = {
                    if (errorCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ) {
                            Text(errorCount.toString(), fontSize = 10.sp)
                        }
                    }
                }
            ) {
                ToolbarIconButton(
                    isActive = isReviewActive,
                    activeColor = activeTint,
                    testTag = "toolbar_review_button",
                    onClick = onToggleReview
                ) {
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = "مراجعة الوثيقة",
                        tint = if (isReviewActive) activeTint else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    isActive: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isActive) activeColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isActive) activeColor else MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(34.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
