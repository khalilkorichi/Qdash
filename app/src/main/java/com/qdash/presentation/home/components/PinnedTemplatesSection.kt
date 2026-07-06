package com.qdash.presentation.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.TransactionTemplate
import com.qdash.domain.model.TransactionType
import com.qdash.domain.model.TransactionDraft
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TransferBlue

@Composable
fun PinnedTemplatesSection(
    pinnedTemplates: List<TransactionTemplate>,
    onManageTemplatesClick: () -> Unit,
    onTemplateSelect: (jsonDraft: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "القوالب المثبتة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "إدارة القوالب",
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                modifier = Modifier
                    .clickable { onManageTemplatesClick() }
                    .padding(4.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(pinnedTemplates, key = { it.id }) { template ->
                val typeAccentColor = when (template.transactionType) {
                    TransactionType.EXPENSE -> ExpenseRed
                    TransactionType.INCOME -> IncomeGreen
                    TransactionType.TRANSFER -> TransferBlue
                }
                Surface(
                    onClick = {
                        val draft = TransactionDraft(
                            amount = template.amount,
                            type = template.transactionType,
                            categoryId = template.categoryId,
                            subcategoryId = template.subcategoryId,
                            accountId = template.accountId,
                            targetAccountId = template.targetAccountId,
                            notes = template.notes,
                            templateId = template.id
                        )
                        val json = """
                            {
                                "amount": ${draft.amount},
                                "type": "${draft.type.name}",
                                "categoryId": ${draft.categoryId ?: "null"},
                                "subcategoryId": ${draft.subcategoryId ?: "null"},
                                "accountId": ${draft.accountId},
                                "targetAccountId": ${draft.targetAccountId ?: "null"},
                                "notes": "${draft.notes?.replace("\"", "\\\"") ?: ""}",
                                "templateId": ${draft.templateId ?: "null"}
                            }
                        """.trimIndent().replace("\n", "").replace(" ", "")
                        onTemplateSelect(json)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, typeAccentColor.copy(alpha = 0.15f)),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val emoji = template.iconEmoji
                        if (emoji != null && emoji.isNotBlank()) {
                            Text(text = emoji, fontSize = 18.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = template.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = FormatterUtils.formatCurrency(template.amount),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = typeAccentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
