package com.qdash.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.presentation.ai.AccountBalanceItem
import com.qdash.presentation.ai.WalletSnapshot
import com.qdash.ui.theme.TextGray
import com.qdash.ui.designsystem.tokens.ShapeTokens

/**
 * AI chat card that shows total portfolio balance (large) and individual account
 * balances in compact rows. Rendered when the AI responds to a general balance query.
 *
 * Design: Notion-inspired monochrome, light 1dp border, no heavy shadow, full RTL.
 */
@Composable
fun WalletAccountsBalanceCard(
    snapshot: WalletSnapshot,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth(),
        shape = ShapeTokens.Xl,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Header row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "رصيد المحفظة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextGray
                    )
                }

                // Toggle accounts list
                if (snapshot.accounts.isNotEmpty()) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "طي" else "توسيع",
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Total balance (hero number) ─────────────────────────────────
            val balanceText = remember(snapshot.totalBalance) {
                FormatterUtils.formatCurrency(snapshot.totalBalance)
            }
            val balanceFontSize = remember(balanceText) {
                when {
                    balanceText.length > 18 -> 20.sp
                    balanceText.length > 14 -> 24.sp
                    else -> 28.sp
                }
            }
            Text(
                text = balanceText,
                fontSize = balanceFontSize,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
            Text(
                text = "الإجمالي الكلي · ${snapshot.currency}",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )

            // ── Account rows ────────────────────────────────────────────────
            if (snapshot.accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        snapshot.accounts.forEachIndexed { index, account ->
                            AccountBalanceRow(account = account)
                            if (index < snapshot.accounts.lastIndex) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                if (!expanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "عرض ${snapshot.accounts.size} حسابات ←",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { expanded = true }
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountBalanceRow(account: AccountBalanceItem) {
    val accentColor = remember(account.color) {
        try {
            Color(android.graphics.Color.parseColor(
                account.color.let { if (it.startsWith("#")) it else "#$it" }
            ))
        } catch (_: Exception) {
            Color(0xFF6B7280) // fallback gray
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: colored dot + account name + type
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = account.typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
        }

        // Right: balance
        Text(
            text = FormatterUtils.formatCurrency(account.balance),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
