package com.example.domain.usecase.budget

import com.example.domain.model.BudgetGoal
import com.example.domain.model.BudgetType
import com.example.domain.model.TransactionType
import com.example.domain.repository.BudgetGoalRepository
import com.example.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

class CalculateBudgetSpentUseCase(
    private val transactionRepository: TransactionRepository,
    private val budgetGoalRepository: BudgetGoalRepository
) {
    suspend operator fun invoke(budgetGoal: BudgetGoal): Double {
        val allTx = transactionRepository.getAllTransactions().first()
        val expenseTx = allTx.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
            tx.date in budgetGoal.startDate..budgetGoal.endDate
        }

        val filteredTx = when (budgetGoal.budgetType) {
            BudgetType.CATEGORY -> {
                if (budgetGoal.linkedCategoryId != null) {
                    expenseTx.filter { it.categoryId == budgetGoal.linkedCategoryId }
                } else {
                    expenseTx
                }
            }
            else -> expenseTx // GLOBAL or CUSTOM
        }

        val sum = filteredTx.sumOf { it.amount }
        if (sum != budgetGoal.spentAmount) {
            budgetGoalRepository.updateSpentAmount(budgetGoal.id, sum)
        }
        return sum
    }
}
