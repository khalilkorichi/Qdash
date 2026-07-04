package com.qdash.presentation.transactions.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TransferBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionFloatingBar(
    selectedTransactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    selectedTotal: Double,
    onEditClick: () -> Unit,
    onCloseClick: () -> Unit,
    onRemoveTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Auto-collapse if selection becomes empty
    val selectedCount = selectedTransactions.size
    LaunchedEffect(selectedCount) {
        if (selectedCount == 0) {
            isExpanded = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AnimatedVisibility(
            visible = selectedCount > 0,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = MotionTokens.springFluid()
            ) + fadeIn(animationSpec = MotionTokens.tweenMedium()),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = MotionTokens.springFluid()
            ) + fadeOut(animationSpec = MotionTokens.tweenMedium()),
            modifier = modifier
        ) {
            val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
            val barBgColor = if (isDark) ColorTokens.SurfaceDark else ColorTokens.SurfaceLight
            val borderColor = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight
            val itemBgColor = if (isDark) ColorTokens.ElevatedSurfaceDark else ColorTokens.BackgroundLight
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = MotionTokens.springFluid())
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = barBgColor,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // —— Expanded Content: Selected Transactions List ————————
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(animationSpec = MotionTokens.springFluid()) + fadeIn(animationSpec = MotionTokens.tweenMedium()),
                        exit = shrinkVertically(animationSpec = MotionTokens.springFluid()) + fadeOut(animationSpec = MotionTokens.tweenMedium())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "العمليات المحددة (${selectedCount})",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                IconButton(
                                    onClick = { isExpanded = false },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "تصغير",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(selectedTransactions, key = { it.id }) { tx ->
                                    val cat = categories.find { it.id == tx.categoryId }
                                    val acc = accounts.find { it.id == tx.accountId }
                                    val catColor = try {
                                        Color(android.graphics.Color.parseColor(cat?.color ?: "#6C63FF"))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(itemBgColor)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Details & Category Icon (on the right in RTL)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Category bubble icon (rightmost inside Details)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = cat?.icon ?: "📁",
                                                    fontSize = 16.sp
                                                )
                                            }

                                            // Text Details (Note, Category)
                                            Column(
                                                horizontalAlignment = Alignment.Start,
                                                modifier = Modifier.weight(1.5f)
                                            ) {
                                                Text(
                                                    text = tx.note ?: cat?.name ?: "عملية مالية",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = cat?.name ?: "بدون فئة",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ColorTokens.TextGray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Amount & Account
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = FormatterUtils.formatCurrency(tx.amount),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = when (tx.type) {
                                                        TransactionType.EXPENSE -> ExpenseRed
                                                        TransactionType.INCOME -> IncomeGreen
                                                        TransactionType.TRANSFER -> TransferBlue
                                                    }
                                                )
                                                Text(
                                                    text = acc?.name ?: "حساب غير معروف",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ColorTokens.TextGray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // 2. Remove button (on the left in RTL)
                                        IconButton(
                                            onClick = { onRemoveTransaction(tx) },
                                            modifier = Modifier.size(32.dp),
                                            colors = IconButtonDefaults.iconButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RemoveCircleOutline,
                                                contentDescription = "إزالة من التحديد",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            HorizontalDivider(
                                color = borderColor,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }

                    // —— Compact Bar Content (Always visible) ————————
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right side (RTL): Amount, count & expand toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Expand/collapse arrow icon button
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = if (isExpanded) "تصغير" else "توسيع",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Text(
                                text = "مجموع: ${FormatterUtils.formatCurrency(selectedTotal)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = "$selectedCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }

                        // Left side (RTL): Action Buttons (Edit, Close)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Edit (Pencil) button
                            IconButton(
                                onClick = onEditClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تعديل جماعي",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Close (✕) button
                            IconButton(
                                onClick = onCloseClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إلغاء التحديد",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
