package com.qdash.presentation.simulator.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.PostalAmountSuggester
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.presentation.simulator.DocumentSimulatorUiState
import com.qdash.presentation.simulator.SfpOperationType
import com.qdash.ui.designsystem.tokens.ColorTokens

internal enum class SfpDropdownPanelType {
    NONE,
    OPERATION_AND_SERVICES,
    MOTIF
}

internal val QUICK_SFP_MOTIFS = listOf(
    "مصاريف شخصية",
    "أمانة / عائلة",
    "إيجار",
    "تسديد فاتورة",
    "راتب / منحة",
    "شراء سلعة"
)

/**
 * Centered floating popup box preserving the Smart 3x3 Dinar Amount picker design
 * without pushing or crowding portrait screen controls underneath.
 */
@Composable
fun SmartAmountPopupDialog(
    currentAmountRaw: String,
    onAmountChange: (String) -> Unit,
    onClearAmountClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val parsedAmount = AmountConversionEngine.parseFlexibleAmount(currentAmountRaw)
    val gridAmounts = remember(currentAmountRaw) {
        PostalAmountSuggester.suggestGridAmounts(currentAmountRaw)
    }

    SimulatorModalDialogFrame(
        title = "المبلغ بالدينار (DA)",
        icon = Icons.Default.Payments,
        testTagName = "amount_dropdown_panel",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    "اقتراحات مطابقة لـ «$currentAmountRaw»:"
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
                                    onDismiss()
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

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تأكيد", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Floating popup box for Chèque Secours Operation/Services and Motif choices.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SfpUnifiedOptionPopupDialog(
    panelType: SfpDropdownPanelType,
    uiState: DocumentSimulatorUiState,
    onSfpOperationChange: (SfpOperationType) -> Unit,
    onSfpReleveOperationsChange: (Boolean) -> Unit,
    onSfpAvecSoldeChange: (Boolean) -> Unit,
    onSfpNotificationAvoirChange: (Boolean) -> Unit,
    onSfpCommandeCarnetChange: (Boolean) -> Unit,
    onSfpReleveDuChange: (String) -> Unit,
    onSfpReleveAuChange: (String) -> Unit,
    onSfpMotifChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isOp = panelType == SfpDropdownPanelType.OPERATION_AND_SERVICES
    SimulatorModalDialogFrame(
        title = if (isOp) "نوع العملية والخدمات المرفقة" else "سبب العملية (Motif)",
        icon = if (isOp) Icons.Default.Tune else Icons.Default.Description,
        testTagName = "sfp_unified_dropdown_panel",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isOp) {
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
            } else {
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
                                onDismiss()
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

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ وإغلاق", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Single unified account/beneficiary popup combining:
 * - "حسابي المسجل (CCP)"
 * - "لنفسي / Moi-même" (Check) or Role target switch (Chèque Secours)
 * - Saved Accounts & Beneficiaries list + direct link to Postal Profiles manager.
 */
@Composable
fun UnifiedIdentityPopupDialog(
    isCheque: Boolean,
    sfpOperation: SfpOperationType,
    profiles: List<PostalProfile>,
    chequeBeneficiary: String,
    onPullMyAccountClick: () -> Unit,
    onMoiMemeClick: () -> Unit,
    onSelectProfile: (PostalProfile, PostalProfileRole) -> Unit,
    onManageProfilesClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var selectedSfpTargetRole by remember(sfpOperation) {
        mutableStateOf(
            if (sfpOperation == SfpOperationType.RETRAIT) PostalProfileRole.SELF
            else PostalProfileRole.BENEFICIARY
        )
    }
    val sortedProfiles = remember(profiles) {
        profiles.sortedByDescending { it.isFavorite }
    }

    SimulatorModalDialogFrame(
        title = "الحساب والمستفيد (تعبئة فورية)",
        icon = Icons.Default.AccountCircle,
        testTagName = "unified_identity_popup",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Quick 1-tap actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = {
                        onPullMyAccountClick()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("unified_action_my_ccp")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حسابي المسجل (CCP)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isCheque) {
                    val isMoiMeme = chequeBeneficiary.equals("لنفسي", ignoreCase = true) ||
                        chequeBeneficiary.equals("Moi-même", ignoreCase = true)
                    Surface(
                        onClick = {
                            onMoiMemeClick()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMoiMeme) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isMoiMeme) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("unified_action_moi_meme")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "لنفسي (Moi-même)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (!isCheque) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعبئة الحساب كـ:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = selectedSfpTargetRole == PostalProfileRole.SENDER ||
                            selectedSfpTargetRole == PostalProfileRole.SELF,
                        onClick = { selectedSfpTargetRole = PostalProfileRole.SENDER },
                        label = { Text("مرسل / حسابي", fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = selectedSfpTargetRole == PostalProfileRole.BENEFICIARY,
                        onClick = { selectedSfpTargetRole = PostalProfileRole.BENEFICIARY },
                        label = { Text("المستفيد", fontSize = 11.5.sp) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "الحسابات والمستفيدون المحفوظون",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (sortedProfiles.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "لا توجد حسابات محفوظة بعد. أضف حسابك أو مستفيديك من تبويب «الحساب البريدي».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 230.dp)
                        .testTag("unified_saved_profiles_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedProfiles, key = { it.id }) { profile ->
                        val roleBadge = when (profile.defaultRole) {
                            PostalProfileRole.SELF -> "حسابي"
                            PostalProfileRole.SENDER -> "مرسل"
                            PostalProfileRole.BENEFICIARY -> "مستفيد"
                        }
                        val accent = when (profile.defaultRole) {
                            PostalProfileRole.SELF -> if (isDark) ColorTokens.InfoDark else ColorTokens.PosteBlue
                            PostalProfileRole.SENDER -> if (isDark) ColorTokens.SuccessDark else ColorTokens.Success
                            PostalProfileRole.BENEFICIARY -> if (isDark) ColorTokens.PosteYellow else Color(0xFFD97706)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val applyRole = if (isCheque) {
                                        if (profile.defaultRole == PostalProfileRole.SELF) {
                                            PostalProfileRole.SELF
                                        } else {
                                            PostalProfileRole.BENEFICIARY
                                        }
                                    } else {
                                        if (profile.defaultRole == PostalProfileRole.BENEFICIARY) {
                                            PostalProfileRole.BENEFICIARY
                                        } else {
                                            selectedSfpTargetRole
                                        }
                                    }
                                    onSelectProfile(profile, applyRole)
                                    onDismiss()
                                }
                                .testTag("unified_profile_item_${profile.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.08f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (profile.isFavorite) Icons.Default.Star else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = profile.profileName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            color = accent.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = roleBadge,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = accent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "\u2066CCP: ${profile.accountNumber} / ${profile.accountKey}\u2069",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onManageProfilesClick()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إدارة أو إضافة حساب بريدي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SimulatorModalDialogFrame(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTagName: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .imePadding()
                .testTag(testTagName),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
            shadowElevation = 16.dp,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                content()
            }
        }
    }
}
