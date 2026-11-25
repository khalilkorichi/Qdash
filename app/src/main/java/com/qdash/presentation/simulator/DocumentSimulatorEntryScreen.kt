package com.qdash.presentation.simulator

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens

@Composable
fun DocumentSimulatorEntryScreen(
    viewModel: DocumentSimulatorViewModel,
    onSelectDocType: (DocumentType) -> Unit,
    onManageProfiles: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val chequeAccent = if (isDark) ColorTokens.InfoDark else ColorTokens.PosteBlue
    val chequeSecoursAccent = if (isDark) ColorTokens.PosteYellow else Color(0xFFD97706)
    val profilesAccent = if (isDark) ColorTokens.SuccessDark else ColorTokens.Success

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FinTrackTopBar(
                title = "المحاكي البريدي",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("simulator_bento_entry"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Wide Rectangular Bento Tile: Postal Account Quick Setup
            SimulatorBentoTile(
                icon = Icons.Default.AccountBalanceWallet,
                title = "بيانات الحساب البريدي",
                subtitle = if (uiState.savedProfiles.isEmpty()) {
                    "أضف حساب CCP للتعبئة الفورية لاحقاً"
                } else {
                    "${uiState.savedProfiles.size} حساب محفوظ للتعبئة الفورية"
                },
                accentColor = profilesAccent,
                isWide = true,
                onClick = onManageProfiles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .testTag("entry_bento_profiles")
            )

            // Two Square Bento Tiles Side-by-Side: Postal Check & Chèque Secours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SimulatorBentoTile(
                    icon = Icons.Default.Description,
                    title = "الصك البريدي",
                    subtitle = "معاينة وتفقيط الصك فوراً",
                    accentColor = chequeAccent,
                    isWide = false,
                    onClick = { onSelectDocType(DocumentType.CHEQUE) },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .testTag("entry_bento_cheque")
                )

                SimulatorBentoTile(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "شاك سوكور",
                    subtitle = "تعبئة استمارة SFP 01",
                    accentColor = chequeSecoursAccent,
                    isWide = false,
                    onClick = { onSelectDocType(DocumentType.SFP01) },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .testTag("entry_bento_cheque_secours")
                )
            }
        }
    }
}

/**
 * Bento action tile matching QuickActionTile on the Home screen:
 * surfaceVariant container, rounded 16.dp corners, subtle radial top-corner glow,
 * linear-gradient icon container, corner directional arrow, title, and concise 1-line subtitle.
 */
@Composable
private fun SimulatorBentoTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    isWide: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = MotionTokens.springResponsive(),
        label = "sim_bento_press_scale"
    )

    val iconBg = remember(accentColor) {
        Brush.linearGradient(
            listOf(accentColor.copy(alpha = 0.26f), accentColor.copy(alpha = 0.08f))
        )
    }

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val glowCenter = Offset(x = if (isRtl) 0f else size.width, y = 0f)
                    val glowRadius = size.minDimension * 0.95f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (isWide) 0.14f else 0.12f),
                                accentColor.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            center = glowCenter,
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = glowCenter
                    )
                }
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isRtl) Icons.Default.NorthWest else Icons.Default.NorthEast,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.55f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isWide) 15.sp else 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}
