package com.qdash.presentation.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import com.qdash.presentation.components.getIconByName
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.Category
import com.qdash.domain.model.TransactionType
import com.qdash.core.utils.FormatterUtils
import com.qdash.core.ui.StableList
import com.qdash.core.ui.asStable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.domain.model.Account
import com.qdash.domain.model.AccountType
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle


// â”€â”€â”€ Add Account Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, balance: Double, color: String, icon: String) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val accountColorPalette = remember(Primary) {
        listOf(
            "#6C63FF" to Primary,
            "#22C55E" to IncomeGreen,
            "#EF4444" to ExpenseRed,
            "#3B82F6" to TransferBlue,
            "#F59E0B" to SavingsAmber,
            "#06B6D4" to Color(0xFF06B6D4),
            "#8B5CF6" to Color(0xFF8B5CF6),
            "#EC4899" to Color(0xFFEC4899)
        )
    }
    var accName by rememberSaveable { mutableStateOf("") }
    var accType by rememberSaveable(saver = Saver<MutableState<AccountType>, String>(
        save = { it.value.name },
        restore = { mutableStateOf(AccountType.valueOf(it)) }
    )) { mutableStateOf(AccountType.BARIDIMOB) }
    var accBalance by rememberSaveable { mutableStateOf("") }
    var accColor by rememberSaveable { mutableStateOf("#6C63FF") }

    val accountTypes = listOf(
        AccountType.BARIDIMOB to "بريدي موب",
        AccountType.CCP       to "CCP",
        AccountType.CASH      to "نقدي",
        AccountType.BANK      to "بنك",
        AccountType.SAVINGS   to "توفير",
        AccountType.WALLET    to "محفظة"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "إضافة حساب مالي جديد",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppInput(
                    value = accName,
                    onValueChange = { accName = it },
                    modifier = Modifier.testTag("account_name_input"),
                    placeholder = "اسم الحساب (مثال: بريدي موب شخصي)"
                )

                AppInput(
                    value = accBalance,
                    onValueChange = { accBalance = it },
                    modifier = Modifier.testTag("account_balance_input"),
                    placeholder = "الرصيد الافتتاحي (دج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )

                Text("نوع الحساب:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)

                // Type selector in 2 rows of 3
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accountTypes.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (type, label) ->
                                val isSelected = accType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { accType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Text("اللون:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accountColorPalette.forEach { (hex, color) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (accColor == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { accColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (accColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    val bal = com.qdash.core.utils.FormatterUtils.normalizeAmount(accBalance).toDoubleOrNull() ?: 0.0
                    if (accName.isNotBlank() && bal >= 0) {
                        val icon = when (accType) {
                            AccountType.BARIDIMOB -> "phonelink_ring"
                            AccountType.CCP -> "credit_card"
                            AccountType.BANK -> "account_balance"
                            AccountType.SAVINGS -> "savings"
                            AccountType.WALLET -> "account_balance_wallet"
                            else -> "payments"
                        }
                        onConfirm(accName, accType, bal, accColor, icon)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("حفظ الحساب", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun NetWealthCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(146.dp)
            .shimmerEffect(RoundedCornerShape(24.dp))
    )
}

@Composable
fun AccountItemSkeleton(modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shimmerEffect(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(16.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(14.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountBottomSheet(
    acc: Account,
    accountColorPalette: List<Pair<String, Color>>,
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit
) {
    var editName by remember(acc.id) { mutableStateOf(acc.name) }
    var editBalance by remember(acc.id) { mutableStateOf(acc.balance.toInt().toString()) }
    var editColor by remember(acc.id) { mutableStateOf(acc.color) }

    AppBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "تعديل الحساب",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )

            AppInput(
                value = editName,
                onValueChange = { editName = it },
                modifier = Modifier.testTag("edit_account_name_input"),
                label = "اسم الحساب"
            )

            AppInput(
                value = editBalance,
                onValueChange = { editBalance = it },
                modifier = Modifier.testTag("edit_account_balance_input"),
                label = "الرصيد (دج)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
            )

            Text(
                "لون الحساب:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                accountColorPalette.forEach { (hex, color) ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (editColor == hex) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { editColor = hex }
                            .testTag("color_circle_$hex")
                    ) {
                        if (editColor == hex) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
                AppButton(
                    onClick = {
                        val bal = com.qdash.core.utils.FormatterUtils.normalizeAmount(editBalance).toDoubleOrNull() ?: acc.balance
                        if (editName.isNotBlank()) {
                            onConfirm(
                                acc.copy(
                                    name = editName,
                                    balance = bal,
                                    color = editColor
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text("حفظ التعديلات", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyAccountConfirmDialog(
    acc: Account,
    countdownSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "تفريغ رصيد الحساب",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "هل أنت متأكد من تفريغ رصيد الحساب \"${acc.name}\" بالكامل؟ سيتم تصفير الرصيد وتعيينه إلى 0 دج.",
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                if (countdownSeconds > 0) {
                    Text(
                        "يرجى الانتظار ${countdownSeconds} ثوانٍ لتأكيد العملية...",
                        color = ExpenseRed,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "يمكنك الآن تأكيد العملية.",
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (countdownSeconds == 0) {
                AppButton(
                    onClick = onConfirm,
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.DANGER
                ) {
                    Text("تأكيد تفريغ الحساب", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun TransferDialog(
    accounts: List<Account>,
    fromAccountId: Long?,
    toAccountId: Long?,
    transferAmount: String,
    onFromAccountChange: (Long) -> Unit,
    onToAccountChange: (Long) -> Unit,
    onTransferAmountChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (fromId: Long, toId: Long, amount: Double) -> Unit,
    Primary: Color
) {
    val currentFrom = fromAccountId ?: accounts.firstOrNull()?.id ?: 0L
    val currentTo = toAccountId ?: accounts.lastOrNull()?.id ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "تحويل مالي بين الحسابات",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الحساب المرسل:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { acc ->
                        val isSelected = currentFrom == acc.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onFromAccountChange(acc.id) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                acc.name,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text("الحساب المستلم:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { acc ->
                        val isSelected = currentTo == acc.id
                        val disabled = acc.id == currentFrom
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> TransferBlue
                                        disabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { if (!disabled) onToAccountChange(acc.id) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                acc.name,
                                color = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        else if (isSelected) Color.White
                                        else MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                AppInput(
                    value = transferAmount,
                    onValueChange = onTransferAmountChange,
                    modifier = Modifier.testTag("transfer_amount_input"),
                    placeholder = "مبلغ التحويل (دج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation()
                )
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    val amt = com.qdash.core.utils.FormatterUtils.normalizeAmount(transferAmount).toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(currentFrom, currentTo, amt)
                    }
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.INFO
            ) {
                Text("تنفيذ التحويل", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun NetWealthCard(
    total: Double,
    showTotalBalance: Boolean,
    onToggleTotalBalance: () -> Unit,
    activeAccountsCount: Int,
    isDark: Boolean,
    Primary: Color,
    PrimaryDark: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleTotalBalance() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        if (isDark) listOf(Primary, PrimaryDark) else listOf(Primary, Primary)
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "صافي الثروة الكلية",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onToggleTotalBalance,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (showTotalBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (showTotalBalance) FormatterUtils.formatCurrency(total) else "•••••• دج",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عدد الحسابات النشطة: $activeAccountsCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "مفعلة بالكامل",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAccountsState(
    modifier: Modifier = Modifier,
    onAddAccountClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing wallet illustration
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "لا توجد محافظ بعد",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "أنشئ محفظتك الأولى لتبدأ تتبع أموالك بذكاء",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        if (onAddAccountClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddAccountClick,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "أنشئ محفظتك الأولى",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AccountsDialogs(
    uiState: AccountsUiState,
    viewModel: AccountsViewModel,
    showEditSheet: Boolean,
    onEditSheetDismiss: () -> Unit,
    showAddAccountDialog: Boolean,
    onAddAccountDialogDismiss: () -> Unit,
    showTransferDialog: Boolean,
    onTransferDialogDismiss: () -> Unit,
    accountToDelete: Account?,
    onDeleteDialogDismiss: () -> Unit,
    onDeleteConfirm: (Account) -> Unit,
    accountToEmpty: Account?,
    onEmptyDialogDismiss: () -> Unit,
    onEmptyConfirm: (Account) -> Unit,
    countdownSeconds: Int,
    fromAccountId: Long?,
    onFromAccountChange: (Long) -> Unit,
    toAccountId: Long?,
    onToAccountChange: (Long) -> Unit,
    transferAmount: String,
    onTransferAmountChange: (String) -> Unit,
    accountColorPalette: List<Pair<String, Color>>,
    Primary: Color
) {
    if (showEditSheet && uiState.editingAccount != null) {
        EditAccountBottomSheet(
            acc = uiState.editingAccount!!,
            accountColorPalette = accountColorPalette,
            onDismiss = onEditSheetDismiss,
            onConfirm = { updatedAcc ->
                viewModel.editAccount(updatedAcc)
                onEditSheetDismiss()
            }
        )
    }

    accountToDelete?.let { acc ->
        AppDialog(
            onDismissRequest = onDeleteDialogDismiss,
            title = "حذف الحساب",
            text = "هل أنت متأكد من حذف الحساب \"${acc.name}\"؟ سيتم حذف الحساب نهائياً إذا لم تكن هناك معاملات مرتبطة به.",
            confirmButtonText = "نعم، احذف",
            onConfirm = {
                onDeleteConfirm(acc)
            },
            dismissButtonText = "إلغاء",
            isDestructive = true,
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ColorTokens.Danger, modifier = Modifier.size(20.dp))
            }
        )
    }

    accountToEmpty?.let { acc ->
        EmptyAccountConfirmDialog(
            acc = acc,
            countdownSeconds = countdownSeconds,
            onDismiss = onEmptyDialogDismiss,
            onConfirm = {
                onEmptyConfirm(acc)
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = onAddAccountDialogDismiss,
            onConfirm = { name, type, balance, color, icon ->
                viewModel.addAccount(name, type, balance, color, icon)
                onAddAccountDialogDismiss()
            }
        )
    }

    if (showTransferDialog && uiState.accounts.size >= 2) {
        TransferDialog(
            accounts = uiState.accounts,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            transferAmount = transferAmount,
            onFromAccountChange = onFromAccountChange,
            onToAccountChange = onToAccountChange,
            onTransferAmountChange = onTransferAmountChange,
            onDismiss = onTransferDialogDismiss,
            onConfirm = { fromId, toId, amt ->
                viewModel.executeTransfer(fromId, toId, amt, "تحويل داخلي")
                onTransferDialogDismiss()
            },
            Primary = Primary
        )
    }
}

@Composable
fun AccountActionsRow(
    onAddAccountClick: () -> Unit,
    onTransferClick: () -> Unit,
    isLoading: Boolean,
    accountsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppButton(
            onClick = onAddAccountClick,
            enabled = !isLoading,
            modifier = Modifier.weight(1f),
            variant = ButtonVariant.SOLID,
            intent = ButtonIntent.PRIMARY,
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        ) {
            Text("حساب جديد", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        AppButton(
            onClick = onTransferClick,
            enabled = !isLoading && accountsCount >= 2,
            modifier = Modifier.weight(1f),
            variant = ButtonVariant.LIGHT,
            intent = ButtonIntent.PRIMARY,
            leadingIcon = {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        ) {
            Text("تحويل مالي", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}






