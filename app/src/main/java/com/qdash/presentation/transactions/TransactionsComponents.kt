package com.qdash.presentation.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.*
import com.qdash.core.utils.FormatterUtils
import com.qdash.domain.model.TransactionType
import com.qdash.presentation.navigation.Screen
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalDensity
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.Category


// â”€â”€ Summary Mini Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun SummaryMiniCard(
    label: String,
    amount: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Md,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextGray,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = FormatterUtils.formatCurrency(amount),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SummaryMiniCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(72.dp)
            .shimmerEffect(ShapeTokens.Md)
    )
}

@Composable
fun TransactionItemSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shimmerEffect(ShapeTokens.Md)
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FinancialActivityCalendar(
    uiState: TransactionsUiState,
    viewModel: TransactionsViewModel,
    listState: LazyListState,
    onDayDoubleTapped: (Long) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    val monthsArabic = com.qdash.core.utils.FormatterUtils.getMonthNamesList()

    
    val daysHeader = listOf("س", "ح", "ن", "ث", "ر", "خ", "ج")
    
    val calendar = remember(uiState.visibleYear, uiState.visibleMonth) {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, uiState.visibleYear)
            set(java.util.Calendar.MONTH, uiState.visibleMonth)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    val firstDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    
    val startOffset = when (firstDayOfWeek) {
        java.util.Calendar.SATURDAY -> 0
        java.util.Calendar.SUNDAY -> 1
        java.util.Calendar.MONDAY -> 2
        java.util.Calendar.TUESDAY -> 3
        java.util.Calendar.WEDNESDAY -> 4
        java.util.Calendar.THURSDAY -> 5
        java.util.Calendar.FRIDAY -> 6
        else -> 0
    }
    
    val totalCells = startOffset + daysInMonth
    val rowsCount = (totalCells + 6) / 7
    
    val activeColor = when (uiState.selectedMetricMode) {
        "COUNT" -> Primary
        "EXPENSE" -> ExpenseRed
        "INCOME" -> IncomeGreen
        "SCORE" -> SavingsAmber
        "CASHFLOW" -> TransferBlue
        else -> Primary
    }
    
    val maxVal = remember(uiState.dailyAggregates, uiState.selectedMetricMode) {
        uiState.dailyAggregates.maxOfOrNull { agg ->
            when (uiState.selectedMetricMode) {
                "COUNT" -> agg.transactionCount.toDouble()
                "EXPENSE" -> agg.totalExpense
                "INCOME" -> agg.totalIncome
                "SCORE" -> agg.activityScore
                "CASHFLOW" -> kotlin.math.abs(agg.netCashflow)
                else -> 0.0
            }
        } ?: 0.0
    }
    
    val lnMax = kotlin.math.ln(maxVal + 1.0)
    
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.OUTLINED,
        shape = ShapeTokens.Lg,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newMonth = if (uiState.visibleMonth == 0) 11 else uiState.visibleMonth - 1
                        val newYear = if (uiState.visibleMonth == 0) uiState.visibleYear - 1 else uiState.visibleYear
                        viewModel.onCalendarMonthChanged(newYear, newMonth)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "الشهر السابق",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = "${monthsArabic[uiState.visibleMonth]} ${uiState.visibleYear}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(
                    onClick = {
                        val newMonth = if (uiState.visibleMonth == 11) 0 else uiState.visibleMonth + 1
                        val newYear = if (uiState.visibleMonth == 11) uiState.visibleYear + 1 else uiState.visibleYear
                        viewModel.onCalendarMonthChanged(newYear, newMonth)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "الشهر التالي",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysHeader.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = TextGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val navController = com.qdash.presentation.navigation.LocalNavController.current
                for (row in 0 until rowsCount) {
                    var selectedColInThisRow: Int? = null
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - startOffset + 1
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val dayTimestamp = remember(dayNumber) {
                                        java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.YEAR, uiState.visibleYear)
                                            set(java.util.Calendar.MONTH, uiState.visibleMonth)
                                            set(java.util.Calendar.DAY_OF_MONTH, dayNumber)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    }
                                    
                                    val isSelected = uiState.selectedCalendarDate == dayTimestamp
                                    if (isSelected) {
                                        selectedColInThisRow = col
                                    }
                                    val dayAggregate = uiState.dailyAggregates.find { it.localDateTimestamp == dayTimestamp }
                                    
                                    val metricValue = when (uiState.selectedMetricMode) {
                                        "COUNT" -> dayAggregate?.transactionCount?.toDouble() ?: 0.0
                                        "EXPENSE" -> dayAggregate?.totalExpense ?: 0.0
                                        "INCOME" -> dayAggregate?.totalIncome ?: 0.0
                                        "SCORE" -> dayAggregate?.activityScore ?: 0.0
                                        "CASHFLOW" -> dayAggregate?.netCashflow ?: 0.0
                                        else -> 0.0
                                    }
                                    
                                    val metricValueForScale = if (uiState.selectedMetricMode == "CASHFLOW") {
                                        kotlin.math.abs(metricValue)
                                    } else {
                                        metricValue
                                    }
                                    
                                    val cellActiveColor = if (uiState.selectedMetricMode == "CASHFLOW") {
                                        when {
                                            metricValue > 0.0 -> IncomeGreen
                                            metricValue < 0.0 -> ExpenseRed
                                            else -> TextGray
                                        }
                                    } else {
                                        activeColor
                                    }
                                    
                                    val lnVal = kotlin.math.ln(metricValueForScale + 1.0)
                                    val ratio = if (lnMax > 0.0) lnVal / lnMax else 0.0
                                    
                                    val (bubbleSize, showGlow) = when {
                                        metricValueForScale == 0.0 -> Pair(4.dp, false)
                                        ratio <= 0.25 -> Pair(12.dp, false)
                                        ratio <= 0.60 -> Pair(20.dp, false)
                                        ratio <= 0.90 -> Pair(28.dp, false)
                                        else -> Pair(36.dp, true)
                                    }
                                    
                                    val alphaVal = when {
                                        metricValueForScale == 0.0 -> 0.15f
                                        ratio <= 0.25 -> 0.35f
                                        ratio <= 0.60 -> 0.60f
                                        ratio <= 0.90 -> 0.85f
                                        else -> 1.0f
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelected) {
                                                        viewModel.onCalendarDateSelected(null)
                                                    } else {
                                                        viewModel.onCalendarDateSelected(dayTimestamp)
                                                    }
                                                },
                                                onDoubleClick = {
                                                    onDayDoubleTapped(dayTimestamp)
                                                }
                                            )
                                            .background(
                                                color = if (isSelected) cellActiveColor.copy(alpha = 0.12f) else Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Surface(
                                                modifier = Modifier.size(36.dp),
                                                shape = CircleShape,
                                                color = Color.Transparent,
                                                border = BorderStroke(1.5.dp, cellActiveColor)
                                            ) {}
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(bubbleSize)
                                                .clip(CircleShape)
                                                .background(
                                                    color = if (metricValueForScale == 0.0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else cellActiveColor.copy(alpha = alphaVal)
                                                )
                                        ) {
                                            if (showGlow && !isSelected) {
                                                Surface(
                                                    modifier = Modifier.fillMaxSize(),
                                                    shape = CircleShape,
                                                    color = Color.Transparent,
                                                    border = BorderStroke(1.dp, cellActiveColor.copy(alpha = 0.5f))
                                                ) {}
                                            }
                                        }
                                        
                                        Text(
                                            text = "$dayNumber",
                                            color = when {
                                                metricValueForScale > 0.0 -> Color.White
                                                isSelected -> MaterialTheme.colorScheme.onSurface
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (metricValueForScale > 0.0 || isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(16.dp),
                                            shape = CircleShape,
                                            color = Color.Transparent,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        ) {}
                                    }
                                }
                            }
                        }
                    }

                    // Anchored capsule under selected day circle row
                    if (selectedColInThisRow != null) {
                        val coroutineScope = rememberCoroutineScope()
                        val horizontalBias = -1f + (selectedColInThisRow / 3f)
                        
                        // Select a premium, dynamic entrance/exit transition depending on cell position
                        // Saturday is index 0 (far right), Sunday is 1 (right)
                        // Friday is index 6 (far left), Thursday is 5 (left)
                        val enterTransition = remember(selectedColInThisRow) {
                            when (selectedColInThisRow) {
                                0 -> slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                1 -> slideInHorizontally(
                                    initialOffsetX = { it / 2 },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                5 -> slideInHorizontally(
                                    initialOffsetX = { -it / 2 },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                6 -> slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                                else -> slideInVertically(
                                    initialOffsetY = { -30 },
                                    animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + expandVertically(
                                    expandFrom = Alignment.Top,
                                    animationSpec = tween(durationMillis = 250)
                                ) + fadeIn(animationSpec = tween(durationMillis = 200))
                            }
                        }

                        val exitTransition = remember(selectedColInThisRow) {
                            when (selectedColInThisRow) {
                                0 -> slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                                1 -> slideOutHorizontally(
                                    targetOffsetX = { it / 2 },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                                5 -> slideOutHorizontally(
                                    targetOffsetX = { -it / 2 },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                                6 -> slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                                else -> slideOutVertically(
                                    targetOffsetY = { -30 },
                                    animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + shrinkVertically(
                                    shrinkTowards = Alignment.Top,
                                    animationSpec = tween(durationMillis = 200)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                            }
                        }

                        key(uiState.selectedCalendarDate) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = true,
                                enter = enterTransition,
                                exit = exitTransition,
                                modifier = Modifier
                                    .zIndex(10f)
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        // Report 0 height so it floats overlaying rows below it!
                                        layout(placeable.width, 0) {
                                            placeable.placeRelative(0, 0)
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = androidx.compose.ui.BiasAlignment(horizontalBias, -1f)
                                ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    // Arrow (connected directly to the capsule box)
                                    val arrowColor = Color(0xFF1E1E24)
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier.size(width = 14.dp, height = 7.dp)
                                    ) {
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(size.width / 2f, 0f)
                                            lineTo(size.width, size.height)
                                            lineTo(0f, size.height)
                                            close()
                                        }
                                        drawPath(path, color = arrowColor)
                                    }

                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    // Capsule container holding buttons side-by-side
                                    Surface(
                                        color = Color(0xFF1E1E24),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Plus Button (Button 1)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.15f))
                                                    .combinedClickable(
                                                        onClick = {
                                                            val selectedTs = uiState.selectedCalendarDate!!
                                                            val route = com.qdash.presentation.navigation.Screen.AddTransaction.createRoute("EXPENSE", null, selectedTs)
                                                            navController?.navigate(route)
                                                        },
                                                        onLongClick = {
                                                            android.widget.Toast.makeText(context, "أضف عملية في هذا اليوم", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Eye Button (Button 2)
                                            val currentSelectedTs = uiState.selectedCalendarDate
                                            val currentDayAggregate = uiState.dailyAggregates.find { it.localDateTimestamp == currentSelectedTs }
                                            val hasAnyActivity = currentDayAggregate != null && currentDayAggregate.transactionCount > 0

                                            if (hasAnyActivity) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.15f))
                                                        .combinedClickable(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    listState.animateScrollToItem(5)
                                                                }
                                                            },
                                                            onLongClick = {
                                                                android.widget.Toast.makeText(context, "\u0639\u0631\u0636\u0020\u0639\u0645\u0644\u064a\u0627\u062a\u0020\u0627\u0644\u064a\u0648\u0645", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Visibility,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
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
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableTransactionRow(
    transaction: Transaction,
    category: Category?,
    accountName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isSelectionActive: Boolean = false,
    onLongClick: () -> Unit = {},
    currentViewedAccountId: Long? = null,
    toAccountName: String? = null
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val minSwipeDistance = with(density) { 80.dp.toPx() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var hapticTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Background Actions Layer
        if (!isSelectionActive) {
            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit action (Right in RTL - Blue)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .background(TransferBlue)
                        .clickable { 
                            onEdit()
                            offsetX = 0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White)
                }

                // Delete action (Left in RTL - Red)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .background(ExpenseRed)
                        .clickable { 
                            onDelete()
                            offsetX = 0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.White)
                }
            }
        }

        // Foreground Content Layer
        val animOffset by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isSelectionActive) 0f else offsetX)
        Box(
            modifier = Modifier
                .offset(x = with(density) { animOffset.toDp() })
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(isSelectionActive) {
                    if (!isSelectionActive) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                offsetX = when {
                                    offsetX > minSwipeDistance * 0.5f -> minSwipeDistance
                                    offsetX < -minSwipeDistance * 0.5f -> -minSwipeDistance
                                    else -> 0f
                                }
                            },
                            onDragCancel = { offsetX = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                // Subtracting dragAmount to compensate for RTL layout coordinate inversion in Compose absolute offset.
                                // When dragging to the right (dragAmount > 0), we want the row to shift to the right, which translates to a negative offset in RTL.
                                offsetX = (offsetX - dragAmount).coerceIn(-minSwipeDistance, minSwipeDistance)
                                
                                val threshold = minSwipeDistance * 0.5f
                                val crossed = kotlin.math.abs(offsetX) >= threshold
                                if (crossed && !hapticTriggered) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    hapticTriggered = true
                                } else if (!crossed) {
                                    hapticTriggered = false
                                }
                            }
                        )
                    }
                }
        ) {
            TransactionItem(
                transaction = transaction,
                category = category,
                accountName = accountName,
                onClick = {
                    if (offsetX != 0f) {
                        offsetX = 0f
                    } else {
                        onClick()
                    }
                },
                isSelected = isSelected,
                onLongClick = onLongClick,
                currentViewedAccountId = currentViewedAccountId,
                toAccountName = toAccountName
            )
        }
    }
}


