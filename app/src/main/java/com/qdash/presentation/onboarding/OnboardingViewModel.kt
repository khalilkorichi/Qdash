package com.qdash.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.Account
import com.qdash.domain.model.AccountType
import com.qdash.domain.repository.AccountRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.qdash.domain.repository.AuthRepository
import com.qdash.domain.repository.DriveSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.qdash.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.qdash.domain.model.BackupFileMetadata
import com.qdash.domain.usecase.settings.CheckForExistingBackupUseCase
import com.qdash.domain.usecase.settings.RestoreFromDriveUseCase

// Represents a wallet option the user can select/deselect in the setup screen
data class WalletOption(
    val type: AccountType,
    val name: String,
    val color: String,
    val icon: String,
    val isSelected: Boolean,
    val balance: String = ""
)

// Custom wallet added inline by the user (no predefined type)
data class CustomWalletDraft(
    val name: String = "",
    val balance: String = "",
    val color: String = "#6C63FF",
    val icon: String = "account_balance_wallet"
)

data class OnboardingUiState(
    val currentStep: Int = 1,
    val selectedLanguage: String = "ar",
    // Legacy fields kept for backward compat with step 3 old flow (now replaced)
    val baridiMobBalance: String = "",
    val cashBalance: String = "",
    val savingsBalance: String = "",
    // New wallet setup state
    val walletOptions: List<WalletOption> = listOf(
        WalletOption(AccountType.BARIDIMOB, "بريدي موب", "#8A2387", "phonelink_ring", isSelected = true),
        WalletOption(AccountType.CASH, "نقدي / كاش", "#11998e", "payments", isSelected = true),
        WalletOption(AccountType.SAVINGS, "حساب التوفير", "#4facfe", "savings", isSelected = false)
    ),
    val customWallets: List<CustomWalletDraft> = emptyList(),
    val showAddCustomWallet: Boolean = false,
    val customWalletDraft: CustomWalletDraft = CustomWalletDraft(),
    val balanceError: String? = null,
    val isSaving: Boolean = false,
    val savingMessage: String = "جاري تهيئة محفظتك المالية..."
)

