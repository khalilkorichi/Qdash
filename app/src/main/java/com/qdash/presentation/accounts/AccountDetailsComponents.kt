package com.qdash.presentation.accounts

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.Amana
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.presentation.transactions.SwipeableTransactionRow
import com.qdash.ui.designsystem.components.AppBottomSheet
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.AppEmptyState
import com.qdash.ui.designsystem.components.AppInput
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray

// ---------------------------------------------------------------------------
// Header balance card
// ---------------------------------------------------------------------------

/**
 * Displays the account's total balance, active status badge, and — when
 * amanas exist — the amana total and the resulting real balance.
 */
@Composable
internal fun AccountBalanceCard(
    balance: Double,
    isActive: Boolean,
    totalAmana: Double,
    realBalance: Double,
    isAmanaEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الرصيد الكلي",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextGray
                    )
                    Text(
                        text = FormatterUtils.formatCurrency(balance),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!isActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ExpenseRed.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "معطّل",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ExpenseRed
                        )
                    }
                }
            }

            if (isAmanaEnabled) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "الأمانات",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                        Text(
                            text = FormatterUtils.formatCurrency(totalAmana, "-"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = ExpenseRed
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "الرصيد الفعلي",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                        Text(
                            text = FormatterUtils.formatCurrency(realBalance),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = IncomeGreen
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Premium Custom Tab Selector
// ---------------------------------------------------------------------------

@Composable
internal fun AccountDetailsTabSelector(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                label = "tab_bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                label = "tab_fg"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = contentColor
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Transactions tab
// ---------------------------------------------------------------------------

@Composable
internal fun TransactionsTab(
    transactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    currentAccountId: Long?,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            AppEmptyState(
                title = "لا توجد معاملات",
                description = "لم يتم تسجيل أي معاملة لهذا الحساب بعد",
                icon = Icons.Default.ReceiptLong
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions, key = { it.id }, contentType = { "transaction" }) { tx ->
                val cat = categories.firstOrNull { it.id == tx.categoryId }
                val accName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: "غير معروف"
                val toAccName = accounts.firstOrNull { it.id == tx.toAccountId }?.name
                
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SwipeableTransactionRow(
                        transaction = tx,
                        category = cat,
                        accountName = accName,
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { onDeleteTransaction(tx) },
                        onClick = { onEditTransaction(tx) },
                        currentViewedAccountId = currentAccountId,
                        toAccountName = toAccName
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Amanas tab
// ---------------------------------------------------------------------------

@Composable
internal fun AmanasTab(
    amanas: List<Amana>,
    onDeleteAmana: (Amana) -> Unit
) {
    if (amanas.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            AppEmptyState(
                title = "لا توجد أمانات",
                description = "اضغط على زر + لإضافة مبلغ أمانة مؤمَّن في هذا الحساب",
                icon = Icons.Default.Lock
            )
        }
    } else {
        var amanaToDelete by remember { mutableStateOf<Amana?>(null) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(amanas, key = { it.id }, contentType = { "amana" }) { amana ->
                SwipeableAmanaRow(
                    amana = amana,
                    onDelete = { amanaToDelete = amana }
                )
            }
        }

        if (amanaToDelete != null) {
            AlertDialog(
                onDismissRequest = { amanaToDelete = null },
                title = { Text("حذف الأمانة") },
                text = {
                    Text(
                        "هل أنت متأكد من حذف أمانة \"${amanaToDelete!!.name}\" " +
                                "(${FormatterUtils.formatCurrency(amanaToDelete!!.amount)})؟"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteAmana(amanaToDelete!!)
                        amanaToDelete = null
                    }) {
                        Text("حذف", color = ExpenseRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { amanaToDelete = null }) { Text("إلغاء") }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableAmanaRow(
    amana: Amana,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
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
        val animOffset by animateFloatAsState(targetValue = offsetX, label = "offset_anim")
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
            AmanaListItem(
                amana = amana,
                onDelete = {
                    if (offsetX != 0f) {
                        offsetX = 0f
                    }
                }
            )
        }
    }
}

@Composable
internal fun AmanaListItem(amana: Amana, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.INTERACTIVE,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Shield / Lock Badge with Premium Gradient
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(ShapeTokens.Md)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(ExpenseRed.copy(alpha = 0.25f), ExpenseRed.copy(alpha = 0.1f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = amana.name,
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
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(ExpenseRed)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "بصاحب: ${amana.ownerName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!amana.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = amana.notes,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = TextGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Amana Amount shown inside a premium badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ExpenseRed.copy(alpha = 0.08f),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = FormatterUtils.formatCurrency(amana.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    ),
                    color = ExpenseRed,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Add Amana bottom sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddAmanaBottomSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, ownerName: String, amount: Double, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AppBottomSheet(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "إضافة أمانة",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                AppInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "اسم الأمانة",
                    placeholder = "مثال: أمانة والدي",
                    modifier = Modifier.fillMaxWidth()
                )

                AppInput(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = "اسم صاحب الأمانة",
                    placeholder = "مثال: الوالد، محمد...",
                    modifier = Modifier.fillMaxWidth()
                )

                AppInput(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "المبلغ (د.ج)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                AppInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "ملاحظات (اختياري)",
                    placeholder = "أي تفاصيل إضافية...",
                    modifier = Modifier.fillMaxWidth()
                )

                AppButton(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        onConfirm(name, ownerName, amount, notes.ifBlank { null })
                    },
                    isLoading = isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة الأمانة")
                }
            }
        }
    }
}
