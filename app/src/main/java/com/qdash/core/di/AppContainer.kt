package com.qdash.core.di

import com.qdash.data.local.AppDatabase
import com.qdash.domain.repository.*
import com.qdash.domain.usecase.templates.*
import com.qdash.data.backup.BackupManager

/**
 * AppContainer — the DI contract for the entire application.
 * Implementation lives in AppContainerImpl.
 */
interface AppContainer {
    val database: AppDatabase
    val updateRepository: UpdateRepository
    val transactionRepository: TransactionRepository
    val accountRepository: AccountRepository
    val categoryRepository: CategoryRepository
    val incomeRepository: IncomeRepository
    val savingRepository: SavingRepository
    val subscriptionRepository: SubscriptionRepository

    // Budget Goals
    val budgetGoalRepository: BudgetGoalRepository
    val categorizationRepository: CategorizationRepository
    val categorizationEngine: com.qdash.data.categorization.CategorizationEngine
    val calculateBudgetSpentUseCase: com.qdash.domain.usecase.budget.CalculateBudgetSpentUseCase
    val getBudgetGoalsUseCase: com.qdash.domain.usecase.budget.GetBudgetGoalsUseCase
    val addBudgetGoalUseCase: com.qdash.domain.usecase.budget.AddBudgetGoalUseCase
    val updateBudgetGoalUseCase: com.qdash.domain.usecase.budget.UpdateBudgetGoalUseCase
    val deleteBudgetGoalUseCase: com.qdash.domain.usecase.budget.DeleteBudgetGoalUseCase
    val getBudgetAlertsUseCase: com.qdash.domain.usecase.budget.GetBudgetAlertsUseCase
    val getCategorySuggestionUseCase: com.qdash.domain.usecase.categorization.GetCategorySuggestionUseCase
    val learnCategoryMappingUseCase: com.qdash.domain.usecase.categorization.LearnCategoryMappingUseCase

    // New feature repositories
    val debtRepository: DebtRepository
    val transferRepository: TransferRepository
    val exportRepository: ExportRepository
    val notificationRepository: NotificationRepository
    val financialPlanRepository: FinancialPlanRepository
    val aiRepository: AiRepository
    val postalProfileRepository: PostalProfileRepository
    val amanaRepository: AmanaRepository


    // Savings Use Cases
    val addSavingsContributionUseCase: com.qdash.domain.usecase.savings.AddSavingsContributionUseCase
    val withdrawFromSavingsUseCase: com.qdash.domain.usecase.savings.WithdrawFromSavingsUseCase
    val getSavingsInsightsUseCase: com.qdash.domain.usecase.savings.GetSavingsInsightsUseCase
    val getSavingsForecastUseCase: com.qdash.domain.usecase.savings.GetSavingsForecastUseCase
    val getSavingsHistoryUseCase: com.qdash.domain.usecase.savings.GetSavingsHistoryUseCase

    // Debt Use Cases
    val addDebtUseCase: com.qdash.domain.usecase.debt.AddDebtUseCase
    val recordDebtPaymentUseCase: com.qdash.domain.usecase.debt.RecordDebtPaymentUseCase
    val getDebtPlanUseCase: com.qdash.domain.usecase.debt.GetDebtPlanUseCase
    val compareDebtStrategiesUseCase: com.qdash.domain.usecase.debt.CompareDebtStrategiesUseCase
    val getDebtInsightsUseCase: com.qdash.domain.usecase.debt.GetDebtInsightsUseCase
    val closeDebtUseCase: com.qdash.domain.usecase.debt.CloseDebtUseCase

    // Transfer Use Cases
    val transferBetweenAccountsUseCase: com.qdash.domain.usecase.transfer.TransferBetweenAccountsUseCase
    val getTransfersUseCase: com.qdash.domain.usecase.transfer.GetTransfersUseCase
    val validateTransferUseCase: com.qdash.domain.usecase.transfer.ValidateTransferUseCase

    // Export Use Cases
    val exportMonthlyPdfReportUseCase: com.qdash.domain.usecase.export.ExportMonthlyPdfReportUseCase
    val exportAnalyticsPdfUseCase: com.qdash.domain.usecase.export.ExportAnalyticsPdfUseCase
    val exportSavingsPdfUseCase: com.qdash.domain.usecase.export.ExportSavingsPdfUseCase
    val exportDebtPdfUseCase: com.qdash.domain.usecase.export.ExportDebtPdfUseCase
    val exportAccountStatementPdfUseCase: com.qdash.domain.usecase.export.ExportAccountStatementPdfUseCase

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

