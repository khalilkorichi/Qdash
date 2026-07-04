package com.qdash.presentation.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.qdash.core.utils.FormatterUtils
import androidx.work.*
import com.qdash.core.preferences.PreferencesManager
import com.qdash.data.backup.BackupManager
import com.qdash.data.backup.ScheduledBackupWorker
import com.qdash.core.utils.ExportUtility
import com.qdash.data.local.entities.UserProfileEntity
import com.qdash.domain.model.BackupProgress
import com.qdash.domain.model.RestorePreview
import com.qdash.domain.repository.AccountRepository
import com.qdash.domain.repository.AuthRepository
import com.qdash.domain.repository.CategoryRepository
import com.qdash.domain.repository.DriveSyncRepository
import com.qdash.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class BackupUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val restorePreview: RestorePreview? = null,
    val showPasswordPrompt: Boolean = false,
    val passwordError: String? = null,
    val pendingRestoreUri: Uri? = null
)

class BackupViewModel(
    private val backupManager: BackupManager,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository,
    private val driveSyncRepository: DriveSyncRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    val userProfile: StateFlow<UserProfileEntity?> = authRepository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastSyncTimestamp: Long get() = preferencesManager.lastSyncTimestamp

    private val _backupProgress = MutableStateFlow<BackupProgress>(BackupProgress.Idle)
    val backupProgress = _backupProgress.asStateFlow()

    private val _backupFolderUri = MutableStateFlow(preferencesManager.backupFolderUri)
    val backupFolderUri = _backupFolderUri.asStateFlow()

    private val _backupScheduleInterval = MutableStateFlow(preferencesManager.backupScheduleInterval)
    val backupScheduleInterval = _backupScheduleInterval.asStateFlow()

    private val _lastBackupUri = MutableStateFlow<Uri?>(null)
    val lastBackupUri = _lastBackupUri.asStateFlow()

    // SAF directory picker persistence
    fun saveBackupFolder(uri: Uri) {
        try {
            // Take persistable permission to keep access across reboots
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            val uriStr = uri.toString()
            preferencesManager.backupFolderUri = uriStr
            _backupFolderUri.value = uriStr
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(
                errorMessage = "تعذر منح صلاحية الوصول الدائم للمجلد: ${e.localizedMessage}"
            )
        }
    }

    // Schedule background WorkManager tasks
    fun updateScheduleInterval(interval: String) {
        preferencesManager.backupScheduleInterval = interval
        _backupScheduleInterval.value = interval

        val workManager = WorkManager.getInstance(context)
        if (interval == "none") {
            workManager.cancelUniqueWork("scheduled_backup")
        } else {
            val repeatIntervalDays = when (interval) {
                "daily" -> 1L
                "weekly" -> 7L
                "monthly" -> 30L
                else -> 1L
            }

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val backupWorkRequest = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(repeatIntervalDays, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "scheduled_backup",
                ExistingPeriodicWorkPolicy.UPDATE,
                backupWorkRequest
            )
        }
    }

    // Run direct backup into SAF folder
    fun runImmediateFolderBackup(password: CharArray?) {
        val uriStr = _backupFolderUri.value
        if (uriStr.isNullOrEmpty()) {
            _backupProgress.value = BackupProgress.Failure("الرجاء اختيار مجلد الحفظ أولاً.")
            return
        }

        if (!backupManager.isFolderUriValid(uriStr)) {
            _backupProgress.value = BackupProgress.Failure("مجلد النسخ المختار لم يعد صالحاً أو تم سحب الصلاحيات. يرجى إعادة تحديده.")
            return
        }

        viewModelScope.launch {
            _backupProgress.value = BackupProgress.Running("بدء النسخ الاحتياطي...", 0)
            backupManager.exportBackupToFolder(
                folderUri = Uri.parse(uriStr),
                password = password,
                includeAttachments = true,
                maxKeepBackups = preferencesManager.keepMaxBackupsCount,
                onProgress = { stage, percent ->
                    _backupProgress.value = BackupProgress.Running(stage, percent)
                }
            ).fold(
                onSuccess = { fileDetails ->
                    _backupProgress.value = BackupProgress.Success
                    _lastBackupUri.value = Uri.parse(fileDetails.path)
                    val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    preferencesManager.lastBackupDate = formattedDate
                    _uiState.value = _uiState.value.copy(
                        successMessage = "تم إنشاء النسخة الاحتياطية بنجاح: ${fileDetails.name} (${FormatterUtils.formatFileSize(fileDetails.sizeBytes)})"
                    )
                },
                onFailure = { error ->
                    _backupProgress.value = BackupProgress.Failure(error.localizedMessage ?: "حدث خطأ غير معروف.")
                }
            )
        }
    }

    fun clearProgress() {
        _backupProgress.value = BackupProgress.Idle
    }

    // Export safe full JSON-based GZIP ZIP backup to SAF document
    fun exportBackupV2(uri: Uri, password: CharArray?, includeAttachments: Boolean) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isLoading = true)
            backupManager.exportBackupV2(uri, password, includeAttachments) { stage, percent ->
                // Optionally log or report
            }.onSuccess {
                _uiState.value = BackupUiState(successMessage = "تم تصدير النسخة الاحتياطية الموحدة بنجاح!")
            }.onFailure { error ->
                _uiState.value = BackupUiState(errorMessage = "فشل التصدير: ${error.localizedMessage}")
            }
        }
    }

    // Phase 1: Load Backup file, check manifest and decryption
    fun prepareRestore(uri: Uri, password: CharArray? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, passwordError = null, pendingRestoreUri = uri)
            backupManager.getRestorePreview(uri, password)
                .onSuccess { preview ->
                    if (preview.manifest.isEncrypted && (password == null || password.isEmpty())) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            restorePreview = preview,
                            showPasswordPrompt = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            restorePreview = preview,
                            showPasswordPrompt = false
                        )
                    }
                }
                .onFailure { error ->
                    if (password != null && password.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            passwordError = error.localizedMessage ?: "كلمة المرور غير صحيحة."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "فشل قراءة النسخة الاحتياطية: ${error.localizedMessage}"
                        )
                    }
                }
        }
    }

    // Phase 2: Execute Restore
    fun confirmRestore(selectedTables: List<String>?) {
        val preview = _uiState.value.restorePreview ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            backupManager.performRestoreV2(preview, selectedTables) { stage, percent ->
                // Optionally update
            }.onSuccess {
                _uiState.value = BackupUiState(
                    successMessage = "تم استعادة البيانات المحددة بنجاح! سيتم تحديث قاعدة البيانات الحالية."
                )
            }.onFailure { error ->
                _uiState.value = BackupUiState(
                    errorMessage = "فشلت عملية الاستعادة: ${error.localizedMessage}"
                )
            }
        }
    }

    fun cancelRestore() {
        _uiState.value = BackupUiState()
    }

    // Legacy ZIP backup functions
    fun exportBackup(uri: Uri) {
        exportBackupV2(uri, null, true)
    }

    fun importBackup(uri: Uri) {
        prepareRestore(uri, null)
    }

    // Export Transactions report to Excel-compatible CSV
    fun exportTransactionsToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isLoading = true)
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val categories = categoryRepository.getAllCategories().first()
                val accounts = accountRepository.getAllAccounts().first()

                val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ExportUtility.exportTransactionsToCsv(
                        context = context,
                        uri = uri,
                        transactions = transactions,
                        categories = categories,
                        accounts = accounts
                    )
                }

                if (success) {
                    _uiState.value = BackupUiState(successMessage = "تم تصدير كشف المعاملات (CSV) بنجاح!")
                } else {
                    _uiState.value = BackupUiState(errorMessage = "فشل تصدير كشف المعاملات.")
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState(errorMessage = "فشل التصدير: ${e.localizedMessage}")
            }
        }
    }

    // Export Categories to CSV
    fun exportCategoriesToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isLoading = true)
            try {
                val categories = categoryRepository.getAllCategories().first()
                val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ExportUtility.exportCategoriesToCsv(context, uri, categories)
                }
                if (success) {
                    _uiState.value = BackupUiState(successMessage = "تم تصدير تصنيفاتك (CSV) بنجاح!")
                } else {
                    _uiState.value = BackupUiState(errorMessage = "فشل تصدير الأقسام.")
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState(errorMessage = "فشل تصدير الأقسام: ${e.localizedMessage}")
            }
        }
    }

    // Export lightweight data share JSON
    fun exportDataToJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isLoading = true)
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ExportUtility.exportDataToJson(context, uri, transactions)
                }
                if (success) {
                    _uiState.value = BackupUiState(successMessage = "تم تصدير كود تبادل البيانات (JSON) بنجاح!")
                } else {
                    _uiState.value = BackupUiState(errorMessage = "فشل تصدير ملف التبادل.")
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState(errorMessage = "فشل تصدير ملف JSON: ${e.localizedMessage}")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    fun connectGoogleDriveAccount(
        account: GoogleSignInAccount,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                authRepository.signIn(account)
                preferencesManager.isGoogleLinked = true
                
                // Initial sync to upload current state
                _uiState.update { it.copy(isLoading = true) }
                val result = driveSyncRepository.uploadToAppData(context)
                if (result.isSuccess) {
                    preferencesManager.lastSyncTimestamp = System.currentTimeMillis()
                }
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                onFailure(e.localizedMessage ?: "فشل ربط الحساب")
            }
        }
    }

    fun triggerDriveSync(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val result = driveSyncRepository.uploadToAppData(context)
                _uiState.update { it.copy(isLoading = false) }
                if (result.isSuccess) {
                    preferencesManager.lastSyncTimestamp = System.currentTimeMillis()
                    _uiState.update { it.copy(successMessage = "تمت المزامنة السحابية بنجاح!") }
                    onSuccess()
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "فشلت المزامنة السحابية."
                    _uiState.update { it.copy(errorMessage = err) }
                    onFailure(err)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                onFailure(e.localizedMessage ?: "خطأ غير معروف")
            }
        }
    }

    fun triggerDriveRestore(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val result = driveSyncRepository.downloadFromAppData(context)
                _uiState.update { it.copy(isLoading = false) }
                if (result.isSuccess) {
                    val hasBackup = result.getOrThrow()
                    if (hasBackup) {
                        _uiState.update { it.copy(successMessage = "تمت استعادة البيانات السحابية بنجاح!") }
                        onSuccess()
                    } else {
                        _uiState.update { it.copy(errorMessage = "لا توجد نسخة احتياطية سحابية محفوظة.") }
                        onFailure("لا توجد نسخة احتياطية سحابية محفوظة.")
                    }
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "فشلت استعادة البيانات السحابية."
                    _uiState.update { it.copy(errorMessage = err) }
                    onFailure(err)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                onFailure(e.localizedMessage ?: "خطأ غير معروف")
            }
        }
    }
}
