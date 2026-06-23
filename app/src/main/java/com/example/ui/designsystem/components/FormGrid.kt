package com.example.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.usecase.simulator.AmountConversionEngine
import com.example.presentation.simulator.DocumentSimulatorUiState
import com.example.presentation.simulator.SfpOperationType
import com.example.ui.designsystem.tokens.ColorTokens

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
    modifier: Modifier = Modifier
) {
    val formBg = ColorTokens.SfpFormBg
    val lineColor = ColorTokens.SfpBorderBrown
    val headerBg = ColorTokens.SfpHeaderYellow
    val writingColor = Color(0xFF1A1A2E)

    val formattedAmount = remember(uiState.sfpAmount) {
        val amt = uiState.sfpAmount.toDoubleOrNull()
        if (amt != null) AmountConversionEngine.formatAmountToPostal(amt) else ""
    }

    val amountInWords = remember(uiState.sfpAmount) {
        val amt = uiState.sfpAmount.toDoubleOrNull()
        if (amt != null) AmountConversionEngine.convertToArabicWords(amt) else ""
    }

    val isVersement = uiState.sfpOperation == SfpOperationType.VERSEMENT
    val isRetrait = uiState.sfpOperation == SfpOperationType.RETRAIT
    val isVirement = uiState.sfpOperation == SfpOperationType.VIREMENT

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(formBg)
            .border(1.5.dp, lineColor)
    ) {
        // ======= 1. HEADER ROW =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .drawBehind {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Box: SFP 01
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .border(1.dp, lineColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SFP 01",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
            }

            // Center Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.sfp_header_logo),
                    contentDescription = "AP Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "العمليات المالية البريدية",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "Opérations financières postales",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
                        color = Color(0xFF333333)
                    )
                }
            }
            
            // Empty space to balance layout
            Spacer(modifier = Modifier.width(48.dp))
        }

        // ======= 2. OPTIONS & CHECKBOXES ROW =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .drawBehind {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            // Main Operation Type Checkboxes (Mutually Exclusive)
            Row(
                modifier = Modifier
                    .weight(3f)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InlineCheckbox(
                    checked = isVersement,
                    onCheckedChange = { if (it) onOperationChange(SfpOperationType.VERSEMENT) },
                    labelArabic = "دفع",
                    labelFrench = "Versement"
                )
                InlineCheckbox(
                    checked = isRetrait,
                    onCheckedChange = { if (it) onOperationChange(SfpOperationType.RETRAIT) },
                    labelArabic = "سحب",
                    labelFrench = "Retrait"
                )
                InlineCheckbox(
                    checked = isVirement,
                    onCheckedChange = { if (it) onOperationChange(SfpOperationType.VIREMENT) },
                    labelArabic = "تحويل",
                    labelFrench = "Virement"
                )
            }

            // Divider line between Operation & Auxiliary options
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(0.5.dp)
                    .background(lineColor)
            )

            // Auxiliary Checkboxes Column
            Column(
                modifier = Modifier
                    .weight(2f)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                InlineCheckbox(
                    checked = uiState.sfpJustificatifCcp,
                    onCheckedChange = onJustificatifCcpChange,
                    labelArabic = "تبرير الحساب",
                    labelFrench = "Justificatif CCP"
                )
                InlineCheckbox(
                    checked = uiState.sfpAvisCredit,
                    onCheckedChange = onAvisCreditChange,
                    labelArabic = "إشعار بالدائنية",
                    labelFrench = "Avis de crédit"
                )
                InlineCheckbox(
                    checked = uiState.sfpCarnetCheques,
                    onCheckedChange = onCarnetChequesChange,
                    labelArabic = "دفتر صكوك",
                    labelFrench = "Carnet chèques"
                )
            }
        }

        // ======= 3. ACCOUNT NUMBER, KEY & ID CARD =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .drawBehind {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            FormCell(
                modifier = Modifier.weight(2.2f),
                labelArabic = "رقم حساب المرسل إليه",
                labelFrench = "Compte N°",
                isFocused = focusedField == "sfpCcp",
                onTap = { onFieldFocus("sfpCcp") }
            ) {
                SingleLineUnderlineInput(
                    value = uiState.sfpCcp,
                    onValueChange = onCcpChange,
                    placeholder = "_ _ _ _ _ _ _ _ _ _",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = true
                )
            }

            FormCell(
                modifier = Modifier.weight(0.8f),
                labelArabic = "المفتاح",
                labelFrench = "Clé",
                isFocused = focusedField == "sfpKey",
                onTap = { onFieldFocus("sfpKey") }
            ) {
                SingleLineUnderlineInput(
                    value = uiState.sfpKey,
                    onValueChange = onKeyChange,
                    placeholder = "_ _",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = true
                )
            }

            FormCell(
                modifier = Modifier.weight(2.5f),
                labelArabic = "بيانات وثيقة الهوية",
                labelFrench = "Pièce d'identité",
                isFocused = focusedField == "sfpIdDescription",
                onTap = { onFieldFocus("sfpIdDescription") }
            ) {
                SingleLineUnderlineInput(
                    value = uiState.sfpIdDescription,
                    onValueChange = onIdDescriptionChange,
                    placeholder = "الرقم والجهة المصدرة",
                    enabled = true
                )
            }
        }

        // ======= 4. AMOUNTS (DIGITS AND WORDS) =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .drawBehind {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            FormCell(
                modifier = Modifier.weight(2f),
                labelArabic = "المبلغ بالأرقام",
                labelFrench = "Montant en chiffres",
                isFocused = focusedField == "sfpAmount",
                onTap = { onFieldFocus("sfpAmount") }
            ) {
                SingleLineUnderlineInput(
                    value = uiState.sfpAmount,
                    onValueChange = onAmountChange,
                    placeholder = "0.00 DA",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = true
                )
            }

            FormCell(
                modifier = Modifier.weight(3f),
                labelArabic = "بالحروف",
                labelFrench = "Montant en lettres",
                isFocused = false
            ) {
                Text(
                    text = if (amountInWords.isNotEmpty()) amountInWords else "خمسة آلاف دينار جزائري...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (amountInWords.isNotEmpty()) writingColor else ColorTokens.TextGray.copy(alpha = 0.5f)
                    ),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ======= 5. SENDER DETAILS (EXPEDITEUR - DONNEUR D'ORDRE) =======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Expéditeur - Donneur d'ordre",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                    color = ColorTokens.SfpSenderRed
                )
                Text(
                    text = "المرسل / دافع الأموال",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                    color = ColorTokens.SfpSenderRed
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FormCell(
                    modifier = Modifier.weight(1f).height(40.dp),
                    labelArabic = "اللقب",
                    labelFrench = "Nom",
                    isFocused = focusedField == "sfpSenderNom" || focusedField == "sfpSenderName",
                    onTap = { onFieldFocus("sfpSenderNom") }
                ) {
                    SingleLineUnderlineInput(
                        value = uiState.sfpSenderNom,
                        onValueChange = onSenderNomChange,
                        placeholder = "Nom",
                        fontSize = 10f
                    )
                }
                FormCell(
                    modifier = Modifier.weight(1f).height(40.dp),
                    labelArabic = "الاسم",
                    labelFrench = "Prénom",
                    isFocused = focusedField == "sfpSenderPrenom" || focusedField == "sfpSenderName",
                    onTap = { onFieldFocus("sfpSenderPrenom") }
                ) {
                    SingleLineUnderlineInput(
                        value = uiState.sfpSenderPrenom,
                        onValueChange = onSenderPrenomChange,
                        placeholder = "Prénom",
                        fontSize = 10f
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FormCell(
                    modifier = Modifier.weight(1.5f).height(40.dp),
                    labelArabic = "العنوان",
                    labelFrench = "Adresse",
                    isFocused = focusedField == "sfpSenderAddress",
                    onTap = { onFieldFocus("sfpSenderAddress") }
                ) {
                    SingleLineUnderlineInput(
                        value = uiState.sfpSenderAddress,
                        onValueChange = onSenderAddressChange,
                        placeholder = "Adresse",
                        fontSize = 10f
                    )
                }
                FormCell(
                    modifier = Modifier.weight(1f).height(40.dp),
                    labelArabic = "رقم الهاتف",
                    labelFrench = "N° Tél",
                    isFocused = focusedField == "sfpSenderPhone",
                    onTap = { onFieldFocus("sfpSenderPhone") }
                ) {
                    SingleLineUnderlineInput(
                        value = uiState.sfpSenderPhone,
                        onValueChange = onSenderPhoneChange,
                        placeholder = "0600000000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        fontSize = 10f
                    )
                }
            }
        }

        // ======= 6. BENEFICIARY DETAILS (BENEFICIAIRE) =======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bénéficiaire",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                    color = ColorTokens.SfpBeneficiaryGreen
                )
                Text(
                    text = "المرسل إليه / المستفيد",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                    color = ColorTokens.SfpBeneficiaryGreen
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FormCell(
                    modifier = Modifier.weight(1f).height(40.dp),
                    labelArabic = "اللقب",
                    labelFrench = "Nom",
                    isFocused = focusedField == "sfpBeneficiaryNom" || focusedField == "sfpBeneficiaryName",
                    onTap = { onFieldFocus("sfpBeneficiaryNom") }
                ) {
                    SingleLineUnderlineInput(
                        value = uiState.sfpBeneficiaryNom,
                        onValueChange = onBeneficiaryNomChange,
                        placeholder = "Nom",
                        fontSize = 10f
                    )
                }
                FormCell(
                    modifier = Modifier.weight(1f).height(40.dp),
                    labelArabic = "الاسم",
                    labelFrench = "Prénom",
                    isFocused = focusedField == "sfpBeneficiaryPrenom" || focusedField == "sfpBeneficiaryName",
                    onTap = { onFieldFocus("sfpBeneficiaryPrenom") }
                ) {
                    SingleLineUnderlineInput(
                        value = uiState.sfpBeneficiaryPrenom,
                        onValueChange = onBeneficiaryPrenomChange,
                        placeholder = "Prénom",
                        fontSize = 10f
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FormCell(
                    modifier = Modifier.weight(1.5f).height(40.dp),
                    labelArabic = "العنوان",
                    labelFrench = "Adresse",
                    isFocused = focusedField == "sfpBeneficiaryAddress",
                    onTap = { onFieldFocus("sfpBeneficiaryAddress") }
                ) {
                    SingleLineUnderlineInput(
                        value = uiState.sfpBeneficiaryAddress,
                        onValueChange = onBeneficiaryAddressChange,
                        placeholder = "Adresse",
                        fontSize = 10f
                    )
                }
                FormCell(
                    modifier = Modifier.weight(1f).height(40.dp),
                    labelArabic = "رقم الهاتف",
                    labelFrench = "N° Tél",
                    isFocused = focusedField == "sfpBeneficiaryPhone",
                    onTap = { onFieldFocus("sfpBeneficiaryPhone") }
                ) {
                    SingleLineUnderlineInput(
                        value = uiState.sfpBeneficiaryPhone,
                        onValueChange = onBeneficiaryPhoneChange,
                        placeholder = "0600000000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        fontSize = 10f
                    )
                }
            }
        }

        // ======= 7. MOTIF / CORRESPONDANCE =======
        FormCell(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            labelArabic = "سبب الإرسال / المراسلة",
            labelFrench = "Motif - Correspondance",
            isFocused = false
        ) {
            SingleLineUnderlineInput(
                value = "",
                onValueChange = {},
                placeholder = "سبب العملية المالية...",
                enabled = false
            )
        }

        // ======= 8. PLACE, DATE & SIGNATURE =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .drawBehind {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            FormCell(
                modifier = Modifier.weight(2f),
                labelArabic = "في / بتاريخ",
                labelFrench = "A / Le",
                isFocused = focusedField == "sfpPlaceDate" || focusedField == "sfpPlace" || focusedField == "sfpDate",
                onTap = { onFieldFocus("sfpPlace") }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SingleLineUnderlineInput(
                        value = uiState.sfpPlace,
                        onValueChange = onPlaceChange,
                        placeholder = "المكان",
                        modifier = Modifier.weight(1f),
                        fontSize = 10f
                    )
                    SingleLineUnderlineInput(
                        value = uiState.sfpDate,
                        onValueChange = onDateChange,
                        placeholder = "التاريخ",
                        modifier = Modifier.weight(1f),
                        fontSize = 10f
                    )
                }
            }

            FormCell(
                modifier = Modifier.weight(1.5f),
                labelArabic = "الإمضاء",
                labelFrench = "Signature",
                isFocused = focusedField == "sfpSignature",
                onTap = { onFieldFocus("sfpSignature") }
            ) {
                Text(
                    text = "وقع هنا / Signez",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.5.sp,
                        color = ColorTokens.TextGray.copy(alpha = 0.5f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
            }
        }

        // ======= 9. CADRE RESERVE AU BUREAU DE POSTE =======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF9C4)) // Pale Yellow
                .border(BorderStroke(1.dp, lineColor))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cadre réservé au bureau de poste",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF4E342E)
                )
                Text(
                    text = "خاص بمكتب البريد / ختم الآلة والعميل",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF4E342E)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اترك هذا القسم فارغاً — يملؤه عون البريد",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.5.sp, color = Color(0xFF8D6E63))
                )
                
                // Cachet Area
                Box(
                    modifier = Modifier
                        .size(36.dp, 20.dp)
                        .border(0.5.dp, Color(0xFF8D6E63)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "T.A.D - Cachet",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 5.sp, color = Color(0xFF8D6E63))
                    )
                }
            }
        }

        // ======= 10. RECEIPT STRIP =======
        Divider(color = lineColor.copy(alpha = 0.6f), thickness = 0.5.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorTokens.SfpFormBg)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "وصل العملية / Reçu de l'opération (SFP 01)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFF4E342E)
            )
        }
    }
}
