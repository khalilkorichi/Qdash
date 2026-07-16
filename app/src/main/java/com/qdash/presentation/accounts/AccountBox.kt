package com.qdash.presentation.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.qdash.core.ui.StableList
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.AccountType
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.components.getIconByName
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray
import com.qdash.ui.theme.TransferBlue
import kotlin.math.roundToInt

private fun parseColor(hex: String, primaryColor: Color): Color {
    val accountColorPalette = listOf(
        "#6C63FF" to primaryColor,
        "#22C55E" to IncomeGreen,
        "#EF4444" to ExpenseRed,
        "#3B82F6" to TransferBlue,
        "#F59E0B" to SavingsAmber,
        "#06B6D4" to Color(0xFF06B6D4),
        "#8B5CF6" to Color(0xFF8B5CF6),
        "#EC4899" to Color(0xFFEC4899)
    )
    return accountColorPalette.firstOrNull { it.first == hex }?.second
        ?: try {
            val cleaned = hex.trimStart('#')
            Color(android.graphics.Color.parseColor("#$cleaned"))
        } catch (e: Exception) {
            primaryColor
        }
}

private fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.BARIDIMOB -> "بريدي موب"
    AccountType.CCP       -> "CCP"
    AccountType.CASH      -> "نقدي"
    AccountType.BANK      -> "بنك"
    AccountType.SAVINGS   -> "توفير"
    AccountType.WALLET    -> "محفظة"
    AccountType.OTHER     -> "أخرى"
}

