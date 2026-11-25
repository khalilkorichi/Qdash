package com.qdash.presentation.simulator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qdash.presentation.simulator.DocumentSimulatorUiState
import com.qdash.presentation.simulator.SfpOperationType
import com.qdash.ui.designsystem.components.FormGrid
import com.qdash.ui.designsystem.tokens.ColorTokens

/**
 * Smart expandable SFP 01 preview card:
 * - Compact bounded preview (~252.dp) with live auto-focus badge, smooth auto-scroll to active field,
 *   and bottom expand pill.
 * - Full-screen Dialog with pinch-to-zoom (1f..3f), pan, quick 1x/1.5x/2x zoom chips, and optional
 *   AR/FR amount-in-words toggle.
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
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = { next ->
        if (onExpandedChange != null) onExpandedChange(next) else internalExpanded = next
    }
    val compactScrollState = rememberScrollState()
    val focusLabel = remember(focusedField) { resolveFocusFieldLabel(focusedField) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Smoothly auto-scroll compact preview to keep the currently focused SFP 01 section visible
    LaunchedEffect(focusedField, compactScrollState.maxValue, isExpanded) {
        if (!isExpanded && focusedField != null && compactScrollState.maxValue > 0) {
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
        border = BorderStroke(
            1.dp,
            if (isDark) ColorTokens.PosteYellow.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Compact preview header bar with active focus indicator and expand button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { setExpanded(true) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val hasFocus = focusLabel != null
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasFocus) ColorTokens.Warning
                                else if (isDark) ColorTokens.InfoDark
                                else ColorTokens.PosteNavyStripe
                            )
                    )
                    Text(
                        text = if (hasFocus) "التركيز: $focusLabel" else "استمارة SFP 01 — معاينة حية",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (hasFocus) {
                                if (isDark) ColorTokens.WarningDark else Color(0xFFB45309)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = { setExpanded(true) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("sfp01_expand_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "تكبير الاستمارة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "تكبير",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            // Bounded compact preview viewport (~248.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(248.dp)
            ) {
                if (!isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(compactScrollState)
                            .padding(8.dp)
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
                            isPreviewReadOnly = isPreviewReadOnly
                        )
                    }
                }

                // Bottom fade gradient + zoom cue pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFFEF8CE).copy(alpha = 0.85f),
                                    Color(0xFFFEF8CE)
                                )
                            )
                        )
                        .clickable { setExpanded(true) },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        onClick = { setExpanded(true) },
                        color = Color(0xFF0F172A).copy(alpha = 0.86f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = Color(0xFFF8C91C),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "معاينة مدمجة — اضغط للتكبير",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (isExpanded) {
        Sfp01FullscreenDialog(
            uiState = uiState,
            focusLabel = focusLabel,
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
            onDismiss = { setExpanded(false) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Sfp01FullscreenDialog(
    uiState: DocumentSimulatorUiState,
    focusLabel: String?,
    onCcpChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onSenderNomChange: (String) -> Unit,
    onSenderPrenomChange: (String) -> Unit,
    onSenderAddressChange: (String) -> Unit,
    onSenderPhoneChange: (String) -> Unit,
    onBeneficiaryNomChange: (String) -> Unit,
    onBeneficiaryPrenomChange: (String) -> Unit,
    onBeneficiaryAddressChange: (String) -> Unit,
    onBeneficiaryPhoneChange: (String) -> Unit,
    onPlaceChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onIdDescriptionChange: (String) -> Unit,
    onOperationChange: (SfpOperationType) -> Unit,
    onJustificatifCcpChange: (Boolean) -> Unit,
    onAvisCreditChange: (Boolean) -> Unit,
    onCarnetChequesChange: (Boolean) -> Unit,
    onCodeConfidentielChange: (Boolean) -> Unit,
    onRipChange: (Boolean) -> Unit,
    focusedField: String?,
    onFieldFocus: (String?) -> Unit,
    onMotifChange: (String) -> Unit,
    sfpReleveOperations: Boolean,
    onReleveOperationsChange: (Boolean) -> Unit,
    sfpAvecSolde: Boolean,
    onAvecSoldeChange: (Boolean) -> Unit,
    sfpNotificationAvoir: Boolean,
    onNotificationAvoirChange: (Boolean) -> Unit,
    sfpCommandeCarnet: Boolean,
    onCommandeCarnetChange: (Boolean) -> Unit,
    sfpReleveDu: String,
    onReleveDuChange: (String) -> Unit,
    sfpReleveAu: String,
    onReleveAuChange: (String) -> Unit,
    sfpPiType: String,
    sfpPiNumero: String,
    sfpPiDelivreLe: String,
    sfpPiPar: String,
    amountInWords: String?,
    isFrenchWords: Boolean,
    onToggleWordsLanguage: (() -> Unit)?,
    isPreviewReadOnly: Boolean,
    onDismiss: () -> Unit
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (zoomScale * zoomChange).coerceIn(1f, 3f)
        zoomScale = nextScale
        if (nextScale > 1.02f) {
            panOffset += panChange
        } else {
            panOffset = Offset.Zero
        }
    }
    val verticalScroll = rememberScrollState()

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
                .testTag("sfp01_fullscreen_dialog"),
            color = Color(0xFF0E131F)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top action bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161E2E))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق المعاينة",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "استمارة SFP 01 — معاينة كاملة",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        AnimatedVisibility(visible = focusLabel != null, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = "التركيز: ${focusLabel.orEmpty()}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    if (onToggleWordsLanguage != null) {
                        Surface(
                            onClick = onToggleWordsLanguage,
                            color = Color(0xFFF8C91C).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFF8C91C).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "تبديل لغة الحروف",
                                    tint = Color(0xFFF8C91C),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isFrenchWords) "FR ⇄ عربي" else "عربي ⇄ FR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF8C91C)
                                    )
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // Interactive pinch-to-zoom & pan SFP 01 form canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(0.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (zoomScale > 1.05f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2f
                                    }
                                }
                            )
                        }
                        .transformable(
                            state = transformableState,
                            canPan = { zoomScale > 1.02f }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScroll, enabled = zoomScale <= 1.05f)
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        contentAlignment = Alignment.TopCenter
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
                }

                // Bottom quick zoom controls bar (1x / 1.5x / 2x)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161E2E))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1f to "1x", 1.5f to "1.5x", 2f to "2x").forEach { (preset, label) ->
                            val selected = kotlin.math.abs(zoomScale - preset) < 0.15f
                            Surface(
                                onClick = {
                                    zoomScale = preset
                                    if (preset == 1f) panOffset = Offset.Zero
                                },
                                color = if (selected) Color(0xFFF8C91C) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.border(
                                    1.dp,
                                    if (selected) Color(0xFFF8C91C) else Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color(0xFF111827) else Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (zoomScale > 1.05f || panOffset != Offset.Zero) {
                            IconButton(
                                onClick = {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = "إعادة ضبط التكبير",
                                    tint = Color(0xFFF8C91C)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "اسحب بإصبعين للتكبير",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveFocusFieldLabel(focusedField: String?): String? {
    return when (focusedField) {
        "sfpCcp" -> "رقم الحساب (CCP)"
        "sfpKey" -> "المفتاح (Clé)"
        "sfpOperation",
        "sfpJustificatifCcp",
        "sfpAvisCredit",
        "sfpCarnetCheques",
        "sfpCodeConfidentiel",
        "sfpRip",
        "sfpReleveDu",
        "sfpReleveAu" -> "نوع العملية المالية"
        "sfpAmount" -> "المبلغ بالأرقام والحروف"
        "sfpSenderNom", "sfpSenderName" -> "لقب المرسل"
        "sfpSenderPrenom" -> "اسم المرسل"
        "sfpSenderAddress", "sfpSenderPhone" -> "عنوان المرسل"
        "sfpBeneficiaryNom", "sfpBeneficiaryName" -> "لقب المستفيد"
        "sfpBeneficiaryPrenom" -> "اسم المستفيد"
        "sfpBeneficiaryAddress", "sfpBeneficiaryPhone" -> "عنوان المستفيد"
        "sfpMotif" -> "سبب العملية (Motif)"
        "sfpDate", "sfpPlace", "sfpPlaceDate" -> "التاريخ والمكان"
        "sfpSignature" -> "الإمضاء"
        "sfpIdDescription" -> "بيانات بطاقة الهوية"
        else -> null
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
        "sfpReleveAu",
        "sfpAmount" -> 0.16f
        "sfpSenderNom",
        "sfpSenderPrenom",
        "sfpSenderName",
        "sfpSenderAddress",
        "sfpSenderPhone",
        "sfpBeneficiaryNom",
        "sfpBeneficiaryPrenom",
        "sfpBeneficiaryName",
        "sfpBeneficiaryAddress",
        "sfpBeneficiaryPhone" -> 0.45f
        "sfpMotif", "sfpDate", "sfpPlace", "sfpPlaceDate", "sfpSignature" -> 0.68f
        "sfpIdDescription" -> 0.86f
        else -> 0f
    }
}
