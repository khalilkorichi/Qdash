package com.qdash.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.R
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.presentation.simulator.DocumentSimulatorUiState
import com.qdash.presentation.simulator.SfpOperationType
import com.qdash.ui.designsystem.tokens.ColorTokens

// Official Algérie Poste SFP 01 physical paper palette
private val SfpPaperCream = Color(0xFFFEF8CE)
private val SfpBannerYellow = Color(0xFFF8C91C)
private val SfpNavyStripe = Color(0xFF0D47A1)
private val SfpGridBorder = Color(0xFF2E2C2A)
private val SfpBoxBorder = Color(0xFFE0C65A)
private val SfpLabelInk = Color(0xFF1F1D1A)
private val SfpPenBlue = Color(0xFF0F2B8C)
private val SfpFocusAmber = Color(0xFFF59E0B)
private val SfpFocusWash = Color(0xFFF59E0B).copy(alpha = 0.14f)
private val SfpWatermarkArc = Color(0xFFE5E8EE)

@Composable
fun FormCell(
    modifier: Modifier = Modifier,
    labelArabic: String? = null,
    labelFrench: String? = null,
    labelColor: Color = ColorTokens.SfpBorderBrown,
    borderColor: Color = ColorTokens.SfpBorderBrown,
    isFocused: Boolean = false,
    onTap: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .border(0.5.dp, borderColor)
            .background(if (isFocused) ColorTokens.Warning.copy(alpha = 0.08f) else Color.Transparent)
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (labelFrench != null) {
                    Text(
                        text = labelFrench,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.1.sp
                        ),
                        color = labelColor,
                        textAlign = TextAlign.Start
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (labelArabic != null) {
                    Text(
                        text = labelArabic,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = labelColor,
                        textAlign = TextAlign.End
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomStart
            ) {
                content()
            }
        }
    }
}

/**
 * 1:1 photorealistic representation of the official Algérie Poste SFP 01 form.
 * Locked to physical LTR coordinates regardless of app locale so left/right columns
 * match the printed paper slip.
 */
