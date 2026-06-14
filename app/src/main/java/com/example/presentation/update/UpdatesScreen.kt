package com.example.presentation.update

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.core.ui.components.FinTrackTopBar
import com.example.data.update.UpdateInfo
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    viewModel: UpdatesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResumeCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            FinTrackTopBar(
                title = "تحديثات التطبيق",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    val state = uiState
                    if (state !is UpdateUiState.Checking &&
                        state !is UpdateUiState.Downloading &&
                        state !is UpdateUiState.Paused &&
                        state !is UpdateUiState.BackupInProgress &&
                        state !is UpdateUiState.BackupSuccess) {
                        IconButton(onClick = { viewModel.checkForUpdates() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة الفحص",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is UpdateUiState.Idle -> {
                    StatusHero(
                        icon = Icons.Default.SystemUpdate,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "تحديثات النظام",
                        subtitle = "اضغط للتحقق من وجود إصدارات جديدة"
                    )
                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("التحقق من التحديثات", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateUiState.Checking -> {
                    StatusHero(
                        icon = Icons.Default.Sync,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "جاري التحقق...",
                        subtitle = "يرجى الانتظار، نتواصل مع خوادم GitHub"
                    )
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
                is UpdateUiState.NoUpdate -> {
                    StatusHero(
                        icon = Icons.Default.CheckCircle,
                        iconColor = IncomeGreen,
                        title = "تطبيقك محدّث!",
                        subtitle = "أنت تستخدم النسخة الأخيرة من قداشّ"
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("معلومات الإصدار الحالي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("نسخة التطبيق", color = TextGray)
                                Text(state.localVersion, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("إعادة الفحص", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateUiState.UpdateAvailable -> {
                    UpdateCard(info = state.info)
                    Button(
                        onClick = { viewModel.downloadUpdate(state.info) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تحميل التحديث", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateUiState.Downloading -> {
                    UpdateCard(info = state.info)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("جاري تحميل التحديث...", fontWeight = FontWeight.Bold)
                                if (state.progress >= 0) {
                                    Text("${state.progress}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (state.progress >= 0) {
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { viewModel.pauseDownload(state.info) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إيقاف مؤقت", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is UpdateUiState.Paused -> {
                    UpdateCard(info = state.info)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("تم إيقاف التحميل مؤقتاً", fontWeight = FontWeight.Bold)
                                Text("${state.progress}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            LinearProgressIndicator(
                                progress = { state.progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.resumeDownload(state.info) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("استئناف التحميل", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is UpdateUiState.DownloadFailed -> {
                    UpdateCard(info = state.info)
                    Text(
                        text = state.error,
                        color = ExpenseRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.downloadUpdate(state.info) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Text("إعادة محاولة التحميل", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateUiState.ReadyToInstall -> {
                    UpdateCard(info = state.info)
                    
                    Text(
                        text = "سيقوم التطبيق بعمل نسخة احتياطية لبياناتك تلقائياً في مجلد التنزيلات لحمايتها قبل بدء التثبيت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Button(
                        onClick = { viewModel.triggerSafetyBackupAndInstall(context, state.info, state.localApkFile) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تثبيت الآن", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateUiState.BackupInProgress -> {
                    StatusHero(
                        icon = Icons.Default.Backup,
                        iconColor = SavingsAmber,
                        title = "حفظ البيانات الاحتياطية...",
                        subtitle = "يرجى الانتظار، نقوم بتأمين بياناتك المحلية أولاً"
                    )
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = SavingsAmber)
                }
                is UpdateUiState.BackupSuccess -> {
                    StatusHero(
                        icon = Icons.Default.CheckCircle,
                        iconColor = IncomeGreen,
                        title = "تم حفظ النسخة بنجاح!",
                        subtitle = "جارٍ تشغيل مثبت حزم أندرويد..."
                    )
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = IncomeGreen)
                }
                is UpdateUiState.FallbackRecovery -> {
                    FallbackRecoveryFlow(
                        viewModel = viewModel,
                        info = state.info,
                        file = state.localApkFile,
                        step = state.step
                    )
                }
                is UpdateUiState.Error -> {
                    StatusHero(
                        icon = Icons.Default.Error,
                        iconColor = ExpenseRed,
                        title = "حدث خطأ ما",
                        subtitle = state.error
                    )
                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHero(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun UpdateCard(
    info: UpdateInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("إصدار جديد متوفر!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text("الإصدار v${info.versionName}", style = MaterialTheme.typography.labelSmall, color = TextGray)
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("حجم الملف", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    val sizeInMb = String.format("%.2f MB", info.apkSize.toDouble() / (1024 * 1024))
                    Text(sizeInMb, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("المصدر", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Text("GitHub Releases", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (!info.releaseNotes.isNullOrBlank()) {
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                Text("ما الجديد في هذا الإصدار:", style = MaterialTheme.typography.labelSmall, color = TextGray)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackRecoveryFlow(
    viewModel: UpdatesViewModel,
    info: UpdateInfo,
    file: File,
    step: FallbackStep,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportManualBackup(uri)
        } else {
            Toast.makeText(context, "تم إلغاء التصدير.", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ExpenseRed.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed)
                }
                Column {
                    Text("تعذر التثبيت التلقائي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    Text("معالجة تعارض التوقيع الرقمي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                }
            }

            Text(
                text = "يرفض نظام أندرويد تثبيت التحديث مباشرة فوق التطبيق الحالي. يحدث هذا عادة بسبب اختلاف مفتاح التوقيع الرقمي للمطور (Signature Mismatch). لحل المشكلة، يرجى اتباع الخطوات الإرشادية التالية لحماية بياناتك وتثبيت التحديث:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right
            )

            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

            // Step 1: Export Backup
            val step1Done = step != FallbackStep.BACKUP
            FallbackStepItem(
                number = 1,
                title = "تصدير نسخة احتياطية لبياناتك",
                desc = "هام جداً: سيؤدي إلغاء التثبيت إلى حذف قاعدة البيانات. اضغط لتصدير نسخة احتياطية كاملة وحفظها بأمان (في Drive أو التنزيلات).",
                isActive = step == FallbackStep.BACKUP,
                isDone = step1Done,
                actionButton = {
                    Button(
                        onClick = { backupLauncher.launch("Qdash_Backup.zip") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ البيانات الاحتياطية", fontSize = 12.sp)
                    }
                }
            )

            // Step 2: Copy APK
            val step2Done = step == FallbackStep.UNINSTALL
            FallbackStepItem(
                number = 2,
                title = "نسخ ملف التحديث إلى التنزيلات",
                desc = "حفظ ملف التثبيت APK باسم (Qdash-Install-This.apk) في مجلد التنزيلات بالجهاز لتتمكن من فتحه وتثبيته لاحقاً.",
                isActive = step == FallbackStep.COPY_APK,
                isDone = step2Done,
                actionButton = {
                    Button(
                        onClick = { viewModel.copyApkToDownloadsFolder(context, file, info) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("نسخ ملف APK الآن", fontSize = 12.sp)
                    }
                }
            )

            // Step 3: Uninstall App
            FallbackStepItem(
                number = 3,
                title = "إزالة النسخة الحالية وتثبيت التحديث",
                desc = "اضغط لإلغاء تثبيت هذا التطبيق. بعد الإزالة، اذهب إلى تطبيق الملفات (Files/Downloads) في جهازك، وافتح Qdash-Install-This.apk لتثبيته. عند فتح التطبيق الجديد، استورد البيانات من النسخة الاحتياطية.",
                isActive = step == FallbackStep.UNINSTALL,
                isDone = false,
                actionButton = {
                    Button(
                        onClick = { viewModel.launchUninstall(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء التثبيت ومتابعة", fontSize = 12.sp)
                    }
                }
            )
            
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

            TextButton(
                onClick = { viewModel.cancelFallback(info, file) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إلغاء والعودة للخلف", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun FallbackStepItem(
    number: Int,
    title: String,
    desc: String,
    isActive: Boolean,
    isDone: Boolean,
    actionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (isActive || isDone) 1f else 0.5f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isDone) IncomeGreen
                    else if (isActive) MaterialTheme.colorScheme.primary
                    else TextGray.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = number.toString(),
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else TextGray,
                    fontSize = 12.sp
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDone) IncomeGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray.copy(alpha = alpha)
            )
            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                actionButton()
            }
        }
    }
}
