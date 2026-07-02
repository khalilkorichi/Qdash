package com.example.domain.repository

import com.example.domain.model.*
import com.example.domain.usecase.transaction.BulkEditParams
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteTransactionById(id: Long)
    suspend fun isTransactionAlreadyInserted(type: TransactionType, note: String, startDate: Long, endDate: Long): Boolean
    fun getDailyFinancialAggregatesForRange(startDate: Long, endDate: Long): Flow<List<com.example.data.local.entities.DailyFinancialAggregateEntity>>
    suspend fun deleteTransactionsBulk(ids: List<Long>)
    suspend fun updateTransactionsCategoryBulk(ids: List<Long>, newCategoryId: Long)
    suspend fun bulkEditTransactions(params: BulkEditParams): Result<Int>
    suspend fun getPeakTransactionHours(): List<Int>
}

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun getArchivedAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    suspend fun archiveAccount(id: Long)
    suspend fun unarchiveAccount(id: Long)
    suspend fun setDefaultAccount(id: Long)
    suspend fun getTransactionCountForAccount(id: Long): Int
}

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getRootCategories(): Flow<List<Category>>
    fun getSubcategories(parentId: Long): Flow<List<Category>>
    fun searchCategories(query: String): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun getTransactionCountForCategory(id: Long): Int
    suspend fun insertCategory(category: Category): Long
    suspend fun insertCategories(categories: List<Category>)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun deleteSubcategoriesForParent(parentId: Long)
    suspend fun mergeCategories(sourceCategoryId: Long, targetCategoryId: Long)
}

interface IncomeRepository {
    fun getAllIncomeSources(): Flow<List<IncomeSource>>
    fun getActiveIncomeSources(): Flow<List<IncomeSource>>
    suspend fun getIncomeSourceById(id: Long): IncomeSource?
    suspend fun insertIncomeSource(incomeSource: IncomeSource): Long
    suspend fun updateIncomeSource(incomeSource: IncomeSource)
    suspend fun deleteIncomeSource(incomeSource: IncomeSource)
    
    fun getSalaryDelays(salaryId: Long): Flow<List<SalaryDelay>>
    suspend fun confirmSalaryDelay(
        salaryId: Long,
        delayDays: Int,
        originalDate: Long,
        newDate: Long,
        severityScore: Int,
        affectedObligations: List<AffectedObligation>
    )

    suspend fun deleteSalaryDelay(delayId: Long)

    suspend fun updateSalaryDelay(
        delayId: Long,
        newDelayDays: Int,
        newDate: Long,
        newSeverityScore: Int,
        affectedObligations: List<AffectedObligation>
    )
}

interface SavingRepository {
    fun getAllSavingGoals(): Flow<List<SavingGoal>>
    suspend fun getSavingGoalById(id: Long): SavingGoal?
    suspend fun insertSavingGoal(savingGoal: SavingGoal): Long
    suspend fun updateSavingGoal(savingGoal: SavingGoal)
    suspend fun deleteSavingGoal(savingGoal: SavingGoal)
    
    fun getAllContributions(): Flow<List<SavingsContribution>>
    fun getContributionsForGoal(goalId: Long): Flow<List<SavingsContribution>>
    suspend fun insertContribution(contribution: SavingsContribution): Long
    suspend fun deleteContribution(contribution: SavingsContribution)
    suspend fun deleteContributionsForGoal(goalId: Long)
}

interface SubscriptionRepository {
    fun getAllSubscriptions(): Flow<List<Subscription>>
    fun getActiveSubscriptions(): Flow<List<Subscription>>
    suspend fun getSubscriptionById(id: Long): Subscription?
    suspend fun insertSubscription(subscription: Subscription): Long
    suspend fun updateSubscription(subscription: Subscription)
    suspend fun deleteSubscription(subscription: Subscription)
}

interface SalaryDistributionRepository {
    fun getDistributionForSalary(salaryId: Long): Flow<SalaryDistribution?>
    suspend fun getDistributionForSalaryOnce(salaryId: Long): SalaryDistribution?
    fun getEnvelopesForDistribution(distributionId: Long): Flow<List<SalaryEnvelope>>
    suspend fun getEnvelopesForDistributionOnce(distributionId: Long): List<SalaryEnvelope>
    suspend fun insertDistribution(distribution: SalaryDistribution): Long
    suspend fun updateDistribution(distribution: SalaryDistribution)
    suspend fun saveEnvelopes(envelopes: List<SalaryEnvelope>)
    suspend fun updateLinkedCategories(envelopeId: Long, categoryIds: List<Long>)
}

