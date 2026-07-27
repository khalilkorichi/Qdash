package com.qdash.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.qdash.ui.theme.TextGray
import com.qdash.ui.theme.Primary
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    viewModel: AccountManagementViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    var showConfirmDialog by remember { mutableStateOf(false) }
    var birthdateText by remember { mutableStateOf("") }
    
    // Update local state when entity is fetched
    LaunchedEffect(userProfile) {
        birthdateText = userProfile?.birthDate ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إدارة الحساب",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isLinked = userProfile?.isGoogleLinked == true
            val name = userProfile?.name ?: "ضيف قداشّ"
            val email = userProfile?.email
            val avatarUrl = userProfile?.avatarUrl

            // 1. Account Details Card
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "الصورة الشخصية",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!email.isNullOrEmpty()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }

                    // Google badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLinked) Color(0xFF22C55E).copy(alpha = 0.12f) else Color(0xFF6B7280).copy(alpha = 0.12f))
                            .border(
                                width = 1.dp,
                                color = if (isLinked) Color(0xFF22C55E).copy(alpha = 0.3f) else Color(0xFF6B7280).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isLinked) Color(0xFF22C55E) else Color(0xFF6B7280), CircleShape)
                            )
                            Text(
                                text = if (isLinked) "حساب متصل سحابياً" else "وضع حساب ضيف",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isLinked) Color(0xFF22C55E) else TextGray
                            )
                        }
                    }
                }
            }

            // 2. Birthdate Entry Card
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تاريخ الميلاد (اختياري)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    
                    Text(
                        text = "أدخل تاريخ ميلادك لحفظه محلياً في جهازك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    AppInput(
                        value = birthdateText,
                        onValueChange = {
                            birthdateText = it
                            viewModel.saveBirthDate(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = if (userProfile?.birthDate.isNullOrEmpty()) "لم يُحدَّد بعد — اضغط للإضافة" else "مثال: 1995-05-15"
                    )
                }
            }

            // 3. Unlink Action
            if (isLinked) {
                Spacer(modifier = Modifier.height(24.dp))
                AppButton(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.BORDERED,
                    intent = ButtonIntent.DANGER
                ) {
                    Text("قطع ربط حساب Google", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showConfirmDialog) {
        AppDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = "قطع ربط الحساب",
            text = "هل أنت متأكد من رغبتك في قطع ربط هذا الحساب السحابي؟\n\nتنبيه: لن يتم حذف أي بيانات أو نسخ احتياطية محفوظة على Google Drive، سيتم فقط تسجيل الخروج وإلغاء الربط على هذا الجهاز.",
            confirmButtonText = "قطع الربط",
            onConfirm = {
                viewModel.unlinkAccount {
                    showConfirmDialog = false
                    Toast.makeText(context, "تم قطع ربط الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            },
            dismissButtonText = "إلغاء",
            onDismiss = { showConfirmDialog = false },
            isDestructive = true
        )
    }
}
