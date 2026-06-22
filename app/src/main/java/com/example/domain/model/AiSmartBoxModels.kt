package com.example.domain.model

data class AccountDistributionItem(
    val accountId: Long,
    val accountName: String,
    val typeLabel: String,
    val currentBalance: Double,
    val currency: String,
    val color: String,
    val suggestedPercentage: Double,
    val suggestedBalance: Double
)

data class WalletDistributionSuggestion(
    val totalBalance: Double,
    val currency: String = "دج",
    val items: List<AccountDistributionItem>
)

data class RecentActivitySummary(
    val transactions: List<Transaction>
)

data class LowBalanceAccountAlert(
    val accountId: Long,
    val accountName: String,
    val typeLabel: String,
    val currentBalance: Double,
    val limit: Double,
    val currency: String,
    val color: String
)

data class LowBalanceAlertState(
    val limit: Double,
    val accountsUnderLimit: List<LowBalanceAccountAlert>
)

data class TransferDraftState(
    val amount: Double,
    val fromAccountId: Long,
    val toAccountId: Long,
    val note: String? = null,
    val fromAccountName: String = "غير محدد",
    val toAccountName: String = "غير محدد",
    val isConfirmed: Boolean = false,
    val isCancelled: Boolean = false
)

data class SelectedAccountDetailsState(
    val account: Account,
    val recentTransactions: List<Transaction>,
    val activeGoals: List<SavingGoal>
)

data class QuickImpactPreviewState(
    val amount: Double,
    val type: TransactionType,
    // Budget
    val budgetLimit: Double? = null,
    val budgetSpentBefore: Double = 0.0,
    val budgetSpentAfter: Double = 0.0,
    // Savings Goal
    val goalName: String? = null,
    val goalTarget: Double = 0.0,
    val goalSavedBefore: Double = 0.0,
    val goalSavedAfter: Double = 0.0,
    // Debt
    val debtName: String? = null,
    val debtTotal: Double = 0.0,
    val debtRemainingBefore: Double = 0.0,
    val debtRemainingAfter: Double = 0.0
)
