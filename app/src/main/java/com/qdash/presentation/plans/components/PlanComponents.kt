package com.qdash.presentation.plans.components

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.core.utils.ThousandsSeparatorTransformation
import com.qdash.domain.model.FinancialPlan
import com.qdash.domain.model.FinancialPlanStatus
import com.qdash.domain.model.FinancialPlanType
import com.qdash.presentation.components.safeParseColor
import com.qdash.ui.designsystem.components.shimmerEffect
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TransferBlue
import java.util.Locale

fun planTypeLabel(type: FinancialPlanType) = when (type) {
    FinancialPlanType.MONTHLY_SPENDING -> "إنفاق شهري"
    FinancialPlanType.EMERGENCY_FUND -> "صندوق الطوارئ"
    FinancialPlanType.TRAVEL_SAVINGS -> "ادخار للسفر"
    FinancialPlanType.DEBT_PAYOFF -> "سداد الديون"
    FinancialPlanType.ROOM_SETUP -> "تجهيز مسكن"
    FinancialPlanType.FAMILY_BUDGET -> "ميزانية عائلية"
    FinancialPlanType.CUSTOM -> "خطة مخصصة"
}

fun planStatusLabel(status: FinancialPlanStatus) = when (status) {
    FinancialPlanStatus.ACTIVE -> "نشطة"
    FinancialPlanStatus.COMPLETED -> "مكتملة"
    FinancialPlanStatus.PAUSED -> "موقوفة مؤقتاً"
    FinancialPlanStatus.CANCELLED -> "ملغاة"
}

fun planStatusColor(status: FinancialPlanStatus) = when (status) {
    FinancialPlanStatus.ACTIVE -> IncomeGreen
    FinancialPlanStatus.COMPLETED -> TransferBlue
    FinancialPlanStatus.PAUSED -> SavingsAmber
    FinancialPlanStatus.CANCELLED -> ExpenseRed
}

fun parseHex(hex: String): Color {
    return safeParseColor(hex, TransferBlue)
}

fun formatAmount(amount: Double): String {
    return FormatterUtils.formatCurrency(amount)
}

