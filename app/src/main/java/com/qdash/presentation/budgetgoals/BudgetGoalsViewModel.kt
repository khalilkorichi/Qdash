package com.qdash.presentation.budgetgoals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.model.BudgetType
import com.qdash.domain.model.TransactionType
import com.qdash.domain.repository.CategoryRepository
import com.qdash.domain.repository.TransactionRepository
import com.qdash.domain.usecase.budget.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BudgetGoalsViewModel(
    private val getBudgetGoalsUseCase: GetBudgetGoalsUseCase,
    private val addBudgetGoalUseCase: AddBudgetGoalUseCase,
    private val updateBudgetGoalUseCase: UpdateBudgetGoalUseCase,
    private val deleteBudgetGoalUseCase: DeleteBudgetGoalUseCase,
    private val getBudgetAlertsUseCase: GetBudgetAlertsUseCase,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetGoalsUiState())
    val uiState: StateFlow<BudgetGoalsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadData()
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private var categoriesJob: kotlinx.coroutines.Job? = null
    private var budgetGoalsJob: kotlinx.coroutines.Job? = null

    private fun loadData() {
        categoriesJob?.cancel()
        budgetGoalsJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        categoriesJob = viewModelScope.launch {
            // Load categories first
            try {
                categoryRepository.getAllCategories().collectLatest { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

                    // Refresh active selection details if viewing details
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
            updateBudgetGoalUseCase(budgetGoal)
        }
    }

    fun toggleBudgetArchive(budgetGoal: BudgetGoal) {
        viewModelScope.launch {
            updateBudgetGoalUseCase(budgetGoal.copy(isActive = !budgetGoal.isActive))
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

    private var detailsJob: kotlinx.coroutines.Job? = null

    fun loadBudgetDetails(budgetGoal: BudgetGoal) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            transactionRepository.getAllTransactions().collectLatest { allTransactions ->
                val filtered = allTransactions.filter { tx ->
                    tx.type == TransactionType.EXPENSE &&
                    tx.date in budgetGoal.startDate..budgetGoal.endDate &&
                    (budgetGoal.budgetType != BudgetType.CATEGORY || budgetGoal.linkedCategoryId == null || tx.categoryId == budgetGoal.linkedCategoryId)
                }
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
