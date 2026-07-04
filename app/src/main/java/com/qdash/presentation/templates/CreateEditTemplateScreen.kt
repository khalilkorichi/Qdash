package com.qdash.presentation.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.CategoryChip
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.domain.model.*
import com.qdash.presentation.templates.components.EmojiIconPicker
import com.qdash.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTemplateScreen(
    viewModel: TemplatesViewModel,
    templateId: Long? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()

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

    // Load template details if in edit mode
    LaunchedEffect(uiState.templates, templateId) {
        if (templateId != null && uiState.templates.isNotEmpty()) {
            val template = uiState.templates.find { it.id == templateId }
            if (template != null) {
                name = template.name
                amountText = template.amount.toString().replace(".0", "")
                note = template.notes ?: ""
                iconEmoji = template.iconEmoji ?: "☕"
                isPinned = template.isPinned
                type = template.transactionType
                
                selectedCategoryId = template.categoryId
                subcategoryId = template.subcategoryId
                selectedAccountId = template.accountId
                toAccountId = template.targetAccountId
            }
        }
    }

    // Auto-select defaults
    LaunchedEffect(uiState.categories, uiState.accounts, type) {
        if (templateId != null) return@LaunchedEffect
        if (selectedCategoryId == null && uiState.categories.isNotEmpty()) {
            selectedCategoryId = uiState.categories.firstOrNull {
                when (type) {
                    TransactionType.INCOME -> it.type == CategoryType.INCOME
                    else -> it.type == CategoryType.EXPENSE
                }
            }?.id
        }
        if (selectedAccountId == null && uiState.accounts.isNotEmpty()) {
            selectedAccountId = uiState.accounts.find { it.isDefault }?.id ?: uiState.accounts.first().id
        }
        if (toAccountId == null && uiState.accounts.size > 1) {
            toAccountId = uiState.accounts.lastOrNull { it.id != selectedAccountId }?.id ?: uiState.accounts.first().id
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
                    // Emoji Picker Button
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

                    // Template Name Input
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
            Row(
                modifier = Modifier
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
                    val isSelected = type == t
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
                            .clickable {
                                type = t
                                selectedCategoryId = uiState.categories.firstOrNull {
                                    when (t) {
                                        TransactionType.INCOME -> it.type == CategoryType.INCOME
                                        else -> it.type == CategoryType.EXPENSE
                                    }
                                }?.id
                                subcategoryId = null
                            }
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

            // Category Picker
            if (type != TransactionType.TRANSFER) {
                val filteredCats = remember(uiState.categories, type) {
                    uiState.categories.filter { cat ->
                        cat.parentId == null &&
                        when (type) {
                            TransactionType.INCOME -> cat.type == CategoryType.INCOME
                            else -> cat.type == CategoryType.EXPENSE
                        }
                    }
                }
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
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        subcategoryId = null
                                    }
                                )
                            }
                        }
                    }
                }

                // Subcategories
                val subcats = remember(uiState.categories, selectedCategoryId) {
                    uiState.categories.filter { it.parentId != null && it.parentId == selectedCategoryId }
                }
                if (subcats.isNotEmpty()) {
                    Column {
                        Text("الفئة الفرعية (اختياري)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(subcats) { sub ->
                                CategoryChip(
                                    category = sub,
                                    isSelected = subcategoryId == sub.id,
                                    onClick = { subcategoryId = if (subcategoryId == sub.id) null else sub.id }
                                )
                            }
                        }
                    }
                }
            }

            // Account selection
            if (uiState.accounts.isNotEmpty()) {
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
                        uiState.accounts.forEach { acc ->
                            val isSelected = selectedAccountId == acc.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) typeAccentColor.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedAccountId = acc.id }
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
            }

            // Target Account selection (Transfer only)
            if (type == TransactionType.TRANSFER && uiState.accounts.isNotEmpty()) {
                Column {
                    Text("الحساب المستلم", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.accounts.forEach { acc ->
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
                                    .clickable(enabled = !isDisabled) { toAccountId = acc.id }
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
                            Text(text = "${amountText.ifBlank { "0" }} دج", fontSize = 11.sp, color = typeAccentColor, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save Action Button
            Button(
                onClick = {
                    val parsedAmt = amountText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && parsedAmt > 0) {
                        val template = TransactionTemplate(
                            id = templateId ?: 0L,
                            name = name,
                            amount = parsedAmt,
                            transactionType = type,
                            accountId = selectedAccountId ?: 1L,
                            targetAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                            categoryId = if (type == TransactionType.TRANSFER) null else (subcategoryId ?: selectedCategoryId),
                            notes = note.ifBlank { null },
                            iconEmoji = iconEmoji,
                            colorHex = String.format("#%06X", (0xFFFFFF and typeAccentColor.value.toInt())),
                            isPinned = isPinned,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        if (templateId == null) {
                            viewModel.onCreateTemplate(template)
                        } else {
                            viewModel.onUpdateTemplate(template)
                        }
                        onBack()
                    }
                },
                enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("حفظ القالب", fontWeight = FontWeight.Bold, color = Color.White)
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
                        viewModel.forceCreateTemplate(dupWarning)
                        onBack()
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
