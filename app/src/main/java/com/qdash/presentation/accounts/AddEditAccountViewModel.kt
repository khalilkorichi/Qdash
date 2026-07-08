package com.qdash.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.Account
import com.qdash.domain.model.AccountType
import com.qdash.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditAccountUiState(
    val accountId: Long? = null,
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val balance: Double = 0.0,
    val color: String = "#1976D2",
    val icon: String = "account_balance_wallet",
    val iconPath: String? = null, // local gallery image path
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false
)

class AddEditAccountViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditAccountUiState())
    val uiState: StateFlow<AddEditAccountUiState> = _uiState.asStateFlow()

    fun loadAccount(accountId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val account = accountRepository.getAccountById(accountId)
            if (account != null) {
                _uiState.update {
                    it.copy(
                        accountId = account.id,
                        name = account.name,
                        type = account.type,
                        balance = account.balance,
                        color = account.color,
                        icon = account.icon,
                        iconPath = account.iconPath,
                        isDefault = account.isDefault,
                        isActive = account.isActive,
                        isArchived = account.isArchived,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "الحساب غير موجود") }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onBalanceChange(value: Double) = _uiState.update { it.copy(balance = value) }
    fun onTypeChange(value: AccountType) = _uiState.update { it.copy(type = value) }
    fun onColorChange(value: String) = _uiState.update { it.copy(color = value) }
    fun onIconChange(value: String) = _uiState.update { it.copy(icon = value, iconPath = null) }
    fun onIconPathChange(path: String?) = _uiState.update { it.copy(iconPath = path) }
    fun onIsDefaultChange(value: Boolean) = _uiState.update { it.copy(isDefault = value) }
    fun onIsActiveChange(value: Boolean) = _uiState.update { it.copy(isActive = value) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun saveAccount() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "الرجاء إدخال اسم الحساب") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                if (state.accountId == null) {
                    // Create new
                    val newAccount = Account(
                        name = state.name.trim(),
                        type = state.type,
                        balance = state.balance,
                        color = state.color,
                        icon = state.icon,
                        iconPath = state.iconPath,
                        isDefault = state.isDefault,
                        isActive = state.isActive
                    )
                    val newId = accountRepository.insertAccount(newAccount)
                    if (state.isDefault) {
                        accountRepository.setDefaultAccount(newId)
                    }
                } else {
                    // Edit existing
                    val existing = accountRepository.getAccountById(state.accountId) ?: return@launch
                    val updated = existing.copy(
                        name = state.name.trim(),
                        type = state.type,
                        color = state.color,
                        icon = state.icon,
                        iconPath = state.iconPath,
                        isDefault = state.isDefault,
                        isActive = state.isActive
                    )
                    accountRepository.updateAccount(updated)
                    if (state.isDefault) {
                        accountRepository.setDefaultAccount(state.accountId)
                    }
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.localizedMessage ?: "حدث خطأ") }
            }
        }
    }

    fun archiveAccount() {
        val accountId = _uiState.value.accountId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                accountRepository.archiveAccount(accountId)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.localizedMessage ?: "حدث خطأ أثناء أرشفة الحساب") }
            }
        }
    }

    fun unarchiveAccount() {
        val accountId = _uiState.value.accountId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                accountRepository.unarchiveAccount(accountId)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.localizedMessage ?: "حدث خطأ أثناء إلغاء أرشفة الحساب") }
            }
        }
    }

    fun deleteAccount() {
        val accountId = _uiState.value.accountId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val account = accountRepository.getAccountById(accountId)
                if (account != null) {
                    accountRepository.deleteAccount(account)
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.localizedMessage ?: "حدث خطأ أثناء حذف الحساب") }
            }
        }
    }

    fun emptyAccount() {
        val accountId = _uiState.value.accountId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val account = accountRepository.getAccountById(accountId)
                if (account != null) {
                    val updated = account.copy(balance = 0.0)
                    accountRepository.updateAccount(updated)
                    _uiState.update { it.copy(balance = 0.0, isSaving = false) }
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.localizedMessage ?: "حدث خطأ أثناء تفريغ الحساب") }
            }
        }
    }
}
