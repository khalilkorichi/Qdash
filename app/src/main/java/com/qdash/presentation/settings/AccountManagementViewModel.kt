package com.qdash.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.data.local.entities.UserProfileEntity
import com.qdash.domain.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountManagementViewModel(
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModel() {

    val userProfile: StateFlow<UserProfileEntity?> = authRepository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
