package com.qdash.presentation.simulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.tokens.ColorTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentSimulatorEntryScreen(
    viewModel: DocumentSimulatorViewModel,
    onSelectDocType: (DocumentType) -> Unit,
    onManageProfiles: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val posteBrandTextColor = if (isDark) ColorTokens.PosteYellow else ColorTokens.PosteNavyStripe
    val postePillBg = if (isDark) ColorTokens.PosteYellow.copy(alpha = 0.14f) else ColorTokens.PosteYellow.copy(alpha = 0.24f)
    val postePillBorder = if (isDark) ColorTokens.PosteYellow.copy(alpha = 0.35f) else ColorTokens.PosteYellow.copy(alpha = 0.7f)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FinTrackTopBar(
                title = "المساعد البريدي المالي",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = postePillBg,
                        border = BorderStroke(1.dp, postePillBorder),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ColorTokens.PosteYellow, CircleShape)
                            )
                            Text(
                                text = "بريد الجزائر • قداشّ",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = posteBrandTextColor
                            )
                        }
                    }

                    Text(
                        text = "محاكي المعاملات والتعليم البريدي",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "املأ صكوكك البريدية وحوالات الدفع بكل ثقة! يساعدك هذا القسم على تجنب الأخطاء الشائعة وحساب المبالغ وتحويل الأرقام إلى حروف.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EntryFeaturePill("تفقيط بالعربية والفرنسية", isDark)
                        EntryFeaturePill("حساب Clé CCP تلقائياً", isDark)
                        EntryFeaturePill("معاينة ورقية 1:1", isDark)
                    }
                }
            }

            // Quick Saved Profiles Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حسابات بريدية سريعة التعبئة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onManageProfiles() }
                                .testTag("btn_manage_profiles"),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.16f else 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "إدارة الحسابات",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (uiState.savedProfiles.isEmpty()) {
                        AppCard(
                            variant = CardVariant.SOLID,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                (if (isDark) ColorTokens.InfoDark else ColorTokens.PosteBlue).copy(alpha = 0.12f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (isDark) ColorTokens.InfoDark else ColorTokens.PosteBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "احفظ حساباتك البريدية لتعبئتها فوراً",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "احفظ رقم الحساب (CCP) والمستفيدين لتجنب تكرار كتابتها يدوياً.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                AppButton(
                                    onClick = onManageProfiles,
                                    variant = ButtonVariant.FLAT,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                ) {
                                    Text("حفظ حساب جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("entry_profiles_list")
                        ) {
                            items(uiState.savedProfiles, key = { it.id }) { profile ->
                                val isSelected = uiState.landingSelectedProfileId == profile.id
                                val selectedAccent = if (isDark) ColorTokens.PosteYellow else ColorTokens.PosteBlue
                                val cardBorderColor = if (isSelected) {
                                    selectedAccent
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                                val cardBorderWidth = if (isSelected) 2.dp else 1.dp
                                val cardBgColor = if (isSelected) {
                                    selectedAccent.copy(alpha = if (isDark) 0.12f else 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }

                                Card(
                                    modifier = Modifier
                                        .widthIn(min = 166.dp, max = 196.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.selectLandingProfileId(if (isSelected) null else profile.id)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(cardBorderWidth, cardBorderColor),
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor)
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
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = if (isSelected) selectedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (profile.isFavorite) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = ColorTokens.PosteYellow,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "محدد",
                                                        tint = selectedAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = profile.profileName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "\u2066${profile.accountNumber} (${profile.accountKey})\u2069",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Simulator Selection Options
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val activeProfile = uiState.savedProfiles.find { it.id == uiState.landingSelectedProfileId }
                    val chequeAccent = if (isDark) ColorTokens.InfoDark else ColorTokens.PosteBlue
                    val sfpAccent = if (isDark) ColorTokens.PosteYellow else Color(0xFFB45309)

                    // Cheque Card
                    PremiumSelectionCard(
                        title = "محاكي الصك البريدي (Chèque)",
                        description = "اكتب مبالغك بالحروف بدقة (التفقيط)، وحدد حسابك البريدي مع المفتاح لتوليد نموذج صك جاهز للمطابقة وتجنب الرفض.",
                        icon = Icons.Default.Description,
                        badgeText = activeProfile?.let { "تعبئة تلقائية باسم: ${it.fullName}" },
                        accentColor = chequeAccent,
                        categoryLabel = "Chèque Postal",
                        modifier = Modifier.testTag("entry_card_cheque"),
                        actionButton = {
                            AppButton(
                                onClick = { onSelectDocType(DocumentType.CHEQUE) },
                                variant = ButtonVariant.SOLID,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_start_cheque")
                            ) {
                                Text("بدء محاكاة الصك", fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    // SFP01 Card
                    PremiumSelectionCard(
                        title = "محاكي استمارة الحوالة (SFP 01)",
                        description = "تدرّب على تعبئة الاستمارة الصفراء الخاصة بعمليات الدفع (صب الأموال)، السحب، كشف العمليات، أو طلب دفتر الصكوك.",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        badgeText = activeProfile?.let { "تعبئة تلقائية باسم: ${it.fullName}" },
                        accentColor = sfpAccent,
                        categoryLabel = "Formulaire SFP 01",
                        modifier = Modifier.testTag("entry_card_sfp01"),
                        actionButton = {
                            AppButton(
                                onClick = { onSelectDocType(DocumentType.SFP01) },
                                variant = ButtonVariant.BORDERED,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_start_sfp01")
                            ) {
                                Text("بدء محاكاة الاستمارة", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryFeaturePill(
    label: String,
    isDark: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isDark) ColorTokens.TextPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PremiumSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    badgeText: String?,
    actionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    categoryLabel: String? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AppCard(
        variant = CardVariant.SOLID,
        shape = RoundedCornerShape(16.dp),
        borderStroke = BorderStroke(
            width = 1.dp,
            color = if (badgeText != null) accentColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            accentColor.copy(alpha = if (isDark) 0.16f else 0.10f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (categoryLabel != null) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = accentColor
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badgeText != null) {
                        Surface(
                            color = accentColor.copy(alpha = if (isDark) 0.16f else 0.10f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            actionButton()
        }
    }
}
