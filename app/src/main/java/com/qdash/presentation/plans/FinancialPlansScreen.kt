package com.qdash.presentation.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.domain.model.FinancialPlan
import com.qdash.domain.model.FinancialPlanStatus
import com.qdash.presentation.navigation.LocalBottomBarPadding
import com.qdash.presentation.plans.components.*
import com.qdash.ui.designsystem.components.AppDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialPlansScreen(
    viewModel: FinancialPlansViewModel,
    onBack: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val bottomBarPadding = LocalBottomBarPadding.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<FinancialPlan?>(null) }
    var planToUpdateProgress by remember { mutableStateOf<FinancialPlan?>(null) }
    var planToDelete by remember { mutableStateOf<FinancialPlan?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredPlans = remember(uiState.plans, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> uiState.plans.filter { it.status == FinancialPlanStatus.ACTIVE }
            "PAUSED" -> uiState.plans.filter { it.status == FinancialPlanStatus.PAUSED }
            "COMPLETED" -> uiState.plans.filter { it.status == FinancialPlanStatus.COMPLETED }
            else -> uiState.plans
        }
    }

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
                        Icon(Icons.Default.Add, contentDescription = "إضافة خطة", tint = primary)
                    }
                }
            )

            if (uiState.isLoading) {
                SummaryHeaderSkeleton()
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = bottomBarPadding + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(3) {
                        PlanCardSkeleton()
                    }
                }
            } else {
                if (uiState.plans.isNotEmpty()) {
                    SummaryHeader(plans = uiState.plans)

                    val filterItems = listOf(
                        "ALL" to "الكل (${uiState.plans.size})",
                        "ACTIVE" to "نشطة (${uiState.plans.count { it.status == FinancialPlanStatus.ACTIVE }})",
                        "PAUSED" to "موقوفة (${uiState.plans.count { it.status == FinancialPlanStatus.PAUSED }})",
                        "COMPLETED" to "مكتملة (${uiState.plans.count { it.status == FinancialPlanStatus.COMPLETED }})"
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterItems) { (key, label) ->
                            val selected = selectedFilter == key
                            FilterChip(
                                selected = selected,
                                onClick = { selectedFilter = key },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                if (uiState.plans.isEmpty()) {
                    EmptyPlansState(onAddClick = { showAddDialog = true })
                } else if (filteredPlans.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد خطط مطابقة لهذا التصنيف",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = bottomBarPadding + 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredPlans, key = { it.id }) { plan ->
                            PlanCard(
                                plan = plan,
                                onDelete = { planToDelete = plan },
                                onPause = {
                                    val newStatus = if (plan.status == FinancialPlanStatus.PAUSED) {
                                        if (plan.targetAmount > 0 && plan.currentAmount >= plan.targetAmount) {
                                            FinancialPlanStatus.COMPLETED
                                        } else {
                                            FinancialPlanStatus.ACTIVE
                                        }
                                    } else {
                                        FinancialPlanStatus.PAUSED
                                    }
                                    viewModel.updateStatus(plan.id, newStatus)
                                },
                                onUpdateProgress = { planToUpdateProgress = plan },
                                onEdit = { planToEdit = plan }
                            )
                        }
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

    planToEdit?.let { editingPlan ->
        AddPlanDialog(
            initialPlan = editingPlan,
            onDismiss = { planToEdit = null },
            onConfirm = { title, type, target, notes, color ->
                viewModel.updatePlan(
                    editingPlan.copy(
                        title = title,
                        type = type,
                        targetAmount = target,
                        notes = notes,
                        color = color
                    )
                )
                planToEdit = null
            }
        )
    }

    planToUpdateProgress?.let { targetPlan ->
        UpdatePlanProgressDialog(
            plan = targetPlan,
            onDismiss = { planToUpdateProgress = null },
            onConfirm = { newAmount ->
                viewModel.updateProgress(targetPlan.id, newAmount)
                planToUpdateProgress = null
            }
        )
    }

    planToDelete?.let { targetPlan ->
        AppDialog(
            onDismissRequest = { planToDelete = null },
            title = "حذف الخطة المالية",
            text = "هل أنت متأكد من حذف خطة «${targetPlan.title}» نهائياً؟ لا يمكن التراجع عن هذا الإجراء.",
            confirmButtonText = "حذف الخطة",
            onConfirm = {
                viewModel.deletePlan(targetPlan)
                planToDelete = null
            },
            dismissButtonText = "إلغاء",
            onDismiss = { planToDelete = null },
            isDestructive = true,
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}
