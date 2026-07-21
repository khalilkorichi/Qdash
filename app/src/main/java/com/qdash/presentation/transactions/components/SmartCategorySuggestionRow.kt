package com.qdash.presentation.transactions.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.model.SuggestionSource

@Immutable
data class SmartSuggestionUiState(
    val suggestion: CategorySuggestion?,
    val matchedCategory: Category?
)

/**
 * Isolated, non-blocking Composable for real-time category suggestions below transaction input.
 * Displays high-confidence auto-apply vs soft suggestion chips.
 */
@Composable
fun SmartCategorySuggestionRow(
    suggestion: CategorySuggestion?,
    matchedCategory: Category?,
    onAcceptSuggestion: (Long) -> Unit,
    onDismissSuggestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = suggestion != null && (matchedCategory != null || suggestion.newCategoryName != null)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (suggestion == null) return@AnimatedVisibility
        val categoryName = matchedCategory?.name ?: suggestion.newCategoryName ?: return@AnimatedVisibility
        val categoryId = matchedCategory?.id
        val confidence = suggestion.confidenceScore
        val isHighConfidence = confidence >= 0.85f
        val sourceLabel = when (suggestion.suggestionSource) {
            SuggestionSource.HISTORY -> "سجل سابق"
            SuggestionSource.KEYWORD -> "مطابقة ذكية"
            SuggestionSource.RULE -> "قاعدة معرفية"
            SuggestionSource.AI -> "ذكاء اصطناعي"
            SuggestionSource.NONE -> ""
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isHighConfidence) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isHighConfidence) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isHighConfidence) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "اقتراح: $categoryName",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isHighConfidence) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "تطبيق تلقائي",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (sourceLabel.isNotEmpty()) {
                            Text(
                                text = "$sourceLabel • نسبة الثقة ${(confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (categoryId != null) {
                        IconButton(
                            onClick = { onAcceptSuggestion(categoryId) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تأكيد",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissSuggestion,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
