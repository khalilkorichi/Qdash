package com.qdash.presentation.debt

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import com.qdash.domain.usecase.debt.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class DebtUiState(
    val debts: List<Debt> = emptyList(),
    val orderedPlanDebts: List<Debt> = emptyList(),
    val payments: List<DebtPayment> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val insights: List<String> = emptyList(),
    val selectedDebt: Debt? = null,
    val selectedDebtPayments: List<DebtPayment> = emptyList(),
    val strategyResults: List<DebtStrategyResult> = emptyList(),
    val selectedStrategy: String = "snowball", // "snowball", "avalanche", "priority"
    val selectedDirection: DebtDirection = DebtDirection.OWED_BY_ME,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val strategyComparison: List<DebtStrategyResult> get() = strategyResults
    val debtsOwedByMe: List<Debt> get() = debts.filter { it.direction == DebtDirection.OWED_BY_ME }
    val debtsOwedToMe: List<Debt> get() = debts.filter { it.direction == DebtDirection.OWED_TO_ME }
    val totalOwedByMe: Double get() = debtsOwedByMe.filter { !it.isClosed }.sumOf { it.remainingAmount }
    val totalOwedToMe: Double get() = debtsOwedToMe.filter { !it.isClosed }.sumOf { it.remainingAmount }
    val netPosition: Double get() = totalOwedToMe - totalOwedByMe
}

