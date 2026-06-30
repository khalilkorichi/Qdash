package com.example.core.di

import android.content.Context
import androidx.room.Room
import com.example.core.data.ALL_MIGRATIONS
import com.example.core.data.DatabaseSeeder
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.repository.*
import com.example.domain.repository.*
import com.example.domain.usecase.templates.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AppContainerImpl — concrete implementation of [AppContainer].
 * Uses extracted migrations from [com.example.core.data.ALL_MIGRATIONS]
 * and database seeder from [DatabaseSeeder].
 */
class AppContainerImpl(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "kdach_database"
        )
        .addMigrations(*ALL_MIGRATIONS)
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
        TransactionRepositoryImpl(
            database = database,
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao(),
            budgetGoalRepositoryProvider = { budgetGoalRepository },
            notificationRepositoryProvider = { notificationRepository },
            getBudgetAlertsUseCaseProvider = { getBudgetAlertsUseCase },
            getBudgetGoalsUseCaseProvider = { getBudgetGoalsUseCase }
        )
    }

    override val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(database, database.accountDao(), database.transactionDao())
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database, database.categoryDao(), database.transactionDao(), database.budgetGoalDao())
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
            database.userCategoryMappingDao(),
            aiRepository
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
            aiChatDao = database.aiChatDao(),
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            budgetGoalRepository = budgetGoalRepository,
            debtRepository = debtRepository,
            notificationRepository = notificationRepository,
            geminiApiKey = com.example.BuildConfig.GEMINI_API_KEY,
            openRouterApiKey = com.example.BuildConfig.OPENROUTER_API_KEY,
            nvidiaApiKey = com.example.BuildConfig.NVIDIA_API_KEY
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

    // Singleton BackupManager — no longer created per ViewModel
    override val backupManager: BackupManager by lazy {
        BackupManager(context, database)
    }

    override val preferencesManager: com.example.core.preferences.PreferencesManager by lazy {
        com.example.core.preferences.PreferencesManager(context)
    }

    override val backupRepository: com.example.domain.repository.BackupRepository by lazy {
        BackupRepositoryImpl(database)
    }

    override val postalProfileRepository: com.example.domain.repository.PostalProfileRepository by lazy {
        PostalProfileRepositoryImpl(database.postalProfileDao())
    }

    override val getRecentActivitySummaryUseCase: com.example.domain.usecase.ai.GetRecentActivitySummaryUseCase by lazy {
        com.example.domain.usecase.ai.GetRecentActivitySummaryUseCase(transactionRepository)
    }

    override val getWalletDistributionUseCase: com.example.domain.usecase.ai.GetWalletDistributionUseCase by lazy {
        com.example.domain.usecase.ai.GetWalletDistributionUseCase(accountRepository)
    }

    override val evaluateLowBalanceAlertsUseCase: com.example.domain.usecase.ai.EvaluateLowBalanceAlertsUseCase by lazy {
        com.example.domain.usecase.ai.EvaluateLowBalanceAlertsUseCase(accountRepository, preferencesManager)
    }

    override val getQuickImpactPreviewUseCase: com.example.domain.usecase.ai.GetQuickImpactPreviewUseCase by lazy {
        com.example.domain.usecase.ai.GetQuickImpactPreviewUseCase(
            transactionRepository,
            budgetGoalRepository,
            savingRepository,
            debtRepository
        )
    }

    override val deleteAccountUseCase: com.example.domain.usecase.accounts.DeleteAccountUseCase by lazy {
        com.example.domain.usecase.accounts.DeleteAccountUseCase(accountRepository)
    }

    override val updateAccountsOrderUseCase: com.example.domain.usecase.accounts.UpdateAccountsOrderUseCase by lazy {
        com.example.domain.usecase.accounts.UpdateAccountsOrderUseCase(accountRepository)
    }

    override val bulkEditTransactionsUseCase: com.example.domain.usecase.transaction.BulkEditTransactionsUseCase by lazy {
        com.example.domain.usecase.transaction.BulkEditTransactionsUseCase(transactionRepository)
    }

    override val cardDbSnapshotUseCase: com.example.domain.usecase.ai.CardDbSnapshotUseCase by lazy {
        com.example.domain.usecase.ai.CardDbSnapshotUseCase(
            transactionRepository,
            accountRepository,
            budgetGoalRepository,
            savingRepository,
            categoryRepository
        )
    }

    override val sendCardAiMessageUseCase: com.example.domain.usecase.ai.SendCardAiMessageUseCase by lazy {
        com.example.domain.usecase.ai.SendCardAiMessageUseCase(aiRepository)
    }

    init {
        // Pre-warm SharedPreferences to avoid main-thread blocking I/O on startup
        preferencesManager
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseSeeder.prepopulateSystemDefaults(database)
        }
    }
}
