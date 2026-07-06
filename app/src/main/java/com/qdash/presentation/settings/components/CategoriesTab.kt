package com.qdash.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.SavingsAmber
import com.qdash.ui.theme.TransferBlue

@Composable
fun CategoriesTab(onNavigateToCategories: () -> Unit) {
    val Primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionTitle("إدارة فئات المعاملات")

        // Hero card to navigate
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            variant = CardVariant.SOLID,
            shape = ShapeTokens.Xl,
            onClick = onNavigateToCategories,
            backgroundColor = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(SavingsAmber.copy(alpha = 0.8f), Color(0xFFEC4899).copy(alpha = 0.6f))
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "إدارة الفئات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "أضف وعدّل وحذف فئات المصاريف والدخل وفئاتها الفرعية",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("فتح إدارة الفئات", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        SettingsSectionTitle("معلومات الفئات")

        SettingsItem(
            icon = Icons.Default.Info,
            iconTint = TransferBlue,
            title = "الفئات النظامية",
            subtitle = "لا يمكن حذف الفئات المدمجة مع التطبيق، لكن يمكن تعديلها",
            trailing = {}
        )

        SettingsItem(
            icon = Icons.Default.AccountTree,
            iconTint = Primary,
            title = "الفئات الفرعية",
            subtitle = "يمكنك إنشاء فئات فرعية داخل كل فئة رئيسية للتصنيف الدقيق",
            trailing = {}
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}
