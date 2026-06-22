package com.example.domain.usecase.ai

import com.example.domain.model.*
import com.example.domain.repository.*
import com.example.core.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GetRecentActivitySummaryUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(limit: Int = 3): RecentActivitySummary {
        val list = transactionRepository.getRecentTransactions(limit).first()
        return RecentActivitySummary(transactions = list)
    }
}

class GetWalletDistributionUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(): WalletDistributionSuggestion {
        val accounts = accountRepository.getAllAccounts().first().filter { !it.isArchived }
        val totalBalance = accounts.sumOf { it.balance }
        
        val items = accounts.map { acc ->
            val typeLabel = when (acc.type) {
                AccountType.BANK -> "بنك"
                AccountType.CCP -> "CCP"
                AccountType.BARIDIMOB -> "بريدي موب"
                AccountType.CASH -> "نقداً"
                AccountType.SAVINGS -> "ادخار"
                AccountType.WALLET -> "محفظة"
                AccountType.OTHER -> "أخرى"
            }
            
            val suggestedPercentage = if (accounts.isNotEmpty()) 100.0 / accounts.size else 0.0
            val suggestedBalance = if (accounts.isNotEmpty()) totalBalance / accounts.size else 0.0
            
            AccountDistributionItem(
                accountId = acc.id,
                accountName = acc.name,
                typeLabel = typeLabel,
                currentBalance = acc.balance,
                currency = acc.currency,
                color = acc.color,
                suggestedPercentage = suggestedPercentage,
                suggestedBalance = suggestedBalance
            )
        }
        
        return WalletDistributionSuggestion(
            totalBalance = totalBalance,
            currency = accounts.firstOrNull()?.currency ?: "دج",
            items = items
        )
    }
}

class EvaluateLowBalanceAlertsUseCase(
    private val accountRepository: AccountRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(customLimit: Double? = null): LowBalanceAlertState {
        val accounts = accountRepository.getAllAccounts().first().filter { !it.isArchived }
        val limitToUse = customLimit ?: preferencesManager.lowBalanceLimit
        
        val lowBalanceAccounts = accounts.filter { it.balance < limitToUse }.map { acc ->
            val typeLabel = when (acc.type) {
                AccountType.BANK -> "بنك"
                AccountType.CCP -> "CCP"
                AccountType.BARIDIMOB -> "بريدي موب"
                AccountType.CASH -> "نقداً"
                AccountType.SAVINGS -> "ادخار"
                AccountType.WALLET -> "محفظة"
                AccountType.OTHER -> "أخرى"
            }
            
            LowBalanceAccountAlert(
                accountId = acc.id,
                accountName = acc.name,
                typeLabel = typeLabel,
                currentBalance = acc.balance,
                limit = limitToUse,
                currency = acc.currency,
                color = acc.color
            )
        }
        
        return LowBalanceAlertState(
            limit = limitToUse,
            accountsUnderLimit = lowBalanceAccounts
        )
    }
}

class GetQuickImpactPreviewUseCase(
    private val transactionRepository: TransactionRepository,
    private val budgetGoalRepository: BudgetGoalRepository,
    private val savingRepository: SavingRepository,
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(amount: Double, type: TransactionType, categoryId: Long? = null, accountId: Long? = null): QuickImpactPreviewState {
        var budgetLimit: Double? = null
        var budgetSpentBefore = 0.0
        var budgetSpentAfter = 0.0
        
        if (type == TransactionType.EXPENSE && categoryId != null) {
            val budgets = budgetGoalRepository.getAllBudgetGoals().first()
            val matchedBudget = budgets.firstOrNull { it.linkedCategoryId == categoryId }
            if (matchedBudget != null) {
                budgetLimit = matchedBudget.amountLimit
                budgetSpentBefore = matchedBudget.spentAmount
                budgetSpentAfter = matchedBudget.spentAmount + amount
            } else {
                val transactions = transactionRepository.getAllTransactions().first()
                val monthlySpent = transactions
                    .filter { it.type == TransactionType.EXPENSE && isCurrentMonth(it.date) }
                    .sumOf { it.amount }
                budgetSpentBefore = monthlySpent
                budgetSpentAfter = monthlySpent + amount
            }
        }
        
        var goalName: String? = null
        var goalTarget = 0.0
        var goalSavedBefore = 0.0
        var goalSavedAfter = 0.0
        
        if (accountId != null) {
            val goals = savingRepository.getAllSavingGoals().first()
            val matchedGoal = goals.firstOrNull { it.accountId == accountId && !it.isCompleted }
            if (matchedGoal != null) {
                goalName = matchedGoal.name
                goalTarget = matchedGoal.targetAmount
                goalSavedBefore = matchedGoal.currentAmount
                goalSavedAfter = if (type == TransactionType.INCOME || type == TransactionType.TRANSFER) {
                    matchedGoal.currentAmount + amount
                } else {
                    matchedGoal.currentAmount - amount
                }
            }
        }
        
        var debtName: String? = null
        var debtTotal = 0.0
        var debtRemainingBefore = 0.0
        var debtRemainingAfter = 0.0
        
        val debts = debtRepository.getAllDebts().first().filter { !it.isClosed }
        val matchedDebt = debts.firstOrNull()
        if (matchedDebt != null) {
            debtName = matchedDebt.title
            debtTotal = matchedDebt.totalAmount
            debtRemainingBefore = matchedDebt.remainingAmount
            if (type == TransactionType.EXPENSE) {
                debtRemainingAfter = matchedDebt.remainingAmount + amount
            } else if (type == TransactionType.INCOME) {
                debtRemainingAfter = maxOf(0.0, matchedDebt.remainingAmount - amount)
            } else {
                debtRemainingAfter = matchedDebt.remainingAmount
            }
        }
        
        return QuickImpactPreviewState(
            amount = amount,
            type = type,
            budgetLimit = budgetLimit,
            budgetSpentBefore = budgetSpentBefore,
            budgetSpentAfter = budgetSpentAfter,
            goalName = goalName,
            goalTarget = goalTarget,
            goalSavedBefore = goalSavedBefore,
            goalSavedAfter = goalSavedAfter,
            debtName = debtName,
            debtTotal = debtTotal,
            debtRemainingBefore = debtRemainingBefore,
            debtRemainingAfter = debtRemainingAfter
        )
    }
    
    private fun isCurrentMonth(date: Long): Boolean {
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        val currentYear = cal.get(java.util.Calendar.YEAR)
        cal.timeInMillis = date
        return cal.get(java.util.Calendar.MONTH) == currentMonth && cal.get(java.util.Calendar.YEAR) == currentYear
    }
}