@Composable
fun SummaryHeader(plans: List<FinancialPlan>) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val activeCount = plans.count { it.status == FinancialPlanStatus.ACTIVE }
    val completedCount = plans.count { it.status == FinancialPlanStatus.COMPLETED }
    val pausedCount = plans.count { it.status == FinancialPlanStatus.PAUSED }
    val totalTarget = plans.sumOf { it.targetAmount }
    val totalCurrent = plans.sumOf { it.currentAmount }
    val overallProgress = if (totalTarget > 0) (totalCurrent / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(primary, primary.copy(alpha = 0.85f))
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val statusSubtitle = buildString {
                        append("$activeCount نشطة")
                        if (pausedCount > 0) append(" • $pausedCount موقوفة")
                        if (completedCount > 0) append(" • $completedCount مكتملة")
                    }
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = onPrimary.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatAmount(totalCurrent),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = onPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "المنجز من أصل ${formatAmount(totalTarget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = onPrimary.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(overallProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = onPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "التقدم الكلي",
                        style = MaterialTheme.typography.labelSmall,
                        color = onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = onPrimary,
                trackColor = onPrimary.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun PlanCard(
    plan: FinancialPlan,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onUpdateProgress: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val planColor = parseHex(plan.color)
    val isPaused = plan.status == FinancialPlanStatus.PAUSED
    val isCancelled = plan.status == FinancialPlanStatus.CANCELLED

    val animatedProgress by animateFloatAsState(
        targetValue = plan.progressPercent,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "plan_progress_${plan.id}"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isPaused) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            else planColor.copy(alpha = 0.28f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(planColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = null,
                            tint = planColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = planTypeLabel(plan.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusTint = planStatusColor(plan.status)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusTint.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = planStatusLabel(plan.status),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusTint,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "خيارات الخطة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("تحديث التقدم") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onUpdateProgress()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تعديل الخطة") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isPaused) "استئناف الخطة" else "إيقاف مؤقت") },
                                leadingIcon = {
                                    Icon(
                                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = if (isPaused) IncomeGreen else SavingsAmber
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onPause()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            DropdownMenuItem(
                                text = { Text("حذف الخطة", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المنجز: ${formatAmount(plan.currentAmount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "الهدف: ${formatAmount(plan.targetAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (isPaused) SavingsAmber else planColor,
                    trackColor = (if (isPaused) SavingsAmber else planColor).copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isExceeded = plan.currentAmount > plan.targetAmount && plan.type == FinancialPlanType.MONTHLY_SPENDING
                    val isSurpassedGoal = plan.currentAmount > plan.targetAmount && plan.type != FinancialPlanType.MONTHLY_SPENDING
                    Text(
                        text = when {
                            isExceeded -> "المبلغ المتجاوز: ${formatAmount(plan.currentAmount - plan.targetAmount)}"
                            isSurpassedGoal -> "فائض عن الهدف: +${formatAmount(plan.currentAmount - plan.targetAmount)}"
                            else -> "المتبقي: ${formatAmount(plan.remainingAmount)}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            isExceeded -> MaterialTheme.colorScheme.error
                            isSurpassedGoal -> IncomeGreen
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = "${(animatedProgress * 100).toInt()}% مكتمل",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isExceeded) MaterialTheme.colorScheme.error else planColor
                    )
                }
            }

            plan.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isCancelled) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onUpdateProgress,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تحديث التقدم", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    if (isPaused) {
                        OutlinedButton(
                            onClick = onPause,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IncomeGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("استئناف", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlansState(onAddClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "لا توجد خطط مالية بعد",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "ابدأ بإنشاء خطة مالية لتتبع تقدمك نحو أهدافك",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("إنشاء خطة مالية", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

private fun formatEditableNumber(value: Double): String {
    if (value <= 0.0) return ""
    return if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, type: FinancialPlanType, target: Double, notes: String?, color: String) -> Unit,
    initialPlan: FinancialPlan? = null
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val isEditing = initialPlan != null

    var title by remember(initialPlan) { mutableStateOf(initialPlan?.title.orEmpty()) }
    var targetAmountStr by remember(initialPlan) {
        mutableStateOf(initialPlan?.let { formatEditableNumber(it.targetAmount) }.orEmpty())
    }
    var notes by remember(initialPlan) { mutableStateOf(initialPlan?.notes.orEmpty()) }
    var selectedType by remember(initialPlan) { mutableStateOf(initialPlan?.type ?: FinancialPlanType.CUSTOM) }
    var selectedColor by remember(initialPlan) { mutableStateOf(initialPlan?.color ?: "#6C63FF") }

    val planColors = listOf("#6C63FF", "#22C55E", "#EF4444", "#3B82F6", "#F59E0B", "#8B5CF6", "#14B8A6", "#EC4899")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "تعديل الخطة المالية" else "خطة مالية جديدة",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الخطة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetAmountStr,
                    onValueChange = { raw ->
                        val normalized = FormatterUtils.normalizeAmount(raw)
                        targetAmountStr = normalized.filter { c -> c.isDigit() || c == '.' }
                    },
                    label = { Text("المبلغ المستهدف (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "نوع الخطة:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    enumValues<FinancialPlanType>().toList().chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    label = { Text(planTypeLabel(type), fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                            if (row.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Text(
                    text = "اللون:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(planColors) { hex ->
                        val color = parseHex(hex)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = FormatterUtils.normalizeAmount(targetAmountStr).toDoubleOrNull() ?: 0.0
                    when {
                        title.isBlank() -> {
                            Toast.makeText(context, "يرجى إدخال اسم الخطة", Toast.LENGTH_SHORT).show()
                        }
                        amount <= 0 -> {
                            Toast.makeText(context, "يرجى إدخال مبلغ مستهدف صحيح", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            onConfirm(title.trim(), selectedType, amount, notes.ifBlank { null }, selectedColor)
                        }
                    }
                }
            ) {
                Text(
                    text = if (isEditing) "حفظ التعديلات" else "إنشاء",
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePlanProgressDialog(
    plan: FinancialPlan,
    onDismiss: () -> Unit,
    onConfirm: (newAmount: Double) -> Unit
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    // Mode: true = add incremental contribution, false = set total currentAmount directly
    var isAdditiveMode by remember { mutableStateOf(true) }
    var amountInput by remember { mutableStateOf("") }

    val parsedInput = FormatterUtils.normalizeAmount(amountInput).toDoubleOrNull() ?: 0.0
    val previewTotal = if (isAdditiveMode) {
        (plan.currentAmount + parsedInput).coerceAtLeast(0.0)
    } else {
        parsedInput.coerceAtLeast(0.0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تحديث تقدم: ${plan.title}",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "المنجز حالياً",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatAmount(plan.currentAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "الهدف الكلي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatAmount(plan.targetAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isAdditiveMode,
                        onClick = {
                            isAdditiveMode = true
                            amountInput = ""
                        },
                        label = { Text("إضافة مبلغ", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = !isAdditiveMode,
                        onClick = {
                            isAdditiveMode = false
                            amountInput = formatEditableNumber(plan.currentAmount)
                        },
                        label = { Text("تعيين الإجمالي", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { raw ->
                        val normalized = FormatterUtils.normalizeAmount(raw)
                        amountInput = normalized.filter { c -> c.isDigit() || c == '.' }
                    },
                    label = {
                        Text(if (isAdditiveMode) "المبلغ المضاف (دج)" else "إجمالي المبلغ المنجز (دج)")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (plan.targetAmount > 0) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val quickOptions = listOf(
                            "+10%" to (plan.targetAmount * 0.10),
                            "+25%" to (plan.targetAmount * 0.25),
                            "إكمال الهدف" to plan.remainingAmount
                        ).filter { it.second > 0 }

                        items(quickOptions) { (label, deltaAmount) ->
                            SuggestionChip(
                                onClick = {
                                    isAdditiveMode = true
                                    amountInput = formatEditableNumber(deltaAmount)
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (amountInput.isNotBlank()) {
                    Text(
                        text = "الرصيد المنجز بعد التحديث: ${formatAmount(previewTotal)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = FormatterUtils.normalizeAmount(amountInput).toDoubleOrNull()
                    if (value == null || (isAdditiveMode && value <= 0.0) || (!isAdditiveMode && value < 0.0)) {
                        Toast.makeText(context, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                    } else {
                        onConfirm(previewTotal)
                    }
                }
            ) {
                Text("حفظ التقدم", fontWeight = FontWeight.Bold, color = primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun SummaryHeaderSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(96.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect(RoundedCornerShape(22.dp))
        )
    }
}

@Composable
fun PlanCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shimmerEffect(CircleShape)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(16.dp)
                                .shimmerEffect(RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(10.dp)
                                .shimmerEffect(RoundedCornerShape(4.dp))
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 24.dp)
                        .shimmerEffect(RoundedCornerShape(20.dp))
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(10.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(10.dp)
                            .shimmerEffect(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
