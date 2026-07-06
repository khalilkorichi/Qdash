package com.qdash.presentation.update

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray

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

    val isMandatory = remember(uiState) {
        when (val state = uiState) {
            is UpdateUiState.UpdateAvailable -> state.info.mandatory
            is UpdateUiState.Downloading -> state.info.mandatory
            is UpdateUiState.Paused -> state.info.mandatory
            is UpdateUiState.ReadyToInstall -> state.info.mandatory
            is UpdateUiState.BackupInProgress -> state.info.mandatory
            is UpdateUiState.BackupSuccess -> state.info.mandatory
            else -> false
        }
    }

    BackHandler(enabled = isMandatory) {
        // Intercept and do nothing to prevent back navigation if mandatory update is active
    }

    Scaffold(
        topBar = {
            FinTrackTopBar(
                title = "تحديثات التطبيق",
                showBackButton = !isMandatory,
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
                    val steps by viewModel.checkingSteps.collectAsState()
                    CheckingUpdatesSection(steps = steps)
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
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
                    DownloadingUpdatesSection(
                        progress = state.progress,
                        speed = state.speed,
                        eta = state.eta,
                        onPauseDownload = { viewModel.pauseDownload(state.info) }
                    )
                }
                is UpdateUiState.Paused -> {
                    UpdateCard(info = state.info)
                    PausedUpdatesSection(
                        progress = state.progress,
                        onResumeDownload = { viewModel.resumeDownload(state.info) }
                    )
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

            val state = uiState
            if (state !is UpdateUiState.Checking &&
                state !is UpdateUiState.Downloading &&
                state !is UpdateUiState.Paused &&
                state !is UpdateUiState.BackupInProgress &&
                state !is UpdateUiState.BackupSuccess) {
                
                val downloadedApks by viewModel.downloadedApks.collectAsState()
                if (downloadedApks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DownloadedUpdatesSection(
                        downloadedApks = downloadedApks,
                        onInstall = { file, version -> viewModel.installDownloadedApk(context, file, version) },
                        onDelete = { file -> viewModel.deleteDownloadedApk(file) }
                    )
                }
            }
        }
    }
}
