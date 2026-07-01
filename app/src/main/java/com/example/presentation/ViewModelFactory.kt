package com.example.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.di.AppContainer
import com.example.presentation.accounts.AccountsViewModel
import com.example.presentation.analytics.AnalyticsViewModel
import com.example.presentation.home.HomeViewModel
import com.example.presentation.salary.SalaryViewModel
import com.example.presentation.savings.SavingsViewModel
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.subscriptions.SubscriptionsViewModel
import com.example.presentation.transactions.TransactionsViewModel
import com.example.presentation.debt.DebtViewModel
import com.example.presentation.transfer.TransferViewModel
import com.example.presentation.export.ExportViewModel
import com.example.presentation.notifications.NotificationsViewModel
import com.example.presentation.search.SearchViewModel
import com.example.presentation.categories.CategoriesViewModel
import com.example.presentation.plans.FinancialPlansViewModel
import com.example.presentation.backup.BackupViewModel
import com.example.presentation.templates.TemplatesViewModel
import com.example.presentation.ai.AiChatViewModel
import com.example.presentation.simulator.DocumentSimulatorViewModel
import com.example.data.backup.BackupManager

class ViewModelFactory(
    private val container: AppContainer,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(com.example.presentation.analytics.CardAiChatViewModel::class.java) -> {
                com.example.presentation.analytics.CardAiChatViewModel(
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
                    container.aiRepository
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
                    container.transactionRepository,
                    container.accountRepository,
                    container.categoryRepository,
                    container.incomeRepository,
                    container.savingRepository,
                    container.subscriptionRepository,
                    container.backupRepository,
                    container.preferencesManager,
                    context
                ) as T
            }
            modelClass.isAssignableFrom(com.example.presentation.budgetgoals.BudgetGoalsViewModel::class.java) -> {
                com.example.presentation.budgetgoals.BudgetGoalsViewModel(
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
                    container.backupManager,
                    container.transactionRepository,
                    container.categoryRepository,
                    container.accountRepository
                ) as T
            }
            modelClass.isAssignableFrom(com.example.presentation.update.UpdatesViewModel::class.java) -> {
                com.example.presentation.update.UpdatesViewModel(
                    container.updateRepository,
                    container.backupManager,
                    container.notificationRepository,
                    container.preferencesManager,
                    context
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
                    container.subscriptionRepository
                ) as T
            }
            
            modelClass.isAssignableFrom(com.example.presentation.onboarding.OnboardingViewModel::class.java) -> {
                com.example.presentation.onboarding.OnboardingViewModel(
                    container.accountRepository,
                    container.preferencesManager
                ) as T
            }
            modelClass.isAssignableFrom(DocumentSimulatorViewModel::class.java) -> {
                DocumentSimulatorViewModel(
                    container.postalProfileRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
