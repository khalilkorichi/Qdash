package com.qdash.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    accountName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    currentViewedAccountId: Long? = null,
    toAccountName: String? = null
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isTransfer = transaction.type == TransactionType.TRANSFER
    val isIncomingTransfer = isTransfer && transaction.toAccountId == currentViewedAccountId
    val isOutgoingTransfer = isTransfer && transaction.accountId == currentViewedAccountId

    val amountPrefix = when {
        isExpense || isOutgoingTransfer -> "-"
        isIncomingTransfer -> "+"
        else -> ""
    }

    val accentColor = when {
        isExpense || isOutgoingTransfer -> ExpenseRed
        isIncomingTransfer -> IncomeGreen
        else -> TransferBlue
    }

    val textColor = when {
        isExpense || isOutgoingTransfer -> ExpenseRed
        isIncomingTransfer -> IncomeGreen
        else -> TransferBlue
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
    val amountText = FormatterUtils.formatCurrency(transaction.amount, amountPrefix)

    val primaryColor = MaterialTheme.colorScheme.primary
    val cardModifier = if (onLongClick != null) {
        modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    } else {
        modifier
    }

    AppCard(
        modifier = cardModifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        variant = CardVariant.INTERACTIVE,
        shape = ShapeTokens.Xl,
        backgroundColor = if (isSelected) primaryColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        borderStroke = if (isSelected) BorderStroke(2.dp, primaryColor) else null,
        onClick = if (onLongClick != null) null else onClick
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
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "محدد",
                        tint = primaryColor,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                    )
                }

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

                Column(modifier = Modifier.weight(1f)) {
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
                        val subtitleText = buildString {
                            if (isTransfer && toAccountName != null && currentViewedAccountId == null) {
                                append("$accountName ← $toAccountName")
                            } else {
                                append(accountName)
                            }
                            category?.name?.let {
                                append(" • ")
                                append(it)
                            }
                            append(" • ")
                            val dateText = if (transaction.occurredAt != null) {
                                FormatterUtils.formatDateTime(transaction.occurredAt)
                            } else {
                                FormatterUtils.formatDate(transaction.date)
                            }
                            append(dateText)
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
