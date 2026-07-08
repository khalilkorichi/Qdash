package com.qdash.presentation.accounts

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset
import com.qdash.core.ui.StableList
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import kotlin.math.roundToInt

/**
 * Wraps [AccountItemCard] with drag-and-drop visual treatment.
 *
 * @param isDragging         True when *this* item is being actively dragged.
 * @param isSomethingDragging True when *any* item in the list is being dragged.
 * @param dragOffsetY        Current Y offset in pixels (0f when not dragging).
 * @param dragHandleModifier Modifier created by the parent that installs the
 *                           [pointerInput] gesture detector; passed through to
 *                           [AccountItemCard] unchanged.
 */
@Composable
internal fun DraggableAccountItem(
    account: Account,
    transactions: StableList<Transaction>,
    categories: StableList<Category>,
    showBalance: Boolean,
    onToggleBalance: () -> Unit,
    onEdit: () -> Unit,
    onCardClick: () -> Unit,
    isDragging: Boolean,
    isSomethingDragging: Boolean,
    dragOffsetY: Float,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragScale"
    )
    val dragAlpha by animateFloatAsState(
        targetValue = if (isSomethingDragging && !isDragging) 0.65f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dragAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 15f else 1f)
    ) {
        // Drop-target placeholder shown behind the elevated dragged card
        if (isDragging) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        AccountItemCard(
            account = account,
            transactions = transactions,
            categories = categories,
            showBalance = showBalance,
            onToggleBalance = onToggleBalance,
            onEdit = onEdit,
            onCardClick = onCardClick,
            modifier = Modifier
                .scale(dragScale)
                .graphicsLayer { alpha = dragAlpha }
                .offset { IntOffset(0, dragOffsetY.roundToInt()) },
            dragHandleModifier = dragHandleModifier
        )
    }
}
