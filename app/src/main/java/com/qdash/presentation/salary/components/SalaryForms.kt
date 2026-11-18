package com.qdash.presentation.salary.components

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.core.utils.ThousandsSeparatorTransformation
import com.qdash.domain.model.AffectedObligation
import com.qdash.domain.model.DelaySeverity
import com.qdash.presentation.salary.SalaryUiState
import com.qdash.presentation.salary.SalaryViewModel
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSalaryForm(uiState: SalaryUiState, viewModel: SalaryViewModel) {
    val primary = MaterialTheme.colorScheme.primary
    val allDays = remember { (1..31).toList() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (uiState.editingId == null) "إضافة راتب جديد" else "تعديل إعدادات الراتب",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("اسم الراتب") },
                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("قيمة الراتب الشهري (د.ج)") },
                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
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
                            color = primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Show ALL accounts in LazyRow (not .take(3)!)
            Text(
                text = "يُودع في حساب:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.accounts, key = { it.id }) { account ->
                    val isSelected = uiState.selectedAccountId == account.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.onAccountSelected(account.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = account.name,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Clean Day Stepper + Selectable Day Chips (replaces finicky 1..31 Slider!)
            Text(
                text = "يوم استلام الراتب من كل شهر:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.onDayOfMonthChange((uiState.dayOfMonth - 1).coerceAtLeast(1)) },
                        enabled = uiState.dayOfMonth > 1
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "إنقاص يوم", tint = primary)
                    }

                    Text(
                        text = "اليوم ${FormatterUtils.convertNumerals(uiState.dayOfMonth.toString())} من الشهر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primary
                    )

                    IconButton(
                        onClick = { viewModel.onDayOfMonthChange((uiState.dayOfMonth + 1).coerceAtMost(31)) },
                        enabled = uiState.dayOfMonth < 31
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "زيادة يوم", tint = primary)
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allDays, key = { it }) { day ->
                    val isSelected = uiState.dayOfMonth == day
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { viewModel.onDayOfMonthChange(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = FormatterUtils.convertNumerals(day.toString()),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (uiState.dayOfMonth >= 29) {
                Text(
                    text = "ملاحظة: في الأشهر القصيرة (مثل فيفري) سيتم ضبط الموعد تلقائياً على آخر يوم من الشهر.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AppButton(
                onClick = { viewModel.saveSalary() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY,
                shape = ShapeTokens.Lg
            ) {
                Text("حفظ إعدادات الراتب", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelaySalaryForm(uiState: SalaryUiState, viewModel: SalaryViewModel) {
    val primary = MaterialTheme.colorScheme.primary
    if (uiState.overview?.salary == null) return

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (uiState.isEditMode) "تعديل تأجيل الراتب" else "تأجيل راتب الشهر الحالي",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            OutlinedTextField(
                value = uiState.delayDaysInput,
                onValueChange = viewModel::onDelayDaysChange,
                label = { Text("عدد أيام التأجيل") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.isAnalyzingDelay) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primary)
                }
            } else {
                uiState.delayImpact?.let { impact ->
                    val severityColor = when (impact.severity) {
                        DelaySeverity.LOW -> IncomeGreen
                        DelaySeverity.MEDIUM -> SavingsAmber
                        DelaySeverity.HIGH -> SavingsAmber
                        DelaySeverity.CRITICAL -> ExpenseRed
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("التاريخ المتوقع الجديد:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FormatterUtils.formatDate(impact.newDate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("مؤشر أثر التأجيل:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = severityColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = when (impact.severity) {
                                        DelaySeverity.LOW -> "أثر منخفض"
                                        DelaySeverity.MEDIUM -> "أثر متوسط"
                                        DelaySeverity.HIGH -> "أثر مرتفع"
                                        DelaySeverity.CRITICAL -> "أثر حرج جداً"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = severityColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            val progressFraction = (impact.severityScore / 100f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction)
                                    .background(severityColor)
                            )
                        }
                    }

                    Text(
                        text = "الالتزامات المتأثرة خلال فترة التأجيل (${impact.affectedCount})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (impact.affectedObligations.isEmpty()) {
                        Text(
                            text = "لا توجد التزامات مالية متأثرة في هذه الفترة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
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

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { viewModel.confirmSalaryDelay() },
                enabled = uiState.delayImpact != null && !uiState.isConfirmingDelay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (uiState.delayImpact?.severity) {
                        DelaySeverity.CRITICAL -> ExpenseRed
                        DelaySeverity.HIGH -> SavingsAmber
                        else -> primary
                    },
                    contentColor = Color.White
                )
            ) {
                if (uiState.isConfirmingDelay) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (uiState.isEditMode) "تأكيد تعديل التأجيل" else "تأكيد تأجيل الراتب",
                        fontWeight = FontWeight.Bold
                    )
                }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (obs.type == "DEBT") "التزام دين" else "اشتراك دوري",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
