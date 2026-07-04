package com.qdash.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.qdash.core.di.AppContainer
import com.qdash.presentation.ViewModelFactory
import com.qdash.presentation.accounts.AccountsScreen
import com.qdash.presentation.accounts.AccountsViewModel
import com.qdash.presentation.analytics.AnalyticsScreen
import com.qdash.presentation.analytics.AnalyticsViewModel
import com.qdash.presentation.analytics.CardAiChatViewModel
import com.qdash.presentation.home.HomeScreen
import com.qdash.presentation.home.HomeViewModel
import com.qdash.presentation.onboarding.OnboardingScreen
import com.qdash.presentation.savings.SavingsScreen
import com.qdash.presentation.savings.SavingsViewModel
import com.qdash.presentation.debt.DebtViewModel
import com.qdash.presentation.debt.DebtsScreen
import com.qdash.presentation.transfer.TransferViewModel
import com.qdash.presentation.transfer.TransferScreen
import com.qdash.presentation.export.ExportViewModel
import com.qdash.presentation.export.ExportScreen
import com.qdash.presentation.settings.SettingsScreen
import com.qdash.presentation.settings.SettingsViewModel
import com.qdash.presentation.settings.AccountManagementScreen
import com.qdash.presentation.settings.AccountManagementViewModel
import com.qdash.presentation.splash.SplashScreen
import com.qdash.presentation.subscriptions.SubscriptionsScreen
import com.qdash.presentation.subscriptions.SubscriptionsViewModel
import com.qdash.presentation.transactions.AddTransactionScreen
import com.qdash.presentation.transactions.TransactionsScreen
import com.qdash.presentation.transactions.IncomeHistoryScreen
import com.qdash.presentation.transactions.TransactionsViewModel
import com.qdash.presentation.notifications.NotificationsScreen
import com.qdash.presentation.notifications.NotificationsViewModel
import com.qdash.presentation.search.SearchScreen
import com.qdash.presentation.search.SearchViewModel
import com.qdash.presentation.categories.CategoriesScreen
import com.qdash.presentation.categories.CategoriesViewModel
import com.qdash.presentation.plans.FinancialPlansScreen
import com.qdash.presentation.plans.FinancialPlansViewModel
import com.qdash.presentation.templates.TemplatesViewModel
import com.qdash.presentation.templates.TemplatesScreen
import com.qdash.presentation.templates.CreateEditTemplateScreen
import com.qdash.presentation.simulator.DocumentSimulatorEntryScreen
import com.qdash.presentation.simulator.DocumentSimulatorScreen
import com.qdash.presentation.simulator.PostalProfilesScreen
import com.qdash.presentation.simulator.CreateEditPostalProfileScreen
import com.qdash.presentation.simulator.DocumentSimulatorViewModel
import com.qdash.presentation.simulator.DocumentType
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
    updatesViewModel: com.qdash.presentation.update.UpdatesViewModel,
    aiChatViewModel: com.qdash.presentation.ai.AiChatViewModel,
    scope: CoroutineScope,
    isFirstLaunch: Boolean,
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
            val initial = initialState.destination.route
            val target = targetState.destination.route
            if (isBottomNavRoute(initial) && isBottomNavRoute(target)) {
                fadeIn(animationSpec = tween(220))
            } else {
                slideInHorizontally(
                    initialOffsetX = { -it / 6 }, // RTL-aware slide in from left to right
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            }
        },
        exitTransition = {
            val initial = initialState.destination.route
            val target = targetState.destination.route
            if (isBottomNavRoute(initial) && isBottomNavRoute(target)) {
                fadeOut(animationSpec = tween(220))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { it / 6 }, // RTL-aware slide out to right
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            }
        },
        popEnterTransition = {
            val initial = initialState.destination.route
            val target = targetState.destination.route
            if (isBottomNavRoute(initial) && isBottomNavRoute(target)) {
                fadeIn(animationSpec = tween(220))
            } else {
                slideInHorizontally(
                    initialOffsetX = { it / 6 }, // RTL-aware slide in from right to left
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            }
        },
        popExitTransition = {
            val initial = initialState.destination.route
            val target = targetState.destination.route
            if (isBottomNavRoute(initial) && isBottomNavRoute(target)) {
                fadeOut(animationSpec = tween(220))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { -it / 6 }, // RTL-aware slide out to left
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            }
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    val target = if (isFirstLaunch) Screen.Onboarding.route else Screen.Home.route
                    navController.navigate(target) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            val onboardingViewModel: com.qdash.presentation.onboarding.OnboardingViewModel =
                viewModel(factory = factory)
            val context = androidx.compose.ui.platform.LocalContext.current
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onFinished = {
                    scope.launch {
                        val welcomeNotification = com.qdash.domain.model.AppNotification(
                            id = 10001L,
                            title = context.getString(com.qdash.R.string.welcome_title),
                            message = context.getString(com.qdash.R.string.welcome_desc),
                            type = com.qdash.domain.model.NotificationType.TIP,
                            isRead = false,
                            timestamp = System.currentTimeMillis(),
                            deepLinkRoute = Screen.Home.route,
                            relatedEntityId = null
                        )
                        container.notificationRepository.insertNotification(welcomeNotification)
                        com.qdash.core.utils.SystemNotificationHelper.showNotification(
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
            val cardAiChatViewModel: CardAiChatViewModel = viewModel(factory = factory)
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                cardAiChatViewModel = cardAiChatViewModel
            )
        }

        composable(Screen.Settings.route) {
            val backupViewModel: com.qdash.presentation.backup.BackupViewModel =
                viewModel(factory = factory)
            SettingsScreen(
                viewModel = settingsViewModel,
                backupViewModel = backupViewModel,
                onNavigateToBudgetGoals = { navController.navigate(Screen.BudgetGoals.route) },
                onNavigateToDebts = { navController.navigate(Screen.Debts.route) },
                onNavigateToTransfer = { navController.navigate(Screen.Transfer.route) },
                onNavigateToExport = { navController.navigate(Screen.Export.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToFinancialPlans = { navController.navigate(Screen.FinancialPlans.route) },
                onNavigateToSalary = { navController.navigate(Screen.Salary.route) },
                onNavigateToAccountManagement = { navController.navigate(Screen.AccountManagement.route) }
            )
        }

        composable(Screen.AccountManagement.route) {
            val accountViewModel: AccountManagementViewModel = viewModel(factory = factory)
            AccountManagementScreen(
                viewModel = accountViewModel,
                onBack = { navController.popBackStack() }
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
            val budgetGoalsViewModel: com.qdash.presentation.budgetgoals.BudgetGoalsViewModel =
                viewModel(factory = factory)
            com.qdash.presentation.budgetgoals.BudgetGoalsScreen(
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
                viewModel<com.qdash.presentation.salary.SalaryViewModel>(factory = factory)
            com.qdash.presentation.salary.SalaryScreen(
                viewModel = salaryViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddBudgetGoal.route) {
            val budgetGoalsViewModel: com.qdash.presentation.budgetgoals.BudgetGoalsViewModel =
                viewModel(factory = factory)
            com.qdash.presentation.budgetgoals.AddBudgetGoalScreen(
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
            val budgetGoalsViewModel: com.qdash.presentation.budgetgoals.BudgetGoalsViewModel =
                viewModel(factory = factory)
            com.qdash.presentation.budgetgoals.BudgetGoalDetailsScreen(
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
            val initialMessage = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("ai_initial_message")
            com.qdash.presentation.ai.AiChatScreen(
                viewModel = aiChatViewModel,
                onBack = { navController.popBackStack() },
                onVoiceInput = { navController.navigate(Screen.AiVoice.route) },
                initialMessage = initialMessage
            )
            LaunchedEffect(initialMessage) {
                if (!initialMessage.isNullOrBlank()) {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String>("ai_initial_message")
                }
            }
        }

        composable(Screen.AiVoice.route) {
            com.qdash.presentation.ai.AiVoiceInputScreen(
                viewModel = aiChatViewModel,
                onNavigateToChat = { initialText ->
                    if (initialText.isNotBlank()) {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("ai_initial_message", initialText)
                    }
                    navController.navigate(Screen.AiChat.route)
                },
                onClose = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
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
            val backupViewModel: com.qdash.presentation.backup.BackupViewModel =
                viewModel(factory = factory)
            com.qdash.presentation.backup.BackupScreen(
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
            com.qdash.presentation.update.UpdatesScreen(
                viewModel = updatesViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DocumentSimulatorEntry.route) {
            val simulatorViewModel: DocumentSimulatorViewModel = viewModel(factory = factory)
            DocumentSimulatorEntryScreen(
                viewModel = simulatorViewModel,
                onSelectDocType = { docType ->
                    navController.navigate(Screen.DocumentSimulator.createRoute(docType.name))
                },
                onManageProfiles = {
                    navController.navigate(Screen.PostalProfiles.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DocumentSimulator.route,
            arguments = listOf(
                androidx.navigation.navArgument("docType") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val simulatorViewModel: DocumentSimulatorViewModel = viewModel(factory = factory)
            val docType = backStackEntry.arguments?.getString("docType")
                ?.let { runCatching { DocumentType.valueOf(it) }.getOrNull() }
                ?: DocumentType.CHEQUE
            LaunchedEffect(docType) {
                simulatorViewModel.selectDocumentType(docType)
            }
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

private fun isBottomNavRoute(route: String?): Boolean {
    if (route == null) return false
    val base = route.substringBefore("?").substringBefore("/")
    return base == "home" || base == "analytics" || base == "accounts" || base == "settings"
}
