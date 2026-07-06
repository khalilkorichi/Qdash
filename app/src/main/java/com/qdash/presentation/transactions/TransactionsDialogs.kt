package com.qdash.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.theme.TextGray

@Composable
fun DeleteTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = "حذف العملية المالية",
        text = "هل أنت متأكد من رغبتك في حذف هذا الإنفاق؟ سيتم موازنة الرصيد وتحديث الحساب تلقائياً.",
        confirmButtonText = "نعم، حذف",
        onConfirm = onConfirm,
        dismissButtonText = "إلغاء",
        isDestructive = true,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = ColorTokens.Danger
            )
        }
    )
}

@Composable
fun BulkDeleteDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = "حذف العمليات المحددة",
        text = "هل أنت متأكد من رغبتك في حذف $selectedCount عملية مجمعة نهائياً؟ لا يمكن التراجع عن هذا الإجراء وسيتم تحديث الحسابات تلقائياً.",
        confirmButtonText = "نعم، حذف الكل",
        onConfirm = onConfirm,
        dismissButtonText = "إلغاء",
        isDestructive = true,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = ColorTokens.Danger
            )
        }
    )
}

@Composable
fun BulkCategoryDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "تغيير فئة العمليات المحددة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "اختر الفئة الجديدة لنقل العمليات المحددة إليها:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(category.color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Surface(
                            onClick = { onConfirm(category.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextGray)
            }
        }
    )
}
