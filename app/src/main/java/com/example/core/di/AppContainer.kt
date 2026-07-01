package com.example.core.di

import com.example.data.local.AppDatabase
import com.example.domain.repository.*
import com.example.domain.usecase.templates.*
import com.example.data.backup.BackupManager

/**
 * AppContainer — the DI contract for the entire application.
 * Implementation lives in AppContainerImpl.
 */
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
    val postalProfileRepository: PostalProfileRepository


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

    // Singleton BackupManager
    val backupManager: BackupManager

    // Preferences Manager
    val preferencesManager: com.example.core.preferences.PreferencesManager

    // Backup Repository
    val backupRepository: com.example.domain.repository.BackupRepository

    // AI Smart Box Use Cases
    val getRecentActivitySummaryUseCase: com.example.domain.usecase.ai.GetRecentActivitySummaryUseCase
    val getWalletDistributionUseCase: com.example.domain.usecase.ai.GetWalletDistributionUseCase
    val evaluateLowBalanceAlertsUseCase: com.example.domain.usecase.ai.EvaluateLowBalanceAlertsUseCase
    val getQuickImpactPreviewUseCase: com.example.domain.usecase.ai.GetQuickImpactPreviewUseCase

    // Accounts Use Cases
    val deleteAccountUseCase: com.example.domain.usecase.accounts.DeleteAccountUseCase
    val updateAccountsOrderUseCase: com.example.domain.usecase.accounts.UpdateAccountsOrderUseCase
    val bulkEditTransactionsUseCase: com.example.domain.usecase.transaction.BulkEditTransactionsUseCase
    val cardDbSnapshotUseCase: com.example.domain.usecase.ai.CardDbSnapshotUseCase
    val sendCardAiMessageUseCase: com.example.domain.usecase.ai.SendCardAiMessageUseCase
    
    // Salary Use Cases
    val getSalaryManagementOverviewUseCase: com.example.domain.usecase.salary.GetSalaryManagementOverviewUseCase
    val analyzeSalaryDelayImpactUseCase: com.example.domain.usecase.salary.AnalyzeSalaryDelayImpactUseCase
    val confirmSalaryDelayUseCase: com.example.domain.usecase.salary.ConfirmSalaryDelayUseCase
    val deleteSalaryDelayUseCase: com.example.domain.usecase.salary.DeleteSalaryDelayUseCase
    val updateSalaryDelayUseCase: com.example.domain.usecase.salary.UpdateSalaryDelayUseCase
}
