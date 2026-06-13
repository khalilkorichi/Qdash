package com.example.presentation.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.core.utils.ExportUtility
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
    val errorMessage: String? = null
)

class BackupViewModel(
    private val backupManager: BackupManager,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    // Export safe full system ZIP backup
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isLoading = true)
            backupManager.exportBackup(uri)
                .onSuccess {
                    _uiState.value = BackupUiState(successMessage = "تم تصدير النسخة الاحتياطية بنجاح!")
                }
                .onFailure { error ->
                    _uiState.value = BackupUiState(errorMessage = "فشل التصدير: ${error.localizedMessage}")
                }
        }
    }

    // Import and restore safe system ZIP backup
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isLoading = true)
            backupManager.importBackup(uri)
                .onSuccess {
                    _uiState.value = BackupUiState(successMessage = "تمت استعادة البيانات بنجاح! سيتم تحديث قاعدة البيانات الحالية.")
                }
                .onFailure { error ->
                    _uiState.value = BackupUiState(errorMessage = "فشل الاستعادة: ${error.localizedMessage}")
                }
        }
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
