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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    accountName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null
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
        modifier = if (isSelected) {
            cardModifier
                .fillMaxWidth()
                .border(2.dp, primaryColor, ShapeTokens.Md)
                .testTag("transaction_item_${transaction.id}")
        } else {
            cardModifier
                .fillMaxWidth()
                .testTag("transaction_item_${transaction.id}")
        },
        variant = CardVariant.INTERACTIVE,
        shape = ShapeTokens.Md,
        backgroundColor = if (isSelected) primaryColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
