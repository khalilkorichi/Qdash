package com.qdash.presentation.onboarding.components

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.AccountType
import com.qdash.presentation.onboarding.CustomWalletDraft
import com.qdash.presentation.onboarding.OnboardingUiState
import com.qdash.presentation.onboarding.WalletOption
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray

@Composable
fun LanguageSetupScreen(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Premium Icon Background Glow
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Primary.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "لغة التطبيق / App Language",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "يرجى اختيار لغة التطبيق المفضلة لديك للمتابعة.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Selectable Arabic Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(2.dp, Primary, RoundedCornerShape(16.dp))
                .clickable { onLanguageSelected("ar") }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🇩🇿",
                        fontSize = 28.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "العربية (Arabic)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "واجهة مستخدم محسنة وتدعم RTL بالكامل",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Future Languages Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "سنقوم بإضافة لغات أخرى قريباً (الفرنسية، الإنجليزية) لتخصيص أفضل لتجربتك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text(
                text = "التالي",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
fun NotificationPermissionScreen(
    onEnable: () -> Unit,
    onLater: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Bell Icon
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Primary.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "تفعيل التنبيهات الذكية",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "احصل على إشعارات مخصصة لمساعدتك على إدارة أموالك بكفاءة وبدون نسيان.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Features Grid / list of why notifications are useful
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NotificationFeatureItem(
                imageVector = Icons.Default.Wallet,
                title = "تذكير بالراتب الشهري",
                description = "تنبيه لطيف مطلع كل شهر لتسجيل راتبك وجدولة ميزانيتك."
            )
            NotificationFeatureItem(
                imageVector = Icons.Default.TrendingDown,
                title = "تنبيهات تجاوز الميزانية",
                description = "إشعار فوري عند اقترابك من تجاوز الحد المحدد للفئات والإنفاق."
            )
            NotificationFeatureItem(
                imageVector = Icons.Default.Update,
                title = "تذكير بالاشتراكات الدورية",
                description = "تذكير قبل اقتطاع فواتير منصات الترفيه أو باقات الإنترنت."
            )
            NotificationFeatureItem(
                imageVector = Icons.Default.TaskAlt,
                title = "تذكير بالديون والادخار",
                description = "متابعة أهدافك الادخارية ومواعيد استرجاع أو دفع الديون."
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Bottom Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onEnable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = "تفعيل التنبيهات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            TextButton(
                onClick = onLater,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "لاحقاً، ليس الآن",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextGray
                )
            }
        }
    }
}

@Composable
fun NotificationFeatureItem(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                lineHeight = 16.sp
            )
        }
    }
}

// ─── Wallet Setup Screen (Hybrid Onboarding) ───────────────────────────────

