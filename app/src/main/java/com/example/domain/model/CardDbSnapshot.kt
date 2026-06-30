package com.example.domain.model

data class CardDbSnapshot(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val budgets: List<BudgetGoal>,
    val savings: List<SavingGoal>,
    val categories: List<Category>
)
