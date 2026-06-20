package com.example.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.core.di.AppContainer
import com.example.presentation.ViewModelFactory
import com.example.presentation.accounts.AccountsScreen
import com.example.presentation.accounts.AccountsViewModel
import com.example.presentation.analytics.AnalyticsScreen
import com.example.presentation.analytics.AnalyticsViewModel
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.savings.SavingsScreen
import com.example.presentation.savings.SavingsViewModel
import com.example.presentation.debt.DebtViewModel
import com.example.presentation.debt.DebtsScreen
import com.example.presentation.transfer.TransferViewModel
import com.example.presentation.transfer.TransferScreen
import com.example.presentation.export.ExportViewModel
import com.example.presentation.export.ExportScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.subscriptions.SubscriptionsScreen
import com.example.presentation.subscriptions.SubscriptionsViewModel
import com.example.presentation.transactions.AddTransactionScreen
import com.example.presentation.transactions.TransactionsScreen
import com.example.presentation.transactions.IncomeHistoryScreen
import com.example.presentation.transactions.TransactionsViewModel
import com.example.presentation.notifications.NotificationsScreen
import com.example.presentation.notifications.NotificationsViewModel
import com.example.presentation.search.SearchScreen
import com.example.presentation.search.SearchViewModel
import com.example.presentation.categories.CategoriesScreen
import com.example.presentation.categories.CategoriesViewModel
import com.example.presentation.plans.FinancialPlansScreen
import com.example.presentation.plans.FinancialPlansViewModel
import com.example.presentation.templates.TemplatesViewModel
import com.example.presentation.templates.TemplatesScreen
import com.example.presentation.templates.CreateEditTemplateScreen
import com.example.presentation.simulator.DocumentSimulatorEntryScreen
import com.example.presentation.simulator.DocumentSimulatorScreen
import com.example.presentation.simulator.PostalProfilesScreen
import com.example.presentation.simulator.CreateEditPostalProfileScreen
import com.example.presentation.simulator.DocumentSimulatorViewModel
import com.example.presentation.simulator.DocumentType
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * All composable() navigation registrations for the app.
 * Extracted from MainActivity — exact same behavior.
 */
