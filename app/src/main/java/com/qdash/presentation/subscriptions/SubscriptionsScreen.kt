package com.qdash.presentation.subscriptions

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.FinTrackApp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.FormatterUtils
import com.qdash.core.utils.ThousandsSeparatorTransformation
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import com.qdash.domain.model.Subscription
import com.qdash.presentation.navigation.LocalBottomBarPadding
import com.qdash.presentation.navigation.LocalNavController
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val navController = LocalNavController.current
    val bottomPadding = LocalBottomBarPadding.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()

    val containerCategoriesFlow = remember(context) {
        (context.applicationContext as? FinTrackApp)?.container?.categoryRepository?.getAllCategories()
            ?: flowOf(emptyList())
    }
    val containerCategories by containerCategoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val allCategories = if (uiState.categories.isNotEmpty()) uiState.categories else containerCategories
    val expenseCategories = remember(allCategories) {
        allCategories.filter { it.type == CategoryType.EXPENSE }
    }

    var showAddSubDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }
    var deletingSubscription by remember { mutableStateOf<Subscription?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("subscriptions_screen"),
        topBar = {
            FinTrackTopBar(
                title = "الاشتراكات والالتزامات الدورية",
                showBackButton = true,
                onBackClick = {
                    if (navController != null) {
                        navController.popBackStack()
                    } else {
                        onBack()
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 24.dp)
            ) {
                // Cost Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(primary, TransferBlue)
                                    )
                                )
                                .padding(22.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "إجمالي التكلفة الشهرية للاشتراكات النشطة",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${FormatterUtils.formatCurrency(uiState.totalMonthlyCost)} / شهر",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                if (uiState.foreignMonthlyTotals.isNotEmpty()) {
                                    val foreignStr = uiState.foreignMonthlyTotals.entries.joinToString(" • ") { (curr, amt) ->
                                        "${String.format(Locale.US, "%.2f", amt)} $curr / شهر"
                                    }
                                    Text(
                                        text = "اشتراكات بالعملة الأجنبية: ${FormatterUtils.convertNumerals(foreignStr)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick add button
                item {
                    AppButton(
                        onClick = { showAddSubDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.PRIMARY,
                        shape = ShapeTokens.Lg,
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    ) {
                        Text(
                            text = "إضافة اشتراك أو التزام دوري",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (uiState.subscriptions.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "لا توجد اشتراكات مضافة بعد",
                            description = "أضف خدمات البث، التخزين السحابي، الإيجار أو باقات الإنترنت لمراقبة مواعيد تجديدها وتكلفتها الثابتة.",
                            icon = Icons.Default.Receipt,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    items(uiState.subscriptions, key = { it.id }) { sub ->
                        val accName = uiState.accounts.firstOrNull { it.id == sub.accountId }?.name ?: "غير محدد"
                        val catName = allCategories.firstOrNull { it.id == sub.categoryId }?.name
                        SubscriptionCardRow(
                            subscription = sub,
                            accountName = accName,
                            categoryName = catName,
                            onToggleActive = { active -> viewModel.toggleSubscriptionActive(sub, active) },
                            onEditClick = { editingSubscription = sub },
                            onDeleteClick = { deletingSubscription = sub }
                        )
                    }
                }
            }
        }
    }

    if (showAddSubDialog) {
        AddOrEditSubscriptionDialog(
            initialSubscription = null,
            accounts = uiState.accounts,
            expenseCategories = expenseCategories,
            onDismiss = { showAddSubDialog = false },
            onSave = { subName, amount, currency, billingCycle, nextBillingDate, accountId, categoryId, reminderDays, isShiftable ->
                viewModel.addSubscription(
                    name = subName,
                    amount = amount,
                    billingCycle = billingCycle,
                    nextBillingDate = nextBillingDate,
                    accountId = accountId,
                    categoryId = categoryId,
                    reminderDaysBefore = reminderDays,
                    currency = currency,
                    isAutoShiftableBySalary = isShiftable
                )
                showAddSubDialog = false
                Toast.makeText(context, "تمت إضافة الاشتراك بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    editingSubscription?.let { targetSub ->
        AddOrEditSubscriptionDialog(
            initialSubscription = targetSub,
            accounts = uiState.accounts,
            expenseCategories = expenseCategories,
            onDismiss = { editingSubscription = null },
            onSave = { subName, amount, currency, billingCycle, nextBillingDate, accountId, categoryId, reminderDays, isShiftable ->
                viewModel.updateSubscription(
                    targetSub.copy(
                        name = subName,
                        amount = amount,
                        currency = currency,
                        billingCycle = billingCycle,
                        nextBillingDate = nextBillingDate,
                        accountId = accountId,
                        categoryId = categoryId,
                        reminderDaysBefore = reminderDays,
                        isAutoShiftableBySalary = isShiftable
                    )
                )
                editingSubscription = null
                Toast.makeText(context, "تم تحديث الاشتراك بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    deletingSubscription?.let { targetSub ->
        AppDialog(
            onDismissRequest = { deletingSubscription = null },
            title = "حذف الاشتراك الدوري",
            text = "هل أنت متأكد من حذف اشتراك «${targetSub.name}» نهائياً؟ لن تتأثر المعاملات السابقة المسجلة في محفظتك.",
            confirmButtonText = "تأكيد الحذف",
            onConfirm = {
                viewModel.deleteSubscription(targetSub)
                deletingSubscription = null
                Toast.makeText(context, "تم حذف الاشتراك", Toast.LENGTH_SHORT).show()
            },
            dismissButtonText = "إلغاء",
            onDismiss = { deletingSubscription = null },
            isDestructive = true
        )
    }
}

@Composable
private fun SubscriptionCardRow(
    subscription: Subscription,
    accountName: String,
    categoryName: String?,
    onToggleActive: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val cycleLabel = when (subscription.billingCycle.uppercase(Locale.US)) {
        "YEARLY" -> "سنوي"
        "WEEKLY" -> "أسبوعي"
        else -> "شهري"
    }
    val formattedAmount = if (subscription.currency.equals("DZD", ignoreCase = true) || subscription.currency.isBlank()) {
        FormatterUtils.formatCurrency(subscription.amount)
    } else {
        FormatterUtils.convertNumerals("${String.format(Locale.US, "%.2f", subscription.amount)} ${subscription.currency.uppercase(Locale.US)}")
    }

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_item_${subscription.id}"),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.surface,
        onClick = onEditClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (subscription.isActive) primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (subscription.isActive) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = subscription.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = ShapeTokens.Full,
                                color = primary.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = cycleLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        val catPart = if (!categoryName.isNullOrBlank()) " • $categoryName" else ""
                        Text(
                            text = "$accountName$catPart • التجديد: ${FormatterUtils.formatDate(subscription.nextBillingDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (subscription.isActive) primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = subscription.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = primary
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تنبيه قبل ${FormatterUtils.convertNumerals(subscription.reminderDaysBefore.toString())} أيام" +
                            if (subscription.isAutoShiftableBySalary) " • مرن مع الراتب" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الاشتراك",
                            tint = primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف الاشتراك",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOrEditSubscriptionDialog(
    initialSubscription: Subscription?,
    accounts: List<Account>,
    expenseCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        amount: Double,
        currency: String,
        billingCycle: String,
        nextBillingDate: Long,
        accountId: Long,
        categoryId: Long,
        reminderDaysBefore: Int,
        isAutoShiftableBySalary: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val isEditing = initialSubscription != null

    val defaultCategoryId = remember(expenseCategories, initialSubscription) {
        initialSubscription?.categoryId?.takeIf { id -> expenseCategories.any { it.id == id } }
            ?: expenseCategories.firstOrNull {
                it.name.contains("فواتير") || it.name.contains("اشتراك") || it.name.contains("إنترنت") || it.name.contains("انترنت")
            }?.id
            ?: expenseCategories.firstOrNull()?.id
            ?: 1L
    }

    var subName by remember(initialSubscription?.id) { mutableStateOf(initialSubscription?.name ?: "") }
    var subAmount by remember(initialSubscription?.id) {
        mutableStateOf(
            initialSubscription?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var subCurrency by remember(initialSubscription?.id) {
        mutableStateOf(initialSubscription?.currency?.uppercase(Locale.US)?.ifBlank { "DZD" } ?: "DZD")
    }
    var subCycle by remember(initialSubscription?.id) {
        mutableStateOf(initialSubscription?.billingCycle?.uppercase(Locale.US) ?: "MONTHLY")
    }
    var nextBillingDate by remember(initialSubscription?.id) {
        mutableLongStateOf(
            initialSubscription?.nextBillingDate ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
        )
    }
    var selectedAccountId by remember(initialSubscription?.id, accounts) {
        mutableStateOf<Long?>(
            initialSubscription?.accountId?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.firstOrNull()?.id
        )
    }
    var selectedCategoryId by remember(initialSubscription?.id, defaultCategoryId) {
        mutableLongStateOf(defaultCategoryId)
    }
    var reminderDays by remember(initialSubscription?.id) {
        mutableStateOf((initialSubscription?.reminderDaysBefore ?: 3).toString())
    }
    var isShiftable by remember(initialSubscription?.id) {
        mutableStateOf(initialSubscription?.isAutoShiftableBySalary ?: false)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = if (isEditing) "تعديل الاشتراك الدوري" else "تسجيل اشتراك جديد",
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subscription_name_input"),
                        label = { Text("اسم الخدمة أو الاشتراك") },
                        placeholder = { Text("مثال: نيتفليكس، الإنترنت، إيجار") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Amount + Currency Selector
                    OutlinedTextField(
                        value = subAmount,
                        onValueChange = { subAmount = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subscription_amount_input"),
                        label = { Text("قيمة الاشتراك ($subCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = if (subCurrency == "DZD") ThousandsSeparatorTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("العملة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("DZD" to "د.ج (DZD)", "EUR" to "أورو (€)", "USD" to "دولار ($)").forEach { (code, label) ->
                            val isSelected = subCurrency == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { subCurrency = code }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Pick billing cycle
                    Text("دورة الفوترة والدفع:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("MONTHLY" to "شهري", "YEARLY" to "سنوي", "WEEKLY" to "أسبوعي").forEach { (cycle, label) ->
                            val isSelected = subCycle == cycle
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { subCycle = cycle }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Next Billing Date Picker
                    Text("تاريخ التجديد القادم:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = CardVariant.FLAT,
                        onClick = { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                Text(
                                    text = FormatterUtils.formatDate(nextBillingDate),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (showDatePicker) {
                        AppDatePickerDialog(
                            initialSelectedDateMillis = nextBillingDate,
                            onDismissRequest = { showDatePicker = false },
                            onDateSelected = { picked ->
                                nextBillingDate = picked
                                showDatePicker = false
                            }
                        )
                    }

                    // Pick connected account (LazyRow so it never cramps or clips!)
                    if (accounts.isNotEmpty()) {
                        Text("الحساب المخصوم منه:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(accounts, key = { it.id }) { acc ->
                                val isSelected = selectedAccountId == acc.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) primary else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedAccountId = acc.id }
                                        .padding(vertical = 8.dp, horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = acc.name,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Category selector (LazyRow backed by real DB categories)
                    if (expenseCategories.isNotEmpty()) {
                        Text("التصنيف المالي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(expenseCategories, key = { it.id }) { cat ->
                                val isSelected = selectedCategoryId == cat.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) primary else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCategoryId = cat.id }
                                        .padding(vertical = 7.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat.name,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reminderDays,
                        onValueChange = { reminderDays = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("التنبيه قبل التجديد بـ (أيام)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isShiftable = !isShiftable }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "قابل للتأجيل تلقائياً عند تأخر الراتب",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(checked = isShiftable, onCheckedChange = { isShiftable = it })
                    }
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        val amt = FormatterUtils.normalizeAmount(subAmount).toDoubleOrNull() ?: 0.0
                        val rem = reminderDays.toIntOrNull()?.coerceIn(0, 30) ?: 3
                        when {
                            subName.isBlank() -> Toast.makeText(context, "الرجاء إدخال اسم الخدمة", Toast.LENGTH_SHORT).show()
                            amt <= 0.0 -> Toast.makeText(context, "الرجاء إدخال مبلغ صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                            selectedAccountId == null -> Toast.makeText(context, "الرجاء اختيار الحساب المخصوم منه", Toast.LENGTH_SHORT).show()
                            else -> {
                                onSave(
                                    subName.trim(),
                                    amt,
                                    subCurrency,
                                    subCycle,
                                    nextBillingDate,
                                    selectedAccountId!!,
                                    selectedCategoryId,
                                    rem,
                                    isShiftable
                                )
                            }
                        }
                    },
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text(if (isEditing) "حفظ التعديلات" else "حفظ الاشتراك", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                AppButton(
                    onClick = onDismiss,
                    variant = ButtonVariant.LIGHT,
                    intent = ButtonIntent.PRIMARY
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
