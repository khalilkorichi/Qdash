package com.qdash.presentation.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.domain.model.FinancialPlanStatus
import com.qdash.presentation.plans.components.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FinancialPlansScreen(
    viewModel: FinancialPlansViewModel,
    onBack: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            UnifiedScreenHeader(
                title = "الخطط المالية",
                subtitle = "حدد ميزانيتك التقديرية وحقق أهدافك بكفاءة",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة خطة", tint = Primary)
                    }
                }
            )
            if (uiState.isLoading) {
                SummaryHeaderSkeleton()
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(3) {
                        PlanCardSkeleton()
                    }
                }
            } else {
                // Summary header
                if (uiState.plans.isNotEmpty()) {
                    SummaryHeader(plans = uiState.plans)
                }

                if (uiState.plans.isEmpty()) {
                    EmptyPlansState(onAddClick = { showAddDialog = true })
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.plans, key = { it.id }) { plan ->
                            PlanCard(
                                plan = plan,
                                onDelete = { viewModel.deletePlan(plan) },
                                onPause = {
                                    val newStatus = if (plan.status == FinancialPlanStatus.PAUSED)
                                        FinancialPlanStatus.ACTIVE
                                    else FinancialPlanStatus.PAUSED
                                    viewModel.updateStatus(plan.id, newStatus)
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlanDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, type, target, notes, color ->
                viewModel.addPlan(title, type, target, notes, color)
                showAddDialog = false
            }
        )
    }
}
