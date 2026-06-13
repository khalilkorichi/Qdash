package com.example.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AccountEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.repository.*
import com.example.domain.repository.*
import com.example.domain.usecase.templates.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface AppContainer {
    val database: AppDatabase
    val updateRepository: com.example.data.update.UpdateRepository
    val transactionRepository: TransactionRepository
    val accountRepository: AccountRepository
    val categoryRepository: CategoryRepository
    val incomeRepository: IncomeRepository
    val savingRepository: SavingRepository
    val subscriptionRepository: SubscriptionRepository

    // Budget Goals
    val budgetGoalRepository: BudgetGoalRepository
    val categorizationRepository: CategorizationRepository
    val categorizationEngine: com.example.data.categorization.CategorizationEngine
    val calculateBudgetSpentUseCase: com.example.domain.usecase.budget.CalculateBudgetSpentUseCase
    val getBudgetGoalsUseCase: com.example.domain.usecase.budget.GetBudgetGoalsUseCase
    val addBudgetGoalUseCase: com.example.domain.usecase.budget.AddBudgetGoalUseCase
    val updateBudgetGoalUseCase: com.example.domain.usecase.budget.UpdateBudgetGoalUseCase
    val deleteBudgetGoalUseCase: com.example.domain.usecase.budget.DeleteBudgetGoalUseCase
    val getBudgetAlertsUseCase: com.example.domain.usecase.budget.GetBudgetAlertsUseCase
    val getCategorySuggestionUseCase: com.example.domain.usecase.categorization.GetCategorySuggestionUseCase
    val learnCategoryMappingUseCase: com.example.domain.usecase.categorization.LearnCategoryMappingUseCase

    // New feature repositories
    val debtRepository: DebtRepository
    val transferRepository: TransferRepository
    val exportRepository: ExportRepository
    val notificationRepository: NotificationRepository
    val financialPlanRepository: FinancialPlanRepository
    val aiRepository: AiRepository


    // Savings Use Cases
    val addSavingsContributionUseCase: com.example.domain.usecase.savings.AddSavingsContributionUseCase
    val withdrawFromSavingsUseCase: com.example.domain.usecase.savings.WithdrawFromSavingsUseCase
    val getSavingsInsightsUseCase: com.example.domain.usecase.savings.GetSavingsInsightsUseCase
    val getSavingsForecastUseCase: com.example.domain.usecase.savings.GetSavingsForecastUseCase
    val getSavingsHistoryUseCase: com.example.domain.usecase.savings.GetSavingsHistoryUseCase

    // Debt Use Cases
    val addDebtUseCase: com.example.domain.usecase.debt.AddDebtUseCase
    val recordDebtPaymentUseCase: com.example.domain.usecase.debt.RecordDebtPaymentUseCase
    val getDebtPlanUseCase: com.example.domain.usecase.debt.GetDebtPlanUseCase
    val compareDebtStrategiesUseCase: com.example.domain.usecase.debt.CompareDebtStrategiesUseCase
    val getDebtInsightsUseCase: com.example.domain.usecase.debt.GetDebtInsightsUseCase
    val closeDebtUseCase: com.example.domain.usecase.debt.CloseDebtUseCase

    // Transfer Use Cases
    val transferBetweenAccountsUseCase: com.example.domain.usecase.transfer.TransferBetweenAccountsUseCase
    val getTransfersUseCase: com.example.domain.usecase.transfer.GetTransfersUseCase
    val validateTransferUseCase: com.example.domain.usecase.transfer.ValidateTransferUseCase

    // Export Use Cases
    val exportMonthlyPdfReportUseCase: com.example.domain.usecase.export.ExportMonthlyPdfReportUseCase
    val exportAnalyticsPdfUseCase: com.example.domain.usecase.export.ExportAnalyticsPdfUseCase
    val exportSavingsPdfUseCase: com.example.domain.usecase.export.ExportSavingsPdfUseCase
    val exportDebtPdfUseCase: com.example.domain.usecase.export.ExportDebtPdfUseCase
    val exportAccountStatementPdfUseCase: com.example.domain.usecase.export.ExportAccountStatementPdfUseCase

    val transactionTemplateRepository: TransactionTemplateRepository
    val getAllTemplatesUseCase: GetAllTemplatesUseCase
    val getPinnedTemplatesUseCase: GetPinnedTemplatesUseCase
    val getFrequentTemplatesUseCase: GetFrequentTemplatesUseCase
    val createTemplateUseCase: CreateTemplateUseCase
    val updateTemplateUseCase: UpdateTemplateUseCase
    val deleteTemplateUseCase: DeleteTemplateUseCase
    val useTemplateUseCase: UseTemplateUseCase
    val saveTransactionAsTemplateUseCase: SaveTransactionAsTemplateUseCase
    val togglePinTemplateUseCase: TogglePinTemplateUseCase
    val searchTemplatesUseCase: SearchTemplatesUseCase
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `notifications` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `message` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `isRead` INTEGER NOT NULL DEFAULT 0, 
                `timestamp` INTEGER NOT NULL, 
                `deepLinkRoute` TEXT, 
                `relatedEntityId` INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `financial_plans` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `targetAmount` REAL NOT NULL, 
                `currentAmount` REAL NOT NULL DEFAULT 0.0, 
                `linkedAccountIds` TEXT NOT NULL, 
                `linkedCategoryIds` TEXT NOT NULL, 
                `startDate` INTEGER NOT NULL, 
                `endDate` INTEGER, 
                `status` TEXT NOT NULL DEFAULT 'ACTIVE', 
                `notes` TEXT, 
                `color` TEXT NOT NULL DEFAULT '#6C63FF', 
                `icon` TEXT NOT NULL DEFAULT 'flag', 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("ALTER TABLE `accounts` ADD COLUMN `isDefault` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `accounts` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `categories` ADD COLUMN `isSystem` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `parentId` INTEGER")
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
        
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `savings_contributions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `savingGoalId` INTEGER NOT NULL, 
                `accountId` INTEGER NOT NULL, 
                `amount` REAL NOT NULL, 
                `type` TEXT NOT NULL, 
                `note` TEXT, 
                `date` INTEGER NOT NULL, 
                `linkedTransactionId` INTEGER, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `debts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `creditorName` TEXT NOT NULL, 
                `totalAmount` REAL NOT NULL, 
                `remainingAmount` REAL NOT NULL, 
                `interestRate` REAL, 
                `dueDate` INTEGER, 
                `minimumPayment` REAL NOT NULL, 
                `recommendedPayment` REAL, 
                `paymentFrequency` TEXT NOT NULL, 
                `linkedAccountId` INTEGER, 
                `priority` INTEGER NOT NULL, 
                `notes` TEXT, 
                `color` TEXT NOT NULL, 
                `icon` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `isClosed` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `debt_payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `debtId` INTEGER NOT NULL, 
                `accountId` INTEGER NOT NULL, 
                `amount` REAL NOT NULL, 
                `paymentDate` INTEGER NOT NULL, 
                `paymentType` TEXT NOT NULL, 
                `note` TEXT, 
                `linkedTransactionId` INTEGER, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `transfers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `fromAccountId` INTEGER NOT NULL, 
                `toAccountId` INTEGER NOT NULL, 
                `amount` REAL NOT NULL, 
                `feeAmount` REAL, 
                `note` TEXT, 
                `date` INTEGER NOT NULL, 
                `referenceId` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_parentId` ON `categories` (`parentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_sources_accountId` ON `income_sources` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saving_goals_accountId` ON `saving_goals` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_accountId` ON `subscriptions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_categoryId` ON `subscriptions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_savingGoalId` ON `savings_contributions` (`savingGoalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_accountId` ON `savings_contributions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_date` ON `savings_contributions` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_linkedAccountId` ON `debts` (`linkedAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debtId` ON `debt_payments` (`debtId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_accountId` ON `debt_payments` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_fromAccountId` ON `transfers` (`fromAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_toAccountId` ON `transfers` (`toAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_date` ON `transfers` (`date`)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_financial_aggregates` (
                `localDateTimestamp` INTEGER NOT NULL, 
                `totalExpense` REAL NOT NULL, 
                `totalIncome` REAL NOT NULL, 
                `transactionCount` INTEGER NOT NULL, 
                `netCashflow` REAL NOT NULL, 
                `activityScore` REAL NOT NULL, 
                PRIMARY KEY(`localDateTimestamp`)
            )
        """.trimIndent())
        
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_financial_aggregates_localDateTimestamp` ON `daily_financial_aggregates` (`localDateTimestamp`)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `transaction_templates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `amount` REAL NOT NULL, 
                `transactionType` TEXT NOT NULL, 
                `accountId` INTEGER NOT NULL, 
                `targetAccountId` INTEGER, 
                `categoryId` INTEGER, 
                `subcategoryId` INTEGER, 
                `notes` TEXT, 
                `iconEmoji` TEXT, 
                `colorHex` TEXT, 
                `isPinned` INTEGER NOT NULL DEFAULT 0, 
                `usageCount` INTEGER NOT NULL DEFAULT 0, 
                `lastUsedAt` INTEGER, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_isPinned` ON `transaction_templates` (`isPinned`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_usageCount` ON `transaction_templates` (`usageCount`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_lastUsedAt` ON `transaction_templates` (`lastUsedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_transactionType` ON `transaction_templates` (`transactionType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_categoryId` ON `transaction_templates` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_accountId` ON `transaction_templates` (`accountId`)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Schema v8 and v9 are identical — only version bumped.
        // No structural changes required. Room still needs this
        // migration path so it does NOT fall back to destructive.
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budget_goals_linkedCategoryId ON budget_goals (linkedCategoryId)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_chat_messages_sessionTitle` ON `ai_chat_messages` (`sessionTitle`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_paymentDate` ON `debt_payments` (`paymentDate`)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_category_mappings_normalizedText` ON `user_category_mappings` (`normalizedText`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_rules_keyword` ON `category_rules` (`keyword`)")
    }
}

class AppContainerImpl(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "kdach_database"
        )
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
        .addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = ON;")
            }
        })
        .build()
    }

    override val updateRepository: com.example.data.update.UpdateRepository by lazy {
        com.example.data.update.UpdateRepositoryImpl(context)
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database, database.transactionDao(), database.accountDao())
    }

    override val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(database.accountDao(), database.transactionDao())
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao())
    }

    override val incomeRepository: IncomeRepository by lazy {
        IncomeRepositoryImpl(database.incomeSourceDao())
    }

    override val savingRepository: SavingRepository by lazy {
        SavingRepositoryImpl(database.savingGoalDao(), database.savingsContributionDao())
    }

    override val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepositoryImpl(database.subscriptionDao())
    }

    override val budgetGoalRepository: BudgetGoalRepository by lazy {
        BudgetGoalRepositoryImpl(database.budgetGoalDao())
    }

    override val categorizationRepository: CategorizationRepository by lazy {
        CategorizationRepositoryImpl(database.categoryRuleDao(), database.userCategoryMappingDao())
    }

    override val categorizationEngine: com.example.data.categorization.CategorizationEngine by lazy {
        com.example.data.categorization.RuleBasedCategorizationEngine(
            database.categoryDao(),
            database.categoryRuleDao(),
            database.userCategoryMappingDao()
        )
    }

    override val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl(database.notificationDao(), context)
    }

    override val financialPlanRepository: FinancialPlanRepository by lazy {
        FinancialPlanRepositoryImpl(database.financialPlanDao())
    }

    override val aiRepository: AiRepository by lazy {
        AiRepositoryImpl(
            database.aiChatDao(),
            transactionRepository,
            accountRepository,
            categoryRepository,
            budgetGoalRepository,
            debtRepository,
            notificationRepository
        )
    }


    override val calculateBudgetSpentUseCase: com.example.domain.usecase.budget.CalculateBudgetSpentUseCase by lazy {
        com.example.domain.usecase.budget.CalculateBudgetSpentUseCase(transactionRepository, budgetGoalRepository)
    }

    override val getBudgetGoalsUseCase: com.example.domain.usecase.budget.GetBudgetGoalsUseCase by lazy {
        com.example.domain.usecase.budget.GetBudgetGoalsUseCase(budgetGoalRepository, calculateBudgetSpentUseCase)
    }

    override val addBudgetGoalUseCase: com.example.domain.usecase.budget.AddBudgetGoalUseCase by lazy {
        com.example.domain.usecase.budget.AddBudgetGoalUseCase(budgetGoalRepository)
    }

    override val updateBudgetGoalUseCase: com.example.domain.usecase.budget.UpdateBudgetGoalUseCase by lazy {
        com.example.domain.usecase.budget.UpdateBudgetGoalUseCase(budgetGoalRepository)
    }

    override val deleteBudgetGoalUseCase: com.example.domain.usecase.budget.DeleteBudgetGoalUseCase by lazy {
        com.example.domain.usecase.budget.DeleteBudgetGoalUseCase(budgetGoalRepository)
    }

    override val getBudgetAlertsUseCase: com.example.domain.usecase.budget.GetBudgetAlertsUseCase by lazy {
        com.example.domain.usecase.budget.GetBudgetAlertsUseCase()
    }

    override val getCategorySuggestionUseCase: com.example.domain.usecase.categorization.GetCategorySuggestionUseCase by lazy {
        com.example.domain.usecase.categorization.GetCategorySuggestionUseCase(categorizationEngine)
    }

    override val learnCategoryMappingUseCase: com.example.domain.usecase.categorization.LearnCategoryMappingUseCase by lazy {
        com.example.domain.usecase.categorization.LearnCategoryMappingUseCase(categorizationRepository)
    }

    override val debtRepository: DebtRepository by lazy {
        DebtRepositoryImpl(database.debtDao(), database.debtPaymentDao())
    }

    override val transferRepository: TransferRepository by lazy {
        TransferRepositoryImpl(database.transferDao())
    }

    override val exportRepository: ExportRepository by lazy {
        ExportRepositoryImpl(context, database.accountDao(), database.transactionDao())
    }

    override val addSavingsContributionUseCase: com.example.domain.usecase.savings.AddSavingsContributionUseCase by lazy {
        com.example.domain.usecase.savings.AddSavingsContributionUseCase(savingRepository, accountRepository, transactionRepository)
    }

    override val withdrawFromSavingsUseCase: com.example.domain.usecase.savings.WithdrawFromSavingsUseCase by lazy {
        com.example.domain.usecase.savings.WithdrawFromSavingsUseCase(savingRepository, accountRepository, transactionRepository)
    }

    override val getSavingsInsightsUseCase: com.example.domain.usecase.savings.GetSavingsInsightsUseCase by lazy {
        com.example.domain.usecase.savings.GetSavingsInsightsUseCase(savingRepository)
    }

    override val getSavingsForecastUseCase: com.example.domain.usecase.savings.GetSavingsForecastUseCase by lazy {
        com.example.domain.usecase.savings.GetSavingsForecastUseCase(savingRepository)
    }

    override val getSavingsHistoryUseCase: com.example.domain.usecase.savings.GetSavingsHistoryUseCase by lazy {
        com.example.domain.usecase.savings.GetSavingsHistoryUseCase(savingRepository)
    }

    override val addDebtUseCase: com.example.domain.usecase.debt.AddDebtUseCase by lazy {
        com.example.domain.usecase.debt.AddDebtUseCase(debtRepository)
    }

    override val recordDebtPaymentUseCase: com.example.domain.usecase.debt.RecordDebtPaymentUseCase by lazy {
        com.example.domain.usecase.debt.RecordDebtPaymentUseCase(debtRepository, transactionRepository)
    }

    override val getDebtPlanUseCase: com.example.domain.usecase.debt.GetDebtPlanUseCase by lazy {
        com.example.domain.usecase.debt.GetDebtPlanUseCase()
    }

    override val compareDebtStrategiesUseCase: com.example.domain.usecase.debt.CompareDebtStrategiesUseCase by lazy {
        com.example.domain.usecase.debt.CompareDebtStrategiesUseCase()
    }

    override val getDebtInsightsUseCase: com.example.domain.usecase.debt.GetDebtInsightsUseCase by lazy {
        com.example.domain.usecase.debt.GetDebtInsightsUseCase(debtRepository)
    }

    override val closeDebtUseCase: com.example.domain.usecase.debt.CloseDebtUseCase by lazy {
        com.example.domain.usecase.debt.CloseDebtUseCase(debtRepository)
    }

    override val transferBetweenAccountsUseCase: com.example.domain.usecase.transfer.TransferBetweenAccountsUseCase by lazy {
        com.example.domain.usecase.transfer.TransferBetweenAccountsUseCase(transferRepository, transactionRepository, accountRepository)
    }

    override val getTransfersUseCase: com.example.domain.usecase.transfer.GetTransfersUseCase by lazy {
        com.example.domain.usecase.transfer.GetTransfersUseCase(transferRepository)
    }

    override val validateTransferUseCase: com.example.domain.usecase.transfer.ValidateTransferUseCase by lazy {
        com.example.domain.usecase.transfer.ValidateTransferUseCase(accountRepository)
    }

    override val exportMonthlyPdfReportUseCase: com.example.domain.usecase.export.ExportMonthlyPdfReportUseCase by lazy {
        com.example.domain.usecase.export.ExportMonthlyPdfReportUseCase(exportRepository)
    }

    override val exportAnalyticsPdfUseCase: com.example.domain.usecase.export.ExportAnalyticsPdfUseCase by lazy {
        com.example.domain.usecase.export.ExportAnalyticsPdfUseCase(exportRepository)
    }

    override val exportSavingsPdfUseCase: com.example.domain.usecase.export.ExportSavingsPdfUseCase by lazy {
        com.example.domain.usecase.export.ExportSavingsPdfUseCase(exportRepository)
    }

    override val exportDebtPdfUseCase: com.example.domain.usecase.export.ExportDebtPdfUseCase by lazy {
        com.example.domain.usecase.export.ExportDebtPdfUseCase(exportRepository)
    }

    override val exportAccountStatementPdfUseCase: com.example.domain.usecase.export.ExportAccountStatementPdfUseCase by lazy {
        com.example.domain.usecase.export.ExportAccountStatementPdfUseCase(exportRepository)
    }

    override val transactionTemplateRepository: TransactionTemplateRepository by lazy {
        TransactionTemplateRepositoryImpl(database.transactionTemplateDao())
    }

    override val getAllTemplatesUseCase: GetAllTemplatesUseCase by lazy {
        GetAllTemplatesUseCase(transactionTemplateRepository)
    }

    override val getPinnedTemplatesUseCase: GetPinnedTemplatesUseCase by lazy {
        GetPinnedTemplatesUseCase(transactionTemplateRepository)
    }

    override val getFrequentTemplatesUseCase: GetFrequentTemplatesUseCase by lazy {
        GetFrequentTemplatesUseCase(transactionTemplateRepository)
    }

    override val createTemplateUseCase: CreateTemplateUseCase by lazy {
        CreateTemplateUseCase(transactionTemplateRepository)
    }

    override val updateTemplateUseCase: UpdateTemplateUseCase by lazy {
        UpdateTemplateUseCase(transactionTemplateRepository)
    }

    override val deleteTemplateUseCase: DeleteTemplateUseCase by lazy {
        DeleteTemplateUseCase(transactionTemplateRepository)
    }

    override val useTemplateUseCase: UseTemplateUseCase by lazy {
        UseTemplateUseCase(transactionTemplateRepository)
    }

    override val saveTransactionAsTemplateUseCase: SaveTransactionAsTemplateUseCase by lazy {
        SaveTransactionAsTemplateUseCase(transactionTemplateRepository)
    }

    override val togglePinTemplateUseCase: TogglePinTemplateUseCase by lazy {
        TogglePinTemplateUseCase(transactionTemplateRepository)
    }

    override val searchTemplatesUseCase: SearchTemplatesUseCase by lazy {
        SearchTemplatesUseCase(transactionTemplateRepository)
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            prepopulateSystemDefaults()
        }
    }

    private suspend fun prepopulateSystemDefaults() {
        try {
            val categoryDao = database.categoryDao()
            val accountDao = database.accountDao()

            val expectedIncomeCategories = listOf(
                CategoryEntity(id = 11, name = "راتب", type = "INCOME", icon = "work", color = "#22C55E", isSystem = true, sortOrder = 1),
                CategoryEntity(id = 12, name = "مكافآت وهدايا", type = "INCOME", icon = "redeem", color = "#EF4444", isSystem = true, sortOrder = 2),
                CategoryEntity(id = 13, name = "مبيعات", type = "INCOME", icon = "storefront", color = "#F59E0B", isSystem = true, sortOrder = 3),
                CategoryEntity(id = 14, name = "عمل إضافي", type = "INCOME", icon = "schedule", color = "#3B82F6", isSystem = true, sortOrder = 4),
                CategoryEntity(id = 15, name = "أخرى", type = "INCOME", icon = "monetization_on", color = "#00F2FE", isSystem = true, sortOrder = 5)
            )

            val existingCategories = categoryDao.getAllCategories().first()
            if (existingCategories.isEmpty()) {
                val defaultCategories = listOf(
                    // Root expense categories
                    CategoryEntity(id = 1, name = "شخصي", type = "EXPENSE", icon = "person", color = "#6C63FF", isSystem = true, sortOrder = 1),
                    CategoryEntity(id = 2, name = "عائلي", type = "EXPENSE", icon = "groups", color = "#22C55E", isSystem = true, sortOrder = 2),
                    CategoryEntity(id = 3, name = "منزلي", type = "EXPENSE", icon = "home", color = "#EF4444", isSystem = true, sortOrder = 3),
                    CategoryEntity(id = 4, name = "طعام", type = "EXPENSE", icon = "restaurant", color = "#F59E0B", isSystem = true, sortOrder = 4),
                    CategoryEntity(id = 5, name = "مواصلات", type = "EXPENSE", icon = "directions_car", color = "#3B82F6", isSystem = true, sortOrder = 5),
                    CategoryEntity(id = 6, name = "فواتير", type = "EXPENSE", icon = "receipt_long", color = "#EE5F5B", isSystem = true, sortOrder = 6),
                    CategoryEntity(id = 7, name = "تسوق", type = "EXPENSE", icon = "shopping_bag", color = "#A770EF", isSystem = true, sortOrder = 7),
                    CategoryEntity(id = 8, name = "صحة", type = "EXPENSE", icon = "medical_services", color = "#00F2FE", isSystem = true, sortOrder = 8),
                    CategoryEntity(id = 9, name = "تعليم", type = "EXPENSE", icon = "school", color = "#4FACFE", isSystem = true, sortOrder = 9),
                    CategoryEntity(id = 10, name = "ترفيه", type = "EXPENSE", icon = "sports_esports", color = "#F35588", isSystem = true, sortOrder = 10),
                    // Income categories
                    expectedIncomeCategories[0],
                    expectedIncomeCategories[1],
                    expectedIncomeCategories[2],
                    expectedIncomeCategories[3],
                    expectedIncomeCategories[4],
                    // Subcategories for منزلي (id=3)
                    CategoryEntity(name = "إيجار", type = "EXPENSE", icon = "house", color = "#EF4444", isSystem = true, parentId = 3, sortOrder = 1),
                    CategoryEntity(name = "كهرباء وغاز", type = "EXPENSE", icon = "bolt", color = "#F59E0B", isSystem = true, parentId = 3, sortOrder = 2),
                    CategoryEntity(name = "ماء", type = "EXPENSE", icon = "water_drop", color = "#3B82F6", isSystem = true, parentId = 3, sortOrder = 3),
                    CategoryEntity(name = "إنترنت", type = "EXPENSE", icon = "wifi", color = "#6C63FF", isSystem = true, parentId = 3, sortOrder = 4),
                    CategoryEntity(name = "أثاث", type = "EXPENSE", icon = "chair", color = "#A770EF", isSystem = true, parentId = 3, sortOrder = 5),
                    // Subcategories for طعام (id=4)
                    CategoryEntity(name = "بقالة", type = "EXPENSE", icon = "shopping_cart", color = "#22C55E", isSystem = true, parentId = 4, sortOrder = 1),
                    CategoryEntity(name = "مطاعم", type = "EXPENSE", icon = "restaurant", color = "#F59E0B", isSystem = true, parentId = 4, sortOrder = 2),
                    CategoryEntity(name = "قهوة", type = "EXPENSE", icon = "coffee", color = "#8B4513", isSystem = true, parentId = 4, sortOrder = 3),
                    // Subcategories for مواصلات (id=5)
                    CategoryEntity(name = "وقود", type = "EXPENSE", icon = "local_gas_station", color = "#3B82F6", isSystem = true, parentId = 5, sortOrder = 1),
                    CategoryEntity(name = "تاكسي", type = "EXPENSE", icon = "local_taxi", color = "#F59E0B", isSystem = true, parentId = 5, sortOrder = 2),
                    CategoryEntity(name = "حافلة", type = "EXPENSE", icon = "directions_bus", color = "#22C55E", isSystem = true, parentId = 5, sortOrder = 3),
                    // Subcategories for شخصي (id=1)
                    CategoryEntity(name = "ملابس", type = "EXPENSE", icon = "checkroom", color = "#6C63FF", isSystem = true, parentId = 1, sortOrder = 1),
                    CategoryEntity(name = "عناية شخصية", type = "EXPENSE", icon = "spa", color = "#F35588", isSystem = true, parentId = 1, sortOrder = 2),
                    // Subcategories for ترفيه (id=10)
                    CategoryEntity(name = "ألعاب", type = "EXPENSE", icon = "sports_esports", color = "#F35588", isSystem = true, parentId = 10, sortOrder = 1),
                    CategoryEntity(name = "بث مباشر", type = "EXPENSE", icon = "live_tv", color = "#EF4444", isSystem = true, parentId = 10, sortOrder = 2),
                    CategoryEntity(name = "فعاليات", type = "EXPENSE", icon = "event", color = "#A770EF", isSystem = true, parentId = 10, sortOrder = 3)
                )
                categoryDao.insertCategories(defaultCategories)
            } else {
                for (cat in expectedIncomeCategories) {
                    val exists = existingCategories.any { it.name == cat.name && it.type == "INCOME" }
                    if (!exists) {
                        categoryDao.insertCategory(cat)
                    }
                }
            }

            val existingAccounts = accountDao.getAllAccounts().first()
            if (existingAccounts.isEmpty()) {
                val defaultAccounts = listOf(
                    AccountEntity(name = "بريدي موب", type = "BARIDIMOB", balance = 45000.0, color = "#8A2387", icon = "phonelink_ring", isDefault = true),
                    AccountEntity(name = "نقدي / كاش", type = "CASH", balance = 5000.0, color = "#11998e", icon = "payments", isDefault = false),
                    AccountEntity(name = "حساب التوفير", type = "SAVINGS", balance = 15000.0, color = "#4facfe", icon = "savings", isDefault = false)
                )
                for (account in defaultAccounts) {
                    accountDao.insertAccount(account)
                }
            }

            // Database Mock Seeding removed as requested by the user for a clean starting experience
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
