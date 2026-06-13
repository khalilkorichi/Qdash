package com.example.presentation.transactions

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
import com.example.core.ui.components.CategoryChip
import com.example.core.ui.components.FinTrackTopBar
import com.example.domain.model.CategoryType
import com.example.domain.model.TransactionType
import com.example.domain.model.TransactionTemplate
import com.example.ui.theme.*
import com.example.core.utils.FormatterUtils
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


// ─────────────────────────────────────────────────────────
//  AddTransactionScreen
// ─────────────────────────────────────────────────────────

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
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val showAmountWords = remember {
        context.getSharedPreferences("kdach_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("amount_words_enabled", true)
    }

    // ── Local state ──────────────────────────────────────
    var rawAmount by remember { mutableStateOf("0") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var isAddingSubcategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("📁") }
    var newCategoryColor by remember { mutableStateOf("#8B5CF6") }

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

    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var subcategoryId by remember { mutableStateOf<Long?>(null) }

    // Auto-select newly created category or subcategory
    var previousCategories by remember { mutableStateOf<List<com.example.domain.model.Category>>(emptyList()) }
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
    var note by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurringPeriod by remember { mutableStateOf("MONTHLY") }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var isKeypadExpanded by remember { mutableStateOf(true) }
    var transactionDate by remember { mutableStateOf(initialDate ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var type by remember {
        mutableStateOf(
            when (initialType) {
                "INCOME"   -> TransactionType.INCOME
                "TRANSFER" -> TransactionType.TRANSFER
                else       -> TransactionType.EXPENSE
            }
        )
    }

    var selectedAccountId   by remember { mutableStateOf<Long?>(null) }
    var toAccountId         by remember { mutableStateOf<Long?>(null) }

    val parsedAmount = remember(rawAmount) {
        com.example.core.utils.CalculatorParser.evaluate(rawAmount)
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

    // Reactively load transaction details if in edit mode
    LaunchedEffect(uiState.transactions, transactionId) {
        if (transactionId != null && uiState.transactions.isNotEmpty()) {
            val transaction = uiState.transactions.find { it.id == transactionId }
            if (transaction != null) {
                rawAmount = transaction.amount.toString().replace(".0", "")
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
            }
        }
    }

    // Reactively load pre-fill details from draftJson
    LaunchedEffect(draftJson) {
        if (!draftJson.isNullOrBlank()) {
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
                
                rawAmount = if (parsedAmount > 0.0) parsedAmount.toString().replace(".0", "") else "0"
                type = parsedType
                selectedCategoryId = parsedCatId
                subcategoryId = parsedSubcatId
                selectedAccountId = parsedAccountId
                toAccountId = parsedTargetAccountId
                note = parsedNotes ?: ""
                isKeypadExpanded = false
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
        if (rawAmount == "0" || rawAmount.isEmpty()) {
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
    }

    // Compute live evaluated preview
    val hasOperators = remember(rawAmount) {
        operatorsList.any { rawAmount.contains(it) }
    }
    val livePreviewAmount: String = remember(rawAmount) {
        if (hasOperators) {
            val eval = com.example.core.utils.CalculatorParser.evaluate(rawAmount)
            if (eval % 1 == 0.0) {
                java.text.DecimalFormat("#,###").format(eval.toLong())
            } else {
                "%,.2f".format(eval).replace(",", ".")
            }
        } else {
            ""
        }
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

    // Active-type accent color
    val typeAccentColor = when (type) {
        TransactionType.EXPENSE  -> ExpenseRed
        TransactionType.INCOME   -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }



    val scrollState = rememberScrollState()

    // ── Root column: top-bar / scrollable form / keypad ──
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("add_transaction_screen")
    ) {

        // ── Top bar ──────────────────────────────────────
        FinTrackTopBar(
            title = if (transactionId != null) "تعديل العملية المالية" else "إضافة عملية مالية",
            showBackButton = true,
            onBackClick = onBack
        )

        // ── Scrollable form body ─────────────────────────
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
                    selectedCategoryId = uiState.categories.firstOrNull {
                        when (newType) {
                            TransactionType.INCOME -> it.type == CategoryType.INCOME
                            else                   -> it.type == CategoryType.EXPENSE
                        }
                    }?.id
                    subcategoryId = null
                }
            )

            // Amount display card
            AmountDisplayCard(
                displayAmount = displayAmount,
                livePreviewAmount = livePreviewAmount,
                accentColor = typeAccentColor,
                showAmountWords = showAmountWords,
                onTap = { isKeypadExpanded = true }
            )

            // ── Note input (Moved to the top) ─────────────
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

            // ── Tags Section ──────────────────────────────
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

            // ── Budget Warning Card ───────────────────────
            activeBudget?.let { budget ->
                val inputAmount = com.example.core.utils.CalculatorParser.evaluate(rawAmount)
                if (inputAmount > 0.0) {
                    val newSpent = budget.spentAmount + inputAmount
                    val remaining = budget.amountLimit - budget.spentAmount
                    val isExceeded = newSpent > budget.amountLimit
                    val isWarning = !isExceeded && newSpent >= (budget.amountLimit * 0.90)
                    
                    if (isExceeded || isWarning) {
                        val remainingFormatted = com.example.core.utils.FormatterUtils.formatCurrency(remaining.coerceAtLeast(0.0))
                        val limitFormatted = com.example.core.utils.FormatterUtils.formatCurrency(budget.amountLimit)
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
                                            "ميزانية الفئة: $limitFormatted (تجاوز بقيمة: ${com.example.core.utils.FormatterUtils.formatCurrency(newSpent - budget.amountLimit)})"
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

            // ── Category dropdown (not for Transfer) ───────
            if (type != TransactionType.TRANSFER) {
                CategoryDropdownSelector(
                    categories = uiState.categories,
                    type = type,
                    selectedCategoryId = selectedCategoryId,
                    subcategoryId = subcategoryId,
                    typeAccentColor = typeAccentColor,
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
                    }
                )
            }

            // ── Source account picker ─────────────────────
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
                        onSelect = { selectedAccountId = it }
                    )
                }
            }

            // ── Destination account (Transfer only) ───────
            if (type == TransactionType.TRANSFER && uiState.accounts.isNotEmpty()) {
                Column {
                    SectionLabel(text = "إلى حساب")
                    Spacer(modifier = Modifier.height(6.dp))
                    AccountPickerRow(
                        accounts = uiState.accounts,
                        selectedId = toAccountId,
                        accentColor = TransferBlue,
                        disabledId = selectedAccountId,
                        expectedBalances = expectedBalances,
                        parsedAmount = parsedAmount,
                        onSelect = { toAccountId = it }
                    )
                }
            }

            // ── Date Selector ─────────────────────────────
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
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = transactionDate
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    transactionDate = it
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("موافق", color = typeAccentColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("إلغاء", color = TextGray)
                        }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            selectedDayContainerColor = typeAccentColor,
                            selectedDayContentColor = Color.White,
                            todayContentColor = typeAccentColor,
                            todayDateBorderColor = typeAccentColor
                        )
                    )
                }
            }



            // ── Smart category suggestion ─────────────────
            val suggestedCat = uiState.suggestedCategory
            val confidence = uiState.currentSuggestion?.confidenceScore ?: 0.85f
            if (suggestedCat != null && type == TransactionType.EXPENSE) {
                com.example.presentation.components.SuggestedCategoryCard(
                    category = suggestedCat,
                    confidenceScore = confidence,
                    onAccept = {
                        selectedCategoryId = suggestedCat.id
                        viewModel.learnMapping(note, suggestedCat.id)
                        viewModel.clearSuggestion()
                    }
                )
            }

            // ── Recurring toggle ──────────────────────────
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

        // ── Fixed bottom area: keypad + save button ───────
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
                        rawAmount = handleNumpadKey(rawAmount, key)
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
                if (transactionId == null) {
                    OutlinedButton(
                        onClick = { showSaveTemplateDialog = true },
                        enabled = com.example.core.utils.CalculatorParser.evaluate(rawAmount) > 0.0,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, typeAccentColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = typeAccentColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            tint = typeAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ كقالب",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Button(
                    onClick = {
                        val parsedAmount = com.example.core.utils.CalculatorParser.evaluate(rawAmount)
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
                            if (transactionId != null) {
                                viewModel.updateTransaction(
                                    id = transactionId,
                                    amount = parsedAmount,
                                    type = type,
                                    categoryId = if (type == TransactionType.TRANSFER) {
                                        12L
                                    } else {
                                        subcategoryId ?: selectedCategoryId ?: 1L
                                    },
                                    accountId = selectedAccountId 
                                        ?: uiState.accounts.find { it.isDefault }?.id 
                                        ?: uiState.accounts.firstOrNull()?.id 
                                        ?: 1L,
                                    toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                                    note = note.ifBlank { null },
                                    date = transactionDate,
                                    isRecurring = isRecurring,
                                    recurringPeriod = if (isRecurring) recurringPeriod else null,
                                    tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null
                                )
                            } else {
                                viewModel.addTransaction(
                                    amount = parsedAmount,
                                    type = type,
                                    categoryId = if (type == TransactionType.TRANSFER) {
                                        12L
                                    } else {
                                        subcategoryId ?: selectedCategoryId ?: 1L
                                    },
                                    accountId = selectedAccountId 
                                        ?: uiState.accounts.find { it.isDefault }?.id 
                                        ?: uiState.accounts.firstOrNull()?.id 
                                        ?: 1L,
                                    toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                                    note = note.ifBlank { null },
                                    date = transactionDate,
                                    isRecurring = isRecurring,
                                    recurringPeriod = if (isRecurring) recurringPeriod else null,
                                    tags = if (selectedTags.isNotEmpty()) selectedTags.joinToString(",") else null
                                )
                            }
                            onBack()
                        }
                    },
                    enabled = com.example.core.utils.CalculatorParser.evaluate(rawAmount) > 0.0,
                    modifier = Modifier
                        .weight(if (transactionId == null) 1.5f else 1f)
                        .height(50.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = typeAccentColor,
                        disabledContainerColor = typeAccentColor.copy(alpha = 0.35f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (transactionId != null) "حفظ التعديلات" else "تسجيل العملية",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ── Add Category/Subcategory Dialog ──
        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
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
                                val catType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                                val parentId = if (isAddingSubcategory) selectedCategoryId else null
                                viewModel.addCategory(
                                    name = newCategoryName.trim(),
                                    type = catType,
                                    icon = newCategoryIcon.trim(),
                                    color = newCategoryColor,
                                    parentId = parentId
                                )
                                showAddCategoryDialog = false
                            }
                        },
                        enabled = newCategoryName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = typeAccentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إنشاء الفئة", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) {
                        Text("إلغاء", color = TextGray)
                    }
                }
            )
        }

        // ── Template Dialogs & Overlays ──
        if (showSaveTemplateDialog) {
            AlertDialog(
                onDismissRequest = { showSaveTemplateDialog = false },
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
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
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
                    TextButton(
                        onClick = {
                            val parsedAmount = com.example.core.utils.CalculatorParser.evaluate(rawAmount)
                            if (templateName.isNotBlank() && parsedAmount > 0) {
                                viewModel.saveAsTemplate(
                                    name = templateName,
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
                                    iconEmoji = templateEmoji,
                                    isPinned = templatePinned
                                )
                                showSaveTemplateDialog = false
                                templateName = ""
                            }
                        },
                        enabled = templateName.isNotBlank()
                    ) {
                        Text("حفظ", color = typeAccentColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveTemplateDialog = false }) {
                        Text("إلغاء", color = TextGray)
                    }
                }
            )
        }

        if (showEmojiPicker) {
            com.example.presentation.templates.components.EmojiIconPicker(
                selectedEmoji = templateEmoji,
                onEmojiSelected = {
                    templateEmoji = it
                    showEmojiPicker = false
                },
                onDismissRequest = { showEmojiPicker = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Numpad key handler (pure function, easy to test)
// ─────────────────────────────────────────────────────────

private fun handleNumpadKey(current: String, key: String): String {
    val operators = setOf("+", "-", "×", "÷")
    return when (key) {
        "⌫" -> {
            if (current.length <= 1) "0"
            else {
                // If ending with operator and spaces, drop last 3 characters " op "
                if (current.endsWith(" ")) {
                    val trimmed = current.trimEnd()
                    if (trimmed.isNotEmpty() && operators.contains(trimmed.last().toString())) {
                        trimmed.dropLast(1).trimEnd()
                    } else {
                        current.dropLast(1)
                    }
                } else {
                    current.dropLast(1)
                }
            }
        }
        "C" -> "0"
        "00" -> {
            if (current == "0") "0"
            else {
                val trimmed = current.trim()
                if (trimmed.isNotEmpty() && operators.contains(trimmed.last().toString())) {
                    current + "0"
                } else {
                    current + "00"
                }
            }
        }
        "+", "-", "×", "÷" -> {
            if (current.isEmpty()) "0"
            else {
                val trimmed = current.trim()
                val lastChar = if (trimmed.isNotEmpty()) trimmed.last().toString() else ""
                if (operators.contains(lastChar)) {
                    // Replace operator: drop " op " (3 chars) and append new " op "
                    current.trimEnd().dropLast(1).trimEnd() + " $key "
                } else {
                    current + " $key "
                }
            }
        }
        "." -> {
            val parts = current.split(" ")
            val lastToken = parts.lastOrNull() ?: ""
            if (lastToken.contains(".") || operators.contains(lastToken)) current
            else if (lastToken.isEmpty()) current + "0."
            else current + "."
        }
        "=" -> {
            val eval = com.example.core.utils.CalculatorParser.evaluate(current)
            if (eval % 1 == 0.0) {
                eval.toInt().toString()
            } else {
                "%.2f".format(eval).replace(",", ".")
            }
        }
        else -> { // Digit keys "0" - "9"
            if (current == "0") key
            else {
                val parts = current.split(" ")
                val lastToken = parts.lastOrNull() ?: ""
                val dotIndex = lastToken.indexOf('.')
                if (dotIndex != -1 && lastToken.length - dotIndex > 2) current
                else current + key
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Sub-composables
// ─────────────────────────────────────────────────────────

@Composable
private fun TypeSelectorBar(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit
) {
    val types = listOf(
        TransactionType.EXPENSE  to "مصروف",
        TransactionType.INCOME   to "دخل",
        TransactionType.TRANSFER to "تحويل"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        types.forEach { (t, label) ->
            val isSelected = selected == t
            val activeColor = when (t) {
                TransactionType.EXPENSE  -> ExpenseRed
                TransactionType.INCOME   -> IncomeGreen
                TransactionType.TRANSFER -> TransferBlue
            }
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) activeColor else Color.Transparent,
                animationSpec = tween(200),
                label = "type_bg_$t"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable { onSelect(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AmountDisplayCard(
    displayAmount: String,
    livePreviewAmount: String,
    accentColor: Color,
    showAmountWords: Boolean = true,
    onTap: () -> Unit
) {
    // Calculate the actual numeric amount for words conversion
    val numericAmount = remember(displayAmount, livePreviewAmount) {
        if (livePreviewAmount.isNotEmpty()) {
            livePreviewAmount.replace(",", "").replace(" ", "").toDoubleOrNull() ?: 0.0
        } else {
            displayAmount.replace(",", "").replace(" ", "").toDoubleOrNull() ?: 0.0
        }
    }
    val amountWords = remember(numericAmount) {
        numberToArabicWordsDZ(numericAmount)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = accentColor.copy(alpha = 0.15f),
                spotColor = accentColor.copy(alpha = 0.25f)
            )
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "المبلغ",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayAmount,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (displayAmount.length > 12) 28.sp else 38.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "دج",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            // Amount in Arabic words (Algerian Dinar + Centime system)
            if (showAmountWords && numericAmount > 0 && amountWords.first.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                // Dinar words
                Text(
                    text = amountWords.first,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = accentColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                // Centime equivalent (as Algerians speak daily)
                if (amountWords.second.isNotEmpty()) {
                    Text(
                        text = "بالسنتيم: ${amountWords.second}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextGray.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
            if (livePreviewAmount.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "= $livePreviewAmount دج",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Converts a Dinar amount to Arabic words.
 * Returns Pair(dinarText, centimeText).
 * Example: 1500 -> ("ألف وخمسمائة دينار", "مية وخمسين ألف سنتيم")
 * 1 دينار = 100 سنتيم
 */
private fun numberToArabicWordsDZ(amount: Double): Pair<String, String> {
    if (amount <= 0) return Pair("", "")
    
    val wholePart = amount.toLong()
    
    // Dinar representation
    val dinarText = if (wholePart > 0) {
        val words = convertWholeNumber(wholePart)
        val suffix = when {
            wholePart == 1L -> " دينار"
            wholePart == 2L -> " ديناران"
            wholePart in 3..10 -> " دنانير"
            else -> " دينار"
        }
        "$words$suffix"
    } else ""
    
    // Centime representation (×100) - as Algerians speak daily
    val centimeValue = wholePart * 100
    val centimeText = if (centimeValue > 0) {
        val words = convertWholeNumber(centimeValue)
        val suffix = when {
            centimeValue == 1L -> " سنتيم"
            centimeValue == 2L -> " سنتيمان"
            centimeValue in 3..10 -> " سنتيمات"
            else -> " سنتيم"
        }
        "$words$suffix"
    } else ""
    
    return Pair(dinarText, centimeText)
}

private fun convertWholeNumber(n: Long): String {
    if (n == 0L) return "صفر"
    if (n == 1L) return "واحد"
    if (n == 2L) return "اثنان"

    val ones = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    val teens = arrayOf("عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر",
        "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر")
    val tens = arrayOf("", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    val hundreds = arrayOf("", "مائة", "مئتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")

    val parts = mutableListOf<String>()
    var remaining = n

    // Billions
    if (remaining >= 1_000_000_000) {
        val b = remaining / 1_000_000_000
        remaining %= 1_000_000_000
        parts.add(when {
            b == 1L -> "مليار"
            b == 2L -> "ملياران"
            b in 3..10 -> "${convertSmall(b)} مليارات"
            else -> "${convertSmall(b)} مليار"
        })
    }

    // Millions
    if (remaining >= 1_000_000) {
        val m = remaining / 1_000_000
        remaining %= 1_000_000
        parts.add(when {
            m == 1L -> "مليون"
            m == 2L -> "مليونان"
            m in 3..10 -> "${convertSmall(m)} ملايين"
            else -> "${convertSmall(m)} مليون"
        })
    }

    // Thousands
    if (remaining >= 1000) {
        val t = remaining / 1000
        remaining %= 1000
        parts.add(when {
            t == 1L -> "ألف"
            t == 2L -> "ألفان"
            t in 3..10 -> "${convertSmall(t)} آلاف"
            else -> "${convertSmall(t)} ألف"
        })
    }

    // Hundreds
    if (remaining >= 100) {
        val h = (remaining / 100).toInt()
        remaining %= 100
        parts.add(hundreds[h])
    }

    // Tens and ones
    if (remaining > 0) {
        if (remaining in 10..19) {
            parts.add(teens[(remaining - 10).toInt()])
        } else {
            val o = (remaining % 10).toInt()
            val t = (remaining / 10).toInt()
            if (o > 0 && t > 0) {
                parts.add("${ones[o]} و${tens[t]}")
            } else if (t > 0) {
                parts.add(tens[t])
            } else if (o > 0) {
                parts.add(ones[o])
            }
        }
    }

    return parts.joinToString(" و")
}

private fun convertSmall(n: Long): String {
    val ones = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    val teens = arrayOf("عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر",
        "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر")
    val tens = arrayOf("", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    val hundreds = arrayOf("", "مائة", "مئتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")
    
    if (n == 0L) return "صفر"
    val parts = mutableListOf<String>()
    var r = n
    if (r >= 100) { parts.add(hundreds[(r/100).toInt()]); r %= 100 }
    if (r in 10..19) { parts.add(teens[(r-10).toInt()]); r = 0 }
    if (r > 0) {
        val o = (r % 10).toInt()
        val t = (r / 10).toInt()
        if (o > 0 && t > 0) parts.add("${ones[o]} و${tens[t]}")
        else if (t > 0) parts.add(tens[t])
        else if (o > 0) parts.add(ones[o])
    }
    return parts.joinToString(" و")
}

@Composable
private fun AccountPickerRow(
    accounts: List<com.example.domain.model.Account>,
    selectedId: Long?,
    accentColor: Color,
    disabledId: Long?,
    expectedBalances: Map<Long, Double> = emptyMap(),
    parsedAmount: Double = 0.0,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accounts.forEach { acc ->
            val isSelected = selectedId == acc.id
            val isDisabled = acc.id == disabledId
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected  -> accentColor.copy(alpha = 0.18f)
                            isDisabled  -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else        -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable(enabled = !isDisabled) { onSelect(acc.id) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = acc.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                        isSelected -> accentColor
                        else       -> MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = FormatterUtils.formatCurrency(acc.balance),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        isSelected -> accentColor.copy(alpha = 0.8f)
                        else       -> TextGray
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // Live preview of expected balance
                val expectedBalance = expectedBalances[acc.id] ?: acc.balance
                val showExpected = isSelected && parsedAmount > 0.0 && expectedBalance != acc.balance
                if (showExpected) {
                    val isPlus = expectedBalance > acc.balance
                    Text(
                        text = "➔ " + FormatterUtils.formatCurrency(expectedBalance),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isPlus) IncomeGreen else ExpenseRed,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun KeypadToggleBar(
    isExpanded: Boolean,
    currentAmount: String,
    onToggle: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown
                              else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isExpanded) "إخفاء لوحة المفاتيح" else "إظهار لوحة المفاتيح",
                style = MaterialTheme.typography.labelMedium,
                color = Primary
            )
        }
        Text(
            text = "$currentAmount دج",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NumPad(onKeyPress: (String) -> Unit) {
    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf(".", "0", "⌫", "+")
    )
    val bottomRow = listOf("C", "00", "=")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isDelete = key == "⌫"
                        val isOperator = key == "+" || key == "-" || key == "×" || key == "÷"
                        val buttonBg = when {
                            isDelete -> ExpenseRed.copy(alpha = 0.12f)
                            isOperator -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        }
                        val textColor = when {
                            isDelete -> ExpenseRed
                            isOperator -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(buttonBg)
                                .clickable { onKeyPress(key) }
                                .testTag("numpad_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDelete) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "حذف",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bottomRow.forEach { key ->
                    val isEquals = key == "="
                    val isClear = key == "C"
                    val buttonBg = when {
                        isEquals -> MaterialTheme.colorScheme.primary
                        isClear -> ExpenseRed.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    }
                    val textColor = when {
                        isEquals -> Color.White
                        isClear -> ExpenseRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val weight = if (isEquals) 2f else 1f

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(buttonBg)
                            .clickable { onKeyPress(key) }
                            .testTag("numpad_key_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveTransactionButton(
    accentColor: Color,
    isEnabled: Boolean,
    isEditMode: Boolean = false,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Button(
            onClick = onClick,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_transaction_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = accentColor.copy(alpha = 0.35f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEditMode) "حفظ التعديلات" else "تسجيل العملية المالية",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CategoryIconView(
    iconStr: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isMaterialIcon = remember(iconStr) {
        iconStr.matches(Regex("^[a-zA-Z_]+$"))
    }

    if (isMaterialIcon) {
        val vectorIcon = when (iconStr) {
            "person" -> Icons.Default.Person
            "groups" -> Icons.Default.Groups
            "home" -> Icons.Default.Home
            "restaurant" -> Icons.Default.Restaurant
            "directions_car" -> Icons.Default.DirectionsCar
            "receipt_long" -> Icons.Default.ReceiptLong
            "shopping_bag" -> Icons.Default.ShoppingBag
            "medical_services" -> Icons.Default.MedicalServices
            "school" -> Icons.Default.School
            "sports_esports" -> Icons.Default.SportsEsports
            "work" -> Icons.Default.Work
            "redeem" -> Icons.Default.Redeem
            "storefront" -> Icons.Default.Storefront
            "schedule" -> Icons.Default.Schedule
            "monetization_on" -> Icons.Default.MonetizationOn
            "savings" -> Icons.Default.Savings
            "payments" -> Icons.Default.Payments
            "account_balance" -> Icons.Default.AccountBalance
            "trending_up" -> Icons.Default.TrendingUp
            "card_giftcard" -> Icons.Default.CardGiftcard
            "shopping_cart" -> Icons.Default.ShoppingCart
            "local_gas_station" -> Icons.Default.LocalGasStation
            "directions_bus" -> Icons.Default.DirectionsBus
            "local_taxi" -> Icons.Default.LocalTaxi
            "flight" -> Icons.Default.Flight
            "checkroom" -> Icons.Default.Checkroom
            "spa" -> Icons.Default.Spa
            "fitness_center" -> Icons.Default.FitnessCenter
            "live_tv" -> Icons.Default.LiveTv
            "event" -> Icons.Default.Event
            "phone_android" -> Icons.Default.PhoneAndroid
            "wifi" -> Icons.Default.Wifi
            "bolt" -> Icons.Default.Bolt
            "water_drop" -> Icons.Default.WaterDrop
            "chair" -> Icons.Default.Chair
            "coffee" -> Icons.Default.Coffee
            "child_care" -> Icons.Default.ChildCare
            "pets" -> Icons.Default.Pets
            "favorite" -> Icons.Default.Favorite
            "star" -> Icons.Default.Star
            "attach_money" -> Icons.Default.AttachMoney
            "receipt" -> Icons.Default.Receipt
            "build" -> Icons.Default.Build
            "local_hospital" -> Icons.Default.LocalHospital
            "mosque" -> Icons.Default.Mosque
            "volunteer_activism" -> Icons.Default.VolunteerActivism
            else -> Icons.Default.Category
        }
        Icon(
            imageVector = vectorIcon,
            contentDescription = null,
            tint = color,
            modifier = modifier
        )
    } else {
        // Emoji or special character icon
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconStr,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

sealed interface DropdownCategoryItem {
    data class MainCategory(val category: com.example.domain.model.Category, val isSelected: Boolean) : DropdownCategoryItem
    data class SubCategory(val category: com.example.domain.model.Category, val parentCategory: com.example.domain.model.Category, val isSelected: Boolean) : DropdownCategoryItem
}

@Composable
private fun CategoryDropdownSelector(
    categories: List<com.example.domain.model.Category>,
    type: TransactionType,
    selectedCategoryId: Long?,
    subcategoryId: Long?,
    typeAccentColor: Color,
    onCategorySelected: (Long?, Long?) -> Unit,
    onAddMainCategory: () -> Unit,
    onAddSubCategory: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val selectedCategory = remember(categories, selectedCategoryId) {
        categories.find { it.id == selectedCategoryId }
    }
    val selectedSubcategory = remember(categories, subcategoryId) {
        categories.find { it.id == subcategoryId }
    }

    val buttonText = remember(selectedCategory, selectedSubcategory) {
        when {
            selectedSubcategory != null && selectedCategory != null -> 
                "${selectedCategory.name} ➔ ${selectedSubcategory.name}"
            selectedCategory != null -> 
                selectedCategory.name
            else -> "اختر الفئة الرئيسية والفرعية"
        }
    }

    val dropdownItems = remember(categories, selectedCategoryId, subcategoryId, type) {
        val mainCats = categories.filter { cat ->
            cat.parentId == null &&
            when (type) {
                TransactionType.INCOME -> cat.type == CategoryType.INCOME
                else                   -> cat.type == CategoryType.EXPENSE
            }
        }
        val subs = categories.filter { it.parentId != null }
        val subsMap = subs.groupBy { it.parentId!! }

        buildList {
            mainCats.forEach { mainCat ->
                val isMainSelected = selectedCategoryId == mainCat.id && subcategoryId == null
                add(DropdownCategoryItem.MainCategory(mainCat, isMainSelected))
                
                val childSubs = subsMap[mainCat.id] ?: emptyList()
                childSubs.forEach { subCat ->
                    val isSubSelected = subcategoryId == subCat.id
                    add(DropdownCategoryItem.SubCategory(subCat, mainCat, isSubSelected))
                }
            }
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(text = "الفئة")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectedCategoryId != null) {
                    Text(
                        text = "إضافة فرعية +",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = typeAccentColor,
                        modifier = Modifier
                            .clickable { onAddSubCategory() }
                            .padding(4.dp)
                    )
                }
                Text(
                    text = "إضافة فئة رئيسية +",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = typeAccentColor,
                    modifier = Modifier
                        .clickable { onAddMainCategory() }
                        .padding(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Category selection button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp))
                .clickable { showDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
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
                    if (selectedCategory != null) {
                        CategoryIconView(
                            iconStr = selectedCategory.icon,
                            color = typeAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = typeAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedCategoryId != null) MaterialTheme.colorScheme.onSurface else TextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "اختر الفئة",
                    tint = TextGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Category picker dialog with LazyColumn for performance
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(
                        text = "اختر الفئة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    if (dropdownItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد فئات مضافة بعد.",
                                color = TextGray
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                count = dropdownItems.size,
                                key = { index ->
                                    when (val item = dropdownItems[index]) {
                                        is DropdownCategoryItem.MainCategory -> "main_${item.category.id}"
                                        is DropdownCategoryItem.SubCategory -> "sub_${item.category.id}"
                                    }
                                }
                            ) { index ->
                                val item = dropdownItems[index]
                                when (item) {
                                    is DropdownCategoryItem.MainCategory -> {
                                        val mainCat = item.category
                                        val isCatSelected = item.isSelected
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(mainCat.color))
                                        } catch (_: Exception) {
                                            typeAccentColor
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isCatSelected) typeAccentColor.copy(alpha = 0.1f)
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    onCategorySelected(mainCat.id, null)
                                                    showDialog = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            CategoryIconView(
                                                iconStr = mainCat.icon,
                                                color = catColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = mainCat.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (isCatSelected) typeAccentColor
                                                        else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isCatSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = typeAccentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    is DropdownCategoryItem.SubCategory -> {
                                        val subCat = item.category
                                        val isSubCatSelected = item.isSelected
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 20.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSubCatSelected) typeAccentColor.copy(alpha = 0.08f)
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    onCategorySelected(item.parentCategory.id, subCat.id)
                                                    showDialog = false
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SubdirectoryArrowLeft,
                                                contentDescription = null,
                                                tint = TextGray.copy(alpha = 0.4f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            CategoryIconView(
                                                iconStr = subCat.icon,
                                                color = typeAccentColor.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = subCat.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isSubCatSelected) typeAccentColor
                                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSubCatSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = typeAccentColor.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("إغلاق", color = typeAccentColor)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun RowScope.QuickDateButton(
    text: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
