package com.qdash.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.BudgetStatus
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.SavingsAmber

@Composable
fun BudgetStatusChip(
    status: BudgetStatus,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (status) {
        BudgetStatus.SAFE -> "آمن" to IncomeGreen
        BudgetStatus.WARNING -> "تحذير" to SavingsAmber
        BudgetStatus.CRITICAL -> "حرج" to ExpenseRed
        BudgetStatus.EXCEEDED -> "متجاوز" to ExpenseRed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        )
    }
}
