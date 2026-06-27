package com.example.presentation.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.TransactionType
import com.example.ui.designsystem.components.AppCard
import com.example.ui.designsystem.components.CardVariant
import com.example.ui.designsystem.tokens.ColorTokens
import com.example.ui.designsystem.tokens.MotionTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserMessageBubble(
    message: AiChatMessage,
    maxWidth: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Absolute.Right
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .background(
                        color = ColorTokens.Primary,
                        shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDirection = TextDirection.Content
                    ),
                    color = Color.White,
                    textAlign = TextAlign.End
                )
            }
            MessageTime(timestamp = message.timestamp, alignment = TextAlign.End)
        }
    }
}

@Composable
fun AiMessageBubble(
    message: AiChatMessage,
    isFirstInGroup: Boolean,
    isLatestAiMessage: Boolean,
    maxWidth: Dp,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val surface = if (isDark) ColorTokens.SurfaceDark else ColorTokens.SurfaceLight
    val border = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Absolute.Left,
        verticalAlignment = Alignment.Top
    ) {
        if (isFirstInGroup) {
            BotAvatar(size = AiChatConstants.BOT_ICON_SIZE.dp)
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width((AiChatConstants.BOT_ICON_SIZE + 8).dp))
        }

        Column(horizontalAlignment = Alignment.Start) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .background(surface, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                    .border(BorderStroke(1.dp, border), RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TypingTextEffect(
                    text = message.text,
                    enabled = isLatestAiMessage,
                    color = MaterialTheme.colorScheme.onSurface
                )
                message.draftTransaction?.let { transaction ->
                    AiStructuredCard(
                        transactionType = if (transaction.type == TransactionType.INCOME) "دخل" else "مصروف",
                        amount = transaction.amount.toLong(),
                        category = message.categoryName ?: "غير محدد",
                        timestamp = formatMessageTime(message.timestamp),
                        onViewDetails = onViewDetails
                    )
                }
            }
            MessageTime(timestamp = message.timestamp, alignment = TextAlign.Start)
        }
    }
}

@Composable
fun TypingIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(MotionTokens.DurationMedium)) + slideInVertically { it / 2 },
        exit = fadeOut(tween(MotionTokens.DurationMedium)) + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp),
            horizontalArrangement = Arrangement.Absolute.Left,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotAvatar(size = AiChatConstants.BOT_ICON_SIZE.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) { index ->
                    TypingDot(delayMillis = index * 150)
                }
            }
        }
    }
}

@Composable
fun AiStructuredCard(
    transactionType: String,
    amount: Long,
    category: String,
    timestamp: String,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transactionType == "دخل"
    val accent = if (isIncome) ColorTokens.Success else ColorTokens.Danger

    AppCard(
        variant = CardVariant.OUTLINED,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$transactionType مسودة",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "$category · $timestamp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onViewDetails) {
                    Text("عرض التفاصيل")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$amount دج",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = accent
                )
            }
        }
    }
}

@Composable
fun QuickActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.End
            )
        },
        shape = CircleShape,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    )
}

@Composable
fun QuickActionsRow(
    onQuickActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        items(AiChatConstants.QUICK_ACTIONS) { action ->
            QuickActionChip(text = action, onClick = { onQuickActionClick(action) })
        }
    }
}

@Composable
fun AiChatEmptyState(
    onQuickActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_state_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), CircleShape)
            )
            BotAvatar(size = AiChatConstants.EMPTY_STATE_ICON_SIZE.dp)
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "مرحباً! أنا مستشارك المالي",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "اسألني عن مصاريفك، ادخاراتك، أو سجّل معاملة بصوتك",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        QuickActionsRow(onQuickActionClick = onQuickActionClick)
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val surface = if (isDark) ColorTokens.SurfaceDark else ColorTokens.SurfaceLight
    val border = if (isDark) ColorTokens.BorderDark else ColorTokens.BorderLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(surface)
            .border(BorderStroke(1.dp, border.copy(alpha = 0.75f)))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onMicClick) {
            Icon(Icons.Default.Mic, contentDescription = "الإدخال الصوتي", tint = MaterialTheme.colorScheme.primary)
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("اكتب رسالتك...") },
            maxLines = AiChatConstants.MAX_INPUT_LINES,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDirection = TextDirection.Content,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            shape = RoundedCornerShape(22.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
        IconButton(onClick = onSend, enabled = text.isNotBlank()) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "إرسال",
                tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun BotAvatar(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF10B981))),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (size > 40.dp) Icons.Default.Android else Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
private fun TypingTextEffect(
    text: String,
    enabled: Boolean,
    color: Color
) {
    var visibleCharCount by remember(text, enabled) { mutableIntStateOf(if (enabled) 0 else text.length) }

    LaunchedEffect(text, enabled) {
        if (!enabled) return@LaunchedEffect
        visibleCharCount = 0
        repeat(text.length) { index ->
            delay(18L)
            visibleCharCount = index + 1
        }
    }

    Text(
        text = text.take(visibleCharCount),
        style = MaterialTheme.typography.bodyLarge.copy(
            textDirection = TextDirection.Content,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        ),
        color = color,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TypingDot(delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dot_$delayMillis")
    val y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 650
                0f at delayMillis
                -6f at delayMillis + 180
                0f at delayMillis + 360
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "typing_dot_y_$delayMillis"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(y = y.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
    )
}

@Composable
private fun MessageTime(
    timestamp: Long,
    alignment: TextAlign
) {
    Text(
        text = formatMessageTime(timestamp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        textAlign = alignment,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

private fun formatMessageTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.forLanguageTag("ar-DZ")).format(Date(timestamp))
}
