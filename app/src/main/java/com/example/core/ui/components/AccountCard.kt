package com.example.core.ui.components

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
import com.example.core.utils.FormatterUtils
import com.example.domain.model.*
import com.example.ui.theme.*
import com.example.ui.designsystem.components.*
import com.example.ui.designsystem.tokens.*


@Composable
fun AccountCard(
    account: Account,
    showBalance: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        onClick = onToggleBalanceVisibility,
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
