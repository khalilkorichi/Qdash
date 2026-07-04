package com.qdash.presentation.budgetgoals

import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.domain.usecase.budget.BudgetAlert

data class BudgetGoalsUiState(
    val budgets: List<BudgetGoal> = emptyList(),
    val categories: List<Category> = emptyList(),
    val alerts: List<BudgetAlert> = emptyList(),
    val selectedBudget: BudgetGoal? = null,
    val selectedBudgetTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)
