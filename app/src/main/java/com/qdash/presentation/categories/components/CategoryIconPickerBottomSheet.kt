package com.qdash.presentation.categories.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.presentation.components.getIconByName
import com.qdash.ui.designsystem.tokens.CategoryIconRegistry

/**
 * Modal bottom sheet for choosing category vector icons or standard icons,
 * keeping screen files isolated and lightweight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryIconPickerBottomSheet(
    selectedIconKey: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "اختر أيقونة الفئة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                items(CategoryIconRegistry.registeredIcons) { token ->
                    val isSelected = token.key.equals(selectedIconKey, ignoreCase = true)
                    val accentColor = MaterialTheme.colorScheme.primary

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) accentColor else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                onIconSelected(token.key)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (token.drawableResId != null) {
                            Icon(
                                painter = painterResource(id = token.drawableResId),
                                contentDescription = token.labelAr,
                                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Icon(
                                imageVector = getIconByName(token.key),
                                contentDescription = token.labelAr,
                                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
