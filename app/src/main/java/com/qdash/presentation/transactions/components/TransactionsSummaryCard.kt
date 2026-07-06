package com.qdash.presentation.transactions.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.core.ui.components.CategoryChip
import com.qdash.ui.designsystem.components.shimmerEffect
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TextGray
import com.qdash.ui.theme.TransferBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsSummaryCard(
    isLoading: Boolean,
    totalExpenses: Double,
    totalIncome: Double,
    netBalance: Double,
    netColor: Color,
    selectedType: TransactionType?,
    onTypeSelected: (TransactionType?) -> Unit,
    categories: List<Category>,
    transactions: List<Transaction>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(60.dp)
                .shimmerEffect(ShapeTokens.Md)
        )
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Summary Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Section 1: Expenses
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).background(ExpenseRed, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المصاريف", fontSize = 11.sp, color = TextGray)
                        }
                        Text(FormatterUtils.formatCurrency(totalExpenses), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))

                    // Section 2: Income
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).background(IncomeGreen, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المداخيل", fontSize = 11.sp, color = TextGray)
                        }
                        Text(FormatterUtils.formatCurrency(totalIncome), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))

                    // Section 3: Net
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).background(netColor, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الصافي", fontSize = 11.sp, color = TextGray)
                        }
                        Text(FormatterUtils.formatCurrency(netBalance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = netColor)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.height(10.dp))

                // Centered & Compact Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chipHeight = 32.dp
                    val textStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)

                    FilterChip(
                        selected = selectedType == null,
                        onClick = { onTypeSelected(null) },
                        label = { Text("الكل", style = textStyle) },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        shape = RoundedCornerShape(50.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor, selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(chipHeight).padding(horizontal = 2.dp)
                    )

                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { onTypeSelected(TransactionType.EXPENSE) },
                        label = { Text("مصاريف", style = textStyle) },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        shape = RoundedCornerShape(50.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed, selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(chipHeight).padding(horizontal = 2.dp)
                    )

                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { onTypeSelected(TransactionType.INCOME) },
                        label = { Text("مداخيل", style = textStyle) },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        shape = RoundedCornerShape(50.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen, selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(chipHeight).padding(horizontal = 2.dp)
                    )

                    FilterChip(
                        selected = selectedType == TransactionType.TRANSFER,
                        onClick = { onTypeSelected(TransactionType.TRANSFER) },
                        label = { Text("تحويلات", style = textStyle) },
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        shape = RoundedCornerShape(50.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TransferBlue, selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(chipHeight).padding(horizontal = 2.dp)
                    )
                }

                val activeCategories = remember(categories, transactions, selectedType) {
                    val activeIds = transactions
                        .filter { tx -> selectedType == null || tx.type == selectedType }
                        .map { it.categoryId }
                        .toSet()
                    categories.filter { it.id in activeIds }
                }

                if (activeCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "تصفية حسب الفئة",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextGray,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        item {
                            val isAllSelected = selectedCategoryId == null
                            val containerColor = if (isAllSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            val contentColor = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(containerColor)
                                    .clickable { onCategorySelected(null) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .testTag("category_chip_all"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "كل الفئات",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                        }

                        items(activeCategories, key = { it.id }) { category ->
                            CategoryChip(
                                category = category,
                                isSelected = selectedCategoryId == category.id,
                                onClick = {
                                    if (selectedCategoryId == category.id) {
                                        onCategorySelected(null)
                                    } else {
                                        onCategorySelected(category.id)
                                    }
                                },
                                modifier = Modifier.height(38.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
