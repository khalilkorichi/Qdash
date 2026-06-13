package com.example.presentation.components.radialmenu

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// ─────────────────────────────────────────────────────────
//  Data model for each radial menu action item
// ─────────────────────────────────────────────────────────

data class RadialMenuItem(
    val label: String,
    val icon: ImageVector,
    val tintColor: Color,
    val onClick: () -> Unit
)

// ─────────────────────────────────────────────────────────
//  AddActionFabContainer
//  Place inside a Box(fillMaxSize) – renders a fullscreen
//  dim overlay + vertical stack of capsule action items +
//  center-bottom docked FAB with rotation animation.
// ─────────────────────────────────────────────────────────

@Composable
fun AddActionFabContainer(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onTransfer: () -> Unit,
    onAddSaving: () -> Unit,
    onDebtPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val menuItems = remember(onAddExpense, onAddIncome, onTransfer, onAddSaving, onDebtPayment) {
        listOf(
            RadialMenuItem("إضافة مصروف", Icons.Default.Remove, ExpenseRed) {
                isExpanded = false; onAddExpense()
            },
            RadialMenuItem("إضافة دخل", Icons.Default.Add, IncomeGreen) {
                isExpanded = false; onAddIncome()
            },
            RadialMenuItem("تحويل رصيد", Icons.Default.SwapHoriz, TransferBlue) {
                isExpanded = false; onTransfer()
            },
            RadialMenuItem("حصالة ادخار", Icons.Default.Savings, SavingsAmber) {
                isExpanded = false; onAddSaving()
            },
            RadialMenuItem("سداد ديون", Icons.Default.AccountBalance, Color(0xFF8B5CF6)) {
                isExpanded = false; onDebtPayment()
            }
        )
    }

    // The outer Box fills the full screen so the dim layer covers everything
    Box(modifier = modifier.fillMaxSize()) {

        // ── 1. Full-screen backdrop dim ───────────────────────────────
        val dimAlpha by animateFloatAsState(
            targetValue = if (isExpanded) 0.45f else 0f,
            animationSpec = tween(durationMillis = 250),
            label = "dim_alpha"
        )
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = false }
                    )
                    .testTag("radial_menu_dim_layer")
            )
        }

        // ── 2. Centered bottom stack: capsule items + FAB ─────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("radial_action_menu_container")
            ) {
                // Staggered rising capsule menu items
                menuItems.forEachIndexed { index, item ->
                    val enterDelay  = if (isExpanded) index * 20 else 0
                    val exitDelay   = (menuItems.size - 1 - index) * 12

                    val alpha by animateFloatAsState(
                        targetValue = if (isExpanded) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 150,
                            delayMillis = if (isExpanded) enterDelay else exitDelay,
                            easing = FastOutSlowInEasing
                        ),
                        label = "item_alpha_$index"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isExpanded) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = 1200f
                        ),
                        label = "item_scale_$index"
                    )
                    val translationY by animateFloatAsState(
                        targetValue = if (isExpanded) 0f else 20f,
                        animationSpec = tween(
                            durationMillis = 150,
                            delayMillis = if (isExpanded) enterDelay else exitDelay,
                            easing = FastOutSlowInEasing
                        ),
                        label = "item_ty_$index"
                    )

                    if (alpha > 0.01f) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, item.tintColor.copy(alpha = 0.28f)),
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .graphicsLayer {
                                    this.alpha = alpha
                                    scaleX = scale
                                    scaleY = scale
                                    this.translationY = translationY
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = item.onClick
                                )
                                .testTag("radial_menu_item_$index")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(item.tintColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.tintColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = item.tintColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── 3. Main center-docked FAB with rotation ───────────────
                val fabRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 45f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "fab_rotation"
                )

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(elevation = 12.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Primary)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { isExpanded = !isExpanded }
                        .testTag("main_add_radial_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isExpanded) "إغلاق القائمة" else "إضافة عملية مالية",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(fabRotation)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  RadialActionMenu – public alias kept for backwards compat
//  (HomeScreen calls AddActionFabContainer directly, but
//   other screens may reference this composable.)
// ─────────────────────────────────────────────────────────

@Composable
fun RadialActionMenu(
    isExpanded: Boolean,
    items: List<RadialMenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // This is just the item stack, used when caller manages isExpanded externally.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.testTag("radial_action_menu_container")
    ) {
        items.forEachIndexed { index, item ->
            val enterDelay  = if (isExpanded) index * 20 else 0
            val exitDelay   = (items.size - 1 - index) * 12

            val alpha by animateFloatAsState(
                targetValue = if (isExpanded) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 150,
                    delayMillis = if (isExpanded) enterDelay else exitDelay,
                    easing = FastOutSlowInEasing
                ),
                label = "item_alpha_$index"
            )
            val scale by animateFloatAsState(
                targetValue = if (isExpanded) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 1200f
                ),
                label = "item_scale_$index"
            )

            if (alpha > 0.01f) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, item.tintColor.copy(alpha = 0.28f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .graphicsLayer {
                            this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = item.onClick
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(item.tintColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.tintColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = item.tintColor
                        )
                    }
                }
            }
        }
    }
}
