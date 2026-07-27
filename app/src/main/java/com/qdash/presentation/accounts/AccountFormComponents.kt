package com.qdash.presentation.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.qdash.domain.model.AccountType
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.components.AppInput
import com.qdash.ui.designsystem.tokens.SpacingTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray

data class AccountPreset(
    val title: String,
    val name: String,
    val type: AccountType,
    val color: String,
    val icon: String,
    val isAmana: Boolean
)

val ACCOUNT_PRESETS = listOf(
    AccountPreset(
        title = "حساب الراتب 💼",
        name = "حساب الراتب",
        type = AccountType.BANK,
        color = "#1976D2",
        icon = "account_balance",
        isAmana = false
    ),
    AccountPreset(
        title = "حصالة الادخار 🐷",
        name = "حصالة الادخار",
        type = AccountType.SAVINGS,
        color = "#F59E0B",
        icon = "savings",
        isAmana = false
    ),
    AccountPreset(
        title = "محفظة يومية 💵",
        name = "المحفظة اليومية",
        type = AccountType.CASH,
        color = "#22C55E",
        icon = "payments",
        isAmana = false
    ),
    AccountPreset(
        title = "صندوق الأمانات 🛡️",
        name = "صندوق الأمانات",
        type = AccountType.OTHER,
        color = "#6C63FF",
        icon = "security",
        isAmana = true
    )
)

@Composable
fun QuickPresetsRow(
    onPresetSelected: (AccountPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.Xs)
    ) {
        Text(
            text = "قوالب إعداد سريعة",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Sm),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(ACCOUNT_PRESETS, key = { it.name }, contentType = { "preset" }) { preset ->
                AppCard(
                    modifier = Modifier.wrapContentSize(),
                    variant = CardVariant.INTERACTIVE,
                    shape = ShapeTokens.Md,
                    onClick = { onPresetSelected(preset) }
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                runCatching { Color(android.graphics.Color.parseColor(preset.color)) }
                                    .getOrElse { Primary }
                                    .copy(alpha = 0.05f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveAccountPreviewCard(
    name: String,
    balance: Double,
    type: AccountType,
    color: String,
    icon: String,
    iconPath: String?,
    isAmana: Boolean,
    modifier: Modifier = Modifier
) {
    val themeColor = runCatching { Color(android.graphics.Color.parseColor(color)) }.getOrElse { Primary }
    val displayTitle = name.ifBlank { "اسم الحساب" }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(BorderStroke(1.dp, themeColor.copy(alpha = 0.25f)), ShapeTokens.Lg),
        variant = CardVariant.FLAT,
        shape = ShapeTokens.Lg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.08f),
                            themeColor.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type & Amana indicator tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(themeColor.copy(alpha = 0.15f), ShapeTokens.Xs)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (type) {
                                AccountType.BANK -> "حساب بنكي"
                                AccountType.CASH -> "نقد"
                                AccountType.SAVINGS -> "ادخار"
                                AccountType.BARIDIMOB -> "بريدي موب"
                                AccountType.CCP -> "حساب بريدي"
                                else -> "أخرى"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = themeColor
                        )
                    }

                    if (isAmana) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF6C63FF).copy(alpha = 0.15f), ShapeTokens.Xs)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "أمانة 🛡️",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF6C63FF)
                            )
                        }
                    }
                }

                // Selected Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(ShapeTokens.Md)
                        .background(themeColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconPath != null) {
                        AsyncImage(
                            model = iconPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val vector = ACCOUNT_ICONS.firstOrNull { it.first == icon }?.second ?: Icons.Default.AccountBalanceWallet
                        Icon(
                            imageVector = vector,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FormatterUtils.formatCurrency(balance),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun AccountHeaderEditor(
    name: String,
    onNameChange: (String) -> Unit,
    icon: String,
    iconPath: String?,
    onImageClick: () -> Unit,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // visually right side (first in row for RTL): Image Picker Card
        AppCard(
            modifier = Modifier.size(96.dp),
            variant = CardVariant.FLAT,
            shape = ShapeTokens.Lg,
            onClick = onImageClick
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (iconPath != null) {
                    AsyncImage(
                        model = iconPath,
                        contentDescription = "صورة الحساب",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val vector = ACCOUNT_ICONS.firstOrNull { it.first == icon }?.second ?: Icons.Default.AccountBalanceWallet
                    Icon(
                        imageVector = vector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                // Camera Overlay Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "تغيير الصورة",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // visually left side (second in row for RTL): Name input and alternative icon selector option
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.Sm)
        ) {
            AppInput(
                value = name,
                onValueChange = onNameChange,
                label = "اسم الحساب",
                placeholder = "مثال: محفظتي، CCP...",
                modifier = Modifier.fillMaxWidth()
            )

            AppButton(
                onClick = onIconClick,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY,
                leadingIcon = {
                    val vector = ACCOUNT_ICONS.firstOrNull { it.first == icon }?.second ?: Icons.Default.AccountBalanceWallet
                    Icon(
                        imageVector = vector,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = ShapeTokens.Sm,
                modifier = Modifier.wrapContentSize()
            ) {
                Text("اختيار أيقونة بديلة", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun AccountActionButtons(
    onArchive: () -> Unit,
    onEmpty: () -> Unit,
    isArchived: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Md)
    ) {
        AppButton(
            onClick = onArchive,
            variant = ButtonVariant.BORDERED,
            intent = ButtonIntent.PRIMARY,
            leadingIcon = {
                Icon(
                    imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (isArchived) "إلغاء الأرشفة" else "أرشفة الحساب",
                style = MaterialTheme.typography.labelMedium
            )
        }

        AppButton(
            onClick = onEmpty,
            variant = ButtonVariant.BORDERED,
            intent = ButtonIntent.WARNING,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "تفريغ الحساب",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun AmanaToggleSetting(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ToggleRow(
        title = "تفعيل الأمانة",
        subtitle = "تخصيص هذا الحساب لإدارة الأمانات والودائع",
        checked = isChecked,
        onCheckedChange = onCheckedChange,
        icon = Icons.Default.Security
    )
}
