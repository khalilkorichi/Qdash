package com.qdash.presentation.simulator.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.R
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.PostalCcpUtils
import kotlin.math.PI
import kotlin.math.sin

// Authentic Algérie Poste postal check palette
private val CheckCreamTop = Color(0xFFFCF8E3)
private val CheckCreamMid = Color(0xFFFAF5DC)
private val CheckGuillocheLight = Color(0xFFEFE08B)
private val CheckGuillocheGold = Color(0xFFE8D575)
private val CheckBuildingWatermark = Color(0xFFF3E8A6)
private val CheckInkBlue = Color(0xFF0F2B8C)
private val CheckPosteBlue = Color(0xFF165B93)
private val CheckBoxBorder = Color(0xFF7E8C75)
private val CheckRuleColor = Color(0xFF8C9680)
private val CheckLabelDark = Color(0xFF232724)
private val CheckMutedText = Color(0xFF6B726C)
private val CheckOuterBorder = Color(0xFFCFC9AE)
private val CheckFocusAmber = Color(0xFFF59E0B)

/**
 * Photorealistic 1:1 Algérie Poste Postal Check canvas.
 * Locked to physical LTR geometry so all labels, boxes, watermark, and MICR band
 * preserve their real-world printed positions regardless of system RTL locale.
 */
