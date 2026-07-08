package com.qdash.presentation.accounts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.qdash.ui.designsystem.components.AppInput
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.tokens.ColorTokens
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



