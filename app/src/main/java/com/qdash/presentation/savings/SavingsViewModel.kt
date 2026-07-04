package com.qdash.presentation.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import com.qdash.domain.usecase.savings.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SavingsUiState(
    val goals: List<SavingGoal> = emptyList(),
    val contributions: List<SavingsContribution> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val insights: List<SavingsInsight> = emptyList(),
    val selectedGoal: SavingGoal? = null,
    val selectedGoalHistory: List<SavingsContribution> = emptyList(),
    val forecastText: String = "",
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
    private val getSavingsInsightsUseCase = GetSavingsInsightsUseCase(savingRepository)
    private val getSavingsForecastUseCase = GetSavingsForecastUseCase(savingRepository)
    private val getSavingsHistoryUseCase = GetSavingsHistoryUseCase(savingRepository)

    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState.asStateFlow()

    // Keep track of paused or archived Goals in memory for premium UX demonstration
    private val pausedGoalIds = MutableStateFlow<Set<Long>>(emptySet())
    private val archivedGoalIds = MutableStateFlow<Set<Long>>(emptySet())
    private val goalStrategies = MutableStateFlow<Map<Long, String>>(emptyMap()) // "monthly", "weekly", "manual", "leftover"

    init {
        loadSavingsData()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadSavingsData()
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadSavingsData() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        loadJob = viewModelScope.launch {
            try {
                combine(
                    savingRepository.getAllSavingGoals(),
                    accountRepository.getAllAccounts(),
                    savingRepository.getAllContributions(),
                    getSavingsInsightsUseCase()
                ) { goals, accounts, contributions, insights ->
                    _uiState.value.copy(
                        goals = goals,
                        accounts = accounts,
                        contributions = contributions,
                        insights = insights,
                        isLoading = false
                    )
                }
                .flowOn(kotlinx.coroutines.Dispatchers.Default)
                .collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun addSavingGoal(name: String, targetAmount: Double, deadline: Long?, accountId: Long, color: String, strategy: String = "manual") {
        viewModelScope.launch {
            val goal = SavingGoal(
                name = name,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                deadline = deadline,
                accountId = accountId,
                icon = "savings",
                color = color
            )
            val id = savingRepository.insertSavingGoal(goal)
            if (id > 0) {
                goalStrategies.update { it + (id to strategy) }
            }
        }
    }

    fun editSavingGoal(goalId: Long, name: String, targetAmount: Double, deadline: Long?, accountId: Long, color: String, strategy: String) {
        viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId)
            if (goal != null) {
                val updated = goal.copy(
                    name = name,
                    targetAmount = targetAmount,
                    deadline = deadline,
                    accountId = accountId,
                    color = color
                )
                savingRepository.updateSavingGoal(updated)
                goalStrategies.update { it + (goalId to strategy) }
            }
        }
    }

    fun addDeposit(goalId: Long, accountId: Long, amount: Double, note: String?, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            addSavingsContributionUseCase(goalId, accountId, amount, note, date)
            // Reload details if active
            if (_uiState.value.selectedGoal?.id == goalId) {
                selectGoal(goalId)
            }
        }
    }

    fun addWithdrawal(goalId: Long, accountId: Long, amount: Double, note: String?, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            withdrawFromSavingsUseCase(goalId, accountId, amount, note, date)
            // Reload details if active
            if (_uiState.value.selectedGoal?.id == goalId) {
                selectGoal(goalId)
            }
        }
    }

    fun togglePauseGoal(goalId: Long) {
        pausedGoalIds.update {
            if (it.contains(goalId)) it - goalId else it + goalId
        }
    }

    fun isGoalPaused(goalId: Long): Boolean {
        return pausedGoalIds.value.contains(goalId)
    }

    fun archiveGoal(goalId: Long) {
        archivedGoalIds.update { it + goalId }
    }

    fun isGoalArchived(goalId: Long): Boolean {
        return archivedGoalIds.value.contains(goalId)
    }

    fun getGoalStrategy(goalId: Long): String {
        return goalStrategies.value[goalId] ?: "manual"
    }

    fun updateGoalStrategy(goalId: Long, strategy: String) {
        goalStrategies.update { it + (goalId to strategy) }
    }

    fun markGoalCompleted(goalId: Long) {
        viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId)
            if (goal != null) {
                savingRepository.updateSavingGoal(goal.copy(currentAmount = goal.targetAmount, isCompleted = true))
            }
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId)
            if (goal != null) {
                savingRepository.deleteSavingGoal(goal)
                savingRepository.deleteContributionsForGoal(goalId)
                if (_uiState.value.selectedGoal?.id == goalId) {
                    _uiState.update { it.copy(selectedGoal = null, selectedGoalHistory = emptyList()) }
                }
            }
        }
    }

    private var detailsJob: kotlinx.coroutines.Job? = null

    fun selectGoal(goalId: Long) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            val goal = savingRepository.getSavingGoalById(goalId)
            if (goal != null) {
                val history = savingRepository.getContributionsForGoal(goalId).first()
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