    // Singleton BackupManager
    val backupManager: BackupManager

    // Preferences Manager
    val preferencesManager: com.qdash.core.preferences.PreferencesManager

    // Backup Repository
    val backupRepository: com.qdash.domain.repository.BackupRepository

    // AI Smart Box Use Cases
    val getRecentActivitySummaryUseCase: com.qdash.domain.usecase.ai.GetRecentActivitySummaryUseCase
    val getWalletDistributionUseCase: com.qdash.domain.usecase.ai.GetWalletDistributionUseCase
    val evaluateLowBalanceAlertsUseCase: com.qdash.domain.usecase.ai.EvaluateLowBalanceAlertsUseCase
    val getQuickImpactPreviewUseCase: com.qdash.domain.usecase.ai.GetQuickImpactPreviewUseCase

    // Accounts Use Cases
    val deleteAccountUseCase: com.qdash.domain.usecase.accounts.DeleteAccountUseCase
    val updateAccountsOrderUseCase: com.qdash.domain.usecase.accounts.UpdateAccountsOrderUseCase
    val bulkEditTransactionsUseCase: com.qdash.domain.usecase.transaction.BulkEditTransactionsUseCase
    val cardDbSnapshotUseCase: com.qdash.domain.usecase.ai.CardDbSnapshotUseCase
    val sendCardAiMessageUseCase: com.qdash.domain.usecase.ai.SendCardAiMessageUseCase
    
    // Salary Use Cases
    val getSalaryManagementOverviewUseCase: com.qdash.domain.usecase.salary.GetSalaryManagementOverviewUseCase
    val analyzeSalaryDelayImpactUseCase: com.qdash.domain.usecase.salary.AnalyzeSalaryDelayImpactUseCase
    val confirmSalaryDelayUseCase: com.qdash.domain.usecase.salary.ConfirmSalaryDelayUseCase
    val deleteSalaryDelayUseCase: com.qdash.domain.usecase.salary.DeleteSalaryDelayUseCase
    val updateSalaryDelayUseCase: com.qdash.domain.usecase.salary.UpdateSalaryDelayUseCase
    val depositSalaryNowUseCase: com.qdash.domain.usecase.salary.DepositSalaryNowUseCase

    // Salary Distribution
    val salaryDistributionRepository: SalaryDistributionRepository
    val getSalaryDistributionUseCase: com.qdash.domain.usecase.salary.GetSalaryDistributionUseCase
    val saveSalaryDistributionUseCase: com.qdash.domain.usecase.salary.SaveSalaryDistributionUseCase

    val authRepository: AuthRepository
    val driveSyncRepository: DriveSyncRepository

    // Remediations (ARCH-001 / ARCH-003)
    val getAccountsUseCase: com.qdash.domain.usecase.accounts.GetAccountsUseCase
    val manageAccountUseCase: com.qdash.domain.usecase.accounts.ManageAccountUseCase
    val exportSettingsUseCase: com.qdash.domain.usecase.settings.ExportSettingsUseCase
    val resetAppDataUseCase: com.qdash.domain.usecase.settings.ResetAppDataUseCase
    val restoreBackupUseCase: com.qdash.domain.usecase.settings.RestoreBackupUseCase
    val checkForExistingBackupUseCase: com.qdash.domain.usecase.settings.CheckForExistingBackupUseCase
    val restoreFromDriveUseCase: com.qdash.domain.usecase.settings.RestoreFromDriveUseCase
    val getTransactionsUseCase: com.qdash.domain.usecase.transaction.GetTransactionsUseCase
    val filterTransactionsUseCase: com.qdash.domain.usecase.transaction.FilterTransactionsUseCase
    val checkForUpdateUseCase: com.qdash.domain.usecase.update.CheckForUpdateUseCase
    val downloadUpdateUseCase: com.qdash.domain.usecase.update.DownloadUpdateUseCase

    // Onboarding state management
    val completeOnboardingUseCase: com.qdash.domain.usecase.onboarding.CompleteOnboardingUseCase
    val getOnboardingStateUseCase: com.qdash.domain.usecase.onboarding.GetOnboardingStateUseCase
}

