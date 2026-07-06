package com.qdash.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppInput
import com.qdash.ui.designsystem.components.ButtonIntent
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.theme.TextGray

@Composable
fun ConfirmRestoreDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "تأكيد استعادة البيانات",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        },
        text = {
            Text(
                "تحذير: ستعمل الاستعادة على دمج بيانات النسخة الاحتياطية مع البيانات الحالية. هل أنت متأكد من المتابعة؟",
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            AppButton(
                onClick = onConfirm,
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY
            ) { Text("نعم، تأكيد", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun BirthdateDialog(
    initialBirthdate: String,
    onDismiss: () -> Unit,
    onConfirm: (birthdate: String) -> Unit
) {
    var birthdateInput by remember { mutableStateOf(initialBirthdate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "تاريخ الميلاد",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "أدخل تاريخ ميلادك لحفظه محلياً على جهازك.",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppInput(
                    value = birthdateInput,
                    onValueChange = { birthdateInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "مثال: 1995-05-15"
                )
            }
        },
        confirmButton = {
            AppButton(
                onClick = { onConfirm(birthdateInput) },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = com.qdash.R.drawable.ic_app_logo),
                        contentDescription = "Qdash Logo",
                        modifier = Modifier.size(60.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "قداشّ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "الإصدار v${com.qdash.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "تطبيق متكامل لتتبع وإدارة المصاريف والميزانيات بطابع جزائري أصيل.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "جميع الحقوق محفوظة © 2026",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}
