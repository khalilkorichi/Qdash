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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  HomeTopBar â€” notification bell with optional badge
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun HomeTopBar(
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


// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PremiumTopBalanceCard â€” Edge-to-edge top wallet card
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun PremiumTopBalanceCard(
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
                    val savingsRatio = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense) / monthlyIncome).toFloat() else 0f
                    val savingsPercent = (savingsRatio * 100).toInt()
                    val badgeText = if (savingsPercent >= 0) "+$savingsPercent%" else "$savingsPercent%"
                    val badgeColor = when {
                        savingsPercent > 0 -> IncomeGreen
                        savingsPercent < 0 -> ExpenseRed
                        else -> Primary
                    }

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
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


// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  IncomeExpenseSplitCards â€” savings bar and split cards
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun IncomeExpenseSplitCards(
    monthlyIncome: Double,
    monthlyExpense: Double,
    incomeChangePercent: Double = 0.0,
    expenseChangePercent: Double = 0.0,
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
        val savingsRatio = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense) / monthlyIncome).toFloat() else 0f
        var animationTriggered by remember { mutableStateOf(false) }
        val animatedProgress by animateFloatAsState(
            targetValue = if (animationTriggered) savingsRatio.coerceIn(0f, 1f) else 0f,
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
                    val savingsPercent = (savingsRatio * 100).toInt()
                    val indicatorColor = if (savingsPercent >= 0) IncomeGreen else ExpenseRed
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = indicatorColor,
                        strokeWidth = 5.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = if (savingsPercent >= 0) "+$savingsPercent%" else "$savingsPercent%",
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
                                    Text("إجمالي الدخل الشهري:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
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
                                    Text("إجمالي المصاريف الشهرية:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                                    Text(
                                        text = FormatterUtils.formatCurrency(monthlyExpense),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("معدل الادخار الشهري:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                                    val savingsPercent = (savingsRatio * 100).toInt()
                                    val rateText = if (savingsPercent >= 0) "+$savingsPercent%" else "$savingsPercent%"
                                    Text(
                                        text = rateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (savingsPercent >= 0) IncomeGreen else ExpenseRed
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("الفائض/العجز الشهري:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                                    val netSaved = monthlyIncome - monthlyExpense
                                    Text(
                                        text = FormatterUtils.formatCurrency(netSaved),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netSaved >= 0) IncomeGreen else ExpenseRed
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("الرصيد الحالي المتوفر:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                                    val currentAvailableBalance = accounts.sumOf { it.balance }
                                    Text(
                                        text = FormatterUtils.formatCurrency(currentAvailableBalance),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
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
                                text = "لا توجد تحويلات مباشرة لحسابات الادخار هذا الشهر. الفائض يمثل الفارق الإيجابي بين مداخيلك ومصاريفك.",
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
                        Column {
                            Text(
                                text = "الدخل",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Income change percent badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background((if (incomeChangePercent >= 0) IncomeGreen else ExpenseRed).copy(alpha = 0.1f))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Icon(
                                    imageVector = if (incomeChangePercent >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (incomeChangePercent >= 0) IncomeGreen else ExpenseRed,
                                    modifier = Modifier.size(8.dp)
                                )
                                Text(
                                    text = FormatterUtils.convertNumerals(String.format(java.util.Locale.US, "%.1f%%", kotlin.math.abs(incomeChangePercent))),
                                    fontSize = 8.sp,
                                    color = if (incomeChangePercent >= 0) IncomeGreen else ExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                        Column {
                            Text(
                                text = "المصاريف",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Expense change percent badge (decrease is green/good, increase is red/bad)
                            val isExpenseDecreased = expenseChangePercent <= 0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background((if (isExpenseDecreased) IncomeGreen else ExpenseRed).copy(alpha = 0.1f))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Icon(
                                    imageVector = if (expenseChangePercent >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isExpenseDecreased) IncomeGreen else ExpenseRed,
                                    modifier = Modifier.size(8.dp)
                                )
                                Text(
                                    text = FormatterUtils.convertNumerals(String.format(java.util.Locale.US, "%.1f%%", kotlin.math.abs(expenseChangePercent))),
                                    fontSize = 8.sp,
                                    color = if (isExpenseDecreased) IncomeGreen else ExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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





// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€


//  QuickActionTile â€” compact action card with gradient icon
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun QuickActionTile(
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
fun BalanceHeroCardSkeleton(modifier: Modifier = Modifier) {
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
fun AccountsRowSkeleton(modifier: Modifier = Modifier) {
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
fun ChartSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .shimmerEffect(RoundedCornerShape(20.dp))
    )
}

@Composable
fun TransactionListSkeleton(modifier: Modifier = Modifier) {
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

private data class ContextTemplateData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val suggestionText: String,
    val targetKeyword: String,
    val defaultAmount: Double,
    val note: String,
    val title: String
)

@Composable
fun ContextAwareTemplateCard(
    categories: List<com.example.domain.model.Category>,
    onSelectTemplate: (String) -> Unit
) {
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val templateData = remember(currentHour) {
        when (currentHour) {
            in 6..11 -> ContextTemplateData(
                icon = Icons.Default.LightMode,
                suggestionText = "صباح الخير! هل دفعت ثمن مواصلات العمل اليوم؟",
                targetKeyword = "مواصلات",
                defaultAmount = 150.0,
                note = "تاكسي / حافلة العمل",
                title = "مواصلات الصباح"
            )
            in 12..16 -> ContextTemplateData(
                icon = Icons.Default.Coffee,
                suggestionText = "وقت الاستراحة! هل ترغب في تسجيل وجبة الغداء أو قهوة؟",
                targetKeyword = "طعام",
                defaultAmount = 450.0,
                note = "غداء / قهوة استراحة",
                title = "وجبة الغداء"
            )
            in 17..21 -> ContextTemplateData(
                icon = Icons.Default.ShoppingCart,
                suggestionText = "مساء الخير! هل قمت بشراء البقالة ومستلزمات المنزل؟",
                targetKeyword = "طعام",
                defaultAmount = 1200.0,
                note = "مشتريات البقالة المنزلية",
                title = "عشاء / بقالة المساء"
            )
            else -> ContextTemplateData(
                icon = Icons.Default.NightsStay,
                suggestionText = "سهرة سعيدة! هل قمت بتعبئة رصيد هاتف أو إنترنت؟",
                targetKeyword = "منزلي",
                defaultAmount = 500.0,
                note = "تعبئة رصيد إنترنت / هاتف",
                title = "رصيد إنترنت / مكالمات"
            )
        }
    }
    val icon = templateData.icon
    val suggestionText = templateData.suggestionText
    val targetKeyword = templateData.targetKeyword
    val defaultAmount = templateData.defaultAmount
    
    val note = templateData.note
    val title = templateData.title

    val targetCategory = categories.find { it.name.contains(targetKeyword) }
    val categoryId = targetCategory?.id

    val Primary = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "اقتراح ذكي حسب الوقت",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = suggestionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val draft = """{"amount":$defaultAmount,"type":"EXPENSE","categoryId":${categoryId ?: "null"},"notes":"$note"}"""
                        onSelectTemplate(draft)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سجل الآن (${FormatterUtils.formatCurrency(defaultAmount)})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

fun getDashboardSections(sharedPrefs: android.content.SharedPreferences): List<String> {
    val defaultOrder = "split_cards,context_templates,templates,quick_actions,accounts,chart,budget,subscriptions,recent_transactions"
    val orderStr = sharedPrefs.getString("dashboard_sections_order", defaultOrder) ?: defaultOrder
    return orderStr.split(",")
}

fun isSectionVisible(sharedPrefs: android.content.SharedPreferences, section: String): Boolean {
    return sharedPrefs.getBoolean("dashboard_show_$section", true)
}

