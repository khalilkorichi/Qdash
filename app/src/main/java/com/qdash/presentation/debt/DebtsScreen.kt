package com.qdash.presentation.debt

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.domain.model.*
import com.qdash.domain.model.RegularDebt
import com.qdash.domain.model.InstallmentDebt
import com.qdash.presentation.debt.components.*
import com.qdash.ui.designsystem.components.AppDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: DebtViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    // Sub-screen navigation
    var activeDebtForDetails by remember { mutableStateOf<Debt?>(null) }

    // Dialog visibility states
    var showAddRegularDebtDialog by remember { mutableStateOf(false) }
    var showAddInstallmentDebtDialog by remember { mutableStateOf(false) }
    var showAddLentDebtDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf<Debt?>(null) }
    var showEditDebtBottomSheet by remember { mutableStateOf<Debt?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Debt?>(null) }
    var showForgiveConfirmDialog by remember { mutableStateOf<Debt?>(null) }
    var showPaymentsHistoryBottomSheet by remember { mutableStateOf<Debt?>(null) }
    var showCancelPaymentConfirmDialog by remember { mutableStateOf<DebtPayment?>(null) }

    LaunchedEffect(activeDebtForDetails, uiState.debts) {
        activeDebtForDetails?.let { current ->
            val updated = uiState.debts.find { it.id == current.id }
            activeDebtForDetails = updated
            if (updated != null) viewModel.selectDebt(updated.id)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("debts_screen"),
        topBar = {
            if (activeDebtForDetails != null) {
                FinTrackTopBar(
                    title = activeDebtForDetails!!.title,
                    showBackButton = true,
                    onBackClick = { activeDebtForDetails = null }
                )
            } else {
                FinTrackTopBar(title = "خطة وتسوية الديون والالتزامات")
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
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
                if (activeDebtForDetails != null) {
                    val debt = activeDebtForDetails!!
                    DebtDetailsContent(
                        debt = debt,
                        accounts = uiState.accounts,
                        selectedDebtPayments = uiState.selectedDebtPayments,
                        onPayClick = { showPaymentDialog = debt },
                        onEditClick = { d -> showEditDebtBottomSheet = d },
                        onDeleteClick = { d -> showDeleteConfirmDialog = d },
                        onForgiveClick = { d -> showForgiveConfirmDialog = d },
                        onCloseDebt = { id -> viewModel.closeDebt(id) },
                        onCancelPaymentClick = { payment -> showCancelPaymentConfirmDialog = payment }
                    )
                } else {
                    DebtsMainContent(
                        debts = uiState.debts,
                        insights = uiState.insights,
                        strategyResults = uiState.strategyResults,
                        selectedStrategy = uiState.selectedStrategy,
                        selectedDirection = uiState.selectedDirection,
                        onDirectionChange = { viewModel.selectDirection(it) },
                        onStrategyChange = { viewModel.changeStrategy(it) },
                        onSelectDebt = { d ->
                            activeDebtForDetails = d
                            viewModel.selectDebt(d.id)
                        },
                        onPayClick = { d -> showPaymentDialog = d },
                        onAddDebtClick = { type ->
                            if (type == DebtType.REGULAR) {
                                showAddRegularDebtDialog = true
                            } else {
                                showAddInstallmentDebtDialog = true
                            }
                        },
                        onAddLentDebtClick = { showAddLentDebtDialog = true },
                        onEditClick = { d -> showEditDebtBottomSheet = d },
                        onDeleteClick = { d -> showDeleteConfirmDialog = d },
                        onForgiveClick = { d -> showForgiveConfirmDialog = d },
                        onPaymentsHistoryClick = { d ->
                            viewModel.selectDebt(d.id)
                            showPaymentsHistoryBottomSheet = d
                        }
                    )
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
                                Toast.makeText(context, "تم تسجيل استرداد السلفة وإيداعها في المحفظة بنجاح", Toast.LENGTH_SHORT).show()
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
                                            Toast.makeText(context, "تم تحديث الدين بنجاح", Toast.LENGTH_SHORT).show()
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
                                            Toast.makeText(context, "تم تحديث الدين بنجاح", Toast.LENGTH_SHORT).show()
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
                    val delTitle = if (isLent) "حذف السجل المالي للسلفة" else "حذف السجل المالي للدين"
                    val delMsg = if (isLent) {
                        "هل أنت متأكد من حذف سلفة '${targetDebt.title}' نهائياً؟ سيؤدي ذلك إلى استرجاع مبلغ السلفة الأصلي إلى محفظتك وإلغاء أي دفعات مستردة مرتبطة بها. لا يمكن التراجع عن هذا الإجراء."
                    } else {
                        "هل أنت متأكد من حذف دين '${targetDebt.title}' نهائياً؟ سيؤدي ذلك أيضاً إلى حذف جميع دفعات السداد المسجلة المرتبطة به وإلغاء تأثيرها على رصيد محفظتك المالية (حذف المعاملات). لا يمكن التراجع عن هذا الإجراء."
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
                            val successMsg = if (isLent) "تم حذف السلفة واسترجاع رصيدها لمحفظتك" else "تم حذف الدين وإلغاء دفعاته بنجاح"
                            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                        },
                        dismissButtonText = "إلغاء",
                        onDismiss = { showDeleteConfirmDialog = null },
                        isDestructive = true
                    )
                }

                showForgiveConfirmDialog?.let { targetDebt ->
                    val isLent = targetDebt.direction == DebtDirection.OWED_TO_ME
                    val fTitle = if (isLent) "مسامحة المدين في السلفة" else "الإعفاء من الدين"
                    val fMsg = if (isLent) {
                        "هل أنت متأكد من مسامحة '${targetDebt.creditorName}' في المبلغ المتبقي لسلفة '${targetDebt.title}'؟ سيتم تصفير المبلغ المتبقي وإغلاق السلفة لوجه الله دون أي تأثير إضافي على محافظك."
                    } else {
                        "هل أنت متأكد من الإعفاء من المتبقي لدين '${targetDebt.title}'؟ سيتم تصفير المبلغ المتبقي وتعيين الدين كمغلق دون خصم أي مبلغ من رصيدك المالي أو إنشاء معاملة سداد."
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
                                    if (activeDebtForDetails?.id == targetDebt.id) {
                                        activeDebtForDetails = uiState.debts.find { it.id == targetDebt.id }
                                    }
                                    val toastMsg = if (isLent) "تمت المسامحة في السلفة وتصفيرها لوجه الله" else "تم الإعفاء من الدين وتصفيره"
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
                        title = "إلغاء الدفعة المالية",
                        text = "هل أنت متأكد من إلغاء هذه الدفعة بقيمة ${targetPayment.amount.toInt()} د.ج؟ سيؤدي ذلك إلى حذف المعاملة المالية المرتبطة بها واستعادة توازن الرصيد في محفظتك، وتحديث المبلغ المتبقي. لا يمكن التراجع عن هذا الإجراء.",
                        confirmButtonText = "تأكيد الإلغاء",
                        onConfirm = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.cancelPayment(
                                paymentId = targetPayment.id,
                                onSuccess = {
                                    showCancelPaymentConfirmDialog = null
                                    Toast.makeText(context, "تم إلغاء الدفعة وتحديث الرصيد", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
                            )
                        },
                        dismissButtonText = "إلغاء",
                        onDismiss = { showCancelPaymentConfirmDialog = null },
                        isDestructive = true
                    )
                }
            }
        }
    }
}
