package com.qdash.presentation.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.BackupProgress
import com.qdash.domain.model.RestorePreview
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
    showTopBar: Boolean = true
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val backupFolderUri by viewModel.backupFolderUri.collectAsState()
    val backupScheduleInterval by viewModel.backupScheduleInterval.collectAsState()
    val lastBackupUriForShare by viewModel.lastBackupUri.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var showFolderBackupPasswordDialog by remember { mutableStateOf(false) }
    var exportEncrypt by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }

    // Password input for restore
    var restorePasswordInput by remember { mutableStateOf("") }

    // Coming soon Bottom Sheet
    var showComingSoonSheet by remember { mutableStateOf(false) }

    // Tab state
    var activeTab by remember { mutableIntStateOf(0) }

    // Table selection for restore
    var restoreAccountsAndTransactions by remember { mutableStateOf(true) }
    var restoreSavingsAndGoals by remember { mutableStateOf(true) }
    var restoreDebts by remember { mutableStateOf(true) }
    var restoreAdvanced by remember { mutableStateOf(true) }

    // Light mode design system alignment
    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
    val textSecondaryColor = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight
    val segmentedControlBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFEEEEEC)
    val selectedOptionBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val selectedOptionBorder = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFD0D0CD)
    val cardBgColor = if (isDark) ColorTokens.CardDark else ColorTokens.CardLight
    // Pill tab colors — light mode: unselected = subtle warm surface, selected = white + colored border
    val tabUnselectedBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color(0xFFF3F3F1)
    val tabUnselectedBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f) else ColorTokens.BorderLight
    val tabUnselectedContent = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else ColorTokens.TextSecondaryLight
    val infoBannerBg = if (isDark) Primary.copy(alpha = 0.12f) else Color(0xFFF8F8F6)
    val infoBannerBorder = if (isDark) Primary.copy(alpha = 0.0f) else ColorTokens.BorderLight

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
                                    if (isDark) Primary.copy(alpha = 0.20f) else ColorTokens.BorderLight,
                                    ShapeTokens.Md
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isDark) Primary else ColorTokens.TextPrimaryLight,
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

                // ── Premium Pill Tab Bar ────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "النسخ الاحتياطي اليدوي" to Icons.Default.Save,
                        "النسخ السحابي" to Icons.Default.Cloud
                    )
                    tabs.forEachIndexed { index, (label, icon) ->
                        val isSelected = activeTab == index

                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Primary.copy(alpha = 0.15f) else Color.White
                            } else tabUnselectedBg,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "backupTabBg"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) Primary else tabUnselectedContent,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "backupTabContent"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Primary.copy(alpha = 0.3f) else Color(0xFFB0B0AD)
                            } else tabUnselectedBorder,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "backupTabBorder"
                        )

                        val pillShape = RoundedCornerShape(12.dp)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(pillShape)
                                .background(bgColor)
                                .border(
                                    width = 1.dp,
                                    color = borderColor,
                                    shape = pillShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    activeTab = index
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    color = contentColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Render Active Tab Content
                if (activeTab == 0) {
                    // TAB 0: MANUAL BACKUP
                    Text(
                        text = "خيارات الحفظ والاسترداد المحلي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    AppCard(
                        variant = CardVariant.SOLID,
                        shape = ShapeTokens.Lg,
                        backgroundColor = cardBgColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Folder path check
                            if (backupFolderUri.isNullOrEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ColorTokens.Danger.copy(alpha = 0.08f), ShapeTokens.Md)
                                        .padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = ColorTokens.Danger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "يرجى تحديد مجلد لحفظ النسخ الاحتياطية لتنشيط الجدولة والنسخ الفوري.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorTokens.Danger,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Primary.copy(alpha = 0.04f), ShapeTokens.Md)
                                        .border(1.dp, Primary.copy(alpha = 0.08f), ShapeTokens.Md)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "مجلد الحفظ المعتمد حالياً:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textSecondaryColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = backupFolderUri!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Select Folder Button
                            AppButton(
                                onClick = { openFolderLauncher.launch(null) },
                                modifier = Modifier.fillMaxWidth(),
                                variant = ButtonVariant.BORDERED,
                                intent = ButtonIntent.PRIMARY,
                                shape = ShapeTokens.Lg,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Primary
                                    )
                                }
                            ) {
                                Text(
                                    text = if (backupFolderUri.isNullOrEmpty()) "تحديد مجلد الحفظ" else "تغيير مجلد الحفظ",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }

                            HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant else ColorTokens.BorderLight)

                            // Scheduling Interval Selector
                            Text(
                                text = "جدولة النسخ الاحتياطي التلقائي الخلفي:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(segmentedControlBg, RoundedCornerShape(8.dp))
                                    .padding(4.dp)
                            ) {
                                val options = listOf(
                                    "none" to "أبداً",
                                    "daily" to "يومياً",
                                    "weekly" to "أسبوعياً",
                                    "monthly" to "شهرياً"
                                )
                                options.forEach { (key, label) ->
                                    val selected = backupScheduleInterval == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selected) selectedOptionBg else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                width = if (selected) 1.dp else 0.dp,
                                                color = if (selected) selectedOptionBorder else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { viewModel.updateScheduleInterval(key) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) Primary else textSecondaryColor
                                        )
                                    }
                                }
                            }

                            // Immediate backup button
                            AppButton(
                                onClick = {
                                    if (!backupFolderUri.isNullOrEmpty()) {
                                        exportPassword = ""
                                        exportEncrypt = false
                                        showFolderBackupPasswordDialog = true
                                    }
                                },
                                enabled = !backupFolderUri.isNullOrEmpty(),
                                modifier = Modifier.fillMaxWidth(),
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
                                    text = "بدء نسخ فوري الآن",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Share Backup option
                            if (lastBackupUriForShare != null) {
                                AppButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, lastBackupUriForShare)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية"))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = ButtonVariant.FLAT,
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
                                        text = "مشاركة النسخة الاحتياطية الأخيرة",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorTokens.Info
                                    )
                                }
                            }
                        }
                    }

                    // Import ZIP button (Stays in manual section)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(ShapeTokens.Lg)
                            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(0.3f) else Color(0xFFF3F3F1))
                            .border(
                                width = 1.dp,
                                color = if (isDark) MaterialTheme.colorScheme.outlineVariant else ColorTokens.BorderLight,
                                shape = ShapeTokens.Lg
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { openBackupLauncher.launch(arrayOf("application/zip")) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isDark) Primary else ColorTokens.TextPrimaryLight
                            )
                            Text(
                                text = "استيراد واستعادة البيانات من ملف (ZIP)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Primary else ColorTokens.TextPrimaryLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Section 3: CSV / excel exports
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(ShapeTokens.Lg)
                            .background(
                                if (isDark) ColorTokens.Info.copy(alpha = 0.10f)
                                else ColorTokens.Info.copy(alpha = 0.08f)
                            )
                            .border(
                                width = 1.5.dp,
                                color = ColorTokens.Info.copy(alpha = if (isDark) 0.30f else 0.40f),
                                shape = ShapeTokens.Lg
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val timestamp = System.currentTimeMillis() / 1000
                                createJsonLauncher.launch("kdach-share-$timestamp.json")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isDark) ColorTokens.InfoDark else ColorTokens.Info
                            )
                            Text(
                                text = "مشاركة وتبادل البيانات الفردية (JSON)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ColorTokens.InfoDark else ColorTokens.Info
                            )
                        }
                    }
                } else {
                    // TAB 1: CLOUD BACKUP
                    Text(
                        text = "المزامنة السحابية وآمنة عبر السحاب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    AppCard(
                        variant = CardVariant.OUTLINED,
                        shape = ShapeTokens.Lg,
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = textSecondaryColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "النسخ الاحتياطي عبر سحابة Google",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                // Badge "قريباً"
                                Box(
                                    modifier = Modifier
                                        .background(textSecondaryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "قريباً",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textSecondaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "مزامنة سحابية تلقائية وآمنة لملفات النسخ الاحتياطي عبر حسابك الشخصي في Google Drive.",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondaryColor
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppButton(
                                    onClick = { },
                                    modifier = Modifier.weight(1f),
                                    variant = ButtonVariant.SOLID,
                                    intent = ButtonIntent.PRIMARY,
                                    enabled = false
                                ) {
                                    Text("مزامنة الآن", fontWeight = FontWeight.Bold)
                                }

                                AppButton(
                                    onClick = { },
                                    modifier = Modifier.weight(1f),
                                    variant = ButtonVariant.LIGHT,
                                    intent = ButtonIntent.PRIMARY,
                                    enabled = false
                                ) {
                                    Text("استعادة من السحاب", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Processing overlay (Default imports/exports)
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
                                color = textSecondaryColor
                            )
                        }
                    }
                }
            }

            // Real-time Progress Dialog (Immediate folder backups)
            val progressState = backupProgress
            if (progressState is BackupProgress.Running) {
                AlertDialog(
                    onDismissRequest = { /* Non-dismissable */ },
                    title = {
                        Text(
                            text = "جاري إنشاء النسخة الاحتياطية...",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = progressState.stage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            LinearProgressIndicator(
                                progress = { progressState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = Primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "${progressState.progressPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            } else if (progressState is BackupProgress.Success) {
                LaunchedEffect(Unit) {
                    viewModel.clearProgress()
                }
            } else if (progressState is BackupProgress.Failure) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearProgress() },
                    title = {
                        Text(
                            text = "فشل النسخ الاحتياطي",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    text = {
                        Text(
                            text = progressState.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearProgress() }) {
                            Text("موافق", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Google Drive Bottom Sheet
            if (showComingSoonSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showComingSoonSheet = false },
                    sheetState = rememberModalBottomSheetState(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), ShapeTokens.Md),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "المزامنة السحابية (Google Drive) - قريباً",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "الميزة قيد التطوير حالياً. لضمان أقصى درجات الخصوصية والأمان المالي وحماية بياناتك الحساسة، تم تفعيل النسخ الاحتياطي اليدوي الكامل والمشفّر محلياً (BackupFormatV2).\n\nسنقوم بتوفير إمكانية الربط السحابي المباشر بمجرد اكتمال اختبارات المزامنة والتحقق الأمني.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondaryColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        AppButton(
                            onClick = { showComingSoonSheet = false },
                            modifier = Modifier.fillMaxWidth(),
                            variant = ButtonVariant.SOLID,
                            intent = ButtonIntent.PRIMARY,
                            shape = ShapeTokens.Lg
                        ) {
                            Text("فهمت ذلك", color = Color.White)
                        }
                    }
                }
            }

            // Folder Backup Options Dialog
            if (showFolderBackupPasswordDialog) {
                AlertDialog(
                    onDismissRequest = { showFolderBackupPasswordDialog = false },
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
                                showFolderBackupPasswordDialog = false
                                val pw = if (exportEncrypt) exportPassword.toCharArray() else null
                                viewModel.runImmediateFolderBackup(pw)
                            }
                        ) {
                            Text("بدء النسخ", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFolderBackupPasswordDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Export Options Dialog (Single ZIP SAF)
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("خيارات التصدير الفردي", fontWeight = FontWeight.Bold) },
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

            // Restore Preview Dialog
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

                            HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant else ColorTokens.BorderLight)

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
                                    Text("يحتوي على: $accCount حسابات، $txCount عمليات", fontSize = 11.sp, color = textSecondaryColor)
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
                                    Text("يحتوي على: $sCount أهداف ادخار، $subCount اشتراكات", fontSize = 11.sp, color = textSecondaryColor)
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
                                    Text("يحتوي على: $debtCount ديون مسجلة", fontSize = 11.sp, color = textSecondaryColor)
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
                                    Text("يحتوي على: $templateCount قوالب، $profileCount ملفات بريدية، إلخ", fontSize = 11.sp, color = textSecondaryColor)
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
