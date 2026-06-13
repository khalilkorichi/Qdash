package com.example.presentation.budgetgoals

import com.example.domain.model.BudgetGoal
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.usecase.budget.BudgetAlert

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
