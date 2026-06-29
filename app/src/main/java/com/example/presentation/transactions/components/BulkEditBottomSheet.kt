package com.example.presentation.transactions.components

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
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.ui.designsystem.components.AppBottomSheet
import com.example.ui.designsystem.components.AppButton
import com.example.ui.designsystem.components.ButtonIntent
import com.example.ui.designsystem.components.ButtonVariant
import com.example.ui.designsystem.tokens.ColorTokens

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
    val borderColor = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight
    val triggerBgColor = if (isDark) ColorTokens.CardDark else ColorTokens.SurfaceLight

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
                horizontalAlignment = Alignment.End
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
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = ColorTokens.TextGray
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = selectedCategory?.name ?: "بدون تغيير",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (selectedCategory == null) ColorTokens.TextGray else MaterialTheme.colorScheme.onSurface
                            )
                            selectedCategory?.let { cat ->
                                val catColor = try {
                                    Color(android.graphics.Color.parseColor(cat.color))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(if (isDark) ColorTokens.ElevatedSurfaceDark else MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون تغيير", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                            onClick = {
                                selectedCategory = null
                                categoryDropdownExpanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category.name,
                                            textAlign = TextAlign.Right,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(category.color))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
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

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Change Account Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
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
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = ColorTokens.TextGray
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = selectedAccount?.name ?: "بدون تغيير",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (selectedAccount == null) ColorTokens.TextGray else MaterialTheme.colorScheme.onSurface
                            )
                            selectedAccount?.let { acc ->
                                val accColor = try {
                                    Color(android.graphics.Color.parseColor(acc.color))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(accColor)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(if (isDark) ColorTokens.ElevatedSurfaceDark else MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون تغيير", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                            onClick = {
                                selectedAccount = null
                                accountDropdownExpanded = false
                            }
                        )
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = account.name,
                                            textAlign = TextAlign.Right,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val accColor = try {
                                            Color(android.graphics.Color.parseColor(account.color))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(accColor)
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
                            textAlign = TextAlign.Right,
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
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
