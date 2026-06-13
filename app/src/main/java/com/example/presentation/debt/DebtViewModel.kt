package com.example.presentation.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import com.example.domain.usecase.debt.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    private fun loadDebtData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
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
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val debt = Debt(
                title = title,
                creditorName = creditorName,
                totalAmount = totalAmount,
                remainingAmount = totalAmount,
                minimumPayment = minimumPayment,
                paymentFrequency = paymentFrequency,
                linkedAccountId = linkedAccountId,
                priority = priority,
                notes = notes,
                color = color,
                icon = "credit_card",
                interestRate = interestRate,
                dueDate = dueDate
            )
            addDebtUseCase(debt)
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

    fun selectDebt(debtId: Long) {
        viewModelScope.launch {
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
            val debt = debtRepository.getDebtById(debtId)
            if (debt != null) {
                debtRepository.deleteDebt(debt)
                debtRepository.deletePaymentsForDebt(debtId)
                if (_uiState.value.selectedDebt?.id == debtId) {
                    _uiState.update { it.copy(selectedDebt = null, selectedDebtPayments = emptyList()) }
                }
            }
        }
    }

    fun changeStrategy(strategy: String) {
        _uiState.update { it.copy(selectedStrategy = strategy) }
    }
}
