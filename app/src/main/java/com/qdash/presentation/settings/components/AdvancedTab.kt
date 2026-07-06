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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TextGray

@Composable
fun AdvancedTab() {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsSectionTitle("الخصوصية والبيانات")

        SettingsNavItem(
            icon = Icons.Default.DeleteForever,
            iconTint = ExpenseRed,
            title = "مسح كل البيانات",
            subtitle = "حذف جميع المعاملات والحسابات والفئات نهائياً",
            onClick = { /* confirm dialog */ }
        )

        SettingsNavItem(
            icon = Icons.Default.RestartAlt,
            iconTint = SavingsAmber,
            title = "إعادة الضبط الكامل",
            subtitle = "إرجاع التطبيق إلى الإعدادات الافتراضية الأولى",
            onClick = { /* confirm dialog */ }
        )

        SettingsSectionTitle("التشخيص والتقارير")

        SettingsItem(
            icon = Icons.Default.Storage,
            iconTint = Color(0xFF8B5CF6),
            title = "حجم قاعدة البيانات",
            subtitle = "يتم حساب هذا الحجم محلياً على الجهاز",
            trailing = {
                Text("محلي", style = MaterialTheme.typography.labelSmall, color = TextGray)
            }
        )

        SettingsItem(
            icon = Icons.Default.BugReport,
            iconTint = TextGray,
            title = "وضع التصحيح",
            subtitle = "عرض السجلات التقنية وتشخيص الأخطاء",
            trailing = {
                Text("معطّل", style = MaterialTheme.typography.labelSmall, color = TextGray)
            }
        )

        SettingsSectionTitle("معلومات التطبيق")

        SettingsItem(
            icon = Icons.Default.Info,
            iconTint = Primary,
            title = "الإصدار الحالي",
            subtitle = "قداشّ — نسخة الإنتاج",
            trailing = {
                Text("v1.0.0", style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.Bold)
            }
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}
