package com.example.presentation.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.presentation.ai.AiChatMessage
import com.example.presentation.ai.DraftField
import com.example.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDraftCard(
    draft: Transaction,
    message: AiChatMessage,
    accounts: List<Account>,
    categories: List<Category>,
    onUpdateDraftField: ((DraftField, Any) -> Unit)?,
    onConfirmDraft: () -> Unit,
    onCancelDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAmount = message.editedAmount ?: draft.amount
    val currentType = message.editedType ?: draft.type
    val currentNote = message.editedNote ?: (draft.note ?: "")
    val currentCategoryId = message.editedCategoryId ?: draft.categoryId
    val currentAccountId = message.editedAccountId ?: draft.accountId

    var amountText by remember(message.id, currentAmount) {
        mutableStateOf(if (currentAmount == currentAmount.toLong().toDouble()) currentAmount.toLong().toString() else currentAmount.toString())
    }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }

    val isInteractable = !message.isConfirmed && !message.isCancelled

    Card(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFE9E9E6)), shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📝 مسودة معاملة مقترحة",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "المبلغ",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (isInteractable) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        amountText = raw.filter { it.isDigit() || it == '.' }
                        val parsed = amountText.toDoubleOrNull()
                        if (parsed != null) onUpdateDraftField?.invoke(DraftField.AMOUNT, parsed)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    suffix = { Text("دج", fontSize = 12.sp, color = TextGray) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            } else {
                Text(
                    text = "$currentAmount دج",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "النوع",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isInteractable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple(TransactionType.EXPENSE, "مصاريف", Color(0xFFEF4444)),
                        Triple(TransactionType.INCOME, "دخل", Color(0xFF22C55E)),
                        Triple(TransactionType.TRANSFER, "تحويل", Color(0xFF3B82F6))
                    ).forEach { (type, label, color) ->
                        val isSelected = currentType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateDraftField?.invoke(DraftField.TYPE, type) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) color else TextGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
            } else {
                val typeStr = when (currentType) {
                    TransactionType.EXPENSE -> "مصاريف 🔴"
                    TransactionType.INCOME -> "دخل 🟢"
                    TransactionType.TRANSFER -> "تحويل 🔵"
                }
                Text(text = typeStr, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "الفئة",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (isInteractable && categories.isNotEmpty()) {
                val selectedCatName = categories.find { it.id == currentCategoryId }?.name
                    ?: message.categoryName ?: "غير محدد"
                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryDropdown = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCatName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, fontSize = 13.sp) },
                                onClick = {
                                    onUpdateDraftField?.invoke(DraftField.CATEGORY_ID, cat.id)
                                    showCategoryDropdown = false
                                },
                                leadingIcon = if (cat.id == currentCategoryId) ({
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }) else null
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = categories.find { it.id == currentCategoryId }?.name
                        ?: message.categoryName ?: "غير محدد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "الحساب",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (isInteractable && accounts.isNotEmpty()) {
                val selectedAccName = accounts.find { it.id == currentAccountId }?.name
                    ?: message.accountName ?: "غير محدد"
                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountDropdown = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedAccName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showAccountDropdown,
                        onDismissRequest = { showAccountDropdown = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name, fontSize = 13.sp) },
                                onClick = {
                                    onUpdateDraftField?.invoke(DraftField.ACCOUNT_ID, acc.id)
                                    showAccountDropdown = false
                                },
                                leadingIcon = if (acc.id == currentAccountId) ({
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }) else null
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = accounts.find { it.id == currentAccountId }?.name
                        ?: message.accountName ?: "غير محدد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            if (isInteractable) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ملاحظة",
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                var noteText by remember(message.id, currentNote) { mutableStateOf(currentNote) }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { raw ->
                        noteText = raw
                        onUpdateDraftField?.invoke(DraftField.NOTE, raw)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    placeholder = { Text("أضف ملاحظة...", fontSize = 12.sp, color = TextGray) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                message.isConfirmed -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF22C55E).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "تمت الإضافة بنجاح ✅",
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                message.isCancelled -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFEF4444).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "تم إلغاء المسودة ❌",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelDraft,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("إلغاء", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onConfirmDraft,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("تأكيد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
