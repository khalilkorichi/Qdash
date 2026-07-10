package com.qdash.presentation.accounts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.core.utils.FileUtils
import com.qdash.domain.model.Account
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.AppInput
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TransferBlue
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


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
    var showIconPicker by remember { mutableStateOf(false) }

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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.Lg),
                contentPadding = PaddingValues(top = SpacingTokens.Lg, bottom = SpacingTokens.Lg)
            ) {
                if (isEditing) {
                    item {
                        LiveAccountPreviewCard(
                            name = uiState.name,
                            balance = uiState.balance,
                            type = uiState.type,
                            color = uiState.color,
                            icon = uiState.icon,
                            iconPath = uiState.iconPath,
                            isAmana = uiState.isAmanaEnabled,
                            modifier = Modifier.padding(bottom = SpacingTokens.Xs)
                        )
                    }

                    item {
                        AccountHeaderEditor(
                            name = uiState.name,
                            onNameChange = viewModel::onNameChange,
                            icon = uiState.icon,
                            iconPath = uiState.iconPath,
                            onImageClick = {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onIconClick = { showIconPicker = !showIconPicker }
                        )
                    }



                    item {
                        AccountActionButtons(
                            onArchive = {
                                if (uiState.isArchived) viewModel.unarchiveAccount() else viewModel.archiveAccount()
                            },
                            onEmpty = { showEmptyDialog = true },
                            isArchived = uiState.isArchived
                        )
                    }

                    item {
                        SectionLabel("الإعدادات")
                        Spacer(modifier = Modifier.height(SpacingTokens.Xs))
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            AmanaToggleSetting(
                                isChecked = uiState.isAmanaEnabled,
                                onCheckedChange = viewModel::onIsAmanaEnabledChange
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(SpacingTokens.Lg))
                        AppButton(
                            onClick = { showDeleteDialog = true },
                            variant = ButtonVariant.BORDERED,
                            intent = ButtonIntent.DANGER,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حذف الحساب نهائياً")
                        }
                    }
                } else {
                    item {
                        QuickPresetsRow(
                            onPresetSelected = { preset ->
                                viewModel.onPresetSelect(
                                    name = preset.name,
                                    type = preset.type,
                                    color = preset.color,
                                    icon = preset.icon,
                                    isAmana = preset.isAmana
                                )
                            },
                            modifier = Modifier.padding(bottom = SpacingTokens.Xs)
                        )
                    }

                    item {
                        LiveAccountPreviewCard(
                            name = uiState.name,
                            balance = uiState.balance,
                            type = uiState.type,
                            color = uiState.color,
                            icon = uiState.icon,
                            iconPath = uiState.iconPath,
                            isAmana = uiState.isAmanaEnabled,
                            modifier = Modifier.padding(bottom = SpacingTokens.Xs)
                        )
                    }

                    item {
                        AccountHeaderEditor(
                            name = uiState.name,
                            onNameChange = viewModel::onNameChange,
                            icon = uiState.icon,
                            iconPath = uiState.iconPath,
                            onImageClick = {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onIconClick = { showIconPicker = !showIconPicker }
                        )
                    }



                    item {
                        // --- Initial balance (only on creation) ---
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

                    item {
                        // --- Account type ---
                        SectionLabel("نوع الحساب")
                        Spacer(modifier = Modifier.height(SpacingTokens.Xs))
                        AccountTypeSelector(
                            selectedType = uiState.type,
                            onTypeSelected = viewModel::onTypeChange
                        )
                    }

                    item {
                        // --- Color picker ---
                        SectionLabel("اللون")
                        Spacer(modifier = Modifier.height(SpacingTokens.Xs))
                        ColorPicker(
                            selectedColor = uiState.color,
                            onColorSelected = viewModel::onColorChange
                        )
                    }

                    item {
                        // --- Toggles ---
                        SectionLabel("الإعدادات")
                        Spacer(modifier = Modifier.height(SpacingTokens.Xs))
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            AmanaToggleSetting(
                                isChecked = uiState.isAmanaEnabled,
                                onCheckedChange = viewModel::onIsAmanaEnabledChange
                            )
                        }
                    }
                }
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

    IconPickerDialog(
        show = showIconPicker,
        selectedIcon = uiState.icon,
        selectedIconPath = uiState.iconPath,
        onIconSelected = {
            viewModel.onIconChange(it)
            showIconPicker = false
        },
        onPickFromGallery = {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onDismissRequest = { showIconPicker = false }
    )
}



