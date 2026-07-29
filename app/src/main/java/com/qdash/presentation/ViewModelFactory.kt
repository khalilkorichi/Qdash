package com.qdash.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qdash.core.di.AppContainer
import com.qdash.presentation.accounts.AccountsViewModel
import com.qdash.presentation.analytics.AnalyticsViewModel
import com.qdash.presentation.home.HomeViewModel
import com.qdash.presentation.salary.SalaryViewModel
import com.qdash.presentation.savings.SavingsViewModel
import com.qdash.presentation.settings.SettingsViewModel
import com.qdash.presentation.subscriptions.SubscriptionsViewModel
import com.qdash.presentation.transactions.TransactionsViewModel
import com.qdash.presentation.debt.DebtViewModel
import com.qdash.presentation.transfer.TransferViewModel
import com.qdash.presentation.export.ExportViewModel
import com.qdash.presentation.notifications.NotificationsViewModel
import com.qdash.presentation.search.SearchViewModel
import com.qdash.presentation.categories.CategoriesViewModel
import com.qdash.presentation.plans.FinancialPlansViewModel
import com.qdash.presentation.backup.BackupViewModel
import com.qdash.presentation.templates.TemplatesViewModel
import com.qdash.presentation.ai.AiChatViewModel
import com.qdash.presentation.simulator.DocumentSimulatorViewModel
import com.qdash.domain.repository.BackupRepository

