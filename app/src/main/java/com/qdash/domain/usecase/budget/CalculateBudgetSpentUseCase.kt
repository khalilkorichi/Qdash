package com.qdash.domain.usecase.budget

import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.model.BudgetType
import com.qdash.domain.model.TransactionType
import com.qdash.domain.repository.BudgetGoalRepository
import com.qdash.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

class CalculateBudgetSpentUseCase(
    private val transactionRepository: TransactionRepository,
    @Suppress("unused") private val budgetGoalRepository: BudgetGoalRepository
) {
    suspend operator fun invoke(budgetGoal: BudgetGoal): Double {
        val txs = transactionRepository.getTransactionsByDateRange(
            budgetGoal.startDate,
            budgetGoal.endDate
        ).first()
        val expenseTx = txs.filter { tx ->
            tx.type == TransactionType.EXPENSE && tx.kind == com.qdash.domain.model.TransactionKind.EXPENSE
        }
        val filteredTx = when (budgetGoal.budgetType) {
            BudgetType.CATEGORY -> {
                if (budgetGoal.linkedCategoryId != null) {
                    expenseTx.filter { it.categoryId == budgetGoal.linkedCategoryId }
                } else {
                    expenseTx
                }
            }
            else -> expenseTx
        }
        return filteredTx.sumOf { it.amount }
    }
}
