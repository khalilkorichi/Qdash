package com.qdash.presentation.salary.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.EnvelopeType
import com.qdash.domain.model.SalaryEnvelope
import com.qdash.presentation.salary.SalaryUiState
import com.qdash.presentation.salary.SalaryViewModel
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray

@Composable
fun SalaryOverviewCard(
    salary: com.qdash.domain.model.IncomeSource,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
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
    delay: com.qdash.domain.model.SalaryDelay,
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
                    DistributionBar(
                        needsPercentage = uiState.needsPercentage,
                        wantsPercentage = uiState.wantsPercentage,
                        savingsPercentage = uiState.savingsPercentage
                    )

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
fun DistributionBar(
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
fun DistributionLegendItem(label: String, percentage: Int, color: Color) {
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
fun PercentageSliderRow(
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
fun EnvelopeCard(
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
