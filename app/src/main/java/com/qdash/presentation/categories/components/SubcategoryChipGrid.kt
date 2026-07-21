package com.qdash.presentation.categories.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.qdash.presentation.components.getIconByName

@Immutable
data class SubcategoryChipGroupState(
    val subcategories: List<Category>,
    val selectedSubcategoryId: Long?
)

/**
 * Lightweight, reusable Composable component displaying nested subcategories as selectable chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubcategoryChipGrid(
    subcategories: List<Category>,
    selectedSubcategoryId: Long?,
    onSubcategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    if (subcategories.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        subcategories.forEach { subcategory ->
            val isSelected = subcategory.id == selectedSubcategoryId

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSubcategorySelected(subcategory) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = getIconByName(subcategory.icon),
                        contentDescription = subcategory.name,
                        tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = subcategory.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
