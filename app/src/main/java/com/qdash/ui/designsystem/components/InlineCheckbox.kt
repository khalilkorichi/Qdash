package com.qdash.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.tokens.ColorTokens

@Composable
fun InlineCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    labelArabic: String,
    labelFrench: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = ColorTokens.SfpBorderBrown,
    textColor: Color = Color(0xFF1A1A1A)
) {
    Row(
        modifier = modifier
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Small checkbox box
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color.White)
                .border(BorderStroke(1.dp, borderColor)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = textColor
                    )
                )
            }
        }

        // Bilingual labels
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = labelArabic,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = textColor,
                lineHeight = 8.sp
            )
            Text(
                text = labelFrench,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = textColor.copy(alpha = 0.8f),
                lineHeight = 8.sp
            )
        }
    }
}
