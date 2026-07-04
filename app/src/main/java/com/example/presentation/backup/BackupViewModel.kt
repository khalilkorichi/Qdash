package com.example.presentation.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.core.utils.ExportUtility
import com.example.domain.model.RestorePreview
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    // Export safe full JSON-based GZIP ZIP backup
    fun exportBackupV2(uri: Uri, password: CharArray?, includeAttachments: Boolean) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isLoading = true)
            backupManager.exportBackupV2(uri, password, includeAttachments)
                .onSuccess {
                    _uiState.value = BackupUiState(successMessage = "تم تصدير النسخة الاحتياطية الموحدة بنجاح!")
                }
                .onFailure { error ->
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
                        // Needs password input
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            restorePreview = preview,
                            showPasswordPrompt = true
                        )
                    } else {
                        // Password verified or backup is unencrypted
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
            backupManager.performRestoreV2(preview, selectedTables)
                .onSuccess {
                    _uiState.value = BackupUiState(
                        successMessage = "تم استعادة البيانات المحددة بنجاح! سيتم تحديث قاعدة البيانات الحالية."
                    )
                }
                .onFailure { error ->
                    _uiState.value = BackupUiState(
                        errorMessage = "فشلت عملية الاستعادة: ${error.localizedMessage}"
                    )
                }
        }
    }

    fun cancelRestore() {
        _uiState.value = BackupUiState()
    }

    // Legacy ZIP backup functions (Redirected to V2 unencrypted)
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
}
