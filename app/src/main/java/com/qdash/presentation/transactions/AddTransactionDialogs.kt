package com.qdash.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
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
import com.qdash.domain.model.CategoryType
import com.qdash.domain.model.TransactionType
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray

private val curatedColors = listOf(
    "#EF4444", "#F59E0B", "#10B981", "#3B82F6",
    "#6366F1", "#8B5CF6", "#EC4899", "#6B7280"
)

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    type: TransactionType,
    typeAccentColor: Color,
    isAddingSubcategory: Boolean,
    selectedCategoryId: Long?,
    onConfirm: (name: String, icon: String, color: String, parentId: Long?) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("📁") }
    var newCategoryColor by remember { mutableStateOf("#8B5CF6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isAddingSubcategory) "إنشاء فئة فرعية جديدة" else "إنشاء فئة رئيسية جديدة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(if (isAddingSubcategory) "اسم الفئة الفرعية" else "اسم الفئة الرئيسية") },
                    placeholder = { Text("مثال: تسوق، هدايا، نقل...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = typeAccentColor,
                        cursorColor = typeAccentColor
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryIcon,
                        onValueChange = { newCategoryIcon = it },
                        label = { Text("أيقونة/رمز") },
                        placeholder = { Text("📁") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(90.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = typeAccentColor,
                            cursorColor = typeAccentColor
                        )
                    )
                    Text(
                        text = "يمكنك كتابة رمز تعبيري (Emoji)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column {
                    Text(
                        text = "اختر لون الفئة:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        curatedColors.forEach { hexColor ->
                            val color = Color(android.graphics.Color.parseColor(hexColor))
                            val isSelected = newCategoryColor == hexColor
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newCategoryColor = hexColor }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newCategoryName.isNotBlank()) {
                        val parentId = if (isAddingSubcategory) selectedCategoryId else null
                        onConfirm(newCategoryName.trim(), newCategoryIcon.trim(), newCategoryColor, parentId)
                    }
                },
                enabled = newCategoryName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = typeAccentColor)
            ) {
                Text("إنشاء الفئة", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SavingsConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: (isContribution: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إيداع في حساب ادخار",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "أنت تضيف دخلاً إلى حساب ادخار. هل تريد تسجيله كمساهمة ادخار (يحافظ على الدخل الشهري دون تغيير) أم كدخل عادي لهذا الحساب (سيؤدي لزيادة الدخل الشهري)؟",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(true) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("مساهمة ادخار (موصى به)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(false) }) {
                Text("دخل عادي", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SaveTemplateDialog(
    onDismiss: () -> Unit,
    typeAccentColor: Color,
    onConfirm: (name: String, emoji: String, isPinned: Boolean) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var templateEmoji by remember { mutableStateOf("📝") }
    var templatePinned by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "حفظ كقالب معاملة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "سيتم حفظ هذه المعاملة كقالب لتتمكن من إعادة استخدامها بضغطة زر واحدة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(typeAccentColor.copy(alpha = 0.1f))
                            .clickable { showEmojiPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = templateEmoji, fontSize = 24.sp)
                    }
                    
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("اسم القالب") },
                        placeholder = { Text("مثال: قهوة، فاتورة الإنترنت…") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = typeAccentColor,
                            cursorColor = typeAccentColor
                        )
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تثبيت القالب في الرئيسية", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = templatePinned,
                        onCheckedChange = { templatePinned = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (templateName.isNotBlank()) {
                        onConfirm(templateName, templateEmoji, templatePinned)
                    }
                },
                enabled = templateName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = typeAccentColor)
            ) {
                Text("حفظ", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showEmojiPicker) {
        com.qdash.presentation.templates.components.EmojiIconPicker(
            selectedEmoji = templateEmoji,
            onEmojiSelected = {
                templateEmoji = it
                showEmojiPicker = false
            },
            onDismissRequest = { showEmojiPicker = false }
        )
    }
}

@Composable
fun AddTransactionDialogsContainer(
    showAddCategoryDialog: Boolean,
    onDismissAddCategory: () -> Unit,
    showSavingsConfirmDialog: Boolean,
    onDismissSavingsConfirm: () -> Unit,
    showSaveTemplateDialog: Boolean,
    onDismissSaveTemplate: () -> Unit,
    type: TransactionType,
    typeAccentColor: Color,
    isAddingSubcategory: Boolean,
    selectedCategoryId: Long?,
    selectedAccountId: Long?,
    toAccountId: Long?,
    subcategoryId: Long?,
    rawAmount: String,
    note: String,
    transactionDate: Long,
    isRecurring: Boolean,
    recurringPeriod: String,
    selectedTags: List<String>,
    transactionId: Long?,
    uiState: TransactionsUiState,
    viewModel: TransactionsViewModel,
    onConfirmSavingsAction: (com.qdash.domain.model.TransactionKind) -> Unit
) {
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = onDismissAddCategory,
            type = type,
            typeAccentColor = typeAccentColor,
            isAddingSubcategory = isAddingSubcategory,
            selectedCategoryId = selectedCategoryId,
            onConfirm = { name, icon, color, parentId ->
                val catType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                viewModel.addCategory(
                    name = name,
                    type = catType,
                    icon = icon,
                    color = color,
                    parentId = parentId
                )
                onDismissAddCategory()
            }
        )
    }

    if (showSavingsConfirmDialog) {
        SavingsConfirmDialog(
            onDismiss = onDismissSavingsConfirm,
            onConfirm = { isContribution ->
                val kind = if (isContribution) {
                    com.qdash.domain.model.TransactionKind.SAVINGS_CONTRIBUTION
                } else {
                    com.qdash.domain.model.TransactionKind.INCOME
                }
                onConfirmSavingsAction(kind)
                onDismissSavingsConfirm()
            }
        )
    }

    if (showSaveTemplateDialog) {
        SaveTemplateDialog(
            onDismiss = onDismissSaveTemplate,
            typeAccentColor = typeAccentColor,
            onConfirm = { name, emoji, isPinned ->
                val parsedAmountVal = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
                if (parsedAmountVal > 0) {
                    viewModel.saveAsTemplate(
                        name = name,
                        amount = parsedAmountVal,
                        type = type,
                        accountId = selectedAccountId 
                            ?: uiState.accounts.find { it.isDefault }?.id 
                            ?: uiState.accounts.firstOrNull()?.id 
                            ?: 1L,
                        targetAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                        categoryId = if (type == TransactionType.TRANSFER) null else (subcategoryId ?: selectedCategoryId),
                        subcategoryId = if (type == TransactionType.TRANSFER) null else subcategoryId,
                        notes = note.ifBlank { null },
                        iconEmoji = emoji,
                        isPinned = isPinned
                    )
                    onDismissSaveTemplate()
                }
            }
        )
    }
}
