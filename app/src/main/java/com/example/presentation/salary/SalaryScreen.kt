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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.UnifiedScreenHeader
import com.example.core.utils.FormatterUtils
import com.example.domain.model.IncomeSource
import com.example.ui.designsystem.tokens.ColorTokens
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (uiState.incomeSources.isEmpty()) {
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

            if (uiState.incomeSources.isEmpty()) {
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
                            text = "أضف راتبك الأساسي لتمكين ميزات التوزيع التلقائي وإدارة الميزانية الذكية.",
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
                items(uiState.incomeSources) { source ->
                    SalaryCard(
                        source = source,
                        onEdit = { viewModel.setShowAddDialog(true, source) },
                        onDelete = { viewModel.deleteSalary(source) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    DistributionConfigCard(uiState, viewModel)
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (uiState.showAddDialog) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowAddDialog(false) },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AddSalaryForm(uiState, viewModel)
            }
        }
    }
}

@Composable
fun SalaryCard(
    source: IncomeSource,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                            text = source.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يوم الاستلام: ${source.dayOfMonth} من كل شهر",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Primary)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = FormatterUtils.formatCurrency(source.amount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف الراتب")
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("سيتم تفعيل هذه الميزة قريباً لتقسيم راتبك تلقائياً على أظرف أو حسابات فرعية لضمان التزامك بخطتك المالية المحددة.", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                    
                    // Future Implementation: Sliders for Needs, Wants, Savings.
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

        OutlinedTextField(
            value = uiState.amount,
            onValueChange = viewModel::onAmountChange,
            label = { Text("قيمة الراتب (دج)") },
            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = com.example.core.utils.ThousandsSeparatorTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

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
