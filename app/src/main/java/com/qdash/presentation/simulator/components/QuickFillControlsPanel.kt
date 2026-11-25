package com.qdash.presentation.simulator.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.PostalAmountSuggester
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.presentation.simulator.DocumentSimulatorUiState
import com.qdash.presentation.simulator.DocumentType
import com.qdash.presentation.simulator.SfpOperationType
import com.qdash.presentation.simulator.WordsLanguage

private val QUICK_SFP_MOTIFS = listOf(
    "مصاريف شخصية",
    "أمانة / عائلة",
    "إيجار",
    "تسديد فاتورة",
    "راتب / منحة",
    "شراء سلعة"
)
private val QUICK_PI_TYPES = listOf("CNI", "Passeport", "Permis")

private enum class SfpDropdownPanelType {
    NONE,
    OPERATION_AND_SERVICES,
    MOTIF
}

/**
 * Streamlined quick-fill controls for Postal Check and Chèque Secours (SFP 01):
 * - Reactive 3x3 Smart Amount dropdown (no cheat sheet, no horizontal preset row)
 * - Unified side-by-side Operation/Services and Motif dropdowns for Chèque Secours
 * - Saved profiles quick pills & optional manual edit drawer
 * - Place/Date/Clé-Calc/RIP-Paste buttons intentionally removed per UX spec.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickFillControlsPanel(
    uiState: DocumentSimulatorUiState,
    onPullMyAccountClick: () -> Unit = {},
    onOpenProfilePicker: (PostalProfileRole) -> Unit = {},
    onSelectProfileDirect: (PostalProfile, PostalProfileRole) -> Unit = { _, _ -> },
    onMoiMemeClick: () -> Unit = {},
    onToggleWordsLanguage: () -> Unit = {},
    onToggleManualEditMode: () -> Unit = {},
    onClearAmountClick: () -> Unit = {},
    onChequeNumberChange: (String) -> Unit = {},
    onChequeAmountChange: (String) -> Unit = {},
    onChequeBeneficiaryChange: (String) -> Unit = {},
    onChequeCcpChange: (String) -> Unit = {},
    onChequeKeyChange: (String) -> Unit = {},
    onChequeAccountHolderChange: (String) -> Unit = {},
    onChequeAccountAddressChange: (String) -> Unit = {},
    onChequePlaceChange: (String) -> Unit = {},
    onChequeDateChange: (String) -> Unit = {},
    onSfpOperationChange: (SfpOperationType) -> Unit = {},
    onSfpReleveOperationsChange: (Boolean) -> Unit = {},
    onSfpAvecSoldeChange: (Boolean) -> Unit = {},
    onSfpNotificationAvoirChange: (Boolean) -> Unit = {},
    onSfpCommandeCarnetChange: (Boolean) -> Unit = {},
    onSfpReleveDuChange: (String) -> Unit = {},
    onSfpReleveAuChange: (String) -> Unit = {},
    onSfpAmountChange: (String) -> Unit = {},
    onSfpCcpChange: (String) -> Unit = {},
    onSfpKeyChange: (String) -> Unit = {},
    onSfpSenderNomChange: (String) -> Unit = {},
    onSfpSenderPrenomChange: (String) -> Unit = {},
    onSfpSenderAddressChange: (String) -> Unit = {},
    onSfpBeneficiaryNomChange: (String) -> Unit = {},
    onSfpBeneficiaryPrenomChange: (String) -> Unit = {},
    onSfpBeneficiaryAddressChange: (String) -> Unit = {},
    onSfpMotifChange: (String) -> Unit = {},
    onSfpPlaceChange: (String) -> Unit = {},
    onSfpDateChange: (String) -> Unit = {},
    onSfpPiTypeChange: (String) -> Unit = {},
    onSfpPiNumeroChange: (String) -> Unit = {},
    onSfpPiDelivreLeChange: (String) -> Unit = {},
    onSfpPiParChange: (String) -> Unit = {},
    onSfpIdDescriptionChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isCheque = uiState.selectedDocType == DocumentType.CHEQUE
    val currentAmountStr = if (isCheque) uiState.chequeAmount else uiState.sfpAmount
    val onCurrentAmountChange = if (isCheque) onChequeAmountChange else onSfpAmountChange
    val wordsLanguage = if (isCheque) uiState.chequeWordsLanguage else uiState.sfpWordsLanguage

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_fill_panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Quick-Action Strip
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPullMyAccountClick,
                modifier = Modifier.testTag("btn_pull_my_account"),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حسابي المسجل (CCP)", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            if (isCheque) {
                val isMoiMeme = uiState.chequeBeneficiary.equals("لنفسي", ignoreCase = true) ||
                    uiState.chequeBeneficiary.equals("Moi-même", ignoreCase = true)
                FilterChip(
                    selected = isMoiMeme,
                    onClick = onMoiMemeClick,
                    modifier = Modifier.testTag("chip_moi_meme"),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("لنفسي / Moi-même", fontSize = 12.sp, maxLines = 1) }
                )
            }

            FilterChip(
                selected = wordsLanguage == WordsLanguage.FRENCH,
                onClick = onToggleWordsLanguage,
                modifier = Modifier.testTag("toggle_words_lang"),
                leadingIcon = {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = {
                    val label = if (wordsLanguage == WordsLanguage.ARABIC) "التفقيط: عربي" else "En lettres: FR"
                    Text(label, fontSize = 12.sp, maxLines = 1)
                }
            )

            FilterChip(
                selected = uiState.isManualEditMode,
                onClick = onToggleManualEditMode,
                modifier = Modifier.testTag("toggle_manual_edit"),
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("تعديل يدوي", fontSize = 12.sp, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }

        // 2. Smart Amount Dropdown Selector (Input + Live 3x3 Prefix Suggestions Grid)
        SmartAmountDropdownSection(
            currentAmountRaw = currentAmountStr,
            onAmountChange = onCurrentAmountChange,
            onClearAmountClick = onClearAmountClick
        )

        // 3. Live Tafqit Summary Banner
        val parsedAmount = AmountConversionEngine.parseFlexibleAmount(currentAmountStr)
        if (parsedAmount != null && parsedAmount > 0.0) {
            TafqitSummaryBanner(
                amount = parsedAmount,
                wordsLanguage = wordsLanguage,
                onCopyWords = { textToCopy ->
                    copyToClipboard(context, "words", textToCopy)
                    Toast.makeText(context, "تم نسخ المبلغ بالحروف!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 4. Chèque Secours Unified Side-by-Side Dropdowns (Operation/Services + Motif)
        if (!isCheque) {
            SfpUnifiedDropdownsSection(
                uiState = uiState,
                onSfpOperationChange = onSfpOperationChange,
                onSfpReleveOperationsChange = onSfpReleveOperationsChange,
                onSfpAvecSoldeChange = onSfpAvecSoldeChange,
                onSfpNotificationAvoirChange = onSfpNotificationAvoirChange,
                onSfpCommandeCarnetChange = onSfpCommandeCarnetChange,
                onSfpReleveDuChange = onSfpReleveDuChange,
                onSfpReleveAuChange = onSfpReleveAuChange,
                onSfpMotifChange = onSfpMotifChange
            )
        }

        // 5. Saved Beneficiaries / Profiles Quick Chips
        SavedProfilesQuickRow(
            profiles = uiState.savedProfiles,
            isCheque = isCheque,
            sfpOperation = uiState.sfpOperation,
            onSelectProfileDirect = onSelectProfileDirect,
            onOpenProfilePicker = onOpenProfilePicker
        )

        // 6. Optional Manual Edit Section
        AnimatedVisibility(
            visible = uiState.isManualEditMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ManualEditSection(
                uiState = uiState,
                onChequeNumberChange = onChequeNumberChange,
                onChequeAmountChange = onChequeAmountChange,
                onChequeBeneficiaryChange = onChequeBeneficiaryChange,
                onChequeCcpChange = onChequeCcpChange,
                onChequeKeyChange = onChequeKeyChange,
                onChequeAccountHolderChange = onChequeAccountHolderChange,
                onChequeAccountAddressChange = onChequeAccountAddressChange,
                onChequePlaceChange = onChequePlaceChange,
                onChequeDateChange = onChequeDateChange,
                onSfpAmountChange = onSfpAmountChange,
                onSfpCcpChange = onSfpCcpChange,
                onSfpKeyChange = onSfpKeyChange,
                onSfpSenderNomChange = onSfpSenderNomChange,
                onSfpSenderPrenomChange = onSfpSenderPrenomChange,
                onSfpSenderAddressChange = onSfpSenderAddressChange,
                onSfpBeneficiaryNomChange = onSfpBeneficiaryNomChange,
                onSfpBeneficiaryPrenomChange = onSfpBeneficiaryPrenomChange,
                onSfpBeneficiaryAddressChange = onSfpBeneficiaryAddressChange,
                onSfpMotifChange = onSfpMotifChange,
                onSfpPlaceChange = onSfpPlaceChange,
                onSfpDateChange = onSfpDateChange,
                onSfpReleveDuChange = onSfpReleveDuChange,
                onSfpReleveAuChange = onSfpReleveAuChange,
                onSfpPiTypeChange = onSfpPiTypeChange,
                onSfpPiNumeroChange = onSfpPiNumeroChange,
                onSfpPiDelivreLeChange = onSfpPiDelivreLeChange,
                onSfpPiParChange = onSfpPiParChange,
                onSfpIdDescriptionChange = onSfpIdDescriptionChange
            )
        }
    }
}

@Composable
private fun SmartAmountDropdownSection(
    currentAmountRaw: String,
    onAmountChange: (String) -> Unit,
    onClearAmountClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val parsedAmount = AmountConversionEngine.parseFlexibleAmount(currentAmountRaw)
    val gridAmounts = remember(currentAmountRaw) {
        PostalAmountSuggester.suggestGridAmounts(currentAmountRaw)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Dropdown Trigger Button
        Surface(
            onClick = { isExpanded = !isExpanded },
            shape = RoundedCornerShape(14.dp),
            color = if (isExpanded) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(
                width = if (isExpanded) 1.5.dp else 1.dp,
                color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("amount_dropdown_trigger")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المبلغ بالدينار (DA)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (parsedAmount != null && parsedAmount > 0.0) {
                            "\u2066${AmountConversionEngine.formatAmountToPostal(parsedAmount)} DA\u2069"
                        } else {
                            "اضغط لإدخال أو اختيار المبلغ"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (parsedAmount != null && parsedAmount > 0.0) FontFamily.Monospace else FontFamily.Default
                        ),
                        color = if (parsedAmount != null && parsedAmount > 0.0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (currentAmountRaw.isNotBlank()) {
                    IconButton(
                        onClick = onClearAmountClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_clear_amount")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "مسح المبلغ",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "إغلاق قائمة المبلغ" else "فتح قائمة المبلغ",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Expandable Dropdown Box: Input Rectangle + Live 3x3 Suggestions Grid
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_dropdown_panel"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = currentAmountRaw,
                        onValueChange = onAmountChange,
                        label = { Text("اكتب المبلغ يدوياً (مثلاً 2 أو 5200)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_dropdown_input"),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            if (currentAmountRaw.isNotEmpty()) {
                                IconButton(onClick = onClearAmountClick) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        }
                    )

                    Text(
                        text = if (currentAmountRaw.isBlank()) {
                            "المبالغ الشائعة (اضغط للاختيار):"
                        } else {
                            "اقتراحات مطابقة لـ «${currentAmountRaw}»:"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 3 rows x 3 columns grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_suggestions_grid"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridAmounts.chunked(3).forEach { rowAmounts ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowAmounts.forEach { suggested ->
                                    val isSelected = parsedAmount != null &&
                                        kotlin.math.abs(parsedAmount - suggested.toDouble()) < 0.001
                                    Surface(
                                        onClick = {
                                            onAmountChange(suggested.toString())
                                            isExpanded = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("amount_grid_item_$suggested")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = "\u2066${PostalAmountSuggester.formatGridAmount(suggested)}\u2069",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp
                                                ),
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SfpUnifiedDropdownsSection(
    uiState: DocumentSimulatorUiState,
    onSfpOperationChange: (SfpOperationType) -> Unit,
    onSfpReleveOperationsChange: (Boolean) -> Unit,
    onSfpAvecSoldeChange: (Boolean) -> Unit,
    onSfpNotificationAvoirChange: (Boolean) -> Unit,
    onSfpCommandeCarnetChange: (Boolean) -> Unit,
    onSfpReleveDuChange: (String) -> Unit,
    onSfpReleveAuChange: (String) -> Unit,
    onSfpMotifChange: (String) -> Unit
) {
    var openPanel by remember { mutableStateOf(SfpDropdownPanelType.NONE) }

    val operationSummary = remember(
        uiState.sfpOperation,
        uiState.sfpReleveOperations,
        uiState.sfpAvecSolde,
        uiState.sfpNotificationAvoir,
        uiState.sfpCommandeCarnet
    ) {
        val base = when (uiState.sfpOperation) {
            SfpOperationType.VERSEMENT -> "دفع"
            SfpOperationType.RETRAIT -> "سحب"
            SfpOperationType.VIREMENT -> "تحويل"
        }
        val extraCount = listOf(
            uiState.sfpReleveOperations,
            uiState.sfpAvecSolde,
            uiState.sfpNotificationAvoir,
            uiState.sfpCommandeCarnet
        ).count { it }
        if (extraCount > 0) "$base +$extraCount" else base
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Two unified dropdown buttons side-by-side in one Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UnifiedDropdownTriggerBox(
                title = "العملية والخدمات",
                valueText = operationSummary,
                isExpanded = openPanel == SfpDropdownPanelType.OPERATION_AND_SERVICES,
                onClick = {
                    openPanel = if (openPanel == SfpDropdownPanelType.OPERATION_AND_SERVICES) {
                        SfpDropdownPanelType.NONE
                    } else {
                        SfpDropdownPanelType.OPERATION_AND_SERVICES
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("sfp_operation_dropdown_trigger")
            )

            UnifiedDropdownTriggerBox(
                title = "سبب العملية",
                valueText = uiState.sfpMotif.ifBlank { "اختر السبب" },
                isExpanded = openPanel == SfpDropdownPanelType.MOTIF,
                onClick = {
                    openPanel = if (openPanel == SfpDropdownPanelType.MOTIF) {
                        SfpDropdownPanelType.NONE
                    } else {
                        SfpDropdownPanelType.MOTIF
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("sfp_motif_dropdown_trigger")
            )
        }

        // Unified Dropdown Content Panel
        AnimatedVisibility(
            visible = openPanel != SfpDropdownPanelType.NONE,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sfp_unified_dropdown_panel"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (openPanel) {
                        SfpDropdownPanelType.OPERATION_AND_SERVICES -> {
                            Text(
                                text = "نوع العملية والخدمات المرفقة",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.sfpOperation == SfpOperationType.VERSEMENT,
                                    onClick = { onSfpOperationChange(SfpOperationType.VERSEMENT) },
                                    label = { Text("دفع Versement", fontSize = 12.sp, maxLines = 1) }
                                )
                                FilterChip(
                                    selected = uiState.sfpOperation == SfpOperationType.RETRAIT,
                                    onClick = { onSfpOperationChange(SfpOperationType.RETRAIT) },
                                    label = { Text("سحب Retrait", fontSize = 12.sp, maxLines = 1) }
                                )
                                FilterChip(
                                    selected = uiState.sfpOperation == SfpOperationType.VIREMENT,
                                    onClick = { onSfpOperationChange(SfpOperationType.VIREMENT) },
                                    label = { Text("تحويل Virement", fontSize = 12.sp, maxLines = 1) }
                                )
                                FilterChip(
                                    selected = uiState.sfpReleveOperations,
                                    onClick = { onSfpReleveOperationsChange(!uiState.sfpReleveOperations) },
                                    label = { Text("كشف العمليات", fontSize = 12.sp, maxLines = 1) }
                                )
                                FilterChip(
                                    selected = uiState.sfpAvecSolde,
                                    onClick = { onSfpAvecSoldeChange(!uiState.sfpAvecSolde) },
                                    label = { Text("إظهار الرصيد", fontSize = 12.sp, maxLines = 1) }
                                )
                                FilterChip(
                                    selected = uiState.sfpNotificationAvoir,
                                    onClick = { onSfpNotificationAvoirChange(!uiState.sfpNotificationAvoir) },
                                    label = { Text("إشعار بالرصيد", fontSize = 12.sp, maxLines = 1) }
                                )
                                FilterChip(
                                    selected = uiState.sfpCommandeCarnet,
                                    onClick = { onSfpCommandeCarnetChange(!uiState.sfpCommandeCarnet) },
                                    label = { Text("طلب دفتر", fontSize = 12.sp, maxLines = 1) }
                                )
                            }

                            if (uiState.sfpReleveOperations) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.sfpReleveDu,
                                        onValueChange = onSfpReleveDuChange,
                                        label = { Text("من (Du)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = uiState.sfpReleveAu,
                                        onValueChange = onSfpReleveAuChange,
                                        label = { Text("إلى (Au)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        SfpDropdownPanelType.MOTIF -> {
                            Text(
                                text = "سبب العملية (Motif)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QUICK_SFP_MOTIFS.forEach { motif ->
                                    FilterChip(
                                        selected = uiState.sfpMotif == motif,
                                        onClick = {
                                            onSfpMotifChange(motif)
                                            openPanel = SfpDropdownPanelType.NONE
                                        },
                                        label = { Text(motif, fontSize = 12.sp, maxLines = 1) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = uiState.sfpMotif,
                                onValueChange = onSfpMotifChange,
                                label = { Text("أو اكتب سبباً مخصصاً") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    if (uiState.sfpMotif.isNotEmpty()) {
                                        IconButton(onClick = { onSfpMotifChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                                        }
                                    }
                                }
                            )
                        }

                        SfpDropdownPanelType.NONE -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedDropdownTriggerBox(
    title: String,
    valueText: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isExpanded) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TafqitSummaryBanner(
    amount: Double,
    wordsLanguage: WordsLanguage,
    onCopyWords: (String) -> Unit
) {
    val formatted = AmountConversionEngine.formatAmountToPostal(amount)
    val colloquial = AmountConversionEngine.getAlgerianColloquialWords(amount)
    val arabicOfficial = AmountConversionEngine.convertToArabicWords(amount, preferCentimes = false)
    val frenchOfficial = AmountConversionEngine.convertToFrenchWords(amount)
    val primaryWords = if (wordsLanguage == WordsLanguage.FRENCH) frenchOfficial else arabicOfficial
    val secondaryWords = if (wordsLanguage == WordsLanguage.FRENCH) arabicOfficial else frenchOfficial

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التنسيق المضاد للتزوير:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\u2066#$formatted#\u2069",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Text(
                text = "بالسنتيم (الدارجة): $colloquial",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (wordsLanguage == WordsLanguage.FRENCH) "En lettres (FR): $primaryWords" else "بالفصحى: $primaryWords",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (wordsLanguage == WordsLanguage.FRENCH) "بالفصحى: $secondaryWords" else "FR: $secondaryWords",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCopyWords(primaryWords) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ الحروف",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "نسخ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedProfilesQuickRow(
    profiles: List<PostalProfile>,
    isCheque: Boolean,
    sfpOperation: SfpOperationType,
    onSelectProfileDirect: (PostalProfile, PostalProfileRole) -> Unit,
    onOpenProfilePicker: (PostalProfileRole) -> Unit
) {
    val sorted = remember(profiles) {
        profiles.sortedByDescending { it.isFavorite }
    }
    val defaultPickRole = when {
        isCheque -> PostalProfileRole.BENEFICIARY
        sfpOperation == SfpOperationType.RETRAIT -> PostalProfileRole.SELF
        else -> PostalProfileRole.BENEFICIARY
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "حسابات ومستفيدون محفوظون",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = { onOpenProfilePicker(defaultPickRole) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("القائمة", fontSize = 12.sp)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(sorted, key = { it.id }) { profile ->
                val applyRole = resolveQuickTapRole(profile, isCheque, sfpOperation)
                FilterChip(
                    selected = false,
                    onClick = { onSelectProfileDirect(profile, applyRole) },
                    leadingIcon = if (profile.isFavorite) {
                        { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    label = {
                        Text(
                            text = profile.profileName,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            if (sorted.isEmpty()) {
                item {
                    OutlinedButton(
                        onClick = { onOpenProfilePicker(defaultPickRole) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("اختر من الحسابات المحفوظة", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun resolveQuickTapRole(
    profile: PostalProfile,
    isCheque: Boolean,
    sfpOperation: SfpOperationType
): PostalProfileRole {
    return if (isCheque) {
        if (profile.defaultRole == PostalProfileRole.SELF) PostalProfileRole.SELF else PostalProfileRole.BENEFICIARY
    } else {
        when (profile.defaultRole) {
            PostalProfileRole.BENEFICIARY -> PostalProfileRole.BENEFICIARY
            PostalProfileRole.SENDER -> PostalProfileRole.SENDER
            PostalProfileRole.SELF -> if (sfpOperation == SfpOperationType.RETRAIT) PostalProfileRole.SELF else PostalProfileRole.SENDER
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualEditSection(
    uiState: DocumentSimulatorUiState,
    onChequeNumberChange: (String) -> Unit,
    onChequeAmountChange: (String) -> Unit,
    onChequeBeneficiaryChange: (String) -> Unit,
    onChequeCcpChange: (String) -> Unit,
    onChequeKeyChange: (String) -> Unit,
    onChequeAccountHolderChange: (String) -> Unit,
    onChequeAccountAddressChange: (String) -> Unit,
    onChequePlaceChange: (String) -> Unit,
    onChequeDateChange: (String) -> Unit,
    onSfpAmountChange: (String) -> Unit,
    onSfpCcpChange: (String) -> Unit,
    onSfpKeyChange: (String) -> Unit,
    onSfpSenderNomChange: (String) -> Unit,
    onSfpSenderPrenomChange: (String) -> Unit,
    onSfpSenderAddressChange: (String) -> Unit,
    onSfpBeneficiaryNomChange: (String) -> Unit,
    onSfpBeneficiaryPrenomChange: (String) -> Unit,
    onSfpBeneficiaryAddressChange: (String) -> Unit,
    onSfpMotifChange: (String) -> Unit,
    onSfpPlaceChange: (String) -> Unit,
    onSfpDateChange: (String) -> Unit,
    onSfpReleveDuChange: (String) -> Unit,
    onSfpReleveAuChange: (String) -> Unit,
    onSfpPiTypeChange: (String) -> Unit,
    onSfpPiNumeroChange: (String) -> Unit,
    onSfpPiDelivreLeChange: (String) -> Unit,
    onSfpPiParChange: (String) -> Unit,
    onSfpIdDescriptionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("manual_edit_section"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "التعديل اليدوي الكامل",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            if (uiState.selectedDocType == DocumentType.CHEQUE) {
                OutlinedTextField(
                    value = uiState.chequeAmount,
                    onValueChange = onChequeAmountChange,
                    label = { Text("المبلغ بالدينار (DA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        if (uiState.chequeAmount.isNotEmpty()) {
                            IconButton(onClick = { onChequeAmountChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = uiState.chequeBeneficiary,
                    onValueChange = onChequeBeneficiaryChange,
                    label = { Text("المستفيد (لأمر / A l'ordre de)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                CcpAndKeyManualRow(
                    ccp = uiState.chequeCcp,
                    key = uiState.chequeKey,
                    onCcpChange = onChequeCcpChange,
                    onKeyChange = onChequeKeyChange
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.chequePlace,
                        onValueChange = onChequePlaceChange,
                        label = { Text("المكان") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = uiState.chequeDate,
                        onValueChange = onChequeDateChange,
                        label = { Text("التاريخ") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = uiState.chequeNumber,
                    onValueChange = onChequeNumberChange,
                    label = { Text("رقم الصك (Chèque N°)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = uiState.chequeAccountHolder,
                    onValueChange = onChequeAccountHolderChange,
                    label = { Text("صاحب الحساب (M. / Mme)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = uiState.chequeAccountAddress,
                    onValueChange = onChequeAccountAddressChange,
                    label = { Text("عنوان صاحب الحساب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                OutlinedTextField(
                    value = uiState.sfpAmount,
                    onValueChange = onSfpAmountChange,
                    label = { Text("المبلغ بالدينار (DA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        if (uiState.sfpAmount.isNotEmpty()) {
                            IconButton(onClick = { onSfpAmountChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    }
                )

                CcpAndKeyManualRow(
                    ccp = uiState.sfpCcp,
                    key = uiState.sfpKey,
                    onCcpChange = onSfpCcpChange,
                    onKeyChange = onSfpKeyChange
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.sfpSenderNom,
                        onValueChange = onSfpSenderNomChange,
                        label = { Text("لقب المرسل (Nom)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = uiState.sfpSenderPrenom,
                        onValueChange = onSfpSenderPrenomChange,
                        label = { Text("اسم المرسل (Prénom)") },
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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

                OutlinedTextField(
                    value = uiState.sfpMotif,
                    onValueChange = onSfpMotifChange,
                    label = { Text("الموضوع / السبب (Motif)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.sfpPlace,
                        onValueChange = onSfpPlaceChange,
                        label = { Text("المكان") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = uiState.sfpDate,
                        onValueChange = onSfpDateChange,
                        label = { Text("التاريخ") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (uiState.sfpReleveOperations) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.sfpReleveDu,
                            onValueChange = onSfpReleveDuChange,
                            label = { Text("من تاريخ (Du)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = uiState.sfpReleveAu,
                            onValueChange = onSfpReleveAuChange,
                            label = { Text("إلى تاريخ (Au)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_PI_TYPES.forEach { piType ->
                        FilterChip(
                            selected = uiState.sfpPiType == piType,
                            onClick = { onSfpPiTypeChange(piType) },
                            label = { Text(piType, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.sfpPiType,
                        onValueChange = onSfpPiTypeChange,
                        label = { Text("نوع الوثيقة (P.I)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = uiState.sfpPiNumero,
                        onValueChange = onSfpPiNumeroChange,
                        label = { Text("رقم الوثيقة (N°)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.sfpPiDelivreLe,
                        onValueChange = onSfpPiDelivreLeChange,
                        label = { Text("صدرت بتاريخ") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = uiState.sfpPiPar,
                        onValueChange = onSfpPiParChange,
                        label = { Text("عن (Par)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = uiState.sfpIdDescription,
                    onValueChange = onSfpIdDescriptionChange,
                    label = { Text("وصف مختصر للهوية (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
private fun CcpAndKeyManualRow(
    ccp: String,
    key: String,
    onCcpChange: (String) -> Unit,
    onKeyChange: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = ccp,
            onValueChange = onCcpChange,
            label = { Text("رقم الحساب CCP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(2f),
            shape = RoundedCornerShape(10.dp)
        )
        OutlinedTextField(
            value = key,
            onValueChange = onKeyChange,
            label = { Text("المفتاح") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cb.setPrimaryClip(ClipData.newPlainText(label, text))
}
