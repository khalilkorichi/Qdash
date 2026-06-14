package com.example.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.*
import com.example.core.utils.FormatterUtils
import com.example.domain.model.CategoryType
import com.example.domain.model.Account
import com.example.domain.model.Transaction
import com.example.domain.model.AccountType
import com.example.domain.model.TransactionType
import com.example.ui.theme.*
import com.example.presentation.components.radialmenu.AddActionFabContainer
import com.example.ui.designsystem.components.*
import com.example.ui.designsystem.tokens.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddTransactionClick: () -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onViewAllIncomeClick: () -> Unit = {},
    onAccountClick: (Long) -> Unit,
    onSavingsClick: () -> Unit,
    onSubscriptionsClick: () -> Unit,
    onBudgetGoalsClick: () -> Unit,
    onAddExpenseClick: () -> Unit = {},
    onAddIncomeClick: () -> Unit = {},
    onTransferClick: () -> Unit = {},
    onAddDebtClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    unreadCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()
    val navController = com.example.presentation.navigation.LocalNavController.current

    var showBalanceDetails by remember { mutableStateOf(false) }
    val showBalances = uiState.showBalances
    val shouldShowReminder = uiState.showWalletReminder && uiState.accounts.none { it.balance > 0.0 }

    // Root Box so the radial FAB floats above the Scaffold content
    Box(modifier = Modifier.fillMaxSize()) {

        val blurRadius by animateDpAsState(
            targetValue = if (showBalanceDetails) 15.dp else 0.dp,
            animationSpec = tween(durationMillis = 300),
            label = "blur"
        )

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .blur(blurRadius)
                .testTag("home_screen"),
            floatingActionButton = {} // Radial FAB is overlaid manually below
        ) { innerPadding ->

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                val sections = uiState.visibleSections

                LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp) // Space for radial FAB
            ) {

                // â”€â”€ Premium Top Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                item {
                    PremiumTopBalanceCard(
                        totalBalance = uiState.totalBalance,
                        monthlyIncome = uiState.monthlyIncome,
                        monthlyExpense = uiState.monthlyExpense,
                        unreadCount = unreadCount,
                        onNotificationClick = onNotificationClick,
                        onSearchClick = onSearchClick,
                        onAddTransactionClick = onAddTransactionClick,
                        onTransferClick = onTransferClick,
                        onAddIncomeClick = onAddIncomeClick,
                        onCardClick = { showBalanceDetails = true },
                        showBalances = showBalances,
                        onToggleShowBalances = { viewModel.toggleShowBalances() }
                    )
                }

                // â”€â”€ Setup Reminder (if needed) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                if (shouldShowReminder) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Primary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "إعداد أرصدة الحسابات",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "أضف أرصدتك الحالية لجعل تتبع مصاريفك أكثر دقة وواقعية.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray,
                                        lineHeight = 16.sp
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.dismissWalletReminder() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = TextGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // â”€â”€ Customizable Dashboard Sections â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                sections.forEach { section ->
                        when (section) {
                            "split_cards" -> {
                                item {
                                    IncomeExpenseSplitCards(
                                        monthlyIncome = uiState.monthlyIncome,
                                        monthlyExpense = uiState.monthlyExpense,
                                        incomeChangePercent = uiState.incomeChangePercent,
                                        expenseChangePercent = uiState.expenseChangePercent,
                                        accounts = uiState.accounts,
                                        recentTransactions = uiState.recentTransactions,
                                        onIncomeShowAllClick = onViewAllIncomeClick,
                                        onExpenseShowAllClick = onViewAllTransactionsClick,
                                        showBalances = showBalances,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                            "context_templates" -> {
                                item {
                                    ContextAwareTemplateCard(
                                        categories = uiState.categories,
                                        onSelectTemplate = { json ->
                                            navController?.navigate(com.example.presentation.navigation.Screen.AddTransaction.createRoute("EXPENSE", draft = json))
                                        }
                                    )
                                }
                            }
                            "templates" -> {
                                if (uiState.pinnedTemplates.isNotEmpty()) {
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "القوالب المثبتة",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = "إدارة القوالب",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Primary,
                                                    modifier = Modifier
                                                        .clickable { navController?.navigate(com.example.presentation.navigation.Screen.Templates.route) }
                                                        .padding(4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(uiState.pinnedTemplates, key = { it.id }) { template ->
                                                    val typeAccentColor = when (template.transactionType) {
                                                        com.example.domain.model.TransactionType.EXPENSE -> ExpenseRed
                                                        com.example.domain.model.TransactionType.INCOME -> IncomeGreen
                                                        com.example.domain.model.TransactionType.TRANSFER -> TransferBlue
                                                    }
                                                    Surface(
                                                        onClick = {
                                                            // Serialize draft to JSON string to pass safely via Navigation Compose route args
                                                            val draft = com.example.domain.model.TransactionDraft(
                                                                amount = template.amount,
                                                                type = template.transactionType,
                                                                categoryId = template.categoryId,
                                                                subcategoryId = template.subcategoryId,
                                                                accountId = template.accountId,
                                                                targetAccountId = template.targetAccountId,
                                                                notes = template.notes,
                                                                templateId = template.id
                                                            )
                                                            // Custom simple JSON encoding
                                                            val json = """
                                                                {
                                                                    "amount": ${draft.amount},
                                                                    "type": "${draft.type.name}",
                                                                    "categoryId": ${draft.categoryId ?: "null"},
                                                                    "subcategoryId": ${draft.subcategoryId ?: "null"},
                                                                    "accountId": ${draft.accountId},
                                                                    "targetAccountId": ${draft.targetAccountId ?: "null"},
                                                                    "notes": "${draft.notes?.replace("\"", "\\\"") ?: ""}",
                                                                    "templateId": ${draft.templateId ?: "null"}
                                                                }
                                                            """.trimIndent().replace("\n", "").replace(" ", "")
                                                            navController?.navigate(com.example.presentation.navigation.Screen.AddTransaction.createRoute("EXPENSE", draft = json))
                                                        },
                                                        shape = RoundedCornerShape(16.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        border = BorderStroke(1.dp, typeAccentColor.copy(alpha = 0.15f)),
                                                        tonalElevation = 1.dp
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            val emoji = template.iconEmoji
                                                            if (emoji != null && emoji.isNotBlank()) {
                                                                Text(text = emoji, fontSize = 18.sp)
                                                            } else {
                                                                Icon(
                                                                    imageVector = Icons.Default.ReceiptLong,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            Column {
                                                                Text(
                                                                    text = template.name,
                                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Text(
                                                                    text = FormatterUtils.formatCurrency(template.amount),
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                                    color = typeAccentColor
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "quick_actions" -> {
                                item {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Text(
                                            text = "الوصول السريع",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            QuickActionTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Default.Payments,
                                                label = "إضافة معاملة",
                                                iconBg = Brush.linearGradient(
                                                    listOf(Primary.copy(alpha = 0.25f), Primary.copy(alpha = 0.08f))
                                                ),
                                                iconColor = Primary,
                                                onClick = onAddTransactionClick
                                            )
                                            QuickActionTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Default.ReceiptLong,
                                                label = "سجل المعاملات",
                                                iconBg = Brush.linearGradient(
                                                    listOf(TransferBlue.copy(alpha = 0.25f), TransferBlue.copy(alpha = 0.08f))
                                                ),
                                                iconColor = TransferBlue,
                                                onClick = onViewAllTransactionsClick
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            QuickActionTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Default.Savings,
                                                label = "حصالة الادخار",
                                                iconBg = Brush.linearGradient(
                                                    listOf(SavingsAmber.copy(alpha = 0.25f), SavingsAmber.copy(alpha = 0.08f))
                                                ),
                                                iconColor = SavingsAmber,
                                                onClick = onSavingsClick
                                            )
                                            QuickActionTile(
                                                modifier = Modifier.weight(1f),
                                                icon = Icons.Default.CardMembership,
                                                label = "الاشتراكات",
                                                iconBg = Brush.linearGradient(
                                                    listOf(ExpenseRed.copy(alpha = 0.20f), ExpenseRed.copy(alpha = 0.06f))
                                                ),
                                                iconColor = ExpenseRed,
                                                onClick = onSubscriptionsClick
                                            )
                                        }
                                    }
                                }
                            }
                            "accounts" -> {
                                item {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Text(
                                            text = "حساباتي المالية",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        if (uiState.isLoading) {
                                            AccountsRowSkeleton()
                                        } else if (uiState.accounts.isEmpty()) {
                                            Text(
                                                text = "لا توجد حسابات مضافة حالياً.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextGray
                                            )
                                        } else {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(uiState.accounts, key = { it.id }) { acc ->
                                                    AccountCard(
                                                          account = acc,
                                                          showBalance = uiState.accountBalancesVisibility[acc.id] ?: true,
                                                          onToggleBalanceVisibility = { viewModel.toggleAccountBalanceVisibility(acc.id) },
                                                          onClick = { onAccountClick(acc.id) }
                                                      )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "chart" -> {
                                item {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Text(
                                            text = "تحليل المصروفات",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        if (uiState.isLoading) {
                                            ChartSkeleton()
                                        } else {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                shape = RoundedCornerShape(20.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    // Time Period Selector Pills
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceEvenly
                                                    ) {
                                                        listOf(
                                                            "DAY" to "يومي",
                                                            "WEEK" to "أسبوعي",
                                                            "MONTH" to "شهري",
                                                            "YEAR" to "سنوي"
                                                        ).forEach { (p, label) ->
                                                            val isSelected = uiState.chartPeriod == p
                                                            val bg = if (isSelected) Primary else MaterialTheme.colorScheme.background
                                                            val tc = if (isSelected) Color.White else TextGray
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(bg)
                                                                    .clickable { viewModel.setChartPeriod(p) }
                                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(text = label, style = MaterialTheme.typography.labelMedium, color = tc, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                                            }
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(20.dp))
                                                    
                                                    // Line Chart Canvas
                                                    val points = uiState.expenseTrendData
                                                    if (points.isEmpty() || points.all { it.amount == 0.0 }) {
                                                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                                            Text("لا توجد مصروفات متاحة في هذه الفترة", color = TextGray, style = MaterialTheme.typography.labelMedium)
                                                        }
                                                    } else {
                                                        val maxAmount = points.maxOfOrNull { it.amount }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                                                        val primaryColor = Primary
                                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                                            val w = size.width
                                                            val h = size.height - 20.dp.toPx() // Reserve 20dp for labels
                                                            val stepX = w / (points.size - 1).coerceAtLeast(1)
                                                            
                                                            val path = androidx.compose.ui.graphics.Path()
                                                            points.forEachIndexed { i, pt ->
                                                                val x = i * stepX
                                                                val y = h - ((pt.amount.toFloat() / maxAmount) * h * 0.8f) // leave top padding
                                                                if (i == 0) {
                                                                    path.moveTo(x, y)
                                                                } else {
                                                                    val prevX = (i - 1) * stepX
                                                                    val prevY = h - ((points[i - 1].amount.toFloat() / maxAmount) * h * 0.8f)
                                                                    path.cubicTo(
                                                                        prevX + (x - prevX) / 2f, prevY,
                                                                        prevX + (x - prevX) / 2f, y,
                                                                        x, y
                                                                    )
                                                                }
                                                            }
                                                            
                                                            // Draw gradient fill
                                                            val fillPath = androidx.compose.ui.graphics.Path().apply {
                                                                addPath(path)
                                                                lineTo(w, h)
                                                                lineTo(0f, h)
                                                                close()
                                                            }
                                                            drawPath(
                                                                path = fillPath,
                                                                brush = Brush.verticalGradient(
                                                                    colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
                                                                    startY = 0f,
                                                                    endY = h
                                                                ),
                                                                style = androidx.compose.ui.graphics.drawscope.Fill
                                                            )
                                                            
                                                            // Draw stroke
                                                            drawPath(
                                                                path = path,
                                                                color = primaryColor,
                                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                                    width = 3.dp.toPx(),
                                                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                                                )
                                                            )
                                                            
                                                            // Draw points & labels
                                                            val textPaint = android.graphics.Paint().apply {
                                                                color = android.graphics.Color.GRAY
                                                                textSize = 10.sp.toPx()
                                                                textAlign = android.graphics.Paint.Align.CENTER
                                                                isAntiAlias = true
                                                            }
                                                            points.forEachIndexed { i, pt ->
                                                                val x = i * stepX
                                                                val y = h - ((pt.amount.toFloat() / maxAmount) * h * 0.8f)
                                                                
                                                                // Find the highest point to highlight
                                                                val isMax = pt.amount == points.maxOf { it.amount } && pt.amount > 0.0
                                                                
                                                                if (isMax) {
                                                                    // Glowing/highlighted point
                                                                    drawCircle(color = Color.Black, radius = 7.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                                                    drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                                                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                                                } else {
                                                                    // Smaller, cleaner secondary points
                                                                    drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                                                    drawCircle(color = primaryColor, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                                                                }
                                                                
                                                                // Only draw every nth label if too many points to avoid clutter, or draw all for small sets
                                                                if (points.size <= 7 || i % (points.size / 7) == 0 || i == points.lastIndex) {
                                                                    drawContext.canvas.nativeCanvas.drawText(pt.label, x, h + 15.dp.toPx(), textPaint)
                                                                }
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "budget" -> {
                                item {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Text(
                                            text = "الميزانية الشهرية",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        val budgetCat = uiState.categories.firstOrNull {
                                            it.type == CategoryType.EXPENSE && it.budgetLimit != null
                                        }
                                        if (budgetCat != null) {
                                            val matchingSpent = uiState.recentTransactions
                                                .filter { it.categoryId == budgetCat.id }
                                                .sumOf { it.amount }
                                            BudgetProgressCard(
                                                category = budgetCat,
                                                currentUsage = matchingSpent
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onBudgetGoalsClick() },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                            ),
                                            border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .background(Primary.copy(alpha = 0.12f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PieChart,
                                                            contentDescription = null,
                                                            tint = Primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = "لوحة تتبع الميزانية الذكية وتنبيهات الإنفاق",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onBackground
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "انقر لعرض وتخصيص حدود ميزانياتك الشهرية والأسبوعية الفعالة تفصيلياً.",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextGray,
                                                            lineHeight = 16.sp
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                                    contentDescription = "انتقال",
                                                    tint = Primary.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            "subscriptions" -> {
                                if (uiState.upcomingSubscriptions.isNotEmpty()) {
                                    item {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "تذكيرات الاشتراكات القادمة",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = Primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                uiState.upcomingSubscriptions.take(2).forEach { sub ->
                                                    val linkedAcc = uiState.accounts
                                                        .firstOrNull { it.id == sub.accountId }?.name ?: "غير محدد"
                                                    SubscriptionItem(
                                                        subscription = sub,
                                                        onToggleActive = {},
                                                        accountName = linkedAcc
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "recent_transactions" -> {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "آخر العمليات والإنفاق",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "عرض الكل",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Primary,
                                            modifier = Modifier
                                                .clickable { onViewAllTransactionsClick() }
                                                .padding(4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                if (uiState.isLoading) {
                                    item {
                                        TransactionListSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                } else if (uiState.recentTransactions.isEmpty()) {
                                    item {
                                        EmptyStateView(
                                            title = "لا توجد معاملات مضافة!",
                                            description = "اضغط على زر الإضافة العائم لإدراج أول مصروف لك اليوم.",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                        )
                                    }
                                } else {
                                    items(uiState.recentTransactions.take(5), key = { it.id }) { tx ->
                                        val cat = uiState.categories.firstOrNull { it.id == tx.categoryId }
                                        val accName = uiState.accounts
                                            .firstOrNull { it.id == tx.accountId }?.name ?: "غير معروف"
                                        TransactionItem(
                                            transaction = tx,
                                            category = cat,
                                            accountName = accName,
                                            onClick = onViewAllTransactionsClick,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .padding(bottom = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                }
            }
            } // end PullToRefreshBox
        } // end Scaffold


        // â”€â”€ Available Balance Detailed Modal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        val overlayAlphaState = animateFloatAsState(
            targetValue = if (showBalanceDetails) 0.6f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "overlayAlpha"
        )
        val scaleState = animateFloatAsState(
            targetValue = if (showBalanceDetails) 1f else 0.9f,
            animationSpec = tween(durationMillis = 300),
            label = "scale"
        )
        val cardAlphaState = animateFloatAsState(
            targetValue = if (showBalanceDetails) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "cardAlpha"
        )

        val overlayAlpha = overlayAlphaState.value
        val scale = scaleState.value
        val cardAlpha = cardAlphaState.value

        if (overlayAlpha > 0.01f) {
            // Backdrop dim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        showBalanceDetails = false
                    }
            )

            // Centered dialog container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            alpha = cardAlpha
                        )
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            // Prevent click propagation
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) {
                            ColorTokens.ElevatedSurfaceDark
                        } else {
                            ColorTokens.Primary
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Decorative background elements
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 40.dp, y = (-30).dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                        )
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .align(Alignment.BottomStart)
                                .offset(x = (-20).dp, y = 30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // Header row with title & actions (eye toggle + close button)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(IncomeGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تفاصيل الرصيد المتوفر",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                viewModel.toggleShowBalances()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (showBalances) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "إخفاء/إظهار الرصيد",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable { showBalanceDetails = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "إجمالي الرصيد الحالي",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (showBalances) FormatterUtils.formatCurrency(uiState.totalBalance) else "•••• دج",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 30.sp
                                ),
                                color = Color.White
                            )
                            val dialogColloquial = remember(uiState.totalBalance, showBalances) {
                                if (showBalances) FormatterUtils.formatColloquialAlgerian(uiState.totalBalance) else null
                            }
                            if (dialogColloquial != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "أي ما يعادل: $dialogColloquial",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "توزيع الرصيد حسب الحسابات:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                uiState.accounts.forEach { acc ->
                                    val accentColor = when (acc.type) {
                                        com.example.domain.model.AccountType.BARIDIMOB -> Color(0xFF005CA9)
                                        com.example.domain.model.AccountType.CCP -> Color(0xFFF59E0B)
                                        com.example.domain.model.AccountType.CASH -> Color(0xFF22C55E)
                                        com.example.domain.model.AccountType.SAVINGS -> Color(0xFF3B82F6)
                                        else -> if (isSystemInDarkTheme()) ColorTokens.TextSecondaryDark else Primary
                                    }

                                    Surface(
                                        onClick = {
                                            showBalanceDetails = false
                                            onAccountClick(acc.id)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.05f),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(accentColor.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (acc.type == com.example.domain.model.AccountType.BARIDIMOB) {
                                                        androidx.compose.foundation.Image(
                                                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_baridimob),
                                                            contentDescription = "بريدي موب",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = when (acc.type) {
                                                                com.example.domain.model.AccountType.CCP -> Icons.Default.CreditCard
                                                                com.example.domain.model.AccountType.CASH -> Icons.Default.Payments
                                                                com.example.domain.model.AccountType.SAVINGS -> Icons.Default.Savings
                                                                else -> Icons.Default.AccountBalanceWallet
                                                            },
                                                            contentDescription = null,
                                                            tint = accentColor,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column {
                                                    Text(
                                                        text = acc.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = when (acc.type) {
                                                            com.example.domain.model.AccountType.BARIDIMOB -> "بريدي موب"
                                                            com.example.domain.model.AccountType.CCP -> "حساب جاري CCP"
                                                            com.example.domain.model.AccountType.CASH -> "نقد سلّة"
                                                            com.example.domain.model.AccountType.SAVINGS -> "ادخار"
                                                            else -> "حساب آخر"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = if (showBalances) FormatterUtils.formatCurrency(acc.balance) else "•••• دج",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color.White
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronLeft,
                                                    contentDescription = "عرض التفاصيل",
                                                    tint = Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    } // end root Box
}

