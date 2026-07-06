package com.qdash.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.TransferBlue

@Composable
fun NotificationsTab(
    billingReminder: Boolean,
    salaryReminder: Boolean,
    budgetAlert: Boolean,
    weeklyReport: Boolean,
    goalProgress: Boolean,
    onBillingToggle: (Boolean) -> Unit,
    onSalaryToggle: (Boolean) -> Unit,
    onBudgetToggle: (Boolean) -> Unit,
    onWeeklyToggle: (Boolean) -> Unit,
    onGoalToggle: (Boolean) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSectionTitle("إشعارات الدخل والفواتير")

        SettingsItem(
            icon = Icons.Default.ReceiptLong,
            iconTint = SavingsAmber,
            title = "تذكير الفواتير",
            subtitle = "إشعار قبل موعد دفع الفواتير بـ 3 أيام",
            trailing = {
                Switch(
                    checked = billingReminder,
                    onCheckedChange = onBillingToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        SettingsItem(
            icon = Icons.Default.Payments,
            iconTint = IncomeGreen,
            title = "تذكير الراتب",
            subtitle = "إشعار عند توقع استلام الراتب الشهري",
            trailing = {
                Switch(
                    checked = salaryReminder,
                    onCheckedChange = onSalaryToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        SettingsSectionTitle("إشعارات الميزانية والأهداف")

        SettingsItem(
            icon = Icons.Default.Warning,
            iconTint = ExpenseRed,
            title = "تنبيهات تجاوز الميزانية",
            subtitle = "إشعار عند الاقتراب من حد الميزانية (80%)",
            trailing = {
                Switch(
                    checked = budgetAlert,
                    onCheckedChange = onBudgetToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        SettingsItem(
            icon = Icons.Default.Flag,
            iconTint = TransferBlue,
            title = "تقدم الأهداف الادخارية",
            subtitle = "إشعار عند اكتمال نسبة مئوية من الهدف",
            trailing = {
                Switch(
                    checked = goalProgress,
                    onCheckedChange = onGoalToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        SettingsSectionTitle("تقارير دورية")

        SettingsItem(
            icon = Icons.Default.BarChart,
            iconTint = Color(0xFF8B5CF6),
            title = "التقرير الأسبوعي",
            subtitle = "ملخص أسبوعي للإنفاق والادخار كل أحد",
            trailing = {
                Switch(
                    checked = weeklyReport,
                    onCheckedChange = onWeeklyToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}