private fun accountTypeIcon(type: AccountType): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
    AccountType.BARIDIMOB -> Icons.Default.PhoneAndroid
    AccountType.CCP       -> Icons.Default.CreditCard
    AccountType.CASH      -> Icons.Default.Payments
    AccountType.BANK      -> Icons.Default.AccountBalance
    AccountType.SAVINGS   -> Icons.Default.Savings
    AccountType.WALLET    -> Icons.Default.AccountBalanceWallet
    AccountType.OTHER     -> Icons.Default.MonetizationOn
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountItemCard(
    account: Account,
    transactions: StableList<Transaction>,
    categories: StableList<Category>,
    showBalance: Boolean,
    onToggleBalance: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTxFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"
    val Primary = MaterialTheme.colorScheme.primary
    val accentColor = parseColor(account.color, Primary)

    val filteredTransactions = remember(transactions, selectedTxFilter, account.id) {
        transactions.items.filter { tx ->
            val isIncoming = tx.type == TransactionType.INCOME || (tx.type == TransactionType.TRANSFER && tx.toAccountId == account.id)
            when (selectedTxFilter) {
                "INCOME" -> isIncoming
                "EXPENSE" -> !isIncoming
                else -> true
            }
        }.take(5)
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("account_card_${account.id}"),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderStroke = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 16.dp, end = 8.dp, top = 0.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon (visually right side in RTL, so first in Row)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (account.type == AccountType.BARIDIMOB && account.iconPath == null) Color.Transparent
                            else accentColor.copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (account.iconPath != null) {
                        coil.compose.AsyncImage(
                            model = account.iconPath,
                            contentDescription = "صورة الحساب",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else if (account.type == AccountType.BARIDIMOB) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.qdash.R.drawable.ic_baridimob),
                            contentDescription = "بريدي موب",
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        val iconVector = getIconByName(account.icon) ?: accountTypeIcon(account.type)
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Details (name, type badge)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (account.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "افتراضي",
                                tint = SavingsAmber,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = accountTypeLabel(account.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Balance & Eye Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    IconButton(
                        onClick = onToggleBalance,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "إخفاء/إظهار",
                            tint = TextGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (showBalance) FormatterUtils.formatCurrency(account.balance) else "•••• دج",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Drag handle (visually left side in RTL, so last in Row)
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "سحب للترتيب",
                    tint = TextGray.copy(alpha = 0.5f),
                    modifier = dragHandleModifier
                        .padding(horizontal = 8.dp)
                        .size(20.dp)
                )
            }

            // Expandable Transactions Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp, top = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "العمليات الأخيرة لهذا الحساب",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${filteredTransactions.size} عمليات",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Elegant Filter Row (الكل | المداخيل | المصاريف)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filters = listOf(
                            Triple("ALL", "الكل", accentColor),
                            Triple("INCOME", "المداخيل", IncomeGreen),
                            Triple("EXPENSE", "المصاريف", ExpenseRed)
                        )
                        
                        filters.forEach { (filterType, label, color) ->
                            val isSelected = selectedTxFilter == filterType
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) color.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) color else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTxFilter = filterType }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(color, CircleShape)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredTransactions.isEmpty()) {
                        Text(
                            text = when (selectedTxFilter) {
                                "INCOME" -> "لا توجد مداخيل مسجلة لهذا الحساب حالياً."
                                "EXPENSE" -> "لا توجد مصاريف مسجلة لهذا الحساب حالياً."
                                else -> "لا توجد عمليات مسجلة لهذا الحساب حالياً."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredTransactions.forEach { tx ->
                                val cat = categories.items.firstOrNull { it.id == tx.categoryId }
                                val isIncoming = tx.type == TransactionType.INCOME || (tx.type == TransactionType.TRANSFER && tx.toAccountId == account.id)
                                val amountColor = if (isIncoming) IncomeGreen else ExpenseRed
                                val amountPrefix = if (isIncoming) "+" else "-"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Small category circle
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(cat?.color ?: "#6C63FF"))
                                        } catch (e: Exception) {
                                            accentColor
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(catColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getIconByName(cat?.icon ?: "receipt_long"),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = tx.note ?: cat?.name ?: "عملية مالية",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = FormatterUtils.formatDate(tx.date),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextGray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = FormatterUtils.formatCurrency(tx.amount, amountPrefix),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = amountColor
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

@Composable
fun SwipeableAccountRow(
    account: Account,
    transactions: StableList<Transaction>,
    categories: StableList<Category>,
    showBalance: Boolean,
    onToggleBalance: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val minSwipeDistance = with(density) { 80.dp.toPx() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var hapticTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Background Actions Layer
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit action (Right in RTL - Blue)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(TransferBlue)
                    .clickable {
                        onEdit()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White)
            }

            // Delete action (Left in RTL - Red)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(ExpenseRed)
                    .clickable {
                        onDelete()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.White)
            }
        }

        // Foreground Content Layer
        val animOffset by androidx.compose.animation.core.animateFloatAsState(targetValue = offsetX)
        Box(
            modifier = Modifier
                .offset(x = with(density) { animOffset.toDp() })
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = when {
                                offsetX > minSwipeDistance * 0.5f -> minSwipeDistance
                                offsetX < -minSwipeDistance * 0.5f -> -minSwipeDistance
                                else -> 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            // Subtracting dragAmount to compensate for RTL layout coordinate inversion in Compose absolute offset.
                            offsetX = (offsetX - dragAmount).coerceIn(-minSwipeDistance, minSwipeDistance)
                            
                            val threshold = minSwipeDistance * 0.5f
                            val crossed = kotlin.math.abs(offsetX) >= threshold
                            if (crossed && !hapticTriggered) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                hapticTriggered = true
                            } else if (!crossed) {
                                hapticTriggered = false
                            }
                        }
                    )
                }
        ) {
            AccountItemCard(
                account = account,
                transactions = transactions,
                categories = categories,
                showBalance = showBalance,
                onToggleBalance = onToggleBalance,
                onCardClick = {
                    if (offsetX != 0f) {
                        offsetX = 0f
                    } else {
                        onCardClick()
                    }
                },
                dragHandleModifier = dragHandleModifier
            )
        }
    }
}

@Composable
fun DraggableAccountItem(
    account: Account,
    transactions: StableList<Transaction>,
    categories: StableList<Category>,
    showBalance: Boolean,
    onToggleBalance: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
    isDragging: Boolean,
    isSomethingDragging: Boolean,
    dragOffsetY: Float,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragScale"
    )
    val dragAlpha by animateFloatAsState(
        targetValue = if (isSomethingDragging && !isDragging) 0.65f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dragAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 15f else 1f)
    ) {
        // Drop-target placeholder shown behind the elevated dragged card
        if (isDragging) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        SwipeableAccountRow(
            account = account,
            transactions = transactions,
            categories = categories,
            showBalance = showBalance,
            onToggleBalance = onToggleBalance,
            onEdit = onEdit,
            onDelete = onDelete,
            onCardClick = onCardClick,
            dragHandleModifier = dragHandleModifier,
            modifier = Modifier
                .scale(dragScale)
                .graphicsLayer { alpha = dragAlpha }
                .offset { IntOffset(0, dragOffsetY.roundToInt()) }
        )
    }
}
