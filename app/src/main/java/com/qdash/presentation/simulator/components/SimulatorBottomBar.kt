package com.qdash.presentation.simulator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.theme.TextGray

enum class SimulatorBottomTab {
    CHEQUE,
    CHEQUE_SECOURS,
    POSTAL_PROFILES
}

/**
 * Floating 3-tab bottom bar matching FinTrackBottomNavBar's shape, border, shadow,
 * pill animation, and slide/fade visibility transitions.
 */
@Composable
fun SimulatorBottomBar(
    selectedTab: SimulatorBottomTab,
    isVisible: Boolean,
    onSelectTab: (SimulatorBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(200)
        ),
        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .testTag("simulator_bottom_bar")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.12f)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimulatorBottomNavItem(
                        icon = if (selectedTab == SimulatorBottomTab.CHEQUE) {
                            Icons.Default.Description
                        } else {
                            Icons.Outlined.Description
                        },
                        label = "الصك البريدي",
                        isSelected = selectedTab == SimulatorBottomTab.CHEQUE,
                        onClick = { onSelectTab(SimulatorBottomTab.CHEQUE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bottom_tab_cheque")
                    )

                    SimulatorBottomNavItem(
                        icon = if (selectedTab == SimulatorBottomTab.CHEQUE_SECOURS) {
                            Icons.AutoMirrored.Filled.ReceiptLong
                        } else {
                            Icons.AutoMirrored.Outlined.ReceiptLong
                        },
                        label = "شاك سوكور",
                        isSelected = selectedTab == SimulatorBottomTab.CHEQUE_SECOURS,
                        onClick = { onSelectTab(SimulatorBottomTab.CHEQUE_SECOURS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bottom_tab_cheque_secours")
                    )

                    SimulatorBottomNavItem(
                        icon = if (selectedTab == SimulatorBottomTab.POSTAL_PROFILES) {
                            Icons.Default.AccountBalanceWallet
                        } else {
                            Icons.Outlined.AccountBalanceWallet
                        },
                        label = "الحساب البريدي",
                        isSelected = selectedTab == SimulatorBottomTab.POSTAL_PROFILES,
                        onClick = { onSelectTab(SimulatorBottomTab.POSTAL_PROFILES) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bottom_tab_postal_profiles")
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulatorBottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "sim_nav_item_scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else TextGray.copy(alpha = 0.65f),
        animationSpec = tween(120),
        label = "sim_nav_item_color"
    )

    val pillBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(120),
        label = "sim_pill_bg_color"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(pillBgColor)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = iconColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
