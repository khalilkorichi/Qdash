package com.qdash.presentation.savings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.SavingGoal
import com.qdash.domain.model.SavingsContribution
import com.qdash.domain.model.SavingsContributionType
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()
    
    // Sub-screen navigation states inside view
    var activeGoalForDetails by remember { mutableStateOf<SavingGoal?>(null) }
    
    // Form and action states
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showEditGoalDialog by remember { mutableStateOf<SavingGoal?>(null) }
    var showDepositDialog by remember { mutableStateOf<SavingGoal?>(null) }
    var showWithdrawDialog by remember { mutableStateOf<SavingGoal?>(null) }

    LaunchedEffect(activeGoalForDetails, uiState.goals) {
        activeGoalForDetails?.let { current ->
            val updated = uiState.goals.find { it.id == current.id }
            if (updated != null) {
                activeGoalForDetails = updated
                viewModel.selectGoal(updated.id)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("savings_screen"),
        topBar = {
            if (activeGoalForDetails != null) {
                FinTrackTopBar(
                    title = activeGoalForDetails!!.name,
                    showBackButton = true,
                    onBackClick = { activeGoalForDetails = null }
                )
            } else {
                FinTrackTopBar(
                    title = "حصالة المدخرات والأهداف المالية",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }
        }
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                SavingsDashboardSkeleton()
            } else if (activeGoalForDetails != null) {
                // GOAL DETAILS SCREEN
                val goal = activeGoalForDetails!!
                GoalDetailsContent(
                    goal = goal,
                    uiState = uiState,
                    viewModel = viewModel,
                    onAddDeposit = { showDepositDialog = goal },
                    onWithdraw = { showWithdrawDialog = goal },
                    onEdit = {
                        showEditGoalDialog = goal
                    }
                )
            } else {
                // SAVINGS OVERVIEW DASHBOARD
                SavingsDashboardContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSelectGoal = { goal ->
                        activeGoalForDetails = goal
                        viewModel.selectGoal(goal.id)
                    },
                    onAddContribution = { goal ->
                        showDepositDialog = goal
                    },
                    onWithdrawSavings = { goal ->
                        showWithdrawDialog = goal
                    },
                    onCreateGoalClick = {
                        showAddGoalDialog = true
                    }
                )
            }

            // --- DIALOGS ---

            // ADD SAVINGS GOAL DIALOG
            if (showAddGoalDialog) {
                AddSavingsGoalDialog(
                    accounts = uiState.accounts,
                    onDismiss = { showAddGoalDialog = false },
                    onConfirm = { name, target, accountId, color, strategy ->
                        viewModel.addSavingGoal(
                            name = name,
                            targetAmount = target,
                            deadline = System.currentTimeMillis() + (180L * 24L * 60L * 60L * 1000L), // Standard 6 months
                            accountId = accountId,
                            color = color,
                            strategy = strategy
                        )
                        showAddGoalDialog = false
                    }
                )
            }

            // EDIT SAVINGS GOAL DIALOG
            showEditGoalDialog?.let { goal ->
                EditSavingsGoalDialog(
                    goal = goal,
                    accounts = uiState.accounts,
                    onDismiss = { showEditGoalDialog = null },
                    onConfirm = { name, target, color ->
                        viewModel.editSavingGoal(
                            goalId = goal.id,
                            name = name,
                            targetAmount = target,
                            deadline = goal.deadline,
                            accountId = goal.accountId,
                            color = color,
                            strategy = viewModel.getGoalStrategy(goal.id)
                        )
                        showEditGoalDialog = null
                    }
                )
            }

            // DEPOSIT DIALOG (ADD CONTRIBUTION)
            showDepositDialog?.let { goal ->
                SavingsDepositDialog(
                    goal = goal,
                    accounts = uiState.accounts,
                    onDismiss = { showDepositDialog = null },
                    onConfirm = { amount, note, accountId, date ->
                        viewModel.addDeposit(
                            goalId = goal.id,
                            accountId = accountId,
                            amount = amount,
                            note = note,
                            date = date
                        )
                        showDepositDialog = null
                    }
                )
            }

            // WITHDRAW DIALOG (WITHDRAW SAVINGS)
            showWithdrawDialog?.let { goal ->
                SavingsWithdrawDialog(
                    goal = goal,
                    accounts = uiState.accounts,
                    onDismiss = { showWithdrawDialog = null },
                    onConfirm = { amount, note, accountId, date ->
                        viewModel.addWithdrawal(
                            goalId = goal.id,
                            accountId = accountId,
                            amount = amount,
                            note = note,
                            date = date
                        )
                        showWithdrawDialog = null
                    }
                )
            }
        }
        } // end PullToRefreshBox
    }
}

