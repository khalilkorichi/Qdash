package com.qdash.domain.model

import com.qdash.domain.model.common.*

data class AccountDistributionItem(
    override val /* contract */ accountId: Long,
    val accountName: String,
    val typeLabel: String,
    val currentBalance: Double,
    val currency: String,
    override val /* contract */ color: String,
    val suggestedPercentage: Double,
    val suggestedBalance: Double
) : AccountLinked, ColorTagged

data class WalletDistributionSuggestion(
    val totalBalance: Double,
    val currency: String = "دج",
    val items: List<AccountDistributionItem>
)

data class RecentActivitySummary(
    val transactions: List<Transaction>
)

data class LowBalanceAccountAlert(
    override val /* contract */ accountId: Long,
    val accountName: String,
    val typeLabel: String,
    val currentBalance: Double,
    val limit: Double,
    val currency: String,
    override val /* contract */ color: String
) : AccountLinked, ColorTagged

data class LowBalanceAlertState(
    val limit: Double,
    val accountsUnderLimit: List<LowBalanceAccountAlert>
)

data class TransferDraftState(
    override val /* contract */ amount: Double,
    val fromAccountId: Long,
    val toAccountId: Long,
    override val /* contract */ note: String? = null,
    val fromAccountName: String = "غير محدد",
    val toAccountName: String = "غير محدد",
    val isConfirmed: Boolean = false,
    val isCancelled: Boolean = false
) : AmountHolder, Notable

data class SelectedAccountDetailsState(
    val account: Account,
    val recentTransactions: List<Transaction>,
    val activeGoals: List<SavingGoal>
)

data class QuickImpactPreviewState(
    override val /* contract */ amount: Double,
    override val /* contract */ type: TransactionType,
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
) : AmountHolder, TypeHolder<TransactionType>
