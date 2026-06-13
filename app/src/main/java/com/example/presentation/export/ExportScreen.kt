package com.example.presentation.export

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.FinTrackTopBar
import com.example.domain.model.Account
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // User choices
    var selectedReportType by remember { mutableStateOf("MONTHLY") }
    val selectedAccounts = remember { mutableStateListOf<Long>() }
    var includeDebtSection by remember { mutableStateOf(true) }
    var includeSavingsSection by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.accounts) {
        if (uiState.accounts.isNotEmpty() && selectedAccounts.isEmpty()) {
            selectedAccounts.addAll(uiState.accounts.map { it.id })
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("export_screen"),
        topBar = {
            FinTrackTopBar(title = "تصدير التقارير المالية وكشوف الحسابات")
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.exportResult != null) {
                // SUCCESS EXPORT TRANSITION VIEW
                val result = uiState.exportResult!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(IncomeGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TaskAlt, null, tint = IncomeGreen, modifier = Modifier.size(42.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("تم توليد تقرير الـ PDF بنجاح!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تم جرد الحسابات وحفظ النسخة الرسمية للتقرير المالي المختار في مدير الملفات بالبرنامج.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = TextGray
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, tint = TransferBlue)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(result.fileName, fontWeight = FontWeight.Bold)
                            Text(result.fileUri ?: "مسار داخلي مؤمن", fontSize = 11.sp, color = TextGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    val context = LocalContext.current
                    Button(
                        onClick = {
                            result.fileUri?.let { com.example.core.utils.FileUtils.openPdfFile(context, it) }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فتح التقرير مباشرة")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { viewModel.clearResult() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, TransferBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TransferBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء تقرير مالي آخر")
                    }
                }
            } else if (uiState.isLoading) {
                // LOADING PROGRESS RADIAL DIALOG
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = TransferBlue, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "جاري تحضير ملف PDF الرسمي...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.progressText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // REPORT CONFIGURATION HUB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    // HERO EXPLAINER CARD
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = TransferBlue.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = TransferBlue, modifier = Modifier.size(32.dp))
                                Column {
                                    Text("كشوف وتقارير رسمية فائقة التنسيق", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TransferBlue)
                                    Text("قم بتوليد وتصدير ملفات PDF مفصلة لإحصاءاتك، مصاريفك، مدخراتك والتزامات ديونك لمشاركتها أو أرشفتها.", fontSize = 11.sp, color = TextGray)
                                }
                            }
                        }
                    }

                    // REPORT FORM TYPES SELECTOR
                    item {
                        Text("1. نوع التقرير المطلوب تصديره:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // First 4 square items (2x2 grid)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ReportSquareCard(
                                        title = "التقرير الشهري",
                                        desc = "حسابات ومقاصة الشهر الجاري",
                                        icon = Icons.Default.CalendarMonth,
                                        isSelected = selectedReportType == "MONTHLY",
                                        onClick = { selectedReportType = "MONTHLY" }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    ReportSquareCard(
                                        title = "الكشف السنوي",
                                        desc = "ميزانية الأداء السنوية",
                                        icon = Icons.Default.Analytics,
                                        isSelected = selectedReportType == "ANNUAL",
                                        onClick = { selectedReportType = "ANNUAL" }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ReportSquareCard(
                                        title = "سجل المدخرات",
                                        desc = "حركة المشاريع والحصالة",
                                        icon = Icons.Default.Savings,
                                        isSelected = selectedReportType == "SAVINGS",
                                        onClick = { selectedReportType = "SAVINGS" }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    ReportSquareCard(
                                        title = "بيان الديون",
                                        desc = "خطة وجدولة تسوية الديون",
                                        icon = Icons.Default.ReceiptLong,
                                        isSelected = selectedReportType == "DEBTS",
                                        onClick = { selectedReportType = "DEBTS" }
                                    )
                                }
                            }

                            // Item 5: Rectangular full-width card
                            ReportRectangularCard(
                                title = "دفتر التحليل والذكاء المالي",
                                desc = "تحليل التوزيع والسلبيات والمشورة الذكية التلقائية",
                                icon = Icons.Default.AutoAwesome,
                                isSelected = selectedReportType == "ANALYTICS",
                                onClick = { selectedReportType = "ANALYTICS" }
                            )
                        }
                    }

                    // ASSOCIATED ACCOUNT SELECTOR CHECKLISTS
                    item {
                        Text("2. تصفية وحصر الحسابات المالية المشمولة:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.accounts.isEmpty()) {
                                Text(
                                    "لا توجد حسابات مسجلة للحصر",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                uiState.accounts.forEach { account ->
                                    val isChecked = selectedAccounts.contains(account.id)
                                    val icon = when (account.type) {
                                        com.example.domain.model.AccountType.BARIDIMOB -> Icons.Default.Smartphone
                                        com.example.domain.model.AccountType.CCP -> Icons.Default.AccountBalance
                                        com.example.domain.model.AccountType.CASH -> Icons.Default.Payments
                                        com.example.domain.model.AccountType.SAVINGS -> Icons.Default.Savings
                                        else -> Icons.Default.CreditCard
                                    }
                                    val tint = when (account.type) {
                                        com.example.domain.model.AccountType.BARIDIMOB -> Color(0xFF005CA9)
                                        com.example.domain.model.AccountType.CCP -> Color(0xFFF59E0B)
                                        com.example.domain.model.AccountType.CASH -> Color(0xFF22C55E)
                                        com.example.domain.model.AccountType.SAVINGS -> Color(0xFF3B82F6)
                                        else -> TransferBlue
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isChecked) TransferBlue.copy(alpha = 0.04f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                if (isChecked) selectedAccounts.remove(account.id)
                                                else selectedAccounts.add(account.id)
                                            }
                                            .border(
                                                width = 1.dp,
                                                color = if (isChecked) TransferBlue.copy(alpha = 0.15f)
                                                else Color.Transparent,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isChecked) TransferBlue else Color.Transparent
                                                    )
                                                    .border(
                                                        width = if (isChecked) 0.dp else 2.dp,
                                                        color = if (isChecked) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isChecked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = account.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isChecked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = "الرصيد: ${account.balance.toInt()} د.ج",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isChecked) TransferBlue else TextGray
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(tint.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = tint,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ADD-ON DATA SECTIONS
                    item {
                        Text("3. إضافات وأقسام اختيارية للتقرير:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Savings option row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (includeSavingsSection) TransferBlue.copy(alpha = 0.03f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (includeSavingsSection) TransferBlue.copy(alpha = 0.1f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { includeSavingsSection = !includeSavingsSection }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Switch(
                                        checked = includeSavingsSection,
                                        onCheckedChange = { includeSavingsSection = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = TransferBlue,
                                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )

                                    Column {
                                        Text(
                                            text = "تضمين سجل المشروعات والادخار",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "توليد نسب تحقيق الأهداف وتاريخ حركة المدخرات",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextGray
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TransferBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = TransferBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Debts option row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (includeDebtSection) ExpenseRed.copy(alpha = 0.03f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (includeDebtSection) ExpenseRed.copy(alpha = 0.1f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { includeDebtSection = !includeDebtSection }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Switch(
                                        checked = includeDebtSection,
                                        onCheckedChange = { includeDebtSection = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = ExpenseRed,
                                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )

                                    Column {
                                        Text(
                                            text = "تضمين جداول وتفاصيل استرداد الديون",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "إدراج جدول الأولوية والمبالغ القائمة وجدولة كرة الثلج والمتبقي",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextGray
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ExpenseRed.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // EXPORT GEN BUTTON
                    item {
                        Button(
                            onClick = {
                                viewModel.generateReport(
                                    reportType = selectedReportType,
                                    accounts = selectedAccounts.toList(),
                                    includeDebt = includeDebtSection,
                                    includeSavings = includeSavingsSection
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TransferBlue),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedAccounts.isNotEmpty()
                        ) {
                            Icon(Icons.Default.FileDownload, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("توليد وتصدير ملف الـ PDF", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Error display if validation / generation fails
                    uiState.error?.let { err ->
                        item {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSquareCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TransferBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TransferBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(TransferBlue, CircleShape)
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) TransferBlue else TextGray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    textAlign = TextAlign.Center,
                    color = TextGray,
                    maxLines = 2,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ReportRectangularCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TransferBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TransferBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isSelected) TransferBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) TransferBlue else TextGray,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(TransferBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}
