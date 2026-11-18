package com.qdash.presentation.savings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import com.qdash.domain.usecase.savings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class SavingsUiState(
    val goals: List<SavingGoal> = emptyList(),
    val contributions: List<SavingsContribution> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val insights: List<SavingsInsight> = emptyList(),
    val selectedGoal: SavingGoal? = null,
    val selectedGoalHistory: List<SavingsContribution> = emptyList(),
    val forecastText: String = "",
    val showArchived: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class SavingsViewModel(
    private val savingRepository: SavingRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val addSavingsContributionUseCase = AddSavingsContributionUseCase(savingRepository, accountRepository, transactionRepository)
    private val withdrawFromSavingsUseCase = WithdrawFromSavingsUseCase(savingRepository, accountRepository, transactionRepository)
    private val getSavingsForecastUseCase = GetSavingsForecastUseCase(savingRepository)

    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var detailsJob: Job? = null

    private data class SavingsSnapshot(
        val goals: List<SavingGoal>,
        val accounts: List<Account>,
        val contributions: List<SavingsContribution>,
        val insights: List<SavingsInsight>
    )

    init {
        loadSavingsData()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadSavingsData(showInitialSkeleton = false)
            kotlinx.coroutines.delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleShowArchived() {
        _uiState.update { it.copy(showArchived = !it.showArchived) }
    }

    private fun loadSavingsData(showInitialSkeleton: Boolean = true) {
        loadJob?.cancel()
        if (showInitialSkeleton && _uiState.value.goals.isEmpty()) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }
        loadJob = viewModelScope.launch {
            try {
                combine(
                    savingRepository.getAllSavingGoals(),
                    accountRepository.getAllAccounts(),
                    savingRepository.getAllContributions()
                ) { goals, accounts, contributions ->
                    val activeGoals = goals.filter { !it.isArchived }
                    val insights = buildDynamicInsights(activeGoals, contributions)
                    SavingsSnapshot(
                        goals = goals,
                        accounts = accounts,
                        contributions = contributions,
                        insights = insights
                    )
                }
                    .flowOn(Dispatchers.Default)
                    .collect { snapshot ->
                        _uiState.update { current ->
                            val refreshedSelected = current.selectedGoal?.let { sel ->
                                snapshot.goals.find { it.id == sel.id }
                            }
                            val refreshedHistory = refreshedSelected?.let { sel ->
                                snapshot.contributions
                                    .filter { it.savingGoalId == sel.id }
                                    .sortedByDescending { it.date }
                            } ?: emptyList()

                            current.copy(
                                goals = snapshot.goals,
                                accounts = snapshot.accounts,
                                contributions = snapshot.contributions,
                                insights = snapshot.insights,
                                selectedGoal = refreshedSelected,
                                selectedGoalHistory = refreshedHistory,
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

    private fun buildDynamicInsights(
        goals: List<SavingGoal>,
        contributions: List<SavingsContribution>
    ): List<SavingsInsight> {
        val list = mutableListOf<SavingsInsight>()
        if (goals.isEmpty()) {
            list.add(SavingsInsight("لم تفعل أي خطط للادخار بعد، ابدأ اليوم لتأمين مستقبلك المالي.", false, "info"))
        } else {
            val totalSaved = goals.sumOf { it.currentAmount }
            val totalTarget = goals.sumOf { it.targetAmount }
            val percentage = if (totalTarget > 0) (totalSaved / totalTarget * 100).toInt() else 0

            list.add(SavingsInsight("أحرزت تقدماً بنسبة $percentage% إجمالياً نحو أهدافك الادخارية النشطة.", true, "trending_up"))

            val closest = goals.filter { !it.isCompleted && !it.isPaused }
                .maxByOrNull { if (it.targetAmount > 0) it.currentAmount / it.targetAmount else 0.0 }
            if (closest != null && closest.targetAmount > 0 && closest.currentAmount > 0) {
                val pct = (closest.currentAmount / closest.targetAmount * 100).toInt()
                list.add(SavingsInsight("أنت قريب من تحقيق هدف '${closest.name}' بنسبة $pct%. واصل التقدم!", true, "insights"))
            }

            val emptyGoal = goals.firstOrNull { g ->
                !g.isCompleted && !g.isPaused && contributions.none { it.savingGoalId == g.id }
            }
            if (emptyGoal != null) {
                list.add(SavingsInsight("الهدف '${emptyGoal.name}' لم تسجل فيه أي مساهمة بعد، خصص له دفعة أولى.", false, "warning"))
            }
        }
        return list
    }

    fun addSavingGoal(
        name: String,
        targetAmount: Double,
        deadline: Long?,
        accountId: Long,
        color: String,
        strategy: String = "manual"
    ) {
        viewModelScope.launch {
            val goal = SavingGoal(
                name = name,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                deadline = deadline,
                accountId = accountId,
                icon = "savings",
                color = color,
                isCompleted = false,
                isPaused = false,
                isArchived = false,
                strategy = strategy
            )
            savingRepository.insertSavingGoal(goal)
        }
    }

    fun editSavingGoal(
        goalId: Long,
        name: String,
        targetAmount: Double,
        deadline: Long?,
        accountId: Long,
        color: String,
        strategy: String
    ) {
        viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId)
            if (goal != null) {
                val updated = goal.copy(
                    name = name,
                    targetAmount = targetAmount,
                    deadline = deadline,
                    accountId = accountId,
                    color = color,
                    strategy = strategy,
                    isCompleted = goal.currentAmount >= targetAmount && targetAmount > 0
                )
                savingRepository.updateSavingGoal(updated)
                if (_uiState.value.selectedGoal?.id == goalId) {
                    selectGoal(goalId)
                }
            }
        }
    }

    fun addDeposit(goalId: Long, accountId: Long, amount: Double, note: String?, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            addSavingsContributionUseCase(goalId, accountId, amount, note, date)
            if (_uiState.value.selectedGoal?.id == goalId) {
                selectGoal(goalId)
            }
        }
    }

    fun addWithdrawal(goalId: Long, accountId: Long, amount: Double, note: String?, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            withdrawFromSavingsUseCase(goalId, accountId, amount, note, date)
            if (_uiState.value.selectedGoal?.id == goalId) {
                selectGoal(goalId)
            }
        }
    }

    fun togglePauseGoal(goalId: Long) {
        viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId) ?: return@launch
            savingRepository.updateSavingGoal(goal.copy(isPaused = !goal.isPaused))
        }
    }

    fun isGoalPaused(goalId: Long): Boolean {
        return _uiState.value.goals.find { it.id == goalId }?.isPaused == true
    }

    fun archiveGoal(goalId: Long) {
        viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId) ?: return@launch
            savingRepository.updateSavingGoal(goal.copy(isArchived = !goal.isArchived))
        }
    }

    fun isGoalArchived(goalId: Long): Boolean {
        return _uiState.value.goals.find { it.id == goalId }?.isArchived == true
    }

    fun getGoalStrategy(goalId: Long): String {
        return _uiState.value.goals.find { it.id == goalId }?.strategy ?: "manual"
    }

    fun updateGoalStrategy(goalId: Long, strategy: String) {
        viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId) ?: return@launch
            savingRepository.updateSavingGoal(goal.copy(strategy = strategy))
        }
    }

    fun markGoalCompleted(goalId: Long) {
        viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId)
            if (goal != null) {
                savingRepository.updateSavingGoal(goal.copy(isCompleted = true))
            }
        }
    }

    fun deleteGoal(goalId: Long, refundToWallet: Boolean = true) {
        viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId)
            if (goal != null) {
                if (refundToWallet) {
                    val goalContribs = savingRepository.getContributionsForGoal(goalId).first()
                    val linkedIds = goalContribs.mapNotNull { it.linkedTransactionId }
                    if (linkedIds.isNotEmpty()) {
                        linkedIds.forEach { txId ->
                            transactionRepository.deleteTransactionById(txId)
                        }
                    } else if (goal.currentAmount > 0.0) {
                        val targetAccount = accountRepository.getAccountById(goal.accountId)
                            ?: _uiState.value.accounts.firstOrNull()
                        if (targetAccount != null) {
                            transactionRepository.insertTransaction(
                                Transaction(
                                    amount = goal.currentAmount,
                                    type = TransactionType.INCOME,
                                    categoryId = null,
                                    accountId = targetAccount.id,
                                    note = "استرداد رصيد هدف ادخار محذوف: ${goal.name}",
                                    date = System.currentTimeMillis(),
                                    occurredAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                savingRepository.deleteContributionsForGoal(goalId)
                savingRepository.deleteSavingGoal(goal)
                if (_uiState.value.selectedGoal?.id == goalId) {
                    _uiState.update { it.copy(selectedGoal = null, selectedGoalHistory = emptyList()) }
                }
            }
        }
    }

    fun selectGoal(goalId: Long) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId)
            if (goal != null) {
                val history = savingRepository.getContributionsForGoal(goalId).first()
                    .sortedByDescending { it.date }
                val forecast = getSavingsForecastUseCase(goalId)
                _uiState.update {
                    it.copy(
                        selectedGoal = goal,
                        selectedGoalHistory = history,
                        forecastText = forecast
                    )
                }
            }
        }
    }
}
