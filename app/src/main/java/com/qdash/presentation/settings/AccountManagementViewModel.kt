package com.qdash.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.qdash.FinTrackApp
import com.qdash.domain.model.BackupFileMetadata
import com.qdash.domain.model.UserProfile
import com.qdash.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountManagementViewModel(
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModel() {

    private val container get() = (context.applicationContext as? FinTrackApp)?.container

    val userProfile: StateFlow<UserProfile?> = authRepository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _pendingCloudBackup = MutableStateFlow<BackupFileMetadata?>(null)
    val pendingCloudBackup: StateFlow<BackupFileMetadata?> = _pendingCloudBackup.asStateFlow()

    fun connectGoogleAccount(
        account: GoogleSignInAccount,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = authRepository.signIn(account)
            if (result.isSuccess) {
                val appContainer = container
                if (appContainer != null) {
                    appContainer.preferencesManager.hasPendingBackupRestoreCheck = true
                    val checkResult = appContainer.checkForExistingBackupUseCase(context)
                    val existing = checkResult.getOrNull()
                    if (existing != null) {
                        _pendingCloudBackup.value = existing
                    } else {
                        appContainer.driveSyncRepository.uploadToAppData(context)
                        appContainer.preferencesManager.hasPendingBackupRestoreCheck = false
                        onSuccess()
                    }
                } else {
                    onSuccess()
                }
            } else {
                onFailure(result.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف")
            }
            _isSyncing.value = false
        }
    }

    fun restoreExistingCloudBackup(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val appContainer = container ?: return@launch
            _isSyncing.value = true
            val result = appContainer.restoreFromDriveUseCase(context)
            _isSyncing.value = false
            if (result.isSuccess && result.getOrDefault(false)) {
                _pendingCloudBackup.value = null
                appContainer.preferencesManager.hasPendingBackupRestoreCheck = false
                onSuccess()
            } else {
                onFailure(result.exceptionOrNull()?.localizedMessage ?: "تعذّرت استعادة النسخة السحابية")
            }
        }
    }

    fun keepLocalDataAndUpload(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val appContainer = container
            _pendingCloudBackup.value = null
            if (appContainer != null) {
                _isSyncing.value = true
                appContainer.driveSyncRepository.uploadToAppData(context)
                appContainer.preferencesManager.hasPendingBackupRestoreCheck = false
                _isSyncing.value = false
            }
            onComplete()
        }
    }

    fun unlinkAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            onSuccess()
        }
    }

    fun saveBirthDate(birthDate: String) {
        viewModelScope.launch {
            authRepository.updateBirthDate(birthDate)
        }
    }
}
