package com.qdash.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.presentation.home.components.*
import com.qdash.presentation.navigation.LocalNavController
import com.qdash.presentation.navigation.Screen
import com.qdash.domain.model.CategoryType
import androidx.compose.ui.text.font.FontWeight
import com.qdash.core.ui.components.SubscriptionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddTransactionClick: () -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onViewAllIncomeClick: () -> Unit = {},
    onAccountClick: (Long) -> Unit,
    onSavingsClick: () -> Unit,
    onSubscriptionsClick: () -> Unit,
    onBudgetGoalsClick: () -> Unit,
    onAddExpenseClick: () -> Unit = {},
    onAddIncomeClick: () -> Unit = {},
    onTransferClick: () -> Unit = {},
    onAddDebtClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onDocumentSimulatorClick: () -> Unit = {},
    unreadCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()
    val navController = LocalNavController.current

    var showBalanceDetails by remember { mutableStateOf(false) }
    val showBalances = uiState.showBalances
    val shouldShowReminder = uiState.showWalletReminder && uiState.accounts.none { it.balance > 0.0 }

    Box(modifier = Modifier.fillMaxSize()) {
        val blurRadius by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (showBalanceDetails) 15.dp else 0.dp,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
            label = "blur"
        )

        // Stable lambda references — wrapped in remember so child composables
        // are not recomposed when HomeScreen recomposes for unrelated state changes.
        val onRefresh = remember(viewModel) { { viewModel.refresh() } }
        val onToggleShowBalances = remember(viewModel) { { viewModel.toggleShowBalances() } }
        val onDismissReminder = remember(viewModel) { { viewModel.dismissWalletReminder() } }
        val onToggleAccountBalance = remember(viewModel) { { id: Long -> viewModel.toggleAccountBalanceVisibility(id) } }
        val onPeriodSelected = remember(viewModel) { { period: String -> viewModel.setChartPeriod(period) } }

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                // conditionalBlur: RenderThread on API 31+, no-op on API 24-30
                .conditionalBlur(blurRadius)
                .testTag("home_screen"),
            floatingActionButton = {} // Radial FAB is overlaid manually by the shell container
        ) { innerPadding ->

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                val sections = uiState.visibleSections

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(bottom = innerPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp) // Space for radial FAB
                ) {

                    // ── Premium Top Card ──
                    item {
                        PremiumTopBalanceCard(
                            totalBalance = uiState.totalBalance,
                            monthlyIncome = uiState.monthlyIncome,
                            monthlyExpense = uiState.monthlyExpense,
                            unreadCount = unreadCount,
                            onNotificationClick = onNotificationClick,
                            onSearchClick = onSearchClick,
                            onAddTransactionClick = onAddTransactionClick,
                            onTransferClick = onTransferClick,
                            onAddIncomeClick = onAddIncomeClick,
                            onCardClick = { showBalanceDetails = true },
                            showBalances = showBalances,
                            onToggleShowBalances = { viewModel.toggleShowBalances() }
                        )
                    }

                    // ── Setup Reminder (if needed) ──
                    if (shouldShowReminder) {
                        item {
                            WalletSetupReminderCard(
                                onDismiss = onDismissReminder
                            )
                        }
                    }

                    // ── Customizable Dashboard Sections ──
                    sections.forEach { section ->
                        when (section) {
                            "split_cards" -> {
                                item {
                                    IncomeExpenseSplitCards(
                                        monthlyIncome = uiState.monthlyIncome,
                                        monthlyExpense = uiState.monthlyExpense,
                                        incomeChangePercent = uiState.incomeChangePercent,
                                        expenseChangePercent = uiState.expenseChangePercent,
                                        accounts = uiState.accounts,
                                        recentTransactions = uiState.recentTransactions,
                                        onIncomeShowAllClick = onViewAllIncomeClick,
                                        onExpenseShowAllClick = onViewAllTransactionsClick,
                                        showBalances = showBalances,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                            "context_templates" -> {
                                item {
                                    ContextAwareTemplateCard(
                                        categories = uiState.categories,
                                        aiTimeSuggestion = uiState.aiTimeSuggestion,
                                        onSelectTemplate = { json ->
                                            navController?.navigate(Screen.AddTransaction.createRoute("EXPENSE", draft = json))
                                        }
                                    )
                                }
                                if (uiState.proactiveInsights.isNotEmpty()) {
                                    item {
                                        AiProactiveInsightsSection(insights = uiState.proactiveInsights)
                                    }
                                }
                            }
                            "templates" -> {
                                if (uiState.pinnedTemplates.isNotEmpty()) {
                                    item {
                                        PinnedTemplatesSection(
                                            pinnedTemplates = uiState.pinnedTemplates,
                                            onManageTemplatesClick = { navController?.navigate(Screen.Templates.route) },
                                            onTemplateSelect = { json ->
                                                navController?.navigate(Screen.AddTransaction.createRoute("EXPENSE", draft = json))
                                            }
                                        )
                                    }
                                }
                            }
                            "quick_actions" -> {
                                item {
                                    QuickActionsSection(
                                        onAddTransactionClick = onAddTransactionClick,
                                        onViewAllTransactionsClick = onViewAllTransactionsClick,
                                        onSavingsClick = onSavingsClick,
                                        onSubscriptionsClick = onSubscriptionsClick,
                                        onDocumentSimulatorClick = onDocumentSimulatorClick,
                                        onAiAssistantClick = { navController?.navigate(Screen.AiChat.route) }
                                    )
                                }
                            }
                            "accounts" -> {
                                item {
                                    HomeAccountsSection(
                                        accounts = uiState.accounts,
                                        accountBalancesVisibility = uiState.accountBalancesVisibility,
                                        isLoading = uiState.isLoading,
                                        onToggleBalanceVisibility = onToggleAccountBalance,
                                        onAccountClick = onAccountClick
                                    )
                                }
                            }
                            "chart" -> {
                                item {
                                    ExpenseAnalysisChartCard(
                                        expenseTrendData = uiState.expenseTrendData,
                                        chartPeriod = uiState.chartPeriod,
                                        isLoading = uiState.isLoading,
                                        onPeriodSelected = onPeriodSelected
                                    )
                                }
                            }
                            "budget" -> {
                                item {
                                    val budgetCat = uiState.categories.firstOrNull {
                                        it.type == CategoryType.EXPENSE && it.budgetLimit != null
                                    }
                                    val matchingSpent = if (budgetCat != null) {
                                        uiState.recentTransactions
                                            .filter { it.categoryId == budgetCat.id }
                                            .sumOf { it.amount }
                                    } else 0.0

                                    BudgetProgressSection(
                                        budgetCategory = budgetCat,
                                        spentAmount = matchingSpent,
                                        onBudgetGoalsClick = onBudgetGoalsClick
                                    )
                                }
                            }
                            "subscriptions" -> {
                                if (uiState.upcomingSubscriptions.isNotEmpty()) {
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                                Text(
                                                    text = "تذكيرات الاشتراكات القادمة",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                uiState.upcomingSubscriptions.take(2).forEach { sub ->
                                                    val linkedAcc = uiState.accounts
                                                        .firstOrNull { it.id == sub.accountId }?.name ?: "غير محدد"
                                                    SubscriptionItem(
                                                        subscription = sub,
                                                        onToggleActive = {},
                                                        accountName = linkedAcc
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "recent_transactions" -> {
                                homeRecentTransactionsSection(
                                    recentTransactions = uiState.recentTransactions,
                                    categories = uiState.categories,
                                    accounts = uiState.accounts,
                                    isLoading = uiState.isLoading,
                                    onViewAllTransactionsClick = onViewAllTransactionsClick,
                                    primaryColor = primary
                                )
                            }
                        }
                    }
                }
            } // end PullToRefreshBox
        } // end Scaffold

        // ── Available Balance Detailed Modal ──
        BalanceDetailsDialog(
            showBalanceDetails = showBalanceDetails,
            totalBalance = uiState.totalBalance,
            showBalances = showBalances,
            accounts = uiState.accounts,
            accountBalancesVisibility = uiState.accountBalancesVisibility,
            onToggleShowBalances = onToggleShowBalances,
            onToggleAccountBalanceVisibility = onToggleAccountBalance,
            onAccountClick = onAccountClick,
            onDismiss = { showBalanceDetails = false },
            primaryColor = primary
        )
    } // end root Box
}
