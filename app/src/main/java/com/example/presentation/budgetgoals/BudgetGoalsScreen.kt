package com.example.presentation.budgetgoals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.presentation.components.AlertBanner
import com.example.presentation.components.BudgetGoalCard
import com.example.ui.theme.Primary
import com.example.ui.theme.TextGray
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetGoalsScreen(
    viewModel: BudgetGoalsViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetails: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    var showArchived by remember { mutableStateOf(false) }
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()

    val activeBudgets = uiState.budgets.filter { it.isActive }
    val archivedBudgets = uiState.budgets.filter { !it.isActive }

    val activeCount = activeBudgets.size
    val totalLimit = activeBudgets.sumOf { it.amountLimit }
    val totalSpent = activeBudgets.sumOf { it.spentAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أهداف الميزانية") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showArchived = !showArchived }) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "المؤرشفة",
                            tint = if (showArchived) Primary else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة ميزانية")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Primary, Color(0xFF3B82F6))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                text = "ملخص الميزانية النشطة",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "إجمالي الحد المسموح",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = com.example.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", totalLimit)} د.ج"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = "إجمالي المنفق",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = com.example.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", totalSpent)} د.ج"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = "المتبقي الآمن",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = com.example.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", (totalLimit - totalSpent).coerceAtLeast(0.0))} د.ج"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (activeCount > 0 && uiState.alerts.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تنبيهات الميزانية الذكية",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        uiState.alerts.take(3).forEach { alert ->
                            AlertBanner(message = alert.message, status = alert.status)
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (showArchived) "الميزانيات المؤرشفة (${archivedBudgets.size})" else "الميزانيات النشطة ($activeCount)",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val currentList = if (showArchived) archivedBudgets else activeBudgets

            if (currentList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Recommend,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TextGray
                            )
                            Text(
                                text = if (showArchived) "لا توجد ميزانيات مؤرشفة" else "ابدأ بإنشاء ميزانية تتبع ذكية لحماية أموالك!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                        }
                    }
                }
            } else {
                items(currentList, key = { it.id }) { budget ->
                    BudgetGoalCard(
                        budget = budget,
                        onClick = { onNavigateToDetails(budget.id) },
                        onLongClick = {
                            viewModel.toggleBudgetArchive(budget)
                        }
                    )
                }
            }
        }
        } // end PullToRefreshBox
    }
}
