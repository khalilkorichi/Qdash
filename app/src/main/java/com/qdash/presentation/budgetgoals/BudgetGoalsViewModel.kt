package com.qdash.presentation.budgetgoals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.model.BudgetType
import com.qdash.domain.model.TransactionType
import com.qdash.domain.repository.AccountRepository
import com.qdash.domain.repository.CategoryRepository
import com.qdash.domain.repository.TransactionRepository
import com.qdash.domain.usecase.budget.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BudgetGoalsViewModel(
    private val getBudgetGoalsUseCase: GetBudgetGoalsUseCase,
    private val addBudgetGoalUseCase: AddBudgetGoalUseCase,
    private val updateBudgetGoalUseCase: UpdateBudgetGoalUseCase,
    private val deleteBudgetGoalUseCase: DeleteBudgetGoalUseCase,
    private val getBudgetAlertsUseCase: GetBudgetAlertsUseCase,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetGoalsUiState())
    val uiState: StateFlow<BudgetGoalsUiState> = _uiState.asStateFlow()

    private var categoriesJob: Job? = null
    private var accountsJob: Job? = null
    private var budgetGoalsJob: Job? = null
    private var detailsJob: Job? = null

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadData(showLoading = false)
            kotlinx.coroutines.delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadData(showLoading: Boolean = true) {
        categoriesJob?.cancel()
        accountsJob?.cancel()
        budgetGoalsJob?.cancel()
        if (showLoading && _uiState.value.budgets.isEmpty()) {
            _uiState.update { it.copy(isLoading = true) }
        }

        categoriesJob = viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().collectLatest { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
            } catch (_: Exception) {
            }
        }

        if (accountRepository != null) {
            accountsJob = viewModelScope.launch {
                try {
                    accountRepository.getAllAccounts().collectLatest { accounts ->
                        _uiState.update { it.copy(accounts = accounts) }
                    }
                } catch (_: Exception) {
                }
            }
        }

        budgetGoalsJob = viewModelScope.launch {
            try {
                getBudgetGoalsUseCase().collectLatest { budgets ->
                    val alerts = getBudgetAlertsUseCase(budgets)
                    _uiState.update {
                        it.copy(
                            budgets = budgets,
                            alerts = alerts,
                            isLoading = false
                        )
                    }

                    val currentSelected = _uiState.value.selectedBudget
                    if (currentSelected != null) {
                        val refreshedSelected = budgets.find { it.id == currentSelected.id }
                        if (refreshedSelected != null) {
                            loadBudgetDetails(refreshedSelected)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun addBudgetGoal(
        title: String,
        linkedCategoryId: Long?,
        budgetType: BudgetType,
        amountLimit: Double,
        startDate: Long,
        endDate: Long,
        alertThresholdPercent: Int = 80,
        color: String,
        icon: String
    ) {
        viewModelScope.launch {
            val goal = BudgetGoal(
                title = title,
                linkedCategoryId = if (budgetType == BudgetType.CATEGORY) linkedCategoryId else null,
                budgetType = budgetType,
                amountLimit = amountLimit,
                startDate = startDate,
                endDate = endDate,
                alertThresholdPercent = alertThresholdPercent,
                isActive = true,
                color = color,
                icon = icon
            )
            addBudgetGoalUseCase(goal)
        }
    }

    fun updateBudgetGoal(budgetGoal: BudgetGoal) {
        viewModelScope.launch {
            val normalized = budgetGoal.copy(
                linkedCategoryId = if (budgetGoal.budgetType == BudgetType.CATEGORY) budgetGoal.linkedCategoryId else null
            )
            updateBudgetGoalUseCase(normalized)
            if (_uiState.value.selectedBudget?.id == normalized.id) {
                loadBudgetDetails(normalized)
            }
        }
    }

    fun toggleBudgetArchive(budgetGoal: BudgetGoal) {
        viewModelScope.launch {
            val updated = budgetGoal.copy(isActive = !budgetGoal.isActive)
            updateBudgetGoalUseCase(updated)
            if (_uiState.value.selectedBudget?.id == updated.id) {
                _uiState.update { it.copy(selectedBudget = updated) }
            }
        }
    }

    fun deleteBudgetGoal(budgetGoal: BudgetGoal) {
        viewModelScope.launch {
            deleteBudgetGoalUseCase(budgetGoal)
            if (_uiState.value.selectedBudget?.id == budgetGoal.id) {
                _uiState.update { it.copy(selectedBudget = null, selectedBudgetTransactions = emptyList()) }
            }
        }
    }

    fun selectBudgetGoal(budgetGoal: BudgetGoal) {
        _uiState.update { it.copy(selectedBudget = budgetGoal) }
        loadBudgetDetails(budgetGoal)
    }

    fun loadBudgetDetails(budgetGoal: BudgetGoal) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            transactionRepository.getAllTransactions().collectLatest { allTransactions ->
                val targetCatIds: Set<Long> = if (budgetGoal.budgetType == BudgetType.CATEGORY && budgetGoal.linkedCategoryId != null) {
                    val subIds = _uiState.value.categories
                        .filter { it.parentId == budgetGoal.linkedCategoryId }
                        .map { it.id }
                    setOf(budgetGoal.linkedCategoryId) + subIds
                } else emptySet()

                val filtered = allTransactions.filter { tx ->
                    tx.type == TransactionType.EXPENSE &&
                            tx.date in budgetGoal.startDate..budgetGoal.endDate &&
                            (budgetGoal.budgetType != BudgetType.CATEGORY ||
                                    budgetGoal.linkedCategoryId == null ||
                                    tx.categoryId in targetCatIds)
                }.sortedByDescending { it.date }

                _uiState.update {
                    it.copy(
                        selectedBudget = budgetGoal,
                        selectedBudgetTransactions = filtered
                    )
                }
            }
        }
    }
}
