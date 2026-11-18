package com.qdash.presentation.salary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.FinTrackApp
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.core.utils.FormatterUtils
import com.qdash.presentation.components.UnifiedCategoryIcon
import com.qdash.presentation.components.safeParseColor
import com.qdash.presentation.navigation.LocalBottomBarPadding
import com.qdash.presentation.salary.components.AddSalaryForm
import com.qdash.presentation.salary.components.DelaySalaryForm
import com.qdash.presentation.salary.components.DistributionConfigCard
import com.qdash.presentation.salary.components.SalaryDelayHistoryCard
import com.qdash.presentation.salary.components.SalaryOverviewCard
import com.qdash.ui.designsystem.components.AppBottomSheet
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryScreen(
    viewModel: SalaryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val bottomPadding = LocalBottomBarPadding.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val txRepo = remember(context) {
        (context.applicationContext as? FinTrackApp)?.container?.transactionRepository
    }

    var showDepositOnTimeConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.overview?.salary == null && !uiState.isLoading) {
                FloatingActionButton(
                    onClick = { viewModel.setShowAddDialog(true) },
                    containerColor = primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة راتب")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = bottomPadding + 24.dp)
        ) {
            // Header sits OUTSIDE horizontal padding because UnifiedScreenHeader applies its own 16.dp horizontal padding
            item {
                Spacer(modifier = Modifier.height(8.dp))
                UnifiedScreenHeader(
                    title = "إدارة الراتب",
                    subtitle = "تحكم في راتبك وتوزيعه التلقائي بذكاء",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        com.qdash.ui.designsystem.components.salary.SalarySkeleton()
                    }
                }
            } else if (uiState.overview?.salary == null) {
                item {
                    SalaryEmptyState(
                        onAddClick = { viewModel.setShowAddDialog(true) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                val salary = uiState.overview!!.salary!!
                val accountName = uiState.accounts.find { it.id == salary.accountId }?.name

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SalaryOverviewCard(
                            salary = salary,
                            accountName = accountName,
                            onEdit = { viewModel.setShowAddDialog(true, salary) },
                            onDepositNowClick = { showDepositOnTimeConfirm = true },
                            onDelayClick = { viewModel.startAddSalaryDelay() }
                        )
                    }
                }

                if (uiState.overview!!.delays.isNotEmpty()) {
                    item {
                        Text(
                            text = "سجل التأجيلات الأخير",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        )
                    }
                    items(uiState.overview!!.delays.take(3), key = { it.id }) { delay ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SalaryDelayHistoryCard(
                                delay = delay,
                                onEdit = { viewModel.startEditSalaryDelay(delay) },
                                onDelete = { viewModel.deleteSalaryDelay(delay.id) },
                                onDepositNow = { viewModel.depositSalaryNow(delayId = delay.id, fallbackTransactionRepo = txRepo) }
                            )
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        DistributionConfigCard(uiState, viewModel)
                    }
                }
            }
        }

        if (showDepositOnTimeConfirm && uiState.overview?.salary != null) {
            val salary = uiState.overview!!.salary!!
            val targetAccName = uiState.accounts.find { it.id == salary.accountId }?.name ?: "الحساب المختار"
            AppDialog(
                onDismissRequest = { showDepositOnTimeConfirm = false },
                title = "تأكيد استلام وإيداع الراتب",
                text = "سيتم تسجيل إيداع راتب بقيمة ${FormatterUtils.formatCurrency(salary.amount)} في «$targetAccName» وتحديث موعد الدفعة القادمة للشهر المقبل" +
                        if (uiState.distributionEnabled && uiState.envelopes.any { it.linkedAccountId != null && it.linkedAccountId != salary.accountId }) {
                            "، مع تحويل حصص الأظرف المرتبطة بحسابات فرعية تلقائياً."
                        } else ".",
                confirmButtonText = "تأكيد الإيداع الآن",
                onConfirm = {
                    viewModel.depositSalaryNow(delayId = null, fallbackTransactionRepo = txRepo)
                    showDepositOnTimeConfirm = false
                },
                dismissButtonText = "إلغاء",
                onDismiss = { showDepositOnTimeConfirm = false }
            )
        }

        val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val delaySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        if (uiState.showAddDialog) {
            AppBottomSheet(
                onDismissRequest = { viewModel.setShowAddDialog(false) },
                sheetState = addSheetState
            ) {
                AddSalaryForm(uiState, viewModel)
            }
        }

        if (uiState.showDelayDialog) {
            AppBottomSheet(
                onDismissRequest = { viewModel.setShowDelayDialog(false) },
                sheetState = delaySheetState
            ) {
                DelaySalaryForm(uiState, viewModel)
            }
        }

        // Category Picker Sheet for Salary Envelopes
        uiState.showCategoryPicker?.let { envelopeId ->
            val targetEnvelope = uiState.envelopes.find { it.id == envelopeId }
            if (targetEnvelope != null) {
                AppBottomSheet(
                    onDismissRequest = { viewModel.dismissCategoryPicker() }
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.75f)
                                .padding(horizontal = 20.dp)
                                .padding(top = 4.dp, bottom = 24.dp)
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "ربط فئات المصاريف بظرف «${targetEnvelope.label}»",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "أي معاملة إنفاق ضمن الفئات المختارة ستُحتسب تلقائياً من ميزانية هذا الظرف:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.categories, key = { it.id }) { category ->
                                    val isLinked = category.id in targetEnvelope.linkedCategoryIds
                                    val catColor = safeParseColor(category.color, primary)

                                    AppCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        variant = CardVariant.SOLID,
                                        shape = ShapeTokens.Md,
                                        backgroundColor = if (isLinked) catColor.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                                        onClick = { viewModel.toggleCategoryForEnvelope(envelopeId, category.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(catColor.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    UnifiedCategoryIcon(
                                                        iconKey = category.icon,
                                                        tint = catColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isLinked) FontWeight.Bold else FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Checkbox(
                                                checked = isLinked,
                                                onCheckedChange = {
                                                    viewModel.toggleCategoryForEnvelope(envelopeId, category.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            AppButton(
                                onClick = { viewModel.dismissCategoryPicker() },
                                modifier = Modifier.fillMaxWidth(),
                                variant = ButtonVariant.SOLID,
                                intent = ButtonIntent.PRIMARY,
                                shape = ShapeTokens.Lg
                            ) {
                                Text("تم الحفظ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalaryEmptyState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = "لم تقم بإضافة راتب بعد",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "أضف راتبك الأساسي لتمكين استلام الراتب بضغطة واحدة، التوزيع التلقائي على الأظرف (50/30/20)، وإدارة تأخر الصرف.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        AppButton(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth(0.75f),
            variant = ButtonVariant.SOLID,
            intent = ButtonIntent.PRIMARY,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("إضافة الراتب الآن", fontWeight = FontWeight.Bold)
        }
    }
}
