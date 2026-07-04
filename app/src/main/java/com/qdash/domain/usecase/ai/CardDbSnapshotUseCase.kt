package com.qdash.domain.usecase.ai

import com.qdash.domain.model.CardDbSnapshot
import com.qdash.domain.model.CardAiContextType
import com.qdash.domain.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class CardDbSnapshotUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetGoalRepository: BudgetGoalRepository,
    private val savingRepository: SavingRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        periodStart: Long,
        periodEnd: Long,
        cardType: CardAiContextType
    ): CardDbSnapshot = withContext(Dispatchers.IO) {
        val txs = transactionRepository.getAllTransactions().first()
            .filter { it.date in periodStart..periodEnd }
        val accounts = accountRepository.getAllAccounts().first()
        val budgets = budgetGoalRepository.getAllBudgetGoals().first()
        val savings = savingRepository.getAllSavingGoals().first()
        val categories = categoryRepository.getAllCategories().first()
        
        CardDbSnapshot(
            transactions = txs,
            accounts = accounts,
            budgets = budgets,
            savings = savings,
            categories = categories
        )
    }
}
