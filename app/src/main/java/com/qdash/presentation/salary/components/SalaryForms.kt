package com.qdash.presentation.salary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.AffectedObligation
import com.qdash.domain.model.DelaySeverity
import com.qdash.presentation.salary.SalaryUiState
import com.qdash.presentation.salary.SalaryViewModel
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray

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
                visualTransformation = com.qdash.core.utils.ThousandsSeparatorTransformation(),
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
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            }
        }
    }
}
