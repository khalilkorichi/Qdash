package com.example.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.UnifiedScreenHeader
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

    // Activity Result Launchers for Scoped SAF Document Creation/Opening
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
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
                                text = "حافظ على أمان أموالك بالتصدير والاستيراد الفوري والكامل يدوياً، أو دع أندرويد يحمي تطبيقك تلقائياً.",
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
                        val timestamp = System.currentTimeMillis() / 1000
                        createBackupLauncher.launch("budget-backup-v4-$timestamp.zip")
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
        }
    }
}
