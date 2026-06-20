package com.example.presentation.budgetgoals

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.TransactionItem
import com.example.domain.model.BudgetGoal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.Primary
import com.example.ui.theme.TextGray
import com.example.ui.designsystem.components.shimmerEffect
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
        val budget = uiState.budgets.find { it.id == budgetId }
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

    val dzdAmountLimit = com.example.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", budget.amountLimit)} د.ج")
    val dzdAmountSpent = com.example.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", budget.spentAmount)} د.ج")
    val dzdRemaining = com.example.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", budget.remainingAmount.coerceAtLeast(0.0))} د.ج")

    val now = System.currentTimeMillis()
    val totalDuration = budget.endDate - now
    val daysLeft = if (totalDuration <= 0) 0L else TimeUnit.MILLISECONDS.toDays(totalDuration).coerceAtLeast(1)

    // Suggest daily safe expense
    val dailySuggestion = if (daysLeft > 0 && budget.remainingAmount > 0) {
        budget.remainingAmount / daysLeft
    } else {
        0.0
    }
    val dzdDailySuggestion = com.example.core.utils.FormatterUtils.convertNumerals("${String.format("%,.0f", dailySuggestion)} د.ج")

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val periodStr = com.example.core.utils.FormatterUtils.convertNumerals("${dateFormat.format(Date(budget.startDate))} - ${dateFormat.format(Date(budget.endDate))}")

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
            // Main Stat Card with Radial Gradient Tint
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        customColor.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Budget Category indicator / name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(customColor, CircleShape)
                                )
                                Text(
                                    text = budget.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Surface(
                                    color = if (budget.isActive) IncomeGreen.copy(alpha = 0.1f) else TextGray.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (budget.isActive) "نشطة" else "مؤرشفة",
                                        color = if (budget.isActive) IncomeGreen else TextGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "المبلغ المتبقي للإنفاق",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dzdRemaining,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 32.sp
                                ),
                                color = if (budget.remainingAmount < 0) ExpenseRed else IncomeGreen
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Premium Custom Progress Bar with Glowing style
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "نسبة الاستهلاك",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (progress >= 1f) ExpenseRed else customColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedProgress)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = if (progress >= 1.0f) {
                                                        listOf(ExpenseRed, ExpenseRed.copy(alpha = 0.8f))
                                                    } else {
                                                        listOf(customColor, customColor.copy(alpha = 0.7f))
                                                    }
                                                )
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Spacer(modifier = Modifier.height(20.dp))

                            // Premium grid detail rows
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "الحد الأقصى",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dzdAmountLimit,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(30.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "المستهلك",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dzdAmountSpent,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ExpenseRed
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(30.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "الأيام المتبقية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$daysLeft يوم",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Coach box: Safe daily spending suggestion
            if (budget.isActive && daysLeft > 0 && budget.remainingAmount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "المصروف اليومي الآمن كحد أقصى",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "للبقاء تحت سقف الميزانية، ننصحك ألا يتجاوز إنفاقك اليومي $dzdDailySuggestion.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Period, category, threshold metadata Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "تفاصيل وتفضيلات الميزانية",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Period Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "النطاق الزمني:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = periodStr, 
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        // Warning Threshold Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "نسبة حذر التنبيه:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${budget.alertThresholdPercent}% من الميزانية", 
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        // Category Row
                        if (budget.linkedCategoryId != null) {
                            val category = uiState.categories.find { it.id == budget.linkedCategoryId }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "التصنيف المرتبط:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = category?.name ?: "غير معروف", 
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = customColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "العمليات المؤثرة على الميزانية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${relatedTransactions.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (relatedTransactions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أي معاملات مسجلة في هذا النطاق حتى الآن.",
                                color = TextGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(relatedTransactions, key = { it.id }) { tx ->
                    val category = uiState.categories.find { it.id == tx.categoryId }
                    TransactionItem(
                        transaction = tx,
                        category = category,
                        accountName = "الحساب الجاري", // Fallback text safely
                        onClick = {}
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetGoalDetailsSkeleton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.size(width = 140.dp, height = 24.dp).shimmerEffect(RoundedCornerShape(6.dp)))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Stat Dial / Display Card Skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(width = 120.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(width = 180.dp, height = 36.dp).shimmerEffect(RoundedCornerShape(8.dp)))

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(width = 60.dp, height = 12.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.size(width = 80.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(width = 60.dp, height = 12.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.size(width = 80.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(width = 60.dp, height = 12.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.size(width = 80.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }

            // Coach card suggestion skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.size(width = 160.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.size(width = 200.dp, height = 12.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                    }
                }
            }

            // Period, category, threshold metadata Card Skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(width = 100.dp, height = 18.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.size(width = 80.dp, height = 14.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                        Box(modifier = Modifier.size(width = 140.dp, height = 14.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.size(width = 80.dp, height = 14.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                        Box(modifier = Modifier.size(width = 100.dp, height = 14.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}
