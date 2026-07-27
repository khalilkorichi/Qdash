package com.qdash.presentation.backup

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.presentation.backup.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
    showTopBar: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backupProgress by viewModel.backupProgress.collectAsStateWithLifecycle()
    val backupFolderUri by viewModel.backupFolderUri.collectAsStateWithLifecycle()
    val backupScheduleInterval by viewModel.backupScheduleInterval.collectAsStateWithLifecycle()
    val lastBackupUriForShare by viewModel.lastBackupUri.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var showFolderBackupPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }
    var showComingSoonSheet by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.connectGoogleDriveAccount(
                    account,
                    onSuccess = { Toast.makeText(context, "تم ربط الحساب بنجاح ومزامنة البيانات!", Toast.LENGTH_SHORT).show() },
                    onFailure = { err -> Toast.makeText(context, "فشل ربط الحساب: $err", Toast.LENGTH_LONG).show() }
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "فشل تسجيل الدخول: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val launchGoogleSignIn = {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    // Tab state
    var activeTab by remember { mutableIntStateOf(0) }

    // SAF Activity Result Launchers
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            viewModel.exportBackupV2(it, pendingExportPassword, includeAttachments = true)
            pendingExportPassword = null
        }
    }

    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.saveBackupFolder(it) }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.prepareRestore(it, null) }
    }

    val createCsvTransactionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values")
    ) { uri ->
        uri?.let { viewModel.exportTransactionsToCsv(context, it) }
    }

    val createCsvCategoriesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values")
    ) { uri ->
        uri?.let { viewModel.exportCategoriesToCsv(context, it) }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportDataToJson(context, it) }
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                UnifiedScreenHeader(
                    title = "النسخ الاحتياطي والاستعادة",
                    subtitle = "حماية ونقل بياناتك المالية بأمان وسهولة",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = if (!showTopBar) 96.dp else 16.dp)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
        val textSecondaryColor = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight
        val infoBannerBg = if (isDark) primaryColor.copy(alpha = 0.12f) else Color(0xFFF8F8F6)
        val infoBannerBorder = if (isDark) primaryColor.copy(alpha = 0.0f) else ColorTokens.BorderLight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeTokens.Lg)
                        .background(infoBannerBg)
                        .border(1.dp, infoBannerBorder, ShapeTokens.Lg)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isDark) primaryColor.copy(alpha = 0.20f) else ColorTokens.BorderLight,
                                    ShapeTokens.Md
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isDark) primaryColor else ColorTokens.TextPrimaryLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حماية البيانات المالية المشفرة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "حافظ على أمان أموالك بالتصدير والاستيراد الفوري والكامل يدوياً، أو باستخدام كلمة مرور مخصصة.",
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondaryColor
                            )
                        }
                    }
                }

                BackupTabHeader(
                    activeTab = activeTab,
                    onTabSelect = { activeTab = it }
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (activeTab == 0) {
                    ManualBackupTabContent(
                        backupFolderUri = backupFolderUri,
                        backupScheduleInterval = backupScheduleInterval,
                        lastBackupUriForShare = lastBackupUriForShare,
                        onSelectFolderClick = { openFolderLauncher.launch(null) },
                        onScheduleIntervalChange = { viewModel.updateScheduleInterval(it) },
                        onImmediateBackupClick = {
                            if (!backupFolderUri.isNullOrEmpty()) {
                                showFolderBackupPasswordDialog = true
                            }
                        },
                        onImportZipClick = { openBackupLauncher.launch(arrayOf("application/zip")) },
                        onExportCsvTransactionsClick = {
                            val timestamp = System.currentTimeMillis() / 1000
                            createCsvTransactionsLauncher.launch("transactions-report-$timestamp.csv")
                        },
                        onExportCsvCategoriesClick = {
                            val timestamp = System.currentTimeMillis() / 1000
                            createCsvCategoriesLauncher.launch("categories-report-$timestamp.csv")
                        },
                        onExportJsonClick = {
                            val timestamp = System.currentTimeMillis() / 1000
                            createJsonLauncher.launch("kdach-share-$timestamp.json")
                        }
                    )
                } else {
                    CloudBackupTabContent(
                        isLinked = userProfile?.isGoogleLinked == true,
                        email = userProfile?.email,
                        lastSyncTimestamp = viewModel.lastSyncTimestamp,
                        isLoading = uiState.isLoading,
                        onTriggerDriveSync = {
                            viewModel.triggerDriveSync(
                                onSuccess = { Toast.makeText(context, "تمت المزامنة بنجاح!", Toast.LENGTH_SHORT).show() },
                                onFailure = { err -> Toast.makeText(context, "فشلت المزامنة: $err", Toast.LENGTH_LONG).show() }
                            )
                        },
                        onTriggerDriveRestore = {
                            viewModel.triggerDriveRestore(
                                onSuccess = { Toast.makeText(context, "تمت الاستعادة بنجاح!", Toast.LENGTH_SHORT).show() },
                                onFailure = { err -> Toast.makeText(context, "فشلت الاستعادة: $err", Toast.LENGTH_LONG).show() }
                            )
                        },
                        onLaunchGoogleSignIn = { launchGoogleSignIn() }
                    )
                }
            }

            // --- DIALOGS ---

            ProcessingOverlayDialog(isLoading = uiState.isLoading)

            BackupProgressDialog(
                backupProgress = backupProgress,
                onClearProgress = { viewModel.clearProgress() }
            )

            ComingSoonBottomSheet(
                showComingSoonSheet = showComingSoonSheet,
                onDismissRequest = { showComingSoonSheet = false }
            )

            FolderBackupOptionsDialog(
                showFolderBackupPasswordDialog = showFolderBackupPasswordDialog,
                onDismissRequest = { showFolderBackupPasswordDialog = false },
                onConfirm = { pw ->
                    showFolderBackupPasswordDialog = false
                    viewModel.runImmediateFolderBackup(pw)
                }
            )

            ExportOptionsDialog(
                showExportDialog = showExportDialog,
                onDismissRequest = { showExportDialog = false },
                onConfirm = { pw ->
                    showExportDialog = false
                    pendingExportPassword = pw
                    val timestamp = System.currentTimeMillis() / 1000
                    createBackupLauncher.launch("budget-backup-v4-$timestamp.zip")
                }
            )

            ImportPasswordDialog(
                showPasswordPrompt = uiState.showPasswordPrompt,
                passwordError = uiState.passwordError,
                onDismissRequest = { viewModel.cancelRestore() },
                onConfirm = { pw ->
                    val uri = uiState.pendingRestoreUri
                    if (uri != null) {
                        viewModel.prepareRestore(uri, pw)
                    }
                }
            )

            RestorePreviewDialog(
                restorePreview = uiState.restorePreview,
                onDismissRequest = { viewModel.cancelRestore() },
                onConfirm = { selectedTables ->
                    viewModel.confirmRestore(selectedTables)
                }
            )
        }
    }
}
