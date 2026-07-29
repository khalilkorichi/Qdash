package com.qdash.presentation.salary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.presentation.salary.components.AddSalaryForm
import com.qdash.presentation.salary.components.DelaySalaryForm
import com.qdash.presentation.salary.components.DistributionConfigCard
import com.qdash.presentation.salary.components.SalaryDelayHistoryCard
import com.qdash.presentation.salary.components.SalaryOverviewCard
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryScreen(
    viewModel: SalaryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    containerColor = Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة راتب")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                UnifiedScreenHeader(
                    title = "إدارة الراتب",
                    subtitle = "تحكم في راتبك وتوزيعه التلقائي بذكاء",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }

            if (uiState.isLoading) {
                item {
                    com.qdash.ui.designsystem.components.salary.SalarySkeleton()
                }
            } else if (uiState.overview?.salary == null) {
                item {
                    SalaryEmptyState(
                        onAddClick = { viewModel.setShowAddDialog(true) }
                    )
                }
            } else {
                val salary = uiState.overview!!.salary!!

                item {
                    SalaryOverviewCard(
                        salary = salary,
                        onEdit = { viewModel.setShowAddDialog(true, salary) },
                        onDelayClick = { viewModel.startAddSalaryDelay() }
                    )
                }

                if (uiState.overview!!.delays.isNotEmpty()) {
                    item {
                        Text(
                            text = "سجل التأجيلات الأخير",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(uiState.overview!!.delays.take(3)) { delay ->
                        SalaryDelayHistoryCard(
                            delay = delay,
                            onEdit = { viewModel.startEditSalaryDelay(delay) },
                            onDelete = { viewModel.deleteSalaryDelay(delay.id) },
                            onDepositNow = { viewModel.depositSalaryNow(delay.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    DistributionConfigCard(uiState, viewModel)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val delaySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        if (uiState.showAddDialog) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowAddDialog(false) },
                sheetState = addSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AddSalaryForm(uiState, viewModel)
            }
        }

        if (uiState.showDelayDialog) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowDelayDialog(false) },
                sheetState = delaySheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DelaySalaryForm(uiState, viewModel)
            }
        }
    }
}

@Composable
private fun SalaryEmptyState(onAddClick: () -> Unit) {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = "لم تقم بإضافة راتب بعد",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "أضف راتبك الأساسي لتمكين ميزات التوزيع التلقائي وإدارة الميزانية الذكية وتأجيل الرواتب.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth(0.7f),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("إضافة الراتب الآن", color = Color.White)
        }
    }
}
