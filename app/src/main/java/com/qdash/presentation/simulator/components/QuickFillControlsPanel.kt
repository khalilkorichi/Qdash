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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.PostalCcpUtils
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.presentation.simulator.DocumentSimulatorUiState
import com.qdash.presentation.simulator.DocumentType
import com.qdash.presentation.simulator.SfpOperationType
import com.qdash.presentation.simulator.WordsLanguage

private val PRESET_AMOUNTS = listOf(1000, 2000, 5000, 10000, 20000)
private val INCREMENT_DELTAS = listOf(500, 1000, 5000)
private val QUICK_SFP_MOTIFS = listOf("مصاريف شخصية", "أمانة / عائلة", "إيجار", "تسديد فاتورة")
private val QUICK_PI_TYPES = listOf("CNI", "Passeport", "Permis")

/**
 * Zero-typing-first quick-fill panel for Algerian postal checks and SFP 01 forms.
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
    onPresetAmountClick: (Int) -> Unit = {},
    onIncrementAmountClick: (Int) -> Unit = {},
    onClearAmountClick: () -> Unit = {},
    onOpenReadyAmountsClick: () -> Unit = {},
    onQuickPlaceClick: (String) -> Unit = {},
    onTodayDateClick: () -> Unit = {},
    onAutoCalcKeyClick: () -> Unit = {},
    onExtractRipInput: (String) -> Unit = {},
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
    val currentCcp = if (isCheque) uiState.chequeCcp else uiState.sfpCcp
    val currentPlace = if (isCheque) uiState.chequePlace else uiState.sfpPlace
    val wordsLanguage = if (isCheque) uiState.chequeWordsLanguage else uiState.sfpWordsLanguage

    var showRipPasteDialog by remember { mutableStateOf(false) }
    var ripDialogText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_fill_panel"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Quick-Action Bar (Zero-Typing First)
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
                    val label = if (wordsLanguage == WordsLanguage.ARABIC) "عربي ⇄ FR (عربي)" else "عربي ⇄ FR (FR)"
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

        // 2. Preset Amounts & Quick Increments
        QuickAmountSection(
            currentAmountRaw = currentAmountStr,
            onPresetAmountClick = onPresetAmountClick,
            onIncrementAmountClick = onIncrementAmountClick,
            onClearAmountClick = onClearAmountClick,
            onOpenReadyAmountsClick = onOpenReadyAmountsClick
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

        // 4. Saved Beneficiaries / Profiles Quick Chips
        SavedProfilesQuickRow(
            profiles = uiState.savedProfiles,
            isCheque = isCheque,
            sfpOperation = uiState.sfpOperation,
            onSelectProfileDirect = onSelectProfileDirect,
            onOpenProfilePicker = onOpenProfilePicker
        )

        // 5. SFP 01 Operation & Motif Quick Chips
        if (!isCheque) {
            SfpQuickOptionsSection(
                uiState = uiState,
                onSfpOperationChange = onSfpOperationChange,
                onSfpReleveOperationsChange = onSfpReleveOperationsChange,
                onSfpAvecSoldeChange = onSfpAvecSoldeChange,
                onSfpNotificationAvoirChange = onSfpNotificationAvoirChange,
                onSfpCommandeCarnetChange = onSfpCommandeCarnetChange,
                onSfpMotifChange = onSfpMotifChange
            )
        }

        // 6. Quick Place, Today Date, Clé Auto-Calc & RIP Paste
        QuickPlaceAndToolsSection(
            lastSavedPlace = uiState.lastSavedPlace,
            currentPlace = currentPlace,
            currentCcp = currentCcp,
            onQuickPlaceClick = onQuickPlaceClick,
            onTodayDateClick = onTodayDateClick,
            onAutoCalcKeyClick = onAutoCalcKeyClick,
            onPasteRipClick = {
                val clipText = readClipboardText(context)
                if (!clipText.isNullOrBlank() && PostalCcpUtils.extractFromInput(clipText) != null) {
                    onExtractRipInput(clipText)
                    Toast.makeText(context, "تم استخراج بيانات الحساب من الحافظة!", Toast.LENGTH_SHORT).show()
                } else {
                    ripDialogText = clipText.orEmpty()
                    showRipPasteDialog = true
                }
            }
        )

        // 7. Manual Edit Mode (hidden by default for zero-typing UX)
        AnimatedVisibility(
            visible = uiState.isManualEditMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ManualEditSection(
                uiState = uiState,
                onAutoCalcKeyClick = onAutoCalcKeyClick,
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

    if (showRipPasteDialog) {
        AlertDialog(
            onDismissRequest = { showRipPasteDialog = false },
            title = { Text("استخراج CCP والمفتاح من RIP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الصق رقم RIP (20 رقماً) أو صيغة CCP/Clé مثل 42339121/98:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = ripDialogText,
                        onValueChange = { ripDialogText = it },
                        singleLine = true,
                        label = { Text("RIP أو CCP/Clé") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExtractRipInput(ripDialogText)
                        showRipPasteDialog = false
                    }
                ) {
                    Text("استخراج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRipPasteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAmountSection(
    currentAmountRaw: String,
    onPresetAmountClick: (Int) -> Unit,
    onIncrementAmountClick: (Int) -> Unit,
    onClearAmountClick: () -> Unit,
    onOpenReadyAmountsClick: () -> Unit
) {
    val parsedCurrent = AmountConversionEngine.parseFlexibleAmount(currentAmountRaw)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "المبلغ السريع (دج)",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(PRESET_AMOUNTS) { amount ->
                val isSelected = parsedCurrent != null && kotlin.math.abs(parsedCurrent - amount.toDouble()) < 0.001
                val formattedLabel = String.format(java.util.Locale.US, "%,d دج", amount)
                FilterChip(
                    selected = isSelected,
                    onClick = { onPresetAmountClick(amount) },
                    modifier = Modifier.testTag("preset_amount_$amount"),
                    label = {
                        Text(
                            text = formattedLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            INCREMENT_DELTAS.forEach { delta ->
                val label = String.format(java.util.Locale.US, "+%,d", delta)
                OutlinedButton(
                    onClick = { onIncrementAmountClick(delta) },
                    modifier = Modifier.testTag("increment_amount_$delta"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            OutlinedButton(
                onClick = onOpenReadyAmountsClick,
                modifier = Modifier.testTag("btn_ready_amounts"),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.LocalAtm, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("جداول المبالغ", fontSize = 12.sp, maxLines = 1)
            }

            if (currentAmountRaw.isNotBlank()) {
                TextButton(
                    onClick = onClearAmountClick,
                    modifier = Modifier.testTag("btn_clear_amount"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مسح", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, maxLines = 1)
                }
            }
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
                    text = "#$formatted#",
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
private fun SfpQuickOptionsSection(
    uiState: DocumentSimulatorUiState,
    onSfpOperationChange: (SfpOperationType) -> Unit,
    onSfpReleveOperationsChange: (Boolean) -> Unit,
    onSfpAvecSoldeChange: (Boolean) -> Unit,
    onSfpNotificationAvoirChange: (Boolean) -> Unit,
    onSfpCommandeCarnetChange: (Boolean) -> Unit,
    onSfpMotifChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "نوع العملية والخدمات البريدية",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
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

        Text(
            text = "سبب العملية (Motif)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QUICK_SFP_MOTIFS.forEach { motif ->
                FilterChip(
                    selected = uiState.sfpMotif == motif,
                    onClick = { onSfpMotifChange(motif) },
                    label = { Text(motif, fontSize = 12.sp, maxLines = 1) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickPlaceAndToolsSection(
    lastSavedPlace: String,
    currentPlace: String,
    currentCcp: String,
    onQuickPlaceClick: (String) -> Unit,
    onTodayDateClick: () -> Unit,
    onAutoCalcKeyClick: () -> Unit,
    onPasteRipClick: () -> Unit
) {
    val places = remember(lastSavedPlace) {
        buildList {
            if (lastSavedPlace.isNotBlank()) add(lastSavedPlace)
            PostalCcpUtils.quickAlgerianPlaces.forEach { place ->
                if (!place.equals(lastSavedPlace, ignoreCase = true)) add(place)
            }
        }
    }
    val computedRip = remember(currentCcp) { PostalCcpUtils.buildFullRip(currentCcp) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "المكان والتاريخ والمفتاح",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(places) { place ->
                val isSelected = currentPlace.equals(place, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onQuickPlaceClick(place) },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    label = { Text(place, fontSize = 12.sp, maxLines = 1) }
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onTodayDateClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تاريخ اليوم", fontSize = 12.sp, maxLines = 1)
            }

            OutlinedButton(
                onClick = onAutoCalcKeyClick,
                modifier = Modifier.testTag("btn_auto_calc_key"),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حساب المفتاح", fontSize = 12.sp, maxLines = 1)
            }

            OutlinedButton(
                onClick = onPasteRipClick,
                modifier = Modifier.testTag("btn_paste_rip"),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("لصق RIP", fontSize = 12.sp, maxLines = 1)
            }
        }

        if (computedRip != null) {
            Text(
                text = "RIP: $computedRip",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualEditSection(
    uiState: DocumentSimulatorUiState,
    onAutoCalcKeyClick: () -> Unit,
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

                CcpAndKeyRow(
                    ccp = uiState.chequeCcp,
                    key = uiState.chequeKey,
                    onCcpChange = onChequeCcpChange,
                    onKeyChange = onChequeKeyChange,
                    onAutoCalcKeyClick = onAutoCalcKeyClick
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

                CcpAndKeyRow(
                    ccp = uiState.sfpCcp,
                    key = uiState.sfpKey,
                    onCcpChange = onSfpCcpChange,
                    onKeyChange = onSfpKeyChange,
                    onAutoCalcKeyClick = onAutoCalcKeyClick
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

                Text(
                    text = "بطاقة الهوية (P.I - اختياري للمراجعة)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
private fun CcpAndKeyRow(
    ccp: String,
    key: String,
    onCcpChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onAutoCalcKeyClick: () -> Unit
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
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(10.dp),
            trailingIcon = {
                IconButton(onClick = onAutoCalcKeyClick) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "حساب المفتاح",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cb.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun readClipboardText(context: Context): String? {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = cb.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0)?.coerceToText(context)?.toString()
}
