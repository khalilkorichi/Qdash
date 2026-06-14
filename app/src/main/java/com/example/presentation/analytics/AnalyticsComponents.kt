package com.example.presentation.analytics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.EmptyStateView
import com.example.core.ui.components.FinTrackTopBar
import com.example.core.ui.components.UnifiedScreenHeader
import com.example.core.utils.FormatterUtils
import com.example.ui.theme.*
import com.example.ui.designsystem.components.*
import com.example.ui.designsystem.tokens.*
import com.example.presentation.navigation.LocalNavController
import com.example.presentation.navigation.Screen
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.asAndroidPath
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.geometry.Size
import kotlin.math.asin


@Composable
fun DonutChartSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(width = 180.dp, height = 24.dp)
                    .shimmerEffect(ShapeTokens.Md)
            )
            Spacer(modifier = Modifier.height(20.dp))
            // Central Circular Shimmer
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .shimmerEffect(CircleShape)
            )
            Spacer(modifier = Modifier.height(20.dp))
            // Legend rows
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .shimmerEffect(ShapeTokens.Sm)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(96.dp)
            .shimmerEffect(ShapeTokens.Lg)
    )
}

@Composable
fun BarChartSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .shimmerEffect(RoundedCornerShape(20.dp))
    )
}

@Composable
fun CategoryRankSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shimmerEffect(RoundedCornerShape(14.dp))
            )
        }
    }
}

