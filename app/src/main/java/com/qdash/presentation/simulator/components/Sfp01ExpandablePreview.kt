package com.qdash.presentation.simulator.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.qdash.presentation.simulator.DocumentSimulatorUiState
import com.qdash.presentation.simulator.SfpOperationType
import com.qdash.ui.designsystem.components.FormGrid

/**
 * Compact Chèque Secours (SFP 01) preview card keeping the exact 1:1 FormGrid visual design
 * while offering smooth 2-finger pinch-to-zoom, active-field auto-scroll, and two very
 * translucent floating bottom-right buttons (Fullscreen Landscape Rotate + Educational Guide).
 */
@Composable
fun Sfp01ExpandablePreview(
    uiState: DocumentSimulatorUiState,
    onCcpChange: (String) -> Unit = {},
    onKeyChange: (String) -> Unit = {},
    onAmountChange: (String) -> Unit = {},
    onSenderNomChange: (String) -> Unit = {},
    onSenderPrenomChange: (String) -> Unit = {},
    onSenderAddressChange: (String) -> Unit = {},
    onSenderPhoneChange: (String) -> Unit = {},
    onBeneficiaryNomChange: (String) -> Unit = {},
    onBeneficiaryPrenomChange: (String) -> Unit = {},
    onBeneficiaryAddressChange: (String) -> Unit = {},
    onBeneficiaryPhoneChange: (String) -> Unit = {},
    onPlaceChange: (String) -> Unit = {},
    onDateChange: (String) -> Unit = {},
    onIdDescriptionChange: (String) -> Unit = {},
    onOperationChange: (SfpOperationType) -> Unit = {},
    onJustificatifCcpChange: (Boolean) -> Unit = {},
    onAvisCreditChange: (Boolean) -> Unit = {},
    onCarnetChequesChange: (Boolean) -> Unit = {},
    onCodeConfidentielChange: (Boolean) -> Unit = {},
    onRipChange: (Boolean) -> Unit = {},
    focusedField: String? = uiState.focusedField,
    onFieldFocus: (String?) -> Unit = {},
    onMotifChange: (String) -> Unit = {},
    sfpReleveOperations: Boolean = false,
    onReleveOperationsChange: (Boolean) -> Unit = {},
    sfpAvecSolde: Boolean = false,
    onAvecSoldeChange: (Boolean) -> Unit = {},
    sfpNotificationAvoir: Boolean = false,
    onNotificationAvoirChange: (Boolean) -> Unit = {},
    sfpCommandeCarnet: Boolean = false,
    onCommandeCarnetChange: (Boolean) -> Unit = {},
    sfpReleveDu: String = "",
    onReleveDuChange: (String) -> Unit = {},
    sfpReleveAu: String = "",
    onReleveAuChange: (String) -> Unit = {},
    sfpPiType: String = "",
    sfpPiNumero: String = "",
    sfpPiDelivreLe: String = "",
    sfpPiPar: String = "",
    amountInWords: String? = null,
    isFrenchWords: Boolean = false,
    onToggleWordsLanguage: (() -> Unit)? = null,
    isPreviewReadOnly: Boolean = true,
    isGuideActive: Boolean = false,
    onRotateExpandClick: () -> Unit = {},
    onToggleGuideClick: () -> Unit = {},
    onZoomChanged: (Boolean) -> Unit = {},
    previewHeight: androidx.compose.ui.unit.Dp = 264.dp,
    modifier: Modifier = Modifier
) {
    val compactScrollState = rememberScrollState()
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(zoomScale) {
        onZoomChanged(zoomScale > 1.05f)
    }

    // Smoothly auto-scroll compact preview to keep the currently focused Chèque Secours section visible
    LaunchedEffect(focusedField, compactScrollState.maxValue) {
        if (focusedField != null && compactScrollState.maxValue > 0) {
            val targetFraction = resolveFocusScrollFraction(focusedField)
            val targetY = (compactScrollState.maxValue * targetFraction).toInt()
            compactScrollState.animateScrollTo(targetY)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sfp01_preview_card")
            .shadow(3.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF6E6)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .clip(RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoomScale > 1.05f) {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            } else {
                                zoomScale = 1.6f
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
                    maxScale = 2.6f
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(compactScrollState, enabled = zoomScale <= 1.05f)
                    .padding(6.dp)
            ) {
                FormGrid(
                    uiState = uiState,
                    onCcpChange = onCcpChange,
                    onKeyChange = onKeyChange,
                    onAmountChange = onAmountChange,
                    onSenderNomChange = onSenderNomChange,
                    onSenderPrenomChange = onSenderPrenomChange,
                    onSenderAddressChange = onSenderAddressChange,
                    onSenderPhoneChange = onSenderPhoneChange,
                    onBeneficiaryNomChange = onBeneficiaryNomChange,
                    onBeneficiaryPrenomChange = onBeneficiaryPrenomChange,
                    onBeneficiaryAddressChange = onBeneficiaryAddressChange,
                    onBeneficiaryPhoneChange = onBeneficiaryPhoneChange,
                    onPlaceChange = onPlaceChange,
                    onDateChange = onDateChange,
                    onIdDescriptionChange = onIdDescriptionChange,
                    onOperationChange = onOperationChange,
                    onJustificatifCcpChange = onJustificatifCcpChange,
                    onAvisCreditChange = onAvisCreditChange,
                    onCarnetChequesChange = onCarnetChequesChange,
                    onCodeConfidentielChange = onCodeConfidentielChange,
                    onRipChange = onRipChange,
                    focusedField = focusedField,
                    onFieldFocus = onFieldFocus,
                    onMotifChange = onMotifChange,
                    sfpReleveOperations = sfpReleveOperations,
                    onReleveOperationsChange = onReleveOperationsChange,
                    sfpAvecSolde = sfpAvecSolde,
                    onAvecSoldeChange = onAvecSoldeChange,
                    sfpNotificationAvoir = sfpNotificationAvoir,
                    onNotificationAvoirChange = onNotificationAvoirChange,
                    sfpCommandeCarnet = sfpCommandeCarnet,
                    onCommandeCarnetChange = onCommandeCarnetChange,
                    sfpReleveDu = sfpReleveDu,
                    onReleveDuChange = onReleveDuChange,
                    sfpReleveAu = sfpReleveAu,
                    onReleveAuChange = onReleveAuChange,
                    sfpPiType = sfpPiType,
                    sfpPiNumero = sfpPiNumero,
                    sfpPiDelivreLe = sfpPiDelivreLe,
                    sfpPiPar = sfpPiPar,
                    amountInWords = amountInWords,
                    isFrenchWords = isFrenchWords,
                    onToggleWordsLanguage = onToggleWordsLanguage,
                    isPreviewReadOnly = isPreviewReadOnly,
                    modifier = Modifier.graphicsLayer(
                        scaleX = zoomScale,
                        scaleY = zoomScale,
                        translationX = panOffset.x,
                        translationY = panOffset.y
                    )
                )
            }

            TranslucentPreviewFloatingActions(
                isGuideActive = isGuideActive,
                onRotateExpandClick = onRotateExpandClick,
                onToggleGuideClick = onToggleGuideClick,
                expandButtonTestTag = "sfp01_expand_button",
                guideButtonTestTag = "sfp01_guide_button",
                modifier = Modifier.align(AbsoluteAlignment.BottomRight)
            )
        }
    }
}

private fun resolveFocusScrollFraction(focusedField: String): Float {
    return when (focusedField) {
        "sfpCcp", "sfpKey" -> 0.04f
        "sfpOperation",
        "sfpJustificatifCcp",
        "sfpAvisCredit",
        "sfpCarnetCheques",
        "sfpCodeConfidentiel",
        "sfpRip",
        "sfpReleveDu",
        "sfpReleveAu" -> 0.14f
        "sfpAmount" -> 0.18f
        "sfpSenderNom", "sfpSenderPrenom", "sfpSenderAddress", "sfpSenderName", "sfpSenderPhone" -> 0.34f
        "sfpBeneficiaryNom", "sfpBeneficiaryPrenom", "sfpBeneficiaryAddress", "sfpBeneficiaryName", "sfpBeneficiaryPhone" -> 0.36f
        "sfpMotif", "sfpDate", "sfpPlace", "sfpPlaceDate", "sfpSignature" -> 0.56f
        "sfpIdDescription" -> 0.78f
        else -> 0f
    }
}
