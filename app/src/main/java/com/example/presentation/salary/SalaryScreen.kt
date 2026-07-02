package com.example.presentation.salary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.UnifiedScreenHeader
import com.example.core.utils.FormatterUtils
import com.example.domain.model.IncomeSource
import com.example.domain.model.Subscription
import com.example.domain.model.Account
import com.example.domain.model.AffectedObligation
import com.example.domain.model.Category
import com.example.domain.model.DelaySeverity
import com.example.domain.model.EnvelopeType
import com.example.domain.model.SalaryEnvelope
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.Primary
import com.example.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryScreen(
    viewModel: SalaryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.overview?.salary == null && !uiState.isLoading) {
                FloatingActionButton(
                    onClick = { viewModel.setShowAddDialog(true) },
                    containerColor = Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة راتب")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                UnifiedScreenHeader(
                    title = "إدارة الراتب",
                    subtitle = "تحكم في راتبك وتوزيعه التلقائي بذكاء",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else if (uiState.overview?.salary == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                               .size(80.dp)
                               .background(Primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Text(
                            text = "لم تقم بإضافة راتب بعد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "أضف راتبك الأساسي لتمكين ميزات التوزيع التلقائي وإدارة الميزانية الذكية وتأجيل الرواتب.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.setShowAddDialog(true) },
                            modifier = Modifier.fillMaxWidth(0.7f),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("إضافة الراتب الآن", color = Color.White)
                        }
                    }
                }
            } else {
                val salary = uiState.overview!!.salary!!
                
                item {
                    SalaryOverviewCard(
                        salary = salary,
                        onEdit = { viewModel.setShowAddDialog(true, salary) },
                        onDelayClick = { viewModel.startAddSalaryDelay() }
                    )
                }

                if (uiState.overview!!.delays.isNotEmpty()) {
                    item {
                        Text(
                            text = "سجل التأجيلات الأخير",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(uiState.overview!!.delays.take(3)) { delay ->
                        SalaryDelayHistoryCard(
                            delay = delay,
                            onEdit = { viewModel.startEditSalaryDelay(delay) },
                            onDelete = { viewModel.deleteSalaryDelay(delay.id) },
                            onDepositNow = { viewModel.depositSalaryNow(delay.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    DistributionConfigCard(uiState, viewModel)
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val delaySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        if (uiState.showAddDialog) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowAddDialog(false) },
                sheetState = addSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AddSalaryForm(uiState, viewModel)
            }
        }

        if (uiState.showDelayDialog) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowDelayDialog(false) },
                sheetState = delaySheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DelaySalaryForm(uiState, viewModel)
            }
        }
    }
}

@Composable
fun SalaryOverviewCard(
    salary: IncomeSource,
    onEdit: () -> Unit,
    onDelayClick: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(IncomeGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = IncomeGreen)
                    }
                    Column {
                        Text(
                            text = salary.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اليوم المعتاد: ${salary.dayOfMonth} من كل شهر",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Primary)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = FormatterUtils.formatCurrency(salary.amount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            val colloquialText = remember(salary.amount) {
                FormatterUtils.formatColloquialAlgerian(salary.amount)
            }
            if (colloquialText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "أي ما يعادل: $colloquialText",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الدفعة القادمة المتوقعة",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Text(
                        text = FormatterUtils.formatDate(salary.nextExpectedDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                Button(
                    onClick = onDelayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأجيل الصرف")
                }
            }
        }
    }
}

