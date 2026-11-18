package com.qdash.presentation.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import com.qdash.presentation.components.UnifiedCategoryIcon
import com.qdash.presentation.components.safeParseColor
import com.qdash.ui.designsystem.components.AppBottomSheet
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkEditBottomSheet(
    selectedCount: Int,
    categories: List<Category>,
    accounts: List<Account>,
    onConfirm: (newCategoryId: Long?, newAccountId: Long?) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
    val borderColor = MaterialTheme.colorScheme.outline
    val triggerBgColor = if (isDark) ColorTokens.CardDark else ColorTokens.SurfaceLight

    val expenseCategories = remember(categories) {
        categories.filter { it.type == CategoryType.EXPENSE }
    }
    val incomeCategories = remember(categories) {
        categories.filter { it.type == CategoryType.INCOME }
    }
    val selectableAccounts = remember(accounts) {
        accounts.filter { !it.isArchived }
    }

    AppBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "تعديل العمليات المحددة",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 1. Change Category Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "تغيير الفئة",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(triggerBgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { categoryDropdownExpanded = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedCategory?.let { cat ->
                                val catColor = safeParseColor(cat.color, MaterialTheme.colorScheme.primary)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(catColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    UnifiedCategoryIcon(
                                        iconKey = cat.icon,
                                        tint = catColor,
                                        modifier = Modifier.size(14.dp),
                                        emojiFontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = selectedCategory?.name ?: "بدون تغيير",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (selectedCategory == null) ColorTokens.TextGray else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = ColorTokens.TextGray
                        )
                    }

                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .heightIn(max = 320.dp)
                            .background(if (isDark) ColorTokens.ElevatedSurfaceDark else MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون تغيير", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) },
                            onClick = {
                                selectedCategory = null
                                categoryDropdownExpanded = false
                            }
                        )
                        if (expenseCategories.isNotEmpty()) {
                            HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                            Text(
                                text = "فئات المصروفات",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = ExpenseRed,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            expenseCategories.forEach { category ->
                                val catColor = safeParseColor(category.color, ExpenseRed)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                UnifiedCategoryIcon(
                                                    iconKey = category.icon,
                                                    tint = catColor,
                                                    modifier = Modifier.size(14.dp),
                                                    emojiFontSize = 12.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = category.name,
                                                textAlign = TextAlign.Start,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCategory = category
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                        if (incomeCategories.isNotEmpty()) {
                            HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                            Text(
                                text = "فئات المداخيل",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = IncomeGreen,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            incomeCategories.forEach { category ->
                                val catColor = safeParseColor(category.color, IncomeGreen)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                UnifiedCategoryIcon(
                                                    iconKey = category.icon,
                                                    tint = catColor,
                                                    modifier = Modifier.size(14.dp),
                                                    emojiFontSize = 12.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = category.name,
                                                textAlign = TextAlign.Start,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCategory = category
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Change Account Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "تغيير الحساب",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(triggerBgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { accountDropdownExpanded = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedAccount?.let { acc ->
                                val accColor = safeParseColor(acc.color, MaterialTheme.colorScheme.primary)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(accColor)
                                )
                            }
                            Text(
                                text = selectedAccount?.name ?: "بدون تغيير",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (selectedAccount == null) ColorTokens.TextGray else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = ColorTokens.TextGray
                        )
                    }

                    DropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(if (isDark) ColorTokens.ElevatedSurfaceDark else MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون تغيير", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) },
                            onClick = {
                                selectedAccount = null
                                accountDropdownExpanded = false
                            }
                        )
                        selectableAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val accColor = safeParseColor(account.color, MaterialTheme.colorScheme.primary)
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(accColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = account.name,
                                            textAlign = TextAlign.Start,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                },
                                onClick = {
                                    selectedAccount = account
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Informational Note if Account is Selected
            if (selectedAccount != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ColorTokens.Info.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.Info.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ColorTokens.Info,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "سيتم تصحيح أرصدة الحسابات تلقائيًا",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ColorTokens.Info,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                AppButton(
                    onClick = onDismissRequest,
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.PRIMARY,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "إلغاء",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Apply Button
                val hasChanges = selectedCategory != null || selectedAccount != null
                AppButton(
                    onClick = {
                        onConfirm(selectedCategory?.id, selectedAccount?.id)
                    },
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY,
                    enabled = hasChanges,
                    modifier = Modifier.weight(2f)
                ) {
                    Text(
                        text = "تطبيق على $selectedCount عمليات",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
