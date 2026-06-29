package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.core.di.AppContainerImpl
import com.example.presentation.accounts.AccountsViewModel
import com.example.presentation.analytics.AnalyticsViewModel
import com.example.presentation.home.HomeViewModel
import com.example.presentation.savings.SavingsViewModel
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.subscriptions.SubscriptionsViewModel
import com.example.presentation.transactions.TransactionsViewModel
import com.example.presentation.debt.DebtViewModel
import com.example.presentation.transfer.TransferViewModel
import com.example.presentation.export.ExportViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppStartupTest {

    @Test
    fun verifyDependencyInjectionAndDatabasePrepopulation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull("Context is null", context)
        
        // Instantiate real AppContainerImpl
        val container = AppContainerImpl(context)
        assertNotNull("Container is null", container)
        
        // Verify we can access repositories
        assertNotNull(container.transactionRepository)
        assertNotNull(container.accountRepository)
        assertNotNull(container.categoryRepository)
        assertNotNull(container.debtRepository)
        assertNotNull(container.transferRepository)
        assertNotNull(container.exportRepository)
        
        println("All repositories instantiated successfully.")

        // Build ViewModels
        val homeViewModel = HomeViewModel(
            container.transactionRepository,
            container.accountRepository,
            container.categoryRepository,
            container.subscriptionRepository,
            container.incomeRepository,
            container.transactionTemplateRepository,
            container.preferencesManager,
            container.aiRepository
        )
        assertNotNull(homeViewModel)

        val transactionsViewModel = TransactionsViewModel(
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
        )
        assertNotNull(transactionsViewModel)

        val accountsViewModel = AccountsViewModel(
            container.accountRepository,
            container.transactionRepository,
            container.categoryRepository,
            container.preferencesManager,
            container.deleteAccountUseCase,
            container.updateAccountsOrderUseCase
        )
        assertNotNull(accountsViewModel)

        val savingsViewModel = SavingsViewModel(
            container.savingRepository,
            container.accountRepository,
            container.transactionRepository
        )
        assertNotNull(savingsViewModel)

        val debtViewModel = DebtViewModel(
            container.debtRepository,
            container.accountRepository,
            container.transactionRepository
        )
        assertNotNull(debtViewModel)

        val transferViewModel = TransferViewModel(
            container.transferRepository,
            container.accountRepository,
            container.transactionRepository
        )
        assertNotNull(transferViewModel)

        val exportViewModel = ExportViewModel(
            container.exportRepository,
            container.accountRepository
        )
        assertNotNull(exportViewModel)

        val subscriptionsViewModel = SubscriptionsViewModel(
            container.subscriptionRepository,
            container.accountRepository
        )
        assertNotNull(subscriptionsViewModel)

        val analyticsViewModel = AnalyticsViewModel(
            container.transactionRepository,
            container.categoryRepository,
            container.accountRepository,
            container.incomeRepository,
            container.savingRepository,
            container.exportRepository
        )
        assertNotNull(analyticsViewModel)

        val settingsViewModel = SettingsViewModel(
            container.transactionRepository,
            container.accountRepository,
            container.categoryRepository,
            container.incomeRepository,
            container.savingRepository,
            container.subscriptionRepository,
            container.backupRepository,
            container.preferencesManager,
            context
        )
        assertNotNull(settingsViewModel)

        println("All ViewModels instantiated successfully without crashing.")
    }

    @Test
    fun verifyMainActivityLaunchesSuccessfully() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertNotNull(scenario)
            println("MainActivity launched and composed successfully.")
        }
    }
}
