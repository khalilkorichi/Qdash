package com.example.presentation.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.backup.BackupManager
import com.example.data.update.DownloadState
import com.example.data.update.UpdateInfo
import com.example.data.update.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class NoUpdate(val localVersion: String) : UpdateUiState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateUiState()
    data class Downloading(val info: UpdateInfo, val progress: Int) : UpdateUiState()
    data class Paused(val info: UpdateInfo, val progress: Int) : UpdateUiState()
    data class DownloadFailed(val info: UpdateInfo, val error: String) : UpdateUiState()
    data class ReadyToInstall(val info: UpdateInfo, val localApkFile: File) : UpdateUiState()
    data class BackupInProgress(val info: UpdateInfo, val localApkFile: File) : UpdateUiState()
    data class BackupSuccess(val info: UpdateInfo, val localApkFile: File, val backupUri: Uri) : UpdateUiState()
    data class FallbackRecovery(val info: UpdateInfo, val localApkFile: File, val step: FallbackStep) : UpdateUiState()
    data class Error(val error: String) : UpdateUiState()
}

enum class FallbackStep {
    BACKUP,
    COPY_APK,
    UNINSTALL
}

class UpdatesViewModel(
    private val repository: UpdateRepository,
    private val backupManager: BackupManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var wasInstallationTriggered = false
    private var lastTriggeredApkFile: File? = null
    private var lastUpdateInfo: UpdateInfo? = null
    private var downloadJob: kotlinx.coroutines.Job? = null
    private var currentProgress = 0

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            repository.checkForUpdates()
                .onSuccess { info ->
                    if (info.hasUpdate) {
                        _uiState.value = UpdateUiState.UpdateAvailable(info)
                    } else {
                        val versionStr = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                        _uiState.value = UpdateUiState.NoUpdate(versionStr)
                    }
                }
                .onFailure { error ->
                    _uiState.value = UpdateUiState.Error(error.localizedMessage ?: "فشل التحقق من التحديثات")
                }
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        val startBytes = if (_uiState.value is UpdateUiState.Paused) {
            File(context.cacheDir, "Qdash-update-temp.apk").let { if (it.exists()) it.length() else 0L }
        } else {
            File(context.cacheDir, "Qdash-update-temp.apk").let { if (it.exists()) it.delete() }
            0L
        }

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            repository.downloadApk(info.apkUrl, startBytes).collect { downloadState ->
                when (downloadState) {
                    is DownloadState.Idle -> {
                        if (_uiState.value !is UpdateUiState.Downloading) {
                            val initialPercent = if (info.apkSize > 0) (startBytes * 100 / info.apkSize).toInt().coerceIn(0, 100) else 0
                            _uiState.value = UpdateUiState.Downloading(info, initialPercent)
                        }
                    }
                    is DownloadState.Progress -> {
                        val percentage = if (downloadState.percentage >= 0) downloadState.percentage else currentProgress
                        currentProgress = percentage
                        _uiState.value = UpdateUiState.Downloading(info, percentage)
                    }
                    is DownloadState.Success -> {
                        // Checksum validation
                        val isValid = if (!info.apkSha256.isNullOrBlank()) {
                            repository.verifyApkSha256(downloadState.file, info.apkSha256)
                        } else {
                            true
                        }

                        if (isValid) {
                            _uiState.value = UpdateUiState.ReadyToInstall(info, downloadState.file)
                        } else {
                            _uiState.value = UpdateUiState.DownloadFailed(info, "فشلت عملية التحقق من سلامة الملف (SHA256 Mismatch).")
                        }
                    }
                    is DownloadState.Error -> {
                        _uiState.value = UpdateUiState.DownloadFailed(info, downloadState.throwable.localizedMessage ?: "فشل تحميل الملف.")
                    }
                }
            }
        }
    }

    fun pauseDownload(info: UpdateInfo) {
        downloadJob?.cancel()
        _uiState.value = UpdateUiState.Paused(info, currentProgress)
    }

    fun resumeDownload(info: UpdateInfo) {
        downloadUpdate(info)
    }

    fun triggerSafetyBackupAndInstall(context: Context, info: UpdateInfo, file: File) {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.BackupInProgress(info, file)
            // Perform automatic silent backup to Downloads folder before update
            repository.backupDataBeforeUpdate(backupManager)
                .onSuccess { uri ->
                    _uiState.value = UpdateUiState.BackupSuccess(info, file, uri)
                    installUpdate(context, info, file)
                }
                .onFailure { error ->
                    // Even if backup fails, we let the user install, but warning them
                    _uiState.value = UpdateUiState.ReadyToInstall(info, file)
                    // We will launch installation anyway as user choice
                    installUpdate(context, info, file)
                }
        }
    }

    fun installUpdate(context: Context, info: UpdateInfo, file: File) {
        // Check unknown source install permissions on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            wasInstallationTriggered = true
            lastTriggeredApkFile = file
            lastUpdateInfo = info
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.value = UpdateUiState.DownloadFailed(info, "تعذر تشغيل مثبت الحزم: ${e.localizedMessage}")
        }
    }

    fun onResumeCheck() {
        // If the installation was triggered and we are resuming the app, it means the install failed or was cancelled
        if (wasInstallationTriggered) {
            wasInstallationTriggered = false
            val file = lastTriggeredApkFile
            val info = lastUpdateInfo
            if (file != null && info != null) {
                // Set state to FallbackRecovery so the user can access uninstall instructions
                _uiState.value = UpdateUiState.FallbackRecovery(info, file, FallbackStep.BACKUP)
            }
        }
    }

    fun exportManualBackup(uri: Uri) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is UpdateUiState.FallbackRecovery) {
                backupManager.exportBackup(uri)
                    .onSuccess {
                        _uiState.value = currentState.copy(step = FallbackStep.COPY_APK)
                    }
            }
        }
    }

    fun copyApkToDownloadsFolder(context: Context, file: File, info: UpdateInfo) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is UpdateUiState.FallbackRecovery) {
                val filename = "Qdash-Install-This.apk"
                val resultUri = repository.copyApkToDownloads(file, filename)
                if (resultUri != null) {
                    _uiState.value = currentState.copy(step = FallbackStep.UNINSTALL)
                }
            }
        }
    }

    fun launchUninstall(context: Context) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun cancelFallback(info: UpdateInfo, file: File) {
        _uiState.value = UpdateUiState.ReadyToInstall(info, file)
    }
}