@Composable
fun AnalyticsEmptyState(
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Glowing Premium Icon Container
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Primary.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PieChart,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ط³ط¬ظ„ ط¥ط­طµط§ط¦ظٹط§طھظƒ ظپط§ط±ط؛ ط­ط§ظ„ظٹط§ظ‹",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ط§ط¨ط¯ط£ ط¨طھط³ط¬ظٹظ„ ظ…طµط§ط±ظٹظپظƒ ظˆط¯ط®ظ„ظƒ ط§ظ„ظٹظˆظ…ظٹ ظ„طھط¸ظ‡ط± ظ‡ظ†ط§ طھط­ظ„ظٹظ„ط§طھ ظˆط±ط³ظˆظ…ط§طھ ط°ظƒظ‘ظٹط© طھط³ط§ط¹ط¯ظƒ ط¹ظ„ظ‰ ظپظ‡ظ… ظ†ظ…ط· ط¥ظ†ظپط§ظ‚ظƒ ظˆطھظˆظپظٹط± ط§ظ„ظ…ط²ظٹط¯.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Dotted Preview Placeholders (Visual Promise)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ظ…ط§ ط³طھط­طµظ„ ط¹ظ„ظٹظ‡ ط¨ط¹ط¯ طھط³ط¬ظٹظ„ ط§ظ„ط¹ظ…ظ„ظٹط§طھ:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )

                    // Item 1: Spending distribution preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PieChart,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "طھظˆط²ظٹط¹ ط§ظ„ظ…طµط§ط±ظٹظپ ط­ط³ط¨ ط§ظ„ظپط¦ط§طھ",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ط±ط³ظ… ط¯ط§ط¦ط±ظٹ طھظپط§ط¹ظ„ظٹ ظٹظˆط¶ط­ ط§ظ„ظپط¦ط§طھ ط§ظ„ط£ظƒط«ط± ط§ط³طھظ‡ظ„ط§ظƒط§ظ‹ ظ„ظ…ظٹط²ط§ظ†ظٹطھظƒ",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    }

                    // Item 2: Cash flow trends preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(IncomeGreen.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ظ…ط®ط·ط· ط§ظ„طھط¯ظپظ‚ ط§ظ„ظ†ظ‚ط¯ظٹ ط§ظ„طھط§ط±ظٹط®ظٹ",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ظ…ظ‚ط§ط±ظ†ط© ط¨طµط±ظٹط© ط°ظƒظٹط© ظˆطھظپط§ط¹ظ„ظٹط© ط¨ظٹظ† ط¥ط¬ظ…ط§ظ„ظٹ ط§ظ„ط¯ط®ظ„ ظˆط§ظ„ظ…طµط§ط±ظٹظپ ط´ظ‡ط±ظٹط§ظ‹",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    }

                    // Item 3: Smart Insights
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SavingsAmber.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SavingsAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "طھط­ظ„ظٹظ„ط§طھ ظˆظ…ط¤ط´ط±ط§طھ ط°ظƒظٹط©",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "طھظˆظ‚ط¹ط§طھ ط§ظ„ط¥ظ†ظپط§ظ‚طŒ ط£ظ…ط§ظ† طµظ†ط¯ظˆظ‚ ط§ظ„ط·ظˆط§ط±ط¦ ظˆظ…ط¹ط¯ظ„ ط§ظ„ط§ط¯ط®ط§ط±",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddTransactionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = "ط³ط¬ظ„ ط£ظˆظ„ ظ…ط¹ط§ظ…ظ„ط© ط§ظ„ط¢ظ†",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}
@Composable
fun InteractiveDonutCard(
    shares: List<CategoryShare>,
    selectedCategory: CategoryShare?,
    onSelectedCategoryChange: (CategoryShare?) -> Unit,
    onHelpClick: (String, String) -> Unit,
    onCategoryLongClick: (CategoryShare) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    var viewMode by remember { mutableStateOf(ChartViewMode.DONUT) }

    // Bouncy animated progresses mapped to each category share to allow smooth transitions
    val animatedProgresses = shares.associate { share ->
        val isSelected = selectedCategory?.categoryName == share.categoryName
        share.categoryName to animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "anim_${share.categoryName}"
        ).value
    }
    // Cache all share parsed colors once â€” avoids Color.parseColor on every animation frame
    val parsedShareColors = remember(shares) {
        shares.associate { share ->
            share.categoryName to (try { Color(android.graphics.Color.parseColor(share.color)) } catch (e: Exception) { null })
        }
    }
    // Cache bar chart gradients per share â€” avoids Brush creation on every recomposition
    val parsedBarBrushes = remember(shares) {
        shares.associate { share ->
            val c = try { Color(android.graphics.Color.parseColor(share.color)) } catch (e: Exception) { Color.Gray }
            share.categoryName to Brush.verticalGradient(colors = listOf(c, c.copy(alpha = 0.5f)))
        }
    }
    // Cache total amount â€” avoids sumOf traversal on every recomposition
    val totalAmount = remember(shares) { shares.sumOf { it.amount } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "طھظˆط²ظٹط¹ ط§ظ„ظ…طµط§ط±ظٹظپ ط­ط³ط¨ ط§ظ„ظپط¦ط©",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "طھط­ظ„ظٹظ„ ظˆطھظˆط²ظٹط¹ ط§ظ„ظ†ظپظ‚ط§طھ طھظپطµظٹظ„ظٹط§ظ‹",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    HelpIconButton(
                        onClick = {
                            onHelpClick(
                                "طھظˆط²ظٹط¹ ط§ظ„ظ…طµط§ط±ظٹظپ ط­ط³ط¨ ط§ظ„ظپط¦ط©",
                                "ظٹظ‚ظٹط³ ظ‡ط°ط§ ط§ظ„ظ…ط®ط·ط· ط§ظ„ظ†ط³ط¨ط© ط§ظ„ظ…ط¦ظˆظٹط© ظ„ط¥ط¬ظ…ط§ظ„ظٹ ظ†ظپظ‚ط§طھظƒ ط§ظ„ظ…ظˆط²ط¹ط© ط¹ظ„ظ‰ ظ…ط®طھظ„ظپ ط§ظ„ظپط¦ط§طھ ط§ظ„ظ…ط§ظ„ظٹط© (ظƒط§ظ„ط£ط؛ط°ظٹط©طŒ ط§ظ„ظ†ظ‚ظ„طŒ ط§ظ„ظپظˆط§طھظٹط±طŒ ط¥ظ„ط®) ط®ظ„ط§ظ„ ط§ظ„ظپطھط±ط© ط§ظ„ط²ظ…ظ†ظٹط© ط§ظ„ظ…ط­ط¯ط¯ط©.\n\nط§ظ„ظپط§ط¦ط¯ط©: ظٹط³ط§ط¹ط¯ظƒ ط¹ظ„ظ‰ ط±طµط¯ ط§ظ„ظپط¦ط§طھ ط§ظ„ط±ط¦ظٹط³ظٹط© ط§ظ„ط£ظƒط«ط± ط§ط³طھظ‡ظ„ط§ظƒط§ظ‹ ظ„ط³ظٹظˆظ„طھظƒ ط§ظ„ظ†ظ‚ط¯ظٹط© ظ„طھطھظ…ظƒظ† ظ…ظ† ط§طھط®ط§ط° ظ‚ط±ط§ط±ط§طھ ظˆط§ط¹ظٹط© ط¨ظƒط¨ط­ ط§ظ„طµط±ظپ ظپظٹ ط§ظ„ط¬ظˆط§ظ†ط¨ ط§ظ„طھط±ظپظٹظ‡ظٹط© ظˆط²ظٹط§ط¯ط© ظ…ط¯ط®ط±ط§طھظƒ."
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                // Premium Segmented Control Switcher
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bar chart button
                        val barBg by animateColorAsState(
                            targetValue = if (viewMode == ChartViewMode.BAR) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = tween(220),
                            label = "barBg"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(barBg)
                                .clickable { viewMode = ChartViewMode.BAR }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "ط£ط¹ظ…ط¯ط©",
                                tint = if (viewMode == ChartViewMode.BAR) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )

                        // Donut chart button
                        val donutBg by animateColorAsState(
                            targetValue = if (viewMode == ChartViewMode.DONUT) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = tween(220),
                            label = "donutBg"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(donutBg)
                                .clickable { viewMode = ChartViewMode.DONUT }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PieChart,
                                contentDescription = "ط¯ط§ط¦ط±ط©",
                                tint = if (viewMode == ChartViewMode.DONUT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))

            if (viewMode == ChartViewMode.DONUT) {
                // Donut Canvas â€” 240dp with premium visual layout and floating badge
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .pointerInput(shares) {
                            // Custom gesture handler: tap fires IMMEDIATELY on UP (no 500ms delay),
                            // long press fires after 400ms. This avoids detectTapGestures' built-in
                            // delay that blocks onTap when onLongPress is also registered.
                            val longPressTimeoutMs = 400L
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downTime = System.currentTimeMillis()
                                val downOffset = down.position
                                var longPressTriggered = false

                                // Wait for UP or timeout
                                do {
                                    val elapsed = System.currentTimeMillis() - downTime
                                    if (!longPressTriggered && elapsed >= longPressTimeoutMs) {
                                        longPressTriggered = true
                                        // --- LONG PRESS LOGIC ---
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val dx = downOffset.x - centerX
                                        val dy = downOffset.y - centerY
                                        val distance = sqrt(dx * dx + dy * dy)
                                        val outerRadius = centerX
                                        val innerRadius = outerRadius - 44.dp.toPx()
                                        if (distance in innerRadius..outerRadius) {
                                            val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                            var chartAngle = (degrees + 90f + 360f) % 360f
                                            val minTouchDegrees = 18f
                                            var currentAngle = 0f
                                            var bestMatch: CategoryShare? = null
                                            var bestDistance = Float.MAX_VALUE
                                            for (share in shares) {
                                                val sweep = share.percentage * 360f
                                                val center = currentAngle + sweep / 2f
                                                if (chartAngle >= currentAngle && chartAngle < currentAngle + sweep) {
                                                    bestMatch = share; break
                                                }
                                                if (sweep < minTouchDegrees) {
                                                    val diff = kotlin.math.abs(chartAngle - center) % 360f
                                                    val dist = if (diff > 180f) 360f - diff else diff
                                                    if (dist <= minTouchDegrees / 2f && dist < bestDistance) {
                                                        bestMatch = share; bestDistance = dist
                                                    }
                                                }
                                                currentAngle += sweep
                                            }
                                            if (bestMatch != null) onCategoryLongClick(bestMatch)
                                        }
                                    }
                                    val event = withTimeoutOrNull(16) {
                                        awaitPointerEvent()
                                    } ?: continue
                                    val upPointer = event.changes.firstOrNull { it.changedToUp() }
                                    if (upPointer != null) {
                                        upPointer.consume()
                                        if (!longPressTriggered) {
                                            // --- TAP LOGIC (fires immediately) ---
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val dx = downOffset.x - centerX
                                            val dy = downOffset.y - centerY
                                            val distance = sqrt(dx * dx + dy * dy)
                                            val outerRadius = centerX
                                            val innerRadius = outerRadius - 44.dp.toPx()
                                            if (distance in innerRadius..outerRadius) {
                                                val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                var chartAngle = (degrees + 90f + 360f) % 360f
                                                val minTouchDegrees = 18f
                                                var currentAngle = 0f
                                                var bestMatch: CategoryShare? = null
                                                var bestDistance = Float.MAX_VALUE
                                                for (share in shares) {
                                                    val sweep = share.percentage * 360f
                                                    val center = currentAngle + sweep / 2f
                                                    if (chartAngle >= currentAngle && chartAngle < currentAngle + sweep) {
                                                        bestMatch = share; break
                                                    }
                                                    if (sweep < minTouchDegrees) {
                                                        val diff = kotlin.math.abs(chartAngle - center) % 360f
                                                        val dist = if (diff > 180f) 360f - diff else diff
                                                        if (dist <= minTouchDegrees / 2f && dist < bestDistance) {
                                                            bestMatch = share; bestDistance = dist
                                                        }
                                                    }
                                                    currentAngle += sweep
                                                }
                                                // Toggle: tap selected -> deselect, tap other -> select
                                                onSelectedCategoryChange(
                                                    if (bestMatch != null && selectedCategory?.categoryName == bestMatch.categoryName) null
                                                    else bestMatch
                                                )
                                            } else {
                                                onSelectedCategoryChange(null)
                                            }
                                        }
                                        break
                                    }
                                } while (true)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val themePrimaryColor = MaterialTheme.colorScheme.primary
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val outerRadius = centerX - 12.dp.toPx() // leave padding for pop-out

                        var currentAngle = -90f
                        shares.forEachIndexed { index, share ->
                            val sweep = share.percentage * 360f
                            
                            val parseColor = parsedShareColors[share.categoryName] ?: themePrimaryColor
                            
                            val isSelected = selectedCategory?.categoryName == share.categoryName
                            val isActive = selectedCategory == null || isSelected
                            
                            val progress = animatedProgresses[share.categoryName] ?: 0f
                            
                            val colorAlpha = if (isActive) 1f else 0.25f
                            // Smoothly animate stroke width from 32.dp to 40.dp
                            val strokeWidthVal = with(density) { (32.dp + (8.dp * progress)).toPx() }
                            
                            val shiftX = if (progress > 0f) {
                                val midAngle = currentAngle + sweep / 2f
                                val rad = Math.toRadians(midAngle.toDouble())
                                val popOutDist = with(density) { (6.dp * progress).toPx() }
                                (popOutDist * cos(rad)).toFloat()
                            } else 0f
                            
                            val shiftY = if (progress > 0f) {
                                val midAngle = currentAngle + sweep / 2f
                                val rad = Math.toRadians(midAngle.toDouble())
                                val popOutDist = with(density) { (6.dp * progress).toPx() }
                                (popOutDist * sin(rad)).toFloat()
                            } else 0f
                            
                            // The mid-radius of the arc
                            val r_outer = outerRadius
                            val r_mid = r_outer - strokeWidthVal / 2f
                            
                            // Calculate the cap angle in degrees
                            val capRadius = strokeWidthVal / 2f
                            val capAngle = if (r_mid > 0f) {
                                Math.toDegrees(asin((capRadius / r_mid).coerceIn(-1f, 1f).toDouble())).toFloat()
                            } else 0f
                            
                            val gap = if (shares.size > 1) 2.5f else 0f
                            val adjustedSweep = (sweep - gap).coerceAtLeast(0f)
                            
                            val maxCapAngle = adjustedSweep / 2f
                            val localCapAngle = capAngle.coerceAtMost(maxCapAngle)
                            
                            val drawStartAngle = currentAngle + gap / 2f + localCapAngle
                            val drawSweepAngle = (adjustedSweep - 2 * localCapAngle).coerceAtLeast(0f)
                            
                            val arcSize = Size(r_mid * 2, r_mid * 2)
                            val topLeftOffset = Offset(
                                centerX + shiftX - r_mid,
                                centerY + shiftY - r_mid
                            )
                            
                            drawArc(
                                color = parseColor.copy(alpha = colorAlpha),
                                startAngle = drawStartAngle,
                                sweepAngle = drawSweepAngle,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = arcSize,
                                style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round)
                            )
                            currentAngle += sweep
                        }
                    }

                    // Sleek Center Info Badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectedCategoryChange(null) }
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (selectedCategory != null) {
                            val cat = selectedCategory
                            
                            Text(
                                text = FormatterUtils.formatCurrency(cat.amount),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                "ط¥ط¬ظ…ط§ظ„ظٹ ط§ظ„ظ…طµط§ط±ظٹظپ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray,
                                    fontSize = 9.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatterUtils.formatCurrency(totalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // Category percentage labels drawn directly on top of the selected segment
                    var accumulatedAngle = 0f
                    shares.forEach { share ->
                        val sweep = share.percentage * 360f
                        val isSelected = selectedCategory?.categoryName == share.categoryName
                        
                        if (isSelected) {
                            val midAngle = -90f + accumulatedAngle + sweep / 2f
                            val rad = Math.toRadians(midAngle.toDouble())
                            
                            val progress = animatedProgresses[share.categoryName] ?: 0f
                            
                            val strokeWidthDp = 32.dp + 8.dp * progress
                            val radiusDp = 120.dp - 12.dp - strokeWidthDp / 2f
                            val popOutDistDp = 6.dp * progress
                            val finalRadiusDp = radiusDp + popOutDistDp
                            
                            val badgeOffsetX = (finalRadiusDp.value * cos(rad)).toFloat().dp
                            val badgeOffsetY = (finalRadiusDp.value * sin(rad)).toFloat().dp
                            
                            val parseColor = parsedShareColors[share.categoryName] ?: Primary

                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .absoluteOffset(x = badgeOffsetX, y = badgeOffsetY),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    border = BorderStroke(1.5.dp, parseColor),
                                    shape = RoundedCornerShape(20.dp),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.scale(0.95f + 0.05f * progress)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(parseColor, CircleShape)
                                        )
                                        Text(
                                            text = "${share.categoryName} â€¢ ${(share.percentage * 100).toInt()}%",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        accumulatedAngle += sweep
                    }
                }
            } else {
                // Interactive Column Chart view mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val totalBars = shares.size.coerceAtMost(6)
                    val barSpacing = 16.dp
                    
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        shares.take(6).forEach { share ->
                            val parseColor = parsedShareColors[share.categoryName] ?: Primary
                            val isSelected = selectedCategory?.categoryName == share.categoryName
                            val isActive = selectedCategory == null || isSelected

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectedCategoryChange(if (selectedCategory?.categoryName == share.categoryName) null else share) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(140.dp * share.percentage.coerceIn(0.05f, 1f))
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            parsedBarBrushes[share.categoryName] ?: Brush.verticalGradient(
                                                colors = listOf(parseColor, parseColor.copy(alpha = 0.5f))
                                            ),
                                            alpha = if (isActive) 1f else 0.3f
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) parseColor else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = share.categoryName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isSelected) parseColor else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${(share.percentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Premium Grid-style Legend
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                shares.take(5).forEachIndexed { index, share ->
                    val parseColor = parsedShareColors[share.categoryName] ?: MaterialTheme.colorScheme.primary
                    val isSelected = selectedCategory?.categoryName == share.categoryName
                    
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) parseColor.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) parseColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectedCategoryChange(if (selectedCategory?.categoryName == share.categoryName) null else share) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(parseColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = share.categoryName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = FormatterUtils.formatCurrency(share.amount),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = { share.percentage.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(3.dp)
                                            .clip(CircleShape),
                                        color = parseColor,
                                        trackColor = parseColor.copy(alpha = 0.12f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${(share.percentage * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) parseColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A unified smart date navigator that replaces the old tab + dropdown selectors.
 * Shows period-mode pills (ظٹظˆظ… / ط£ط³ط¨ظˆط¹ / ط´ظ‡ط± / ط³ظ†ط©) and left/right arrow
 * navigator to move between periods. RTL-aware.
 */
@Composable
fun SmartDateNavigator(
    uiState: AnalyticsUiState,
    onPeriodChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val arabicMonths = arrayOf(
        "ط¬ط§ظ†ظپظٹ", "ظپظٹظپط±ظٹ", "ظ…ط§ط±ط³", "ط£ظپط±ظٹظ„", "ظ…ط§ظٹ", "ط¬ظˆط§ظ†",
        "ط¬ظˆظٹظ„ظٹط©", "ط£ظˆطھ", "ط³ط¨طھظ…ط¨ط±", "ط£ظƒطھظˆط¨ط±", "ظ†ظˆظپظ…ط¨ط±", "ط¯ظٹط³ظ…ط¨ط±"
    )
    val arabicDays = arrayOf(
        "ط§ظ„ظٹظˆظ…", "ط£ظ…ط³", "ظ‚ط¨ظ„ ظٹظˆظ…ظٹظ†", "ظ‚ط¨ظ„ 3 ط£ظٹط§ظ…", "ظ‚ط¨ظ„ 4 ط£ظٹط§ظ…", "ظ‚ط¨ظ„ 5 ط£ظٹط§ظ…", "ظ‚ط¨ظ„ 6 ط£ظٹط§ظ…"
    )
    val arabicWeeks = arrayOf(
        "ط§ظ„ط£ط³ط¨ظˆط¹ ط§ظ„ط­ط§ظ„ظٹ", "ط§ظ„ط£ط³ط¨ظˆط¹ ط§ظ„ظ…ط§ط¶ظٹ", "ظ‚ط¨ظ„ ط£ط³ط¨ظˆط¹ظٹظ†", "ظ‚ط¨ظ„ 3 ط£ط³ط§ط¨ظٹط¹"
    )

    val centerLabel = when (uiState.selectedPeriod) {
        "ALL"   -> "ظƒظ„ ط§ظ„ط£ظˆظ‚ط§طھ"
        "DAY"   -> arabicDays.getOrElse(uiState.selectedDayOffset) { "ط§ظ„ظٹظˆظ…" }
        "WEEK"  -> arabicWeeks.getOrElse(uiState.selectedWeekOffset) { "ط§ظ„ط£ط³ط¨ظˆط¹ ط§ظ„ط­ط§ظ„ظٹ" }
        "MONTH" -> "${arabicMonths.getOrElse(uiState.selectedMonth) { arabicMonths[0] }} ${uiState.selectedYear}"
        "YEAR"  -> "${uiState.selectedYear}"
        else    -> ""
    }
    val canGoNext = when (uiState.selectedPeriod) {
        "ALL"   -> false
        "DAY"   -> uiState.selectedDayOffset > 0
        "WEEK"  -> uiState.selectedWeekOffset > 0
        "MONTH" -> {
            val now = java.util.Calendar.getInstance()
            uiState.selectedMonth < now.get(java.util.Calendar.MONTH) ||
                uiState.selectedYear < now.get(java.util.Calendar.YEAR)
        }
        "YEAR"  -> uiState.selectedYear < java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        else    -> false
    }
    val canGoPrev = when (uiState.selectedPeriod) {
        "ALL"   -> false
        "DAY"   -> uiState.selectedDayOffset < 6
        "WEEK"  -> uiState.selectedWeekOffset < 3
        "MONTH" -> true
        "YEAR"  -> uiState.selectedYear > (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 4)
        else    -> false
    }

    val periods = listOf(
        "ALL"   to "ط§ظ„ظƒظ„",
        "DAY"   to "ظٹظˆظ…",
        "WEEK"  to "ط£ط³ط¨ظˆط¹",
        "MONTH" to "ط´ظ‡ط±",
        "YEAR"  to "ط³ظ†ط©"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Period mode pills row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                    RoundedCornerShape(16.dp)
                )
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            periods.forEach { (key, label) ->
                val isSelected = uiState.selectedPeriod == key
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = tween(200), label = "pill_$key"
                )
                val pillText by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    animationSpec = tween(200), label = "pillText_$key"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(pillBg)
                        .clickable { onPeriodChange(key) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = pillText
                    )
                }
            }
        }

        // Navigation row: [>] [Center Label] [<]  (RTL: ChevronRight = go back, ChevronLeft = go forward)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // RTL: ChevronRight = go backward in time (prev)
                IconButton(
                    onClick = onPrev,
                    enabled = canGoPrev,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (canGoPrev) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.06f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "ط§ظ„ط³ط§ط¨ظ‚",
                        tint = if (canGoPrev) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = centerLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            text = when (uiState.selectedPeriod) {
                                "ALL"   -> "ط¥ط­طµط§ط¦ظٹط§طھ ظƒظ„ ط§ظ„ط£ظˆظ‚ط§طھ"
                                "DAY"   -> "ط¥ط­طµط§ط¦ظٹط§طھ ط§ظ„ظٹظˆظ…"
                                "WEEK"  -> "ط¥ط­طµط§ط¦ظٹط§طھ ط§ظ„ط£ط³ط¨ظˆط¹"
                                "MONTH" -> "ط¥ط­طµط§ط¦ظٹط§طھ ط§ظ„ط´ظ‡ط±"
                                "YEAR"  -> "ط¥ط­طµط§ط¦ظٹط§طھ ط§ظ„ط³ظ†ط©"
                                else    -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                }

                // RTL: ChevronLeft = go forward in time (next)
                IconButton(
                    onClick = onNext,
                    enabled = canGoNext,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (canGoNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.06f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "ط§ظ„طھط§ظ„ظٹ",
                        tint = if (canGoNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SalaryCycleProjectionCard(
    uiState: AnalyticsUiState,
    onHelpClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.hasSalarySource) "طھظˆظ‚ط¹ط§طھ ط§ظ„ط¥ظ†ظپط§ظ‚ ظˆط¯ظˆط±ط© ط§ظ„ط±ط§طھط¨ CCP" else "طھظˆظ‚ط¹ط§طھ ط§ظ„ط¥ظ†ظپط§ظ‚ ظ„ظ„ط´ظ‡ط± ط§ظ„ط­ط§ظ„ظٹ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.hasSalarySource) "ط¯ظˆط±ط© ط§ظ„ط±ط§طھط¨: ${uiState.salaryCycleStartLabel} â†گ ${uiState.salaryCycleEndLabel}" 
                                   else "ط§ظ„ظپطھط±ط©: 1 ط¥ظ„ظ‰ ظ†ظ‡ط§ظٹط© ط§ظ„ط´ظ‡ط±",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    HelpIconButton(
                        onClick = {
                            onHelpClick(
                                "طھظˆظ‚ط¹ط§طھ ط§ظ„ط¥ظ†ظپط§ظ‚ ظˆط¯ظˆط±ط© ط§ظ„ط±ط§طھط¨",
                                "ظٹظ‚ظٹط³ ظ‡ط°ط§ ط§ظ„ظ…ط¤ط´ط± ط³ط±ط¹ط© ظˆطھظ‚ط¯ظ… ظ…ط¹ط¯ظ„ ط¥ظ†ظپط§ظ‚ظƒ ط§ظ„ظٹظˆظ…ظٹ ط§ظ„ظپط¹ظ„ظٹ ظˆظ…ظ‚ط§ط±ظ†طھظ‡ ط¨ط§ظ„ظ…ظٹط²ط§ظ†ظٹط© ط§ظ„ظ…ط­ط¯ط¯ط© ط£ظˆ ط§ظ„ط±ط§طھط¨ ط§ظ„ظ…ط±ط¬ط¹ظٹ ط¹ظ„ظ‰ ظ…ط¯ط§ط± ط£ظٹط§ظ… ط¯ظˆط±ط© ط§ظ„ط±ط§طھط¨ ط§ظ„ظ…ط§ظ„ظٹ ط§ظ„ظ…طھط¨ظ‚ظٹط©.\n\nط§ظ„ظپط§ط¦ط¯ط©: ظٹطھظ†ط¨ط£ ط¨ط¥ط¬ظ…ط§ظ„ظٹ ظ†ظپظ‚ط§طھظƒ ط¨ظ†ظ‡ط§ظٹط© ط§ظ„ط´ظ‡ط± ط§ظ„ط¬ط§ط±ظٹ ظˆظٹط­ط°ط±ظƒ ظ…ط¨ظƒط±ط§ظ‹ ط¥ط°ط§ ظƒظ†طھ ظ…طھط¬ظ‡ط§ظ‹ ظ„طھط¬ط§ظˆط² ط§ظ„ظ…ظٹط²ط§ظ†ظٹط© ظ„طھطھظ…ظƒظ† ظ…ظ† طھط±ط´ظٹط¯ ظ†ظپظ‚ط§طھظƒ ظˆطھط¹ط¯ظٹظ„ ط³ظ„ظˆظƒ ط§ظ„ط§ط³طھظ‡ظ„ط§ظƒ ظ‚ط¨ظ„ ظپظˆط§طھ ط§ظ„ط£ظˆط§ظ†."
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.isProjectedToExceedBudget) ExpenseRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (uiState.isProjectedToExceedBudget) "طھظ†ط¨ظٹظ‡ ط¨ط§ظ„طµط±ظپ" else "ط¥ظ†ظپط§ظ‚ ظ…ط³طھظ‚ط±",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.isProjectedToExceedBudget) ExpenseRed else IncomeGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Bar and Info
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "طھظ‚ط¯ظ… ط§ظ„ط¯ظˆط±ط© ط§ظ„ط²ظ…ظ†ظٹط© (${((uiState.salaryCyclePercentageElapsed) * 100).toInt()}% ظ…ظ† ط§ظ„ط´ظ‡ط±)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Text(
                        text = "ظ…طھط¨ظ‚ظٹ ${uiState.daysRemainingInCycle} ظٹظˆظ…",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { uiState.salaryCyclePercentageElapsed },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Projections grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ط§ظ„ط¥ظ†ظپط§ظ‚ ط§ظ„ظ…طھظˆظ‚ط¹",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatterUtils.formatCurrency(uiState.projectedEndMonthSpending),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (uiState.isProjectedToExceedBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.referenceBudget == uiState.salaryAmount) "ظ…ظٹط²ط§ظ†ظٹط© ط§ظ„ط±ط§طھط¨ ط§ظ„ظ…ط±ط¬ط¹ظٹط©" else "ط¥ط¬ظ…ط§ظ„ظٹ ط§ظ„ظ…ظٹط²ط§ظ†ظٹط§طھ ط§ظ„ظ…ط­ط¯ط¯ط©",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.referenceBudget > 0) FormatterUtils.formatCurrency(uiState.referenceBudget) else "ط؛ظٹط± ظ…ط­ط¯ط¯ط©",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Exceed Warning Banner
            if (uiState.isProjectedToExceedBudget && uiState.referenceBudget > 0) {
                val exceedAmount = uiState.projectedEndMonthSpending - uiState.referenceBudget
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ExpenseRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(ExpenseRed, CircleShape)
                        )
                        Text(
                            text = "ط¨ظ†ط§ط،ظ‹ ط¹ظ„ظ‰ ط³ط±ط¹ط© ط¥ظ†ظپط§ظ‚ظƒطŒ ط³طھطھط¬ط§ظˆط² ط§ظ„ظ…ظٹط²ط§ظ†ظٹط© ط¨ظ€ ${FormatterUtils.formatCurrency(exceedAmount)} ط¨ظ†ظ‡ط§ظٹط© ط§ظ„ط´ظ‡ط±. ط­ط§ظˆظ„ طھط±ط´ظٹط¯ ظ†ظپظ‚ط§طھظƒ ط§ظ„ظƒط¨ظٹط±ط©.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeekendWeekdaySpendingCard(
    uiState: AnalyticsUiState,
    onHelpClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.hasWeekendData) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "طھط­ظ„ظٹظ„ ط§ظ„ط¥ظ†ظپط§ظ‚: ط£ظٹط§ظ… ط§ظ„ط¹ظ…ظ„ ظ…ظ‚ط§ط¨ظ„ ظ†ظ‡ط§ظٹط© ط§ظ„ط£ط³ط¨ظˆط¹",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "طھظˆط²ظٹط¹ ط§ظ„ظ†ظپظ‚ط§طھ ط¨ظٹظ† ط£ظٹط§ظ… ط§ظ„ط£ط³ط¨ظˆط¹ ظˆط§ظ„ط¬ظ…ط¹ط©/ط§ظ„ط³ط¨طھ ط¨ط§ظ„ط¬ط²ط§ط¦ط±",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                HelpIconButton(
                    onClick = {
                        onHelpClick(
                            "طھط­ظ„ظٹظ„ ط£ظٹط§ظ… ط§ظ„ط¹ظ…ظ„ ظ…ظ‚ط§ط¨ظ„ ط¹ط·ظ„ط© ظ†ظ‡ط§ظٹط© ط§ظ„ط£ط³ط¨ظˆط¹",
                            "ظٹظ‚ظٹط³ ظ‡ط°ط§ ط§ظ„طھط­ظ„ظٹظ„ ظ…طھظˆط³ط· ط­ط¬ظ… ط¥ظ†ظپط§ظ‚ظƒ ط§ظ„ظ…ط§ظ„ظٹ ظپظٹ ط£ظٹط§ظ… ط§ظ„ط£ط³ط¨ظˆط¹ ط§ظ„ط¹ط§ط¯ظٹط© (ظ…ظ† ط§ظ„ط£ط­ط¯ ط¥ظ„ظ‰ ط§ظ„ط®ظ…ظٹط³) ظ…ظ‚ط§ط±ظ†ط© ط¨ظ…طھظˆط³ط· ط¥ظ†ظپط§ظ‚ظƒ ظپظٹ ط¹ط·ظ„ط© ظ†ظ‡ط§ظٹط© ط§ظ„ط£ط³ط¨ظˆط¹ (ط§ظ„ط¬ظ…ط¹ط© ظˆط§ظ„ط³ط¨طھ ط¨ط§ظ„ط¬ط²ط§ط¦ط±).\n\nط§ظ„ظپط§ط¦ط¯ط©: ظٹظˆط¶ط­ ظ„ظƒ ط³ظ„ظˆظƒظƒ ط§ظ„طھط±ظپظٹظ‡ظٹ ط£ظˆ ط§ظ„ط§ط³طھظ‡ظ„ط§ظƒظٹ ط®ظ„ط§ظ„ ط§ظ„ط¹ط·ظ„ط§طھطŒ ظ…ظ…ط§ ظٹط³ط§ط¹ط¯ظƒ ط¹ظ„ظ‰ ظƒط¨ط­ ط§ظ„ظ…طµط§ط±ظٹظپ ط؛ظٹط± ط§ظ„ط¶ط±ظˆط±ظٹط© ط£ظˆ ط§ظ„ظ…ط¨ط§ظ„ط؛ ظپظٹظ‡ط§ ط®ظ„ط§ظ„ ط¹ط·ظ„ط© ظ†ظ‡ط§ظٹط© ط§ظ„ط£ط³ط¨ظˆط¹."
                        )
                    }
                )
            }

            // Ratio Bar Indicator
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ط£ظٹط§ظ… ط§ظ„ط¹ظ…ظ„ (${(uiState.weekdayPercentage * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "ظ†ظ‡ط§ظٹط© ط§ظ„ط£ط³ط¨ظˆط¹ (${(uiState.weekendPercentage * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavingsAmber
                    )
                }

                // Dual progress bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    if (uiState.weekdayPercentage > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(uiState.weekdayPercentage.coerceAtLeast(0.01f))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (uiState.weekendPercentage > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(uiState.weekendPercentage.coerceAtLeast(0.01f))
                                .background(SavingsAmber)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Averages Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ظ…ط¹ط¯ظ„ ط£ظٹط§ظ… ط§ظ„ط¹ظ…ظ„ ط§ظ„ظٹظˆظ…ظٹ", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatterUtils.formatCurrency(uiState.weekdayDailyAverage),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(SavingsAmber, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ظ…ط¹ط¯ظ„ ظ†ظ‡ط§ظٹط© ط§ظ„ط£ط³ط¨ظˆط¹ ط§ظ„ظٹظˆظ…ظٹ", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatterUtils.formatCurrency(uiState.weekendDailyAverage),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Insights text
            val trendMessage = if (uiState.weekendDailyAverage > uiState.weekdayDailyAverage * 1.3) {
                "ظ…ط¹ط¯ظ„ ط¥ظ†ظپط§ظ‚ظƒ ط§ظ„ظٹظˆظ…ظٹ ظپظٹ ط¹ط·ظ„ط© ظ†ظ‡ط§ظٹط© ط§ظ„ط£ط³ط¨ظˆط¹ ظ…ط±طھظپط¹ ط¬ط¯ط§ظ‹ ظ…ظ‚ط§ط±ظ†ط© ط¨ط£ظٹط§ظ… ط§ظ„ط¹ظ…ظ„. ط§ط­ط°ط± ظ…ظ† ط§ظ„طھط¨ط°ظٹط± ط®ظ„ط§ظ„ ط§ظ„ط¹ط·ظ„ط©."
            } else {
                "ظ…ط¹ط¯ظ„ ط¥ظ†ظپط§ظ‚ظƒ ط§ظ„ظٹظˆظ…ظٹ ظ…طھط²ظ† ط¨ظٹظ† ط£ظٹط§ظ… ط§ظ„ط¹ظ…ظ„ ظˆط¹ط·ظ„ط© ظ†ظ‡ط§ظٹط© ط§ظ„ط£ط³ط¨ظˆط¹. ط§ط³طھظ…ط± ظپظٹ ظ‡ط°ط§ ط§ظ„ط§ظ†ط¶ط¨ط§ط· ط§ظ„ظ…ط§ظ„ظٹ."
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    text = trendMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun EmergencyFundRunwayCard(
    uiState: AnalyticsUiState,
    onHelpClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusLabel, statusColor) = when (uiState.emergencyFundStatus) {
        "CRITICAL" -> "ط­ط±ظگط¬ (ط£ظ‚ظ„ ظ…ظ† ط´ظ‡ط±)" to ExpenseRed
        "ACCEPTABLE" -> "ظ…ظ‚ط¨ظˆظ„ (1-3 ط£ط´ظ‡ط±)" to SavingsAmber
        "SAFE" -> "ط¢ظ…ظ† (3-6 ط£ط´ظ‡ط±)" to IncomeGreen
        else -> "ظ…ظ…طھط§ط² (+6 ط£ط´ظ‡ط±)" to MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ظ…ط¤ط´ط± طµظ†ط¯ظˆظ‚ ط§ظ„ط·ظˆط§ط±ط¦ ظˆط§ظ„ط£ظ…ط§ظ† ط§ظ„ظ…ط§ظ„ظٹ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ظ‚ط¯ط±طھظƒ ط¹ظ„ظ‰ ط§ظ„طµظ…ظˆط¯ ط§ظ„ظ…ط§ظ„ظٹ ظپظٹ ط­ط§ظ„ ط§ظ†ظ‚ط·ط§ط¹ ط§ظ„ط¯ط®ظ„",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    HelpIconButton(
                        onClick = {
                            onHelpClick(
                                "ظ…ط¤ط´ط± طµظ†ط¯ظˆظ‚ ط§ظ„ط·ظˆط§ط±ط¦ ظˆط§ظ„ط£ظ…ط§ظ† ط§ظ„ظ…ط§ظ„ظٹ",
                                "ظٹظ‚ظٹط³ ظ‡ط°ط§ ط§ظ„ظ…ط¤ط´ط± ط¹ط¯ط¯ ط§ظ„ط£ط´ظ‡ط± ط§ظ„ط§ظپطھط±ط§ط¶ظٹط© ط§ظ„طھظٹ ظٹظ…ظƒظ†ظƒ ط§ظ„ط¹ظٹط´ ظپظٹظ‡ط§ ظ…ط¹طھظ…ط¯ط§ظ‹ ط¨ط§ظ„ظƒط§ظ…ظ„ ط¹ظ„ظ‰ ظ…ط¯ط®ط±ط§طھظƒ ط§ظ„ط­ط§ظ„ظٹط© ظ„طھط؛ط·ظٹط© ظ…طھظˆط³ط· ظ†ظپظ‚ط§طھظƒ ط§ظ„ط´ظ‡ط±ظٹط© ط¥ط°ط§ ط§ظ†ظ‚ط·ط¹ ط¯ط®ظ„ظƒ ظپط¬ط£ط©.\n\nط§ظ„ظپط§ط¦ط¯ط©: ظٹظˆظپط± ظ…ظ‚ظٹط§ط³ط§ظ‹ ط­ظ‚ظٹظ‚ظٹط§ظ‹ ظ„ظ…ط¯ظ‰ ط£ظ…ط§ظ†ظƒ ط§ظ„ظ…ط§ظ„ظٹ ظˆطµظ…ظˆط¯ظƒ ط£ظ…ط§ظ… ط§ظ„ط£ط²ظ…ط§طھ ط§ظ„ظ…ظپط§ط¬ط¦ط© (ظ…ط«ظ„ ظپظ‚ط¯ط§ظ† ط§ظ„ط¹ظ…ظ„ ط£ظˆ ط§ظ„ط·ظˆط§ط±ط¦ ط§ظ„طµط­ظٹط©) ط¯ظˆظ† ط§ظ„ط­ط§ط¬ط© ظ„ظ„ط§ظ‚طھط±ط§ط¶ ط£ظˆ ط§ظ„ط¯ظٹظˆظ†."
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large numeric representation
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", uiState.emergencyFundRunwayMonths)} ط£ط´ظ‡ط±",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        ),
                        color = statusColor
                    )
                    Text(
                        text = "ظ…ط¯ط© طھط؛ط·ظٹط© ط§ظ„ط·ظˆط§ط±ط¦",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )

                // Details
                Column(
                    modifier = Modifier.weight(2f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ط¥ط¬ظ…ط§ظ„ظٹ ط§ظ„ظ…ط¯ط®ط±ط§طھ:", style = MaterialTheme.typography.labelSmall, color = TextGray)
                        Text(
                            text = FormatterUtils.formatCurrency(uiState.totalSavingsAmount),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ظ…ط¹ط¯ظ„ ط§ظ„ظ…طµط§ط±ظٹظپ ط§ظ„ط´ظ‡ط±ظٹ:", style = MaterialTheme.typography.labelSmall, color = TextGray)
                        Text(
                            text = FormatterUtils.formatCurrency(uiState.averageMonthlyExpense),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            val explanation = when (uiState.emergencyFundStatus) {
                "CRITICAL" -> "طµظ†ط¯ظˆظ‚ ط·ظˆط§ط±ط¦ظƒ ط§ظ„ط­ط§ظ„ظٹ ط¶ط¹ظٹظپ ط¬ط¯ط§ظ‹ ظˆظ„ط§ ظٹط؛ط·ظٹ ط´ظ‡ط± ظˆط§ط­ط¯ ظ…ظ† ط§ظ„ظ…طµط§ط±ظٹظپ. ظ†ظ†طµط­ظƒ ط¨طھط­ظˆظٹظ„ ظ…ط¨ط§ظ„ط؛ ط¥ط¶ط§ظپظٹط© ظ„ظ„طھظˆظپظٹط± ظپظٹ ط£ظ‚ط±ط¨ ظˆظ‚طھ ظ„طھظپط§ط¯ظٹ ط§ظ„ط£ط²ظ…ط§طھ ط§ظ„ظ…ط§ظ„ظٹط© ط§ظ„ظ…ظپط§ط¬ط¦ط©."
                "ACCEPTABLE" -> "ظ…ط¯ط®ط±ط§طھظƒ ظ…ظ‚ط¨ظˆظ„ط© ظˆطھط؛ط·ظٹ ط¬ط²ط،ط§ظ‹ ظ…ظ† ط§ط­طھظٹط§ط¬ط§طھظƒ ط§ظ„ظ…ط¤ظ‚طھط©. ظٹظڈظ†طµط­ ط¨ط§ظ„ط¹ظ…ظ„ ط¹ظ„ظ‰ ط²ظٹط§ط¯ط© ط§ظ„ظ…ط¯ط®ط±ط§طھ ظ„طھطµظ„ ط¥ظ„ظ‰ طھط؛ط·ظٹط© 3 ط£ط´ظ‡ط± ط¹ظ„ظ‰ ط§ظ„ط£ظ‚ظ„ ظ„طھط­ظ‚ظٹظ‚ ط£ظ…ط§ظ† ط£ظƒط¨ط±."
                "SAFE" -> "طھظ‡ط§ظ†ظٹظ†ط§! ظˆط¶ط¹ظƒ ط§ظ„ظ…ط§ظ„ظٹ ط¢ظ…ظ† ط¬ط¯ط§ظ‹. طھط؛ط·ظٹ ظ…ط¯ط®ط±ط§طھظƒ ظ†ظپظ‚ط§طھظƒ ظ„ط¹ط¯ط© ط£ط´ظ‡ط± ظپظٹ ط­ط§ظ„ ط§ظ„ط·ظˆط§ط±ط¦طŒ ظˆظ‡ظˆ ظ…ط§ ظٹظ…ظ†ط­ظƒ ط±ط§ط­ط© ط¨ط§ظ„ ظ…ظ…طھط§ط²ط© ظ„ظ„طھط¹ط§ظ…ظ„ ظ…ط¹ ط£ظٹ ط·ط§ط±ط¦."
                else -> "ظˆط¶ط¹ظƒ ط§ظ„ظ…ط§ظ„ظٹ ط§ط³طھط«ظ†ط§ط¦ظٹ ظˆظ…ظ…طھط§ط²! ظ„ط¯ظٹظƒ ظˆظپط±ط© ظ…ط§ظ„ظٹط© ظˆطµظ†ط¯ظˆظ‚ ط·ظˆط§ط±ط¦ طµظ„ط¨ ظ„ظ„ط؛ط§ظٹط© ظٹط؛ط·ظٹ ط£ظƒط«ط± ظ…ظ† 6 ط£ط´ظ‡ط± ظ…ظ† ط­ظٹط§طھظƒ ط¯ظˆظ† ط¯ط®ظ„. ط£ظ†طھ طھط³ظٹط± ط¨ط®ط·ظ‰ ط°ظ‡ط¨ظٹط©."
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = statusColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SavingsChallengesSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "طھط­ط¯ظٹط§طھ ط§ظ„ط§ط¯ط®ط§ط± ظˆط§ظ„طھط­ظپظٹط² ط§ظ„ظ…ط§ظ„ظٹ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ط´ط§ط±ظƒ ظپظٹ ط§ظ„طھط­ط¯ظٹط§طھ ظ„طھظ†ظ…ظٹط© ظ…ط¯ط®ط±ط§طھظƒ ط¨ط·ط±ظٹظ‚ط© طھظپط§ط¹ظ„ظٹط©",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "طھط­ط¯ظٹط§طھ ظ†ط´ط·ط©",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Challenge 1: 52-Week Challenge
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SavingsAmber.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = SavingsAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "طھط­ط¯ظٹ ط§ظ„ظ€ 52 ط£ط³ط¨ظˆط¹ط§ظ‹",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ط§ط¯ط®ط± ظ…ط¨ظ„ط؛ط§ظ‹ ظ…طھط²ط§ظٹط¯ط§ظ‹ ظƒظ„ ط£ط³ط¨ظˆط¹",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }
                        
                        Text(
                            text = "ط§ظ„ط£ط³ط¨ظˆط¹ 12 / 52",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SavingsAmber
                        )
                    }

                    // Progress indicators
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ط§ظ„ظ…ط¨ظ„ط؛ ط§ظ„ظ…ظˆظپط±: 6,000 ط¯ط¬",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Text(
                                text = "ط§ظ„ظ‡ط¯ظپ: 26,000 ط¯ط¬",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        LinearProgressIndicator(
                            progress = { 6000f / 26000f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = SavingsAmber,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                        )
                    }

                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "طھظ… ط¥ط¶ط§ظپط© 500 ط¯ط¬ ظ„طھط­ط¯ظٹ ط§ظ„ط§ط¯ط®ط§ط± ط§ظ„ط£ط³ط¨ظˆط¹ظٹ ط¨ظ†ط¬ط§ط­! ظˆط§طµظ„ ط§ظ„طھظ‚ط¯ظ… ًںژ¯",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SavingsAmber)
                    ) {
                        Text(
                            text = "+ ط§ط¯ط®ط± 500 ط¯ط¬ ظ„ظ‡ط°ط§ ط§ظ„ط£ط³ط¨ظˆط¹",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // Challenge 2: Spare Change Round-Up
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "طھط¬ظ…ظٹط¹ ط§ظ„ظپظƒط© (Spare Change)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "طھظ‚ط±ظٹط¨ ط§ظ„ظ…ط¹ط§ظ…ظ„ط§طھ ظ„ط£ظ‚ط±ط¨ 50 ط£ظˆ 100 ط¯ط¬ ظˆط§ط¯ط®ط§ط± ط§ظ„ظپط§ط±ظ‚",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }
                    }

                    // Stats Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ط§ظ„ظپظƒط© ط§ظ„ظ…ط¬ظ…ط¹ط© ظ‡ط°ط§ ط§ظ„ط´ظ‡ط±",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Text(
                                text = "1,450 ط¯ط¬",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = IncomeGreen
                            )
                        }

                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "طھظ… طھط­ظˆظٹظ„ 1,450 ط¯ط¬ ظ…ظ† ط§ظ„ظپظƒط© ط§ظ„ظ…ط¯ط®ط±ط© ط¥ظ„ظ‰ ط­ط³ط§ط¨ ط§ظ„طھظˆظپظٹط± ط¨ظ†ط¬ط§ط­! ًں’°",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(
                                text = "طھط­ظˆظٹظ„ ظ„ظ„ظ…ط¯ط®ط±ط§طھ",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimplePeriodEmptyState(
    selectedPeriod: String,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val periodLabel = when (selectedPeriod) {
        "DAY"   -> "ظ„ظ‡ط°ط§ ط§ظ„ظٹظˆظ…"
        "WEEK"  -> "ظ„ظ‡ط°ط§ ط§ظ„ط£ط³ط¨ظˆط¹"
        "MONTH" -> "ظ„ظ‡ط°ط§ ط§ظ„ط´ظ‡ط±"
        "YEAR"  -> "ظ„ظ‡ط°ظ‡ ط§ظ„ط³ظ†ط©"
        else    -> "ظ„ظ‡ط°ظ‡ ط§ظ„ظپطھط±ط©"
    }

    val periodIcon = when (selectedPeriod) {
        "YEAR"  -> Icons.Default.History
        else    -> Icons.Default.DateRange
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Primary.copy(alpha = 0.25f),
                    Primary.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Glowing Period-Specific Icon Container
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Primary.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = periodIcon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ط§ظ„ظ…ط®ط·ط· ط§ظ„ط¨ظٹط§ظ†ظٹ ظ‡ط§ط¯ط¦ $periodLabel",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ظ„ظ… طھظ‚ظ… ط¨طھط³ط¬ظٹظ„ ط£ظٹ ظ…ط¹ط§ظ…ظ„ط© $periodLabel. ط£ط¶ظپ ظ…ط¹ط§ظ…ظ„ط§طھظƒ ظ„طھظپط¹ظٹظ„ ط§ظ„طھط­ظ„ظٹظ„ط§طھ ظˆط§ظ„ط±ط³ظˆظ… ط§ظ„ط¨ظٹط§ظ†ظٹط© ط§ظ„طھظپط§ط¹ظ„ظٹط©.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Tips Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ًں’،",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "ظ†طµظٹط­ط©: ظٹظ…ظƒظ†ظƒ ط§ظ„طھظ†ظ‚ظ„ ط¨ظٹظ† ط§ظ„ظپطھط±ط§طھ ط§ظ„ط²ظ…ظ†ظٹط© ظ…ظ† ط§ظ„ط£ط¹ظ„ظ‰ ظ„ط±ط¤ظٹط© ط¥ط­طµط§ط¦ظٹط§طھ ط§ظ„ط£ظٹط§ظ… ط£ظˆ ط§ظ„ط£ط³ط§ط¨ظٹط¹ ط§ظ„ط£ط®ط±ظ‰ ط§ظ„ظ†ط´ط·ط©.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Call to Action
            Button(
                onClick = onAddTransactionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = "+ طھط³ط¬ظٹظ„ ظ…ط¹ط§ظ…ظ„ط© $periodLabel",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun HelpIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "طں",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

enum class ChartViewMode {
    DONUT,
    BAR
}

