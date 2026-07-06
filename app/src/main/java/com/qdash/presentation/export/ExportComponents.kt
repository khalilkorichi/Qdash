package com.qdash.presentation.export

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.ExportResult
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.TextGray
import com.qdash.ui.theme.TransferBlue

@Composable
fun ReportSquareCard(
    title: String,
    desc: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
fun ReportRectangularCard(
    title: String,
    desc: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}

@Composable
fun ExportSuccessView(
    result: ExportResult,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
                result.fileUri?.let { com.qdash.core.utils.FileUtils.openPdfFile(context, it) }
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
            onClick = onClearResult,
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
}

@Composable
fun ExportLoadingView(
    progressText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            text = progressText,
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ReportTypeSelectorSection(
    selectedReportType: String,
    onReportTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = TransferBlue,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "كشوف وتقارير رسمية فائقة التنسيق",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TransferBlue
                    )
                    Text(
                        text = "قم بتوليد وتصدير ملفات PDF مفصلة لإحصاءاتك، مصاريفك، مدخراتك والتزامات ديونك لمشاركتها أو أرشفتها.",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("1. نوع التقرير المطلوب تصديره:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        onClick = { onReportTypeSelected("MONTHLY") }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ReportSquareCard(
                        title = "الكشف السنوي",
                        desc = "ميزانية الأداء السنوية",
                        icon = Icons.Default.Analytics,
                        isSelected = selectedReportType == "ANNUAL",
                        onClick = { onReportTypeSelected("ANNUAL") }
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
                        onClick = { onReportTypeSelected("SAVINGS") }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ReportSquareCard(
                        title = "بيان الديون",
                        desc = "خطة وجدولة تسوية الديون",
                        icon = Icons.Default.ReceiptLong,
                        isSelected = selectedReportType == "DEBTS",
                        onClick = { onReportTypeSelected("DEBTS") }
                    )
                }
            }

            ReportRectangularCard(
                title = "دفتر التحليل والذكاء المالي",
                desc = "تحليل التوزيع والسلبيات والمشورة الذكية التلقائية",
                icon = Icons.Default.AutoAwesome,
                isSelected = selectedReportType == "ANALYTICS",
                onClick = { onReportTypeSelected("ANALYTICS") }
            )
        }
    }
}

@Composable
fun AccountSelectorSection(
    accounts: List<com.qdash.domain.model.Account>,
    selectedAccounts: List<Long>,
    onToggleAccount: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("2. تصفية وحصر الحسابات المالية المشمولة:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        if (accounts.isEmpty()) {
            Text(
                "لا توجد حسابات مسجلة للحصر",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            accounts.forEach { account ->
                val isChecked = selectedAccounts.contains(account.id)
                val icon = when (account.type) {
                    com.qdash.domain.model.AccountType.BARIDIMOB -> Icons.Default.Smartphone
                    com.qdash.domain.model.AccountType.CCP -> Icons.Default.AccountBalance
                    com.qdash.domain.model.AccountType.CASH -> Icons.Default.Payments
                    com.qdash.domain.model.AccountType.SAVINGS -> Icons.Default.Savings
                    else -> Icons.Default.CreditCard
                }
                val tint = when (account.type) {
                    com.qdash.domain.model.AccountType.BARIDIMOB -> Color(0xFF005CA9)
                    com.qdash.domain.model.AccountType.CCP -> Color(0xFFF59E0B)
                    com.qdash.domain.model.AccountType.CASH -> Color(0xFF22C55E)
                    com.qdash.domain.model.AccountType.SAVINGS -> Color(0xFF3B82F6)
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
                        .clickable { onToggleAccount(account.id) }
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

@Composable
fun ExportOptionsSection(
    includeSavingsSection: Boolean,
    onIncludeSavingsChanged: (Boolean) -> Unit,
    includeDebtSection: Boolean,
    onIncludeDebtChanged: (Boolean) -> Unit,
    isExportEnabled: Boolean,
    onExportClick: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("3. إضافات وأقسام اختيارية للتقرير:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    .clickable { onIncludeSavingsChanged(!includeSavingsSection) }
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
                        onCheckedChange = onIncludeSavingsChanged,
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
                    .clickable { onIncludeDebtChanged(!includeDebtSection) }
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
                        onCheckedChange = onIncludeDebtChanged,
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

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onExportClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TransferBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = isExportEnabled
        ) {
            Icon(Icons.Default.FileDownload, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("توليد وتصدير ملف الـ PDF", fontWeight = FontWeight.Bold)
        }

        error?.let { err ->
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
