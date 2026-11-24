package com.qdash.presentation.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.domain.model.*
import com.qdash.presentation.templates.components.*
import com.qdash.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTemplateScreen(
    viewModel: TemplatesViewModel,
    templateId: Long? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var iconEmoji by remember { mutableStateOf("☕") }
    var isPinned by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }

    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var subcategoryId       by remember { mutableStateOf<Long?>(null) }
    var selectedAccountId   by remember { mutableStateOf<Long?>(null) }
    var toAccountId         by remember { mutableStateOf<Long?>(null) }

    var showEmojiPicker by remember { mutableStateOf(false) }
    var hasLoadedTemplate by remember(templateId) { mutableStateOf(false) }

    val nonArchivedAccounts = remember(uiState.accounts) {
        uiState.accounts.filter { !it.isArchived }
    }

    // Load template details if in edit mode
    LaunchedEffect(uiState.templates, uiState.categories, templateId) {
        if (!hasLoadedTemplate && templateId != null && uiState.templates.isNotEmpty()) {
            val template = uiState.templates.find { it.id == templateId }
            if (template != null) {
                name = template.name
                amountText = template.amount.toString().replace(".0", "")
                note = template.notes ?: ""
                iconEmoji = template.iconEmoji ?: "☕"
                isPinned = template.isPinned
                type = template.transactionType

                val matchedCat = template.categoryId?.let { id -> uiState.categories.find { it.id == id } }
                if (template.subcategoryId != null) {
                    selectedCategoryId = if (matchedCat?.parentId != null) matchedCat.parentId else template.categoryId
                    subcategoryId = template.subcategoryId
                } else if (matchedCat?.parentId != null) {
                    selectedCategoryId = matchedCat.parentId
                    subcategoryId = matchedCat.id
                } else {
                    selectedCategoryId = template.categoryId
                    subcategoryId = null
                }
                selectedAccountId = template.accountId
                toAccountId = template.targetAccountId
                hasLoadedTemplate = true
            }
        }
    }

    // Auto-select defaults
    LaunchedEffect(uiState.categories, nonArchivedAccounts, type) {
        if (templateId != null) return@LaunchedEffect
        if (selectedCategoryId == null && uiState.categories.isNotEmpty()) {
            selectedCategoryId = uiState.categories.firstOrNull {
                it.parentId == null && when (type) {
                    TransactionType.INCOME -> it.type == CategoryType.INCOME
                    else -> it.type == CategoryType.EXPENSE
                }
            }?.id
        }
        if (selectedAccountId == null && nonArchivedAccounts.isNotEmpty()) {
            selectedAccountId = nonArchivedAccounts.find { it.isDefault }?.id ?: nonArchivedAccounts.first().id
        }
        if (toAccountId == null && nonArchivedAccounts.size > 1) {
            toAccountId = nonArchivedAccounts.lastOrNull { it.id != selectedAccountId }?.id ?: nonArchivedAccounts.first().id
        }
    }

    val typeAccentColor = when (type) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            FinTrackTopBar(
                title = if (templateId != null) "تعديل القالب" else "إنشاء قالب جديد",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form Name and Emoji picker
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.08f))
                            .clickable { showEmojiPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = iconEmoji, fontSize = 26.sp)
                            Text(text = "تعديل", fontSize = 9.sp, color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم القالب (مثال: قهوة الصباح)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // Transaction Type Selector
            TransactionTypeSelector(
                selectedType = type,
                onTypeSelected = { selected ->
                    type = selected
                    selectedCategoryId = uiState.categories.firstOrNull {
                        when (selected) {
                            TransactionType.INCOME -> it.type == CategoryType.INCOME
                            else -> it.type == CategoryType.EXPENSE
                        }
                    }?.id
                    subcategoryId = null
                }
            )

            // Amount TextField
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("المبلغ (دج)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeAccentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                leadingIcon = {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = typeAccentColor)
                }
            )

            // Category Picker Section
            CategoryPickerSection(
                categories = uiState.categories,
                type = type,
                selectedCategoryId = selectedCategoryId,
                subcategoryId = subcategoryId,
                onCategorySelected = {
                    selectedCategoryId = it
                    subcategoryId = null
                },
                onSubcategorySelected = {
                    subcategoryId = it
                }
            )

            // Account Picker Section
            AccountPickerSection(
                accounts = uiState.accounts,
                type = type,
                selectedAccountId = selectedAccountId,
                toAccountId = toAccountId,
                onAccountSelected = { selectedAccountId = it },
                onTargetAccountSelected = { toAccountId = it },
                typeAccentColor = typeAccentColor
            )

            // Notes input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("الملاحظات (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeAccentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Pinned to Home selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PushPin, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تثبيت القالب في الواجهة الرئيسية", fontWeight = FontWeight.SemiBold)
                }
                Switch(
                    checked = isPinned,
                    onCheckedChange = { isPinned = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }

            // Home preview chip
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("معاينة شكل الاختصار:", style = MaterialTheme.typography.labelSmall, color = TextGray)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(BorderStroke(1.dp, typeAccentColor.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = iconEmoji, fontSize = 20.sp)
                        Column {
                            Text(text = name.ifBlank { "اسم القالب" }, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = com.qdash.core.utils.FormatterUtils.formatCurrency(amountText.toDoubleOrNull() ?: 0.0), fontSize = 11.sp, color = typeAccentColor, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save Action Button
            val parsedFormAmount = AmountConversionEngine.parseFlexibleAmount(amountText) ?: 0.0
            val isTransferValid = type != TransactionType.TRANSFER || (toAccountId != null && toAccountId != selectedAccountId)
            Button(
                onClick = {
                    val parsedAmt = AmountConversionEngine.parseFlexibleAmount(amountText) ?: 0.0
                    if (name.isNotBlank() && parsedAmt > 0 && isTransferValid) {
                        val existingTemplate = templateId?.let { id -> uiState.templates.find { it.id == id } }
                        val now = System.currentTimeMillis()
                        val template = TransactionTemplate(
                            id = templateId ?: 0L,
                            name = name.trim(),
                            amount = parsedAmt,
                            transactionType = type,
                            accountId = selectedAccountId ?: nonArchivedAccounts.firstOrNull()?.id ?: 1L,
                            targetAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                            categoryId = if (type == TransactionType.TRANSFER) null else selectedCategoryId,
                            subcategoryId = if (type == TransactionType.TRANSFER) null else subcategoryId,
                            notes = note.ifBlank { null },
                            iconEmoji = iconEmoji,
                            colorHex = String.format("#%06X", 0xFFFFFF and typeAccentColor.toArgb()),
                            isPinned = isPinned,
                            usageCount = existingTemplate?.usageCount ?: 0,
                            lastUsedAt = existingTemplate?.lastUsedAt,
                            createdAt = existingTemplate?.createdAt ?: now,
                            updatedAt = now
                        )
                        if (templateId == null) {
                            viewModel.onCreateTemplate(template, onSaved = onBack)
                        } else {
                            viewModel.onUpdateTemplate(template, onSaved = onBack)
                        }
                    }
                },
                enabled = name.isNotBlank() && parsedFormAmount > 0.0 && isTransferValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("حفظ القالب", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    // Duplicate Detection warning Dialog
    val dupWarning = uiState.duplicateWarningTemplate
    if (dupWarning != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearDuplicateWarning() },
            title = { Text("تنبيه بتكرار القالب", fontWeight = FontWeight.Bold) },
            text = { Text("يوجد قالب آخر يحمل نفس الاسم أو نفس القيم المالية بالكامل. هل تريد حفظ هذا القالب على أي حال؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.forceCreateTemplate(dupWarning, onSaved = onBack)
                    }
                ) {
                    Text("نعم، حفظ", color = Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearDuplicateWarning() }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showEmojiPicker) {
        EmojiIconPicker(
            selectedEmoji = iconEmoji,
            onEmojiSelected = { iconEmoji = it },
            onDismissRequest = { showEmojiPicker = false }
        )
    }
}
