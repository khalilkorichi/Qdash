package com.qdash.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.qdash.data.local.dao.*
import com.qdash.data.local.entities.*

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        IncomeSourceEntity::class,
        SavingGoalEntity::class,
        SubscriptionEntity::class,
        BudgetGoalEntity::class,
        CategoryRuleEntity::class,
        UserCategoryMappingEntity::class,
        SavingsContributionEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        TransferEntity::class,
        NotificationEntity::class,
        FinancialPlanEntity::class,
        DailyFinancialAggregateEntity::class,
        TransactionTemplateEntity::class,
        AiChatMessageEntity::class,
        PostalProfileEntity::class,
        SalaryDelayEntity::class,
        SalaryDistributionEntity::class,
        SalaryEnvelopeEntity::class,
        UserProfileEntity::class
    ],
    version = 25,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeSourceDao(): IncomeSourceDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun userCategoryMappingDao(): UserCategoryMappingDao
    abstract fun savingsContributionDao(): SavingsContributionDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao
    abstract fun transferDao(): TransferDao
    abstract fun notificationDao(): NotificationDao
    abstract fun financialPlanDao(): FinancialPlanDao
    abstract fun dailyFinancialAggregateDao(): DailyFinancialAggregateDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao
    abstract fun aiChatDao(): AiChatDao
    abstract fun postalProfileDao(): PostalProfileDao
    abstract fun salaryDelayDao(): SalaryDelayDao
    abstract fun salaryDistributionDao(): SalaryDistributionDao
    abstract fun userProfileDao(): UserProfileDao
}