class ViewModelFactory(
    private val container: AppContainer,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(com.qdash.presentation.analytics.CardAiChatViewModel::class.java) -> {
                com.qdash.presentation.analytics.CardAiChatViewModel(
                    cardDbSnapshotUseCase = container.cardDbSnapshotUseCase,
                    aiRepository = container.aiRepository
                ) as T
            }
            modelClass.isAssignableFrom(AiChatViewModel::class.java) -> {
                AiChatViewModel(
                    container.aiRepository,
                    container.transactionRepository,
                    container.accountRepository,
                    container.categoryRepository,
                    container.preferencesManager,
                    container.getRecentActivitySummaryUseCase,
                    container.getWalletDistributionUseCase,
                    container.evaluateLowBalanceAlertsUseCase,
                    container.getQuickImpactPreviewUseCase,
                    container.transferBetweenAccountsUseCase,
                    container.savingRepository
                ) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    container.transactionRepository,
                    container.accountRepository,
                    container.categoryRepository,
                    container.subscriptionRepository,
                    container.incomeRepository,
                    container.transactionTemplateRepository,
                    container.preferencesManager,
                    container.aiRepository,
                    container.amanaRepository
                ) as T
            }
            modelClass.isAssignableFrom(com.qdash.presentation.transactions.AddTransactionViewModel::class.java) -> {
                com.qdash.presentation.transactions.AddTransactionViewModel(
                    container.getSmartCategorySuggestionUseCase,
                    container.learnCategoryMappingUseCaseTransaction
                ) as T
            }
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> {
                TransactionsViewModel(
                    container.transactionRepository,
                    container.accountRepository,
                    container.categoryRepository,
                    container.incomeRepository,
                    container.getCategorySuggestionUseCase,
                    container.learnCategoryMappingUseCase,
                    container.budgetGoalRepository,
                    container.transactionTemplateRepository,
                    container.preferencesManager,
                    container.bulkEditTransactionsUseCase
                ) as T
            }
            modelClass.isAssignableFrom(AccountsViewModel::class.java) -> {
                AccountsViewModel(
                    container.accountRepository,
                    container.transactionRepository,
                    container.categoryRepository,
                    container.preferencesManager,
                    container.deleteAccountUseCase,
                    container.updateAccountsOrderUseCase
                ) as T
            }
            modelClass.isAssignableFrom(SavingsViewModel::class.java) -> {
                SavingsViewModel(
                    container.savingRepository,
                    container.accountRepository,
                    container.transactionRepository
                ) as T
            }
            modelClass.isAssignableFrom(DebtViewModel::class.java) -> {
                DebtViewModel(
                    container.debtRepository,
                    container.accountRepository,
                    container.transactionRepository
                ) as T
            }
            modelClass.isAssignableFrom(TransferViewModel::class.java) -> {
                TransferViewModel(
                    container.transferRepository,
                    container.accountRepository,
                    container.transactionRepository
                ) as T
            }
            modelClass.isAssignableFrom(ExportViewModel::class.java) -> {
                ExportViewModel(
                    container.exportRepository,
                    container.accountRepository
                ) as T
            }
            modelClass.isAssignableFrom(SubscriptionsViewModel::class.java) -> {
                SubscriptionsViewModel(
                    container.subscriptionRepository,
                    container.accountRepository
                ) as T
            }
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> {
                AnalyticsViewModel(
                    container.transactionRepository,
                    container.categoryRepository,
                    container.accountRepository,
                    container.incomeRepository,
                    container.savingRepository,
                    container.exportRepository
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    exportSettingsUseCase = container.exportSettingsUseCase,
                    resetAppDataUseCase = container.resetAppDataUseCase,
                    restoreBackupUseCase = container.restoreBackupUseCase,
                    preferencesManager = container.preferencesManager,
                    authRepository = container.authRepository,
                    driveSyncRepository = container.driveSyncRepository,
                    checkForExistingBackupUseCase = container.checkForExistingBackupUseCase,
                    restoreFromDriveUseCase = container.restoreFromDriveUseCase,
                    context = context
                ) as T
            }
            modelClass.isAssignableFrom(com.qdash.presentation.budgetgoals.BudgetGoalsViewModel::class.java) -> {
                com.qdash.presentation.budgetgoals.BudgetGoalsViewModel(
                    container.getBudgetGoalsUseCase,
                    container.addBudgetGoalUseCase,
                    container.updateBudgetGoalUseCase,
                    container.deleteBudgetGoalUseCase,
                    container.getBudgetAlertsUseCase,
                    container.categoryRepository,
                    container.transactionRepository
                ) as T
            }
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> {
                NotificationsViewModel(container.notificationRepository) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(
                    container.transactionRepository,
                    container.categoryRepository,
                    container.accountRepository,
                    container.preferencesManager
                ) as T
            }
            modelClass.isAssignableFrom(CategoriesViewModel::class.java) -> {
                CategoriesViewModel(container.categoryRepository) as T
            }
            modelClass.isAssignableFrom(FinancialPlansViewModel::class.java) -> {
                FinancialPlansViewModel(container.financialPlanRepository) as T
            }
            modelClass.isAssignableFrom(TemplatesViewModel::class.java) -> {
                TemplatesViewModel(
                    container.getAllTemplatesUseCase,
                    container.getPinnedTemplatesUseCase,
                    container.getFrequentTemplatesUseCase,
                    container.createTemplateUseCase,
                    container.updateTemplateUseCase,
                    container.deleteTemplateUseCase,
                    container.useTemplateUseCase,
                    container.togglePinTemplateUseCase,
                    container.searchTemplatesUseCase,
                    container.categoryRepository,
                    container.accountRepository
                ) as T
            }
            modelClass.isAssignableFrom(BackupViewModel::class.java) -> {
                BackupViewModel(
                    backupRepository = container.backupRepository,
                    transactionRepository = container.transactionRepository,
                    categoryRepository = container.categoryRepository,
                    accountRepository = container.accountRepository,
                    preferencesManager = container.preferencesManager,
                    authRepository = container.authRepository,
                    driveSyncRepository = container.driveSyncRepository,
                    context = context,
                    completeOnboardingUseCase = container.completeOnboardingUseCase
                ) as T
            }
            modelClass.isAssignableFrom(com.qdash.presentation.update.UpdatesViewModel::class.java) -> {
                com.qdash.presentation.update.UpdatesViewModel(
                    checkForUpdateUseCase = container.checkForUpdateUseCase,
                    downloadUpdateUseCase = container.downloadUpdateUseCase,
                    backupRepository = container.backupRepository,
                    notificationRepository = container.notificationRepository,
                    preferencesManager = container.preferencesManager,
                    context = context
                ) as T
            }
            modelClass.isAssignableFrom(SalaryViewModel::class.java) -> {
                SalaryViewModel(
                    container.incomeRepository,
                    container.accountRepository,
                    container.getSalaryManagementOverviewUseCase,
                    container.analyzeSalaryDelayImpactUseCase,
                    container.confirmSalaryDelayUseCase,
                    container.deleteSalaryDelayUseCase,
                    container.updateSalaryDelayUseCase,
                    container.depositSalaryNowUseCase,
                    container.subscriptionRepository,
                    container.getSalaryDistributionUseCase,
                    container.saveSalaryDistributionUseCase,
                    container.categoryRepository,
                    container.salaryDistributionRepository,
                    container.transferBetweenAccountsUseCase
                ) as T
            }
            
            modelClass.isAssignableFrom(com.qdash.presentation.onboarding.OnboardingViewModel::class.java) -> {
                com.qdash.presentation.onboarding.OnboardingViewModel(
                    container.accountRepository,
                    container.preferencesManager,
                    container.authRepository,
                    container.driveSyncRepository,
                    container.completeOnboardingUseCase,
                    container.checkForExistingBackupUseCase,
                    container.restoreFromDriveUseCase
                ) as T
            }
            modelClass.isAssignableFrom(DocumentSimulatorViewModel::class.java) -> {
                DocumentSimulatorViewModel(
                    container.postalProfileRepository
                ) as T
            }
            modelClass.isAssignableFrom(com.qdash.presentation.settings.AccountManagementViewModel::class.java) -> {
                com.qdash.presentation.settings.AccountManagementViewModel(
                    container.authRepository,
                    context
                ) as T
            }
            modelClass.isAssignableFrom(com.qdash.presentation.accounts.AddEditAccountViewModel::class.java) -> {
                com.qdash.presentation.accounts.AddEditAccountViewModel(
                    container.accountRepository
                ) as T
            }
            modelClass.isAssignableFrom(com.qdash.presentation.currency.CurrencyExchangeViewModel::class.java) -> {
                com.qdash.presentation.currency.CurrencyExchangeViewModel(
                    getExchangeRatesUseCase = container.getExchangeRatesUseCase,
                    convertCurrencyUseCase = container.convertCurrencyUseCase,
                    refreshOfficialRatesUseCase = container.refreshOfficialRatesUseCase,
                    refreshParallelRatesUseCase = container.refreshParallelRatesUseCase,
                    preferencesManager = container.preferencesManager
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
