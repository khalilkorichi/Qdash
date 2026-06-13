package com.example.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.FormatterUtils
import com.example.domain.model.*
import com.example.ui.theme.*
import com.example.ui.designsystem.components.*
import com.example.ui.designsystem.tokens.*

@Composable
fun UnifiedScreenHeader(
    title: String,
    subtitle: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    AppSectionHeader(
        title = title,
        description = subtitle,
        showBackButton = showBackButton,
        onBackClick = onBackClick,
        actions = { actions() }
    )
}

@Composable
fun FinTrackTopBar(
    title: String,
    onNotificationClick: () -> Unit = {},
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val Primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onBackClick() }
                    .testTag("back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            val navController = com.example.presentation.navigation.LocalNavController.current
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f))
                    .clickable { 
                        navController?.navigate(com.example.presentation.navigation.Screen.Notifications.route) ?: onNotificationClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "الإشعارات",
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                letterSpacing = (-0.3).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            actions()
            if (!showBackButton) {
                val navController = com.example.presentation.navigation.LocalNavController.current
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            navController?.navigate(com.example.presentation.navigation.Screen.Search.route)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceHeroCard(
    totalBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double,
    modifier: Modifier = Modifier,
    onIncomeShowAllClick: () -> Unit = {},
    onExpenseShowAllClick: () -> Unit = {},
    onCardClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("kdach_prefs", android.content.Context.MODE_PRIVATE) }
    var showBalances by remember {
        mutableStateOf(sharedPrefs.getBoolean("show_balance_total", true))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Card: Total Balance
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardClick() }
                .testTag("balance_hero_card_top"),
            variant = CardVariant.SOLID,
            shape = ShapeTokens.Xl,
            backgroundColor = Primary
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                // Decoration circles for depth
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-20).dp, y = 30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                )

                Column {
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
                                text = "إجمالي الرصيد المتوفر",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        IconButton(
                            onClick = {
                                showBalances = !showBalances
                                sharedPrefs.edit().putBoolean("show_balance_total", showBalances).apply()
                            },
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
                    val balanceText = remember(totalBalance, showBalances) {
                        if (showBalances) FormatterUtils.formatCurrency(totalBalance) else "•••• دج"
                    }
                    val balanceFontSize = remember(balanceText) {
                        val length = balanceText.length
                        when {
                            length > 18 -> 20.sp
                            length > 15 -> 24.sp
                            length > 12 -> 28.sp
                            else -> 34.sp
                        }
                    }
                    Text(
                        text = balanceText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = balanceFontSize,
                            letterSpacing = (-1).sp
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Savings progress indicator
                    val savingsRatio = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense) / monthlyIncome).coerceIn(0.0, 1.0).toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { savingsRatio },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = IncomeGreen,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "معدل الادخار: ${(savingsRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Two split cards at the bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Income Card (Right in Arabic RTL)
            AppCard(
                modifier = Modifier.weight(1f),
                variant = CardVariant.FLAT,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surface
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
            AppCard(
                modifier = Modifier.weight(1f),
                variant = CardVariant.FLAT,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surface
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
                                contentDescription = "مصروف",
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

@Composable
fun AccountCard(
    account: Account,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("kdach_prefs", android.content.Context.MODE_PRIVATE) }
    var showBalance by remember(account.id) {
        mutableStateOf(sharedPrefs.getBoolean("show_balance_acc_${account.id}", true))
    }

    val accentColor = when (account.type) {
        AccountType.BARIDIMOB -> Color(0xFF005CA9)
        AccountType.CCP -> Color(0xFFF59E0B)
        AccountType.CASH -> Color(0xFF22C55E)
        AccountType.SAVINGS -> Color(0xFF3B82F6)
        else -> Primary
    }

    AppCard(
        modifier = modifier
            .width(160.dp)
            .height(105.dp)
            .border(1.dp, accentColor.copy(alpha = 0.12f), ShapeTokens.Lg)
            .testTag("account_card_${account.id}"),
        variant = CardVariant.INTERACTIVE,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(accentColor.copy(alpha = 0.03f))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (account.type == AccountType.BARIDIMOB) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_baridimob),
                        contentDescription = "بريدي موب",
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = when (account.type) {
                            AccountType.CCP -> Icons.Default.CreditCard
                            AccountType.CASH -> Icons.Default.Payments
                            AccountType.SAVINGS -> Icons.Default.Savings
                            else -> Icons.Default.AccountBalanceWallet
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الرصيد المتوفر",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextGray
                    )
                    IconButton(
                        onClick = {
                            showBalance = !showBalance
                            sharedPrefs.edit().putBoolean("show_balance_acc_${account.id}", showBalance).apply()
                        },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "إخفاء/إظهار",
                            tint = TextGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (showBalance) FormatterUtils.formatCurrency(account.balance) else "•••• دج",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    accountName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isTransfer = transaction.type == TransactionType.TRANSFER
    val accentColor = when {
        isExpense -> ExpenseRed
        isTransfer -> TransferBlue
        else -> IncomeGreen
    }

    val catColor = category?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: accentColor
    val icon = category?.icon ?: "receipt"
    val vectorIcon = when (icon) {
        "person" -> Icons.Default.Person
        "groups" -> Icons.Default.Groups
        "home" -> Icons.Default.Home
        "restaurant" -> Icons.Default.Restaurant
        "directions_car" -> Icons.Default.DirectionsCar
        "receipt_long" -> Icons.Default.ReceiptLong
        "shopping_bag" -> Icons.Default.ShoppingBag
        "medical_services" -> Icons.Default.MedicalServices
        "school" -> Icons.Default.School
        "sports_esports" -> Icons.Default.SportsEsports
        "work" -> Icons.Default.Work
        else -> Icons.Default.Receipt
    }
    val amountText = (if (isExpense) "-" else "+") + FormatterUtils.formatCurrency(transaction.amount)
    val textColor = if (isExpense) ExpenseRed else IncomeGreen

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        variant = CardVariant.INTERACTIVE,
        shape = ShapeTokens.Md,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon badge with gradient
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(ShapeTokens.Md)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(catColor.copy(alpha = 0.25f), catColor.copy(alpha = 0.1f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = vectorIcon,
                        contentDescription = category?.name,
                        tint = catColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.note ?: category?.name ?: "عملية مالية",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$accountName • ${FormatterUtils.formatDate(transaction.date)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                    color = textColor
                )
                if (isTransfer) {
                    Text(
                        text = "تحويل",
                        style = MaterialTheme.typography.labelSmall,
                        color = TransferBlue
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = Color(android.graphics.Color.parseColor(category.color))
    val containerColor = if (isSelected) catColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("category_chip_${category.id}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val icon = category.icon
            val vectorIcon = when (icon) {
                "person" -> Icons.Default.Person
                "groups" -> Icons.Default.Groups
                "home" -> Icons.Default.Home
                "restaurant" -> Icons.Default.Restaurant
                "directions_car" -> Icons.Default.DirectionsCar
                "receipt_long" -> Icons.Default.ReceiptLong
                "shopping_bag" -> Icons.Default.ShoppingBag
                "medical_services" -> Icons.Default.MedicalServices
                "school" -> Icons.Default.School
                "sports_esports" -> Icons.Default.SportsEsports
                "work" -> Icons.Default.Work
                else -> Icons.Default.Receipt
            }

            Icon(
                imageVector = vectorIcon,
                contentDescription = null,
                tint = if (isSelected) Color.White else catColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun BudgetProgressCard(
    category: Category,
    currentUsage: Double,
    modifier: Modifier = Modifier
) {
    val limit = category.budgetLimit ?: return
    val ratio = currentUsage / limit
    val progress = ratio.coerceIn(0.0, 1.0).toFloat()
    val percentValue = (ratio * 100).toInt()

    val catColor = Color(android.graphics.Color.parseColor(category.color))
    val barColor = if (ratio >= 0.8) ExpenseRed else catColor

    AppCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.OUTLINED,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(catColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ميزانية ${category.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "$percentValue%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المستهلك: ${FormatterUtils.formatCurrency(currentUsage)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "الأقصى: ${FormatterUtils.formatCurrency(limit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (ratio >= 1.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "لقد تجاوزت الحد المسموح به لهذه الميزانية!",
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (ratio >= 0.8) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SavingsAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "انتباه: لقد استهلكت أكثر من 80% من ميزانيتك!",
                        style = MaterialTheme.typography.labelSmall,
                        color = SavingsAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsGoalCard(
    goal: SavingGoal,
    onAddDeposit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val percentValue = (progress * 100).toInt()
    val goalColor = Color(android.graphics.Color.parseColor(goal.color))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("saving_goal_card_${goal.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(goalColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = goalColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (goal.deadline != null) {
                            Text(
                                text = "تاريخ الاستحقاق: ${FormatterUtils.formatDate(goal.deadline)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "$percentValue%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = goalColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = goalColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "المدخر حالياً: ${FormatterUtils.formatCurrency(goal.currentAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "الهدف: ${FormatterUtils.formatCurrency(goal.targetAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddDeposit,
                    colors = ButtonDefaults.buttonColors(containerColor = goalColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ادخار",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionItem(
    subscription: Subscription,
    onToggleActive: (Boolean) -> Unit,
    accountName: String,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subscription_item_${subscription.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$accountName • تجديد فى ${FormatterUtils.formatShortDate(subscription.nextBillingDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = FormatterUtils.formatCurrency(subscription.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Switch(
                    checked = subscription.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Radial glowing icon container
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Primary.copy(alpha = 0.18f), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Primary.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
fun DriveSyncCard(
    lastBackupDate: String,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = Primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "النسخ الاحتياطي عبر Google Drive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "آخر نسخة احتياطية: $lastBackupDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onBackupClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نسخ الآن", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRestoreClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استعادة", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
