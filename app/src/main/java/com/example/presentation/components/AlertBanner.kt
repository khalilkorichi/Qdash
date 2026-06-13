package com.example.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.domain.model.BudgetStatus
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SavingsAmber

@Composable
fun AlertBanner(
    message: String,
    status: BudgetStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (status) {
        BudgetStatus.SAFE -> IncomeGreen to Icons.Default.Info
        BudgetStatus.WARNING -> SavingsAmber to Icons.Default.Warning
        BudgetStatus.CRITICAL -> ExpenseRed to Icons.Default.Warning
        BudgetStatus.EXCEEDED -> ExpenseRed to Icons.Default.Warning
    }

    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    val transition = rememberTransition(transitionState, label = "alert_entrance")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 150, easing = LinearOutSlowInEasing) },
        label = "alert_alpha"
    ) { state ->
        if (state) 1f else 0f
    }
    val translateY by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 150, easing = FastOutSlowInEasing) },
        label = "alert_translateY"
    ) { state ->
        if (state) 0f else 10f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = alpha,
                translationY = translateY
            ),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}
