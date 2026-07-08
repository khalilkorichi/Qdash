package com.qdash.presentation.accounts

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.core.utils.FileUtils
import com.qdash.domain.model.AccountType
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppInput
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.TransferBlue
import com.qdash.ui.theme.SavingsAmber
import com.qdash.domain.model.Account
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// Map of icon key → ImageVector for account icon picker
private val ACCOUNT_ICONS: List<Pair<String, ImageVector>> = listOf(
    "account_balance_wallet" to Icons.Default.AccountBalanceWallet,
    "account_balance" to Icons.Default.AccountBalance,
    "credit_card" to Icons.Default.CreditCard,
    "payments" to Icons.Default.Payments,
    "savings" to Icons.Default.Savings,
    "money" to Icons.Default.Money,
    "attach_money" to Icons.Default.AttachMoney,
    "currency_exchange" to Icons.Default.CurrencyExchange,
    "home" to Icons.Default.Home,
    "business" to Icons.Default.Business,
    "work" to Icons.Default.Work,
    "local_atm" to Icons.Default.LocalAtm,
    "store" to Icons.Default.Store,
    "shopping_bag" to Icons.Default.ShoppingBag,
    "star" to Icons.Default.Star,
    "favorite" to Icons.Default.Favorite
)

private val ACCOUNT_COLORS = listOf(
    "#1976D2", "#6C63FF", "#22C55E", "#EF4444",
    "#F59E0B", "#06B6D4", "#8B5CF6", "#EC4899",
    "#10B981", "#F97316", "#6366F1", "#14B8A6"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    viewModel: AddEditAccountViewModel,
    accountId: Long?,
    onBack: () -> Unit,
    onNavigateToAccountDetails: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEmptyDialog by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(5) }

    LaunchedEffect(showEmptyDialog) {
        if (showEmptyDialog) {
            countdownSeconds = 5
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds--
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val isEditing = accountId != null
    val title = if (isEditing) "تعديل الحساب" else "إضافة حساب جديد"

    // Load account when editing
    LaunchedEffect(accountId) {
        if (accountId != null) {
            viewModel.loadAccount(accountId)
        }
    }

    // Navigate back on successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    // Show errors as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, "حسناً", duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    // Gallery picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = FileUtils.copyUriToInternalStorage(context, uri)
                viewModel.onIconPathChange(path)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            UnifiedScreenHeader(
                title = title,
                subtitle = if (isEditing) "قم بتعديل بيانات حسابك" else "أنشئ حساباً مالياً جديداً",
                showBackButton = true,
                onBackClick = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // --- Account name ---
                AppInput(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = "اسم الحساب",
                    placeholder = "مثال: محفظتي، CCP، راتب...",
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Initial balance (only on creation) ---
                if (!isEditing) {
                    var balanceText by remember { mutableStateOf("") }
                    AppInput(
                        value = balanceText,
                        onValueChange = { text ->
                            balanceText = text
                            val parsed = text.toDoubleOrNull() ?: 0.0
                            viewModel.onBalanceChange(parsed)
                        },
                        label = "الرصيد الأولي (اختياري)",
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // --- Account type ---
                SectionLabel("نوع الحساب")
                AccountTypeSelector(
                    selectedType = uiState.type,
                    onTypeSelected = viewModel::onTypeChange
                )

                // --- Color picker ---
                SectionLabel("اللون")
                ColorPicker(
                    selectedColor = uiState.color,
                    onColorSelected = viewModel::onColorChange
                )

                // --- Icon picker ---
                SectionLabel("الأيقونة")
                IconPicker(
                    selectedIcon = uiState.icon,
                    selectedIconPath = uiState.iconPath,
                    onIconSelected = viewModel::onIconChange,
                    onPickFromGallery = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )

                // --- Toggles ---
                SectionLabel("الإعدادات")
                SettingsCard {
                    ToggleRow(
                        title = "الحساب الافتراضي",
                        subtitle = "سيُستخدم كحساب افتراضي عند إنشاء المعاملات",
                        checked = uiState.isDefault,
                        onCheckedChange = viewModel::onIsDefaultChange,
                        icon = Icons.Default.Star
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ToggleRow(
                        title = "الحساب مفعّل",
                        subtitle = "الحسابات المعطّلة لا تُحتسب في صافي الثروة الشخصية",
                        checked = uiState.isActive,
                        onCheckedChange = viewModel::onIsActiveChange,
                        icon = Icons.Default.ToggleOn
                    )
                }

                if (isEditing) {
                    SectionLabel("إدارة الحساب")
                    ActionsCard {
                        ActionRow(
                            title = "تفاصيل الحساب",
                            subtitle = "عرض المعاملات والأمانات المرتبطة بهذا الحساب",
                            icon = Icons.Default.Info,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { accountId?.let { onNavigateToAccountDetails(it) } }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        ActionRow(
                            title = if (uiState.isArchived) "إلغاء الأرشفة" else "أرشفة الحساب",
                            subtitle = if (uiState.isArchived) "إعادة تنشيط الحساب وعرضه في القوائم" else "إخفاء الحساب ونقله للأرشيف دون حذفه",
                            icon = if (uiState.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            color = TransferBlue,
                            onClick = {
                                if (uiState.isArchived) viewModel.unarchiveAccount() else viewModel.archiveAccount()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        ActionRow(
                            title = "تفريغ رصيد الحساب",
                            subtitle = "تصفير الرصيد المالي للحساب وتعيينه إلى 0 دج",
                            icon = Icons.Default.RestartAlt,
                            color = ExpenseRed,
                            onClick = { showEmptyDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        ActionRow(
                            title = "حذف الحساب نهائياً",
                            subtitle = "إزالة الحساب بالكامل من التطبيق",
                            icon = Icons.Default.Delete,
                            color = ExpenseRed,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- Save button ---
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                AppButton(
                    onClick = { viewModel.saveAccount() },
                    isLoading = uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isEditing) "حفظ التغييرات" else "إنشاء الحساب")
                }
            }
        }
    }

    // Dialogs
    if (showDeleteDialog) {
        AppDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "حذف الحساب",
            text = "هل أنت متأكد من حذف الحساب \"${uiState.name}\"؟ سيتم حذف الحساب نهائياً إذا لم تكن هناك معاملات مرتبطة به.",
            confirmButtonText = "نعم، احذف",
            onConfirm = {
                viewModel.deleteAccount()
                showDeleteDialog = false
            },
            dismissButtonText = "إلغاء",
            isDestructive = true,
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ColorTokens.Danger, modifier = Modifier.size(20.dp))
            }
        )
    }

    if (showEmptyDialog) {
        val tempAccount = Account(
            id = uiState.accountId ?: 0L,
            name = uiState.name,
            type = uiState.type,
            balance = uiState.balance,
            color = uiState.color,
            icon = uiState.icon,
            iconPath = uiState.iconPath,
            isDefault = uiState.isDefault,
            isActive = uiState.isActive,
            isArchived = uiState.isArchived
        )
        EmptyAccountConfirmDialog(
            acc = tempAccount,
            countdownSeconds = countdownSeconds,
            onDismiss = { showEmptyDialog = false },
            onConfirm = {
                viewModel.emptyAccount()
                showEmptyDialog = false
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun ToggleRow(
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
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextGray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun AccountTypeSelector(
    selectedType: AccountType,
    onTypeSelected: (AccountType) -> Unit
) {
    val types = listOf(
        AccountType.CASH to "كاش",
        AccountType.BANK to "بنكي",
        AccountType.CCP to "CCP",
        AccountType.BARIDIMOB to "بريدي موب",
        AccountType.SAVINGS to "توفير",
        AccountType.WALLET to "محفظة",
        AccountType.OTHER to "أخرى"
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(types) { (type, label) ->
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

@Composable
private fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(ACCOUNT_COLORS) { hex ->
            val color = remember(hex) { runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Gray } }
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

@Composable
private fun IconPicker(
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
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (selectedIconPath != null) "تغيير الصورة" else "اختر من المعرض",
                    color = Primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Icon grid
        Text(
            text = "أو اختر أيقونة",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.heightIn(max = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ACCOUNT_ICONS) { (key, vector) ->
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
                        tint = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun ActionRow(
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
