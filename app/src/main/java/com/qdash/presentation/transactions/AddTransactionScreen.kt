package com.qdash.presentation.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.CategoryChip
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.domain.model.CategoryType
import com.qdash.domain.model.TransactionType
import com.qdash.domain.model.TransactionTemplate
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.core.utils.FormatterUtils
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  AddTransactionScreen
// ————————————————————————————————————————————————————————————————————————————————

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionsViewModel,
    initialType: String = "EXPENSE",
    transactionId: Long? = null,
    initialDate: Long? = null,
    draftJson: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val showAmountWords = uiState.isAmountWordsEnabled

    // ── Local state ──────────────────────────────────────────────────────────────────
    var rawAmountValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue("0", selection = TextRange(1)))
    }
    val rawAmount = rawAmountValue.text
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var isAddingSubcategory by rememberSaveable { mutableStateOf(false) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }
    var newCategoryIcon by rememberSaveable { mutableStateOf("📁") }
    var newCategoryColor by rememberSaveable { mutableStateOf("#8B5CF6") }

    val curatedColors = listOf(
        "#8B5CF6", // Violet
        "#EF4444", // Rose
        "#F59E0B", // Amber
        "#10B981", // Emerald
        "#0EA5E9", // Sky
        "#EC4899", // Pink
        "#6366F1", // Indigo
        "#64748B"  // Slate
    )

    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var subcategoryId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Auto-select newly created category or subcategory
    var previousCategories by remember { mutableStateOf<List<com.qdash.domain.model.Category>>(emptyList()) }
    LaunchedEffect(uiState.categories) {
        if (previousCategories.isNotEmpty() && uiState.categories.size > previousCategories.size) {
            val newlyAdded = uiState.categories.filter { newCat ->
                previousCategories.none { oldCat -> oldCat.id == newCat.id }
            }
            newlyAdded.firstOrNull()?.let { newCat ->
                if (newCat.parentId != null) {
                    selectedCategoryId = newCat.parentId
                    subcategoryId = newCat.id
                } else {
                    selectedCategoryId = newCat.id
                    subcategoryId = null
                }
            }
        }
        previousCategories = uiState.categories
    }
    var note by rememberSaveable { mutableStateOf("") }
    var isRecurring by rememberSaveable { mutableStateOf(false) }
    var recurringPeriod by rememberSaveable { mutableStateOf("MONTHLY") }
    var selectedTags by rememberSaveable(saver = Saver<MutableState<List<String>>, String>(
        save = { it.value.joinToString(",") },
        restore = { mutableStateOf(it.split(",").filter { tag -> tag.isNotBlank() }) }
    )) { mutableStateOf<List<String>>(emptyList()) }
    var isKeypadExpanded by rememberSaveable { mutableStateOf(true) }
    var transactionDate by rememberSaveable { mutableStateOf(initialDate ?: System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    var type by rememberSaveable(saver = Saver<MutableState<TransactionType>, String>(
        save = { it.value.name },
        restore = { mutableStateOf(TransactionType.valueOf(it)) }
    )) {
        mutableStateOf(
            when (initialType) {
                "INCOME"   -> TransactionType.INCOME
                "TRANSFER" -> TransactionType.TRANSFER
                else       -> TransactionType.EXPENSE
            }
        )
    }

    val typeIntent = when (type) {
        TransactionType.INCOME -> ButtonIntent.SUCCESS
        TransactionType.EXPENSE -> ButtonIntent.DANGER
        TransactionType.TRANSFER -> ButtonIntent.INFO
    }

    var selectedAccountId   by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId         by rememberSaveable { mutableStateOf<Long?>(null) }

    var isSalaryAutomation by rememberSaveable { mutableStateOf(false) }
    var showSavingsConfirmDialog by remember { mutableStateOf(false) }

    var hasLoadedInitialData by rememberSaveable { mutableStateOf(false) }
    var hasLoadedDraft by rememberSaveable { mutableStateOf(false) }

    val parsedAmount = remember(rawAmount) {
        com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
    }

    val expectedBalances = remember(uiState.accounts, selectedAccountId, toAccountId, type, parsedAmount) {
        uiState.accounts.associate { acc ->
            val expected = when (type) {
                TransactionType.EXPENSE -> {
                    if (acc.id == selectedAccountId) acc.balance - parsedAmount else acc.balance
                }
                TransactionType.INCOME -> {
                    if (acc.id == selectedAccountId) acc.balance + parsedAmount else acc.balance
                }
                TransactionType.TRANSFER -> {
                    when (acc.id) {
                        selectedAccountId -> acc.balance - parsedAmount
                        toAccountId -> acc.balance + parsedAmount
                        else -> acc.balance
                    }
                }
            }
            acc.id to expected
        }
    }

    val activeBudget = remember(selectedCategoryId, uiState.budgetGoals, type) {
        if (type == TransactionType.EXPENSE) {
            uiState.budgetGoals.find { it.linkedCategoryId == selectedCategoryId }
        } else {
            null
        }
    }

    val effectiveCategoryId = subcategoryId ?: selectedCategoryId
    val effectiveAccountId = selectedAccountId
        ?: uiState.accounts.find { it.isDefault }?.id
        ?: uiState.accounts.firstOrNull()?.id
    val canSaveTransaction = parsedAmount > 0.0 &&
        (type == TransactionType.TRANSFER || effectiveCategoryId != null) &&
        effectiveAccountId != null &&
        (type != TransactionType.TRANSFER || (toAccountId != null && effectiveAccountId != toAccountId)) &&
        !uiState.isSaving

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.consumeSaveCompleted()
            onBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    // Reactively load transaction details if in edit mode
    LaunchedEffect(uiState.transactions, transactionId) {
        if (!hasLoadedInitialData && transactionId != null && uiState.transactions.isNotEmpty()) {
            val transaction = uiState.transactions.find { it.id == transactionId }
            if (transaction != null) {
                val amtStr = transaction.amount.toString().replace(".0", "")
                rawAmountValue = TextFieldValue(amtStr, selection = TextRange(amtStr.length))
                note = transaction.note ?: ""
                isRecurring = transaction.isRecurring
                recurringPeriod = transaction.recurringPeriod ?: "MONTHLY"
                selectedTags = transaction.tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                type = transaction.type
                
                val cat = uiState.categories.find { it.id == transaction.categoryId }
                if (cat != null) {
                    if (cat.parentId != null) {
                        selectedCategoryId = cat.parentId
                        subcategoryId = cat.id
                    } else {
                        selectedCategoryId = cat.id
                        subcategoryId = null
                    }
                } else {
                    selectedCategoryId = transaction.categoryId
                    subcategoryId = null
                }
                
                selectedAccountId = transaction.accountId
                toAccountId = transaction.toAccountId
                transactionDate = transaction.date
                isKeypadExpanded = false
                hasLoadedInitialData = true
            }
        }
    }

    // Reactively load pre-fill details from draftJson
    LaunchedEffect(draftJson) {
        if (!hasLoadedDraft && !draftJson.isNullOrBlank()) {
            try {
                // Quick parse JSON string without heavy library dependency
                val cleanedJson = draftJson.trim().removePrefix("{").removeSuffix("}").trim()
                val pairs = cleanedJson.split(",")
                var parsedAmount = 0.0
                var parsedType = TransactionType.EXPENSE
                var parsedCatId: Long? = null
                var parsedSubcatId: Long? = null
                var parsedAccountId: Long? = null
                var parsedTargetAccountId: Long? = null
                var parsedNotes: String? = null
                
                pairs.forEach { pair ->
                    val parts = pair.split(":")
                    if (parts.size >= 2) {
                        val key = parts[0].trim().removeSurrounding("\"")
                        val value = parts.drop(1).joinToString(":").trim().removeSurrounding("\"")
                        if (value != "null") {
                            when (key) {
                                "amount" -> parsedAmount = value.toDoubleOrNull() ?: 0.0
                                "type" -> parsedType = TransactionType.valueOf(value)
                                "categoryId" -> parsedCatId = value.toLongOrNull()
                                "subcategoryId" -> parsedSubcatId = value.toLongOrNull()
                                "accountId" -> parsedAccountId = value.toLongOrNull()
                                "targetAccountId" -> parsedTargetAccountId = value.toLongOrNull()
                                "notes" -> parsedNotes = value.replace("\\\"", "\"")
                            }
                        }
                    }
                }
                
                val amtStr = if (parsedAmount > 0.0) parsedAmount.toString().replace(".0", "") else "0"
                rawAmountValue = TextFieldValue(amtStr, selection = TextRange(amtStr.length))
                type = parsedType
                selectedCategoryId = parsedCatId
                subcategoryId = parsedSubcatId
                selectedAccountId = parsedAccountId
                toAccountId = parsedTargetAccountId
                note = parsedNotes ?: ""
                isKeypadExpanded = false
                hasLoadedDraft = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var templateEmoji by remember { mutableStateOf("📝") }
    var templatePinned by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val operatorsList = setOf("+", "-", "×", "÷")

    // Compute a clean display string for the expression with thousands separator commas on digits
    val displayAmount: String = remember(rawAmount) {
        val result = if (rawAmount == "0" || rawAmount.isEmpty()) {
            "0"
        } else {
            val parts = rawAmount.split(" ")
            parts.map { part ->
                if (operatorsList.contains(part)) {
                    part
                } else if (part.isEmpty()) {
                    ""
                } else {
                    val subParts = part.split(".")
                    val integerPart = subParts[0]
                    val decimalPart = if (subParts.size > 1) subParts[1] else null
                    val formattedInteger = try {
                        val longVal = integerPart.toLongOrNull()
                        if (longVal != null) {
                            java.text.DecimalFormat("#,###").format(longVal)
                        } else {
                            integerPart
                        }
                    } catch (e: Exception) {
                        integerPart
                    }
                    buildString {
                        append(formattedInteger)
                        if (decimalPart != null) {
                            append(".")
                            append(decimalPart)
                        } else if (part.endsWith(".")) {
                            append(".")
                        }
                    }
                }
            }.joinToString(" ")
        }
        com.qdash.core.utils.FormatterUtils.convertNumerals(result)
    }

    // Compute live evaluated preview
    val hasOperators = remember(rawAmount) {
        operatorsList.any { rawAmount.contains(it) }
    }
    val livePreviewAmount: String = remember(rawAmount) {
        val result = if (hasOperators) {
            val eval = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
            if (eval % 1 == 0.0) {
                java.text.DecimalFormat("#,###").format(eval.toLong())
            } else {
                "%,.2f".format(eval).replace(",", ".")
            }
        } else {
            ""
        }
        com.qdash.core.utils.FormatterUtils.convertNumerals(result)
    }

    // Auto-select first fitting category/account on first load or type switch
    LaunchedEffect(uiState.categories, uiState.accounts, type) {
        if (transactionId != null) return@LaunchedEffect // Skip auto-selection in edit mode
        if (selectedCategoryId == null && uiState.categories.isNotEmpty()) {
            selectedCategoryId = uiState.categories.firstOrNull {
                when (type) {
                    TransactionType.INCOME -> it.type == CategoryType.INCOME
                    else                   -> it.type == CategoryType.EXPENSE
                }
            }?.id
        }
        if (selectedAccountId == null && uiState.accounts.isNotEmpty()) {
            selectedAccountId = uiState.accounts.find { it.isDefault }?.id
                ?: uiState.accounts.first().id
        }
        if (toAccountId == null && uiState.accounts.size > 1) {
            toAccountId = uiState.accounts.lastOrNull { it.id != selectedAccountId }?.id
                ?: uiState.accounts.first().id
        }
    }

    LaunchedEffect(selectedCategoryId, uiState.categories) {
        if (selectedCategoryId != null) {
            val category = uiState.categories.find { it.id == selectedCategoryId }
            if (category?.name?.contains("راتب") == true || category?.name?.contains("الراتب") == true) {
                isSalaryAutomation = true
            }
        }
    }

    // Active-type accent color
    val typeAccentColor = when (type) {
        TransactionType.EXPENSE  -> ExpenseRed
        TransactionType.INCOME   -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }



    val scrollState = rememberScrollState()

    // â”€â”€ Root column: top-bar / scrollable form / keypad â”€â”€
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("add_transaction_screen")
    ) {

        // â”€â”€ Top bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        FinTrackTopBar(
            title = if (transactionId != null) "تعديل العملية المالية" else "إضافة عملية مالية",
            showBackButton = true,
            onBackClick = onBack
        )

        // â”€â”€ Scrollable form body â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Transaction type selector chip bar
            TypeSelectorBar(
                selected = type,
                onSelect = { newType ->
                    type = newType
                    if (newType == TransactionType.TRANSFER) {
                        selectedCategoryId = null
                        subcategoryId = null
                    } else {
                        selectedCategoryId = uiState.categories.firstOrNull {
                            when (newType) {
                                TransactionType.INCOME -> it.type == CategoryType.INCOME
                                else                   -> it.type == CategoryType.EXPENSE
                            }
                        }?.id
                        subcategoryId = null
                    }
                }
            )

            // Amount display card
            AmountDisplayCard(
                rawAmountValue = rawAmountValue,
                onValueChange = { newValue ->
                    rawAmountValue = if (newValue.text == rawAmountValue.text) {
                        newValue
                    } else {
                        sanitizeAmountTextFieldValue(newValue)
                    }
                },
                displayAmount = displayAmount,
                livePreviewAmount = livePreviewAmount,
                accentColor = typeAccentColor,
                showAmountWords = showAmountWords,
                onTap = { isKeypadExpanded = true }
            )

            // â”€â”€ Note input (Moved to the top) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            OutlinedTextField(
                value = note,
                onValueChange = { newNote ->
                    note = newNote
                    val parsedAmt = rawAmount.toDoubleOrNull() ?: 0.0
                    viewModel.onNoteChanged(newNote, parsedAmt, selectedAccountId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input"),
                placeholder = {
                    Text(
                        text = "أضف ملاحظة أو سبب المعاملة…",
                        color = TextGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = TextGray)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeAccentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = typeAccentColor
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // â”€â”€ Tags Section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                SectionLabel(text = "الوسوم (Tags)")
                Spacer(modifier = Modifier.height(4.dp))
                
                // Display selected tags as small chips
                if (selectedTags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedTags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(typeAccentColor.copy(alpha = 0.12f))
                                    .border(
                                        width = 0.5.dp,
                                        color = typeAccentColor.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedTags = selectedTags - tag }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = typeAccentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف",
                                        tint = typeAccentColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Preset recommended tags
                val presetTags = listOf("غذاء", "بنزين", "فاتورة", "سفر", "هدايا", "صحة", "ملابس", "إنترنت")
                val availablePresets = presetTags.filter { it !in selectedTags }
                
                Text(
                    text = "وسوم مقترحة:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availablePresets.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { selectedTags = selectedTags + tag }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Button to add custom tag
                    var showCustomTagInput by remember { mutableStateOf(false) }
                    var customTagText by remember { mutableStateOf("") }
                    
                    if (showCustomTagInput) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            BasicTextField(
                                value = customTagText,
                                onValueChange = { customTagText = it },
                                textStyle = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .width(80.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val trimmed = customTagText.trim().replace("#", "")
                                    if (trimmed.isNotEmpty() && trimmed !in selectedTags) {
                                        selectedTags = selectedTags + trimmed
                                    }
                                    customTagText = ""
                                    showCustomTagInput = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "إضافة",
                                    tint = typeAccentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    customTagText = ""
                                    showCustomTagInput = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إلغاء",
                                    tint = TextGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(typeAccentColor.copy(alpha = 0.08f))
                                .clickable { showCustomTagInput = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ وسم مخصص",
                                style = MaterialTheme.typography.labelSmall,
                                color = typeAccentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // â”€â”€ Budget Warning Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            activeBudget?.let { budget ->
                val inputAmount = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
                if (inputAmount > 0.0) {
                    val newSpent = budget.spentAmount + inputAmount
                    val remaining = budget.amountLimit - budget.spentAmount
                    val isExceeded = newSpent > budget.amountLimit
                    val isWarning = !isExceeded && newSpent >= (budget.amountLimit * 0.90)
                    
                    if (isExceeded || isWarning) {
                        val remainingFormatted = com.qdash.core.utils.FormatterUtils.formatCurrency(remaining.coerceAtLeast(0.0))
                        val limitFormatted = com.qdash.core.utils.FormatterUtils.formatCurrency(budget.amountLimit)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExceeded) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                } else {
                                    SavingsAmber.copy(alpha = 0.15f)
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isExceeded) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else SavingsAmber.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExceeded) Icons.Default.Warning else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isExceeded) MaterialTheme.colorScheme.error else SavingsAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isExceeded) "تنبيه: هذا المبلغ يتجاوز الميزانية المحددة للفئة!" else "انتبه: الميزانية المحددة لهذه الفئة أوشكت على النفاد!",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isExceeded) {
                                            "ميزانية الفئة: $limitFormatted (تجاوز بقيمة: ${com.qdash.core.utils.FormatterUtils.formatCurrency(newSpent - budget.amountLimit)})"
                                        } else {
                                            "المتبقي في الميزانية: $remainingFormatted من إجمالي $limitFormatted"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // â”€â”€ Category dropdown (not for Transfer) â”€â”€â”€â”€â”€â”€â”€
            if (type != TransactionType.TRANSFER) {
                Column {
                    // Section header with AI suggestion trigger button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel(text = "الفئة")
                        // AI suggestion trigger button
                        val isAiLoading = uiState.currentSuggestion != null || uiState.suggestedCategory != null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    BorderStroke(1.dp, TransferBlue),
                                    RoundedCornerShape(8.dp)
                                )
                                .background(TransferBlue.copy(alpha = 0.08f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onNoteChanged(
                                        note.ifBlank { "auto" },
                                        parsedAmount,
                                        selectedAccountId
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = TransferBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "اقتراح ذكي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TransferBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    CategoryDropdownSelector(
                        categories = uiState.categories,
                        transactions = uiState.transactions,
                        type = type,
                        selectedCategoryId = selectedCategoryId,
                        subcategoryId = subcategoryId,
                        typeAccentColor = typeAccentColor,
                        smartSortEnabled = uiState.smartCategorySortEnabled,
                        onToggleSmartSort = { viewModel.toggleSmartCategorySort() },
                        onCategorySelected = { parentCatId, subCatId ->
                            selectedCategoryId = parentCatId
                            subcategoryId = subCatId
                        },
                        onAddMainCategory = {
                            newCategoryName = ""
                            newCategoryIcon = "📁"
                            newCategoryColor = "#8B5CF6"
                            isAddingSubcategory = false
                            showAddCategoryDialog = true
                        },
                        onAddSubCategory = {
                            newCategoryName = ""
                            newCategoryIcon = "📁"
                            newCategoryColor = "#8B5CF6"
                            isAddingSubcategory = true
                            showAddCategoryDialog = true
                        },
                        suggestionData = remember(uiState.currentSuggestion, uiState.suggestedCategory, type) {
                            val sugg = uiState.currentSuggestion
                            if (sugg != null && type == TransactionType.EXPENSE) {
                                CategorySuggestionData(
                                    existingCategory = uiState.suggestedCategory,
                                    newCategoryName = sugg.newCategoryName,
                                    newCategoryColor = sugg.newCategoryColor,
                                    newCategoryIcon = sugg.newCategoryIcon,
                                    confidenceScore = sugg.confidenceScore
                                )
                            } else null
                        },
                        onAcceptSuggestion = { catId ->
                            selectedCategoryId = catId
                            viewModel.learnMapping(note, catId)
                            viewModel.clearSuggestion()
                        },
                        onAcceptAndCreateSuggestion = {
                            val newSuggestion = uiState.currentSuggestion
                            if (newSuggestion != null && newSuggestion.newCategoryName != null) {
                                viewModel.createCategoryAndSelect(
                                    name = newSuggestion.newCategoryName,
                                    typeStr = newSuggestion.newCategoryType ?: "EXPENSE",
                                    color = newSuggestion.newCategoryColor ?: "#7f7f7f",
                                    icon = newSuggestion.newCategoryIcon ?: "category",
                                    onCreated = { id ->
                                        selectedCategoryId = id
                                        viewModel.learnMapping(note, id)
                                        viewModel.clearSuggestion()
                                    }
                                )
                            }
                        },
                        onDismissSuggestion = {
                            viewModel.clearSuggestion()
                        }
                    )
                }
            }

            // —— Source account picker —————————————————
            if (uiState.accounts.isNotEmpty()) {
                Column {
                    SectionLabel(
                        text = when (type) {
                            TransactionType.INCOME   -> "الإيداع في حِساب"
                            TransactionType.EXPENSE  -> "الدفع من حِساب"
                            TransactionType.TRANSFER -> "من حساب"
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AccountPickerRow(
                        accounts = uiState.accounts,
                        selectedId = selectedAccountId,
                        accentColor = typeAccentColor,
                        disabledId = null,
                        expectedBalances = expectedBalances,
                        parsedAmount = parsedAmount,
                        onSelect = { newSourceId ->
                            if (type == TransactionType.TRANSFER && toAccountId == newSourceId) {
                                toAccountId = selectedAccountId
                            }
                            selectedAccountId = newSourceId
                        }
                    )
                }
            }

            // —— Destination account (Transfer only) ———————
            if (type == TransactionType.TRANSFER && uiState.accounts.isNotEmpty()) {
                Column {
                    SectionLabel(text = "إلى حساب")
                    Spacer(modifier = Modifier.height(6.dp))
                    AccountPickerRow(
                        accounts = uiState.accounts,
                        selectedId = toAccountId,
                        accentColor = TransferBlue,
                        disabledId = null,
                        expectedBalances = expectedBalances,
                        parsedAmount = parsedAmount,
                        onSelect = { newDestId ->
                            if (selectedAccountId == newDestId) {
                                selectedAccountId = toAccountId
                            }
                            toAccountId = newDestId
                        }
                    )
                }
            }

            // â”€â”€ Date Selector â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Column {
                SectionLabel(text = "تاريخ العملية")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 1.dp,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = typeAccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = FormatterUtils.formatDate(transactionDate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل التاريخ",
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val todayStart = remember { getStartOfDay(System.currentTimeMillis()) }
                    val yesterdayStart = remember { todayStart - 24 * 60 * 60 * 1000L }
                    val isToday = remember(transactionDate) { getStartOfDay(transactionDate) == todayStart }
                    val isYesterday = remember(transactionDate) { getStartOfDay(transactionDate) == yesterdayStart }
                    val isOther = !isToday && !isYesterday

                    // Today Button
                    QuickDateButton(
                        text = "اليوم",
                        isSelected = isToday,
                        accentColor = typeAccentColor,
                        onClick = { transactionDate = System.currentTimeMillis() }
                    )

                    // Yesterday Button
                    QuickDateButton(
                        text = "البارحة",
                        isSelected = isYesterday,
                        accentColor = typeAccentColor,
                        onClick = { transactionDate = System.currentTimeMillis() - 24 * 60 * 60 * 1000L }
                    )

                    // Other Date Button
                    QuickDateButton(
                        text = "تاريخ آخر...",
                        isSelected = isOther,
                        accentColor = typeAccentColor,
                        onClick = { showDatePicker = true }
                    )
                }
            }

            if (showDatePicker) {
                AppDatePickerDialog(
                    initialSelectedDateMillis = transactionDate,
                    onDismissRequest = { showDatePicker = false },
                    onDateSelected = { transactionDate = it },
                    confirmButtonColor = typeAccentColor
                )
            }




            // â”€â”€ Recurring toggle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (isRecurring) Primary else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "عملية متكررة دورياً؟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Primary,
                        checkedThumbColor = Color.White
                    )
                )
            }

            if (type == TransactionType.INCOME) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isSalaryAutomation) Primary else TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل كراتب تلقائي (أتمتة الراتب)؟",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = isSalaryAutomation,
                        onCheckedChange = { isSalaryAutomation = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Primary,
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }

            AnimatedVisibility(visible = isRecurring) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        text = "وتيرة التكرار",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "DAILY" to "يومياً",
                            "WEEKLY" to "أسبوعياً",
                            "MONTHLY" to "شهرياً",
                            "YEARLY" to "سنوياً"
                        ).forEach { (period, label) ->
                            val isSelected = recurringPeriod == period
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) typeAccentColor.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { recurringPeriod = period }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) typeAccentColor else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) typeAccentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // â”€â”€ Fixed bottom area: keypad + save button â”€â”€â”€â”€â”€â”€â”€
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .navigationBarsPadding()
        ) {
            // Keypad toggle handle
            KeypadToggleBar(
                isExpanded = isKeypadExpanded,
                currentAmount = displayAmount,
                onToggle = { isKeypadExpanded = !isKeypadExpanded }
            )

            // Collapsible numpad
            AnimatedVisibility(
                visible = isKeypadExpanded,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
                ) + fadeOut(animationSpec = tween(100))
            ) {
                NumPad(
                    onKeyPress = { key ->
                        rawAmountValue = handleNumpadKey(rawAmountValue, key)
                    }
                )
            }

            // Save transaction / save as template action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val typeIntent = when (type) {
                    TransactionType.INCOME -> ButtonIntent.SUCCESS
                    TransactionType.EXPENSE -> ButtonIntent.DANGER
                    TransactionType.TRANSFER -> ButtonIntent.INFO
                }

                if (transactionId == null) {
                    AppButton(
                        onClick = { showSaveTemplateDialog = true },
                        enabled = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount) > 0.0,
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.BORDERED,
                        intent = typeIntent,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(
                            text = "حفظ كقالب",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                AppButton(
                    onClick = {
                        val parsedAmount = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
                        if (parsedAmount > 0) {
                            val budget = activeBudget
                            val isBudgetWarningOrExceeded = if (budget != null) {
                                val newSpent = budget.spentAmount + parsedAmount
                                  val isExceeded = newSpent > budget.amountLimit
                                  val isWarning = !isExceeded && newSpent >= (budget.amountLimit * 0.90)
                                  isExceeded || isWarning
                            } else false

                            if (isBudgetWarningOrExceeded) {
                                scope.launch {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    delay(150)
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                }
                            } else {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                            val categoryId = if (type == TransactionType.TRANSFER) null else effectiveCategoryId
                            val accountId = effectiveAccountId
                            if ((type != TransactionType.TRANSFER && categoryId == null) || accountId == null) {
                                Toast.makeText(context, "يرجى اختيار حساب وفئة صالحين قبل الحفظ.", Toast.LENGTH_LONG).show()
                                return@AppButton
                            }

                            val selectedAccount = uiState.accounts.find { it.id == accountId }
                            val isSavings = selectedAccount?.type == com.qdash.domain.model.AccountType.SAVINGS
                            
                            if (type == TransactionType.INCOME) {
                                if (isSalaryAutomation) {
                                    if (transactionId != null) {
                                        viewModel.updateTransaction(
                                            id = transactionId,
                                            amount = parsedAmount,
                                            type = type,
                                            categoryId = categoryId,
                                            accountId = accountId,
                                            toAccountId = null,
                                            note = note.ifBlank { null },
                                            date = transactionDate,
                                            isRecurring = isRecurring,
                                            recurringPeriod = if (isRecurring) recurringPeriod else null,
                                            tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                                            kind = com.qdash.domain.model.TransactionKind.SALARY
                                        )
                                    } else {
                                        viewModel.addTransaction(
                                            amount = parsedAmount,
                                            type = type,
                                            categoryId = categoryId,
                                            accountId = accountId,
                                            toAccountId = null,
                                            note = note.ifBlank { null },
                                            date = transactionDate,
                                            isRecurring = isRecurring,
                                            recurringPeriod = if (isRecurring) recurringPeriod else null,
                                            tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                                            kind = com.qdash.domain.model.TransactionKind.SALARY
                                        )
                                    }
                                } else if (isSavings) {
                                    showSavingsConfirmDialog = true
                                } else {
                                    if (transactionId != null) {
                                        viewModel.updateTransaction(
                                            id = transactionId,
                                            amount = parsedAmount,
                                            type = type,
                                            categoryId = categoryId,
                                            accountId = accountId,
                                            toAccountId = null,
                                            note = note.ifBlank { null },
                                            date = transactionDate,
                                            isRecurring = isRecurring,
                                            recurringPeriod = if (isRecurring) recurringPeriod else null,
                                            tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                                            kind = com.qdash.domain.model.TransactionKind.INCOME
                                        )
                                    } else {
                                        viewModel.addTransaction(
                                            amount = parsedAmount,
                                            type = type,
                                            categoryId = categoryId,
                                            accountId = accountId,
                                            toAccountId = null,
                                            note = note.ifBlank { null },
                                            date = transactionDate,
                                            isRecurring = isRecurring,
                                            recurringPeriod = if (isRecurring) recurringPeriod else null,
                                            tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                                            kind = com.qdash.domain.model.TransactionKind.INCOME
                                        )
                                    }
                                }
                            } else if (type == TransactionType.EXPENSE) {
                                val kind = if (isSavings) com.qdash.domain.model.TransactionKind.SAVINGS_WITHDRAWAL else com.qdash.domain.model.TransactionKind.EXPENSE
                                if (transactionId != null) {
                                    viewModel.updateTransaction(
                                        id = transactionId,
                                        amount = parsedAmount,
                                        type = type,
                                        categoryId = categoryId,
                                        accountId = accountId,
                                        toAccountId = null,
                                        note = note.ifBlank { null },
                                        date = transactionDate,
                                        isRecurring = isRecurring,
                                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                                        kind = kind
                                    )
                                } else {
                                    viewModel.addTransaction(
                                        amount = parsedAmount,
                                        type = type,
                                        categoryId = categoryId,
                                        accountId = accountId,
                                        toAccountId = null,
                                        note = note.ifBlank { null },
                                        date = transactionDate,
                                        isRecurring = isRecurring,
                                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                                        kind = kind
                                    )
                                }
                            } else {
                                if (transactionId != null) {
                                    viewModel.updateTransaction(
                                        id = transactionId,
                                        amount = parsedAmount,
                                        type = type,
                                        categoryId = categoryId,
                                        accountId = accountId,
                                        toAccountId = toAccountId,
                                        note = note.ifBlank { null },
                                        date = transactionDate,
                                        isRecurring = isRecurring,
                                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                                        kind = com.qdash.domain.model.TransactionKind.TRANSFER
                                    )
                                } else {
                                    viewModel.addTransaction(
                                        amount = parsedAmount,
                                        type = type,
                                        categoryId = categoryId,
                                        accountId = accountId,
                                        toAccountId = toAccountId,
                                        note = note.ifBlank { null },
                                        date = transactionDate,
                                        isRecurring = isRecurring,
                                        recurringPeriod = if (isRecurring) recurringPeriod else null,
                                        tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                                        kind = com.qdash.domain.model.TransactionKind.TRANSFER
                                    )
                                }
                            }
                        }
                    },
                    enabled = canSaveTransaction,
                    modifier = Modifier.weight(if (transactionId == null) 1.5f else 1f).testTag("save_transaction_button"),
                    variant = ButtonVariant.SOLID,
                    intent = typeIntent,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                ) {
                    Text(
                        text = if (transactionId != null) "حفظ التعديلات" else "تسجيل العملية",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // —— Add Category/Subcategory Dialog ——
        if (showAddCategoryDialog) {
            AddCategoryDialog(
                onDismiss = { showAddCategoryDialog = false },
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
                    showAddCategoryDialog = false
                }
            )
        }

        // —— Savings Confirmation Dialog ——
        if (showSavingsConfirmDialog) {
            SavingsConfirmDialog(
                onDismiss = { showSavingsConfirmDialog = false },
                onConfirm = { isContribution ->
                    showSavingsConfirmDialog = false
                    val categoryId = effectiveCategoryId ?: return@SavingsConfirmDialog
                    val accountId = effectiveAccountId ?: return@SavingsConfirmDialog
                    val parsedAmount = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
                    val kind = if (isContribution) {
                        com.qdash.domain.model.TransactionKind.SAVINGS_CONTRIBUTION
                    } else {
                        com.qdash.domain.model.TransactionKind.INCOME
                    }
                    if (transactionId != null) {
                        viewModel.updateTransaction(
                            id = transactionId,
                            amount = parsedAmount,
                            type = type,
                            categoryId = categoryId,
                            accountId = accountId,
                            toAccountId = null,
                            note = note.ifBlank { null },
                            date = transactionDate,
                            isRecurring = isRecurring,
                            recurringPeriod = if (isRecurring) recurringPeriod else null,
                            tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                            kind = kind
                        )
                    } else {
                        viewModel.addTransaction(
                            amount = parsedAmount,
                            type = type,
                            categoryId = categoryId,
                            accountId = accountId,
                            toAccountId = null,
                            note = note.ifBlank { null },
                            date = transactionDate,
                            isRecurring = isRecurring,
                            recurringPeriod = if (isRecurring) recurringPeriod else null,
                            tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null,
                            kind = kind
                        )
                    }
                }
            )
        }

        // —— Template Dialogs & Overlays ——
        if (showSaveTemplateDialog) {
            SaveTemplateDialog(
                onDismiss = { showSaveTemplateDialog = false },
                typeAccentColor = typeAccentColor,
                onConfirm = { name, emoji, isPinned ->
                    val parsedAmount = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount)
                    if (parsedAmount > 0) {
                        viewModel.saveAsTemplate(
                            name = name,
                            amount = parsedAmount,
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
                        showSaveTemplateDialog = false
                    }
                }
            )
        }
    }
}

private fun getStartOfDay(millis: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
