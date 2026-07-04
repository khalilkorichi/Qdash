package com.qdash.presentation.onboarding

import android.content.Context
import android.content.SharedPreferences
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

data class OnboardingUiState(
    val currentStep: Int = 1,
    val selectedLanguage: String = "ar",
    val baridiMobBalance: String = "",
    val cashBalance: String = "",
    val savingsBalance: String = "",
    val isSaving: Boolean = false,
    val savingMessage: String = "جاري تهيئة محفظتك المالية..."
)

class OnboardingViewModel(
    private val accountRepository: AccountRepository,
    private val preferencesManager: com.qdash.core.preferences.PreferencesManager,
    private val authRepository: AuthRepository,
    private val driveSyncRepository: DriveSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

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
        // Filter out non-numeric characters except one dot
        val sanitized = value.filter { it.isDigit() || it == '.' }
        _uiState.value = when (accountType) {
            AccountType.BARIDIMOB -> _uiState.value.copy(baridiMobBalance = sanitized)
            AccountType.CASH -> _uiState.value.copy(cashBalance = sanitized)
            AccountType.SAVINGS -> _uiState.value.copy(savingsBalance = sanitized)
            else -> _uiState.value
        }
    }

    fun saveNotificationPermission(granted: Boolean) {
        preferencesManager.notificationPermissionHandled = true
        preferencesManager.notificationPermissionGranted = granted
    }

    fun completeWalletSetup(skip: Boolean, onFinished: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            // Get existing seeded default accounts
            val existing = accountRepository.getAllAccounts().first()

            val baridiBalance = if (skip) 0.0 else (_uiState.value.baridiMobBalance.toDoubleOrNull() ?: 0.0)
            val cBalance = if (skip) 0.0 else (_uiState.value.cashBalance.toDoubleOrNull() ?: 0.0)
            val sBalance = if (skip) 0.0 else (_uiState.value.savingsBalance.toDoubleOrNull() ?: 0.0)

            // Update or insert BaridiMob
            val baridiMobAcc = existing.find { it.type == AccountType.BARIDIMOB }
            if (baridiMobAcc != null) {
                accountRepository.updateAccount(baridiMobAcc.copy(balance = baridiBalance))
            } else {
                accountRepository.insertAccount(
                    Account(name = "بريدي موب", type = AccountType.BARIDIMOB, balance = baridiBalance, color = "#8A2387", icon = "phonelink_ring", isDefault = true)
                )
            }

            // Update or insert Cash
            val cashAcc = existing.find { it.type == AccountType.CASH }
            if (cashAcc != null) {
                accountRepository.updateAccount(cashAcc.copy(balance = cBalance))
            } else {
                accountRepository.insertAccount(
                    Account(name = "نقدي / كاش", type = AccountType.CASH, balance = cBalance, color = "#11998e", icon = "payments", isDefault = false)
                )
            }

            // Update or insert Savings
            val savingsAcc = existing.find { it.type == AccountType.SAVINGS }
            if (savingsAcc != null) {
                accountRepository.updateAccount(savingsAcc.copy(balance = sBalance))
            } else {
                accountRepository.insertAccount(
                    Account(name = "حساب التوفير", type = AccountType.SAVINGS, balance = sBalance, color = "#4facfe", icon = "savings", isDefault = false)
                )
            }

            // Save state flags in shared preferences
            preferencesManager.walletSetupCompleted = !skip
            preferencesManager.walletSetupSkipped = skip

            _uiState.value = _uiState.value.copy(isSaving = false)
            nextStep()
        }
    }

    fun linkGoogleAccount(account: GoogleSignInAccount, context: Context, onFinished: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, savingMessage = "جاري ربط حساب Google ومزامنة البيانات...")
            val result = authRepository.signIn(account)
            if (result.isSuccess) {
                // Try pulling from Google Drive
                val syncResult = driveSyncRepository.downloadFromAppData(context)
                if (syncResult.isSuccess) {
                    val hasBackup = syncResult.getOrThrow()
                    if (!hasBackup) {
                        // No backup found: first-time Google user, upload current local seeded database
                        driveSyncRepository.uploadToAppData(context)
                    }
                }
                preferencesManager.isFirstLaunch = false
                _uiState.value = _uiState.value.copy(isSaving = false)
                onFinished()
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savingMessage = "فشل ربط الحساب: ${result.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"}"
                )
            }
        }
    }

    fun skipGoogleSignIn(onFinished: () -> Unit) {
        preferencesManager.isFirstLaunch = false
        onFinished()
    }
}
