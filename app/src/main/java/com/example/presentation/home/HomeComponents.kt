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
            text = "ظ‚ط¯ط§ط´ظ‘",
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
                    contentDescription = "ط¨ط­ط«",
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
                        contentDescription = "ط§ظ„ط¥ط´ط¹ط§ط±ط§طھ",
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
                        text = "ظ‚ط¯ط§ط´ظ‘",
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
                                contentDescription = "ط¨ط­ط«",
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
                                    contentDescription = "ط§ظ„ط¥ط´ط¹ط§ط±ط§طھ",
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
                            text = "ظ…ط±ط­ط¨ط§ظ‹ ط¨ظƒ ًں‘‹",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "طھط¨ط¯ظˆ ظ…ظٹط²ط§ظ†ظٹطھظƒ ط¨ط£ظ…ط§ظ† ط§ظ„ظٹظˆظ…",
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
                            contentDescription = "ط§ظ„ظ…ظ„ظپ ط§ظ„ط´ط®طµظٹ",
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
                        text = "ط§ظ„ط±طµظٹط¯ ط§ظ„ظ…طھظˆظپط± (ط¯ط¬)",
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
                            contentDescription = if (showBalances) "ط¥ط®ظپط§ط، ط§ظ„ط±طµظٹط¯" else "ط¥ط¸ظ‡ط§ط± ط§ظ„ط±طµظٹط¯",
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
                        if (showBalances) FormatterUtils.formatCurrency(totalBalance) else "â€¢â€¢â€¢â€¢ ط¯ط¬"
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
                                    text = "ط£ظٹ ظ…ط§ ظٹط¹ط§ط¯ظ„: $colloquialText",
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
                                text = "ط¥ط¶ط§ظپط© +",
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
                            contentDescription = "ط§ظ„طھظ‚ظˆظٹظ… ظˆط§ظ„ط®ط·ط·",
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
                            contentDescription = "ط§ظ„ط­ط³ط§ط¨ط§طھ",
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
                            contentDescription = "ط§ظ„ط¥ط¹ط¯ط§ط¯ط§طھ",
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
                        text = "ظ…ط¹ط¯ظ„ ط§ظ„ط§ط¯ط®ط§ط± ط§ظ„ط´ظ‡ط±ظٹ",
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
                                "ط£ط¯ط§ط، ظ…ظ…طھط§ط²! طھط¨ظ†ظٹ ظ…ط³طھظ‚ط¨ظ„ط§ظ‹ ظ…ط§ظ„ظٹط§ظ‹ ط¢ظ…ظ†ط§ظ‹",
                                Icons.Default.AutoAwesome,
                                IncomeGreen
                            )
                            savingsRatio >= 0.2f -> Triple(
                                "ط£ط¯ط§ط، ط¬ظٹط¯طŒ ط­ط§ظˆظ„ ط²ظٹط§ط¯ط© ظ…ط¯ط®ط±ط§طھظƒ ظ‚ظ„ظٹظ„ط§ظ‹",
                                Icons.Default.CheckCircle,
                                SavingsAmber
                            )
                            else -> Triple(
                                "ظ…طµط§ط±ظٹظپظƒ ظ…ط±طھظپط¹ط© ط¨ط§ظ„ظ†ط³ط¨ط© ظ„ط¯ط®ظ„ظƒ ظ‡ط°ط§ ط§ظ„ط´ظ‡ط±",
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
                    contentDescription = "ط¹ط±ط¶ ط§ظ„طھظپط§طµظٹظ„",
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
                        text = "طھظپط§طµظٹظ„ ط§ظ„ط§ط¯ط®ط§ط± ط§ظ„ط´ظ‡ط±ظٹ",
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
                                    Text("ط¥ط¬ظ…ط§ظ„ظٹ ط§ظ„ط¯ط®ظ„:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
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
                                    Text("ط¥ط¬ظ…ط§ظ„ظٹ ط§ظ„ظ…طµط§ط±ظٹظپ:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
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
                                    Text("ط§ظ„طµط§ظپظٹ ط§ظ„ظ…ط¯ط®ط±:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
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
                            text = "طھط­ظˆظٹظ„ط§طھ ط­ط³ط§ط¨ط§طھ ط§ظ„ط§ط¯ط®ط§ط±:",
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
                                    val accFromName = accounts.find { it.id == tx.accountId }?.name ?: "ط­ط³ط§ط¨"
                                    val accToName = savingsAccounts.find { it.id == tx.toAccountId }?.name ?: "ط§ط¯ط®ط§ط±"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "طھط­ظˆظٹظ„: $accFromName â‍” $accToName",
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
                                text = "ظ„ط§ طھظˆط¬ط¯ طھط­ظˆظٹظ„ط§طھ ظ…ط¨ط§ط´ط±ط© ظ„ط­ط³ط§ط¨ط§طھ ط§ظ„ط§ط¯ط®ط§ط± ظ‡ط°ط§ ط§ظ„ط´ظ‡ط±. ط§ظ„ط§ط¯ط®ط§ط± ظٹظ…ط«ظ„ ط§ظ„ظپط§ط±ظ‚ ط§ظ„ط¥ظٹط¬ط§ط¨ظٹ ط¨ظٹظ† ظ…ط¯ط§ط®ظٹظ„ظƒ ظˆظ…طµط§ط±ظٹظپظƒ.",
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
                        Text("ط¥ط؛ظ„ط§ظ‚", color = Primary)
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
                                contentDescription = "ط¯ط®ظ„",
                                tint = IncomeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ط§ظ„ط¯ط®ظ„",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (showBalances) FormatterUtils.formatCurrency(monthlyIncome) else "â€¢â€¢â€¢â€¢ ط¯ط¬",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Income change percent badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background((if (incomeChangePercent >= 0) IncomeGreen else ExpenseRed).copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (incomeChangePercent >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (incomeChangePercent >= 0) IncomeGreen else ExpenseRed,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", kotlin.math.abs(incomeChangePercent)),
                            fontSize = 9.sp,
                            color = if (incomeChangePercent >= 0) IncomeGreen else ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }

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
                            text = "ط¥ط¸ظ‡ط§ط± ط§ظ„ظƒظ„",
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
                                contentDescription = "ظ…طµط±ظˆظپط§طھ",
                                tint = ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ط§ظ„ظ…طµط§ط±ظٹظپ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (showBalances) FormatterUtils.formatCurrency(monthlyExpense) else "â€¢â€¢â€¢â€¢ ط¯ط¬",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Expense change percent badge (decrease is green/good, increase is red/bad)
                    val isExpenseDecreased = expenseChangePercent <= 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background((if (isExpenseDecreased) IncomeGreen else ExpenseRed).copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (expenseChangePercent >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isExpenseDecreased) IncomeGreen else ExpenseRed,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", kotlin.math.abs(expenseChangePercent)),
                            fontSize = 9.sp,
                            color = if (isExpenseDecreased) IncomeGreen else ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }

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
                            text = "ط¥ط¸ظ‡ط§ط± ط§ظ„ظƒظ„",
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
    val emoji: String,
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
                emoji = "ًںŒ…",
                suggestionText = "طµط¨ط§ط­ ط§ظ„ط®ظٹط±! ظ‡ظ„ ط¯ظپط¹طھ ط«ظ…ظ† ظ…ظˆط§طµظ„ط§طھ ط§ظ„ط¹ظ…ظ„ ط§ظ„ظٹظˆظ…طں",
                targetKeyword = "ظ…ظˆط§طµظ„ط§طھ",
                defaultAmount = 150.0,
                note = "طھط§ظƒط³ظٹ / ط­ط§ظپظ„ط© ط§ظ„ط¹ظ…ظ„",
                title = "ظ…ظˆط§طµظ„ط§طھ ط§ظ„طµط¨ط§ط­"
            )
            in 12..16 -> ContextTemplateData(
                emoji = "âک•",
                suggestionText = "ظˆظ‚طھ ط§ظ„ط§ط³طھط±ط§ط­ط©! ظ‡ظ„ طھط±ط؛ط¨ ظپظٹ طھط³ط¬ظٹظ„ ظˆط¬ط¨ط© ط§ظ„ط؛ط¯ط§ط، ط£ظˆ ظ‚ظ‡ظˆط©طں",
                targetKeyword = "ط·ط¹ط§ظ…",
                defaultAmount = 450.0,
                note = "ط؛ط¯ط§ط، / ظ‚ظ‡ظˆط© ط§ط³طھط±ط§ط­ط©",
                title = "ظˆط¬ط¨ط© ط§ظ„ط؛ط¯ط§ط،"
            )
            in 17..21 -> ContextTemplateData(
                emoji = "ًں›’",
                suggestionText = "ظ…ط³ط§ط، ط§ظ„ط®ظٹط±! ظ‡ظ„ ظ‚ظ…طھ ط¨ط´ط±ط§ط، ط§ظ„ط¨ظ‚ط§ظ„ط© ظˆظ…ط³طھظ„ط²ظ…ط§طھ ط§ظ„ظ…ظ†ط²ظ„طں",
                targetKeyword = "ط·ط¹ط§ظ…",
                defaultAmount = 1200.0,
                note = "ظ…ط´طھط±ظٹط§طھ ط§ظ„ط¨ظ‚ط§ظ„ط© ط§ظ„ظ…ظ†ط²ظ„ظٹط©",
                title = "ط¹ط´ط§ط، / ط¨ظ‚ط§ظ„ط© ط§ظ„ظ…ط³ط§ط،"
            )
            else -> ContextTemplateData(
                emoji = "ًںŒ™",
                suggestionText = "ط³ظ‡ط±ط© ط³ط¹ظٹط¯ط©! ظ‡ظ„ ظ‚ظ…طھ ط¨طھط¹ط¨ط¦ط© ط±طµظٹط¯ ظ‡ط§طھظپ ط£ظˆ ط¥ظ†طھط±ظ†طھطں",
                targetKeyword = "ظ…ظ†ط²ظ„ظٹ",
                defaultAmount = 500.0,
                note = "طھط¹ط¨ط¦ط© ط±طµظٹط¯ ط¥ظ†طھط±ظ†طھ / ظ‡ط§طھظپ",
                title = "ط±طµظٹط¯ ط¥ظ†طھط±ظ†طھ / ظ…ظƒط§ظ„ظ…ط§طھ"
            )
        }
    }
    val emoji = templateData.emoji
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
                Text(text = emoji, fontSize = 24.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ط§ظ‚طھط±ط§ط­ ط°ظƒظٹ ط­ط³ط¨ ط§ظ„ظˆظ‚طھ",
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
                        text = "ط³ط¬ظ„ ط§ظ„ط¢ظ† (${FormatterUtils.formatCurrency(defaultAmount)})",
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

