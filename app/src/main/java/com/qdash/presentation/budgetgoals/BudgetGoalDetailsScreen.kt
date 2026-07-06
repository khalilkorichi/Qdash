package com.qdash.presentation.budgetgoals

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetGoalDetailsScreen(
    budgetId: Long,
    viewModel: BudgetGoalsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()

    // Select budget once loaded
    LaunchedEffect(budgetId, uiState.budgets) {
        val budget = uiState.selectedBudget ?: uiState.budgets.find { it.id == budgetId }
        if (budget != null) {
            viewModel.selectBudgetGoal(budget)
        }
    }

    val budget = uiState.selectedBudget
    val relatedTransactions = uiState.selectedBudgetTransactions

    if (budget == null) {
        BudgetGoalDetailsSkeleton(onBack = onBack)
        return
    }

    val dzdAmountLimit = com.qdash.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", budget.amountLimit)} د.ج")
    val dzdAmountSpent = com.qdash.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", budget.spentAmount)} د.ج")
    val dzdRemaining = com.qdash.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", budget.remainingAmount.coerceAtLeast(0.0))} د.ج")

    val now = System.currentTimeMillis()
    val totalDuration = budget.endDate - now
    val daysLeft = if (totalDuration <= 0) 0L else TimeUnit.MILLISECONDS.toDays(totalDuration).coerceAtLeast(1)

    // Suggest daily safe expense
    val dailySuggestion = if (daysLeft > 0 && budget.remainingAmount > 0) {
        budget.remainingAmount / daysLeft
    } else {
        0.0
    }
    val dzdDailySuggestion = com.qdash.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", dailySuggestion)} د.ج")

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val periodStr = com.qdash.core.utils.FormatterUtils.convertNumerals("${dateFormat.format(Date(budget.startDate))} - ${dateFormat.format(Date(budget.endDate))}")

    val customColor = try {
        Color(android.graphics.Color.parseColor(budget.color))
    } catch (e: Exception) {
        Primary
    }

    val progress = if (budget.amountLimit > 0) {
        (budget.spentAmount / budget.amountLimit).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "BudgetProgress"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "تفاصيل الميزانية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.toggleBudgetArchive(budget)
                    }) {
                        Icon(
                            imageVector = if (budget.isActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                            contentDescription = "تعطيل أو تنشيط",
                            tint = if (budget.isActive) IncomeGreen else TextGray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.deleteBudgetGoal(budget)
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف الميزانية", tint = ExpenseRed)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Main Stat Card
            item {
                BudgetStatCard(
                    budget = budget,
                    customColor = customColor,
                    progress = progress,
                    animatedProgress = animatedProgress,
                    dzdRemaining = dzdRemaining,
                    dzdAmountLimit = dzdAmountLimit,
                    dzdAmountSpent = dzdAmountSpent,
                    daysLeft = daysLeft
                )
            }

            // Coach box: Safe daily spending suggestion
            if (budget.isActive && daysLeft > 0 && budget.remainingAmount > 0) {
                item {
                    BudgetCoachCard(
                        dzdDailySuggestion = dzdDailySuggestion
                    )
                }
            }

            // Period, category, threshold metadata Card
            item {
                val categoryName = if (budget.linkedCategoryId != null) {
                    uiState.categories.find { it.id == budget.linkedCategoryId }?.name ?: "غير معروف"
                } else null

                BudgetPreferencesCard(
                    budget = budget,
                    periodStr = periodStr,
                    categoryName = categoryName,
                    customColor = customColor
                )
            }

            // Transactions Header & List
            budgetTransactionsSection(
                relatedTransactions = relatedTransactions,
                categories = uiState.categories
            )
        }
    }
}