@Composable
fun InitialWalletSetupScreen(
    uiState: OnboardingUiState,
    onWalletToggled: (AccountType) -> Unit,
    onBalanceChanged: (AccountType, String) -> Unit,
    onShowAddCustomWallet: () -> Unit,
    onCustomWalletNameChanged: (String) -> Unit,
    onCustomWalletBalanceChanged: (String) -> Unit,
    onCustomWalletColorChanged: (String) -> Unit,
    onConfirmCustomWallet: () -> Unit,
    onDismissCustomWallet: () -> Unit,
    onRemoveCustomWallet: (Int) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val customColors = listOf("#6C63FF", "#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#EC4899", "#8B5CF6", "#06B6D4")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.radialGradient(colors = listOf(Primary.copy(alpha = 0.18f), Color.Transparent)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AccountBalanceWallet, null, tint = Primary, modifier = Modifier.size(42.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "لنجهّز محافظك",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "اختر ما تستخدمه فعلاً — يمكنك تعديل هذا لاحقاً",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Predefined wallet selectable cards
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.walletOptions.forEach { option ->
                SelectableWalletCard(
                    option = option,
                    onToggle = { onWalletToggled(option.type) },
                    onBalanceChanged = { onBalanceChanged(option.type, it) },
                    balanceError = if (option.isSelected) uiState.balanceError else null
                )
            }

            // Custom wallets already added
            uiState.customWallets.forEachIndexed { index, draft ->
                AddedCustomWalletCard(
                    draft = draft,
                    onRemove = { onRemoveCustomWallet(index) }
                )
            }

            // Inline custom wallet form
            AnimatedVisibility(
                visible = uiState.showAddCustomWallet,
                enter = expandVertically(animationSpec = tween(280)),
                exit = shrinkVertically(animationSpec = tween(200))
            ) {
                CustomWalletForm(
                    draft = uiState.customWalletDraft,
                    colorOptions = customColors,
                    balanceError = uiState.balanceError,
                    onNameChanged = onCustomWalletNameChanged,
                    onBalanceChanged = onCustomWalletBalanceChanged,
                    onColorChanged = onCustomWalletColorChanged,
                    onConfirm = onConfirmCustomWallet,
                    onDismiss = onDismissCustomWallet
                )
            }

            // Add custom wallet button (hidden while form is open)
            if (!uiState.showAddCustomWallet) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .clickable(onClick = onShowAddCustomWallet)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null, tint = Primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "إضافة محفظة مخصصة",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // CTA buttons
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (uiState.walletOptions.any { it.isSelected } || uiState.customWallets.isNotEmpty())
                            "ابدأ" else "تخطي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "تخطي — سأضيف محافظي لاحقاً",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextGray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─── Selectable Wallet Card with Expand/Collapse balance field ──────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableWalletCard(
    option: WalletOption,
    onToggle: () -> Unit,
    onBalanceChanged: (String) -> Unit,
    balanceError: String?
) {
    val accentColor = remember(option.color) {
        try { Color(android.graphics.Color.parseColor(option.color)) } catch (e: Exception) { Primary }
    }

    val walletIcon = when (option.type) {
        AccountType.BARIDIMOB -> Icons.Default.PhonelinkRing
        AccountType.CASH -> Icons.Default.Payments
        AccountType.SAVINGS -> Icons.Default.Savings
        else -> Icons.Default.AccountBalanceWallet
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (option.isSelected) 2.dp else 1.dp,
                color = if (option.isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (option.isSelected)
                accentColor.copy(alpha = 0.06f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row — tap to toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(walletIcon, null, tint = accentColor, modifier = Modifier.size(22.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (option.isSelected) "محددة — اضغط لإلغاء التحديد" else "اضغط لتحديدها",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (option.isSelected) accentColor else TextGray
                    )
                }

                // Checkbox visual
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (option.isSelected) accentColor else Color.Transparent,
                            CircleShape
                        )
                        .border(
                            2.dp,
                            if (option.isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (option.isSelected) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Expand balance field when selected
            AnimatedVisibility(
                visible = option.isSelected,
                enter = expandVertically(animationSpec = tween(260)),
                exit = shrinkVertically(animationSpec = tween(200))
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "الرصيد الافتتاحي (اختياري)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = option.balance,
                        onValueChange = onBalanceChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0 دج", color = TextGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
                        isError = balanceError != null,
                        supportingText = if (balanceError != null) {
                            { Text(balanceError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        }
    }
}

// ─── Added custom wallet chip ───────────────────────────────────────────────

@Composable
private fun AddedCustomWalletCard(draft: CustomWalletDraft, onRemove: () -> Unit) {
    val accentColor = remember(draft.color) {
        try { Color(android.graphics.Color.parseColor(draft.color)) } catch (e: Exception) { Primary }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(accentColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AccountBalanceWallet, null, tint = accentColor, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(draft.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            val bal = draft.balance.toDoubleOrNull()
            if (bal != null && bal > 0) {
                Text("${bal.toInt()} دج", style = MaterialTheme.typography.labelSmall, color = TextGray)
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = TextGray, modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Inline custom wallet form ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomWalletForm(
    draft: CustomWalletDraft,
    colorOptions: List<String>,
    balanceError: String?,
    onNameChanged: (String) -> Unit,
    onBalanceChanged: (String) -> Unit,
    onColorChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "محفظة مخصصة جديدة",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Primary
            )

            TextField(
                value = draft.name,
                onValueChange = onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("اسم المحفظة (مثال: CCP، بنك BNA)", color = TextGray) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            TextField(
                value = draft.balance,
                onValueChange = onBalanceChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("الرصيد الافتتاحي (اختياري)", color = TextGray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
                isError = balanceError != null,
                supportingText = if (balanceError != null) {
                    { Text(balanceError, color = MaterialTheme.colorScheme.error) }
                } else null,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            // Color picker
            Text("اللون:", style = MaterialTheme.typography.labelSmall, color = TextGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colorOptions.forEach { hex ->
                    val c = remember(hex) {
                        try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Primary }
                    }
                    val isSelected = draft.color.uppercase() == hex.uppercase()
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                            .clickable { onColorChanged(hex) }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1.5f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = draft.name.isNotBlank()
                ) {
                    Text("إضافة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Legacy WalletSetupCard (kept for any remaining references) ─────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSetupCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    value: String,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.00 دج", color = TextGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    }
}

@Composable
fun AuthSetupScreen(
    onSignInSuccess: (com.google.android.gms.auth.api.signin.GoogleSignInAccount) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                onSignInSuccess(account)
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "فشل تسجيل الدخول: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Cloud Icon
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Primary.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "حسابك والمزامنة السحابية",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "اربط حسابك بجوجل لحفظ نسخة احتياطية مشفرة وتلقائية من بياناتك المالية على Google Drive لضمان عدم فقدانها عند تغيير جهازك.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Badge showing connection state
        var isConnected by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isConnected = GoogleSignIn.getLastSignedInAccount(context) != null
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isConnected) Color(0xFF22C55E).copy(alpha = 0.12f) else Color(0xFF6B7280).copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = if (isConnected) Color(0xFF22C55E).copy(alpha = 0.4f) else Color(0xFF6B7280).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isConnected) Color(0xFF22C55E) else Color(0xFF6B7280), CircleShape)
                )
                Text(
                    text = if (isConnected) "متصل بخدمات Google" else "غير متصل بالسحابة",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isConnected) Color(0xFF22C55E) else TextGray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    keyboardController?.hide()
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestProfile()
                        .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(client.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تسجيل الدخول باستخدام Google",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "تخطي هذه الخطوة للآن",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextGray
                )
            }
        }
    }
}
