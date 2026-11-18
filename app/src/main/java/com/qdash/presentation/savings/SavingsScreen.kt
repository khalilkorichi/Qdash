package com.qdash.presentation.savings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.SavingGoal
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.tokens.ShapeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()

    // Sub-screen navigation state
    var activeGoalForDetails by remember { mutableStateOf<SavingGoal?>(null) }

    BackHandler(enabled = activeGoalForDetails != null) {
        activeGoalForDetails = null
    }

    // Form and action states
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showEditGoalDialog by remember { mutableStateOf<SavingGoal?>(null) }
    var showDepositDialog by remember { mutableStateOf<SavingGoal?>(null) }
    var showWithdrawDialog by remember { mutableStateOf<SavingGoal?>(null) }
    var goalPendingDelete by remember { mutableStateOf<SavingGoal?>(null) }

    LaunchedEffect(activeGoalForDetails?.id, uiState.goals) {
        activeGoalForDetails?.let { current ->
            val updated = uiState.goals.find { it.id == current.id }
            activeGoalForDetails = updated
            if (updated != null) {
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
                    title = "حصالة المدخرات والأهداف",
                    showBackButton = true,
                    onBackClick = onBack,
                    actions = {
                        if (uiState.goals.any { it.isArchived } || uiState.showArchived) {
                            IconButton(onClick = { viewModel.toggleShowArchived() }) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = "الأهداف المؤرشفة",
                                    tint = if (uiState.showArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    uiState.isLoading && uiState.goals.isEmpty() -> {
                        SavingsDashboardSkeleton()
                    }
                    activeGoalForDetails != null -> {
                        val goal = activeGoalForDetails!!
                        GoalDetailsContent(
                            goal = goal,
                            uiState = uiState,
                            viewModel = viewModel,
                            onAddDeposit = { showDepositDialog = goal },
                            onWithdraw = { showWithdrawDialog = goal },
                            onEdit = { showEditGoalDialog = goal },
                            onDeleteClick = { goalPendingDelete = goal }
                        )
                    }
                    else -> {
                        SavingsDashboardContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSelectGoal = { goal ->
                                activeGoalForDetails = goal
                                viewModel.selectGoal(goal.id)
                            },
                            onAddContribution = { goal -> showDepositDialog = goal },
                            onWithdrawSavings = { goal -> showWithdrawDialog = goal },
                            onCreateGoalClick = { showAddGoalDialog = true }
                        )
                    }
                }

                // --- DIALOGS ---

                if (showAddGoalDialog) {
                    AddSavingsGoalDialog(
                        accounts = uiState.accounts,
                        onDismiss = { showAddGoalDialog = false },
                        onConfirm = { name, target, deadline, accountId, color, strategy ->
                            viewModel.addSavingGoal(
                                name = name,
                                targetAmount = target,
                                deadline = deadline,
                                accountId = accountId,
                                color = color,
                                strategy = strategy
                            )
                            showAddGoalDialog = false
                        }
                    )
                }

                showEditGoalDialog?.let { goal ->
                    EditSavingsGoalDialog(
                        goal = goal,
                        accounts = uiState.accounts,
                        onDismiss = { showEditGoalDialog = null },
                        onConfirm = { name, target, deadline, accountId, color, strategy ->
                            viewModel.editSavingGoal(
                                goalId = goal.id,
                                name = name,
                                targetAmount = target,
                                deadline = deadline,
                                accountId = accountId,
                                color = color,
                                strategy = strategy
                            )
                            showEditGoalDialog = null
                            Toast.makeText(context, "تم تحديث الهدف الادخاري بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

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

                goalPendingDelete?.let { targetGoal ->
                    DeleteSavingGoalConfirmDialog(
                        goal = targetGoal,
                        onDismiss = { goalPendingDelete = null },
                        onConfirmDelete = { refundToWallet ->
                            viewModel.deleteGoal(targetGoal.id, refundToWallet = refundToWallet)
                            if (activeGoalForDetails?.id == targetGoal.id) {
                                activeGoalForDetails = null
                            }
                            goalPendingDelete = null
                            val msg = if (refundToWallet && targetGoal.currentAmount > 0.0) {
                                "تم حذف الهدف وإرجاع الرصيد إلى محفظتك"
                            } else {
                                "تم حذف الهدف الادخاري بنجاح"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteSavingGoalConfirmDialog(
    goal: SavingGoal,
    onDismiss: () -> Unit,
    onConfirmDelete: (refundToWallet: Boolean) -> Unit
) {
    var refundToWallet by remember(goal.id) { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "حذف الهدف الادخاري",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "هل أنت متأكد من حذف هدف «${goal.name}» وجميع سجلات الحصالة المرتبطة به؟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (goal.currentAmount > 0.0) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = CardVariant.FLAT,
                            shape = ShapeTokens.Md,
                            onClick = { refundToWallet = !refundToWallet }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = refundToWallet,
                                    onCheckedChange = { refundToWallet = it }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "استرجاع الرصيد المدخر (${FormatterUtils.formatCurrency(goal.currentAmount)}) إلى المحفظة",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "يلغي معاملات الخصم المرتبطة بالحصالة ليستعيد حسابك رصيده الأصلي.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = { onConfirmDelete(refundToWallet) },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.DANGER
            ) {
                Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        }
    )
}
