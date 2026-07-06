package com.qdash.presentation.templates.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.CategoryChip
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import com.qdash.domain.model.TransactionType
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TransferBlue

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            TransactionType.EXPENSE to "مصروف",
            TransactionType.INCOME to "دخل",
            TransactionType.TRANSFER to "تحويل"
        ).forEach { (t, label) ->
            val isSelected = selectedType == t
            val activeColor = when (t) {
                TransactionType.EXPENSE -> ExpenseRed
                TransactionType.INCOME -> IncomeGreen
                TransactionType.TRANSFER -> TransferBlue
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) activeColor else Color.Transparent)
                    .clickable { onTypeSelected(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryPickerSection(
    categories: List<Category>,
    type: TransactionType,
    selectedCategoryId: Long?,
    subcategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    onSubcategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (type == TransactionType.TRANSFER) return

    val filteredCats = remember(categories, type) {
        categories.filter { cat ->
            cat.parentId == null &&
            when (type) {
                TransactionType.INCOME -> cat.type == CategoryType.INCOME
                else -> cat.type == CategoryType.EXPENSE
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (filteredCats.isNotEmpty()) {
            Column {
                Text("اختر الفئة", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 160.dp),
                    userScrollEnabled = false
                ) {
                    items(filteredCats) { cat ->
                        CategoryChip(
                            category = cat,
                            isSelected = selectedCategoryId == cat.id,
                            onClick = { onCategorySelected(cat.id) }
                        )
                    }
                }
            }
        }

        val subcats = remember(categories, selectedCategoryId) {
            categories.filter { it.parentId != null && it.parentId == selectedCategoryId }
        }
        if (subcats.isNotEmpty()) {
            Column {
                Text("الفئة الفرعية (اختياري)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subcats) { sub ->
                        CategoryChip(
                            category = sub,
                            isSelected = subcategoryId == sub.id,
                            onClick = { onSubcategorySelected(if (subcategoryId == sub.id) null else sub.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountPickerSection(
    accounts: List<Account>,
    type: TransactionType,
    selectedAccountId: Long?,
    toAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    onTargetAccountSelected: (Long) -> Unit,
    typeAccentColor: Color,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Source Account
        Column {
            Text(
                text = when (type) {
                    TransactionType.INCOME -> "حساب الإيداع"
                    TransactionType.EXPENSE -> "حساب الدفع"
                    TransactionType.TRANSFER -> "الحساب المرسل"
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { acc ->
                    val isSelected = selectedAccountId == acc.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) typeAccentColor.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onAccountSelected(acc.id) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = acc.name,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) typeAccentColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Target Account (Transfer only)
        if (type == TransactionType.TRANSFER) {
            Column {
                Text("الحساب المستلم", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        val isSelected = toAccountId == acc.id
                        val isDisabled = acc.id == selectedAccountId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSelected -> TransferBlue.copy(alpha = 0.18f)
                                        isDisabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable(enabled = !isDisabled) { onTargetAccountSelected(acc.id) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = acc.name,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                    isSelected -> TransferBlue
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