@Composable
fun FormGrid(
    uiState: DocumentSimulatorUiState,
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
    isPreviewReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val formattedAmount = remember(uiState.sfpAmount) {
        val amt = AmountConversionEngine.parseFlexibleAmount(uiState.sfpAmount)
        if (amt != null && amt > 0.0) AmountConversionEngine.formatAmountToPostal(amt) else ""
    }

    val resolvedWords = remember(amountInWords, uiState.sfpAmount, isFrenchWords) {
        if (!amountInWords.isNullOrBlank()) {
            amountInWords
        } else {
            val amt = AmountConversionEngine.parseFlexibleAmount(uiState.sfpAmount)
            if (amt != null && amt > 0.0) {
                if (isFrenchWords) {
                    AmountConversionEngine.convertToFrenchWords(amt)
                } else {
                    AmountConversionEngine.convertToArabicWords(amt, preferCentimes = false)
                }
            } else {
                ""
            }
        }
    }

    // Physical LTR lock ensures left/right form geometry matches the printed Algerian post slip
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag("sfp01_form_grid")
                .background(Color.White)
                .border(1.dp, SfpGridBorder)
        ) {
            // 1. Top white micro-row with SFP 01 code on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SFP 01",
                    style = TextStyle(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = SfpLabelInk,
                        letterSpacing = 0.3.sp
                    )
                )
            }

            // 2. Yellow Algérie Poste header bar + central white cutout + crisp navy-blue stripe
            SfpHeaderBanner()

            // Main cream paper body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SfpPaperCream)
            ) {
                // 3. Account row: (1) Compte N° / ح ب ج رقم + Clé / المفتاح
                SfpAccountRow(
                    ccp = uiState.sfpCcp,
                    key = uiState.sfpKey,
                    onCcpChange = onCcpChange,
                    onKeyChange = onKeyChange,
                    focusedField = focusedField,
                    onFieldFocus = onFieldFocus,
                    isPreviewReadOnly = isPreviewReadOnly
                )

                SfpHorizontalDivider()

                // 4. Upper 2-column grid: Checkboxes (left) | Amounts (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                ) {
                    SfpOperationsCell(
                        sfpOperation = uiState.sfpOperation,
                        onOperationChange = onOperationChange,
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
                        isFocused = focusedField in OPERATION_FOCUS_FIELDS,
                        onFieldFocus = onFieldFocus,
                        isPreviewReadOnly = isPreviewReadOnly,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    SfpVerticalDivider()

                    SfpAmountCell(
                        rawAmount = uiState.sfpAmount,
                        formattedAmount = formattedAmount,
                        amountInWords = resolvedWords,
                        isFrenchWords = isFrenchWords,
                        onAmountChange = onAmountChange,
                        onToggleWordsLanguage = onToggleWordsLanguage,
                        isFocused = focusedField == "sfpAmount",
                        onFieldFocus = onFieldFocus,
                        isPreviewReadOnly = isPreviewReadOnly,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                SfpHorizontalDivider()

                // 5. Middle 2-column grid: Expéditeur (left) | Bénéficiaire (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                ) {
                    SfpPartyColumn(
                        frenchHeader = "(3) Expéditeur / Donneur d'ordre",
                        arabicHeader = "المرسل",
                        nom = uiState.sfpSenderNom,
                        prenom = uiState.sfpSenderPrenom,
                        address = uiState.sfpSenderAddress,
                        onNomChange = onSenderNomChange,
                        onPrenomChange = onSenderPrenomChange,
                        onAddressChange = onSenderAddressChange,
                        nomFocused = focusedField == "sfpSenderNom" || focusedField == "sfpSenderName",
                        prenomFocused = focusedField == "sfpSenderPrenom" || focusedField == "sfpSenderName",
                        addressFocused = focusedField == "sfpSenderAddress",
                        onNomTap = { onFieldFocus("sfpSenderNom") },
                        onPrenomTap = { onFieldFocus("sfpSenderPrenom") },
                        onAddressTap = { onFieldFocus("sfpSenderAddress") },
                        isPreviewReadOnly = isPreviewReadOnly,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    SfpVerticalDivider()

                    SfpPartyColumn(
                        frenchHeader = "(3) Bénéficiaire",
                        arabicHeader = "المستفيد",
                        nom = uiState.sfpBeneficiaryNom,
                        prenom = uiState.sfpBeneficiaryPrenom,
                        address = uiState.sfpBeneficiaryAddress,
                        onNomChange = onBeneficiaryNomChange,
                        onPrenomChange = onBeneficiaryPrenomChange,
                        onAddressChange = onBeneficiaryAddressChange,
                        nomFocused = focusedField == "sfpBeneficiaryNom" || focusedField == "sfpBeneficiaryName",
                        prenomFocused = focusedField == "sfpBeneficiaryPrenom" || focusedField == "sfpBeneficiaryName",
                        addressFocused = focusedField == "sfpBeneficiaryAddress",
                        onNomTap = { onFieldFocus("sfpBeneficiaryNom") },
                        onPrenomTap = { onFieldFocus("sfpBeneficiaryPrenom") },
                        onAddressTap = { onFieldFocus("sfpBeneficiaryAddress") },
                        isPreviewReadOnly = isPreviewReadOnly,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                SfpHorizontalDivider()

                // 6. Lower user row: Motif / Correspondance (left) | Signature & Date (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                ) {
                    SfpMotifCell(
                        motif = uiState.sfpMotif,
                        onMotifChange = onMotifChange,
                        isFocused = focusedField == "sfpMotif",
                        onTap = { onFieldFocus("sfpMotif") },
                        isPreviewReadOnly = isPreviewReadOnly,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    SfpVerticalDivider()

                    SfpSignatureAndDateCell(
                        date = uiState.sfpDate,
                        place = uiState.sfpPlace,
                        onDateChange = onDateChange,
                        onPlaceChange = onPlaceChange,
                        isSignatureFocused = focusedField == "sfpSignature",
                        isDateFocused = focusedField == "sfpDate" || focusedField == "sfpPlaceDate" || focusedField == "sfpPlace",
                        onSignatureTap = { onFieldFocus("sfpSignature") },
                        onDateTap = { onFieldFocus("sfpDate") },
                        isPreviewReadOnly = isPreviewReadOnly,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // 7. Official yellow banner: Cadre réservé au bureau de poste
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SfpBannerYellow)
                        .border(0.8.dp, SfpGridBorder)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cadre réservé au bureau de poste",
                        style = TextStyle(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SfpLabelInk
                        )
                    )
                    Text(
                        text = "إطار مخصص لمكتب البريد",
                        style = TextStyle(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SfpLabelInk,
                            textDirection = TextDirection.Content
                        )
                    )
                }

                // 8. Post office reserved 2-column grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                ) {
                    SfpIdentityCell(
                        sfpPiType = sfpPiType,
                        sfpPiNumero = sfpPiNumero,
                        sfpPiDelivreLe = sfpPiDelivreLe,
                        sfpPiPar = sfpPiPar,
                        fallbackIdDescription = uiState.sfpIdDescription,
                        onIdDescriptionChange = onIdDescriptionChange,
                        isFocused = focusedField == "sfpIdDescription",
                        onTap = { onFieldFocus("sfpIdDescription") },
                        isPreviewReadOnly = isPreviewReadOnly,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    SfpVerticalDivider()

                    SfpPostOfficeStampCell(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                SfpHorizontalDivider()

                // 9. Footnotes row directly above perforated tear line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "(1) Obligatoire إجباري",
                        style = TextStyle(fontSize = 5.3.sp, color = SfpLabelInk, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    Text(
                        text = "(2) Cocher la ou les cases إشطب الخانة أو الخانات",
                        style = TextStyle(fontSize = 5.3.sp, color = SfpLabelInk, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    Text(
                        text = "(3) Obligatoire dans le cas d'un versement إجباري في حالة الدفع",
                        style = TextStyle(fontSize = 5.3.sp, color = SfpLabelInk, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Perforated dashed tear line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(SfpPaperCream)
            ) {
                drawLine(
                    color = SfpGridBorder,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            }

            // 10. Receipt bar + watermarked white stub
            SfpReceiptStub()
        }
    }
}

private val OPERATION_FOCUS_FIELDS = setOf(
    "sfpOperation",
    "sfpReleveDu",
    "sfpReleveAu",
    "sfpJustificatifCcp",
    "sfpAvisCredit",
    "sfpCarnetCheques",
    "sfpCodeConfidentiel",
    "sfpRip"
)

@Composable
private fun SfpHeaderBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(SfpBannerYellow)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Opérations financières postales",
                    style = TextStyle(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SfpLabelInk
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(42.dp))
                Text(
                    text = "العمليات المالية البريدية",
                    style = TextStyle(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SfpLabelInk,
                        textDirection = TextDirection.Content
                    ),
                    maxLines = 1
                )
            }
            // Crisp navy-blue horizontal stripe directly underneath yellow bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .background(SfpNavyStripe)
            )
        }

        // Central white cutout breaking through the yellow and blue bands
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .background(Color.White)
                .padding(horizontal = 6.dp, vertical = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.sfp_header_logo),
                contentDescription = "Algérie Poste",
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = "بريد الجزائر",
                style = TextStyle(
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Black,
                    color = SfpNavyStripe,
                    lineHeight = 7.sp
                )
            )
        }
    }
}

@Composable
private fun SfpAccountRow(
    ccp: String,
    key: String,
    onCcpChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    focusedField: String?,
    onFieldFocus: (String?) -> Unit,
    isPreviewReadOnly: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "(1)",
            style = TextStyle(fontSize = 6.5.sp, color = SfpLabelInk),
            modifier = Modifier.padding(end = 3.dp)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ح ب ج رقم",
                style = TextStyle(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk,
                    lineHeight = 8.5.sp
                )
            )
            Text(
                text = "Compte N°",
                style = TextStyle(
                    fontSize = 7.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SfpLabelInk,
                    lineHeight = 8.5.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        SfpWhiteInputBox(
            value = ccp,
            onValueChange = onCcpChange,
            placeholder = "",
            isFocused = focusedField == "sfpCcp",
            onTap = { onFieldFocus("sfpCcp") },
            isPreviewReadOnly = isPreviewReadOnly,
            keyboardType = KeyboardType.Number,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(1f)
                .height(21.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "المفتاح",
                style = TextStyle(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk,
                    lineHeight = 8.5.sp
                )
            )
            Text(
                text = "Clé",
                style = TextStyle(
                    fontSize = 7.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SfpLabelInk,
                    lineHeight = 8.5.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        SfpWhiteInputBox(
            value = key,
            onValueChange = onKeyChange,
            placeholder = "",
            isFocused = focusedField == "sfpKey",
            onTap = { onFieldFocus("sfpKey") },
            isPreviewReadOnly = isPreviewReadOnly,
            keyboardType = KeyboardType.Number,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(44.dp)
                .height(21.dp)
        )

        Spacer(modifier = Modifier.width(18.dp))
    }
}

@Composable
private fun SfpOperationsCell(
    sfpOperation: SfpOperationType,
    onOperationChange: (SfpOperationType) -> Unit,
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
    isFocused: Boolean,
    onFieldFocus: (String?) -> Unit,
    isPreviewReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .sfpFocusArea(isFocused)
            .clickable { onFieldFocus("sfpOperation") }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "(2)",
            style = TextStyle(fontSize = 6.sp, color = SfpLabelInk),
            modifier = Modifier.padding(start = 1.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left sub-column: Retrait, Versement, Relevé des opérations
            Column(
                modifier = Modifier.weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SfpFormCheckboxItem(
                    checked = sfpOperation == SfpOperationType.RETRAIT,
                    arabicLabel = "سحب",
                    frenchLabel = "Retrait",
                    onClick = {
                        onOperationChange(SfpOperationType.RETRAIT)
                        onFieldFocus("sfpOperation")
                    }
                )
                SfpFormCheckboxItem(
                    checked = sfpOperation == SfpOperationType.VERSEMENT,
                    arabicLabel = "دفع",
                    frenchLabel = "Versement",
                    onClick = {
                        onOperationChange(SfpOperationType.VERSEMENT)
                        onFieldFocus("sfpOperation")
                    }
                )
                SfpFormCheckboxItem(
                    checked = sfpReleveOperations,
                    arabicLabel = "كشف العمليات",
                    frenchLabel = "Relevé des opérations",
                    onClick = {
                        onReleveOperationsChange(!sfpReleveOperations)
                        onFieldFocus("sfpOperation")
                    }
                )
            }

            // Right sub-column: Avec notification de solde, Notification d'avoir, Commande de carnet
            Column(
                modifier = Modifier.weight(0.55f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SfpFormCheckboxItem(
                    checked = sfpAvecSolde,
                    arabicLabel = "مع إظهار الرصيد",
                    frenchLabel = "Avec notification de solde",
                    onClick = {
                        onAvecSoldeChange(!sfpAvecSolde)
                        onFieldFocus("sfpOperation")
                    }
                )
                SfpFormCheckboxItem(
                    checked = sfpNotificationAvoir,
                    arabicLabel = "إشعار بالرصيد",
                    frenchLabel = "Notification d'avoir",
                    onClick = {
                        onNotificationAvoirChange(!sfpNotificationAvoir)
                        onFieldFocus("sfpOperation")
                    }
                )
                SfpFormCheckboxItem(
                    checked = sfpCommandeCarnet,
                    arabicLabel = "طلب دفتر",
                    frenchLabel = "Commande de carnet",
                    onClick = {
                        onCommandeCarnetChange(!sfpCommandeCarnet)
                        onFieldFocus("sfpOperation")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Date range line: du ...../...../.... au ...../...../....
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "du ",
                style = TextStyle(fontSize = 6.2.sp, color = SfpLabelInk)
            )
            SfpInlineDateSegment(
                value = sfpReleveDu,
                placeholder = "...../...../....",
                onValueChange = onReleveDuChange,
                onTap = { onFieldFocus("sfpOperation") },
                isPreviewReadOnly = isPreviewReadOnly,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = " au ",
                style = TextStyle(fontSize = 6.2.sp, color = SfpLabelInk)
            )
            SfpInlineDateSegment(
                value = sfpReleveAu,
                placeholder = "...../...../....",
                onValueChange = onReleveAuChange,
                onTap = { onFieldFocus("sfpOperation") },
                isPreviewReadOnly = isPreviewReadOnly,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SfpFormCheckboxItem(
    checked: Boolean,
    arabicLabel: String,
    frenchLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.White)
                .border(0.8.dp, if (checked) SfpPenBlue else SfpBoxBorder),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "X",
                    style = TextStyle(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = SfpPenBlue,
                        lineHeight = 9.sp
                    )
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = arabicLabel,
                style = TextStyle(
                    fontSize = 6.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk,
                    lineHeight = 7.5.sp,
                    textDirection = TextDirection.Content
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = frenchLabel,
                style = TextStyle(
                    fontSize = 5.6.sp,
                    fontWeight = FontWeight.Medium,
                    color = SfpLabelInk,
                    lineHeight = 6.5.sp
                ),
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SfpInlineDateSegment(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onTap: () -> Unit,
    isPreviewReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPreviewReadOnly) {
        Text(
            text = value.ifBlank { placeholder },
            style = TextStyle(
                fontSize = if (value.isNotBlank()) 6.8.sp else 6.sp,
                fontWeight = if (value.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                fontFamily = if (value.isNotBlank()) FontFamily.Monospace else FontFamily.Default,
                color = if (value.isNotBlank()) SfpPenBlue else SfpLabelInk.copy(alpha = 0.75f)
            ),
            maxLines = 1,
            modifier = modifier.clickable(onClick = onTap)
        )
    } else {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(SfpPenBlue),
            textStyle = TextStyle(
                fontSize = 6.8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = SfpPenBlue
            ),
            modifier = modifier.onFocusChanged { if (it.isFocused) onTap() },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = 6.sp,
                                color = SfpLabelInk.copy(alpha = 0.75f)
                            ),
                            maxLines = 1
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun SfpAmountCell(
    rawAmount: String,
    formattedAmount: String,
    amountInWords: String,
    isFrenchWords: Boolean,
    onAmountChange: (String) -> Unit,
    onToggleWordsLanguage: (() -> Unit)?,
    isFocused: Boolean,
    onFieldFocus: (String?) -> Unit,
    isPreviewReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .sfpFocusArea(isFocused)
            .clickable { onFieldFocus("sfpAmount") }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: (1) Montant en chiffres + WHITE rectangular box + المبلغ بالأرقام
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "(1)",
                    style = TextStyle(fontSize = 6.sp, color = SfpLabelInk, lineHeight = 6.5.sp)
                )
                Text(
                    text = "Montant",
                    style = TextStyle(
                        fontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SfpLabelInk,
                        lineHeight = 7.5.sp
                    )
                )
                Text(
                    text = "en chiffres",
                    style = TextStyle(
                        fontSize = 6.2.sp,
                        fontWeight = FontWeight.Normal,
                        color = SfpLabelInk,
                        lineHeight = 7.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            SfpWhiteInputBox(
                value = if (isPreviewReadOnly) formattedAmount else rawAmount,
                onValueChange = onAmountChange,
                placeholder = "",
                isFocused = isFocused,
                onTap = { onFieldFocus("sfpAmount") },
                isPreviewReadOnly = isPreviewReadOnly,
                keyboardType = KeyboardType.Number,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "المبلغ",
                    style = TextStyle(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SfpLabelInk,
                        lineHeight = 9.sp
                    )
                )
                Text(
                    text = "بالأرقام",
                    style = TextStyle(
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SfpLabelInk,
                        lineHeight = 7.5.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Amount in letters: "en lettres ...." + two dotted lines
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "en lettres",
                    style = TextStyle(
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Medium,
                        color = SfpLabelInk
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .drawDottedBaseline()
                )
                if (onToggleWordsLanguage != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFrenchWords) "FR" else "عربي",
                        style = TextStyle(
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            color = SfpNavyStripe
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                            .border(0.6.dp, SfpNavyStripe.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .clickable(onClick = onToggleWordsLanguage)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Two dotted lines with blue-ink words drawn cleanly across them
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 28.dp)
                    .drawBehind {
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                        val line1Y = 12.dp.toPx()
                        val line2Y = 24.dp.toPx()
                        drawLine(
                            color = SfpLabelInk.copy(alpha = 0.65f),
                            start = Offset(0f, line1Y),
                            end = Offset(size.width, line1Y),
                            strokeWidth = 0.8.dp.toPx(),
                            pathEffect = dashEffect
                        )
                        drawLine(
                            color = SfpLabelInk.copy(alpha = 0.65f),
                            start = Offset(0f, line2Y),
                            end = Offset(size.width, line2Y),
                            strokeWidth = 0.8.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    },
                contentAlignment = Alignment.TopStart
            ) {
                if (amountInWords.isNotEmpty()) {
                    Text(
                        text = amountInWords,
                        style = TextStyle(
                            fontSize = 7.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SfpPenBlue,
                            lineHeight = 12.sp,
                            textDirection = TextDirection.Content
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SfpPartyColumn(
    frenchHeader: String,
    arabicHeader: String,
    nom: String,
    prenom: String,
    address: String,
    onNomChange: (String) -> Unit,
    onPrenomChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    nomFocused: Boolean,
    prenomFocused: Boolean,
    addressFocused: Boolean,
    onNomTap: () -> Unit,
    onPrenomTap: () -> Unit,
    onAddressTap: () -> Unit,
    isPreviewReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = frenchHeader,
                style = TextStyle(
                    fontSize = 7.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = arabicHeader,
                style = TextStyle(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk.copy(alpha = 0.85f),
                    textDirection = TextDirection.Content
                )
            )
        }

        SfpDottedFieldRow(
            frenchLabel = "Nom :",
            arabicLabel = ": اللقب",
            value = nom,
            onValueChange = onNomChange,
            isFocused = nomFocused,
            onTap = onNomTap,
            isPreviewReadOnly = isPreviewReadOnly
        )

        SfpDottedFieldRow(
            frenchLabel = "Prénom(s) :",
            arabicLabel = ": الإسم",
            value = prenom,
            onValueChange = onPrenomChange,
            isFocused = prenomFocused,
            onTap = onPrenomTap,
            isPreviewReadOnly = isPreviewReadOnly
        )

        SfpDottedFieldRow(
            frenchLabel = "Adresse :",
            arabicLabel = ": العنوان",
            value = address,
            onValueChange = onAddressChange,
            isFocused = addressFocused,
            onTap = onAddressTap,
            isPreviewReadOnly = isPreviewReadOnly
        )
    }
}

@Composable
private fun SfpDottedFieldRow(
    frenchLabel: String,
    arabicLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    isFocused: Boolean,
    onTap: () -> Unit,
    isPreviewReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sfpFocusArea(isFocused)
            .clickable(onClick = onTap)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = frenchLabel,
            style = TextStyle(
                fontSize = 6.8.sp,
                fontWeight = FontWeight.Medium,
                color = SfpLabelInk
            ),
            modifier = Modifier.padding(end = 3.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 14.dp)
                .drawDottedBaseline(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (isPreviewReadOnly) {
                if (value.isNotEmpty()) {
                    Text(
                        text = value,
                        style = TextStyle(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SfpPenBlue,
                            textDirection = TextDirection.Content
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    )
                }
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    cursorBrush = SolidColor(SfpPenBlue),
                    textStyle = TextStyle(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = SfpPenBlue,
                        textDirection = TextDirection.Content
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                        .onFocusChanged { if (it.isFocused) onTap() }
                )
            }
        }

        Text(
            text = arabicLabel,
            style = TextStyle(
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = SfpLabelInk,
                textDirection = TextDirection.Content
            ),
            modifier = Modifier.padding(start = 3.dp)
        )
    }
}

@Composable
private fun SfpMotifCell(
    motif: String,
    onMotifChange: (String) -> Unit,
    isFocused: Boolean,
    onTap: () -> Unit,
    isPreviewReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .sfpFocusArea(isFocused)
            .clickable(onClick = onTap)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "(3) Motif / Correspondance",
                style = TextStyle(
                    fontSize = 7.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk
                )
            )
            Text(
                text = "السبب",
                style = TextStyle(
                    fontSize = 7.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk.copy(alpha = 0.8f),
                    textDirection = TextDirection.Content
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 20.dp)
                .drawDottedBaseline(),
            contentAlignment = Alignment.BottomStart
        ) {
            if (isPreviewReadOnly) {
                if (motif.isNotEmpty()) {
                    Text(
                        text = motif,
                        style = TextStyle(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SfpPenBlue,
                            textDirection = TextDirection.Content
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            } else {
                BasicTextField(
                    value = motif,
                    onValueChange = onMotifChange,
                    maxLines = 2,
                    cursorBrush = SolidColor(SfpPenBlue),
                    textStyle = TextStyle(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SfpPenBlue,
                        textDirection = TextDirection.Content
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                        .onFocusChanged { if (it.isFocused) onTap() }
                )
            }
        }
    }
}

@Composable
private fun SfpSignatureAndDateCell(
    date: String,
    place: String,
    onDateChange: (String) -> Unit,
    onPlaceChange: (String) -> Unit,
    isSignatureFocused: Boolean,
    isDateFocused: Boolean,
    onSignatureTap: () -> Unit,
    onDateTap: () -> Unit,
    isPreviewReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Signature zone (left side of right column)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .sfpFocusArea(isSignatureFocused)
                .clickable(onClick = onSignatureTap),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "(1) Signature  الإمضاء",
                style = TextStyle(
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk
                ),
                maxLines = 1
            )
            if (isSignatureFocused) {
                Text(
                    text = "التوقيع هنا",
                    style = TextStyle(
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SfpPenBlue.copy(alpha = 0.7f),
                        textDirection = TextDirection.Content
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
        }

        // Date & Place zone (right side of right column)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .sfpFocusArea(isDateFocused)
                .clickable(onClick = onDateTap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Date  التاريخ",
                style = TextStyle(
                    fontSize = 7.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk
                ),
                maxLines = 1
            )

            if (place.isNotBlank()) {
                if (isPreviewReadOnly) {
                    Text(
                        text = place,
                        style = TextStyle(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = SfpPenBlue,
                            textDirection = TextDirection.Content
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    BasicTextField(
                        value = place,
                        onValueChange = onPlaceChange,
                        singleLine = true,
                        cursorBrush = SolidColor(SfpPenBlue),
                        textStyle = TextStyle(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = SfpPenBlue,
                            textAlign = TextAlign.Center,
                            textDirection = TextDirection.Content
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SfpWhiteInputBox(
                value = date,
                onValueChange = onDateChange,
                placeholder = "/     /",
                isFocused = isDateFocused,
                onTap = onDateTap,
                isPreviewReadOnly = isPreviewReadOnly,
                keyboardType = KeyboardType.Text,
                textAlign = TextAlign.Center,
                fontSizeSp = 7.5f,
                modifier = Modifier
                    .width(74.dp)
                    .height(17.dp)
            )
        }
    }
}

@Composable
private fun SfpIdentityCell(
    sfpPiType: String,
    sfpPiNumero: String,
    sfpPiDelivreLe: String,
    sfpPiPar: String,
    fallbackIdDescription: String,
    onIdDescriptionChange: (String) -> Unit,
    isFocused: Boolean,
    onTap: () -> Unit,
    isPreviewReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    val displayedPiNumber = sfpPiNumero.ifBlank {
        if (sfpPiType.isBlank()) fallbackIdDescription else ""
    }

    Column(
        modifier = modifier
            .sfpFocusArea(isFocused)
            .clickable(onClick = onTap)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "بيانات بطاقة الهوية",
                style = TextStyle(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SfpLabelInk,
                    lineHeight = 8.5.sp
                )
            )
            Text(
                text = "Description pièce d'identité",
                style = TextStyle(
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = SfpLabelInk,
                    lineHeight = 7.5.sp
                )
            )
        }

        // P.I : [box]  N° [box]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "P.I :",
                style = TextStyle(fontSize = 6.8.sp, fontWeight = FontWeight.Medium, color = SfpLabelInk)
            )
            SfpWhiteInputBox(
                value = sfpPiType,
                onValueChange = {},
                placeholder = "",
                isFocused = false,
                onTap = onTap,
                isPreviewReadOnly = true,
                textAlign = TextAlign.Center,
                fontSizeSp = 7.5f,
                modifier = Modifier
                    .width(28.dp)
                    .height(14.dp)
            )
            Text(
                text = "N°",
                style = TextStyle(fontSize = 6.8.sp, fontWeight = FontWeight.Medium, color = SfpLabelInk)
            )
            SfpWhiteInputBox(
                value = displayedPiNumber,
                onValueChange = onIdDescriptionChange,
                placeholder = "",
                isFocused = isFocused,
                onTap = onTap,
                isPreviewReadOnly = isPreviewReadOnly || sfpPiNumero.isNotBlank(),
                textAlign = TextAlign.Start,
                fontSizeSp = 7.5f,
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
            )
        }

        // Délivré(e) le : ... : صدرت بتاريخ
        SfpDottedFieldRow(
            frenchLabel = "Délivré(e) le :",
            arabicLabel = ": صدرت بتاريخ",
            value = sfpPiDelivreLe,
            onValueChange = {},
            isFocused = false,
            onTap = onTap,
            isPreviewReadOnly = true
        )

        // Par ... : عن
        SfpDottedFieldRow(
            frenchLabel = "Par",
            arabicLabel = ": عن",
            value = sfpPiPar,
            onValueChange = {},
            isFocused = false,
            onTap = onTap,
            isPreviewReadOnly = true
        )
    }
}

@Composable
private fun SfpPostOfficeStampCell(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 62.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ختم التاريخ",
                    style = TextStyle(
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SfpLabelInk,
                        lineHeight = 8.5.sp
                    )
                )
                Text(
                    text = "T.A.D",
                    style = TextStyle(
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SfpLabelInk,
                        lineHeight = 7.5.sp
                    )
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "تأشيرة العون المكلف بالدفع",
                    style = TextStyle(
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SfpLabelInk,
                        lineHeight = 8.5.sp
                    )
                )
                Text(
                    text = "Visa de l'agent payeur",
                    style = TextStyle(
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SfpLabelInk,
                        lineHeight = 7.5.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun SfpReceiptStub() {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Yellow receipt bar with small left white logo cutout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(21.dp)
                        .background(SfpBannerYellow)
                        .padding(start = 34.dp, end = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reçu de l'opération",
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SfpLabelInk
                        )
                    )
                    Text(
                        text = "وصل العملية",
                        style = TextStyle(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SfpLabelInk,
                            textDirection = TextDirection.Content
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(SfpNavyStripe)
                )
            }

            // Small left logo box
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .background(Color.White)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sfp_header_logo),
                    contentDescription = "Algérie Poste",
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "بريد الجزائر",
                    style = TextStyle(
                        fontSize = 4.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SfpNavyStripe
                    )
                )
            }
        }

        // White watermarked receipt stub with subtle grey Algérie Poste swoosh arcs + AP/DEC 09
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .background(Color.White)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Left swoosh crescent watermark
                val leftArc = Path().apply {
                    moveTo(0f, h * 0.12f)
                    cubicTo(w * 0.16f, h * 0.08f, w * 0.28f, h * 0.28f, w * 0.18f, h * 0.56f)
                    cubicTo(w * 0.12f, h * 0.72f, w * 0.03f, h * 0.80f, 0f, h * 0.84f)
                    lineTo(0f, h * 0.66f)
                    cubicTo(w * 0.12f, h * 0.56f, w * 0.14f, h * 0.30f, 0f, h * 0.24f)
                    close()
                }
                drawPath(path = leftArc, color = SfpWatermarkArc.copy(alpha = 0.7f))

                // Large right-center Algérie Poste swallow wing watermark arcs
                val rightWingOuter = Path().apply {
                    moveTo(w * 0.60f, 0f)
                    cubicTo(w * 0.56f, h * 0.45f, w * 0.76f, h * 0.68f, w, h * 0.42f)
                    lineTo(w, h * 0.94f)
                    cubicTo(w * 0.70f, h * 1.05f, w * 0.44f, h * 0.70f, w * 0.50f, 0f)
                    close()
                }
                drawPath(path = rightWingOuter, color = SfpWatermarkArc.copy(alpha = 0.65f))

                val lowerArcStroke = Path().apply {
                    moveTo(w * 0.34f, h)
                    cubicTo(w * 0.42f, h * 0.68f, w * 0.66f, h * 0.64f, w * 0.92f, h)
                }
                drawPath(
                    path = lowerArcStroke,
                    color = SfpWatermarkArc.copy(alpha = 0.8f),
                    style = Stroke(width = 6.dp.toPx())
                )

                // Subtle bottom yellow edge matching printed form stub
                drawLine(
                    color = SfpBannerYellow,
                    start = Offset(0f, h - 1.5.dp.toPx()),
                    end = Offset(w, h - 1.5.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Vertical AP/DEC 09 mark on right edge
            Text(
                text = "AP/DEC 09",
                style = TextStyle(
                    fontSize = 4.8.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 2.dp)
                    .graphicsLayer { rotationZ = -90f }
            )
        }
    }
}

@Composable
private fun SfpWhiteInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isFocused: Boolean,
    onTap: () -> Unit,
    isPreviewReadOnly: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    textAlign: TextAlign = TextAlign.Start,
    fontSizeSp: Float = 10f,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isFocused) SfpFocusAmber else SfpBoxBorder
    val borderWidth = if (isFocused) 1.5.dp else 0.8.dp
    val boxBackground = if (isFocused) Color(0xFFFFFBEB) else Color.White

    Box(
        modifier = modifier
            .background(boxBackground)
            .border(BorderStroke(borderWidth, borderColor))
            .clickable(onClick = onTap)
            .padding(horizontal = 5.dp),
        contentAlignment = if (textAlign == TextAlign.Center) Alignment.Center else if (textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (isPreviewReadOnly) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = TextStyle(
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = SfpPenBlue,
                        textAlign = textAlign
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(
                        fontSize = fontSizeSp.sp,
                        color = SfpLabelInk.copy(alpha = 0.55f),
                        textAlign = textAlign
                    ),
                    maxLines = 1
                )
            }
        } else {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(SfpPenBlue),
                textStyle = TextStyle(
                    fontSize = fontSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = SfpPenBlue,
                    textAlign = textAlign
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) onTap() },
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (textAlign == TextAlign.Center) Alignment.Center else if (textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                style = TextStyle(
                                    fontSize = fontSizeSp.sp,
                                    color = SfpLabelInk.copy(alpha = 0.55f),
                                    textAlign = textAlign
                                ),
                                maxLines = 1
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun SfpHorizontalDivider(thickness: Dp = 0.8.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness)
            .background(SfpGridBorder)
    )
}

@Composable
private fun SfpVerticalDivider(thickness: Dp = 0.8.dp) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(thickness)
            .background(SfpGridBorder)
    )
}

private fun Modifier.sfpFocusArea(isFocused: Boolean): Modifier {
    return if (isFocused) {
        this
            .background(SfpFocusWash, RoundedCornerShape(2.dp))
            .border(1.5.dp, SfpFocusAmber, RoundedCornerShape(2.dp))
    } else {
        this
    }
}

private fun Modifier.drawDottedBaseline(): Modifier {
    return this.drawBehind {
        val y = size.height - 1.dp.toPx()
        drawLine(
            color = SfpLabelInk.copy(alpha = 0.65f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.8.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
        )
    }
}
