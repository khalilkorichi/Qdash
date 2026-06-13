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

    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("kdach_prefs", android.content.Context.MODE_PRIVATE) }
    var showWalletReminder by remember {
        mutableStateOf(
            sharedPrefs.getBoolean("wallet_setup_skipped", false) &&
            !sharedPrefs.getBoolean("wallet_setup_reminder_dismissed", false)
        )
    }
    var showBalanceDetails by remember { mutableStateOf(false) }
    var showBalances by remember {
        mutableStateOf(sharedPrefs.getBoolean("show_balance_total", true))
    }
    val hasBalancesSet = uiState.accounts.any { it.balance > 0.0 }
    val shouldShowReminder = showWalletReminder && !hasBalancesSet

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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp) // Space for radial FAB
            ) {

                // ── Premium Top Card ──────────────────────────────────────
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
                        onToggleShowBalances = {
                            showBalances = !showBalances
                            sharedPrefs.edit().putBoolean("show_balance_total", showBalances).apply()
                        }
                    )
                }

                // ── Setup Reminder (if needed) ───────────────────────────
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
                                    onClick = {
                                        sharedPrefs.edit().putBoolean("wallet_setup_reminder_dismissed", true).apply()
                                        showWalletReminder = false
                                    }
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

                // ── Split Cards & Savings Indicator ───────────────────────
                item {
                    IncomeExpenseSplitCards(
                        monthlyIncome = uiState.monthlyIncome,
                        monthlyExpense = uiState.monthlyExpense,
                        accounts = uiState.accounts,
                        recentTransactions = uiState.recentTransactions,
                        onIncomeShowAllClick = onViewAllIncomeClick,
                        onExpenseShowAllClick = onViewAllTransactionsClick,
                        showBalances = showBalances,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // ── Pinned Templates Strip ────────────────────────────────
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
                                val navController = com.example.presentation.navigation.LocalNavController.current
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
                                    val navController = com.example.presentation.navigation.LocalNavController.current
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
                                            Text(text = template.iconEmoji ?: "📝", fontSize = 18.sp)
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

                // ── Quick actions grid ────────────────────────────────────
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

                // ── Accounts horizontal scroll ────────────────────────────
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
                                        onClick = { onAccountClick(acc.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Line Chart for Expense Trend ──────────────────────────
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

                // ── Budget summary ────────────────────────────────────────
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

                // ── Upcoming subscriptions ────────────────────────────────
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

                // ── Recent transactions header ─────────────────────────────
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

                // ── Recent transactions list ───────────────────────────────
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
            } // end PullToRefreshBox
        } // end Scaffold


        // ── Available Balance Detailed Modal ──────────────────────
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
                                                showBalances = !showBalances
                                                sharedPrefs.edit().putBoolean("show_balance_total", showBalances).apply()
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

// ─────────────────────────────────────────────────────────
//  HomeTopBar — notification bell with optional badge
// ─────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(
    onNotificationClick: () -> Unit,
    onSearchClick: () -> Unit,
    unreadCount: Int,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App logo / title
        Text(
            text = "قداشّ",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Primary,
            modifier = Modifier.testTag("home_app_title")
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search button
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), CircleShape)
                    .clip(CircleShape)
                    .testTag("home_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Notification bell with badge
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Primary.copy(alpha = 0.12f), CircleShape)
                    .clip(CircleShape)
                    .testTag("home_notification_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = ExpenseRed,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "الإشعارات",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────
//  PremiumTopBalanceCard — Edge-to-edge top wallet card
// ─────────────────────────────────────────────────────────

@Composable
private fun PremiumTopBalanceCard(
    totalBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double,
    unreadCount: Int,
    onNotificationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransferClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onCardClick: () -> Unit,
    showBalances: Boolean,
    onToggleShowBalances: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E1E1E), // Greyish black
            Color(0xFF121212)  // Jet Black
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .statusBarsPadding()
                .padding(top = 12.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            // Background decoration circles (glassmorphism look)
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-10).dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-20).dp, y = 25.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top row: App title + Search & Notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قداشّ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onSearchClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Notification Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onNotificationClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(
                                            containerColor = ExpenseRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "الإشعارات",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Greeting section (matches "Hello, James!" layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مرحباً بك 👋",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تبدو ميزانيتك بأمان اليوم",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    // Avatar box (dark/semi-trans background, profile icon)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "الملف الشخصي",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Wallet / Balance Label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "الرصيد المتوفر (دج)",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    // Show/hide eye icon
                    IconButton(
                        onClick = onToggleShowBalances,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showBalances) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showBalances) "إخفاء الرصيد" else "إظهار الرصيد",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Balance amount & savings rate badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val balanceText = remember(totalBalance, showBalances) {
                        if (showBalances) FormatterUtils.formatCurrency(totalBalance) else "•••• دج"
                    }
                    val balanceFontSize = remember(balanceText) {
                        val length = balanceText.length
                        when {
                            length > 18 -> 22.sp
                            length > 15 -> 26.sp
                            length > 12 -> 30.sp
                            else -> 36.sp
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = balanceText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = balanceFontSize,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (showBalances) {
                            val colloquialText = remember(totalBalance) {
                                FormatterUtils.formatColloquialAlgerian(totalBalance)
                            }
                            if (colloquialText != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "أي ما يعادل: $colloquialText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Percentage change / savings rate badge (matching image "+8.56%")
                    val savingsRatio = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense) / monthlyIncome).coerceIn(0.0, 1.0).toFloat() else 0f
                    val savingsPercent = (savingsRatio * 100).toInt()

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "+$savingsPercent%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (savingsPercent > 0) IncomeGreen else Primary
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom actions row (matches image layout: Pill Add button + 3 circular buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Add +" button (wide pill button)
                    Button(
                        onClick = onAddTransactionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "إضافة +",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Calendar button (navigates to financial plans/calendar)
                    val navController = com.example.presentation.navigation.LocalNavController.current
                    IconButton(
                        onClick = { navController?.navigate(com.example.presentation.navigation.Screen.Transactions.route) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "التقويم والخطط",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Wallet icon button (view accounts)
                    IconButton(
                        onClick = onCardClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "الحسابات",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings button (navigates to settings)
                    IconButton(
                        onClick = { navController?.navigate(com.example.presentation.navigation.Screen.Settings.route) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────
//  IncomeExpenseSplitCards — savings bar and split cards
// ─────────────────────────────────────────────────────────

@Composable
private fun IncomeExpenseSplitCards(
    monthlyIncome: Double,
    monthlyExpense: Double,
    accounts: List<Account> = emptyList(),
    recentTransactions: List<Transaction> = emptyList(),
    onIncomeShowAllClick: () -> Unit,
    onExpenseShowAllClick: () -> Unit,
    showBalances: Boolean,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    var showSavingsDetailDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Savings rate card
        val savingsRatio = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense) / monthlyIncome).coerceIn(0.0, 1.0).toFloat() else 0f
        var animationTriggered by remember { mutableStateOf(false) }
        val animatedProgress by animateFloatAsState(
            targetValue = if (animationTriggered) savingsRatio else 0f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            label = "SavingsProgressAnimation"
        )
        LaunchedEffect(savingsRatio) {
            animationTriggered = true
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSavingsDetailDialog = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circle percent progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = IncomeGreen,
                        strokeWidth = 5.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "معدل الادخار الشهري",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val (statusText, statusIcon, iconColor) = when {
                            savingsRatio >= 0.5f -> Triple(
                                "أداء ممتاز! تبني مستقبلاً مالياً آمناً",
                                Icons.Default.AutoAwesome,
                                IncomeGreen
                            )
                            savingsRatio >= 0.2f -> Triple(
                                "أداء جيد، حاول زيادة مدخراتك قليلاً",
                                Icons.Default.CheckCircle,
                                SavingsAmber
                            )
                            else -> Triple(
                                "مصاريفك مرتفعة بالنسبة لدخلك هذا الشهر",
                                Icons.Default.Info,
                                ExpenseRed
                            )
                        }

                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "عرض التفاصيل",
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Savings details dialog
        if (showSavingsDetailDialog) {
            AlertDialog(
                onDismissRequest = { showSavingsDetailDialog = false },
                title = {
                    Text(
                        text = "تفاصيل الادخار الشهري",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Summary breakdown card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي الدخل:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                                    Text(
                                        text = FormatterUtils.formatCurrency(monthlyIncome),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي المصاريف:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                                    Text(
                                        text = FormatterUtils.formatCurrency(monthlyExpense),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("الصافي المدخر:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                                    val netSaved = monthlyIncome - monthlyExpense
                                    Text(
                                        text = FormatterUtils.formatCurrency(netSaved),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netSaved >= 0) IncomeGreen else ExpenseRed
                                    )
                                }
                            }
                        }

                        // Direct transfers to savings accounts
                        val savingsAccounts = accounts.filter { it.type == AccountType.SAVINGS }
                        val savingsAccIds = savingsAccounts.map { it.id }.toSet()
                        
                        val directSavingTxs = recentTransactions.filter { tx ->
                            tx.type == TransactionType.TRANSFER && tx.toAccountId in savingsAccIds
                        }

                        Text(
                            text = "تحويلات حسابات الادخار:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (directSavingTxs.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                directSavingTxs.forEach { tx ->
                                    val accFromName = accounts.find { it.id == tx.accountId }?.name ?: "حساب"
                                    val accToName = savingsAccounts.find { it.id == tx.toAccountId }?.name ?: "ادخار"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "تحويل: $accFromName ➔ $accToName",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = FormatterUtils.formatDate(tx.date),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextGray
                                            )
                                        }
                                        Text(
                                            text = FormatterUtils.formatCurrency(tx.amount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "لا توجد تحويلات مباشرة لحسابات الادخار هذا الشهر. الادخار يمثل الفارق الإيجابي بين مداخيلك ومصاريفك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSavingsDetailDialog = false }) {
                        Text("إغلاق", color = Primary)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Two split cards at the bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Income Card (Right in Arabic RTL)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IncomeGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "دخل",
                                tint = IncomeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الدخل",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (showBalances) FormatterUtils.formatCurrency(monthlyIncome) else "•••• دج",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onIncomeShowAllClick() }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "إظهار الكل",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expense Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ExpenseRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = "مصروفات",
                                tint = ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "المصاريف",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (showBalances) FormatterUtils.formatCurrency(monthlyExpense) else "•••• دج",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onExpenseShowAllClick() }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "إظهار الكل",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}





// ─────────────────────────────────────────────────────────


//  QuickActionTile — compact action card with gradient icon
// ─────────────────────────────────────────────────────────

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    iconBg: Brush,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .testTag("quick_action_$label"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BalanceHeroCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Card Skeleton (Matching exact BalanceHeroCard height and shape)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .shimmerEffect(ShapeTokens.Xl)
        )
        // Two split cards skeletons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .shimmerEffect(ShapeTokens.Lg)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .shimmerEffect(ShapeTokens.Lg)
            )
        }
    }
}

@Composable
private fun AccountsRowSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(3) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(105.dp)
                    .shimmerEffect(ShapeTokens.Lg)
            )
        }
    }
}

@Composable
private fun ChartSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .shimmerEffect(RoundedCornerShape(20.dp))
    )
}

@Composable
private fun TransactionListSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shimmerEffect(ShapeTokens.Md)
            )
        }
    }
}

