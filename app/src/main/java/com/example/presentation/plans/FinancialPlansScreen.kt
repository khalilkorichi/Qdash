package com.example.presentation.plans

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.UnifiedScreenHeader
import com.example.domain.model.FinancialPlan
import com.example.domain.model.FinancialPlanStatus
import com.example.domain.model.FinancialPlanType
import com.example.ui.theme.*
import com.example.ui.designsystem.components.*
import java.text.NumberFormat
import java.util.Locale

private fun planTypeLabel(type: FinancialPlanType) = when (type) {
    FinancialPlanType.MONTHLY_SPENDING -> "إنفاق شهري"
    FinancialPlanType.EMERGENCY_FUND -> "صندوق الطوارئ"
    FinancialPlanType.TRAVEL_SAVINGS -> "ادخار للسفر"
    FinancialPlanType.DEBT_PAYOFF -> "سداد الديون"
    FinancialPlanType.ROOM_SETUP -> "تجهيز مسكن"
    FinancialPlanType.FAMILY_BUDGET -> "ميزانية عائلية"
    FinancialPlanType.CUSTOM -> "خطة مخصصة"
}

private fun planStatusLabel(status: FinancialPlanStatus) = when (status) {
    FinancialPlanStatus.ACTIVE -> "نشط"
    FinancialPlanStatus.COMPLETED -> "مكتمل ✅"
    FinancialPlanStatus.PAUSED -> "موقوف ⏸️"
    FinancialPlanStatus.CANCELLED -> "ملغى"
}

private fun planStatusColor(status: FinancialPlanStatus) = when (status) {
    FinancialPlanStatus.ACTIVE -> Color(0xFF22C55E)
    FinancialPlanStatus.COMPLETED -> Color(0xFF3B82F6)
    FinancialPlanStatus.PAUSED -> Color(0xFFF59E0B)
    FinancialPlanStatus.CANCELLED -> Color(0xFFEF4444)
}

private fun parseHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (e: Exception) { Color(0xFF6C63FF) }
}

private fun formatAmount(amount: Double): String {
    return com.example.core.utils.FormatterUtils.formatCurrency(amount)
}

@Composable
fun FinancialPlansScreen(
    viewModel: FinancialPlansViewModel,
    onBack: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            UnifiedScreenHeader(
                title = "الخطط المالية",
                subtitle = "حدد ميزانيتك التقديرية وحقق أهدافك بكفاءة",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة خطة", tint = Primary)
                    }
                }
            )
            if (uiState.isLoading) {
                SummaryHeaderSkeleton()
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(3) {
                        PlanCardSkeleton()
                    }
                }
            } else {
                // Summary header
                if (uiState.plans.isNotEmpty()) {
                    SummaryHeader(plans = uiState.plans)
                }

                if (uiState.plans.isEmpty()) {
                    EmptyPlansState(onAddClick = { showAddDialog = true })
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.plans, key = { it.id }) { plan ->
                            PlanCard(
                                plan = plan,
                                onDelete = { viewModel.deletePlan(plan) },
                                onPause = {
                                    val newStatus = if (plan.status == FinancialPlanStatus.PAUSED)
                                        FinancialPlanStatus.ACTIVE
                                    else FinancialPlanStatus.PAUSED
                                    viewModel.updateStatus(plan.id, newStatus)
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlanDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, type, target, notes, color ->
                viewModel.addPlan(title, type, target, notes, color)
                showAddDialog = false
            }
        )
    }
}
@Composable
private fun SummaryHeader(plans: List<FinancialPlan>) {
    val Primary = MaterialTheme.colorScheme.primary
    val totalTarget = plans.sumOf { it.targetAmount }
    val totalCurrent = plans.sumOf { it.currentAmount }
    val overallProgress = if (totalTarget > 0) (totalCurrent / totalTarget).toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Primary, Color(0xFF8B5CF6))),
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${plans.size} خطة نشطة",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatAmount(totalTarget),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "مجموع الأهداف",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(overallProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "التقدم الكلي",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: FinancialPlan,
    onDelete: () -> Unit,
    onPause: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val planColor = parseHex(plan.color)

    val animatedProgress by animateFloatAsState(
        targetValue = plan.progressPercent,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "plan_progress_${plan.id}"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, planColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and status row
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
                            .background(planColor.copy(alpha = 0.1f)),
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
                            color = TextGray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = planStatusColor(plan.status).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = planStatusLabel(plan.status),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = planStatusColor(plan.status),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, null, tint = TextGray, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (plan.status == FinancialPlanStatus.PAUSED) "استئناف" else "إيقاف مؤقت") },
                                onClick = { showMenu = false; onPause() }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف الخطة", color = ExpenseRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = ExpenseRed) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress bar and amounts
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
                        color = TextGray
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = planColor,
                    trackColor = planColor.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isExceeded = plan.currentAmount > plan.targetAmount
                    Text(
                        text = if (isExceeded) {
                            "المبلغ المتجاوز: ${formatAmount(plan.currentAmount - plan.targetAmount)}"
                        } else {
                            "متبقي: ${formatAmount(plan.remainingAmount)}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isExceeded) ExpenseRed else TextGray
                    )

                    Text(
                        text = "${(animatedProgress * 100).toInt()}% مكتمل",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isExceeded) ExpenseRed else planColor
                    )
                }
            }

            // Notes if any
            plan.notes?.let { notes ->
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyPlansState(onAddClick: () -> Unit) {
    val Primary = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = Primary,
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
                color = TextGray
            )
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("إنشاء خطة مالية")
            }
        }
    }
}

@Composable
private fun AddPlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, type: FinancialPlanType, target: Double, notes: String?, color: String) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    var title by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(FinancialPlanType.CUSTOM) }
    var selectedColor by remember { mutableStateOf("#6C63FF") }

    val planColors = listOf("#6C63FF", "#22C55E", "#EF4444", "#3B82F6", "#F59E0B", "#8B5CF6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("خطة مالية جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الخطة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetAmountStr,
                    onValueChange = { targetAmountStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("المبلغ المستهدف (دج)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type selector
                Text("نوع الخطة:", style = MaterialTheme.typography.labelMedium, color = TextGray)
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
                                        selectedContainerColor = Primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            if (row.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Color picker
                Text("اللون:", style = MaterialTheme.typography.labelMedium, color = TextGray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    planColors.forEach { hex ->
                        val color = parseHex(hex)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                        ) {
                            if (selectedColor == hex) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = targetAmountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onConfirm(title, selectedType, amount, notes.ifBlank { null }, selectedColor)
                    }
                }
            ) {
                Text("إنشاء", fontWeight = FontWeight.Bold, color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun SummaryHeaderSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(84.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect(RoundedCornerShape(16.dp))
        )
    }
}

@Composable
private fun PlanCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title row outline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shimmerEffect(CircleShape)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Title
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(16.dp)
                                .shimmerEffect(RoundedCornerShape(4.dp))
                        )
                        // Type
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(10.dp)
                                .shimmerEffect(RoundedCornerShape(4.dp))
                        )
                    }
                }
                // Status badge outline
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 24.dp)
                        .shimmerEffect(RoundedCornerShape(20.dp))
                )
            }
            
            Spacer(Modifier.height(20.dp))

            // Progress bar outline
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

