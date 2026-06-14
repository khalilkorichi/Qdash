package com.example.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import com.example.core.utils.FormatterUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

data class SettingsUiState(
    val lastBackupDate: String = "غير متوفر",
    val isAutoBackupEnabled: Boolean = false,
    val connectedAccountEmail: String? = null,
    val backupRestoreStatus: String? = null,
    val isSyncing: Boolean = false,
    val isLoading: Boolean = false,
    val isDarkTheme: Boolean = false,
    val isHideDecimalsEnabled: Boolean = true,
    val isAmountWordsEnabled: Boolean = true,
    val dashboardSectionsOrder: List<String> = emptyList(),
    val dashboardSectionsVisibility: Map<String, Boolean> = emptyMap()
)

class SettingsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val incomeRepository: IncomeRepository,
    private val savingRepository: SavingRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val backupRepository: com.example.domain.repository.BackupRepository,
    private val preferencesManager: com.example.core.preferences.PreferencesManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadBackupPreferences()
    }

    private fun loadBackupPreferences() {
        // Read stats or local caches
        val date = preferencesManager.lastBackupDate
        val autoBackup = preferencesManager.autoBackupEnabled
        val email = preferencesManager.connectedEmail
        val darkTheme = preferencesManager.darkModeEnabled
        val hideDecimals = preferencesManager.hideDecimalsEnabled
        val amountWords = preferencesManager.amountWordsEnabled

        val sectionsOrder = preferencesManager.dashboardSectionsOrder.split(",")
        val sectionsVisibility = sectionsOrder.associateWith { preferencesManager.isSectionVisible(it) }

        FormatterUtils.hideDecimals = hideDecimals

        _uiState.update {
            it.copy(
                lastBackupDate = date,
                isAutoBackupEnabled = autoBackup,
                connectedAccountEmail = email,
                isDarkTheme = darkTheme,
                isHideDecimalsEnabled = hideDecimals,
                isAmountWordsEnabled = amountWords,
                dashboardSectionsOrder = sectionsOrder,
                dashboardSectionsVisibility = sectionsVisibility
            )
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        preferencesManager.darkModeEnabled = enabled
        _uiState.update { it.copy(isDarkTheme = enabled) }
    }

    fun toggleHideDecimals(enabled: Boolean) {
        preferencesManager.hideDecimalsEnabled = enabled
        FormatterUtils.hideDecimals = enabled
        _uiState.update { it.copy(isHideDecimalsEnabled = enabled) }
    }

    fun toggleAmountWords(enabled: Boolean) {
        preferencesManager.amountWordsEnabled = enabled
        _uiState.update { it.copy(isAmountWordsEnabled = enabled) }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        preferencesManager.autoBackupEnabled = enabled
        _uiState.update { it.copy(isAutoBackupEnabled = enabled) }
    }

    fun connectGoogleDriveAccount(email: String) {
        preferencesManager.connectedEmail = email
        _uiState.update { it.copy(connectedAccountEmail = email) }
    }

    fun disconnectGoogleDrive() {
        preferencesManager.connectedEmail = null
        _uiState.update { it.copy(connectedAccountEmail = null) }
    }

    // JSON Backup System: delegates database serialization to BackupRepository
    fun runBackup(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        _uiState.update { it.copy(isSyncing = true, backupRestoreStatus = "جاري تحضير النسخة الاحتياطية...") }
        
        viewModelScope.launch {
            try {
                val backupObj = backupRepository.exportAllDataAsJson()

                // Save JSON to local backup file as a cached secure state (encrypted using AES-256)
                val backupFile = File(context.filesDir, "kdach_backup_drive.json")
                val encryptedData = com.example.core.utils.CryptoUtils.encrypt(backupObj.toString())
                backupFile.writeText(encryptedData)

                // Update settings metadata
                val dateString = FormatterUtils.formatDate(System.currentTimeMillis())
                preferencesManager.lastBackupDate = dateString

                _uiState.update {
                    it.copy(
                        lastBackupDate = dateString,
                        isSyncing = false,
                        backupRestoreStatus = "تم النسخ الاحتياطي بنجاح كملف JSON آمن!"
                    )
                }
                onSuccess(dateString)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, backupRestoreStatus = "فشل النسخ الاحتياطي: ${e.localizedMessage}") }
                onFailure(e.localizedMessage ?: "خطأ غير معروف")
            }
        }
    }

    // JSON Restore System: conflict-safe restore with confirmation
    fun runRestore(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _uiState.update { it.copy(isSyncing = true, backupRestoreStatus = "جاري قراءة ملف الاستعادة...") }

        viewModelScope.launch {
            try {
                val backupFile = File(context.filesDir, "kdach_backup_drive.json")
                if (!backupFile.exists()) {
                    _uiState.update { it.copy(isSyncing = false, backupRestoreStatus = "لا توجد نسخة احتياطية محفوظة للاتصال!") }
                    onFailure("ملف النسخة الاحتياطية غير موجود.")
                    return@launch
                }

                val encryptedBackupStr = backupFile.readText()
                val backupStr = try {
                    com.example.core.utils.CryptoUtils.decrypt(encryptedBackupStr)
                } catch (e: Exception) {
                    // Fallback in case the file was not encrypted (old plain JSON backup compatibility)
                    if (encryptedBackupStr.trim().startsWith("{")) {
                        encryptedBackupStr
                    } else {
                        throw e
                    }
                }
                val backupObj = JSONObject(backupStr)

                // Schema validation: check that the JSON contains required tables before deleting anything
                if (!backupObj.has("accounts") || !backupObj.has("transactions") || !backupObj.has("categories")) {
                    throw IllegalArgumentException("ملف النسخة الاحتياطية غير صالح أو لا يحتوي على الجداول الأساسية للتطبيق!")
                }

                // Restore database using repository
                backupRepository.restoreFromJson(backupObj)

                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        backupRestoreStatus = "تم استعادة البيانات بالكامل بنجاح!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, backupRestoreStatus = "فشلت الاستعادة: ${e.localizedMessage}") }
                onFailure(e.localizedMessage ?: "خطأ غير معروف")
            }
        }
    }

    fun saveDashboardCustomization(order: List<String>, visibility: Map<String, Boolean>) {
        preferencesManager.dashboardSectionsOrder = order.joinToString(",")
        visibility.forEach { (section, isVisible) ->
            preferencesManager.setSectionVisible(section, isVisible)
        }
        _uiState.update {
            it.copy(
                dashboardSectionsOrder = order,
                dashboardSectionsVisibility = visibility
            )
        }
    }
}
