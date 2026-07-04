package com.qdash.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import com.qdash.data.local.entities.UserProfileEntity
import com.qdash.core.utils.FormatterUtils
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
    val useWesternNumerals: Boolean = true,
    val dashboardSectionsOrder: List<String> = emptyList(),
    val dashboardSectionsVisibility: Map<String, Boolean> = emptyMap(),
    val lastSyncTimestamp: Long = 0L
)

class SettingsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val incomeRepository: IncomeRepository,
    private val savingRepository: SavingRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val backupRepository: com.qdash.domain.repository.BackupRepository,
    private val preferencesManager: com.qdash.core.preferences.PreferencesManager,
    private val authRepository: AuthRepository,
    private val driveSyncRepository: DriveSyncRepository,
    private val context: Context
) : ViewModel() {

    val userProfile: StateFlow<UserProfileEntity?> = authRepository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isDarkTheme = preferencesManager.darkModeEnabled,
            isHideDecimalsEnabled = preferencesManager.hideDecimalsEnabled,
            isAmountWordsEnabled = preferencesManager.amountWordsEnabled,
            useWesternNumerals = preferencesManager.useWesternNumerals,
            lastSyncTimestamp = preferencesManager.lastSyncTimestamp
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadBackupPreferences()
    }

    private fun loadBackupPreferences() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Read stats or local caches
            val date = preferencesManager.lastBackupDate
            val autoBackup = preferencesManager.autoBackupEnabled
            val email = preferencesManager.connectedEmail
            val darkTheme = preferencesManager.darkModeEnabled
            val hideDecimals = preferencesManager.hideDecimalsEnabled
            val amountWords = preferencesManager.amountWordsEnabled
            val useWestern = preferencesManager.useWesternNumerals

            val sectionsOrder = preferencesManager.dashboardSectionsOrder.split(",")
            val sectionsVisibility = sectionsOrder.associateWith { preferencesManager.isSectionVisible(it) }

            FormatterUtils.hideDecimals = hideDecimals
            FormatterUtils.useWesternNumerals = useWestern

            _uiState.update {
                it.copy(
                    lastBackupDate = date,
                    isAutoBackupEnabled = autoBackup,
                    connectedAccountEmail = email,
                    isDarkTheme = darkTheme,
                    isHideDecimalsEnabled = hideDecimals,
                    isAmountWordsEnabled = amountWords,
                    useWesternNumerals = useWestern,
                    dashboardSectionsOrder = sectionsOrder,
                    dashboardSectionsVisibility = sectionsVisibility,
                    lastSyncTimestamp = preferencesManager.lastSyncTimestamp
                )
            }
        }
    }

    fun refreshSyncTimestamp() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val lastSync = preferencesManager.lastSyncTimestamp
            _uiState.update {
                it.copy(lastSyncTimestamp = lastSync)
            }
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

    fun toggleWesternNumerals(enabled: Boolean) {
        preferencesManager.useWesternNumerals = enabled
        FormatterUtils.useWesternNumerals = enabled
        _uiState.update { it.copy(useWesternNumerals = enabled) }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        preferencesManager.autoBackupEnabled = enabled
        _uiState.update { it.copy(isAutoBackupEnabled = enabled) }
    }

    fun connectGoogleDriveAccount(account: GoogleSignInAccount, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, backupRestoreStatus = "جاري ربط حساب Google...") }
            val result = authRepository.signIn(account)
            if (result.isSuccess) {
                // Download backup from Drive
                val syncResult = driveSyncRepository.downloadFromAppData(context)
                if (syncResult.isSuccess) {
                    val hasBackup = syncResult.getOrThrow()
                    if (!hasBackup) {
                        // Upload current local database
                        driveSyncRepository.uploadToAppData(context)
                    }
                }
                _uiState.update { it.copy(connectedAccountEmail = account.email) }
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"
                onFailure(err)
            }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    fun disconnectGoogleDrive(onFinished: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            authRepository.signOut(context)
            _uiState.update { it.copy(connectedAccountEmail = null) }
            _uiState.update { it.copy(isSyncing = false) }
            onFinished()
        }
    }

    fun saveBirthDate(birthDate: String) {
        viewModelScope.launch {
            authRepository.updateBirthDate(birthDate)
        }
    }

    fun triggerDriveSync(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, backupRestoreStatus = "جاري رفع النسخة الاحتياطية للسحابة...") }
            val result = driveSyncRepository.uploadToAppData(context)
            if (result.isSuccess) {
                _uiState.update { it.copy(backupRestoreStatus = "تمت المزامنة السحابية بنجاح!") }
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"
                _uiState.update { it.copy(backupRestoreStatus = "فشلت المزامنة: $err") }
                onFailure(err)
            }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    fun triggerDriveRestore(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, backupRestoreStatus = "جاري استعادة البيانات من السحابة...") }
            val result = driveSyncRepository.downloadFromAppData(context)
            if (result.isSuccess) {
                val hasBackup = result.getOrThrow()
                if (hasBackup) {
                    _uiState.update { it.copy(backupRestoreStatus = "تم استعادة البيانات السحابية بنجاح!") }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(backupRestoreStatus = "لا توجد نسخة احتياطية على هذا الحساب.") }
                    onFailure("لا توجد نسخة احتياطية سحابية محفوظة.")
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"
                _uiState.update { it.copy(backupRestoreStatus = "فشلت الاستعادة: $err") }
                onFailure(err)
            }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    // JSON Backup System: delegates database serialization to BackupRepository
    fun runBackup(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        _uiState.update { it.copy(isSyncing = true, backupRestoreStatus = "جاري تحضير النسخة الاحتياطية...") }
        
        viewModelScope.launch {
            try {
                val backupObj = backupRepository.exportAllDataAsJson()

                // Save JSON to local backup file as a cached secure state (encrypted using AES-256)
                val backupFile = File(context.filesDir, "kdach_backup_drive.json")
                val encryptedData = com.qdash.core.utils.CryptoUtils.encrypt(backupObj.toString())
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
                    com.qdash.core.utils.CryptoUtils.decrypt(encryptedBackupStr)
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

    fun restoreDefaultDashboardCustomization() {
        val defaultOrder = "split_cards,context_templates,templates,quick_actions,accounts,chart,budget,subscriptions,recent_transactions".split(",")
        val defaultVisibility = defaultOrder.associateWith { true }
        saveDashboardCustomization(defaultOrder, defaultVisibility)
    }
}
