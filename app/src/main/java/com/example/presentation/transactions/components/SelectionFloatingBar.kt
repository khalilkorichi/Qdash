package com.example.presentation.transactions.components

import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.FormatterUtils
import com.example.ui.designsystem.tokens.ColorTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionFloatingBar(
    selectedCount: Int,
    selectedTotal: Double,
    onEditClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
        val barBgColor = if (isDark) ColorTokens.SurfaceDark else ColorTokens.SurfaceLight
        val borderColor = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = barBgColor,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right side (RTL): Amount & count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مجموع: ${FormatterUtils.formatCurrency(selectedTotal)} د.ج",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = "$selectedCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                // Left side (RTL): Buttons (Edit, Close)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Edit (Pencil) button
                    IconButton(
                        onClick = onEditClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل جماعي",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Close (✕) button
                    IconButton(
                        onClick = onCloseClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء التحديد",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
