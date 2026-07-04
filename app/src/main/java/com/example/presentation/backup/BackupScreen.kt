package com.example.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.UnifiedScreenHeader
import com.example.core.utils.FormatterUtils
import com.example.ui.designsystem.components.*
import com.example.ui.designsystem.tokens.*

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var exportEncrypt by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }

    // Password input for restore
    var restorePasswordInput by remember { mutableStateOf("") }

    // Table selection for restore
    var restoreAccountsAndTransactions by remember { mutableStateOf(true) }
    var restoreSavingsAndGoals by remember { mutableStateOf(true) }
    var restoreDebts by remember { mutableStateOf(true) }
    var restoreAdvanced by remember { mutableStateOf(true) }

    // SAF Activity Result Launchers
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            viewModel.exportBackupV2(it, pendingExportPassword, includeAttachments = true)
            pendingExportPassword = null
        }
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
            UnifiedScreenHeader(
                title = "النسخ الاحتياطي والاستعادة",
                subtitle = "حماية ونقل بياناتك المالية بأمان وسهولة",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Card
                AppCard(
                    variant = CardVariant.FLAT,
                    shape = ShapeTokens.Lg,
                    backgroundColor = Primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Primary.copy(alpha = 0.15f), ShapeTokens.Md),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Primary,
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
                                color = ColorTokens.TextGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 1: Core backups
                Text(
                    text = "النسخ الاحتياطي اليدوي الكامل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Export ZIP button
                AppButton(
                    onClick = {
                        exportPassword = ""
                        exportEncrypt = false
                        showExportDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                ) {
                    Text(
                        text = "تصدير نسخة احتياطية كاملة (ZIP)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Import ZIP button
                AppButton(
                    onClick = {
                        openBackupLauncher.launch(arrayOf("application/zip"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    variant = ButtonVariant.FLAT,
                    intent = ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Primary
                        )
                    }
                ) {
                    Text(
                        text = "استيراد واستعادة البيانات من ملف (ZIP)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: CSV / excel exports
                Text(
                    text = "تصدير التقارير وجداول العمليات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Export CSV transactions
                AppButton(
                    onClick = {
                        val timestamp = System.currentTimeMillis() / 1000
                        createCsvTransactionsLauncher.launch("transactions-report-$timestamp.csv")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    variant = ButtonVariant.BORDERED,
                    intent = ButtonIntent.SUCCESS,
                    shape = ShapeTokens.Lg,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ColorTokens.Success
                        )
                    }
                ) {
                    Text(
                        text = "تصدير كشف المعاملات (CSV)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorTokens.Success
                    )
                }

                // Export CSV categories
                AppButton(
                    onClick = {
                        val timestamp = System.currentTimeMillis() / 1000
                        createCsvCategoriesLauncher.launch("categories-report-$timestamp.csv")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    variant = ButtonVariant.BORDERED,
                    intent = ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Primary
                        )
                    }
                ) {
                    Text(
                        text = "تصدير قوائم الأقسام (CSV)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                // Export JSON data share
                AppButton(
                    onClick = {
                        val timestamp = System.currentTimeMillis() / 1000
                        createJsonLauncher.launch("kdach-share-$timestamp.json")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    variant = ButtonVariant.BORDERED,
                    intent = ButtonIntent.INFO,
                    shape = ShapeTokens.Lg,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ColorTokens.Info
                        )
                    }
                ) {
                    Text(
                        text = "مشاركة وتبادل البيانات الفردية (JSON)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorTokens.Info
                    )
                }
            }

            // Processing overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = ShapeTokens.Lg,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .width(260.dp)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, ShapeTokens.Lg)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 180.dp, height = 6.dp)
                                    .shimmerEffect(RoundedCornerShape(3.dp))
                            )
                            Text(
                                text = "جاري معالجة البيانات...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "يرجى الانتظار ولا تغلق الصفحة",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTokens.TextGray
                            )
                        }
                    }
                }
            }

            // Export Options Dialog
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("خيارات النسخ الاحتياطي", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = exportEncrypt,
                                    onCheckedChange = { exportEncrypt = it }
                                )
                                Text("تشفير النسخة الاحتياطية بكلمة مرور")
                            }

                            if (exportEncrypt) {
                                OutlinedTextField(
                                    value = exportPassword,
                                    onValueChange = { exportPassword = it },
                                    label = { Text("كلمة المرور") },
                                    placeholder = { Text("أدخل كلمة مرور قوية") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "تنبيه: في حال نسيان كلمة المرور، لن تتمكن من استرجاع بياناتك أبداً.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorTokens.Danger
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (exportEncrypt && exportPassword.isBlank()) return@TextButton
                                showExportDialog = false
                                pendingExportPassword = if (exportEncrypt) exportPassword.toCharArray() else null
                                val timestamp = System.currentTimeMillis() / 1000
                                createBackupLauncher.launch("budget-backup-v4-$timestamp.zip")
                            }
                        ) {
                            Text("تصدير", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Import Password Dialog
            if (uiState.showPasswordPrompt) {
                AlertDialog(
                    onDismissRequest = { viewModel.cancelRestore() },
                    title = { Text("الملف مشفر", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("يرجى إدخال كلمة المرور لفك تشفير بيانات النسخة الاحتياطية.")
                            OutlinedTextField(
                                value = restorePasswordInput,
                                onValueChange = { restorePasswordInput = it },
                                label = { Text("كلمة المرور") },
                                visualTransformation = PasswordVisualTransformation(),
                                isError = uiState.passwordError != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (uiState.passwordError != null) {
                                Text(
                                    uiState.passwordError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (restorePasswordInput.isNotBlank()) {
                                    val uri = uiState.pendingRestoreUri
                                    if (uri != null) {
                                        viewModel.prepareRestore(uri, restorePasswordInput.toCharArray())
                                    }
                                }
                            }
                        ) {
                            Text("تحقق وفك التشفير", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelRestore() }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Restore Preview and Table Selection Dialog
            val preview = uiState.restorePreview
            if (preview != null && !uiState.showPasswordPrompt) {
                AlertDialog(
                    onDismissRequest = { viewModel.cancelRestore() },
                    title = { Text("معاينة بيانات النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("تاريخ النسخة: ${FormatterUtils.formatDate(preview.manifest.createdAt)}")
                            Text("إصدار التطبيق: ${preview.manifest.appVersion}")
                            Text("إصدار الصيغة: ${preview.manifest.schemaVersion}")

                            HorizontalDivider()

                            Text("اختر البيانات المراد استعادتها:", fontWeight = FontWeight.Bold)

                            // 1. Accounts & Transactions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = restoreAccountsAndTransactions,
                                    onCheckedChange = { restoreAccountsAndTransactions = it }
                                )
                                Column {
                                    Text("الحسابات والعمليات والتحويلات", fontWeight = FontWeight.Bold)
                                    val txCount = preview.manifest.recordCounts["transactions"] ?: 0
                                    val accCount = preview.manifest.recordCounts["accounts"] ?: 0
                                    Text("يحتوي على: $accCount حسابات، $txCount عمليات", fontSize = 11.sp, color = ColorTokens.TextGray)
                                }
                            }

                            // 2. Savings
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = restoreSavingsAndGoals,
                                    onCheckedChange = { restoreSavingsAndGoals = it }
                                )
                                Column {
                                    Text("أهداف الادخار والاشتراكات", fontWeight = FontWeight.Bold)
                                    val sCount = preview.manifest.recordCounts["saving_goals"] ?: 0
                                    val subCount = preview.manifest.recordCounts["subscriptions"] ?: 0
                                    Text("يحتوي على: $sCount أهداف ادخار، $subCount اشتراكات", fontSize = 11.sp, color = ColorTokens.TextGray)
                                }
                            }

                            // 3. Debts
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = restoreDebts,
                                    onCheckedChange = { restoreDebts = it }
                                )
                                Column {
                                    Text("الديون والمدفوعات المتعلقة بها", fontWeight = FontWeight.Bold)
                                    val debtCount = preview.manifest.recordCounts["debts"] ?: 0
                                    Text("يحتوي على: $debtCount ديون مسجلة", fontSize = 11.sp, color = ColorTokens.TextGray)
                                }
                            }

                            // 4. Advanced Elements
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = restoreAdvanced,
                                    onCheckedChange = { restoreAdvanced = it }
                                )
                                Column {
                                    Text("القوالب، الإعدادات، والميزات الأخرى", fontWeight = FontWeight.Bold)
                                    val templateCount = preview.manifest.recordCounts["transaction_templates"] ?: 0
                                    val profileCount = preview.manifest.recordCounts["postal_profiles"] ?: 0
                                    Text("يحتوي على: $templateCount قوالب، $profileCount ملفات بريدية، إلخ", fontSize = 11.sp, color = ColorTokens.TextGray)
                                }
                            }

                            if (!preview.isCompatible) {
                                Text(
                                    "تحذير: إصدار صيغة النسخة الاحتياطية أحدث من إصدار التطبيق الحالي وقد تظهر مشاكل أثناء الاستعادة.",
                                    color = ColorTokens.Danger,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val selectedTables = mutableListOf<String>()
                                if (restoreAccountsAndTransactions) {
                                    selectedTables.addAll(listOf("accounts", "categories", "transactions", "transfers", "category_rules", "user_category_mappings"))
                                }
                                if (restoreSavingsAndGoals) {
                                    selectedTables.addAll(listOf("saving_goals", "savings_contributions", "subscriptions"))
                                }
                                if (restoreDebts) {
                                    selectedTables.addAll(listOf("debts", "debt_payments"))
                                }
                                if (restoreAdvanced) {
                                    selectedTables.addAll(listOf("income_sources", "budget_goals", "financial_plans", "transaction_templates", "notifications", "ai_chat_messages", "postal_profiles", "salary_delays", "salary_distributions", "salary_envelopes"))
                                }

                                if (selectedTables.isNotEmpty()) {
                                    viewModel.confirmRestore(selectedTables)
                                }
                            }
                        ) {
                            Text("استعادة البيانات المحددة", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelRestore() }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    }
}
