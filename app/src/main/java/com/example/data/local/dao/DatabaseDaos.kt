package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :query || '%' ORDER BY date DESC LIMIT 50")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByDateRangeList(startDate: Long, endDate: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun getTransactionCountForAccount(accountId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE type = :type AND note = :note AND date BETWEEN :startDate AND :endDate)")
    suspend fun isTransactionAlreadyInserted(type: String, note: String, startDate: Long, endDate: Long): Boolean

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE id IN (:transactionIds)")
    suspend fun updateTransactionsCategoryBulk(transactionIds: List<Long>, newCategoryId: Long)

    @Query("DELETE FROM transactions WHERE id IN (:transactionIds)")
    suspend fun deleteTransactionsBulk(transactionIds: List<Long>)

    @Query("SELECT * FROM transactions WHERE id IN (:transactionIds)")
    suspend fun getTransactionsByIds(transactionIds: List<Long>): List<TransactionEntity>

    @Query("UPDATE transactions SET categoryId = :targetCategoryId WHERE categoryId = :sourceCategoryId")
    suspend fun mergeTransactionsCategory(sourceCategoryId: Long, targetCategoryId: Long)

    @Query("SELECT * FROM transactions WHERE transferId = :transferId")
    suspend fun getTransactionsByTransferId(transferId: String): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE transferId = :transferId")
    suspend fun deleteTransactionsByTransferId(transferId: String)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun getAllAccountsIncludingArchived(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): AccountEntity?

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultAccount(id: Long): Int

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :id")
    suspend fun archiveAccount(id: Long)

    @Query("UPDATE accounts SET isArchived = 0 WHERE id = :id")
    suspend fun unarchiveAccount(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun getRootCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun getSubcategories(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name LIKE '%' || :query || '%'")
    fun searchCategories(query: String): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTransactionCountForCategory(categoryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE parentId = :parentId")
    suspend fun deleteSubcategoriesForParent(parentId: Long)
}

@Dao
interface IncomeSourceDao {
    @Query("SELECT * FROM income_sources")
    fun getAllIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE isActive = 1")
    fun getActiveIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE id = :id")
    suspend fun getIncomeSourceById(id: Long): IncomeSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSource(incomeSource: IncomeSourceEntity): Long

    @Update
    suspend fun updateIncomeSource(incomeSource: IncomeSourceEntity)

    @Delete
    suspend fun deleteIncomeSource(incomeSource: IncomeSourceEntity)
}

@Dao
interface SavingGoalDao {
    @Query("SELECT * FROM saving_goals")
    fun getAllSavingGoals(): Flow<List<SavingGoalEntity>>

    @Query("SELECT * FROM saving_goals WHERE id = :id")
    suspend fun getSavingGoalById(id: Long): SavingGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingGoal(savingGoal: SavingGoalEntity): Long

    @Update
    suspend fun updateSavingGoal(savingGoal: SavingGoalEntity)

    @Delete
    suspend fun deleteSavingGoal(savingGoal: SavingGoalEntity)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1")
    fun getActiveSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: Long): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)
}
