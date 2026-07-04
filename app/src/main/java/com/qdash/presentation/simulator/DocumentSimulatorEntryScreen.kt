package com.qdash.presentation.simulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.domain.model.PostalProfile
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentSimulatorEntryScreen(
    viewModel: DocumentSimulatorViewModel,
    onSelectDocType: (DocumentType) -> Unit,
    onManageProfiles: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "محاكي المعاملات والتعليم البريدي",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "املأ صكوكك البريدية وحوالات الدفع بكل ثقة! يساعدك هذا القسم على تجنب الأخطاء الشائعة وحساب المبالغ وتحويل الأرقام إلى حروف.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حسابات بريدية سريعة التعبئة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "إدارة الحسابات",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.clickable { onManageProfiles() }
                        )
                    }

                    if (uiState.savedProfiles.isEmpty()) {
                        AppCard(
                            variant = CardVariant.FLAT,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
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
                                AppButton(
                                    onClick = onManageProfiles,
                                    variant = ButtonVariant.LIGHT,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("حفظ حساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.savedProfiles) { profile ->
                                val isSelected = uiState.landingSelectedProfileId == profile.id
                                val cardBorderColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                }
                                val cardBgColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }

                                Card(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.selectLandingProfileId(if (isSelected) null else profile.id)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, cardBorderColor),
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
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "محدد",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
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
                                            text = "${profile.accountNumber} (${profile.accountKey})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
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

                    // Cheque Card
                    PremiumSelectionCard(
                        title = "محاكي الصك البريدي (Chèque)",
                        description = "اكتب مبالغك بالحروف بدقة (التفقيط)، وحدد حسابك البريدي مع المفتاح لتوليد نموذج صك جاهز للمطابقة وتجنب الرفض.",
                        icon = Icons.Default.Description,
                        badgeText = activeProfile?.let { "تعبئة تلقائية باسم: ${it.fullName}" },
                        actionButton = {
                            AppButton(
                                onClick = { onSelectDocType(DocumentType.CHEQUE) },
                                variant = ButtonVariant.SOLID,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("بدء محاكاة الصك", fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    // SFP01 Card
                    PremiumSelectionCard(
                        title = "محاكي استمارة الحوالة (SFP 01)",
                        description = "تدرّب على تعبئة الاستمارة الصفراء الخاصة بعمليات الدفع (صب الأموال)، السحب، أو التحويل بين الحسابات البريدية الجارية.",
                        icon = Icons.Default.ReceiptLong,
                        badgeText = activeProfile?.let { "تعبئة تلقائية باسم: ${it.fullName}" },
                        actionButton = {
                            AppButton(
                                onClick = { onSelectDocType(DocumentType.SFP01) },
                                variant = ButtonVariant.BORDERED,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
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
fun PremiumSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    badgeText: String?,
    actionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        variant = CardVariant.SOLID,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
