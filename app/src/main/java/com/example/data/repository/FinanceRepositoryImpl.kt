package com.example.data.repository

import com.example.data.local.dao.*
import com.example.data.local.entities.*
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.room.withTransaction

class TransactionRepositoryImpl(
    private val database: com.example.data.local.AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId).map { list -> list.map { it.toDomain() } }
    }

    override fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction): Long = database.withTransaction {
        val account = accountDao.getAccountById(transaction.accountId)
            ?: throw IllegalArgumentException("الحساب المالي المحدد غير موجود في قاعدة البيانات!")

        val offset = if (transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.TRANSFER) -transaction.amount else transaction.amount
        val updatedAccount = account.copy(balance = account.balance + offset)
        accountDao.updateAccount(updatedAccount)

        if (transaction.type == TransactionType.TRANSFER && transaction.toAccountId != null) {
            val destinationAccount = accountDao.getAccountById(transaction.toAccountId)
            if (destinationAccount != null) {
                val updatedDest = destinationAccount.copy(balance = destinationAccount.balance + transaction.amount)
                accountDao.updateAccount(updatedDest)
            }
        }

        val resultId = transactionDao.insertTransaction(transaction.toEntity())
        syncAggregateForDate(transaction.date)
        resultId
    }

    override suspend fun updateTransaction(transaction: Transaction) = database.withTransaction {
        val oldTx = transactionDao.getTransactionById(transaction.id)
        val oldDate = oldTx?.date
        if (oldTx != null) {
            val oldAccount = accountDao.getAccountById(oldTx.accountId)
            if (oldAccount != null) {
                val revertOffset = if (oldTx.type == TransactionType.EXPENSE.name || oldTx.type == TransactionType.TRANSFER.name) oldTx.amount else -oldTx.amount
                accountDao.updateAccount(oldAccount.copy(balance = oldAccount.balance + revertOffset))
            }

            if (oldTx.type == TransactionType.TRANSFER.name && oldTx.toAccountId != null) {
                val oldDest = accountDao.getAccountById(oldTx.toAccountId)
                if (oldDest != null) {
                    accountDao.updateAccount(oldDest.copy(balance = oldDest.balance - oldTx.amount))
                }
            }
        }

        val newAccount = accountDao.getAccountById(transaction.accountId)
            ?: throw IllegalArgumentException("الحساب المالي الجديد المحدد غير موجود!")

        val offset = if (transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.TRANSFER) -transaction.amount else transaction.amount
        accountDao.updateAccount(newAccount.copy(balance = newAccount.balance + offset))

        if (transaction.type == TransactionType.TRANSFER && transaction.toAccountId != null) {
            val newDest = accountDao.getAccountById(transaction.toAccountId)
            if (newDest != null) {
                accountDao.updateAccount(newDest.copy(balance = newDest.balance + transaction.amount))
            }
        }

        transactionDao.updateTransaction(transaction.toEntity())

        if (oldDate != null) {
            syncAggregateForDate(oldDate)
        }
        syncAggregateForDate(transaction.date)
    }

    override suspend fun deleteTransaction(transaction: Transaction) = database.withTransaction {
        val account = accountDao.getAccountById(transaction.accountId)
        if (account != null) {
            val revertOffset = if (transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.TRANSFER) transaction.amount else -transaction.amount
            accountDao.updateAccount(account.copy(balance = account.balance + revertOffset))
        }

        if (transaction.type == TransactionType.TRANSFER && transaction.toAccountId != null) {
            val destAccount = accountDao.getAccountById(transaction.toAccountId)
            if (destAccount != null) {
                accountDao.updateAccount(destAccount.copy(balance = destAccount.balance - transaction.amount))
            }
        }

        transactionDao.deleteTransaction(transaction.toEntity())
        syncAggregateForDate(transaction.date)
    }

    override suspend fun deleteTransactionById(id: Long) = database.withTransaction {
        val tx = transactionDao.getTransactionById(id)
        if (tx != null) {
            deleteTransaction(tx.toDomain())
        }
    }

    override suspend fun isTransactionAlreadyInserted(type: TransactionType, note: String, startDate: Long, endDate: Long): Boolean {
        return transactionDao.isTransactionAlreadyInserted(type.name, note, startDate, endDate)
    }

    override fun getDailyFinancialAggregatesForRange(startDate: Long, endDate: Long): Flow<List<DailyFinancialAggregateEntity>> {
        return database.dailyFinancialAggregateDao().getAggregatesForRange(startDate, endDate)
    }

    override suspend fun deleteTransactionsBulk(ids: List<Long>) = database.withTransaction {
        val transactions = transactionDao.getTransactionsByIds(ids)
        val affectedDates = mutableSetOf<Long>()
        transactions.forEach { tx ->
            val account = accountDao.getAccountById(tx.accountId)
            if (account != null) {
                val revertOffset = if (tx.type == TransactionType.EXPENSE.name || tx.type == TransactionType.TRANSFER.name) tx.amount else -tx.amount
                accountDao.updateAccount(account.copy(balance = account.balance + revertOffset))
            }

            if (tx.type == TransactionType.TRANSFER.name && tx.toAccountId != null) {
                val destAccount = accountDao.getAccountById(tx.toAccountId)
                if (destAccount != null) {
                    accountDao.updateAccount(destAccount.copy(balance = destAccount.balance - tx.amount))
                }
            }
            affectedDates.add(tx.date)
        }
        transactionDao.deleteTransactionsBulk(ids)
        affectedDates.forEach { syncAggregateForDate(it) }
    }

    override suspend fun updateTransactionsCategoryBulk(ids: List<Long>, newCategoryId: Long) = database.withTransaction {
        transactionDao.updateTransactionsCategoryBulk(ids, newCategoryId)
    }

    private suspend fun syncAggregateForDate(date: Long) {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 86400000L - 1L

        val txs = transactionDao.getTransactionsByDateRangeList(startOfDay, endOfDay)
        if (txs.isEmpty()) {
            database.dailyFinancialAggregateDao().deleteAggregateForDay(startOfDay)
        } else {
            val totalExp = txs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            val totalInc = txs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val count = txs.size
            val volume = totalExp + totalInc
            val score = 0.4 * kotlin.math.ln(count.toDouble() + 1.0) + 0.6 * kotlin.math.ln(volume + 1.0)

            database.dailyFinancialAggregateDao().upsertAggregate(
                DailyFinancialAggregateEntity(
                    localDateTimestamp = startOfDay,
                    totalExpense = totalExp,
                    totalIncome = totalInc,
                    transactionCount = count,
                    netCashflow = totalInc - totalExp,
                    activityScore = score
                )
            )
        }
    }
}

