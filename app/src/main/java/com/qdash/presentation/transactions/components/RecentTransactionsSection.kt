package com.qdash.presentation.transactions.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.presentation.transactions.SwipeableTransactionRow
import com.qdash.ui.designsystem.components.shimmerEffect
import com.qdash.ui.designsystem.tokens.ShapeTokens

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.recentTransactionsSection(
    isLoading: Boolean,
    recentTxs: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    selectedTransactionIds: Set<Long>,
    selectedCalendarDate: Long?,
    visibleRecentCount: Int,
    filteredTransactionsCount: Int,
    primaryColor: Color,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onRowClick: (Transaction) -> Unit,
    onRowLongClick: (Transaction) -> Unit,
    onLoadMore: () -> Unit
) {
    if (isLoading) {
        items(4, key = { "recent_skeleton_$it" }) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .height(72.dp)
                    .shimmerEffect(ShapeTokens.Md)
            )
        }
    } else {
        if (recentTxs.isEmpty()) {
            item(key = "recent_empty") {
                EmptyStateView(
                    title = if (selectedCalendarDate != null) "لا عمليات في هذا اليوم" else "لا توجد عمليات مسجلة!",
                    description = if (selectedCalendarDate != null) "لم يتم تسجيل أي معاملات في هذا التاريخ." else "اضغط على زر الإضافة لتسجيل أول عملية مالية.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            }
        } else {
            items(recentTxs, key = { "recent_${it.id}" }) { tx ->
                val cat = categories.firstOrNull { it.id == tx.categoryId }
                val accName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: "غير معروف"
                val isSelected = selectedTransactionIds.contains(tx.id)
                val isSelectionActive = selectedTransactionIds.isNotEmpty()

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .animateItemPlacement()
                ) {
                    SwipeableTransactionRow(
                        transaction = tx,
                        category = cat,
                        accountName = accName,
                        onEdit = { onEdit(tx) },
                        onDelete = { onDelete(tx) },
                        onClick = { onRowClick(tx) },
                        isSelected = isSelected,
                        isSelectionActive = isSelectionActive,
                        onLongClick = { onRowLongClick(tx) }
                    )
                }
            }

            if (filteredTransactionsCount > visibleRecentCount) {
                item(key = "load_more_button") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onLoadMore,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor.copy(alpha = 0.08f),
                                contentColor = primaryColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "عرض المزيد من العمليات (${filteredTransactionsCount - visibleRecentCount})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
