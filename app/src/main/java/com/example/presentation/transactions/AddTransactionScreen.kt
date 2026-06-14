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


// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  AddTransactionScreen
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    val showAmountWords = uiState.isAmountWordsEnabled

    // â”€â”€ Local state â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    var rawAmount by remember { mutableStateOf("0") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var isAddingSubcategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("ًں“پ") }
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
    var templateEmoji by remember { mutableStateOf("ًں“‌") }
    var templatePinned by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val operatorsList = setOf("+", "-", "أ—", "أ·")

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

    // â”€â”€ Root column: top-bar / scrollable form / keypad â”€â”€
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("add_transaction_screen")
    ) {

        // â”€â”€ Top bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        FinTrackTopBar(
            title = if (transactionId != null) "طھط¹ط¯ظٹظ„ ط§ظ„ط¹ظ…ظ„ظٹط© ط§ظ„ظ…ط§ظ„ظٹط©" else "ط¥ط¶ط§ظپط© ط¹ظ…ظ„ظٹط© ظ…ط§ظ„ظٹط©",
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
                        text = "ط£ط¶ظپ ظ…ظ„ط§ط­ط¸ط© ط£ظˆ ط³ط¨ط¨ ط§ظ„ظ…ط¹ط§ظ…ظ„ط©â€¦",
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
                SectionLabel(text = "ط§ظ„ظˆط³ظˆظ… (Tags)")
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
                                        contentDescription = "ط­ط°ظپ",
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
                val presetTags = listOf("ط؛ط°ط§ط،", "ط¨ظ†ط²ظٹظ†", "ظپط§طھظˆط±ط©", "ط³ظپط±", "ظ‡ط¯ط§ظٹط§", "طµط­ط©", "ظ…ظ„ط§ط¨ط³", "ط¥ظ†طھط±ظ†طھ")
                val availablePresets = presetTags.filter { it !in selectedTags }
                
                Text(
                    text = "ظˆط³ظˆظ… ظ…ظ‚طھط±ط­ط©:",
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
                                    contentDescription = "ط¥ط¶ط§ظپط©",
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
                                    contentDescription = "ط¥ظ„ط؛ط§ط،",
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
                                text = "+ ظˆط³ظ… ظ…ط®طµطµ",
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
                                        text = if (isExceeded) "طھظ†ط¨ظٹظ‡: ظ‡ط°ط§ ط§ظ„ظ…ط¨ظ„ط؛ ظٹطھط¬ط§ظˆط² ط§ظ„ظ…ظٹط²ط§ظ†ظٹط© ط§ظ„ظ…ط­ط¯ط¯ط© ظ„ظ„ظپط¦ط©!" else "ط§ظ†طھط¨ظ‡: ط§ظ„ظ…ظٹط²ط§ظ†ظٹط© ط§ظ„ظ…ط­ط¯ط¯ط© ظ„ظ‡ط°ظ‡ ط§ظ„ظپط¦ط© ط£ظˆط´ظƒطھ ط¹ظ„ظ‰ ط§ظ„ظ†ظپط§ط¯!",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isExceeded) {
                                            "ظ…ظٹط²ط§ظ†ظٹط© ط§ظ„ظپط¦ط©: $limitFormatted (طھط¬ط§ظˆط² ط¨ظ‚ظٹظ…ط©: ${com.example.core.utils.FormatterUtils.formatCurrency(newSpent - budget.amountLimit)})"
                                        } else {
                                            "ط§ظ„ظ…طھط¨ظ‚ظٹ ظپظٹ ط§ظ„ظ…ظٹط²ط§ظ†ظٹط©: $remainingFormatted ظ…ظ† ط¥ط¬ظ…ط§ظ„ظٹ $limitFormatted"
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
                        newCategoryIcon = "ًں“پ"
                        newCategoryColor = "#8B5CF6"
                        isAddingSubcategory = false
                        showAddCategoryDialog = true
                    },
                    onAddSubCategory = {
                        newCategoryName = ""
                        newCategoryIcon = "ًں“پ"
                        newCategoryColor = "#8B5CF6"
                        isAddingSubcategory = true
                        showAddCategoryDialog = true
                    }
                )
            }

            // â”€â”€ Source account picker â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (uiState.accounts.isNotEmpty()) {
                Column {
                    SectionLabel(
                        text = when (type) {
                            TransactionType.INCOME   -> "ط§ظ„ط¥ظٹط¯ط§ط¹ ظپظٹ ط­ظگط³ط§ط¨"
                            TransactionType.EXPENSE  -> "ط§ظ„ط¯ظپط¹ ظ…ظ† ط­ظگط³ط§ط¨"
                            TransactionType.TRANSFER -> "ظ…ظ† ط­ط³ط§ط¨"
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

            // â”€â”€ Destination account (Transfer only) â”€â”€â”€â”€â”€â”€â”€
            if (type == TransactionType.TRANSFER && uiState.accounts.isNotEmpty()) {
                Column {
                    SectionLabel(text = "ط¥ظ„ظ‰ ط­ط³ط§ط¨")
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

            // â”€â”€ Date Selector â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Column {
                SectionLabel(text = "طھط§ط±ظٹط® ط§ظ„ط¹ظ…ظ„ظٹط©")
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
                            contentDescription = "طھط¹ط¯ظٹظ„ ط§ظ„طھط§ط±ظٹط®",
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
                        text = "ط§ظ„ظٹظˆظ…",
                        isSelected = isToday,
                        accentColor = typeAccentColor,
                        onClick = { transactionDate = System.currentTimeMillis() }
                    )

                    // Yesterday Button
                    QuickDateButton(
                        text = "ط§ظ„ط¨ط§ط±ط­ط©",
                        isSelected = isYesterday,
                        accentColor = typeAccentColor,
                        onClick = { transactionDate = System.currentTimeMillis() - 24 * 60 * 60 * 1000L }
                    )

                    // Other Date Button
                    QuickDateButton(
                        text = "طھط§ط±ظٹط® ط¢ط®ط±...",
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
                            Text("ظ…ظˆط§ظپظ‚", color = typeAccentColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("ط¥ظ„ط؛ط§ط،", color = TextGray)
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



            // â”€â”€ Smart category suggestion â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                        text = "ط¹ظ…ظ„ظٹط© ظ…طھظƒط±ط±ط© ط¯ظˆط±ظٹط§ظ‹طں",
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
                        text = "ظˆطھظٹط±ط© ط§ظ„طھظƒط±ط§ط±",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "DAILY" to "ظٹظˆظ…ظٹط§ظ‹",
                            "WEEKLY" to "ط£ط³ط¨ظˆط¹ظٹط§ظ‹",
                            "MONTHLY" to "ط´ظ‡ط±ظٹط§ظ‹",
                            "YEARLY" to "ط³ظ†ظˆظٹط§ظ‹"
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
                            text = "ط­ظپط¸ ظƒظ‚ط§ظ„ط¨",
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
                        text = if (transactionId != null) "ط­ظپط¸ ط§ظ„طھط¹ط¯ظٹظ„ط§طھ" else "طھط³ط¬ظٹظ„ ط§ظ„ط¹ظ…ظ„ظٹط©",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // â”€â”€ Add Category/Subcategory Dialog â”€â”€
        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = {
                    Text(
                        text = if (isAddingSubcategory) "ط¥ظ†ط´ط§ط، ظپط¦ط© ظپط±ط¹ظٹط© ط¬ط¯ظٹط¯ط©" else "ط¥ظ†ط´ط§ط، ظپط¦ط© ط±ط¦ظٹط³ظٹط© ط¬ط¯ظٹط¯ط©",
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
                            label = { Text(if (isAddingSubcategory) "ط§ط³ظ… ط§ظ„ظپط¦ط© ط§ظ„ظپط±ط¹ظٹط©" else "ط§ط³ظ… ط§ظ„ظپط¦ط© ط§ظ„ط±ط¦ظٹط³ظٹط©") },
                            placeholder = { Text("ظ…ط«ط§ظ„: طھط³ظˆظ‚طŒ ظ‡ط¯ط§ظٹط§طŒ ظ†ظ‚ظ„...") },
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
                                label = { Text("ط£ظٹظ‚ظˆظ†ط©/ط±ظ…ط²") },
                                placeholder = { Text("ًں“پ") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(90.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = typeAccentColor,
                                    cursorColor = typeAccentColor
                                )
                            )
                            Text(
                                text = "ظٹظ…ظƒظ†ظƒ ظƒطھط§ط¨ط© ط±ظ…ط² طھط¹ط¨ظٹط±ظٹ (Emoji)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Column {
                            Text(
                                text = "ط§ط®طھط± ظ„ظˆظ† ط§ظ„ظپط¦ط©:",
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
                        Text("ط¥ظ†ط´ط§ط، ط§ظ„ظپط¦ط©", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) {
                        Text("ط¥ظ„ط؛ط§ط،", color = TextGray)
                    }
                }
            )
        }

        // â”€â”€ Template Dialogs & Overlays â”€â”€
        if (showSaveTemplateDialog) {
            AlertDialog(
                onDismissRequest = { showSaveTemplateDialog = false },
                title = {
                    Text(
                        text = "ط­ظپط¸ ظƒظ‚ط§ظ„ط¨ ظ…ط¹ط§ظ…ظ„ط©",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ط³ظٹطھظ… ط­ظپط¸ ظ‡ط°ظ‡ ط§ظ„ظ…ط¹ط§ظ…ظ„ط© ظƒظ‚ط§ظ„ط¨ ظ„طھطھظ…ظƒظ† ظ…ظ† ط¥ط¹ط§ط¯ط© ط§ط³طھط®ط¯ط§ظ…ظ‡ط§ ط¨ط¶ط؛ط·ط© ط²ط± ظˆط§ط­ط¯ط©.",
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
                                label = { Text("ط§ط³ظ… ط§ظ„ظ‚ط§ظ„ط¨") },
                                placeholder = { Text("ظ…ط«ط§ظ„: ظ‚ظ‡ظˆط©طŒ ظپط§طھظˆط±ط© ط§ظ„ط¥ظ†طھط±ظ†طھâ€¦") },
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
                                Text("طھط«ط¨ظٹطھ ط§ظ„ظ‚ط§ظ„ط¨ ظپظٹ ط§ظ„ط±ط¦ظٹط³ظٹط©", style = MaterialTheme.typography.bodyMedium)
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
                        Text("ط­ظپط¸", color = typeAccentColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveTemplateDialog = false }) {
                        Text("ط¥ظ„ط؛ط§ط،", color = TextGray)
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  Numpad key handler (pure function, easy to test)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun handleNumpadKey(current: String, key: String): String {
    val operators = setOf("+", "-", "أ—", "أ·")
    return when (key) {
        "âŒ«" -> {
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
        "+", "-", "أ—", "أ·" -> {
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  Sub-composables
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun getStartOfDay(millis: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
