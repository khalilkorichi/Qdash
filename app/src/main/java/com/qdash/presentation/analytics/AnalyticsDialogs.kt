package com.qdash.presentation.analytics

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExplanationInfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("حسناً", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
            )
        }
    )
}

@Composable
fun ExportProgressDialog(
    progressText: String
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = {
            Text(
                text = "جاري تصدير التقرير",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
fun ExportResultDialog(
    fileUri: String,
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    com.qdash.core.utils.FileUtils.openPdfFile(context, fileUri)
                }) {
                    Text("فتح التقرير")
                }
                TextButton(onClick = onDismiss) {
                    Text("حسناً")
                }
            }
        },
        title = {
            Text(
                text = "تم التصدير بنجاح",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(text = "تم حفظ التقرير المالي الشامل بصيغة PDF بنجاح في المسار:\n\n$fileUri")
        }
    )
}

@Composable
fun ExportErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("حسناً")
            }
        },
        title = {
            Text(
                text = "فشل تصدير التقرير",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(text = "حدث خطأ أثناء تصدير التقرير:\n\n$errorMessage")
        }
    )
}
