package com.qdash.presentation.debt

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.*
import com.qdash.presentation.debt.components.*
import com.qdash.ui.designsystem.components.AppBottomSheet
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.components.debt.DebtSkeleton
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.SavingsAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: DebtViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    // Sub-screen navigation
    var activeDebtForDetails by remember { mutableStateOf<Debt?>(null) }

    BackHandler(enabled = activeDebtForDetails != null) {
        activeDebtForDetails = null
    }

    // Dialog & Sheet visibility states
    var showAddDebtChooserSheet by remember { mutableStateOf(false) }
    var showAddRegularDebtDialog by remember { mutableStateOf(false) }
    var showAddInstallmentDebtDialog by remember { mutableStateOf(false) }
    var showAddLentDebtDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf<Debt?>(null) }
    var showEditDebtBottomSheet by remember { mutableStateOf<Debt?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Debt?>(null) }
    var showForgiveConfirmDialog by remember { mutableStateOf<Debt?>(null) }
    var showPaymentsHistoryBottomSheet by remember { mutableStateOf<Debt?>(null) }
    var showCancelPaymentConfirmDialog by remember { mutableStateOf<DebtPayment?>(null) }

    LaunchedEffect(activeDebtForDetails?.id, uiState.debts) {
        activeDebtForDetails?.let { current ->
            val updated = uiState.debts.find { it.id == current.id }
            activeDebtForDetails = updated
            if (updated != null) viewModel.selectDebt(updated.id)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("debts_screen"),
        topBar = {
            if (activeDebtForDetails != null) {
                FinTrackTopBar(
                    title = activeDebtForDetails!!.title,
                    showBackButton = true,
                    onBackClick = { activeDebtForDetails = null }
                )
            } else {
                FinTrackTopBar(
                    title = "خطة الديون والالتزامات",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
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
                    uiState.isLoading && uiState.debts.isEmpty() -> {
                        DebtSkeleton()
                    }
                    activeDebtForDetails != null -> {
                        val debt = activeDebtForDetails!!
                        DebtDetailsContent(
                            debt = debt,
                            accounts = uiState.accounts,
                            selectedDebtPayments = uiState.selectedDebtPayments,
                            onPayClick = { showPaymentDialog = debt },
                            onEditClick = { d -> showEditDebtBottomSheet = d },
                            onDeleteClick = { d -> showDeleteConfirmDialog = d },
                            onForgiveClick = { d -> showForgiveConfirmDialog = d },
                            onPaymentsHistoryClick = { d ->
                                viewModel.selectDebt(d.id)
                                showPaymentsHistoryBottomSheet = d
                            },
                            onCancelPaymentClick = { payment -> showCancelPaymentConfirmDialog = payment }
                        )
                    }
                    else -> {
                        DebtsMainContent(
                            debts = uiState.debts,
                            orderedPlanDebts = uiState.orderedPlanDebts,
                            insights = uiState.insights,
                            strategyResults = uiState.strategyComparison,
                            selectedStrategy = uiState.selectedStrategy,
                            selectedDirection = uiState.selectedDirection,
                            onDirectionChange = { viewModel.selectDirection(it) },
                            onStrategyChange = { viewModel.selectStrategy(it) },
                            onSelectDebt = { d ->
                                activeDebtForDetails = d
                                viewModel.selectDebt(d.id)
                            },
                            onPayClick = { d -> showPaymentDialog = d },
                            onAddPrimaryClick = { preferredFilter ->
                                if (uiState.selectedDirection == DebtDirection.OWED_TO_ME) {
                                    showAddLentDebtDialog = true
                                } else {
                                    when (preferredFilter) {
                                        DebtListFilter.INSTALLMENT -> showAddInstallmentDebtDialog = true
                                        DebtListFilter.REGULAR -> showAddRegularDebtDialog = true
                                        else -> showAddDebtChooserSheet = true
                                    }
                                }
                            }
                        )
                    }
                }

                // --- ADD OBLIGATION TYPE CHOOSER SHEET ---
                if (showAddDebtChooserSheet) {
                    AppBottomSheet(
                        onDismissRequest = { showAddDebtChooserSheet = false }
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 4.dp, bottom = 28.dp)
                                    .navigationBarsPadding(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "إضافة التزام مالي جديد",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "اختر نوع الالتزام ليتم إدراجه تلقائياً في خطة السداد الذكية:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                AppCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = CardVariant.SOLID,
                                    shape = ShapeTokens.Lg,
                                    backgroundColor = MaterialTheme.colorScheme.surface,
                                    onClick = {
                                        showAddDebtChooserSheet = false
                                        showAddInstallmentDebtDialog = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(ExpenseRed.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = ExpenseRed
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "قرض أو أقساط مجدولة",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "تمويل بنكي أو تقسيط تجاري بقسط شهري محدد وأولوية سداد.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                AppCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = CardVariant.SOLID,
                                    shape = ShapeTokens.Lg,
                                    backgroundColor = MaterialTheme.colorScheme.surface,
                                    onClick = {
                                        showAddDebtChooserSheet = false
                                        showAddRegularDebtDialog = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(SavingsAmber.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Handshake,
                                                contentDescription = null,
                                                tint = SavingsAmber
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "دين مرن / سلفة شخصية",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "دين عائلي أو شخصي بدون فوائد يُسدد على دفعات مرنة.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- DIALOGS ---

                if (showAddRegularDebtDialog) {
                    AddRegularDebtDialog(
                        accounts = uiState.accounts,
                        onDismissRequest = { showAddRegularDebtDialog = false },
                        onConfirm = { title, creditorName, totalAmount, linkedAccountId, notes, color, dueDate ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addDebt(
                                title = title,
                                creditorName = creditorName,
                                totalAmount = totalAmount,
                                minimumPayment = 0.0,
                                paymentFrequency = "NONE",
                                linkedAccountId = linkedAccountId,
                                priority = 3,
                                notes = notes,
                                color = color,
                                dueDate = dueDate,
                                debtType = DebtType.REGULAR
                            )
                            showAddRegularDebtDialog = false
                        }
                    )
                }

                if (showAddInstallmentDebtDialog) {
                    AddInstallmentDebtDialog(
                        accounts = uiState.accounts,
                        onDismissRequest = { showAddInstallmentDebtDialog = false },
                        onConfirm = { title, creditorName, totalAmount, minimumPayment, paymentFrequency, linkedAccountId, priority, notes, color, interestRate, dueDate ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addDebt(
                                title = title,
                                creditorName = creditorName,
                                totalAmount = totalAmount,
                                minimumPayment = minimumPayment,
                                paymentFrequency = paymentFrequency,
                                linkedAccountId = linkedAccountId,
                                priority = priority,
                                notes = notes,
                                color = color,
                                interestRate = interestRate,
                                dueDate = dueDate,
                                debtType = DebtType.INSTALLMENT
                            )
                            showAddInstallmentDebtDialog = false
                        }
                    )
                }

                if (showAddLentDebtDialog) {
                    AddLentDebtDialog(
                        accounts = uiState.accounts,
                        onDismissRequest = { showAddLentDebtDialog = false },
                        onConfirm = { title, debtorName, totalAmount, linkedAccountId, dueDate, notes, color ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addLentDebt(
                                title = title,
                                debtorName = debtorName,
                                totalAmount = totalAmount,
                                linkedAccountId = linkedAccountId,
                                dueDate = dueDate,
                                notes = notes,
                                color = color
                            )
                            showAddLentDebtDialog = false
                            Toast.makeText(context, "تم تسجيل السلفة وخصم المبلغ من المحفظة بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                showPaymentDialog?.let { debt ->
                    if (debt.direction == DebtDirection.OWED_TO_ME) {
                        RecordLentRepaymentDialog(
                            debt = debt,
                            accounts = uiState.accounts,
                            onDismissRequest = { showPaymentDialog = null },
                            onConfirm = { amount, note, date, targetAccountId ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.makeLentRepayment(
                                    debtId = debt.id,
                                    receivingAccountId = targetAccountId,
                                    amount = amount,
                                    note = note,
                                    date = date
                                )
                                showPaymentDialog = null
                                Toast.makeText(context, "تم تسجيل تحصيل الدفعة وإيداعها في المحفظة بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else when (debt) {
                        is RegularDebt -> {
                            RecordRegularPaymentDialog(
                                debt = debt,
                                accounts = uiState.accounts,
                                onDismissRequest = { showPaymentDialog = null },
                                onConfirm = { amount, note, date, accountId ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.makePayment(
                                        debtId = debt.id,
                                        accountId = accountId,
                                        amount = amount,
                                        paymentType = DebtPaymentType.MANUAL,
                                        note = note,
                                        date = date
                                    )
                                    showPaymentDialog = null
                                    Toast.makeText(context, "تم تسجيل دفعة السداد بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        is InstallmentDebt -> {
                            RecordInstallmentPaymentDialog(
                                debt = debt,
                                accounts = uiState.accounts,
                                onDismissRequest = { showPaymentDialog = null },
                                onConfirm = { amount, paymentType, note, date, accountId ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.makePayment(
                                        debtId = debt.id,
                                        accountId = accountId,
                                        amount = amount,
                                        paymentType = paymentType,
                                        note = note,
                                        date = date
                                    )
                                    showPaymentDialog = null
                                    Toast.makeText(context, "تم تسجيل تسديد القسط بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                showEditDebtBottomSheet?.let { targetDebt ->
                    when (targetDebt) {
                        is RegularDebt -> {
                            EditRegularDebtBottomSheet(
                                debt = targetDebt,
                                accounts = uiState.accounts,
                                onDismissRequest = { showEditDebtBottomSheet = null },
                                onConfirm = { title, creditorName, totalAmount, linkedAccountId, notes, color, dueDate ->
                                    val isLent = targetDebt.direction == DebtDirection.OWED_TO_ME
                                    viewModel.updateDebtDetails(
                                        debtId = targetDebt.id,
                                        title = title,
                                        creditorName = creditorName,
                                        totalAmount = totalAmount,
                                        minimumPayment = 0.0,
                                        paymentFrequency = "NONE",
                                        linkedAccountId = linkedAccountId,
                                        priority = 3,
                                        notes = notes,
                                        color = color,
                                        interestRate = 0.0,
                                        dueDate = dueDate,
                                        debtType = DebtType.REGULAR,
                                        onSuccess = {
                                            showEditDebtBottomSheet = null
                                            val msg = if (isLent) "تم تحديث بيانات السلفة بنجاح" else "تم تحديث بيانات الالتزام بنجاح"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
                                    )
                                }
                            )
                        }
                        is InstallmentDebt -> {
                            EditInstallmentDebtBottomSheet(
                                debt = targetDebt,
                                accounts = uiState.accounts,
                                onDismissRequest = { showEditDebtBottomSheet = null },
                                onConfirm = { title, creditorName, totalAmount, minimumPayment, paymentFrequency, linkedAccountId, priority, notes, color, interestRate, dueDate ->
                                    viewModel.updateDebtDetails(
                                        debtId = targetDebt.id,
                                        title = title,
                                        creditorName = creditorName,
                                        totalAmount = totalAmount,
                                        minimumPayment = minimumPayment,
                                        paymentFrequency = paymentFrequency,
                                        linkedAccountId = linkedAccountId,
                                        priority = priority,
                                        notes = notes,
                                        color = color,
                                        interestRate = interestRate,
                                        dueDate = dueDate,
                                        debtType = DebtType.INSTALLMENT,
                                        onSuccess = {
                                            showEditDebtBottomSheet = null
                                            Toast.makeText(context, "تم تحديث بيانات القرض بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
                                    )
                                }
                            )
                        }
                    }
                }

                showDeleteConfirmDialog?.let { targetDebt ->
                    val isLent = targetDebt.direction == DebtDirection.OWED_TO_ME
                    val delTitle = if (isLent) "حذف السجل المالي للسلفة" else "حذف الالتزام المالي"
                    val delMsg = if (isLent) {
                        "هل أنت متأكد من حذف سلفة '${targetDebt.title}' نهائياً؟ سيؤدي ذلك إلى استرجاع مبلغ السلفة الأصلي إلى محفظتك وإلغاء أي دفعات مستردة مرتبطة بها."
                    } else {
                        "هل أنت متأكد من حذف التزام '${targetDebt.title}' نهائياً؟ سيؤدي ذلك إلى حذف جميع دفعات السداد المسجلة المرتبطة به واسترجاع أثرها إلى محفظتك المالية."
                    }
                    AppDialog(
                        onDismissRequest = { showDeleteConfirmDialog = null },
                        title = delTitle,
                        text = delMsg,
                        confirmButtonText = "تأكيد الحذف",
                        onConfirm = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteDebt(targetDebt.id)
                            showDeleteConfirmDialog = null
                            if (activeDebtForDetails?.id == targetDebt.id) activeDebtForDetails = null
                            val successMsg = if (isLent) "تم حذف السلفة واسترجاع رصيدها لمحفظتك" else "تم حذف الالتزام واسترجاع دفعاته بنجاح"
                            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                        },
                        dismissButtonText = "إلغاء",
                        onDismiss = { showDeleteConfirmDialog = null },
                        isDestructive = true
                    )
                }

                showForgiveConfirmDialog?.let { targetDebt ->
                    val isLent = targetDebt.direction == DebtDirection.OWED_TO_ME
                    val fTitle = if (isLent) "مسامحة المدين في السلفة" else "الإعفاء من المبلغ المتبقي"
                    val fMsg = if (isLent) {
                        "هل أنت متأكد من مسامحة '${targetDebt.creditorName}' في المبلغ المتبقي (${FormatterUtils.formatCurrency(targetDebt.remainingAmount)}) لسلفة '${targetDebt.title}'؟ سيتم تصفير المتبقي وإغلاق السلفة دون أي تأثير على رصيد محفظتك."
                    } else {
                        "هل أنت متأكد من تسجيل إعفاء من المبلغ المتبقي (${FormatterUtils.formatCurrency(targetDebt.remainingAmount)}) لالتزام '${targetDebt.title}'؟ سيتم تصفير المتبقي وإغلاق الالتزام دون خصم أي مبلغ من محفظتك."
                    }
                    val confirmBtnText = if (isLent) "تأكيد المسامحة" else "تأكيد الإعفاء"
                    AppDialog(
                        onDismissRequest = { showForgiveConfirmDialog = null },
                        title = fTitle,
                        text = fMsg,
                        confirmButtonText = confirmBtnText,
                        onConfirm = {
                            viewModel.forgiveDebt(
                                debtId = targetDebt.id,
                                onSuccess = {
                                    showForgiveConfirmDialog = null
                                    val toastMsg = if (isLent) "تمت المسامحة في السلفة وإغلاقها بنجاح" else "تم الإعفاء من الالتزام وإغلاقه بنجاح"
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                },
                                onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
                            )
                        },
                        dismissButtonText = "إلغاء",
                        onDismiss = { showForgiveConfirmDialog = null }
                    )
                }

                showPaymentsHistoryBottomSheet?.let { targetDebt ->
                    DebtPaymentsBottomSheet(
                        debt = targetDebt,
                        payments = uiState.selectedDebtPayments,
                        onCancelPaymentClick = { payment -> showCancelPaymentConfirmDialog = payment },
                        onDismissRequest = { showPaymentsHistoryBottomSheet = null }
                    )
                }

                showCancelPaymentConfirmDialog?.let { targetPayment ->
                    AppDialog(
                        onDismissRequest = { showCancelPaymentConfirmDialog = null },
                        title = "التراجع عن الدفعة المسجلة",
                        text = "هل أنت متأكد من إلغاء هذه الدفعة بقيمة ${FormatterUtils.formatCurrency(targetPayment.amount)}؟ سيتم حذف المعاملة المالية المرتبطة بها واستعادة رصيد المحفظة وتحديث المبلغ المتبقي تلقائياً.",
                        confirmButtonText = "نعم، إلغاء الدفعة",
                        onConfirm = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.cancelPayment(
                                paymentId = targetPayment.id,
                                onSuccess = {
                                    showCancelPaymentConfirmDialog = null
                                    Toast.makeText(context, "تم إلغاء الدفعة واستعادة رصيد المحفظة", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
                            )
                        },
                        dismissButtonText = "تراجع",
                        onDismiss = { showCancelPaymentConfirmDialog = null },
                        isDestructive = true
                    )
                }
            }
        }
    }
}
