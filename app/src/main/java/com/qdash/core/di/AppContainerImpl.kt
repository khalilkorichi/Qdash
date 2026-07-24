package com.qdash.core.di

import android.content.Context
import androidx.room.Room
import com.qdash.core.data.ALL_MIGRATIONS
import com.qdash.core.data.DatabaseSeeder
import com.qdash.data.backup.BackupManager
import com.qdash.data.local.AppDatabase
import com.qdash.data.repository.*
import com.qdash.domain.repository.*
import com.qdash.domain.usecase.templates.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AppContainerImpl — concrete implementation of [AppContainer].
 * Uses extracted migrations from [com.qdash.core.data.ALL_MIGRATIONS]
 * and database seeder from [DatabaseSeeder].
 */
class AppContainerImpl(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        val builder = Room.databaseBuilder(
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
        builder.build()
    }

    override val updateRepository: com.qdash.domain.repository.UpdateRepository by lazy {
        com.qdash.data.update.UpdateRepositoryImpl(context, backupManager)
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

    override val amanaRepository: AmanaRepository by lazy {
        AmanaRepositoryImpl(database.amanaDao())
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database, database.categoryDao(), database.transactionDao(), database.budgetGoalDao())
    }

    override val incomeRepository: IncomeRepository by lazy {
        IncomeRepositoryImpl(
            database = database,
            incomeSourceDao = database.incomeSourceDao(),
            subscriptionDao = database.subscriptionDao(),
            salaryDelayDao = database.salaryDelayDao(),
            notificationDao = database.notificationDao(),
            context = context
        )
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

    override val categorizationEngine: com.qdash.data.categorization.CategorizationEngine by lazy {
        com.qdash.data.categorization.RuleBasedCategorizationEngine(
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
            salaryDistributionDao = database.salaryDistributionDao(),
            aiChatDao = database.aiChatDao(),
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            budgetGoalRepository = budgetGoalRepository,
            debtRepository = debtRepository,
            notificationRepository = notificationRepository,
            geminiApiKey = com.qdash.BuildConfig.GEMINI_API_KEY,
            openRouterApiKey = com.qdash.BuildConfig.OPENROUTER_API_KEY,
            nvidiaApiKey = com.qdash.BuildConfig.NVIDIA_API_KEY
        )
    }


    override val calculateBudgetSpentUseCase: com.qdash.domain.usecase.budget.CalculateBudgetSpentUseCase by lazy {
        com.qdash.domain.usecase.budget.CalculateBudgetSpentUseCase(transactionRepository, budgetGoalRepository)
    }

    override val getBudgetGoalsUseCase: com.qdash.domain.usecase.budget.GetBudgetGoalsUseCase by lazy {
        com.qdash.domain.usecase.budget.GetBudgetGoalsUseCase(budgetGoalRepository, calculateBudgetSpentUseCase)
    }

    override val addBudgetGoalUseCase: com.qdash.domain.usecase.budget.AddBudgetGoalUseCase by lazy {
        com.qdash.domain.usecase.budget.AddBudgetGoalUseCase(budgetGoalRepository)
    }

    override val updateBudgetGoalUseCase: com.qdash.domain.usecase.budget.UpdateBudgetGoalUseCase by lazy {
        com.qdash.domain.usecase.budget.UpdateBudgetGoalUseCase(budgetGoalRepository)
    }

    override val deleteBudgetGoalUseCase: com.qdash.domain.usecase.budget.DeleteBudgetGoalUseCase by lazy {
        com.qdash.domain.usecase.budget.DeleteBudgetGoalUseCase(budgetGoalRepository)
    }

    override val getBudgetAlertsUseCase: com.qdash.domain.usecase.budget.GetBudgetAlertsUseCase by lazy {
        com.qdash.domain.usecase.budget.GetBudgetAlertsUseCase()
    }

    override val getCategorySuggestionUseCase: com.qdash.domain.usecase.categorization.GetCategorySuggestionUseCase by lazy {
        com.qdash.domain.usecase.categorization.GetCategorySuggestionUseCase(categorizationEngine)
    }

    override val getSmartCategorySuggestionUseCase: com.qdash.domain.usecase.transaction.GetSmartCategorySuggestionUseCase by lazy {
        com.qdash.domain.usecase.transaction.GetSmartCategorySuggestionUseCase(categorizationEngine)
    }

    override val learnCategoryMappingUseCase: com.qdash.domain.usecase.categorization.LearnCategoryMappingUseCase by lazy {
        com.qdash.domain.usecase.categorization.LearnCategoryMappingUseCase(categorizationRepository)
    }

    override val learnCategoryMappingUseCaseTransaction: com.qdash.domain.usecase.transaction.LearnCategoryMappingUseCase by lazy {
        com.qdash.domain.usecase.transaction.LearnCategoryMappingUseCase(categorizationRepository)
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

    override val addSavingsContributionUseCase: com.qdash.domain.usecase.savings.AddSavingsContributionUseCase by lazy {
        com.qdash.domain.usecase.savings.AddSavingsContributionUseCase(savingRepository, accountRepository, transactionRepository)
    }

    override val withdrawFromSavingsUseCase: com.qdash.domain.usecase.savings.WithdrawFromSavingsUseCase by lazy {
        com.qdash.domain.usecase.savings.WithdrawFromSavingsUseCase(savingRepository, accountRepository, transactionRepository)
    }

    override val getSavingsInsightsUseCase: com.qdash.domain.usecase.savings.GetSavingsInsightsUseCase by lazy {
        com.qdash.domain.usecase.savings.GetSavingsInsightsUseCase(savingRepository)
    }

    override val getSavingsForecastUseCase: com.qdash.domain.usecase.savings.GetSavingsForecastUseCase by lazy {
        com.qdash.domain.usecase.savings.GetSavingsForecastUseCase(savingRepository)
    }

    override val getSavingsHistoryUseCase: com.qdash.domain.usecase.savings.GetSavingsHistoryUseCase by lazy {
        com.qdash.domain.usecase.savings.GetSavingsHistoryUseCase(savingRepository)
    }

    override val addDebtUseCase: com.qdash.domain.usecase.debt.AddDebtUseCase by lazy {
        com.qdash.domain.usecase.debt.AddDebtUseCase(debtRepository)
    }

    override val recordDebtPaymentUseCase: com.qdash.domain.usecase.debt.RecordDebtPaymentUseCase by lazy {
        com.qdash.domain.usecase.debt.RecordDebtPaymentUseCase(debtRepository, transactionRepository)
    }

    override val getDebtPlanUseCase: com.qdash.domain.usecase.debt.GetDebtPlanUseCase by lazy {
        com.qdash.domain.usecase.debt.GetDebtPlanUseCase()
    }

    override val compareDebtStrategiesUseCase: com.qdash.domain.usecase.debt.CompareDebtStrategiesUseCase by lazy {
        com.qdash.domain.usecase.debt.CompareDebtStrategiesUseCase()
    }

    override val getDebtInsightsUseCase: com.qdash.domain.usecase.debt.GetDebtInsightsUseCase by lazy {
        com.qdash.domain.usecase.debt.GetDebtInsightsUseCase(debtRepository)
    }

    override val closeDebtUseCase: com.qdash.domain.usecase.debt.CloseDebtUseCase by lazy {
        com.qdash.domain.usecase.debt.CloseDebtUseCase(debtRepository)
    }

    override val transferBetweenAccountsUseCase: com.qdash.domain.usecase.transfer.TransferBetweenAccountsUseCase by lazy {
        com.qdash.domain.usecase.transfer.TransferBetweenAccountsUseCase(transferRepository, transactionRepository, accountRepository)
    }

    override val getTransfersUseCase: com.qdash.domain.usecase.transfer.GetTransfersUseCase by lazy {
        com.qdash.domain.usecase.transfer.GetTransfersUseCase(transferRepository)
    }

    override val validateTransferUseCase: com.qdash.domain.usecase.transfer.ValidateTransferUseCase by lazy {
        com.qdash.domain.usecase.transfer.ValidateTransferUseCase(accountRepository)
    }

    override val exportMonthlyPdfReportUseCase: com.qdash.domain.usecase.export.ExportMonthlyPdfReportUseCase by lazy {
        com.qdash.domain.usecase.export.ExportMonthlyPdfReportUseCase(exportRepository)
    }

    override val exportAnalyticsPdfUseCase: com.qdash.domain.usecase.export.ExportAnalyticsPdfUseCase by lazy {
        com.qdash.domain.usecase.export.ExportAnalyticsPdfUseCase(exportRepository)
    }

    override val exportSavingsPdfUseCase: com.qdash.domain.usecase.export.ExportSavingsPdfUseCase by lazy {
        com.qdash.domain.usecase.export.ExportSavingsPdfUseCase(exportRepository)
    }

    override val exportDebtPdfUseCase: com.qdash.domain.usecase.export.ExportDebtPdfUseCase by lazy {
        com.qdash.domain.usecase.export.ExportDebtPdfUseCase(exportRepository)
    }

    override val exportAccountStatementPdfUseCase: com.qdash.domain.usecase.export.ExportAccountStatementPdfUseCase by lazy {
        com.qdash.domain.usecase.export.ExportAccountStatementPdfUseCase(exportRepository)
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

    override val preferencesManager: com.qdash.core.preferences.PreferencesManager by lazy {
        com.qdash.core.preferences.PreferencesManager(context)
    }

    override val backupRepository: com.qdash.domain.repository.BackupRepository by lazy {
        BackupRepositoryImpl(context, database, preferencesManager, backupManager)
    }

    override val postalProfileRepository: com.qdash.domain.repository.PostalProfileRepository by lazy {
        PostalProfileRepositoryImpl(database.postalProfileDao())
    }

    override val getRecentActivitySummaryUseCase: com.qdash.domain.usecase.ai.GetRecentActivitySummaryUseCase by lazy {
        com.qdash.domain.usecase.ai.GetRecentActivitySummaryUseCase(transactionRepository)
    }

    override val getWalletDistributionUseCase: com.qdash.domain.usecase.ai.GetWalletDistributionUseCase by lazy {
        com.qdash.domain.usecase.ai.GetWalletDistributionUseCase(accountRepository)
    }

    override val evaluateLowBalanceAlertsUseCase: com.qdash.domain.usecase.ai.EvaluateLowBalanceAlertsUseCase by lazy {
        com.qdash.domain.usecase.ai.EvaluateLowBalanceAlertsUseCase(accountRepository, preferencesManager)
    }

    override val getQuickImpactPreviewUseCase: com.qdash.domain.usecase.ai.GetQuickImpactPreviewUseCase by lazy {
        com.qdash.domain.usecase.ai.GetQuickImpactPreviewUseCase(
            transactionRepository,
            budgetGoalRepository,
            savingRepository,
            debtRepository
        )
    }

    override val deleteAccountUseCase: com.qdash.domain.usecase.accounts.DeleteAccountUseCase by lazy {
        com.qdash.domain.usecase.accounts.DeleteAccountUseCase(accountRepository)
    }

    override val updateAccountsOrderUseCase: com.qdash.domain.usecase.accounts.UpdateAccountsOrderUseCase by lazy {
        com.qdash.domain.usecase.accounts.UpdateAccountsOrderUseCase(accountRepository)
    }

    override val bulkEditTransactionsUseCase: com.qdash.domain.usecase.transaction.BulkEditTransactionsUseCase by lazy {
        com.qdash.domain.usecase.transaction.BulkEditTransactionsUseCase(transactionRepository)
    }

    override val cardDbSnapshotUseCase: com.qdash.domain.usecase.ai.CardDbSnapshotUseCase by lazy {
        com.qdash.domain.usecase.ai.CardDbSnapshotUseCase(
            transactionRepository,
            accountRepository,
            budgetGoalRepository,
            savingRepository,
            categoryRepository
        )
    }

    override val sendCardAiMessageUseCase: com.qdash.domain.usecase.ai.SendCardAiMessageUseCase by lazy {
        com.qdash.domain.usecase.ai.SendCardAiMessageUseCase(aiRepository)
    }

    override val getAiSettingsUseCase: com.qdash.domain.usecase.ai.GetAiSettingsUseCase by lazy {
        com.qdash.domain.usecase.ai.GetAiSettingsUseCase(preferencesManager)
    }

    override val saveAiSettingsUseCase: com.qdash.domain.usecase.ai.SaveAiSettingsUseCase by lazy {
        com.qdash.domain.usecase.ai.SaveAiSettingsUseCase(preferencesManager)
    }

    // Salary Use Cases
    override val getSalaryManagementOverviewUseCase: com.qdash.domain.usecase.salary.GetSalaryManagementOverviewUseCase by lazy {
        com.qdash.domain.usecase.salary.GetSalaryManagementOverviewUseCase(incomeRepository, subscriptionRepository, debtRepository)
    }

    override val analyzeSalaryDelayImpactUseCase: com.qdash.domain.usecase.salary.AnalyzeSalaryDelayImpactUseCase by lazy {
        com.qdash.domain.usecase.salary.AnalyzeSalaryDelayImpactUseCase()
    }

    override val confirmSalaryDelayUseCase: com.qdash.domain.usecase.salary.ConfirmSalaryDelayUseCase by lazy {
        com.qdash.domain.usecase.salary.ConfirmSalaryDelayUseCase(incomeRepository, salaryDistributionRepository)
    }

    override val deleteSalaryDelayUseCase: com.qdash.domain.usecase.salary.DeleteSalaryDelayUseCase by lazy {
        com.qdash.domain.usecase.salary.DeleteSalaryDelayUseCase(incomeRepository)
    }

    override val updateSalaryDelayUseCase: com.qdash.domain.usecase.salary.UpdateSalaryDelayUseCase by lazy {
        com.qdash.domain.usecase.salary.UpdateSalaryDelayUseCase(incomeRepository)
    }

    override val depositSalaryNowUseCase: com.qdash.domain.usecase.salary.DepositSalaryNowUseCase by lazy {
        com.qdash.domain.usecase.salary.DepositSalaryNowUseCase(
            incomeRepository = incomeRepository,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )
    }

    // Salary Distribution
    override val salaryDistributionRepository: com.qdash.domain.repository.SalaryDistributionRepository by lazy {
        com.qdash.data.repository.SalaryDistributionRepositoryImpl(database.salaryDistributionDao())
    }

    override val getSalaryDistributionUseCase: com.qdash.domain.usecase.salary.GetSalaryDistributionUseCase by lazy {
        com.qdash.domain.usecase.salary.GetSalaryDistributionUseCase(salaryDistributionRepository)
    }

    override val saveSalaryDistributionUseCase: com.qdash.domain.usecase.salary.SaveSalaryDistributionUseCase by lazy {
        com.qdash.domain.usecase.salary.SaveSalaryDistributionUseCase(salaryDistributionRepository, categoryRepository)
    }

    override val authRepository: AuthRepository by lazy {
        com.qdash.data.repository.AuthRepositoryImpl(
            database.userProfileDao(),
            preferencesManager
        )
    }

    override val driveSyncRepository: DriveSyncRepository by lazy {
        com.qdash.data.repository.DriveSyncRepositoryImpl(
            backupManager,
            preferencesManager
        )
    }

    override val getAccountsUseCase: com.qdash.domain.usecase.accounts.GetAccountsUseCase by lazy {
        com.qdash.domain.usecase.accounts.GetAccountsUseCase(accountRepository)
    }

    override val manageAccountUseCase: com.qdash.domain.usecase.accounts.ManageAccountUseCase by lazy {
        com.qdash.domain.usecase.accounts.ManageAccountUseCase(accountRepository)
    }

    override val exportSettingsUseCase: com.qdash.domain.usecase.settings.ExportSettingsUseCase by lazy {
        com.qdash.domain.usecase.settings.ExportSettingsUseCase(backupRepository)
    }

    override val resetAppDataUseCase: com.qdash.domain.usecase.settings.ResetAppDataUseCase by lazy {
        com.qdash.domain.usecase.settings.ResetAppDataUseCase(backupRepository)
    }

    override val restoreBackupUseCase: com.qdash.domain.usecase.settings.RestoreBackupUseCase by lazy {
        com.qdash.domain.usecase.settings.RestoreBackupUseCase(backupRepository, completeOnboardingUseCase)
    }

    override val checkForExistingBackupUseCase: com.qdash.domain.usecase.settings.CheckForExistingBackupUseCase by lazy {
        com.qdash.domain.usecase.settings.CheckForExistingBackupUseCase(driveSyncRepository)
    }

    override val restoreFromDriveUseCase: com.qdash.domain.usecase.settings.RestoreFromDriveUseCase by lazy {
        com.qdash.domain.usecase.settings.RestoreFromDriveUseCase(driveSyncRepository, completeOnboardingUseCase)
    }

    override val getTransactionsUseCase: com.qdash.domain.usecase.transaction.GetTransactionsUseCase by lazy {
        com.qdash.domain.usecase.transaction.GetTransactionsUseCase(transactionRepository)
    }

    override val filterTransactionsUseCase: com.qdash.domain.usecase.transaction.FilterTransactionsUseCase by lazy {
        com.qdash.domain.usecase.transaction.FilterTransactionsUseCase()
    }

    override val checkForUpdateUseCase: com.qdash.domain.usecase.update.CheckForUpdateUseCase by lazy {
        com.qdash.domain.usecase.update.CheckForUpdateUseCase(updateRepository)
    }

    override val downloadUpdateUseCase: com.qdash.domain.usecase.update.DownloadUpdateUseCase by lazy {
        com.qdash.domain.usecase.update.DownloadUpdateUseCase(updateRepository)
    }

    // Onboarding state management
    override val completeOnboardingUseCase: com.qdash.domain.usecase.onboarding.CompleteOnboardingUseCase by lazy {
        com.qdash.domain.usecase.onboarding.CompleteOnboardingUseCase(preferencesManager)
    }

    override val getOnboardingStateUseCase: com.qdash.domain.usecase.onboarding.GetOnboardingStateUseCase by lazy {
        com.qdash.domain.usecase.onboarding.GetOnboardingStateUseCase(preferencesManager)
    }

    init {
        // Pre-warm SharedPreferences to avoid main-thread blocking I/O on startup
        preferencesManager
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseSeeder.prepopulateSystemDefaults(database)
        }
    }
}