class AccountRepositoryImpl(
    private val database: com.example.data.local.AppDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) : AccountRepository {
    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { list -> list.map { it.toDomain() } }
    }

    override fun getArchivedAccounts(): Flow<List<Account>> {
        return accountDao.getArchivedAccounts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }

    override suspend fun archiveAccount(id: Long) {
        accountDao.archiveAccount(id)
    }

    override suspend fun unarchiveAccount(id: Long) {
        accountDao.unarchiveAccount(id)
    }

    override suspend fun setDefaultAccount(id: Long) = database.withTransaction {
        accountDao.getAccountById(id) ?: throw IllegalArgumentException("الحساب المحدد غير موجود.")
        accountDao.clearDefaultFlag()
        val updatedRows = accountDao.setDefaultAccount(id)
        require(updatedRows == 1) { "تعذر تعيين الحساب الافتراضي." }
    }

    override suspend fun getTransactionCountForAccount(id: Long): Int {
        return transactionDao.getTransactionCountForAccount(id)
    }
}

class CategoryRepositoryImpl(
    private val database: com.example.data.local.AppDatabase,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetGoalDao: BudgetGoalDao
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getRootCategories(): Flow<List<Category>> {
        return categoryDao.getRootCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getSubcategories(parentId: Long): Flow<List<Category>> {
        return categoryDao.getSubcategories(parentId).map { list -> list.map { it.toDomain() } }
    }

    override fun searchCategories(query: String): Flow<List<Category>> {
        return categoryDao.searchCategories(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun getTransactionCountForCategory(id: Long): Int {
        return categoryDao.getTransactionCountForCategory(id)
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories.map { it.toEntity() })
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun deleteSubcategoriesForParent(parentId: Long) {
        categoryDao.deleteSubcategoriesForParent(parentId)
    }

    override suspend fun mergeCategories(sourceCategoryId: Long, targetCategoryId: Long) = database.withTransaction {
        require(sourceCategoryId != targetCategoryId) { "لا يمكن دمج الفئة مع نفسها." }
        val sourceCat = categoryDao.getCategoryById(sourceCategoryId)
            ?: throw IllegalArgumentException("الفئة المصدر غير موجودة.")
        categoryDao.getCategoryById(targetCategoryId)
            ?: throw IllegalArgumentException("الفئة الهدف غير موجودة.")
        transactionDao.mergeTransactionsCategory(sourceCategoryId, targetCategoryId)
        budgetGoalDao.mergeBudgetGoalsCategory(sourceCategoryId, targetCategoryId)
        categoryDao.deleteCategory(sourceCat)
    }
}

class IncomeRepositoryImpl(
    private val incomeSourceDao: IncomeSourceDao
) : IncomeRepository {
    override fun getAllIncomeSources(): Flow<List<IncomeSource>> {
        return incomeSourceDao.getAllIncomeSources().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveIncomeSources(): Flow<List<IncomeSource>> {
        return incomeSourceDao.getActiveIncomeSources().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getIncomeSourceById(id: Long): IncomeSource? {
        return incomeSourceDao.getIncomeSourceById(id)?.toDomain()
    }

    override suspend fun insertIncomeSource(incomeSource: IncomeSource): Long {
        return incomeSourceDao.insertIncomeSource(incomeSource.toEntity())
    }

    override suspend fun updateIncomeSource(incomeSource: IncomeSource) {
        incomeSourceDao.updateIncomeSource(incomeSource.toEntity())
    }

    override suspend fun deleteIncomeSource(incomeSource: IncomeSource) {
        incomeSourceDao.deleteIncomeSource(incomeSource.toEntity())
    }
}

class SavingRepositoryImpl(
    private val savingGoalDao: SavingGoalDao,
    private val savingsContributionDao: SavingsContributionDao
) : SavingRepository {
    override fun getAllSavingGoals(): Flow<List<SavingGoal>> {
        return savingGoalDao.getAllSavingGoals().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSavingGoalById(id: Long): SavingGoal? {
        return savingGoalDao.getSavingGoalById(id)?.toDomain()
    }

    override suspend fun insertSavingGoal(savingGoal: SavingGoal): Long {
        return savingGoalDao.insertSavingGoal(savingGoal.toEntity())
    }

    override suspend fun updateSavingGoal(savingGoal: SavingGoal) {
        savingGoalDao.updateSavingGoal(savingGoal.toEntity())
    }

    override suspend fun deleteSavingGoal(savingGoal: SavingGoal) {
        savingGoalDao.deleteSavingGoal(savingGoal.toEntity())
    }

    override fun getAllContributions(): Flow<List<SavingsContribution>> {
        return savingsContributionDao.getAllContributions().map { list -> list.map { it.toDomain() } }
    }

    override fun getContributionsForGoal(goalId: Long): Flow<List<SavingsContribution>> {
        return savingsContributionDao.getContributionsForGoal(goalId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertContribution(contribution: SavingsContribution): Long {
        return savingsContributionDao.insertContribution(contribution.toEntity())
    }

    override suspend fun deleteContribution(contribution: SavingsContribution) {
        savingsContributionDao.deleteContribution(contribution.toEntity())
    }

    override suspend fun deleteContributionsForGoal(goalId: Long) {
        savingsContributionDao.deleteContributionsForGoal(goalId)
    }
}

class SubscriptionRepositoryImpl(
    private val subscriptionDao: SubscriptionDao
) : SubscriptionRepository {
    override fun getAllSubscriptions(): Flow<List<Subscription>> {
        return subscriptionDao.getAllSubscriptions().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveSubscriptions(): Flow<List<Subscription>> {
        return subscriptionDao.getActiveSubscriptions().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSubscriptionById(id: Long): Subscription? {
        return subscriptionDao.getSubscriptionById(id)?.toDomain()
    }

    override suspend fun insertSubscription(subscription: Subscription): Long {
        return subscriptionDao.insertSubscription(subscription.toEntity())
    }

    override suspend fun updateSubscription(subscription: Subscription) {
        subscriptionDao.updateSubscription(subscription.toEntity())
    }

    override suspend fun deleteSubscription(subscription: Subscription) {
        subscriptionDao.deleteSubscription(subscription.toEntity())
    }
}
