package com.qdash.presentation.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.AccountType
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.theme.IncomeGreen

@Composable
fun BalanceDetailsDialog(
    showBalanceDetails: Boolean,
    totalBalance: Double,
    showBalances: Boolean,
    accounts: List<Account>,
    accountBalancesVisibility: Map<Long, Boolean>,
    onToggleShowBalances: () -> Unit,
    onToggleAccountBalanceVisibility: (Long) -> Unit,
    onAccountClick: (Long) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color
) {
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
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
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
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Prevent click propagation
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight) {
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
                        // Header row with title & actions
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
                                        .clickable { onToggleShowBalances() },
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
                                        .clickable { onDismiss() },
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
                            text = if (showBalances) FormatterUtils.formatCurrency(totalBalance) else "•••• دج",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp
                            ),
                            color = Color.White
                        )
                        val dialogColloquial = remember(totalBalance, showBalances) {
                            if (showBalances) FormatterUtils.formatColloquialAlgerian(totalBalance) else null
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
                            accounts.forEach { acc ->
                                val accentColor = when (acc.type) {
                                    AccountType.BARIDIMOB -> Color(0xFF005CA9)
                                    AccountType.CCP -> Color(0xFFF59E0B)
                                    AccountType.CASH -> Color(0xFF22C55E)
                                    AccountType.SAVINGS -> Color(0xFF3B82F6)
                                    else -> if (MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight) ColorTokens.TextSecondaryDark else primaryColor
                                }

                                Surface(
                                    onClick = {
                                        onDismiss()
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
                                                if (acc.type == AccountType.BARIDIMOB) {
                                                    Image(
                                                        painter = painterResource(id = com.qdash.R.drawable.ic_baridimob),
                                                        contentDescription = "بريدي موب",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = when (acc.type) {
                                                            AccountType.CCP -> Icons.Default.CreditCard
                                                            AccountType.CASH -> Icons.Default.Payments
                                                            AccountType.SAVINGS -> Icons.Default.Savings
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
                                                        AccountType.BARIDIMOB -> "بريدي موب"
                                                        AccountType.CCP -> "حساب جاري CCP"
                                                        AccountType.CASH -> "نقد سلّة"
                                                        AccountType.SAVINGS -> "ادخار"
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
                                                text = if (accountBalancesVisibility[acc.id] ?: true) FormatterUtils.formatCurrency(acc.balance) else "•••• دج",
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
}
