package com.qdash.ui.designsystem.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    dismissButtonText: String? = null,
    onDismiss: (() -> Unit)? = null,
    isDestructive: Boolean = false,
    icon: (@Composable () -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
            val transition = rememberTransition(transitionState, label = "dialog_entrance")
            
            val alpha by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 150, easing = LinearOutSlowInEasing) },
                label = "dialog_alpha"
            ) { state ->
                if (state) 1f else 0f
            }
            
            val translateY by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 150, easing = FastOutSlowInEasing) },
                label = "dialog_translateY"
            ) { state ->
                if (state) 0f else 20f
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer(
                        alpha = alpha,
                        translationY = translateY
                    ),
                shape = ShapeTokens.Xl,
                colors = CardDefaults.cardColors(
                    containerColor = if (MaterialTheme.colorScheme.background != com.qdash.ui.designsystem.tokens.ColorTokens.BackgroundLight) {
                        com.qdash.ui.theme.ElevatedSurfaceDark
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.End // RTL layout compliance
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            ),
                            color = if (isDestructive) ColorTokens.Danger else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                        if (icon != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(contentAlignment = Alignment.Center) {
                                icon()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (dismissButtonText != null) {
                            AppButton(
                                onClick = {
                                    onDismiss?.invoke()
                                    onDismissRequest()
                                },
                                modifier = Modifier.weight(1f),
                                variant = ButtonVariant.LIGHT,
                                intent = ButtonIntent.PRIMARY
                            ) {
                                Text(dismissButtonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        AppButton(
                            onClick = {
                                onConfirm()
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(1.5f),
                            variant = ButtonVariant.SOLID,
                            intent = if (isDestructive) ButtonIntent.DANGER else ButtonIntent.PRIMARY
                        ) {
                            Text(confirmButtonText, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
