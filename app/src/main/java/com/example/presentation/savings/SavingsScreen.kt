package com.example.presentation.savings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.EmptyStateView
import com.example.core.ui.components.FinTrackTopBar
import com.example.core.utils.FormatterUtils
import com.example.domain.model.Account
import com.example.domain.model.SavingGoal
import com.example.domain.model.SavingsContribution
import com.example.domain.model.SavingsContributionType
import com.example.ui.theme.*
import com.example.ui.designsystem.components.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()
    
    // Sub-screen navigation states inside view
    var activeGoalForDetails by remember { mutableStateOf<SavingGoal?>(null) }
    
    // Form and action states
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showEditGoalDialog by remember { mutableStateOf<SavingGoal?>(null) }
    var showDepositDialog by remember { mutableStateOf<SavingGoal?>(null) }
    var showWithdrawDialog by remember { mutableStateOf<SavingGoal?>(null) }
    
    // Add/Edit Goal Form inputs
    var goalName by remember { mutableStateOf("") }
    var goalTarget by remember { mutableStateOf("") }
    var goalColor by remember { mutableStateOf("#4CAF50") }
    var goalStrategy by remember { mutableStateOf("manual") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }

    // Contribution inputs
    var contributionAmount by remember { mutableStateOf("") }
    var contributionNote by remember { mutableStateOf("") }
    var selectedContribAccountId by remember { mutableStateOf<Long?>(null) }
    var contributionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showContribDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.accounts) {
        if (uiState.accounts.isNotEmpty() && selectedAccountId == null) {
            selectedAccountId = uiState.accounts.first().id
            selectedContribAccountId = uiState.accounts.first().id
        }
    }

    LaunchedEffect(activeGoalForDetails, uiState.goals) {
        activeGoalForDetails?.let { current ->
            val updated = uiState.goals.find { it.id == current.id }
            if (updated != null) {
                activeGoalForDetails = updated
                viewModel.selectGoal(updated.id)
            }
        }
    }

    LaunchedEffect(showDepositDialog, showWithdrawDialog) {
        if (showDepositDialog != null || showWithdrawDialog != null) {
            contributionDate = System.currentTimeMillis()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("savings_screen"),
        topBar = {
            if (activeGoalForDetails != null) {
                FinTrackTopBar(
                    title = activeGoalForDetails!!.name,
                    showBackButton = true,
                    onBackClick = { activeGoalForDetails = null }
                )
            } else {
                FinTrackTopBar(
                    title = "حصالة المدخرات والأهداف المالية",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }
        }
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                SavingsDashboardSkeleton()
            } else if (activeGoalForDetails != null) {
                // GOAL DETAILS SCREEN
                val goal = activeGoalForDetails!!
                GoalDetailsContent(
                    goal = goal,
                    uiState = uiState,
                    viewModel = viewModel,
                    onAddDeposit = { showDepositDialog = goal },
                    onWithdraw = { showWithdrawDialog = goal },
                    onEdit = {
                        goalName = goal.name
                        goalTarget = goal.targetAmount.toInt().toString()
                        goalColor = goal.color
                        goalStrategy = viewModel.getGoalStrategy(goal.id)
                        selectedAccountId = goal.accountId
                        showEditGoalDialog = goal
                    }
                )
            } else {
                // SAVINGS OVERVIEW DASHBOARD
                SavingsDashboardContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSelectGoal = { goal ->
                        activeGoalForDetails = goal
                        viewModel.selectGoal(goal.id)
                    },
                    onAddContribution = { goal ->
                        contributionAmount = ""
                        contributionNote = ""
                        showDepositDialog = goal
                    },
                    onWithdrawSavings = { goal ->
                        contributionAmount = ""
                        contributionNote = ""
                        showWithdrawDialog = goal
                    },
                    onCreateGoalClick = {
                        goalName = ""
                        goalTarget = ""
                        goalColor = "#4CAF50"
                        goalStrategy = "manual"
                        if (uiState.accounts.isNotEmpty()) {
                            selectedAccountId = uiState.accounts.first().id
                        }
                        showAddGoalDialog = true
                    }
                )
            }

            // --- DIALOGS ---

            // ADD SAVINGS GOAL DIALOG
            if (showAddGoalDialog) {
                AlertDialog(
                    onDismissRequest = { showAddGoalDialog = false },
                    title = { Text("إنشاء هدف ادخاري جديد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = goalName,
                                onValueChange = { goalName = it },
                                label = { Text("اسم الهدف الادخاري") },
                                placeholder = { Text("مثال: سيارة جديدة، صندوق طوارئ") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = goalTarget,
                                onValueChange = { goalTarget = it },
                                label = { Text("المبلغ المستهدف (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Associated Account Selector
                            Text("ربط الهدف بحساب تمويل أساسي:", style = MaterialTheme.typography.labelSmall)
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                val selectedAccountName = uiState.accounts.find { it.id == selectedAccountId }?.name ?: "اختر الحساب"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(selectedAccountName)
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    uiState.accounts.forEach { account ->
                                        DropdownMenuItem(
                                            text = { Text(account.name) },
                                            onClick = {
                                                selectedAccountId = account.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Savings Strategy
                            Text("خطة الادخار المقترحة:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("manual", "يدوي", SavingsAmber),
                                    Triple("monthly", "شهري", TransferBlue),
                                    Triple("weekly", "أسبوعي", IncomeGreen)
                                ).forEach { (id, label, color) ->
                                    val isSelected = goalStrategy == id
                                    Button(
                                        onClick = { goalStrategy = id },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(label, fontSize = 11.sp)
                                    }
                                }
                            }

                            // Accent Colors selection
                            Text("اللون المميز للمشروع الادخاري:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0").forEach { hex ->
                                    val color = Color(android.graphics.Color.parseColor(hex))
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(color, CircleShape)
                                            .clip(CircleShape)
                                            .clickable { goalColor = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (goalColor == hex) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val target = goalTarget.toDoubleOrNull() ?: 0.0
                                if (goalName.isBlank()) {
                                    Toast.makeText(context, "الرجاء إدخال اسم الهدف الادخاري", Toast.LENGTH_SHORT).show()
                                } else if (goalTarget.isBlank() || target <= 0.0) {
                                    Toast.makeText(context, "الرجاء إدخال مبلغ مستهدف صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                } else if (selectedAccountId == null) {
                                    Toast.makeText(context, "الرجاء تحديد حساب التمويل الأساسي", Toast.LENGTH_SHORT).show()
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addSavingGoal(
                                        name = goalName.trim(),
                                        targetAmount = target,
                                        deadline = System.currentTimeMillis() + (180L * 24L * 60L * 60L * 1000L), // Standard 6 months
                                        accountId = selectedAccountId!!,
                                        color = goalColor,
                                        strategy = goalStrategy
                                    )
                                    showAddGoalDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SavingsAmber)
                        ) {
                            Text("تأكيد الإنشاء")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddGoalDialog = false }) { Text("إلغاء") }
                    }
                )
            }

            // EDIT SAVINGS GOAL DIALOG
            showEditGoalDialog?.let { goal ->
                AlertDialog(
                    onDismissRequest = { showEditGoalDialog = null },
                    title = { Text("تعديل الهدف الادخاري", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = goalName,
                                onValueChange = { goalName = it },
                                label = { Text("اسم الهدف الادخاري") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = goalTarget,
                                onValueChange = { goalTarget = it },
                                label = { Text("المبلغ المستهدف (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Accent colors Selection
                            Text("تغيير اللون المميز:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0").forEach { hex ->
                                    val color = Color(android.graphics.Color.parseColor(hex))
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(color, CircleShape)
                                            .clickable { goalColor = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (goalColor == hex) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val target = goalTarget.toDoubleOrNull() ?: 0.0
                                if (goalName.isBlank()) {
                                    Toast.makeText(context, "الرجاء إدخال اسم الهدف الادخاري", Toast.LENGTH_SHORT).show()
                                } else if (goalTarget.isBlank() || target <= 0.0) {
                                    Toast.makeText(context, "الرجاء إدخال مبلغ مستهدف صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                } else if (selectedAccountId == null) {
                                    Toast.makeText(context, "الرجاء تحديد حساب التمويل الأساسي", Toast.LENGTH_SHORT).show()
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.editSavingGoal(
                                        goalId = goal.id,
                                        name = goalName.trim(),
                                        targetAmount = target,
                                        deadline = goal.deadline,
                                        accountId = selectedAccountId!!,
                                        color = goalColor,
                                        strategy = goalStrategy
                                    )
                                    showEditGoalDialog = null
                                }
                            }
                        ) {
                            Text("حفظ التغييرات")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditGoalDialog = null }) { Text("إلغاء") }
                    }
                )
            }

            // DEPOSIT DIALOG (ADD CONTRIBUTION)
            showDepositDialog?.let { goal ->
                AlertDialog(
                    onDismissRequest = { showDepositDialog = null },
                    title = { Text("تخصيص مدخرات وإيداع رصيد", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("الهدف: ${goal.name}", fontWeight = FontWeight.Medium, color = SavingsAmber)
                            
                            OutlinedTextField(
                                value = contributionAmount,
                                onValueChange = { contributionAmount = it },
                                label = { Text("مبلغ الإيداع (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = contributionNote,
                                onValueChange = { contributionNote = it },
                                label = { Text("ملاحظات (اختياري)") },
                                placeholder = { Text("مثال: من علاوة هذا الشهر") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // --- DATE SELECTOR CARD ---
                            Text("تاريخ عملية الإيداع:", style = MaterialTheme.typography.labelSmall)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showContribDatePicker = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DateRange, null, tint = SavingsAmber, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = FormatterUtils.formatDate(contributionDate),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Icon(Icons.Default.Edit, null, tint = TextGray, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (showContribDatePicker) {
                                val datePickerState = rememberDatePickerState(
                                    initialSelectedDateMillis = contributionDate
                                )
                                DatePickerDialog(
                                    onDismissRequest = { showContribDatePicker = false },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                datePickerState.selectedDateMillis?.let {
                                                    contributionDate = it
                                                }
                                                showContribDatePicker = false
                                            }
                                        ) {
                                            Text("موافق", color = SavingsAmber, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showContribDatePicker = false }) {
                                            Text("إلغاء", color = TextGray)
                                        }
                                    }
                                ) {
                                    DatePicker(
                                        state = datePickerState,
                                        colors = DatePickerDefaults.colors(
                                            selectedDayContainerColor = SavingsAmber,
                                            selectedDayContentColor = Color.White,
                                            todayContentColor = SavingsAmber,
                                            todayDateBorderColor = SavingsAmber
                                        )
                                    )
                                }
                            }

                            // Source account selector
                            Text("حساب مصدر الأموال:", style = MaterialTheme.typography.labelSmall)
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                val selectedAccountName = uiState.accounts.find { it.id == selectedContribAccountId }?.name ?: "اختر الحساب"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedAccountName)
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    uiState.accounts.forEach { account ->
                                        DropdownMenuItem(
                                            text = { Text("${account.name} (رصيد: ${account.balance.toInt()} د.ج)") },
                                            onClick = {
                                                selectedContribAccountId = account.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = contributionAmount.toDoubleOrNull() ?: 0.0
                                if (contributionAmount.isBlank() || amount <= 0.0) {
                                    Toast.makeText(context, "الرجاء إدخال مبلغ إيداع صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                } else if (selectedContribAccountId == null) {
                                    Toast.makeText(context, "الرجاء تحديد حساب مصدر الأموال", Toast.LENGTH_SHORT).show()
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addDeposit(
                                        goalId = goal.id,
                                        accountId = selectedContribAccountId!!,
                                        amount = amount,
                                        note = contributionNote.ifBlank { null },
                                        date = contributionDate
                                    )
                                    showDepositDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SavingsAmber)
                        ) {
                            Text("إيداع الآن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDepositDialog = null }) { Text("إلغاء") }
                    }
                )
            }

            // WITHDRAW DIALOG (WITHDRAW SAVINGS)
            showWithdrawDialog?.let { goal ->
                AlertDialog(
                    onDismissRequest = { showWithdrawDialog = null },
                    title = { Text("سحب من حصالة الادخار", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("الهدف: ${goal.name}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                            Text("المبلغ المدخر المتوفر: ${goal.currentAmount.toInt()} د.ج", style = MaterialTheme.typography.bodySmall)

                            OutlinedTextField(
                                value = contributionAmount,
                                onValueChange = { contributionAmount = it },
                                label = { Text("مبلغ السحب (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = contributionNote,
                                onValueChange = { contributionNote = it },
                                label = { Text("السبب والسحب إلى") },
                                placeholder = { Text("مثال: لتغطية ظرف طارئ") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // --- DATE SELECTOR CARD ---
                            Text("تاريخ عملية السحب:", style = MaterialTheme.typography.labelSmall)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showContribDatePicker = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = FormatterUtils.formatDate(contributionDate),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Icon(Icons.Default.Edit, null, tint = TextGray, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (showContribDatePicker) {
                                val datePickerState = rememberDatePickerState(
                                    initialSelectedDateMillis = contributionDate
                                )
                                DatePickerDialog(
                                    onDismissRequest = { showContribDatePicker = false },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                datePickerState.selectedDateMillis?.let {
                                                    contributionDate = it
                                                }
                                                showContribDatePicker = false
                                            }
                                        ) {
                                            Text("موافق", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showContribDatePicker = false }) {
                                            Text("إلغاء", color = TextGray)
                                        }
                                    }
                                ) {
                                    DatePicker(
                                        state = datePickerState,
                                        colors = DatePickerDefaults.colors(
                                            selectedDayContainerColor = MaterialTheme.colorScheme.error,
                                            selectedDayContentColor = Color.White,
                                            todayContentColor = MaterialTheme.colorScheme.error,
                                            todayDateBorderColor = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }

                            // Target account selector
                            Text("استقبال رصيد السحب في حساب:", style = MaterialTheme.typography.labelSmall)
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                val selectedAccountName = uiState.accounts.find { it.id == selectedContribAccountId }?.name ?: "اختر الحساب"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedAccountName)
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    uiState.accounts.forEach { account ->
                                        DropdownMenuItem(
                                            text = { Text("${account.name} (رصيد: ${account.balance.toInt()} د.ج)") },
                                            onClick = {
                                                selectedContribAccountId = account.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = contributionAmount.toDoubleOrNull() ?: 0.0
                                if (contributionAmount.isBlank() || amount <= 0.0) {
                                    Toast.makeText(context, "الرجاء إدخال مبلغ سحب صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                } else if (selectedContribAccountId == null) {
                                    Toast.makeText(context, "الرجاء تحديد الحساب المستقبل لرصيد السحب", Toast.LENGTH_SHORT).show()
                                } else if (amount > goal.currentAmount) {
                                    Toast.makeText(context, "المبلغ المراد سحبه أكبر من الرصيد المتوفر في الحصالة (${goal.currentAmount.toInt()} د.ج)!", Toast.LENGTH_LONG).show()
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addWithdrawal(
                                        goalId = goal.id,
                                        accountId = selectedContribAccountId!!,
                                        amount = amount,
                                        note = contributionNote.ifBlank { null },
                                        date = contributionDate
                                    )
                                    showWithdrawDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("سحب الأموال")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWithdrawDialog = null }) { Text("إلغاء") }
                    }
                )
            }
        }
        } // end PullToRefreshBox
    }
}

@Composable
fun SavingsDashboardContent(
    uiState: SavingsUiState,
    viewModel: SavingsViewModel,
    onSelectGoal: (SavingGoal) -> Unit,
    onAddContribution: (SavingGoal) -> Unit,
    onWithdrawSavings: (SavingGoal) -> Unit,
    onCreateGoalClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // SAVINGS METRICS OVERVIEW CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                val totalSaved = uiState.goals.sumOf { it.currentAmount }
                val totalTarget = uiState.goals.sumOf { it.targetAmount }
                val completionPct = if (totalTarget > 0) (totalSaved / totalTarget * 100).toInt() else 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(SavingsAmber, Color(0xFFD97706))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text("إجمالي مدخراتك بالمشروعات", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${String.format(Locale.getDefault(), "%,d", totalSaved.toLong())} د.ج",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("أهداف نشطة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("${uiState.goals.count { !it.isCompleted }} أهداف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("أهداف مكتملة", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("${uiState.goals.count { it.isCompleted }} أهداف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("نسبة الإنجاز", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("$completionPct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Progressive Milestones Celebration Bar
                        LinearProgressIndicator(
                            progress = { if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }


        // MOTIVATIONAL SMART SAVINGS INSIGHTS
        if (uiState.insights.isNotEmpty()) {
            item {
                Text("إضاءات وحوافز مالية ذكية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.insights.take(2).forEach { insight ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(TransferBlue.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (insight.isPositive) Icons.Default.ThumbUp else Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = TransferBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = insight.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION HEADER + NEW GOAL BUTTON
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("أهدافك الادخارية الحالية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onCreateGoalClick) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("أضف هدفاً")
                }
            }
        }

        // GOALS LIST
        if (uiState.goals.isEmpty()) {
            item {
                EmptyStateView(
                    title = "لا توجد أهداف ادخارية نشطة بعد",
                    description = "إنشاء الأهداف الادخارية يعطيك دافعاً مالياً قوياً. ابدأ بإنشاء أول هدف ادخاري لك الآن!"
                )
            }
        } else {
            items(uiState.goals, key = { it.id }) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                val colorHex = goal.color
                val badgeColor = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(SavingsAmber)
                val isPaused = viewModel.isGoalPaused(goal.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectGoal(goal) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(badgeColor, CircleShape)
                                )
                                Text(
                                    text = goal.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isPaused) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("موقّف مؤقتاً", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                            
                            // Edit & action triggers
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { onAddContribution(goal) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.AddCircle, contentDescription = "تخصيص مدخرات", tint = IncomeGreen)
                                }
                                IconButton(onClick = { onWithdrawSavings(goal) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "سحب مدخرات", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Current status info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("المبلغ المدخّر", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Text("${goal.currentAmount.toInt()} د.ج", fontWeight = FontWeight.Bold, color = badgeColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("المبلغ المستهدف", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Text("${goal.targetAmount.toInt()} د.ج", fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Celebrational Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = badgeColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalDetailsContent(
    goal: SavingGoal,
    uiState: SavingsUiState,
    viewModel: SavingsViewModel,
    onAddDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(goal.color)) }.getOrDefault(SavingsAmber)
    val isPaused = viewModel.isGoalPaused(goal.id)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // DETAIL METRIC ROUND-UP CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("وضع الادخار الحالي لهذا الهدف", style = MaterialTheme.typography.labelMedium, color = TextGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${goal.currentAmount.toInt()} د.ج", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(accentColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = accentColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("المستهدف الكلي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            Text("${goal.targetAmount.toInt()} د.ج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("نوع الاستراتيجية", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            val stratText = when (viewModel.getGoalStrategy(goal.id)) {
                                "monthly" -> "شهري منتظم"
                                "weekly" -> "أسبوعي دوري"
                                "leftover" -> "بواقي الحساب"
                                else -> "يدوي مرن"
                            }
                            Text(stratText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TransferBlue)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("نسبة الاكتمال", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Celebratory milestone ticks
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("البداية", fontSize = 10.sp, color = TextGray)
                        Text("رُبع الطريق", fontSize = 10.sp, color = if (progress >= 0.25f) accentColor else TextGray)
                        Text("النصف", fontSize = 10.sp, color = if (progress >= 0.5f) accentColor else TextGray)
                        Text("المعظم", fontSize = 10.sp, color = if (progress >= 0.75f) accentColor else TextGray)
                        Text("تم التحقيق!", fontSize = 10.sp, color = if (progress >= 1.0f) IncomeGreen else TextGray)
                    }
                }
            }
        }

        // AI PREDICTIVE FORECAST TIMELINE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingUp, null, tint = TransferBlue)
                    Column {
                        Text("توقعات ذكية لوتيرة الادخار", style = MaterialTheme.typography.labelSmall, color = TextGray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(uiState.forecastText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // QUICK TRANSACTION TOOLS & GOAL MANAGEMENT ACTION ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddDeposit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إيداع الآن", fontSize = 12.sp)
                }
                Button(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Remove, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("سحب مدخرات", fontSize = 12.sp)
                }
            }
        }

        // ADVANCED STRATEGIC CONTROL ROW
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Button
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "تعديل", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", fontSize = 12.sp)
                    }

                    // Pause Button
                    TextButton(onClick = { viewModel.togglePauseGoal(goal.id) }) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "تعليق تجميد",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPaused) "تنشيط" else "تعليق مؤقت", fontSize = 12.sp)
                    }

                    // Mark completed
                    if (!goal.isCompleted) {
                        TextButton(onClick = { viewModel.markGoalCompleted(goal.id) }) {
                            Icon(Icons.Default.CheckCircle, "حل", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اكتمال الهدف", fontSize = 12.sp)
                        }
                    }

                    // Delete Button
                    TextButton(
                        onClick = { viewModel.deleteGoal(goal.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, "حذف", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف المشروع", fontSize = 12.sp)
                    }
                }
            }
        }

        // CHRONOLOGICAL LOGS & CONTRIBUTION HISTORY LIST
        item {
            Text("سجل حركات الحصالة والTimeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (uiState.selectedGoalHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لم يتم تسجيل أي إيداعات أو سحوبات بعد في هذا الهدف الادخاري.", textAlign = TextAlign.Center, color = TextGray)
                    }
                }
            }
        } else {
            items(uiState.selectedGoalHistory, key = { it.id }) { contribution ->
                val isDeposit = contribution.type == SavingsContributionType.DEPOSIT
                val sign = if (isDeposit) "+" else "-"
                val amountColor = if (isDeposit) IncomeGreen else Color.Red
                val iconValue = if (isDeposit) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignTop
                val bgValue = if (isDeposit) IncomeGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(bgValue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconValue, contentDescription = null, tint = amountColor, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                val dateStr = sdf.format(Date(contribution.date))
                                Text(
                                    text = contribution.note ?: if (isDeposit) "مساهمة ادخار" else "سحب مدخرات",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextGray)
                            }
                        }
                        Text(
                            text = "$sign ${contribution.amount.toInt()} د.ج",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsDashboardSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
        userScrollEnabled = false
    ) {
        // Total savings overview card skeleton
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .shimmerEffect(RoundedCornerShape(24.dp))
                )
            }
        }

        // Motivational insights skeletons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shimmerEffect(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }

        // Current goals header outline
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(18.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(18.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
        }

        // Goal card skeletons
        items(2) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect(RoundedCornerShape(18.dp))
                )
            }
        }
    }
}

