package com.qdash.presentation.debt

import androidx.compose.runtime.Immutable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import com.qdash.domain.usecase.debt.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class DebtUiState(
    val debts: List<Debt> = emptyList(),
    val payments: List<DebtPayment> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val insights: List<String> = emptyList(),
    val selectedDebt: Debt? = null,
    val selectedDebtPayments: List<DebtPayment> = emptyList(),
    val strategyResults: List<DebtStrategyResult> = emptyList(),
    val selectedStrategy: String = "snowball", // "snowball", "avalanche"
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class DebtViewModel(
    private val debtRepository: DebtRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val addDebtUseCase = AddDebtUseCase(debtRepository)
    private val recordDebtPaymentUseCase = RecordDebtPaymentUseCase(debtRepository, transactionRepository)
    private val getDebtPlanUseCase = GetDebtPlanUseCase()
    private val compareDebtStrategiesUseCase = CompareDebtStrategiesUseCase()
    private val getDebtInsightsUseCase = GetDebtInsightsUseCase(debtRepository)
    private val closeDebtUseCase = CloseDebtUseCase(debtRepository)
    private val updateRegularDebtUseCase = UpdateRegularDebtUseCase(debtRepository)
    private val updateInstallmentDebtUseCase = UpdateInstallmentDebtUseCase(debtRepository)
    private val deleteDebtUseCase = DeleteDebtUseCase(debtRepository, transactionRepository)
    private val forgiveDebtUseCase = ForgiveDebtUseCase(debtRepository)
    private val cancelDebtPaymentUseCase = CancelDebtPaymentUseCase(debtRepository, transactionRepository)

    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    init {
        loadDebtData()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadDebtData()
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadDebtData() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        loadJob = viewModelScope.launch {
            try {
                combine(
                    debtRepository.getAllDebts(),
                    accountRepository.getAllAccounts(),
                    debtRepository.getAllPayments(),
                    getDebtInsightsUseCase()
                ) { debts, accounts, payments, insights ->
                    val strategies = compareDebtStrategiesUseCase(debts)
                    _uiState.value.copy(
                        debts = debts,
                        accounts = accounts,
                        payments = payments,
                        insights = insights,
                        strategyResults = strategies,
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

    fun addDebt(
        title: String,
        creditorName: String,
        totalAmount: Double,
        minimumPayment: Double,
        paymentFrequency: String,
        linkedAccountId: Long?,
        priority: Int,
        notes: String?,
        color: String,
        interestRate: Double? = null,
        dueDate: Long? = null,
        debtType: DebtType = DebtType.INSTALLMENT
    ) {
        viewModelScope.launch {
            val debt = if (debtType == DebtType.REGULAR) {
                RegularDebt(
                    title = title,
                    creditorName = creditorName,
                    totalAmount = totalAmount,
                    remainingAmount = totalAmount,
                    dueDate = dueDate,
                    linkedAccountId = linkedAccountId,
                    notes = notes,
                    color = color,
                    icon = "credit_card"
                )
            } else {
                InstallmentDebt(
                    title = title,
                    creditorName = creditorName,
                    totalAmount = totalAmount,
                    remainingAmount = totalAmount,
                    dueDate = dueDate,
                    linkedAccountId = linkedAccountId,
                    notes = notes,
                    color = color,
                    icon = "credit_card",
                    interestRate = interestRate ?: 0.0,
                    minimumPayment = minimumPayment,
                    recommendedPayment = null,
                    paymentFrequency = paymentFrequency,
                    priority = priority
                )
            }
            addDebtUseCase(debt)
        }
    }

    fun updateDebtDetails(
        debtId: Long,
        title: String,
        creditorName: String,
        totalAmount: Double,
        minimumPayment: Double,
        paymentFrequency: String,
        linkedAccountId: Long?,
        priority: Int,
        notes: String?,
        color: String,
        interestRate: Double? = null,
        dueDate: Long? = null,
        debtType: DebtType = DebtType.INSTALLMENT,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = if (debtType == DebtType.REGULAR) {
                updateRegularDebtUseCase(
                    debtId = debtId,
                    title = title,
                    creditorName = creditorName,
                    totalAmount = totalAmount,
                    dueDate = dueDate,
                    linkedAccountId = linkedAccountId,
                    notes = notes,
                    color = color,
                    icon = "credit_card"
                )
            } else {
                updateInstallmentDebtUseCase(
                    debtId = debtId,
                    title = title,
                    creditorName = creditorName,
                    totalAmount = totalAmount,
                    minimumPayment = minimumPayment,
                    paymentFrequency = paymentFrequency,
                    linkedAccountId = linkedAccountId,
                    priority = priority,
                    notes = notes,
                    color = color,
                    icon = "credit_card",
                    interestRate = interestRate ?: 0.0,
                    dueDate = dueDate
                )
            }
            result.onSuccess {
                onSuccess()
            }.onFailure {
                onError(it.localizedMessage ?: "حدث خطأ أثناء تعديل الدين")
            }
        }
    }

    fun makePayment(debtId: Long, accountId: Long, amount: Double, paymentType: DebtPaymentType, note: String?, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            recordDebtPaymentUseCase(debtId, accountId, amount, paymentType, note, date)
            if (_uiState.value.selectedDebt?.id == debtId) {
                selectDebt(debtId)
            }
        }
    }

    private var detailsJob: kotlinx.coroutines.Job? = null

    fun selectDebt(debtId: Long) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            val debt = debtRepository.getDebtById(debtId)
            if (debt != null) {
                val payments = debtRepository.getPaymentsForDebt(debtId).first()
                _uiState.update {
                    it.copy(
                        selectedDebt = debt,
                        selectedDebtPayments = payments
                    )
                }
            }
        }
    }

    fun closeDebt(debtId: Long) {
        viewModelScope.launch {
            closeDebtUseCase(debtId)
            if (_uiState.value.selectedDebt?.id == debtId) {
                selectDebt(debtId)
            }
        }
    }

    fun deleteDebt(debtId: Long) {
        viewModelScope.launch {
            deleteDebtUseCase(debtId)
            if (_uiState.value.selectedDebt?.id == debtId) {
                _uiState.update { it.copy(selectedDebt = null, selectedDebtPayments = emptyList()) }
            }
        }
    }

    fun forgiveDebt(debtId: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = forgiveDebtUseCase(debtId)
            result.onSuccess {
                if (_uiState.value.selectedDebt?.id == debtId) {
                    selectDebt(debtId)
                }
                onSuccess()
            }.onFailure {
                onError(it.localizedMessage ?: "حدث خطأ أثناء الإعفاء من الدين")
            }
        }
    }

    fun cancelPayment(paymentId: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = cancelDebtPaymentUseCase(paymentId)
            result.onSuccess {
                _uiState.value.selectedDebt?.let { selected ->
                    selectDebt(selected.id)
                }
                onSuccess()
            }.onFailure {
                onError(it.localizedMessage ?: "حدث خطأ أثناء إلغاء الدفعة")
            }
        }
    }

    fun changeStrategy(strategy: String) {
        _uiState.update { it.copy(selectedStrategy = strategy) }
    }
}
