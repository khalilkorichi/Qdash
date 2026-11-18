package com.qdash.presentation.budgetgoals

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.FinTrackApp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.FormatterUtils
import com.qdash.core.utils.ThousandsSeparatorTransformation
import com.qdash.domain.model.Account
import com.qdash.domain.model.BudgetGoal
import com.qdash.domain.model.BudgetType
import com.qdash.domain.model.Category
import com.qdash.presentation.components.getIconByName
import com.qdash.presentation.components.safeParseColor
import com.qdash.presentation.navigation.LocalBottomBarPadding
import com.qdash.presentation.navigation.LocalNavController
import com.qdash.presentation.navigation.Screen
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TextGray
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetGoalDetailsScreen(
    budgetId: Long,
    viewModel: BudgetGoalsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val navController = LocalNavController.current
    val bottomPadding = LocalBottomBarPadding.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Collect real accounts from DI container (or fallback to uiState.accounts)
    val appAccountsFlow = remember(context) {
        (context.applicationContext as? FinTrackApp)?.container?.accountRepository?.getAllAccounts()
            ?: flowOf(emptyList())
    }
    val containerAccounts by appAccountsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val resolvedAccounts: List<Account> = if (uiState.accounts.isNotEmpty()) uiState.accounts else containerAccounts

    // Select matching budget once loaded
    LaunchedEffect(budgetId, uiState.budgets) {
        val target = uiState.budgets.find { it.id == budgetId }
        if (target != null && uiState.selectedBudget?.id != target.id) {
            viewModel.selectBudgetGoal(target)
        }
    }

    val budget = uiState.budgets.find { it.id == budgetId }
        ?: uiState.selectedBudget?.takeIf { it.id == budgetId }
    val relatedTransactions = uiState.selectedBudgetTransactions

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (budget == null) {
        BudgetGoalDetailsSkeleton(onBack = onBack, modifier = modifier)
        return
    }

    val dzdAmountLimit = FormatterUtils.formatCurrency(budget.amountLimit)
    val dzdAmountSpent = FormatterUtils.formatCurrency(budget.spentAmount)
    val dzdRemaining = FormatterUtils.formatCurrency(budget.remainingAmount.coerceAtLeast(0.0))

    val now = System.currentTimeMillis()
    val totalDuration = budget.endDate - now
    val daysLeft = if (totalDuration <= 0) 0L else TimeUnit.MILLISECONDS.toDays(totalDuration).coerceAtLeast(1)

    val dailySuggestion = if (daysLeft > 0 && budget.remainingAmount > 0) {
        budget.remainingAmount / daysLeft
    } else {
        0.0
    }
    val dzdDailySuggestion = FormatterUtils.formatCurrency(dailySuggestion)

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val periodStr = FormatterUtils.convertNumerals("${dateFormat.format(Date(budget.startDate))} - ${dateFormat.format(Date(budget.endDate))}")

    val customColor = safeParseColor(budget.color, primary)
    val progress = if (budget.amountLimit > 0) {
        (budget.spentAmount / budget.amountLimit).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "BudgetProgress"
    )

    Scaffold(
        topBar = {
            FinTrackTopBar(
                title = "تفاصيل الميزانية",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الميزانية",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.toggleBudgetArchive(budget) }) {
                        Icon(
                            imageVector = if (budget.isActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                            contentDescription = "تعطيل أو تنشيط",
                            tint = if (budget.isActive) IncomeGreen else TextGray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف الميزانية",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 24.dp)
        ) {
            item {
                BudgetStatCard(
                    budget = budget,
                    customColor = customColor,
                    progress = progress,
                    animatedProgress = animatedProgress,
                    dzdRemaining = dzdRemaining,
                    dzdAmountLimit = dzdAmountLimit,
                    dzdAmountSpent = dzdAmountSpent,
                    daysLeft = daysLeft
                )
            }

            if (budget.isActive && daysLeft > 0 && budget.remainingAmount > 0) {
                item {
                    BudgetCoachCard(
                        dzdDailySuggestion = dzdDailySuggestion
                    )
                }
            }

            item {
                val categoryName = if (budget.linkedCategoryId != null) {
                    uiState.categories.find { it.id == budget.linkedCategoryId }?.name ?: "غير معروف"
                } else null

                BudgetPreferencesCard(
                    budget = budget,
                    periodStr = periodStr,
                    categoryName = categoryName,
                    customColor = customColor
                )
            }

            budgetTransactionsSection(
                relatedTransactions = relatedTransactions,
                categories = uiState.categories,
                accounts = resolvedAccounts,
                onTransactionClick = { tx ->
                    navController?.navigate(
                        Screen.AddTransaction.createRoute(tx.type.name, transactionId = tx.id)
                    )
                }
            )
        }

        if (showDeleteConfirm) {
            AppDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = "حذف الميزانية",
                text = "هل أنت متأكد من حذف ميزانية '${budget.title}'؟ لن يتم حذف المعاملات المالية المسجلة، بل سيتم إزالة سقف التتبع فقط.",
                confirmButtonText = "تأكيد الحذف",
                onConfirm = {
                    viewModel.deleteBudgetGoal(budget)
                    showDeleteConfirm = false
                    Toast.makeText(context, "تم حذف الميزانية بنجاح", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                dismissButtonText = "إلغاء",
                onDismiss = { showDeleteConfirm = false },
                isDestructive = true
            )
        }

        if (showEditSheet) {
            EditBudgetGoalBottomSheet(
                budget = budget,
                categories = uiState.categories,
                onDismiss = { showEditSheet = false },
                onSave = { updated ->
                    viewModel.updateBudgetGoal(updated)
                    showEditSheet = false
                    Toast.makeText(context, "تم تحديث الميزانية بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetGoalBottomSheet(
    budget: BudgetGoal,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (BudgetGoal) -> Unit
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary

    var title by remember(budget.id) { mutableStateOf(budget.title) }
    var budgetType by remember(budget.id) { mutableStateOf(budget.budgetType) }
    var linkedCategoryId by remember(budget.id) { mutableStateOf(budget.linkedCategoryId) }
    var amountLimit by remember(budget.id) { mutableStateOf(budget.amountLimit.toInt().toString()) }
    var startDate by remember(budget.id) { mutableLongStateOf(budget.startDate) }
    var endDate by remember(budget.id) { mutableLongStateOf(budget.endDate) }
    var threshold by remember(budget.id) { mutableFloatStateOf(budget.alertThresholdPercent.toFloat()) }
    var selectedColor by remember(budget.id) { mutableStateOf(budget.color) }
    var selectedIcon by remember(budget.id) { mutableStateOf(budget.icon) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val colors = listOf("#6C63FF", "#22C55E", "#EF4444", "#F59E0B", "#3B82F6", "#EE5F5B", "#A770EF", "#00B4D8")
    val icons = listOf("person", "groups", "home", "restaurant", "directions_car", "receipt_long", "shopping_bag", "medical_services")

    AppBottomSheet(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 24.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "تعديل الميزانية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AppInput(
                    value = title,
                    onValueChange = { title = it },
                    label = "عنوان الميزانية"
                )

                Text("نوع الميزانية:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BudgetType.entries.forEach { type ->
                        val isSelected = budgetType == type
                        val label = when (type) {
                            BudgetType.CATEGORY -> "تصنيف محدد"
                            BudgetType.CUSTOM -> "مخصصة"
                            BudgetType.GLOBAL -> "إجمالية"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    budgetType = type
                                    if (type != BudgetType.CATEGORY) linkedCategoryId = null
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (budgetType == BudgetType.CATEGORY) {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedCategory = categories.find { it.id == linkedCategoryId }
                    Box {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = CardVariant.FLAT,
                            onClick = { expanded = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedCategory?.name ?: "اختر التصنيف المستهدف")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.filter { it.type.name == "EXPENSE" }.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        linkedCategoryId = category.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                AppInput(
                    value = amountLimit,
                    onValueChange = { amountLimit = it },
                    label = "الحد الأقصى للميزانية (د.ج)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppCard(
                        modifier = Modifier.weight(1f),
                        variant = CardVariant.FLAT,
                        onClick = { showStartPicker = true }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("تاريخ البداية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FormatterUtils.formatDate(startDate), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    AppCard(
                        modifier = Modifier.weight(1f),
                        variant = CardVariant.FLAT,
                        onClick = { showEndPicker = true }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("تاريخ النهاية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FormatterUtils.formatDate(endDate), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showStartPicker) {
                    AppDatePickerDialog(
                        initialSelectedDateMillis = startDate,
                        onDismissRequest = { showStartPicker = false },
                        onDateSelected = { picked ->
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = picked
                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }
                            startDate = cal.timeInMillis
                        }
                    )
                }

                if (showEndPicker) {
                    AppDatePickerDialog(
                        initialSelectedDateMillis = endDate,
                        onDismissRequest = { showEndPicker = false },
                        onDateSelected = { picked ->
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = picked
                                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                            }
                            endDate = cal.timeInMillis
                        }
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("نسبة تنبيه الحذر:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("${threshold.toInt()}%", style = MaterialTheme.typography.labelMedium, color = primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 50f..100f,
                        steps = 4
                    )
                }

                Text("اللون:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(colors) { hex ->
                        val c = safeParseColor(hex, primary)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Text("الرمز:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(icons) { iconName ->
                        val isSelected = selectedIcon == iconName
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconByName(iconName),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                AppButton(
                    onClick = {
                        val parsedLimit = amountLimit.toDoubleOrNull() ?: 0.0
                        when {
                            title.isBlank() -> Toast.makeText(context, "الرجاء إدخال عنوان الميزانية", Toast.LENGTH_SHORT).show()
                            parsedLimit <= 0.0 -> Toast.makeText(context, "الرجاء إدخال مبلغ صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                            budgetType == BudgetType.CATEGORY && linkedCategoryId == null ->
                                Toast.makeText(context, "الرجاء اختيار التصنيف المرتبط", Toast.LENGTH_SHORT).show()
                            endDate <= startDate ->
                                Toast.makeText(context, "تاريخ النهاية يجب أن يكون بعد تاريخ البداية", Toast.LENGTH_SHORT).show()
                            else -> {
                                onSave(
                                    budget.copy(
                                        title = title.trim(),
                                        budgetType = budgetType,
                                        linkedCategoryId = if (budgetType == BudgetType.CATEGORY) linkedCategoryId else null,
                                        amountLimit = parsedLimit,
                                        startDate = startDate,
                                        endDate = endDate,
                                        alertThresholdPercent = threshold.toInt(),
                                        color = selectedColor,
                                        icon = selectedIcon
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg
                ) {
                    Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
