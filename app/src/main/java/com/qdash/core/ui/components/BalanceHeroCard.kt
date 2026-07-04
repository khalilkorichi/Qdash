package com.qdash.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.*
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*


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
                    val savingsRatio = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense) / monthlyIncome).toFloat() else 0f
                    val savingsPercent = (savingsRatio * 100).toInt()
                    val savingsPercentText = if (savingsPercent >= 0) "+$savingsPercent%" else "$savingsPercent%"
                    LinearProgressIndicator(
                        progress = { savingsRatio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = if (savingsPercent >= 0) IncomeGreen else ExpenseRed,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "معدل الادخار: $savingsPercentText",
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
