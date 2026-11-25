package com.qdash.presentation.simulator.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.PostalAmountSuggester
import com.qdash.core.utils.PostalCcpUtils
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.presentation.simulator.DocumentSimulatorUiState
import com.qdash.presentation.simulator.DocumentType
import com.qdash.presentation.simulator.SfpOperationType
import com.qdash.presentation.simulator.WordsLanguage
import com.qdash.ui.designsystem.components.FormGrid
import com.qdash.ui.designsystem.tokens.ColorTokens

/**
 * Full-screen rotated landscape viewer and interactive editor for both Check and Chèque Secours.
 * Supports 2-finger pinch-to-zoom, pan, double-tap reset, and opens a floating edit card on the
 * opposite vertical side of the tapped document field so the live ink remains visible.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenDocumentEditorDialog(
    uiState: DocumentSimulatorUiState,
    amountInWords: String?,
    onDismiss: () -> Unit,
    onToggleWordsLanguage: () -> Unit,
    onFieldFocus: (String?) -> Unit,
    onSelectProfile: (PostalProfile, PostalProfileRole) -> Unit,
    // Cheque callbacks
    onChequeAmountChange: (String) -> Unit,
    onChequeBeneficiaryChange: (String) -> Unit,
    onChequeCcpChange: (String) -> Unit,
    onChequeKeyChange: (String) -> Unit,
    onChequePlaceChange: (String) -> Unit,
    onChequeDateChange: (String) -> Unit,
    onChequeNumberChange: (String) -> Unit,
    onChequeHolderChange: (String) -> Unit,
    onChequeMoiMemeClick: () -> Unit,
    // Chèque Secours (SFP 01) callbacks
    onSfpCcpChange: (String) -> Unit,
    onSfpKeyChange: (String) -> Unit,
    onSfpAmountChange: (String) -> Unit,
    onSfpOperationChange: (SfpOperationType) -> Unit,
    onSfpReleveOperationsChange: (Boolean) -> Unit,
    onSfpAvecSoldeChange: (Boolean) -> Unit,
    onSfpNotificationAvoirChange: (Boolean) -> Unit,
    onSfpCommandeCarnetChange: (Boolean) -> Unit,
    onSfpReleveDuChange: (String) -> Unit,
    onSfpReleveAuChange: (String) -> Unit,
    onSfpSenderNomChange: (String) -> Unit,
    onSfpSenderPrenomChange: (String) -> Unit,
    onSfpSenderAddressChange: (String) -> Unit,
    onSfpBeneficiaryNomChange: (String) -> Unit,
    onSfpBeneficiaryPrenomChange: (String) -> Unit,
    onSfpBeneficiaryAddressChange: (String) -> Unit,
    onSfpMotifChange: (String) -> Unit,
    onSfpPlaceChange: (String) -> Unit,
    onSfpDateChange: (String) -> Unit,
    onSfpIdDescriptionChange: (String) -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.findActivity()
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = previousOrientation
        }
    }

    val isCheque = uiState.selectedDocType == DocumentType.CHEQUE
    val isFrenchWords = if (isCheque) {
        uiState.chequeWordsLanguage == WordsLanguage.FRENCH
    } else {
        uiState.sfpWordsLanguage == WordsLanguage.FRENCH
    }

    var activeEditorField by remember { mutableStateOf<String?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val scrollState = rememberScrollState()

    val isTopFieldSelected = remember(activeEditorField) {
        isDocumentTopHalfField(activeEditorField)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag(if (isCheque) "cheque_fullscreen_dialog" else "sfp01_fullscreen_dialog"),
            color = Color(0xFF0A0F1A)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Full-width interactive document area with 2-finger pinch-zoom & pan
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (zoomScale > 1.05f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 1.65f
                                    }
                                },
                                onTap = {
                                    if (activeEditorField != null) {
                                        activeEditorField = null
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
                            maxScale = 3f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState, enabled = zoomScale <= 1.05f)
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val docModifier = Modifier
                            .widthIn(max = 780.dp)
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = panOffset.x,
                                translationY = panOffset.y
                            )

                        if (isCheque) {
                            ChequeVisualView(
                                chequeAmount = uiState.chequeAmount,
                                chequeBeneficiary = uiState.chequeBeneficiary,
                                chequePlace = uiState.chequePlace,
                                chequeDate = uiState.chequeDate,
                                chequeCcp = uiState.chequeCcp,
                                chequeKey = uiState.chequeKey,
                                onFieldTap = { tapped ->
                                    onFieldFocus(tapped)
                                    activeEditorField = tapped
                                },
                                focusedField = activeEditorField ?: uiState.focusedField,
                                chequeNumber = uiState.chequeNumber,
                                accountHolderName = uiState.chequeAccountHolder,
                                accountHolderAddress = uiState.chequeAccountAddress,
                                amountInWords = amountInWords,
                                isFrenchWords = isFrenchWords,
                                onToggleWordsLanguage = onToggleWordsLanguage,
                                ripCode = PostalCcpUtils.buildFullRip(uiState.chequeCcp).orEmpty(),
                                modifier = docModifier
                            )
                        } else {
                            FormGrid(
                                uiState = uiState,
                                onCcpChange = onSfpCcpChange,
                                onKeyChange = onSfpKeyChange,
                                onAmountChange = onSfpAmountChange,
                                onSenderNomChange = onSfpSenderNomChange,
                                onSenderPrenomChange = onSfpSenderPrenomChange,
                                onSenderAddressChange = onSfpSenderAddressChange,
                                onSenderPhoneChange = {},
                                onBeneficiaryNomChange = onSfpBeneficiaryNomChange,
                                onBeneficiaryPrenomChange = onSfpBeneficiaryPrenomChange,
                                onBeneficiaryAddressChange = onSfpBeneficiaryAddressChange,
                                onBeneficiaryPhoneChange = {},
                                onPlaceChange = onSfpPlaceChange,
                                onDateChange = onSfpDateChange,
                                onIdDescriptionChange = onSfpIdDescriptionChange,
                                onOperationChange = onSfpOperationChange,
                                onJustificatifCcpChange = {},
                                onAvisCreditChange = {},
                                onCarnetChequesChange = {},
                                onCodeConfidentielChange = {},
                                onRipChange = {},
                                focusedField = activeEditorField ?: uiState.focusedField,
                                onFieldFocus = { tapped ->
                                    onFieldFocus(tapped)
                                    if (tapped != null) activeEditorField = tapped
                                },
                                onMotifChange = onSfpMotifChange,
                                sfpReleveOperations = uiState.sfpReleveOperations,
                                onReleveOperationsChange = onSfpReleveOperationsChange,
                                sfpAvecSolde = uiState.sfpAvecSolde,
                                onAvecSoldeChange = onSfpAvecSoldeChange,
                                sfpNotificationAvoir = uiState.sfpNotificationAvoir,
                                onNotificationAvoirChange = onSfpNotificationAvoirChange,
                                sfpCommandeCarnet = uiState.sfpCommandeCarnet,
                                onCommandeCarnetChange = onSfpCommandeCarnetChange,
                                sfpReleveDu = uiState.sfpReleveDu,
                                onReleveDuChange = onSfpReleveDuChange,
                                sfpReleveAu = uiState.sfpReleveAu,
                                onReleveAuChange = onSfpReleveAuChange,
                                sfpPiType = uiState.sfpPiType,
                                sfpPiNumero = uiState.sfpPiNumero,
                                sfpPiDelivreLe = uiState.sfpPiDelivreLe,
                                sfpPiPar = uiState.sfpPiPar,
                                amountInWords = amountInWords,
                                isFrenchWords = isFrenchWords,
                                onToggleWordsLanguage = onToggleWordsLanguage,
                                isPreviewReadOnly = true,
                                modifier = docModifier
                            )
                        }
                    }
                }

                // Very translucent floating corner actions (Close, AR/FR, Reset Zoom)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (zoomScale > 1.05f || panOffset != Offset.Zero) {
                        Surface(
                            onClick = {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.28f),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.30f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = "إعادة ضبط التكبير",
                                    tint = ColorTokens.PosteYellow.copy(alpha = 0.9f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onToggleWordsLanguage,
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.28f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.30f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "تبديل لغة التفقيط",
                                tint = ColorTokens.PosteYellow.copy(alpha = 0.9f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isFrenchWords) "FR" else "AR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.32f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("fullscreen_close_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق المعاينة الكاملة",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Floating Editor Box positioned on the OPPOSITE side of the tapped document field
                val editorAlignment = if (isTopFieldSelected) Alignment.BottomCenter else Alignment.TopCenter
                AnimatedVisibility(
                    visible = activeEditorField != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { if (isTopFieldSelected) it / 2 else -it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { if (isTopFieldSelected) it / 2 else -it / 2 }),
                    modifier = Modifier
                        .align(editorAlignment)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    activeEditorField?.let { field ->
                        FloatingOppositeFieldEditorCard(
                            fieldName = field,
                            uiState = uiState,
                            onClose = { activeEditorField = null },
                            onSelectProfile = onSelectProfile,
                            onChequeAmountChange = onChequeAmountChange,
                            onChequeBeneficiaryChange = onChequeBeneficiaryChange,
                            onChequeCcpChange = onChequeCcpChange,
                            onChequeKeyChange = onChequeKeyChange,
                            onChequePlaceChange = onChequePlaceChange,
                            onChequeDateChange = onChequeDateChange,
                            onChequeNumberChange = onChequeNumberChange,
                            onChequeHolderChange = onChequeHolderChange,
                            onChequeMoiMemeClick = onChequeMoiMemeClick,
                            onSfpCcpChange = onSfpCcpChange,
                            onSfpKeyChange = onSfpKeyChange,
                            onSfpAmountChange = onSfpAmountChange,
                            onSfpOperationChange = onSfpOperationChange,
                            onSfpReleveOperationsChange = onSfpReleveOperationsChange,
                            onSfpAvecSoldeChange = onSfpAvecSoldeChange,
                            onSfpNotificationAvoirChange = onSfpNotificationAvoirChange,
                            onSfpCommandeCarnetChange = onSfpCommandeCarnetChange,
                            onSfpSenderNomChange = onSfpSenderNomChange,
                            onSfpSenderPrenomChange = onSfpSenderPrenomChange,
                            onSfpSenderAddressChange = onSfpSenderAddressChange,
                            onSfpBeneficiaryNomChange = onSfpBeneficiaryNomChange,
                            onSfpBeneficiaryPrenomChange = onSfpBeneficiaryPrenomChange,
                            onSfpBeneficiaryAddressChange = onSfpBeneficiaryAddressChange,
                            onSfpMotifChange = onSfpMotifChange,
                            onSfpPlaceChange = onSfpPlaceChange,
                            onSfpDateChange = onSfpDateChange,
                            onSfpIdDescriptionChange = onSfpIdDescriptionChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingOppositeFieldEditorCard(
    fieldName: String,
    uiState: DocumentSimulatorUiState,
    onClose: () -> Unit,
    onSelectProfile: (PostalProfile, PostalProfileRole) -> Unit,
    onChequeAmountChange: (String) -> Unit,
    onChequeBeneficiaryChange: (String) -> Unit,
    onChequeCcpChange: (String) -> Unit,
    onChequeKeyChange: (String) -> Unit,
    onChequePlaceChange: (String) -> Unit,
    onChequeDateChange: (String) -> Unit,
    onChequeNumberChange: (String) -> Unit,
    onChequeHolderChange: (String) -> Unit,
    onChequeMoiMemeClick: () -> Unit,
    onSfpCcpChange: (String) -> Unit,
    onSfpKeyChange: (String) -> Unit,
    onSfpAmountChange: (String) -> Unit,
    onSfpOperationChange: (SfpOperationType) -> Unit,
    onSfpReleveOperationsChange: (Boolean) -> Unit,
    onSfpAvecSoldeChange: (Boolean) -> Unit,
    onSfpNotificationAvoirChange: (Boolean) -> Unit,
    onSfpCommandeCarnetChange: (Boolean) -> Unit,
    onSfpSenderNomChange: (String) -> Unit,
    onSfpSenderPrenomChange: (String) -> Unit,
    onSfpSenderAddressChange: (String) -> Unit,
    onSfpBeneficiaryNomChange: (String) -> Unit,
    onSfpBeneficiaryPrenomChange: (String) -> Unit,
    onSfpBeneficiaryAddressChange: (String) -> Unit,
    onSfpMotifChange: (String) -> Unit,
    onSfpPlaceChange: (String) -> Unit,
    onSfpDateChange: (String) -> Unit,
    onSfpIdDescriptionChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 540.dp)
            .fillMaxWidth()
            .testTag("floating_opposite_editor"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resolveFloatingFieldTitle(fieldName),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("floating_editor_done")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "تم",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            when (fieldName) {
                "chequeAmount", "sfpAmount" -> {
                    val value = if (fieldName == "chequeAmount") uiState.chequeAmount else uiState.sfpAmount
                    val onValueChange = if (fieldName == "chequeAmount") onChequeAmountChange else onSfpAmountChange
                    val suggestions = remember(value) { PostalAmountSuggester.suggestGridAmounts(value) }
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text("المبلغ بالدينار (DA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("floating_amount_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suggestions) { suggested ->
                            AssistChip(
                                onClick = { onValueChange(suggested.toString()) },
                                label = {
                                    Text(
                                        text = "${PostalAmountSuggester.formatGridAmount(suggested)} دج",
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                "chequeBeneficiary" -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.chequeBeneficiary.equals("لنفسي", true) ||
                                    uiState.chequeBeneficiary.equals("Moi-même", true),
                                onClick = onChequeMoiMemeClick,
                                label = { Text("لنفسي / Moi-même", fontSize = 11.sp) }
                            )
                        }
                        items(uiState.savedProfiles) { profile ->
                            AssistChip(
                                onClick = { onSelectProfile(profile, PostalProfileRole.BENEFICIARY) },
                                label = { Text(profile.profileName, fontSize = 11.sp, maxLines = 1) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = uiState.chequeBeneficiary,
                        onValueChange = onChequeBeneficiaryChange,
                        label = { Text("لأمر / A l'ordre de") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                "sfpOperation", "sfpJustificatifCcp", "sfpAvisCredit", "sfpCarnetCheques", "sfpReleveDu", "sfpReleveAu" -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.sfpOperation == SfpOperationType.VERSEMENT,
                                onClick = { onSfpOperationChange(SfpOperationType.VERSEMENT) },
                                label = { Text("دفع", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.sfpOperation == SfpOperationType.RETRAIT,
                                onClick = { onSfpOperationChange(SfpOperationType.RETRAIT) },
                                label = { Text("سحب", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.sfpReleveOperations,
                                onClick = { onSfpReleveOperationsChange(!uiState.sfpReleveOperations) },
                                label = { Text("كشف العمليات", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.sfpAvecSolde,
                                onClick = { onSfpAvecSoldeChange(!uiState.sfpAvecSolde) },
                                label = { Text("إظهار الرصيد", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.sfpNotificationAvoir,
                                onClick = { onSfpNotificationAvoirChange(!uiState.sfpNotificationAvoir) },
                                label = { Text("إشعار بالرصيد", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.sfpCommandeCarnet,
                                onClick = { onSfpCommandeCarnetChange(!uiState.sfpCommandeCarnet) },
                                label = { Text("طلب دفتر", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                "chequeCcp", "chequeKey", "sfpCcp", "sfpKey" -> {
                    val isC = fieldName.startsWith("cheque")
                    val ccpVal = if (isC) uiState.chequeCcp else uiState.sfpCcp
                    val keyVal = if (isC) uiState.chequeKey else uiState.sfpKey
                    val onCcp = if (isC) onChequeCcpChange else onSfpCcpChange
                    val onKey = if (isC) onChequeKeyChange else onSfpKeyChange
                    if (uiState.savedProfiles.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(uiState.savedProfiles) { profile ->
                                AssistChip(
                                    onClick = {
                                        val role = if (isC || uiState.sfpOperation == SfpOperationType.RETRAIT) {
                                            PostalProfileRole.SELF
                                        } else {
                                            PostalProfileRole.BENEFICIARY
                                        }
                                        onSelectProfile(profile, role)
                                    },
                                    label = { Text(profile.profileName, fontSize = 11.sp, maxLines = 1) }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ccpVal,
                            onValueChange = onCcp,
                            label = { Text("رقم CCP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = keyVal,
                            onValueChange = onKey,
                            label = { Text("Clé") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                "chequeNumber" -> {
                    OutlinedTextField(
                        value = uiState.chequeNumber,
                        onValueChange = onChequeNumberChange,
                        label = { Text("رقم الصك (Chèque n°)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                "chequePlace" -> {
                    OutlinedTextField(
                        value = uiState.chequePlace,
                        onValueChange = onChequePlaceChange,
                        label = { Text("المكان") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                "chequeDate", "sfpDate", "sfpPlace", "sfpPlaceDate" -> {
                    val dateVal = if (fieldName == "chequeDate") uiState.chequeDate else uiState.sfpDate
                    val onDate = if (fieldName == "chequeDate") onChequeDateChange else onSfpDateChange
                    if (fieldName == "sfpPlace") {
                        OutlinedTextField(
                            value = uiState.sfpPlace,
                            onValueChange = onSfpPlaceChange,
                            label = { Text("المكان") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = dateVal,
                            onValueChange = onDate,
                            label = { Text("التاريخ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                "sfpMotif" -> {
                    val motifs = listOf("مصاريف شخصية", "أمانة / عائلة", "إيجار", "تسديد فاتورة")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(motifs) { motif ->
                            FilterChip(
                                selected = uiState.sfpMotif == motif,
                                onClick = { onSfpMotifChange(motif) },
                                label = { Text(motif, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = uiState.sfpMotif,
                        onValueChange = onSfpMotifChange,
                        label = { Text("سبب العملية (Motif)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                "sfpSenderNom", "sfpSenderPrenom", "sfpSenderAddress", "sfpSenderName" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.sfpSenderNom,
                            onValueChange = onSfpSenderNomChange,
                            label = { Text("اللقب (Nom)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = uiState.sfpSenderPrenom,
                            onValueChange = onSfpSenderPrenomChange,
                            label = { Text("الاسم (Prénom)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = uiState.sfpSenderAddress,
                        onValueChange = onSfpSenderAddressChange,
                        label = { Text("عنوان المرسل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                "sfpBeneficiaryNom", "sfpBeneficiaryPrenom", "sfpBeneficiaryAddress", "sfpBeneficiaryName" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.sfpBeneficiaryNom,
                            onValueChange = onSfpBeneficiaryNomChange,
                            label = { Text("لقب المستفيد (Nom)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = uiState.sfpBeneficiaryPrenom,
                            onValueChange = onSfpBeneficiaryPrenomChange,
                            label = { Text("اسم المستفيد (Prénom)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = uiState.sfpBeneficiaryAddress,
                        onValueChange = onSfpBeneficiaryAddressChange,
                        label = { Text("عنوان المستفيد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                "sfpIdDescription" -> {
                    OutlinedTextField(
                        value = uiState.sfpIdDescription,
                        onValueChange = onSfpIdDescriptionChange,
                        label = { Text("بيانات بطاقة الهوية (P.I / N°)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                else -> {
                    OutlinedTextField(
                        value = uiState.chequeAccountHolder,
                        onValueChange = onChequeHolderChange,
                        label = { Text("صاحب الحساب (M. / Mme)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

internal fun isDocumentTopHalfField(fieldName: String?): Boolean {
    return fieldName in setOf(
        "chequeNumber",
        "chequeAmount",
        "sfpCcp",
        "sfpKey",
        "sfpOperation",
        "sfpAmount",
        "sfpJustificatifCcp",
        "sfpAvisCredit",
        "sfpCarnetCheques",
        "sfpReleveDu",
        "sfpReleveAu"
    )
}

private fun resolveFloatingFieldTitle(fieldName: String): String {
    return when (fieldName) {
        "chequeNumber" -> "رقم الصك (Chèque n°)"
        "chequeAmount", "sfpAmount" -> "تعديل المبلغ (DA)"
        "chequeBeneficiary" -> "المستفيد (لأمر / A l'ordre de)"
        "chequePlace", "sfpPlace" -> "المكان"
        "chequeDate", "sfpDate", "sfpPlaceDate" -> "التاريخ"
        "chequeCcp", "chequeKey", "sfpCcp", "sfpKey" -> "رقم الحساب CCP والمفتاح"
        "chequeSignature", "sfpSignature" -> "بيانات صاحب الحساب / الإمضاء"
        "sfpOperation", "sfpJustificatifCcp", "sfpAvisCredit", "sfpCarnetCheques", "sfpReleveDu", "sfpReleveAu" -> "العملية والخدمات البريدية"
        "sfpSenderNom", "sfpSenderPrenom", "sfpSenderAddress", "sfpSenderName" -> "بيانات المرسل (Expéditeur)"
        "sfpBeneficiaryNom", "sfpBeneficiaryPrenom", "sfpBeneficiaryAddress", "sfpBeneficiaryName" -> "بيانات المستفيد (Bénéficiaire)"
        "sfpMotif" -> "سبب العملية (Motif)"
        "sfpIdDescription" -> "بطاقة الهوية (P.I)"
        else -> "تعديل الحقل"
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
