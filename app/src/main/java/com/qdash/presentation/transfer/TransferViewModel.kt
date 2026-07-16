package com.qdash.presentation.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import com.qdash.domain.usecase.transfer.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TransferUiState(
    val accounts: List<Account> = emptyList(),
    val transfers: List<TransferRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class TransferViewModel(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val transferBetweenAccountsUseCase = TransferBetweenAccountsUseCase(transferRepository, transactionRepository, accountRepository)
    private val getTransfersUseCase = GetTransfersUseCase(transferRepository)
    private val validateTransferUseCase = ValidateTransferUseCase(accountRepository)

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        loadTransferData()
    }

    private fun loadTransferData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                combine(
                    accountRepository.getAllAccounts(),
                    transferRepository.getAllTransfers()
                ) { accounts, transfers ->
                    _uiState.value.copy(
                        accounts = accounts,
                        transfers = transfers,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun executeTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, feeAmount: Double?, note: String?, date: Long = System.currentTimeMillis(), occurredAt: Long? = null, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, success = false, error = null) }
            val isValid = validateTransferUseCase(fromAccountId, amount, feeAmount)
            if (!isValid) {
                _uiState.update { it.copy(isLoading = false, error = "الرصيد في الحساب المصدر غير كافٍ لإجراء هذا التحويل.") }
                onComplete(false)
                return@launch
            }
            
            val request = TransferRequest(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = amount,
                feeAmount = feeAmount,
                note = note,
                date = date,
                occurredAt = occurredAt
            )
            val isSuccess = transferBetweenAccountsUseCase(request)
            if (isSuccess) {
                _uiState.update { it.copy(isLoading = false, success = true) }
                onComplete(true)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "فشل إجراء عملية التحويل. يرجى مراجعة البيانات.") }
                onComplete(false)
            }
        }
    }
}

