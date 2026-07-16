package com.qdash.data.local.dao

import androidx.room.*
import com.qdash.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsContributionDao {
    @Query("SELECT * FROM savings_contributions ORDER BY date DESC")
    fun getAllContributions(): Flow<List<SavingsContributionEntity>>

    @Query("SELECT * FROM savings_contributions WHERE savingGoalId = :goalId ORDER BY date DESC")
    fun getContributionsForGoal(goalId: Long): Flow<List<SavingsContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: SavingsContributionEntity): Long

    @Delete
    suspend fun deleteContribution(contribution: SavingsContributionEntity)

    @Query("DELETE FROM savings_contributions WHERE savingGoalId = :goalId")
    suspend fun deleteContributionsForGoal(goalId: Long)
}

@Dao
interface DebtDao {
    @Transaction
    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun getAllDebts(): Flow<List<DebtWithInstallmentDetails>>

    @Transaction
    @Query("SELECT * FROM debts WHERE isClosed = 0")
    fun getActiveDebts(): Flow<List<DebtWithInstallmentDetails>>

    @Transaction
    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): DebtWithInstallmentDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallmentDetails(details: DebtInstallmentDetailsEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Update
    suspend fun updateInstallmentDetails(details: DebtInstallmentDetailsEntity)

    @Query("DELETE FROM debt_installment_details WHERE debtId = :debtId")
    suspend fun deleteInstallmentDetails(debtId: Long)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)
}

@Dao
interface DebtPaymentDao {
    @Query("SELECT * FROM debt_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDate DESC")
    fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    @Query("SELECT * FROM debt_payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): DebtPaymentEntity?

    @Delete
    suspend fun deletePayment(payment: DebtPaymentEntity)

    @Query("DELETE FROM debt_payments WHERE debtId = :debtId")
    suspend fun deletePaymentsForDebt(debtId: Long)
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY date DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE fromAccountId = :accountId OR toAccountId = :accountId ORDER BY date DESC")
    fun getTransfersByAccount(accountId: Long): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Delete
    suspend fun deleteTransfer(transfer: TransferEntity)
}

@Dao
interface DailyFinancialAggregateDao {
    @Query("SELECT * FROM daily_financial_aggregates WHERE localDateTimestamp = :timestamp")
    suspend fun getAggregateForDay(timestamp: Long): DailyFinancialAggregateEntity?

    @Query("SELECT * FROM daily_financial_aggregates WHERE localDateTimestamp >= :start AND localDateTimestamp <= :end ORDER BY localDateTimestamp ASC")
    fun getAggregatesForRange(start: Long, end: Long): Flow<List<DailyFinancialAggregateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAggregate(aggregate: DailyFinancialAggregateEntity): Long

    @Query("DELETE FROM daily_financial_aggregates WHERE localDateTimestamp = :timestamp")
    suspend fun deleteAggregateForDay(timestamp: Long)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfileEntity): Long

    @Update
    suspend fun updateUserProfile(userProfile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = 1")
    suspend fun deleteUserProfile()
}