class DebtViewModel(
    private val debtRepository: DebtRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val addDebtUseCase = AddDebtUseCase(debtRepository)
    private val addLentDebtUseCase = AddLentDebtUseCase(debtRepository, transactionRepository)
    private val recordDebtPaymentUseCase = RecordDebtPaymentUseCase(debtRepository, transactionRepository)
    private val recordLentDebtRepaymentUseCase = RecordLentDebtRepaymentUseCase(debtRepository, transactionRepository)
    private val getDebtPlanUseCase = GetDebtPlanUseCase()
    private val compareDebtStrategiesUseCase = CompareDebtStrategiesUseCase()
    private val getDebtInsightsUseCase = GetDebtInsightsUseCase(debtRepository)
    private val closeDebtUseCase = CloseDebtUseCase(debtRepository)
    private val updateRegularDebtUseCase = UpdateRegularDebtUseCase(debtRepository, transactionRepository)
    private val updateInstallmentDebtUseCase = UpdateInstallmentDebtUseCase(debtRepository)
    private val deleteDebtUseCase = DeleteDebtUseCase(debtRepository, transactionRepository)
    private val forgiveDebtUseCase = ForgiveDebtUseCase(debtRepository)
    private val cancelDebtPaymentUseCase = CancelDebtPaymentUseCase(debtRepository, transactionRepository)

    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var detailsJob: Job? = null

    private data class DebtStreamSnapshot(
        val debts: List<Debt>,
        val accounts: List<Account>,
        val payments: List<DebtPayment>,
        val insights: List<String>,
        val strategies: List<DebtStrategyResult>
    )

    init {
        loadDebtData()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadDebtData(showInitialSkeleton = false)
            kotlinx.coroutines.delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadDebtData(showInitialSkeleton: Boolean = true) {
        loadJob?.cancel()
        if (showInitialSkeleton && _uiState.value.debts.isEmpty()) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }
        loadJob = viewModelScope.launch {
            try {
                combine(
                    debtRepository.getAllDebts(),
                    accountRepository.getAllAccounts(),
                    debtRepository.getAllPayments()
                ) { debts, accounts, payments ->
                    val strategies = compareDebtStrategiesUseCase(debts)
                    val insights = buildDynamicInsights(debts)
                    DebtStreamSnapshot(
                        debts = debts,
                        accounts = accounts,
                        payments = payments,
                        insights = insights,
                        strategies = strategies
                    )
                }.collect { snapshot ->
                    _uiState.update { current ->
                        val myDebts = snapshot.debts.filter { it.direction == DebtDirection.OWED_BY_ME }
                        val orderedPlan = getDebtPlanUseCase(myDebts, current.selectedStrategy)
                        val refreshedSelected = current.selectedDebt?.let { sel ->
                            snapshot.debts.find { it.id == sel.id }
                        }
                        val refreshedSelectedPayments = refreshedSelected?.let { sel ->
                            snapshot.payments
                                .filter { it.debtId == sel.id }
                                .sortedByDescending { it.paymentDate }
                        } ?: emptyList()

                        current.copy(
                            debts = snapshot.debts,
                            orderedPlanDebts = orderedPlan,
                            accounts = snapshot.accounts,
                            payments = snapshot.payments,
                            insights = snapshot.insights,
                            strategyResults = snapshot.strategies,
                            selectedDebt = refreshedSelected,
                            selectedDebtPayments = refreshedSelectedPayments,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun buildDynamicInsights(debts: List<Debt>): List<String> {
        val activeDebts = debts.filter { !it.isClosed }
        val myDebts = activeDebts.filter { it.direction == DebtDirection.OWED_BY_ME }
        val lentDebts = activeDebts.filter { it.direction == DebtDirection.OWED_TO_ME }
        val list = mutableListOf<String>()

        if (activeDebts.isEmpty()) {
            list.add("الحمد لله، لا توجد ديون معلقة أو التزامات متأخرة حالياً.")
        } else {
            val now = System.currentTimeMillis()
            val overdueCount = myDebts.count { it.dueDate != null && it.dueDate!! < now }
            if (overdueCount > 0) {
                list.add("تنبيه: لديك $overdueCount التزام تجاوز موعد استحقاقه، يُنصح بإعطائه الأولوية.")
            }
            val highPriority = myDebts.filterIsInstance<InstallmentDebt>().minByOrNull { it.priority }
            if (highPriority != null) {
                list.add("نوصي بتركيز السيولة الفائضة لتسوية '${highPriority.title}' للدائن '${highPriority.creditorName}'.")
            }
            if (lentDebts.isNotEmpty()) {
                val overdueLent = lentDebts.count { it.dueDate != null && it.dueDate!! < now }
                if (overdueLent > 0) {
                    list.add("لديك $overdueLent سلفة حلّ موعد استردادها عند الغير.")
                }
            }
        }
        return list
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
                    icon = "payments"
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
            val existingIcon = _uiState.value.debts.find { it.id == debtId }?.icon
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
                    icon = existingIcon ?: "payments"
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
                    icon = existingIcon ?: "credit_card",
                    interestRate = interestRate ?: 0.0,
                    dueDate = dueDate
                )
            }
            result.onSuccess {
                if (_uiState.value.selectedDebt?.id == debtId) {
                    selectDebt(debtId)
                }
                onSuccess()
            }.onFailure {
                onError(it.localizedMessage ?: "حدث خطأ أثناء تعديل البيانات")
            }
        }
    }

    fun makePayment(
        debtId: Long,
        accountId: Long,
        amount: Double,
        paymentType: DebtPaymentType,
        note: String?,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            recordDebtPaymentUseCase(debtId, accountId, amount, paymentType, note, date)
            if (_uiState.value.selectedDebt?.id == debtId) {
                selectDebt(debtId)
            }
        }
    }

    fun selectDebt(debtId: Long) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            val debt = debtRepository.getDebtById(debtId)
            if (debt != null) {
                val payments = debtRepository.getPaymentsForDebt(debtId).first()
                    .sortedByDescending { it.paymentDate }
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

    fun selectStrategy(strategy: String) {
        _uiState.update { current ->
            val myDebts = current.debts.filter { it.direction == DebtDirection.OWED_BY_ME }
            current.copy(
                selectedStrategy = strategy,
                orderedPlanDebts = getDebtPlanUseCase(myDebts, strategy)
            )
        }
    }

    fun changeStrategy(strategy: String) = selectStrategy(strategy)

    fun selectDirection(direction: DebtDirection) {
        _uiState.update { it.copy(selectedDirection = direction) }
    }

    fun addLentDebt(
        title: String,
        debtorName: String,
        totalAmount: Double,
        linkedAccountId: Long,
        dueDate: Long? = null,
        notes: String? = null,
        color: String = "#10B981"
    ) {
        viewModelScope.launch {
            addLentDebtUseCase(
                title = title,
                debtorName = debtorName,
                totalAmount = totalAmount,
                linkedAccountId = linkedAccountId,
                dueDate = dueDate,
                notes = notes,
                color = color
            )
        }
    }

    fun makeLentRepayment(
        debtId: Long,
        receivingAccountId: Long,
        amount: Double,
        note: String?,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            recordLentDebtRepaymentUseCase(
                debtId = debtId,
                receivingAccountId = receivingAccountId,
                amount = amount,
                note = note,
                paymentDate = date
            )
            if (_uiState.value.selectedDebt?.id == debtId) {
                selectDebt(debtId)
            }
        }
    }
}