class OnboardingViewModel(
    private val accountRepository: AccountRepository,
    private val preferencesManager: com.qdash.core.preferences.PreferencesManager,
    private val authRepository: AuthRepository,
    private val driveSyncRepository: DriveSyncRepository,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val checkForExistingBackupUseCase: CheckForExistingBackupUseCase,
    private val restoreFromDriveUseCase: RestoreFromDriveUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _backupFoundToRestore = MutableStateFlow<BackupFileMetadata?>(null)
    val backupFoundToRestore: StateFlow<BackupFileMetadata?> = _backupFoundToRestore.asStateFlow()

    private val _isRestoringBackup = MutableStateFlow(false)
    val isRestoringBackup: StateFlow<Boolean> = _isRestoringBackup.asStateFlow()

    init {
        // Default language is Arabic
        val savedLang = preferencesManager.appLanguage
        _uiState.value = _uiState.value.copy(selectedLanguage = savedLang)
    }

    fun nextStep() {
        val next = _uiState.value.currentStep + 1
        _uiState.value = _uiState.value.copy(currentStep = next)
    }

    fun prevStep() {
        if (_uiState.value.currentStep > 1) {
            val prev = _uiState.value.currentStep - 1
            _uiState.value = _uiState.value.copy(currentStep = prev)
        }
    }

    fun setLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = lang)
        preferencesManager.appLanguage = lang
    }

    fun onBalanceChanged(accountType: AccountType, value: String) {
        val sanitized = sanitizeBalance(value)
        val updated = _uiState.value.walletOptions.map {
            if (it.type == accountType) it.copy(balance = sanitized) else it
        }
        _uiState.value = _uiState.value.copy(walletOptions = updated, balanceError = null)
    }

    fun onWalletToggled(type: AccountType) {
        val updated = _uiState.value.walletOptions.map {
            if (it.type == type) it.copy(isSelected = !it.isSelected) else it
        }
        _uiState.value = _uiState.value.copy(walletOptions = updated)
    }

    fun onShowAddCustomWallet() {
        _uiState.value = _uiState.value.copy(
            showAddCustomWallet = true,
            customWalletDraft = CustomWalletDraft()
        )
    }

    fun onCustomWalletNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            customWalletDraft = _uiState.value.customWalletDraft.copy(name = name)
        )
    }

    fun onCustomWalletBalanceChanged(balance: String) {
        val sanitized = sanitizeBalance(balance)
        _uiState.value = _uiState.value.copy(
            customWalletDraft = _uiState.value.customWalletDraft.copy(balance = sanitized),
            balanceError = null
        )
    }

    fun onCustomWalletColorChanged(color: String) {
        _uiState.value = _uiState.value.copy(
            customWalletDraft = _uiState.value.customWalletDraft.copy(color = color)
        )
    }

    fun onConfirmCustomWallet() {
        val draft = _uiState.value.customWalletDraft
        if (draft.name.isBlank()) return
        val balance = draft.balance.toDoubleOrNull() ?: 0.0
        if (balance < 0) {
            _uiState.value = _uiState.value.copy(balanceError = "لا يمكن إدخال قيمة سالبة")
            return
        }
        _uiState.value = _uiState.value.copy(
            customWallets = _uiState.value.customWallets + draft,
            showAddCustomWallet = false,
            customWalletDraft = CustomWalletDraft()
        )
    }

    fun onDismissCustomWallet() {
        _uiState.value = _uiState.value.copy(
            showAddCustomWallet = false,
            customWalletDraft = CustomWalletDraft()
        )
    }

    fun onRemoveCustomWallet(index: Int) {
        val updated = _uiState.value.customWallets.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(customWallets = updated)
    }

    /** Validates balance input: no negatives, no non-numeric chars except one dot */
    private fun sanitizeBalance(value: String): String {
        val filtered = value.filter { it.isDigit() || it == '.' }
        // Allow only one decimal point
        val parts = filtered.split(".")
        return if (parts.size > 2) parts[0] + "." + parts.drop(1).joinToString("") else filtered
    }

    fun validateBalance(value: String): String? {
        val d = value.toDoubleOrNull()
        return when {
            value.isNotBlank() && d == null -> "أدخل رقماً صحيحاً"
            d != null && d < 0 -> "لا يمكن إدخال قيمة سالبة"
            else -> null
        }
    }

    fun saveNotificationPermission(granted: Boolean) {
        preferencesManager.notificationPermissionHandled = true
        preferencesManager.notificationPermissionGranted = granted
    }

    fun completeWalletSetup(skip: Boolean, onFinished: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            if (!skip) {
                val state = _uiState.value
                var isFirstAccount = true

                // Insert only selected predefined wallets
                state.walletOptions.filter { it.isSelected }.forEachIndexed { _, option ->
                    val balance = option.balance.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    accountRepository.insertAccount(
                        Account(
                            name = option.name,
                            type = option.type,
                            balance = balance,
                            color = option.color,
                            icon = option.icon,
                            isDefault = isFirstAccount
                        )
                    )
                    isFirstAccount = false
                }

                // Insert custom wallets
                state.customWallets.forEach { draft ->
                    val balance = draft.balance.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    accountRepository.insertAccount(
                        Account(
                            name = draft.name,
                            type = AccountType.OTHER,
                            balance = balance,
                            color = draft.color,
                            icon = draft.icon,
                            isDefault = isFirstAccount
                        )
                    )
                    isFirstAccount = false
                }
            }

            // Mark setup as done regardless of skip — prevents showing screen again.
            // Also sets isFirstLaunch = false so returning users skip onboarding on next cold start.
            completeOnboardingUseCase()
            preferencesManager.walletSetupSkipped = skip

            _uiState.value = _uiState.value.copy(isSaving = false)
            onFinished()
        }
    }

    fun linkGoogleAccount(account: GoogleSignInAccount, context: Context, onFinished: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, savingMessage = "جاري ربط حساب Google والتحقق من النسخ الاحتياطية...")
            val result = authRepository.signIn(account)
            if (result.isSuccess) {
                preferencesManager.hasPendingBackupRestoreCheck = true
                val checkResult = checkForExistingBackupUseCase(context)
                if (checkResult.isSuccess) {
                    val metadata = checkResult.getOrThrow()
                    if (metadata != null) {
                        _backupFoundToRestore.value = metadata
                        _uiState.value = _uiState.value.copy(isSaving = false)
                    } else {
                        // No backup found: first-time Google user, upload current local database
                        driveSyncRepository.uploadToAppData(context)
                        preferencesManager.hasPendingBackupRestoreCheck = false
                        preferencesManager.isFirstLaunch = false
                        _uiState.value = _uiState.value.copy(isSaving = false)
                        onFinished()
                    }
                } else {
                    preferencesManager.hasPendingBackupRestoreCheck = false
                    preferencesManager.isFirstLaunch = false
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    onFinished()
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savingMessage = "فشل ربط الحساب: ${result.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"}"
                )
            }
        }
    }

    fun restoreBackup(context: Context, onFinished: () -> Unit) {
        viewModelScope.launch {
            _isRestoringBackup.value = true
            val result = restoreFromDriveUseCase(context)
            _isRestoringBackup.value = false
            if (result.isSuccess) {
                _backupFoundToRestore.value = null
                preferencesManager.hasPendingBackupRestoreCheck = false
                preferencesManager.isFirstLaunch = false
                onFinished()
            }
        }
    }

    fun skipBackupRestore(onFinished: () -> Unit) {
        viewModelScope.launch {
            _backupFoundToRestore.value = null
            preferencesManager.hasPendingBackupRestoreCheck = false
            completeOnboardingUseCase()
            preferencesManager.isFirstLaunch = false
            onFinished()
        }
    }

    fun skipGoogleSignIn(onFinished: () -> Unit) {
        preferencesManager.isFirstLaunch = false
        onFinished()
    }
}