@Composable
internal fun FinTrackNavGraph(
    navController: NavHostController,
    factory: ViewModelFactory,
    container: AppContainer,
    settingsViewModel: SettingsViewModel,
    startDestination: String,
    updatesViewModel: com.example.presentation.update.UpdatesViewModel,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 6 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 6 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 6 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 6 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200))
        }
    ) {
        composable(Screen.Onboarding.route) {
            val onboardingViewModel: com.example.presentation.onboarding.OnboardingViewModel =
                viewModel(factory = factory)
            val context = androidx.compose.ui.platform.LocalContext.current
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onFinished = {
                    scope.launch {
                        val welcomeNotification = com.example.domain.model.AppNotification(
                            id = 10001L,
                            title = context.getString(com.example.R.string.welcome_title),
                            message = context.getString(com.example.R.string.welcome_desc),
                            type = com.example.domain.model.NotificationType.TIP,
                            isRead = false,
                            timestamp = System.currentTimeMillis(),
                            deepLinkRoute = Screen.Home.route,
                            relatedEntityId = null
                        )
                        container.notificationRepository.insertNotification(welcomeNotification)
                        com.example.core.utils.SystemNotificationHelper.showNotification(
                            context = context,
                            appNotification = welcomeNotification
                        )
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = homeViewModel,
                onAddTransactionClick = {
                    navController.navigate(Screen.AddTransaction.createRoute("EXPENSE"))
                },
                onViewAllTransactionsClick = {
                    navController.navigate(Screen.Transactions.route)
                },
                onViewAllIncomeClick = {
                    navController.navigate(Screen.IncomeHistory.route)
                },
                onAccountClick = {
                    navController.navigate(Screen.Accounts.route)
                },
                onSavingsClick = {
                    navController.navigate(Screen.Savings.route)
                },
                onSubscriptionsClick = {
                    navController.navigate(Screen.Subscriptions.route)
                },
                onBudgetGoalsClick = {
                    navController.navigate(Screen.BudgetGoals.route)
                },
                onAddExpenseClick = {
                    navController.navigate(Screen.AddTransaction.createRoute("EXPENSE"))
                },
                onAddIncomeClick = {
                    navController.navigate(Screen.AddTransaction.createRoute("INCOME"))
                },
                onTransferClick = {
                    navController.navigate(Screen.Transfer.route)
                },
                onAddDebtClick = {
                    navController.navigate(Screen.Debts.route)
                },
                onNotificationClick = {
                    navController.navigate(Screen.Notifications.route)
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onDocumentSimulatorClick = {
                    navController.navigate(Screen.DocumentSimulatorEntry.route)
                }
            )
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                androidx.navigation.navArgument("type") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = "EXPENSE"
                },
                androidx.navigation.navArgument("transactionId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("date") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                androidx.navigation.navArgument("draft") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val transactionType = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull()
            val transactionDate = backStackEntry.arguments?.getString("date")?.toLongOrNull()
            val draftJson = backStackEntry.arguments?.getString("draft")
            val transactionsViewModel: TransactionsViewModel = viewModel(factory = factory)
            AddTransactionScreen(
                viewModel = transactionsViewModel,
                initialType = transactionType,
                transactionId = transactionId,
                initialDate = transactionDate,
                draftJson = draftJson,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Transactions.route) {
            val transactionsViewModel: TransactionsViewModel = viewModel(factory = factory)
            TransactionsScreen(
                viewModel = transactionsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Accounts.route) {
            val accountsViewModel: AccountsViewModel = viewModel(factory = factory)
            AccountsScreen(
                viewModel = accountsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            val analyticsViewModel: AnalyticsViewModel = viewModel(factory = factory)
            AnalyticsScreen(
                viewModel = analyticsViewModel
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToBudgetGoals = { navController.navigate(Screen.BudgetGoals.route) },
                onNavigateToDebts = { navController.navigate(Screen.Debts.route) },
                onNavigateToTransfer = { navController.navigate(Screen.Transfer.route) },
                onNavigateToExport = { navController.navigate(Screen.Export.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToFinancialPlans = { navController.navigate(Screen.FinancialPlans.route) },
                onNavigateToSalary = { navController.navigate(Screen.Salary.route) }
            )
        }

        composable(Screen.Savings.route) {
            val savingsViewModel: SavingsViewModel = viewModel(factory = factory)
            SavingsScreen(
                viewModel = savingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Subscriptions.route) {
            val subscriptionsViewModel: SubscriptionsViewModel = viewModel(factory = factory)
            SubscriptionsScreen(
                viewModel = subscriptionsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Debts.route) {
            val debtViewModel: DebtViewModel = viewModel(factory = factory)
            DebtsScreen(viewModel = debtViewModel)
        }

        composable(Screen.Transfer.route) {
            val transferViewModel: TransferViewModel = viewModel(factory = factory)
            TransferScreen(viewModel = transferViewModel)
        }

        composable(Screen.Export.route) {
            val exportViewModel: ExportViewModel = viewModel(factory = factory)
            ExportScreen(viewModel = exportViewModel)
        }

        composable(Screen.BudgetGoals.route) {
            val budgetGoalsViewModel: com.example.presentation.budgetgoals.BudgetGoalsViewModel =
                viewModel(factory = factory)
            com.example.presentation.budgetgoals.BudgetGoalsScreen(
                viewModel = budgetGoalsViewModel,
                onNavigateToAdd = { navController.navigate(Screen.AddBudgetGoal.route) },
                onNavigateToDetails = { id ->
                    navController.navigate(Screen.BudgetGoalDetails.createRoute(id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Salary.route) {
            val salaryViewModel =
                viewModel<com.example.presentation.salary.SalaryViewModel>(factory = factory)
            com.example.presentation.salary.SalaryScreen(
                viewModel = salaryViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddBudgetGoal.route) {
            val budgetGoalsViewModel: com.example.presentation.budgetgoals.BudgetGoalsViewModel =
                viewModel(factory = factory)
            com.example.presentation.budgetgoals.AddBudgetGoalScreen(
                viewModel = budgetGoalsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BudgetGoalDetails.route,
            arguments = listOf(
                androidx.navigation.navArgument("budgetId") {
                    type = androidx.navigation.NavType.LongType
                }
            )
        ) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: 0L
            val budgetGoalsViewModel: com.example.presentation.budgetgoals.BudgetGoalsViewModel =
                viewModel(factory = factory)
            com.example.presentation.budgetgoals.BudgetGoalDetailsScreen(
                budgetId = budgetId,
                viewModel = budgetGoalsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            val notificationsViewModel: NotificationsViewModel = viewModel(factory = factory)
            NotificationsScreen(
                viewModel = notificationsViewModel,
                onNavigateTo = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiChat.route) {
            val aiChatViewModel: com.example.presentation.ai.AiChatViewModel = viewModel(factory = factory)
            com.example.presentation.ai.AiChatScreen(
                viewModel = aiChatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            val searchViewModel: SearchViewModel = viewModel(factory = factory)
            SearchScreen(
                viewModel = searchViewModel,
                onTransactionClick = { navController.navigate(Screen.Transactions.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Categories.route) {
            val categoriesViewModel: CategoriesViewModel = viewModel(factory = factory)
            CategoriesScreen(
                viewModel = categoriesViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FinancialPlans.route) {
            val financialPlansViewModel: FinancialPlansViewModel = viewModel(factory = factory)
            FinancialPlansScreen(
                viewModel = financialPlansViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Templates.route) {
            val templatesViewModel: TemplatesViewModel = viewModel(factory = factory)
            TemplatesScreen(
                viewModel = templatesViewModel,
                onNavigateToCreate = { navController.navigate(Screen.CreateTemplate.route) },
                onNavigateToEdit = { id -> navController.navigate(Screen.EditTemplate.createRoute(id)) },
                onNavigateToPreFill = { json ->
                    navController.navigate(Screen.AddTransaction.createRoute("EXPENSE", draft = json))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateTemplate.route) {
            val templatesViewModel: TemplatesViewModel = viewModel(factory = factory)
            CreateEditTemplateScreen(
                viewModel = templatesViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTemplate.route,
            arguments = listOf(
                androidx.navigation.navArgument("templateId") {
                    type = androidx.navigation.NavType.LongType
                }
            )
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong("templateId") ?: 0L
            val templatesViewModel: TemplatesViewModel = viewModel(factory = factory)
            CreateEditTemplateScreen(
                viewModel = templatesViewModel,
                templateId = templateId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Backup.route) {
            val backupViewModel: com.example.presentation.backup.BackupViewModel =
                viewModel(factory = factory)
            com.example.presentation.backup.BackupScreen(
                viewModel = backupViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.IncomeHistory.route) {
            val transactionsViewModel: TransactionsViewModel = viewModel(factory = factory)
            IncomeHistoryScreen(
                viewModel = transactionsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Updates.route) {
            com.example.presentation.update.UpdatesScreen(
                viewModel = updatesViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DocumentSimulatorEntry.route) {
            val simulatorViewModel: DocumentSimulatorViewModel = viewModel(factory = factory)
            DocumentSimulatorEntryScreen(
                onSelectDocType = { docType ->
                    simulatorViewModel.selectDocumentType(docType)
                    navController.navigate(Screen.DocumentSimulator.route)
                },
                onManageProfiles = {
                    navController.navigate(Screen.PostalProfiles.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DocumentSimulator.route) {
            val simulatorViewModel: DocumentSimulatorViewModel = viewModel(factory = factory)
            DocumentSimulatorScreen(
                viewModel = simulatorViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PostalProfiles.route) {
            val simulatorViewModel: DocumentSimulatorViewModel = viewModel(factory = factory)
            PostalProfilesScreen(
                viewModel = simulatorViewModel,
                onAddProfile = {
                    navController.navigate(Screen.CreateEditPostalProfile.createRoute())
                },
                onEditProfile = { id ->
                    navController.navigate(Screen.CreateEditPostalProfile.createRoute(id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreateEditPostalProfile.route,
            arguments = listOf(
                androidx.navigation.navArgument("profileId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val profileIdStr = backStackEntry.arguments?.getString("profileId")
            val profileId = profileIdStr?.toLongOrNull()
            val simulatorViewModel: DocumentSimulatorViewModel = viewModel(factory = factory)
            CreateEditPostalProfileScreen(
                viewModel = simulatorViewModel,
                profileId = profileId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
