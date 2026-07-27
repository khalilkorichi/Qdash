package com.qdash.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.qdash.domain.model.AccountType
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

/** Map of icon key → ImageVector for the account icon picker grid. */
internal val ACCOUNT_ICONS: List<Pair<String, ImageVector>> = listOf(
    "account_balance_wallet" to Icons.Default.AccountBalanceWallet,
    "account_balance"        to Icons.Default.AccountBalance,
    "credit_card"            to Icons.Default.CreditCard,
    "payments"               to Icons.Default.Payments,
    "savings"                to Icons.Default.Savings,
    "money"                  to Icons.Default.Money,
    "attach_money"           to Icons.Default.AttachMoney,
    "currency_exchange"      to Icons.Default.CurrencyExchange,
    "home"                   to Icons.Default.Home,
    "business"               to Icons.Default.Business,
    "work"                   to Icons.Default.Work,
    "local_atm"              to Icons.Default.LocalAtm,
    "store"                  to Icons.Default.Store,
    "shopping_bag"           to Icons.Default.ShoppingBag,
    "star"                   to Icons.Default.Star,
    "favorite"               to Icons.Default.Favorite,
    "card_giftcard"          to Icons.Default.CardGiftcard,
    "trending_up"            to Icons.Default.TrendingUp,
    "flight"                 to Icons.Default.Flight,
    "directions_car"         to Icons.Default.DirectionsCar,
    "restaurant"             to Icons.Default.Restaurant,
    "school"                 to Icons.Default.School,
    "local_hospital"         to Icons.Default.LocalHospital,
    "computer"               to Icons.Default.Computer,
    "spa"                    to Icons.Default.Spa,
    "games"                  to Icons.Default.Games,
    "receipt"                to Icons.Default.Receipt,
    "lock"                   to Icons.Default.Lock,
    "redeem"                 to Icons.Default.Redeem,
    "shield"                 to Icons.Default.Shield
)

internal val ACCOUNT_COLORS = listOf(
    "#1976D2", "#6C63FF", "#22C55E", "#EF4444",
    "#F59E0B", "#06B6D4", "#8B5CF6", "#EC4899",
    "#10B981", "#F97316", "#6366F1", "#14B8A6"
)

// ---------------------------------------------------------------------------
// Small utility composables
// ---------------------------------------------------------------------------

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
internal fun ActionsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

// ---------------------------------------------------------------------------
// Toggle row (used inside SettingsCard)
// ---------------------------------------------------------------------------

@Composable
internal fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) Primary else TextGray,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextGray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = Primary.copy(alpha = 0.3f)
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Action row (used inside ActionsCard)
// ---------------------------------------------------------------------------

@Composable
internal fun ActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (color == ExpenseRed) ExpenseRed else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = TextGray.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Account type chip selector
// ---------------------------------------------------------------------------

@Composable
internal fun AccountTypeSelector(
    selectedType: AccountType,
    onTypeSelected: (AccountType) -> Unit
) {
    val types = listOf(
        AccountType.CASH      to "كاش",
        AccountType.BANK      to "بنكي",
        AccountType.CCP       to "CCP",
        AccountType.BARIDIMOB to "بريدي موب",
        AccountType.SAVINGS   to "توفير",
        AccountType.WALLET    to "محفظة",
        AccountType.OTHER     to "أخرى"
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(types, key = { (type, _) -> type.name }, contentType = { "account_type" }) { (type, label) ->
            val isSelected = type == selectedType
            FilterChip(
                selected = isSelected,
                onClick = { onTypeSelected(type) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary.copy(alpha = 0.15f),
                    selectedLabelColor = Primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = Primary,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Color swatch picker row
// ---------------------------------------------------------------------------

@Composable
internal fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(ACCOUNT_COLORS, key = { it }, contentType = { "color" }) { hex ->
            val color = remember(hex) {
                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Gray }
            }
            val isSelected = hex == selectedColor
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                        else Modifier
                    )
                    .clickable { onColorSelected(hex) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Icon picker (gallery + vector grid)
// ---------------------------------------------------------------------------

@Composable
internal fun IconPicker(
    selectedIcon: String,
    selectedIconPath: String?,
    onIconSelected: (String) -> Unit,
    onPickFromGallery: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Gallery image preview or pick button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedIconPath != null) {
                AsyncImage(
                    model = selectedIconPath,
                    contentDescription = "صورة الحساب",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            OutlinedButton(
                onClick = onPickFromGallery,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (selectedIconPath != null) "تغيير الصورة" else "اختر من المعرض",
                    color = Primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Vector icon grid
        Text(text = "أو اختر أيقونة", style = MaterialTheme.typography.labelSmall, color = TextGray)
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.heightIn(max = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ACCOUNT_ICONS, key = { (k, _) -> k }, contentType = { "icon" }) { (key, vector) ->
                val isSelected = key == selectedIcon && selectedIconPath == null
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) Primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onIconSelected(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = vector,
                        contentDescription = key,
                        tint = if (isSelected) Primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun IconPickerDialog(
    show: Boolean,
    selectedIcon: String,
    selectedIconPath: String?,
    onIconSelected: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    onDismissRequest: () -> Unit
) {
    if (show) {
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تعديل أيقونة الحساب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = TextGray
                            )
                        }
                    }

                    IconPicker(
                        selectedIcon = selectedIcon,
                        selectedIconPath = selectedIconPath,
                        onIconSelected = onIconSelected,
                        onPickFromGallery = onPickFromGallery
                    )
                }
            }
        }
    }
}
