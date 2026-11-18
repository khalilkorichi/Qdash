package com.qdash.presentation.salary.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.EnvelopeType
import com.qdash.domain.model.SalaryEnvelope
import com.qdash.presentation.components.safeParseColor
import com.qdash.presentation.salary.SalaryUiState
import com.qdash.presentation.salary.SalaryViewModel
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray
import com.qdash.ui.theme.TransferBlue

@Composable
fun SalaryOverviewCard(
    salary: com.qdash.domain.model.IncomeSource,
    accountName: String? = null,
    onEdit: () -> Unit,
    onDepositNowClick: () -> Unit = {},
    onDelayClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(IncomeGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = IncomeGreen)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = salary.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val accSuffix = if (!accountName.isNullOrBlank()) " • $accountName" else ""
                        Text(
                            text = "يوم الصرف: ${FormatterUtils.convertNumerals(salary.dayOfMonth.toString())} من كل شهر$accSuffix",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = primary)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent On-Time Salary Deposit CTA
            AppButton(
                onClick = onDepositNowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.SUCCESS,
                shape = ShapeTokens.Lg,
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(20.dp)) }
            ) {
                Text(
                    text = "استلام وإيداع الراتب الآن",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الدفعة القادمة المتوقعة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatterUtils.formatDate(salary.nextExpectedDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = primary
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأجيل الصرف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    val primary = MaterialTheme.colorScheme.primary

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

    // Use accessible semantic tokens instead of unreadable pale yellow 0xFFFFC107 on light surfaces
    val severityColor = when {
        delay.severityScore <= 20 -> IncomeGreen
        delay.severityScore <= 45 -> SavingsAmber
        delay.severityScore <= 70 -> SavingsAmber
        else -> ExpenseRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                            .size(46.dp)
                            .background(severityColor.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
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
                            .background(severityColor.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "الأثر: ${delay.severityScore}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                    }

                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = primary)
                    }
                    IconButton(onClick = { showConfirmDelete = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "إلغاء", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التاريخ الأصلي",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatterUtils.formatShortDate(delay.newDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = primary
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
    val primary = MaterialTheme.colorScheme.primary
    val salary = uiState.overview?.salary
    val salaryAmount = salary?.amount ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "التوزيع التلقائي للراتب (الأظرف)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "قاعدة 50/30/20 القابلة للتخصيص",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        label = "احتياجات أساسية",
                        percentage = uiState.needsPercentage,
                        color = IncomeGreen,
                        amount = salaryAmount * uiState.needsPercentage / 100.0,
                        onValueChange = { viewModel.updateDistributionPercentage(EnvelopeType.NEEDS, it) },
                        onValueChangeFinished = { viewModel.commitDistributionPercentages() }
                    )
                    PercentageSliderRow(
                        label = "رغبات وترفيه",
                        percentage = uiState.wantsPercentage,
                        color = SavingsAmber,
                        amount = salaryAmount * uiState.wantsPercentage / 100.0,
                        onValueChange = { viewModel.updateDistributionPercentage(EnvelopeType.WANTS, it) },
                        onValueChangeFinished = { viewModel.commitDistributionPercentages() }
                    )
                    PercentageSliderRow(
                        label = "ادخار واستثمار",
                        percentage = uiState.savingsPercentage,
                        color = TransferBlue,
                        amount = salaryAmount * uiState.savingsPercentage / 100.0,
                        onValueChange = { viewModel.updateDistributionPercentage(EnvelopeType.SAVINGS, it) },
                        onValueChangeFinished = { viewModel.commitDistributionPercentages() }
                    )

                    if (uiState.envelopes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الأظرف المالية وربط الفئات",
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
                                color = primary,
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
                        .background(IncomeGreen)
                )
            }
            if (wantsPercentage > 0) {
                Box(
                    modifier = Modifier
                        .weight(wantsPercentage.toFloat())
                        .fillMaxHeight()
                        .background(SavingsAmber)
                )
            }
            if (savingsPercentage > 0) {
                Box(
                    modifier = Modifier
                        .weight(savingsPercentage.toFloat())
                        .fillMaxHeight()
                        .background(TransferBlue)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DistributionLegendItem("احتياجات", needsPercentage, IncomeGreen)
            DistributionLegendItem("رغبات", wantsPercentage, SavingsAmber)
            DistributionLegendItem("ادخار", savingsPercentage, TransferBlue)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val envelopeColor = safeParseColor(envelope.color, MaterialTheme.colorScheme.primary)
    val linkedCategories = remember(categories, envelope.linkedCategoryIds) {
        categories.filter { it.id in envelope.linkedCategoryIds }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        Icon(
                            imageVector = when (envelope.type) {
                                EnvelopeType.NEEDS -> Icons.Default.Home
                                EnvelopeType.WANTS -> Icons.Default.SportsEsports
                                EnvelopeType.SAVINGS -> Icons.Default.Savings
                            },
                            contentDescription = null,
                            tint = envelopeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = envelope.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${envelope.percentage}% من الراتب",
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            when {
                                progress > 0.9f -> ExpenseRed
                                progress > 0.7f -> SavingsAmber
                                else -> envelopeColor
                            }
                        )
                )
            }

            if (linkedCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    linkedCategories.take(3).forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(envelopeColor.copy(alpha = 0.12f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = envelopeColor
                            )
                        }
                    }
                    if (linkedCategories.size > 3) {
                        Text(
                            text = "+${linkedCategories.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 1: Link Categories Button (Previously unwired!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onLinkCategory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = envelopeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (linkedCategories.isEmpty()) "ربط فئات المصاريف"
                            else "الفئات المرتبطة (${FormatterUtils.convertNumerals(linkedCategories.size.toString())})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                // Action 2: Link Sub-Account Dropdown
                var showAccountDropdown by remember { mutableStateOf(false) }
                val linkedAccount = accounts.find { it.id == envelope.linkedAccountId }

                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { showAccountDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (linkedAccount != null) envelopeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = linkedAccount?.name ?: "ربط محفظة فرعية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (linkedAccount != null) envelopeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (linkedAccount != null) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showAccountDropdown,
                        onDismissRequest = { showAccountDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("إلغاء ربط المحفظة", color = ExpenseRed) },
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
}
