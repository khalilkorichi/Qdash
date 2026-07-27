package com.qdash.presentation.update

import androidx.compose.runtime.Immutable

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.BuildConfig
import com.qdash.domain.model.CheckingStep
import com.qdash.domain.model.DownloadState
import com.qdash.domain.model.UpdateInfo
import com.qdash.domain.repository.BackupRepository
import com.qdash.domain.usecase.update.CheckForUpdateUseCase
import com.qdash.domain.usecase.update.DownloadUpdateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

@Immutable
sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class NoUpdate(val localVersion: String) : UpdateUiState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateUiState()
    data class Downloading(val info: UpdateInfo, val progress: Int, val speed: String = "", val eta: String = "") : UpdateUiState()
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

enum class CheckStepStatus {
    PENDING, RUNNING, COMPLETED, FAILED
}

data class CheckStepItem(
    val type: CheckingStepType,
    val title: String,
    val status: CheckStepStatus
)

enum class CheckingStepType {
    READ_LOCAL, FETCH_MANIFEST, FETCH_RELEASE, COMPARE
}

class UpdatesViewModel(
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val downloadUpdateUseCase: DownloadUpdateUseCase,
    private val backupRepository: BackupRepository,
    private val notificationRepository: com.qdash.domain.repository.NotificationRepository,
    private val preferencesManager: com.qdash.core.preferences.PreferencesManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _checkingSteps = MutableStateFlow<List<CheckStepItem>>(emptyList())
    val checkingSteps = _checkingSteps.asStateFlow()

    private val _downloadedApks = MutableStateFlow<List<File>>(emptyList())
    val downloadedApks = _downloadedApks.asStateFlow()

    private var wasInstallationTriggered = false
    private var lastTriggeredApkFile: File? = null
    private var lastUpdateInfo: UpdateInfo? = null
    private var currentProgress = 0

    init {
        loadDownloadedApks()
        observeDownloadState()
    }

    private fun observeDownloadState() {
        viewModelScope.launch {
            downloadUpdateUseCase.downloadState.collect { downloadState ->
                when (downloadState) {
                    is DownloadState.Idle -> {
                        val lastInfo = lastUpdateInfo
                        if (lastInfo != null) {
                            _uiState.value = UpdateUiState.UpdateAvailable(lastInfo)
                        } else {
                            _uiState.value = UpdateUiState.Idle
                        }
                    }
                    is DownloadState.Downloading -> {
                        lastUpdateInfo = downloadState.info
                        _uiState.value = UpdateUiState.Downloading(
                            info = downloadState.info,
                            progress = downloadState.progress,
                            speed = downloadState.speed,
                            eta = downloadState.eta
                        )
                    }
                    is DownloadState.Paused -> {
                        lastUpdateInfo = downloadState.info
                        _uiState.value = UpdateUiState.Paused(
                            info = downloadState.info,
                            progress = downloadState.progress
                        )
                    }
                    is DownloadState.Success -> {
                        lastUpdateInfo = downloadState.info
                        _uiState.value = UpdateUiState.ReadyToInstall(
                            info = downloadState.info,
                            localApkFile = downloadState.file
                        )
                        loadDownloadedApks()
                    }
                    is DownloadState.Error -> {
                        lastUpdateInfo = downloadState.info
                        _uiState.value = UpdateUiState.DownloadFailed(
                            info = downloadState.info,
                            error = downloadState.message
                        )
                    }
                }
            }
        }
    }

    fun loadDownloadedApks() {
        viewModelScope.launch {
            _downloadedApks.value = downloadUpdateUseCase.getDownloadedApks()
        }
    }

    private fun initCheckingSteps() {
        _checkingSteps.value = listOf(
            CheckStepItem(CheckingStepType.READ_LOCAL, "قراءة معلومات النسخة المحلية", CheckStepStatus.PENDING),
            CheckStepItem(CheckingStepType.FETCH_MANIFEST, "الاتصال بالخادم وجلب ملف التحديث السريع", CheckStepStatus.PENDING),
            CheckStepItem(CheckingStepType.FETCH_RELEASE, "التحقق الاحتياطي من خادم الإصدارات", CheckStepStatus.PENDING),
            CheckStepItem(CheckingStepType.COMPARE, "مقارنة الإصدارات وتحديد النتيجة النهائية", CheckStepStatus.PENDING)
        )
    }

    private fun updateStepStatus(type: CheckingStepType, status: CheckStepStatus) {
        _checkingSteps.value = _checkingSteps.value.map {
            if (it.type == type) it.copy(status = status) else it
        }
    }

    fun checkForUpdates(isBackground: Boolean = false) {
        viewModelScope.launch {
            if (!isBackground) {
                initCheckingSteps()
                _uiState.value = UpdateUiState.Checking
            }
            val result = checkForUpdateUseCase { step ->
                if (!isBackground) {
                    when (step) {
                        is CheckingStep.ReadingLocalVersion -> {
                            updateStepStatus(CheckingStepType.READ_LOCAL, CheckStepStatus.RUNNING)
                        }
                        is CheckingStep.FetchingManifest -> {
                            updateStepStatus(CheckingStepType.READ_LOCAL, CheckStepStatus.COMPLETED)
                            updateStepStatus(CheckingStepType.FETCH_MANIFEST, CheckStepStatus.RUNNING)
                        }
                        is CheckingStep.FetchingReleaseFallback -> {
                            updateStepStatus(CheckingStepType.FETCH_MANIFEST, CheckStepStatus.FAILED)
                            updateStepStatus(CheckingStepType.FETCH_RELEASE, CheckStepStatus.RUNNING)
                        }
                        is CheckingStep.ComparingVersions -> {
                            val currentSteps = _checkingSteps.value
                            val manifestStep = currentSteps.find { it.type == CheckingStepType.FETCH_MANIFEST }
                            if (manifestStep?.status == CheckStepStatus.RUNNING) {
                                updateStepStatus(CheckingStepType.FETCH_MANIFEST, CheckStepStatus.COMPLETED)
                                updateStepStatus(CheckingStepType.FETCH_RELEASE, CheckStepStatus.COMPLETED)
                            } else {
                                updateStepStatus(CheckingStepType.FETCH_RELEASE, CheckStepStatus.COMPLETED)
                            }
                            updateStepStatus(CheckingStepType.COMPARE, CheckStepStatus.RUNNING)
                        }
                        is CheckingStep.Success -> {
                            updateStepStatus(CheckingStepType.COMPARE, CheckStepStatus.COMPLETED)
                        }
                        is CheckingStep.Error -> {
                            updateStepStatus(CheckingStepType.COMPARE, CheckStepStatus.FAILED)
                        }
                        else -> {}
                    }
                }
            }

            preferencesManager.lastUpdateCheckTime = System.currentTimeMillis()

            result.onSuccess { info ->
                if (info.hasUpdate) {
                    val localVersionName = BuildConfig.VERSION_NAME
                    val localVersionCode = BuildConfig.VERSION_CODE
                    val isNewerVersion = isVersionNewer(localVersionName, info.versionName)
                    val isNewerVersionCode = info.versionCode > localVersionCode

                    if ((isNewerVersion || isNewerVersionCode) && preferencesManager.lastNotifiedUpdateVersion != info.versionName) {
                        preferencesManager.lastNotifiedUpdateVersion = info.versionName
                        val notification = com.qdash.domain.model.AppNotification(
                            title = "تحديث جديد متوفر! 🎉",
                            message = "إصدار جديد من التطبيق (${info.versionName}) متوفر الآن للتحميل.",
                            type = com.qdash.domain.model.NotificationType.TIP
                        )
                        notificationRepository.insertNotification(notification)
                    }

                    _uiState.value = UpdateUiState.UpdateAvailable(info)
                } else {
                    if (!isBackground) {
                        val versionStr = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                        _uiState.value = UpdateUiState.NoUpdate(versionStr)
                    } else {
                        _uiState.value = UpdateUiState.Idle
                    }
                }
            }.onFailure { error ->
                if (!isBackground) {
                    _uiState.value = UpdateUiState.Error(error.localizedMessage ?: "حدث خطأ أثناء فحص التحديثات")
                }
            }
        }
    }

    private fun isVersionNewer(local: String, remote: String): Boolean {
        val localParts = local.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until maxLength) {
            val localVal = localParts.getOrElse(i) { 0 }
            val remoteVal = remoteParts.getOrElse(i) { 0 }
            if (remoteVal > localVal) return true
            if (localVal > remoteVal) return false
        }
        return false
    }

    fun checkForUpdatesThrottled() {
        val lastCheck = preferencesManager.lastUpdateCheckTime
        val now = System.currentTimeMillis()
        if (now - lastCheck >= 30 * 60 * 1000L) { // 30 minutes throttle
            checkForUpdates(isBackground = true)
        }
    }

    private fun hasEnoughSpace(requiredBytes: Long): Boolean {
        return try {
            val stat = android.os.StatFs(context.cacheDir.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes > requiredBytes
        } catch (e: Exception) {
            true
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        if (info.apkSize > 0 && !hasEnoughSpace(info.apkSize + 10 * 1024 * 1024)) {
            _uiState.value = UpdateUiState.DownloadFailed(info, "مساحة التخزين غير كافية لتحميل هذا التحديث. يرجى توفير مساحة إضافية.")
            return
        }
        downloadUpdateUseCase.startDownload(info)
    }

    fun deleteDownloadedApk(file: File) {
        viewModelScope.launch {
            val success = downloadUpdateUseCase.deleteDownloadedApk(file)
            if (success) {
                loadDownloadedApks()
            }
        }
    }

    fun installDownloadedApk(context: Context, file: File, versionName: String) {
        val dummyInfo = UpdateInfo(
            hasUpdate = false,
            versionCode = 0,
            versionName = versionName,
            updateIdentity = file.lastModified(),
            apkUrl = "",
            apkSize = file.length(),
            apkSha256 = "",
            mandatory = false,
            releaseNotes = null
        )
        triggerSafetyBackupAndInstall(context, dummyInfo, file)
    }

    fun pauseDownload(info: UpdateInfo) {
        downloadUpdateUseCase.pauseDownload(info)
    }

    fun resumeDownload(info: UpdateInfo) {
        downloadUpdateUseCase.startDownload(info)
    }

    fun cancelDownload(info: UpdateInfo) {
        downloadUpdateUseCase.cancelDownload(info)
    }

    fun triggerSafetyBackupAndInstall(context: Context, info: UpdateInfo, file: File) {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.BackupInProgress(info, file)
            // Perform automatic silent backup before update
            downloadUpdateUseCase.backupDataBeforeUpdate()
                .onSuccess { uri ->
                    _uiState.value = UpdateUiState.BackupSuccess(info, file, uri)
                    installUpdate(context, info, file)
                }
                .onFailure { error ->
                    _uiState.value = UpdateUiState.DownloadFailed(
                        info,
                        error.localizedMessage ?: "فشل إنشاء نسخة احتياطية قبل التحديث."
                    )
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
        if (wasInstallationTriggered) {
            wasInstallationTriggered = false
            val file = lastTriggeredApkFile
            val info = lastUpdateInfo
            if (file != null && info != null) {
                _uiState.value = UpdateUiState.FallbackRecovery(info, file, FallbackStep.BACKUP)
            }
        }
    }

    fun exportManualBackup(uri: Uri) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is UpdateUiState.FallbackRecovery) {
                backupRepository.exportBackup(uri)
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
                val resultUri = downloadUpdateUseCase.copyApkToDownloads(file, filename)
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