@Composable
fun ChequeVisualView(
    chequeAmount: String,
    chequeBeneficiary: String,
    chequePlace: String,
    chequeDate: String,
    chequeCcp: String,
    chequeKey: String,
    onFieldTap: (String) -> Unit,
    focusedField: String? = null,
    chequeNumber: String = "XXXX",
    accountHolderName: String = "",
    accountHolderAddress: String = "",
    amountInWords: String? = null,
    isFrenchWords: Boolean = false,
    onToggleWordsLanguage: (() -> Unit)? = null,
    ripCode: String = "",
    modifier: Modifier = Modifier
) {
    val parsedAmount = remember(chequeAmount) {
        AmountConversionEngine.parseFlexibleAmount(chequeAmount)
    }

    val formattedAmount = remember(chequeAmount, parsedAmount) {
        when {
            parsedAmount != null -> AmountConversionEngine.formatAmountToPostal(parsedAmount)
            chequeAmount.isNotBlank() -> chequeAmount
            else -> ""
        }
    }

    val resolvedWords = remember(parsedAmount, amountInWords, isFrenchWords) {
        when {
            !amountInWords.isNullOrBlank() -> amountInWords.trim()
            parsedAmount != null -> {
                if (isFrenchWords) {
                    AmountConversionEngine.convertToFrenchWords(parsedAmount)
                } else {
                    AmountConversionEngine.convertToArabicWords(parsedAmount, preferCentimes = false)
                }
            }
            else -> ""
        }
    }

    val cleanCcpDigits = remember(chequeCcp) { chequeCcp.filter { it.isDigit() } }
    val paddedCcp = remember(cleanCcpDigits) {
        if (cleanCcpDigits.isNotEmpty()) cleanCcpDigits.padStart(10, '0') else ""
    }
    val cleanKeyDigits = remember(chequeKey) { chequeKey.filter { it.isDigit() }.take(2) }
    val displayKey = remember(cleanKeyDigits) {
        if (cleanKeyDigits.isNotEmpty()) cleanKeyDigits.padStart(2, '0') else ".."
    }
    val resolvedChequeNumber = remember(chequeNumber) {
        chequeNumber.trim().ifEmpty { "XXXX" }
    }
    val resolvedRip = remember(ripCode, cleanCcpDigits) {
        ripCode.trim().ifEmpty {
            PostalCcpUtils.buildFullRip(cleanCcpDigits)
                ?: "00799999${cleanCcpDigits.padStart(10, '0')}.."
        }
    }

    val isAmountFocused = focusedField == "chequeAmount"
    val isBeneficiaryFocused = focusedField == "chequeBeneficiary"
    val isPlaceFocused = focusedField == "chequePlace"
    val isDateFocused = focusedField == "chequeDate"
    val isSignatureFocused = focusedField == "chequeSignature"
    val isCcpFocused = focusedField == "chequeCcp"
    val isKeyFocused = focusedField == "chequeKey"

    val cardShape = RoundedCornerShape(6.dp)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag("cheque_preview_card")
                .clip(cardShape)
                .background(CheckCreamTop, cardShape)
                .border(1.dp, CheckOuterBorder, cardShape)
        ) {
            // ======= CREAM SECURITY PAPER BODY =======
            Box(modifier = Modifier.fillMaxWidth()) {
                // Security background: cream paper, guilloché frieze, wave threads & Grande Poste watermark
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawPostalCheckSecurityBackground()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 3.dp)
                ) {
                    // 1. TOP ROW: Cheque N° | Algérie Poste Logo | Amount Box
                    ChequeHeaderRow(
                        chequeNumber = resolvedChequeNumber,
                        formattedAmount = formattedAmount,
                        isAmountFocused = isAmountFocused,
                        onFieldTap = onFieldTap
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. ROW 1: Amount in Words (two ruled lines + optional AR/FR pill)
                    ChequeWordsSection(
                        words = resolvedWords,
                        isFrenchWords = isFrenchWords,
                        isAmountFocused = isAmountFocused,
                        onToggleWordsLanguage = onToggleWordsLanguage,
                        onTap = { onFieldTap("chequeAmount") }
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // 3. ROW 2: Beneficiary ("A l'ordre de" ... "لأمر")
                    ChequeBeneficiaryRow(
                        beneficiary = chequeBeneficiary,
                        isFocused = isBeneficiaryFocused,
                        onTap = { onFieldTap("chequeBeneficiary") }
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    // 4. LOWER BODY: Payable à Box | CCP + Holder | Place/Date + Signature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        PayableAtBox(
                            modifier = Modifier
                                .weight(1.12f)
                                .testTag("cheque_payable_box")
                        )

                        CcpAndHolderSection(
                            paddedCcp = paddedCcp,
                            displayKey = displayKey,
                            accountHolderName = accountHolderName,
                            accountHolderAddress = accountHolderAddress,
                            isCcpFocused = isCcpFocused,
                            isKeyFocused = isKeyFocused,
                            onFieldTap = onFieldTap,
                            modifier = Modifier
                                .weight(0.88f)
                                .padding(top = 5.dp)
                        )

                        PlaceDateSignatureSection(
                            chequePlace = chequePlace,
                            chequeDate = chequeDate,
                            isPlaceFocused = isPlaceFocused,
                            isDateFocused = isDateFocused,
                            isSignatureFocused = isSignatureFocused,
                            onFieldTap = onFieldTap,
                            modifier = Modifier
                                .weight(1.16f)
                                .padding(top = 1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 5. BOTTOM MARGIN: Série AP & Non-writing Zone Warning
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Série :",
                            fontSize = 6.5.sp,
                            color = CheckLabelDark,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AP",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CheckLabelDark,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRIERE DE NE RIEN ECRIRE DANS LA ZONE BLANCHE",
                                fontSize = 4.9.sp,
                                letterSpacing = 0.05.sp,
                                color = CheckLabelDark.copy(alpha = 0.85f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "الرجاء عدم الكتابة في المساحة البيضاء",
                                fontSize = 5.6.sp,
                                color = CheckLabelDark.copy(alpha = 0.85f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // ======= CRISP WHITE BOTTOM MICR BAND =======
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cheque_micr_strip")
                    .background(Color.White)
                    .drawBehind {
                        drawLine(
                            color = CheckOuterBorder.copy(alpha = 0.55f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 0.6.dp.toPx()
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resolvedChequeNumber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.9.sp,
                    color = Color(0xFF1B1E1D),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(0.28f)
                )
                Text(
                    text = resolvedRip,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFF1B1E1D),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.72f)
                )
            }
        }
    }
}

@Composable
private fun ChequeHeaderRow(
    chequeNumber: String,
    formattedAmount: String,
    isAmountFocused: Boolean,
    onFieldTap: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Top-Left: Pre-printed Chèque n° : XXXX (read-only)
        Row(
            modifier = Modifier
                .weight(0.95f)
                .testTag("cheque_number_field"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chèque n° :",
                fontSize = 7.sp,
                color = CheckLabelDark,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = chequeNumber,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = CheckLabelDark,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Top-Center: بريد الجزائر / ALGERIE POSTE beside sfp_header_logo
        Row(
            modifier = Modifier.weight(1.05f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "بريد الجزائر",
                    fontSize = 9.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CheckPosteBlue,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "ALGERIE POSTE",
                    fontSize = 6.2.sp,
                    lineHeight = 7.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.25.sp,
                    color = CheckPosteBlue,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
            Image(
                painter = painterResource(id = R.drawable.sfp_header_logo),
                contentDescription = "Algérie Poste",
                modifier = Modifier.size(22.dp)
            )
        }

        // Top-Right: DA [ Amount Box ] دج
        Row(
            modifier = Modifier.weight(1.20f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DA",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = CheckLabelDark,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 104.dp)
                    .height(23.dp)
                    .testTag("cheque_amount_box")
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isAmountFocused) CheckFocusAmber.copy(alpha = 0.14f)
                        else Color(0xFFFFFEF7)
                    )
                    .border(
                        width = if (isAmountFocused) 1.5.dp else 1.dp,
                        color = if (isAmountFocused) CheckFocusAmber else CheckBoxBorder,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .clickable { onFieldTap("chequeAmount") }
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                val hasAmount = formattedAmount.isNotEmpty()
                Text(
                    text = if (hasAmount) formattedAmount else "0,00",
                    fontSize = if (formattedAmount.length > 11) 8.5.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasAmount) CheckInkBlue else CheckInkBlue.copy(alpha = 0.30f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "دج",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = CheckLabelDark,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ChequeWordsSection(
    words: String,
    isFrenchWords: Boolean,
    isAmountFocused: Boolean,
    onToggleWordsLanguage: (() -> Unit)?,
    onTap: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val leftLabel = "Payez, contre ce chèque"
    val rightLabel = "إدفعوا مقابل هذا الصك"
    val leftLabelStyle = TextStyle(
        fontSize = 7.8.sp,
        fontStyle = FontStyle.Italic,
        color = CheckLabelDark
    )
    val rightLabelStyle = TextStyle(
        fontSize = 8.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = CheckLabelDark
    )
    val wordsStyle = TextStyle(
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = CheckInkBlue,
        textDirection = if (isFrenchWords) TextDirection.Ltr else TextDirection.Rtl
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cheque_words_line")
            .focusHighlightModifier(isFocused = isAmountFocused, paddingHorizontal = 3.dp, paddingVertical = 2.dp)
            .clickable { onTap() }
    ) {
        val totalWidthPx = with(density) { maxWidth.roundToPx() }
        val leftWidthPx = remember(leftLabel) {
            textMeasurer.measure(leftLabel, leftLabelStyle).size.width
        }
        val rightWidthPx = remember(rightLabel) {
            textMeasurer.measure(rightLabel, rightLabelStyle).size.width
        }
        val horizontalGapPx = with(density) { 16.dp.roundToPx() }
        val line1MaxWidthPx = (totalWidthPx - leftWidthPx - rightWidthPx - horizontalGapPx)
            .coerceAtLeast(with(density) { 64.dp.roundToPx() })

        val (line1Words, line2Words) = remember(words, line1MaxWidthPx, isFrenchWords) {
            if (words.isEmpty()) {
                "" to ""
            } else {
                splitAmountWordsAcrossTwoLines(
                    words = words,
                    line1MaxWidthPx = line1MaxWidthPx,
                    textMeasurer = textMeasurer,
                    style = wordsStyle
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Ruled Line 1: Payez, contre ce chèque [words line 1] إدفعوا مقابل هذا الصك
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(19.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = leftLabel,
                    style = leftLabelStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(19.dp)
                        .drawBehind {
                            drawLine(
                                color = CheckRuleColor,
                                start = Offset(0f, size.height - 1.dp.toPx()),
                                end = Offset(size.width, size.height - 1.dp.toPx()),
                                strokeWidth = 0.7.dp.toPx()
                            )
                        }
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    contentAlignment = when {
                        line2Words.isEmpty() -> Alignment.BottomCenter
                        isFrenchWords -> Alignment.BottomStart
                        else -> Alignment.BottomEnd
                    }
                ) {
                    if (line1Words.isNotEmpty()) {
                        Text(
                            text = line1Words,
                            style = wordsStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = if (isFrenchWords) "Montant en lettres..." else "المبلغ بالحروف...",
                            fontSize = 8.sp,
                            color = CheckInkBlue.copy(alpha = 0.28f),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rightLabel,
                    style = rightLabelStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            // Ruled Line 2: Full-width continuation + optional AR/FR pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .drawBehind {
                        drawLine(
                            color = CheckRuleColor,
                            start = Offset(0f, size.height - 1.dp.toPx()),
                            end = Offset(size.width, size.height - 1.dp.toPx()),
                            strokeWidth = 0.7.dp.toPx()
                        )
                    }
                    .padding(horizontal = 2.dp, vertical = 1.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 1.dp),
                    contentAlignment = if (isFrenchWords) Alignment.BottomStart else Alignment.BottomEnd
                ) {
                    if (line2Words.isNotEmpty()) {
                        Text(
                            text = line2Words,
                            style = wordsStyle.copy(
                                fontSize = if (line2Words.length > 52) 8.5.sp else 9.5.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onToggleWordsLanguage != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .testTag("cheque_words_lang_toggle")
                            .clip(RoundedCornerShape(8.dp))
                            .background(CheckPosteBlue.copy(alpha = 0.12f))
                            .border(0.75.dp, CheckPosteBlue.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable { onToggleWordsLanguage() }
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFrenchWords) "FR" else "AR",
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CheckPosteBlue,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChequeBeneficiaryRow(
    beneficiary: String,
    isFocused: Boolean,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .testTag("cheque_beneficiary_line")
            .focusHighlightModifier(isFocused = isFocused, paddingHorizontal = 3.dp, paddingVertical = 1.dp)
            .clickable { onTap() },
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "A l'ordre de",
            fontSize = 7.8.sp,
            fontStyle = FontStyle.Italic,
            color = CheckLabelDark,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(bottom = 1.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(19.dp)
                .drawBehind {
                    drawLine(
                        color = CheckRuleColor,
                        start = Offset(0f, size.height - 1.dp.toPx()),
                        end = Offset(size.width, size.height - 1.dp.toPx()),
                        strokeWidth = 0.7.dp.toPx()
                    )
                }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val trimmed = beneficiary.trim()
            if (trimmed.isNotEmpty()) {
                Text(
                    text = trimmed,
                    style = TextStyle(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CheckInkBlue,
                        textDirection = TextDirection.Content,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "لأمر المستفيد...",
                    fontSize = 8.sp,
                    color = CheckInkBlue.copy(alpha = 0.28f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "لأمر",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = CheckLabelDark,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}

@Composable
private fun PayableAtBox(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .drawBehind {
                // Draw left, bottom, and right borders starting at the midpoint of the top label row
                val strokePx = 0.75.dp.toPx()
                val topY = 4.5.dp.toPx()
                drawLine(CheckRuleColor, Offset(0f, topY), Offset(0f, size.height), strokePx)
                drawLine(CheckRuleColor, Offset(0f, size.height), Offset(size.width, size.height), strokePx)
                drawLine(CheckRuleColor, Offset(size.width, size.height), Offset(size.width, topY), strokePx)
            }
    ) {
        // Broken top border with inline "Payable à" and "يوفى"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(0.75.dp)
                    .background(CheckRuleColor)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "Payable à",
                fontSize = 6.2.sp,
                fontWeight = FontWeight.Medium,
                color = CheckLabelDark,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.75.dp)
                    .background(CheckRuleColor)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "يوفى",
                fontSize = 6.8.sp,
                fontWeight = FontWeight.SemiBold,
                color = CheckLabelDark,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(0.75.dp)
                    .background(CheckRuleColor)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 4.dp, top = 2.dp, bottom = 5.dp),
            verticalArrangement = Arrangement.spacedBy(0.5.dp)
        ) {
            val addressStyle = TextStyle(
                fontSize = 5.5.sp,
                lineHeight = 6.9.sp,
                color = CheckLabelDark.copy(alpha = 0.9f)
            )
            Text(text = "Agence : 99999", style = addressStyle)
            Text(text = "CNCP (Centre National des", style = addressStyle)
            Text(text = "Chèques Postaux)", style = addressStyle)
            Text(text = "1, Avenue du 1er Novembre", style = addressStyle)
            Text(text = "16407 Alger", style = addressStyle)
        }
    }
}

@Composable
private fun CcpAndHolderSection(
    paddedCcp: String,
    displayKey: String,
    accountHolderName: String,
    accountHolderAddress: String,
    isCcpFocused: Boolean,
    isKeyFocused: Boolean,
    onFieldTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = paddedCcp.ifEmpty { "0000000000" },
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (paddedCcp.isNotEmpty()) CheckLabelDark else CheckMutedText.copy(alpha = 0.55f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .testTag("cheque_ccp_field")
                    .focusHighlightModifier(isFocused = isCcpFocused, paddingHorizontal = 2.dp, paddingVertical = 1.dp)
                    .clickable { onFieldTap("chequeCcp") }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayKey,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (displayKey != "..") CheckLabelDark else CheckMutedText.copy(alpha = 0.65f),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .testTag("cheque_key_field")
                    .focusHighlightModifier(isFocused = isKeyFocused, paddingHorizontal = 2.dp, paddingVertical = 1.dp)
                    .clickable { onFieldTap("chequeKey") }
            )
        }

        if (accountHolderName.isNotBlank()) {
            Text(
                text = "M. / Mme ${accountHolderName.trim()}",
                fontSize = 6.3.sp,
                lineHeight = 7.5.sp,
                fontWeight = FontWeight.Medium,
                color = CheckLabelDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onFieldTap("chequeCcp") }
            )
        }

        if (accountHolderAddress.isNotBlank()) {
            Text(
                text = accountHolderAddress.trim(),
                fontSize = 5.8.sp,
                lineHeight = 7.sp,
                color = CheckLabelDark.copy(alpha = 0.85f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onFieldTap("chequeCcp") }
            )
        }
    }
}

@Composable
private fun PlaceDateSignatureSection(
    chequePlace: String,
    chequeDate: String,
    isPlaceFocused: Boolean,
    isDateFocused: Boolean,
    isSignatureFocused: Boolean,
    onFieldTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Place & Date on dotted baseline: [Place] Le [Date] في
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag("cheque_place_field")
                    .focusHighlightModifier(isFocused = isPlaceFocused, paddingHorizontal = 2.dp, paddingVertical = 1.dp)
                    .drawBehind {
                        drawLine(
                            color = CheckRuleColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 0.7.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2.5f), 0f)
                        )
                    }
                    .clickable { onFieldTap("chequePlace") }
                    .padding(bottom = 1.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val placeTrimmed = chequePlace.trim()
                Text(
                    text = placeTrimmed.ifEmpty { "......" },
                    style = TextStyle(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (placeTrimmed.isNotEmpty()) CheckInkBlue else CheckInkBlue.copy(alpha = 0.30f),
                        textDirection = TextDirection.Content
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "Le",
                fontSize = 7.sp,
                fontStyle = FontStyle.Italic,
                color = CheckLabelDark,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = 1.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))

            Box(
                modifier = Modifier
                    .weight(1.12f)
                    .testTag("cheque_date_field")
                    .focusHighlightModifier(isFocused = isDateFocused, paddingHorizontal = 2.dp, paddingVertical = 1.dp)
                    .drawBehind {
                        drawLine(
                            color = CheckRuleColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 0.7.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2.5f), 0f)
                        )
                    }
                    .clickable { onFieldTap("chequeDate") }
                    .padding(bottom = 1.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val dateTrimmed = chequeDate.trim()
                Text(
                    text = dateTrimmed.ifEmpty { "../../...." },
                    fontSize = 7.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dateTrimmed.isNotEmpty()) CheckInkBlue else CheckInkBlue.copy(alpha = 0.30f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "في",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = CheckLabelDark,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }

        // Open interactive signature zone (clean cream paper unless focused)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp)
                .testTag("cheque_signature_zone")
                .then(
                    if (isSignatureFocused) {
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CheckFocusAmber.copy(alpha = 0.14f))
                            .border(1.5.dp, CheckFocusAmber, RoundedCornerShape(4.dp))
                    } else {
                        Modifier
                    }
                )
                .clickable { onFieldTap("chequeSignature") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSignatureFocused) "الإمضاء / Signature" else "Signature",
                fontSize = if (isSignatureFocused) 8.sp else 6.5.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = if (isSignatureFocused) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSignatureFocused) Color(0xFFB45309) else CheckMutedText.copy(alpha = 0.42f),
                maxLines = 1
            )
        }
    }
}

private fun Modifier.focusHighlightModifier(
    isFocused: Boolean,
    paddingHorizontal: Dp,
    paddingVertical: Dp
): Modifier {
    if (!isFocused) return this
    return this
        .clip(RoundedCornerShape(3.dp))
        .background(CheckFocusAmber.copy(alpha = 0.14f))
        .border(1.5.dp, CheckFocusAmber, RoundedCornerShape(3.dp))
        .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
}

/**
 * Draws the authentic Algérie Poste security paper background:
 * 1. Cream-yellow postal paper gradient (#FCF8E3 / #FAF5DC)
 * 2. Faint sinusoidal security wave threads
 * 3. Top ornamental yellow-gold guilloché border & side margins (#EFE08B / #E8D575)
 * 4. Subtle arched Algiers Grande Poste building watermark on the right half (#F3E8A6)
 */
private fun DrawScope.drawPostalCheckSecurityBackground() {
    val w = size.width
    val h = size.height

    // 1. Base cream-yellow paper gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(CheckCreamTop, CheckCreamMid, CheckCreamTop)
        )
    )

    // 2. Subtle wavy micro-guilloché security threads across the cream body
    val waveSpacing = 4.2.dp.toPx()
    val waveAmplitude = 1.1.dp.toPx()
    val waveCycles = 18f
    var y = 14.dp.toPx()
    var rowIdx = 0
    while (y < h - 4.dp.toPx()) {
        val wavePath = Path()
        val steps = 48
        val phase = (rowIdx % 3) * (PI.toFloat() / 3f)
        for (i in 0..steps) {
            val x = (i / steps.toFloat()) * w
            val wy = y + sin((i / steps.toFloat()) * 2f * PI.toFloat() * waveCycles + phase) * waveAmplitude
            if (i == 0) wavePath.moveTo(x, wy) else wavePath.lineTo(x, wy)
        }
        drawPath(
            path = wavePath,
            color = CheckGuillocheLight.copy(alpha = 0.24f),
            style = Stroke(width = 0.55.dp.toPx())
        )
        y += waveSpacing
        rowIdx++
    }

    // 3. Subtle arched Grande Poste building watermark on the right half (#F3E8A6, alpha ~0.55f)
    drawGrandePosteWatermark(w, h)

    // 4. Top ornamental yellow-gold guilloché border (#EFE08B / #E8D575)
    drawTopGuillocheBorder(w, h)
}

private fun DrawScope.drawTopGuillocheBorder(w: Float, h: Float) {
    val bandHeight = 13.dp.toPx()

    // Soft top gold wash
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                CheckGuillocheLight.copy(alpha = 0.50f),
                CheckGuillocheLight.copy(alpha = 0.10f)
            ),
            startY = 0f,
            endY = bandHeight
        ),
        size = Size(w, bandHeight)
    )

    // Repeating diamond/rosette guilloché medallions across the top edge
    val medallionW = 14.dp.toPx()
    val count = (w / medallionW).toInt() + 1
    val strokeThin = 0.85.dp.toPx()
    val cy = bandHeight * 0.45f
    val rx = medallionW * 0.48f
    val ry = bandHeight * 0.36f

    for (i in 0..count) {
        val cx = i * medallionW
        val diamond = Path().apply {
            moveTo(cx, cy - ry)
            quadraticTo(cx + rx * 0.55f, cy - ry * 0.45f, cx + rx, cy)
            quadraticTo(cx + rx * 0.55f, cy + ry * 0.45f, cx, cy + ry)
            quadraticTo(cx - rx * 0.55f, cy + ry * 0.45f, cx - rx, cy)
            quadraticTo(cx - rx * 0.55f, cy - ry * 0.45f, cx, cy - ry)
            close()
        }
        drawPath(
            path = diamond,
            color = CheckGuillocheLight.copy(alpha = 0.45f)
        )
        drawPath(
            path = diamond,
            color = CheckGuillocheGold.copy(alpha = 0.85f),
            style = Stroke(width = strokeThin)
        )
        drawCircle(
            color = CheckGuillocheGold.copy(alpha = 0.70f),
            radius = 1.6.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 0.7.dp.toPx())
        )
    }

    // Interlocking sine braid along bottom of the top ornamental frieze
    val braidPathA = Path()
    val braidPathB = Path()
    val braidSteps = 96
    val braidY = bandHeight * 0.82f
    val braidAmp = 2.0.dp.toPx()
    for (i in 0..braidSteps) {
        val t = i / braidSteps.toFloat()
        val x = t * w
        val angle = t * 2f * PI.toFloat() * (w / medallionW)
        val ya = braidY + sin(angle) * braidAmp
        val yb = braidY - sin(angle) * braidAmp
        if (i == 0) {
            braidPathA.moveTo(x, ya)
            braidPathB.moveTo(x, yb)
        } else {
            braidPathA.lineTo(x, ya)
            braidPathB.lineTo(x, yb)
        }
    }
    drawPath(braidPathA, CheckGuillocheGold.copy(alpha = 0.75f), style = Stroke(width = 0.65.dp.toPx()))
    drawPath(braidPathB, CheckGuillocheLight.copy(alpha = 0.85f), style = Stroke(width = 0.65.dp.toPx()))

    // Subtle vertical guilloché side borders on left and right edges
    val sideW = 5.dp.toPx()
    val sideStep = 10.dp.toPx()
    var sy = bandHeight
    while (sy < h - 4.dp.toPx()) {
        drawOval(
            color = CheckGuillocheGold.copy(alpha = 0.35f),
            topLeft = Offset(1.dp.toPx(), sy),
            size = Size(sideW - 1.dp.toPx(), sideStep * 0.85f),
            style = Stroke(width = 0.6.dp.toPx())
        )
        drawOval(
            color = CheckGuillocheGold.copy(alpha = 0.35f),
            topLeft = Offset(w - sideW, sy),
            size = Size(sideW - 1.dp.toPx(), sideStep * 0.85f),
            style = Stroke(width = 0.6.dp.toPx())
        )
        sy += sideStep
    }
}

private fun DrawScope.drawGrandePosteWatermark(w: Float, h: Float) {
    // Positioned on the right half of the check (x: 48%..97%, y: 24%..95%)
    val left = w * 0.48f
    val right = w * 0.96f
    val bw = right - left
    val top = h * 0.23f
    val bottom = h * 0.94f
    val bh = bottom - top

    val fillCol = CheckBuildingWatermark.copy(alpha = 0.55f)
    val lineCol = CheckGuillocheGold.copy(alpha = 0.48f)
    val stroke = Stroke(width = 0.8.dp.toPx())

    // Left stepped Moorish tower / minaret silhouette
    val towerLeft = left + bw * 0.02f
    val towerW = bw * 0.17f
    val towerTop = top + bh * 0.12f
    val towerBody = Path().apply {
        moveTo(towerLeft, bottom)
        lineTo(towerLeft, towerTop + bh * 0.14f)
        lineTo(towerLeft + towerW * 0.22f, towerTop + bh * 0.14f)
        lineTo(towerLeft + towerW * 0.22f, towerTop)
        lineTo(towerLeft + towerW * 0.78f, towerTop)
        lineTo(towerLeft + towerW * 0.78f, towerTop + bh * 0.14f)
        lineTo(towerLeft + towerW, towerTop + bh * 0.14f)
        lineTo(towerLeft + towerW, bottom)
        close()
    }
    drawPath(towerBody, fillCol)
    drawPath(towerBody, lineCol, style = stroke)

    // Main arched palace facade
    val facadeLeft = towerLeft + towerW * 0.92f
    val facadeW = right - facadeLeft
    val roofY = top + bh * 0.28f
    val domeCx = facadeLeft + facadeW * 0.50f

    // Rooftop cupola/dome
    val domePath = Path().apply {
        val domeR = facadeW * 0.095f
        arcTo(
            rect = Rect(domeCx - domeR, roofY - domeR * 1.4f, domeCx + domeR, roofY + domeR * 0.4f),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        close()
    }
    drawPath(domePath, fillCol)
    drawPath(domePath, lineCol, style = stroke)

    // Facade body with crenellated parapet
    val facadePath = Path().apply {
        moveTo(facadeLeft, bottom)
        lineTo(facadeLeft, roofY)
        val merlons = 14
        val step = facadeW / merlons
        for (i in 0 until merlons) {
            val mx = facadeLeft + i * step
            val dip = if (i % 2 == 1) bh * 0.03f else 0f
            lineTo(mx, roofY + dip)
            lineTo(mx + step, roofY + dip)
        }
        lineTo(right, bottom)
        close()
    }
    drawPath(facadePath, fillCol)
    drawPath(facadePath, lineCol, style = stroke)

    // Central grand Moorish horseshoe arch
    val centerArchW = facadeW * 0.24f
    val centerArchLeft = domeCx - centerArchW / 2f
    val centerArchTop = roofY + bh * 0.18f
    val centerArchPath = Path().apply {
        moveTo(centerArchLeft, bottom)
        lineTo(centerArchLeft, centerArchTop + centerArchW * 0.55f)
        quadraticTo(
            domeCx, centerArchTop - centerArchW * 0.15f,
            centerArchLeft + centerArchW, centerArchTop + centerArchW * 0.55f
        )
        lineTo(centerArchLeft + centerArchW, bottom)
        close()
    }
    drawPath(centerArchPath, CheckCreamMid.copy(alpha = 0.55f))
    drawPath(centerArchPath, lineCol, style = stroke)

    // Flanking symmetrical Moorish arcade arches (2 left, 2 right)
    val sideOffsets = listOf(0.06f, 0.21f, 0.67f, 0.82f)
    val sideArchW = facadeW * 0.11f
    val sideArchTop = roofY + bh * 0.27f
    for (frac in sideOffsets) {
        val ax = facadeLeft + facadeW * frac
        val arch = Path().apply {
            moveTo(ax, bottom)
            lineTo(ax, sideArchTop + sideArchW * 0.55f)
            quadraticTo(
                ax + sideArchW / 2f, sideArchTop,
                ax + sideArchW, sideArchTop + sideArchW * 0.55f
            )
            lineTo(ax + sideArchW, bottom)
            close()
        }
        drawPath(arch, CheckCreamMid.copy(alpha = 0.50f))
        drawPath(arch, lineCol, style = stroke)
    }

    // Upper gallery small arched windows
    val windowY = roofY + bh * 0.08f
    val windowH = bh * 0.08f
    val windowW = facadeW * 0.045f
    for (i in 0..7) {
        val wx = facadeLeft + facadeW * (0.07f + i * 0.12f)
        drawArc(
            color = lineCol,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(wx, windowY),
            size = Size(windowW, windowH),
            style = stroke
        )
    }
}

/**
 * Splits amount-in-words at clean word boundaries so Line 1 fits between the French & Arabic
 * headers and Line 2 overflows cleanly across the full-width second ruled line.
 */
private fun splitAmountWordsAcrossTwoLines(
    words: String,
    line1MaxWidthPx: Int,
    textMeasurer: TextMeasurer,
    style: TextStyle
): Pair<String, String> {
    if (textMeasurer.measure(words, style).size.width <= line1MaxWidthPx) {
        return words to ""
    }

    val tokens = words.split(" ").filter { it.isNotEmpty() }
    if (tokens.size <= 1) return words to ""

    val firstLine = StringBuilder()
    var splitIndex = 0
    for (i in tokens.indices) {
        val candidate = if (firstLine.isEmpty()) tokens[i] else "$firstLine ${tokens[i]}"
        val width = textMeasurer.measure(candidate, style).size.width
        if (width <= line1MaxWidthPx) {
            if (firstLine.isNotEmpty()) firstLine.append(' ')
            firstLine.append(tokens[i])
            splitIndex = i + 1
        } else {
            break
        }
    }

    if (splitIndex == 0) {
        // First token alone exceeds middle slot: push entire phrase onto full-width Line 2
        return "" to words
    }

    val secondLine = tokens.drop(splitIndex).joinToString(" ")
    return firstLine.toString() to secondLine
}