@Composable
fun SalaryDelayHistoryCard(
    delay: com.example.domain.model.SalaryDelay,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDepositNow: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    var showConfirmDeposit by remember { mutableStateOf(false) }
    val Primary = MaterialTheme.colorScheme.primary

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("إلغاء تأجيل الراتب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في إلغاء تأجيل الراتب وإعادة الالتزامات المالية المرتبطة لمواعيدها الأصلية؟", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    }
                ) {
                    Text("تأكيد الإلغاء", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("تراجع", color = TextGray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showConfirmDeposit) {
        AlertDialog(
            onDismissRequest = { showConfirmDeposit = false },
            title = { Text("تأكيد إيداع الراتب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = { Text("هل تم تلقي الراتب بالفعل وتريد إلغاء التأجيل الحالي وإثبات استلام الراتب في الحساب الآن؟", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDepositNow()
                        showConfirmDeposit = false
                    }
                ) {
                    Text("تأكيد الإيداع", color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeposit = false }) {
                    Text("تراجع", color = TextGray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    val severityColor = when {
        delay.severityScore <= 20 -> IncomeGreen
        delay.severityScore <= 45 -> Color(0xFFFFC107)
        delay.severityScore <= 70 -> Color(0xFFFF9800)
        else -> ExpenseRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Primary.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(severityColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = severityColor
                        )
                    }
                    Column {
                        Text(
                            text = "تأجيل صرف الراتب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "بمقدار ${delay.delayDays} أيام",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(severityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "الضرر: ${delay.severityScore}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                    }

                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Primary)
                    }
                    IconButton(onClick = { showConfirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "إلغاء", tint = ExpenseRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التاريخ الأصلي",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Text(
                        text = FormatterUtils.formatShortDate(delay.originalDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = "التاريخ الجديد",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Text(
                        text = FormatterUtils.formatShortDate(delay.newDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                Button(
                    onClick = { showConfirmDeposit = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncomeGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إيداع الآن",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun DistributionConfigCard(
    uiState: SalaryUiState,
    viewModel: SalaryViewModel
) {
    val Primary = MaterialTheme.colorScheme.primary
    val salary = uiState.overview?.salary
    val salaryAmount = salary?.amount ?: 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التوزيع التلقائي للراتب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "قاعدة 50/30/20",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
                Switch(
                    checked = uiState.distributionEnabled,
                    onCheckedChange = { viewModel.toggleDistribution(it) }
                )
            }

            AnimatedVisibility(visible = uiState.distributionEnabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Visual distribution bar
                    DistributionBar(
                        needsPercentage = uiState.needsPercentage,
                        wantsPercentage = uiState.wantsPercentage,
                        savingsPercentage = uiState.savingsPercentage
                    )

                    // Percentage Sliders
                    PercentageSliderRow(
                        label = "🏠 احتياجات",
                        percentage = uiState.needsPercentage,
                        color = Color(0xFF4CAF50),
                        amount = salaryAmount * uiState.needsPercentage / 100.0,
                        onValueChange = { viewModel.updateDistributionPercentage(EnvelopeType.NEEDS, it) },
                        onValueChangeFinished = { viewModel.commitDistributionPercentages() }
                    )
                    PercentageSliderRow(
                        label = "🎮 رغبات",
                        percentage = uiState.wantsPercentage,
                        color = Color(0xFFFF9800),
                        amount = salaryAmount * uiState.wantsPercentage / 100.0,
                        onValueChange = { viewModel.updateDistributionPercentage(EnvelopeType.WANTS, it) },
                        onValueChangeFinished = { viewModel.commitDistributionPercentages() }
                    )
                    PercentageSliderRow(
                        label = "💰 ادخار",
                        percentage = uiState.savingsPercentage,
                        color = Color(0xFF2196F3),
                        amount = salaryAmount * uiState.savingsPercentage / 100.0,
                        onValueChange = { viewModel.updateDistributionPercentage(EnvelopeType.SAVINGS, it) },
                        onValueChangeFinished = { viewModel.commitDistributionPercentages() }
                    )

                    // Envelope cards (when saved to DB)
                    if (uiState.envelopes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الأظرف المالية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.envelopes.forEach { envelope ->
                            EnvelopeCard(
                                envelope = envelope,
                                categories = uiState.categories,
                                accounts = uiState.accounts,
                                onLinkCategory = { viewModel.showCategoryPickerFor(envelope.id) },
                                onLinkAccount = { accountId -> viewModel.linkAccountToEnvelope(envelope.id, accountId) }
                            )
                        }
                    }

                    if (uiState.isDistributionSaving) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DistributionBar(
    needsPercentage: Int,
    wantsPercentage: Int,
    savingsPercentage: Int
) {
    val needsColor = Color(0xFF4CAF50)
    val wantsColor = Color(0xFFFF9800)
    val savingsColor = Color(0xFF2196F3)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            if (needsPercentage > 0) {
                Box(
                    modifier = Modifier
                        .weight(needsPercentage.toFloat())
                        .fillMaxHeight()
                        .background(needsColor)
                )
            }
            if (wantsPercentage > 0) {
                Box(
                    modifier = Modifier
                        .weight(wantsPercentage.toFloat())
                        .fillMaxHeight()
                        .background(wantsColor)
                )
            }
            if (savingsPercentage > 0) {
                Box(
                    modifier = Modifier
                        .weight(savingsPercentage.toFloat())
                        .fillMaxHeight()
                        .background(savingsColor)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DistributionLegendItem("احتياجات", needsPercentage, needsColor)
            DistributionLegendItem("رغبات", wantsPercentage, wantsColor)
            DistributionLegendItem("ادخار", savingsPercentage, savingsColor)
        }
    }
}

@Composable
private fun DistributionLegendItem(label: String, percentage: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = "$label $percentage%",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray
        )
    }
}

@Composable
private fun PercentageSliderRow(
    label: String,
    percentage: Int,
    color: Color,
    amount: Double,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${FormatterUtils.formatCurrency(amount)} ($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Slider(
            value = percentage.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun EnvelopeCard(
    envelope: SalaryEnvelope,
    categories: List<Category>,
    accounts: List<Account>,
    onLinkCategory: () -> Unit,
    onLinkAccount: (Long?) -> Unit
) {
    val envelopeColor = try {
        Color(android.graphics.Color.parseColor(envelope.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(envelopeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (envelope.type) {
                                EnvelopeType.NEEDS -> "🏠"
                                EnvelopeType.WANTS -> "🎮"
                                EnvelopeType.SAVINGS -> "💰"
                            },
                            fontSize = 18.sp
                        )
                    }
                    Column {
                        Text(
                            text = envelope.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${envelope.percentage}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = envelopeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatterUtils.formatCurrency(envelope.allocatedAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = envelopeColor
                    )
                    Text(
                        text = "متبقي: ${FormatterUtils.formatCurrency(envelope.remainingAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Usage progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                val progress = (envelope.usagePercentage / 100.0).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            if (progress > 0.9f) ExpenseRed
                            else if (progress > 0.7f) Color(0xFFFF9800)
                            else envelopeColor
                        )
                )
            }

            // Linked categories
            if (envelope.linkedCategoryIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val linked = categories.filter { it.id in envelope.linkedCategoryIds }
                    linked.take(4).forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(envelopeColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = envelopeColor
                            )
                        }
                    }
                    if (linked.size > 4) {
                        Text(
                            text = "+${linked.size - 4}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            // Sub-account mapping dropdown selector
            var showAccountDropdown by remember { mutableStateOf(false) }
            val linkedAccount = accounts.find { it.id == envelope.linkedAccountId }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { showAccountDropdown = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏦", fontSize = 14.sp)
                    Text(
                        text = linkedAccount?.name ?: "ربط حساب للتوزيع التلقائي",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (linkedAccount != null) envelopeColor else TextGray,
                        fontWeight = if (linkedAccount != null) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextGray
                )
                
                DropdownMenu(
                    expanded = showAccountDropdown,
                    onDismissRequest = { showAccountDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("إلغاء ربط الحساب", color = ExpenseRed) },
                        onClick = {
                            onLinkAccount(null)
                            showAccountDropdown = false
                        }
                    )
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name) },
                            onClick = {
                                onLinkAccount(acc.id)
                                showAccountDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSalaryForm(uiState: SalaryUiState, viewModel: SalaryViewModel) {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (uiState.editingId == null) "إضافة راتب جديد" else "تعديل الراتب",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("اسم الراتب") },
            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("قيمة الراتب (دج)") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            
            val amtDouble = uiState.amount.toDoubleOrNull()
            if (amtDouble != null && amtDouble > 0) {
                val colloquialText = remember(amtDouble) {
                    FormatterUtils.formatColloquialAlgerian(amtDouble)
                }
                if (colloquialText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "أي ما يعادل: $colloquialText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // Account Selection
        Text("يودع في حساب", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.accounts.take(3).forEach { account ->
                val isSelected = uiState.selectedAccountId == account.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewModel.onAccountSelected(account.id) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = account.name,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Payday selection
        Text("يوم استلام الراتب (تاريخ الدفع)", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = uiState.dayOfMonth.toFloat(),
            onValueChange = { viewModel.onDayOfMonthChange(it.toInt()) },
            valueRange = 1f..31f,
            steps = 30
        )
        Text(
            text = "اليوم: ${uiState.dayOfMonth} من الشهر",
            style = MaterialTheme.typography.bodyMedium,
            color = Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.saveSalary() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("حفظ الراتب", color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelaySalaryForm(uiState: SalaryUiState, viewModel: SalaryViewModel) {
    val Primary = MaterialTheme.colorScheme.primary
    val salary = uiState.overview?.salary ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (uiState.isEditMode) "تعديل تأجيل الراتب" else "تأجيل راتب الشهر الحالي",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = uiState.delayDaysInput,
            onValueChange = viewModel::onDelayDaysChange,
            label = { Text("عدد أيام التأجيل") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.isAnalyzingDelay) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            uiState.delayImpact?.let { impact ->
                // New Expected Date Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("التاريخ المتوقع الجديد:", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                        Text(
                            text = FormatterUtils.formatDate(impact.newDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }

                // Severity Indicator
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مؤشر ضرر التأجيل:", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        Text(
                            text = when (impact.severity) {
                                DelaySeverity.LOW -> "ضرر منخفض"
                                DelaySeverity.MEDIUM -> "ضرر متوسط"
                                DelaySeverity.HIGH -> "ضرر مرتفع"
                                DelaySeverity.CRITICAL -> "ضرر حرج جداً"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = when (impact.severity) {
                                DelaySeverity.LOW -> IncomeGreen
                                DelaySeverity.MEDIUM -> Color(0xFFFFC107)
                                DelaySeverity.HIGH -> Color(0xFFFF9800)
                                DelaySeverity.CRITICAL -> ExpenseRed
                            }
                        )
                    }

                    // Monotone Bar with Accent Progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        val progressFraction = impact.severityScore / 100f
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(
                                    color = when (impact.severity) {
                                        DelaySeverity.LOW -> IncomeGreen
                                        DelaySeverity.MEDIUM -> Color(0xFFFFC107)
                                        DelaySeverity.HIGH -> Color(0xFFFF9800)
                                        DelaySeverity.CRITICAL -> ExpenseRed
                                    }
                                )
                        )
                    }
                }

                // Affected obligations
                Text(
                    text = "الالتزامات المتأثرة خلال فترة التأجيل (${impact.affectedCount})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (impact.affectedObligations.isEmpty()) {
                    Text(
                        text = "لا توجد التزامات مالية متأثرة في هذه الفترة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(impact.affectedObligations) { obs ->
                            AffectedObligationRow(obs = obs, viewModel = viewModel, uiState = uiState)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.confirmSalaryDelay() },
            enabled = uiState.delayImpact != null && !uiState.isConfirmingDelay,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (uiState.delayImpact?.severity) {
                    DelaySeverity.CRITICAL -> ExpenseRed
                    DelaySeverity.HIGH -> Color(0xFFFF9800)
                    else -> Primary
                }
            )
        ) {
            if (uiState.isConfirmingDelay) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(if (uiState.isEditMode) "تأكيد تعديل التأجيل" else "تأكيد تأجيل الراتب", color = Color.White)
            }
        }
    }
}

@Composable
fun AffectedObligationRow(
    obs: AffectedObligation,
    viewModel: SalaryViewModel,
    uiState: SalaryUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = obs.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "المستحق: ${FormatterUtils.formatCurrency(obs.amount)} في ${FormatterUtils.formatShortDate(obs.originalDueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
                
                // Badge for obligation type
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (obs.type == "DEBT") "دين مستحق" else "اشتراك شهري",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }
            }

            // If it is a subscription, allow toggling auto-shift
            if (obs.type == "SUBSCRIPTION") {
                val fullSubscription = uiState.overview?.activeSubscriptions?.firstOrNull { it.id == obs.id }
                if (fullSubscription != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "تأجيل تلقائي",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Switch(
                            checked = fullSubscription.isAutoShiftableBySalary,
                            onCheckedChange = { viewModel.toggleSubscriptionAutoShift(fullSubscription) },
                            modifier = Modifier.scale(0.8f) // Scale it slightly smaller
                        )
                    }
                }
            }
        }
    }
}


