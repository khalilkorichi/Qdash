package com.example.presentation.simulator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.FinTrackTopBar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentSimulatorEntryScreen(
    onSelectDocType: (DocumentType) -> Unit,
    onManageProfiles: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val Secondary = MaterialTheme.colorScheme.secondary

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FinTrackTopBar(
                title = "المساعد البريدي المالي",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "محاكي الوثائق والتعلم البريدي",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "املأ صكوكك البريدية وحوالات الدفع بكل ثقة! يساعدك هذا القسم على تجنب الأخطاء الشائعة وحساب المبالغ وتحويل الأرقام إلى حروف.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            item {
                SimulatorSelectionCard(
                    title = "محاكي الصك البريدي (Chèque)",
                    description = "تدرّب على الكتابة الصحيحة للمبالغ بالحروف (التفقيط) وتحديد الحساب البريدي الجاري والمفتاح والمستفيد لتفادي رفض المعاملة.",
                    icon = Icons.Default.Description,
                    gradientColors = listOf(Primary.copy(alpha = 0.85f), Primary.copy(alpha = 0.6f)),
                    onClick = { onSelectDocType(DocumentType.CHEQUE) }
                )
            }

            item {
                SimulatorSelectionCard(
                    title = "محاكي استمارة الحوالة (SFP 01)",
                    description = "محاكاة تعبئة الاستمارة الصفراء الخاصة بعمليات الدفع (صب الأموال)، السحب، أو تحويل الأرصدة إلى حسابات أخرى.",
                    icon = Icons.Default.ReceiptLong,
                    gradientColors = listOf(Secondary.copy(alpha = 0.85f), Secondary.copy(alpha = 0.6f)),
                    onClick = { onSelectDocType(DocumentType.SFP01) }
                )
            }

            item {
                SimulatorSelectionCard(
                    title = "إدارة الحسابات البريدية المحفوظة",
                    description = "احفظ أرقام حساباتك البريدية الجارية (CCP) ومفاتيحها مع الأسماء والعناوين، لتعبئتها فوراً في المحاكي بلمسة واحدة.",
                    icon = Icons.Default.AccountBox,
                    gradientColors = listOf(Color(0xFF607D8B), Color(0xFF90A4AE)),
                    onClick = onManageProfiles
                )
            }
        }
    }
}

@Composable
fun SimulatorSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
