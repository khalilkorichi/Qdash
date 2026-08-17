package com.qdash.presentation.analytics

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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.ui.draw.rotate
import com.qdash.ui.designsystem.components.AppBottomSheet
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.theme.*
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*
import com.qdash.domain.model.Category
import com.qdash.presentation.transactions.CategoryIconView
import com.qdash.presentation.navigation.LocalNavController
import com.qdash.presentation.navigation.Screen
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.qdash.domain.model.CardAiContext
import com.qdash.domain.model.CardAiContextType

private const val DONUT_SMALL_CATEGORY_THRESHOLD = 2000.0

@Composable
fun DonutChartSkeleton(modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
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
    AppCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                    text = "سجل إحصائياتك فارغ حالياً",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ابدأ بتسجيل مصاريفك ودخلك اليومي لتظهر هنا تحليلات ورسومات ذكّية تساعدك على فهم نمط إنفاقك وتوفير المزيد.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Dotted Preview Placeholders (Visual Promise)
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xl,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ما ستحصل عليه بعد تسجيل العمليات:",
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
                                text = "توزيع المصاريف حسب الفئات",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "رسم دائري تفاعلي يوضح الفئات الأكثر استهلاكاً لميزانيتك",
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
                                text = "مخطط التدفق النقدي التاريخي",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مقارنة بصرية ذكية وتفاعلية بين إجمالي الدخل والمصاريف شهرياً",
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
                                text = "تحليلات ومؤشرات ذكية",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "توقعات الإنفاق، أمان صندوق الطوارئ ومعدل الادخار",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppButton(
                onClick = onAddTransactionClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY,
                shape = ShapeTokens.Lg
            ) {
                Text(
                    text = "سجل أول معاملة الآن",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
data class HierarchicalCategoryShare(
    val rootCategory: Category?,
    val rootShare: CategoryShare,
    val subcategoryShares: List<CategoryShare>,
    val directShare: CategoryShare?,
    val isExpandable: Boolean = subcategoryShares.isNotEmpty()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveDonutCard(
    shares: List<CategoryShare>,
    selectedCategory: CategoryShare?,
    onSelectedCategoryChange: (CategoryShare?) -> Unit,
    onAiChatClick: (CardAiContext) -> Unit,
    onCategoryLongClick: (CategoryShare) -> Unit,
    categories: List<Category> = emptyList(),
    periodStart: Long = 0L,
    periodEnd: Long = Long.MAX_VALUE
) {
    val Primary = MaterialTheme.colorScheme.primary
    var viewMode by remember { mutableStateOf(ChartViewMode.DONUT) }
    var showOtherBottomSheet by remember { mutableStateOf(false) }
    var isLegendExpanded by remember { mutableStateOf(false) }
    var expandedCategoryIds by remember { mutableStateOf(setOf<Long>()) }

    // Cache total amount
    val totalAmount = remember(shares) { shares.sumOf { it.amount } }

    // Group categories hierarchically into root categories and their subcategories
    val hierarchicalShares = remember(shares, categories, totalAmount) {
        if (shares.isEmpty()) return@remember emptyList<HierarchicalCategoryShare>()

        val rootCategories = categories.filter { it.parentId == null }
        val categoryMap = categories.associateBy { it.id }

        val rootGroups = mutableMapOf<Long, MutableList<CategoryShare>>()
        val directShares = mutableMapOf<Long, CategoryShare>()
        val unmappedShares = mutableListOf<CategoryShare>()

        shares.forEach { share ->
            val cat = categoryMap[share.categoryId]
            if (cat != null) {
                if (cat.parentId == null) {
                    directShares[cat.id] = share
                    if (!rootGroups.containsKey(cat.id)) {
                        rootGroups[cat.id] = mutableListOf()
                    }
                } else {
                    val parentId = cat.parentId
                    val parentCat = categoryMap[parentId]
                    if (parentCat != null) {
                        rootGroups.getOrPut(parentId) { mutableListOf() }.add(share)
                    } else {
                        unmappedShares.add(share)
                    }
                }
            } else {
                unmappedShares.add(share)
            }
        }

        val result = mutableListOf<HierarchicalCategoryShare>()

        rootCategories.forEach { rootCat ->
            val subShares = (rootGroups[rootCat.id] ?: emptyList()).sortedByDescending { it.amount }
            val direct = directShares[rootCat.id]
            val totalRootAmount = (direct?.amount ?: 0.0) + subShares.sumOf { it.amount }

            if (totalRootAmount > 0.0) {
                val totalPct = if (totalAmount > 0.0) (totalRootAmount / totalAmount).toFloat() else 0f
                val rootShare = CategoryShare(
                    categoryId = rootCat.id,
                    categoryName = rootCat.name,
                    amount = totalRootAmount,
                    percentage = totalPct,
                    color = rootCat.color
                )
                val directSpendingShare = if (subShares.isNotEmpty() && direct != null && direct.amount > 0.0) {
                    CategoryShare(
                        categoryId = rootCat.id,
                        categoryName = "${rootCat.name} (عام)",
                        amount = direct.amount,
                        percentage = direct.percentage,
                        color = rootCat.color
                    )
                } else null

                result.add(
                    HierarchicalCategoryShare(
                        rootCategory = rootCat,
                        rootShare = rootShare,
                        subcategoryShares = subShares,
                        directShare = directSpendingShare,
                        isExpandable = subShares.isNotEmpty()
                    )
                )
            }
        }

        unmappedShares.forEach { orphanShare ->
            val cat = categoryMap[orphanShare.categoryId]
            result.add(
                HierarchicalCategoryShare(
                    rootCategory = cat,
                    rootShare = orphanShare,
                    subcategoryShares = emptyList(),
                    directShare = null,
                    isExpandable = false
                )
            )
        }

        result.sortedByDescending { it.rootShare.amount }
    }

    val rootShares = remember(hierarchicalShares) {
        hierarchicalShares.map { it.rootShare }
    }

    val smallShares = remember(rootShares) { rootShares.filter { it.amount <= DONUT_SMALL_CATEGORY_THRESHOLD } }
    val largeShares = remember(rootShares) { rootShares.filter { it.amount > DONUT_SMALL_CATEGORY_THRESHOLD } }

    val chartShares = remember(rootShares, smallShares, largeShares) {
        if (smallShares.size > 1) {
            val totalSmallAmount = smallShares.sumOf { it.amount }
            val totalSmallPercentage = smallShares.sumOf { it.percentage.toDouble() }.toFloat()
            val otherShare = CategoryShare(
                categoryId = -99L,
                categoryName = "فئات أخرى",
                amount = totalSmallAmount,
                percentage = totalSmallPercentage,
                color = "#9CA3AF"
            )
            largeShares + otherShare
        } else {
            rootShares
        }
    }

    // Bouncy animated progresses mapped to each category share to allow smooth transitions
    val animatedProgresses = chartShares.associate { share ->
        val isSelected = selectedCategory?.categoryName == share.categoryName ||
                selectedCategory?.categoryId == share.categoryId ||
                (share.categoryId == -99L && selectedCategory != null && smallShares.any { it.categoryId == selectedCategory.categoryId }) ||
                hierarchicalShares.any { h -> h.rootShare.categoryId == share.categoryId && h.subcategoryShares.any { it.categoryId == selectedCategory?.categoryId } }
        share.categoryName to animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "anim_${share.categoryName}"
        ).value
    }
    // Cache all share parsed colors once including synthetic and small shares and subcategories
    val parsedShareColors = remember(shares, rootShares, chartShares, categories) {
        val allShares = shares + rootShares + chartShares
        val map = allShares.associate { share ->
            share.categoryName to (try { Color(android.graphics.Color.parseColor(share.color)) } catch (e: Exception) { null })
        }.toMutableMap()
        categories.forEach { cat ->
            if (!map.containsKey(cat.name)) {
                map[cat.name] = try { Color(android.graphics.Color.parseColor(cat.color)) } catch (e: Exception) { null }
            }
        }
        map
    }
    // Cache bar chart gradients per root share
    val parsedBarBrushes = remember(rootShares) {
        rootShares.associate { share ->
            val c = try { Color(android.graphics.Color.parseColor(share.color)) } catch (e: Exception) { Color.Gray }
            share.categoryName to Brush.verticalGradient(colors = listOf(c, c.copy(alpha = 0.5f)))
        }
    }

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
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
                            text = "توزيع المصاريف حسب الفئة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تحليل وتوزيع النفقات تفصيلياً",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    CardAiChatButton(
                        onClick = {
                            val chartDataString = org.json.JSONArray().apply {
                                shares.forEach {
                                    put(org.json.JSONObject().apply {
                                        put("category", it.categoryName)
                                        put("amount", it.amount)
                                        put("percentage", it.percentage)
                                    })
                                }
                            }.toString()
                            
                            val cardContext = CardAiContext(
                                cardId = "donut_chart",
                                cardTitle = "توزيع المصاريف حسب الفئة",
                                cardType = CardAiContextType.DonutChart,
                                chartData = chartDataString,
                                periodStart = periodStart,
                                periodEnd = periodEnd,
                                tooltipContent = "يقيس هذا المخطط النسبة المئوية لإجمالي نفقاتك الموزعة على مختلف الفئات المالية (كالأغذية، النقل، الفواتير، إلخ) خلال الفترة الزمنية المحددة.\n\nالفائدة: يساعدك على رصد الفئات الرئيسية الأكثر استهلاكاً لسيولتك النقدية لتتمكن من اتخاذ قرارات واعية بكبح الصرف في الجوانب الترفيهية وزيادة مدخراتك."
                            )
                            onAiChatClick(cardContext)
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
                                contentDescription = "أعمدة",
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
                                contentDescription = "دائرة",
                                tint = if (viewMode == ChartViewMode.DONUT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))

            if (viewMode == ChartViewMode.DONUT) {
                val adjustedSweeps = remember(chartShares) {
                    val rawSweeps = chartShares.map { it.percentage * 360f }
                    val n = rawSweeps.size
                    if (n <= 1) return@remember rawSweeps
                    
                    val sweeps = rawSweeps.toFloatArray()
                    val minSweep = 25f // minimum sweep angle in degrees
                    
                    val isSmall = BooleanArray(n)
                    var smallCount = 0
                    var smallSum = 0f
                    for (i in 0 until n) {
                        if (sweeps[i] < minSweep) {
                            isSmall[i] = true
                            smallCount++
                            smallSum += sweeps[i]
                            sweeps[i] = minSweep
                        }
                    }
                    
                    if (smallCount > 0) {
                        val extraNeeded = (smallCount * minSweep) - smallSum
                        var largeSlicesSumBefore = 0f
                        for (i in 0 until n) {
                            if (!isSmall[i]) {
                                largeSlicesSumBefore += sweeps[i]
                            }
                        }
                        
                        if (largeSlicesSumBefore > 0f) {
                            for (i in 0 until n) {
                                if (!isSmall[i]) {
                                    val ratio = sweeps[i] / largeSlicesSumBefore
                                    sweeps[i] = (sweeps[i] - extraNeeded * ratio).coerceAtLeast(minSweep)
                                }
                            }
                        }
                    }
                    
                    val sum = sweeps.sum()
                    if (sum > 0f) {
                        for (i in 0 until n) {
                            sweeps[i] = (sweeps[i] / sum) * 360f
                        }
                    }
                    sweeps.toList()
                }

                // Donut Canvas — 240dp with premium visual layout and floating badge
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .pointerInput(chartShares, adjustedSweeps) {
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
                                            chartShares.forEachIndexed { idx, share ->
                                                val sweep = adjustedSweeps[idx]
                                                val center = currentAngle + sweep / 2f
                                                if (chartAngle >= currentAngle && chartAngle < currentAngle + sweep) {
                                                    bestMatch = share
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
                                            if (bestMatch != null) {
                                                if (bestMatch.categoryId == -99L) {
                                                    showOtherBottomSheet = true
                                                } else {
                                                    onCategoryLongClick(bestMatch)
                                                }
                                            }
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
                                                chartShares.forEachIndexed { idx, share ->
                                                    val sweep = adjustedSweeps[idx]
                                                    val center = currentAngle + sweep / 2f
                                                    if (chartAngle >= currentAngle && chartAngle < currentAngle + sweep) {
                                                        bestMatch = share
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
                                                if (bestMatch?.categoryId == -99L) {
                                                    showOtherBottomSheet = true
                                                } else if (bestMatch != null) {
                                                    // Toggle: tap selected -> deselect, tap other -> select & expand
                                                    val isAlreadySelected = selectedCategory?.categoryName == bestMatch.categoryName || selectedCategory?.categoryId == bestMatch.categoryId
                                                    val newSelection = if (isAlreadySelected) null else bestMatch
                                                    onSelectedCategoryChange(newSelection)
                                                    if (newSelection != null && hierarchicalShares.any { it.rootShare.categoryId == bestMatch.categoryId && it.isExpandable }) {
                                                        expandedCategoryIds = expandedCategoryIds + bestMatch.categoryId
                                                    }
                                                } else {
                                                    onSelectedCategoryChange(null)
                                                }
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
                        chartShares.forEachIndexed { index, share ->
                            val sweep = adjustedSweeps[index]
                            
                            val parseColor = parsedShareColors[share.categoryName] ?: themePrimaryColor
                            
                            val isSelected = selectedCategory?.categoryName == share.categoryName ||
                                    (share.categoryId == -99L && selectedCategory != null && smallShares.any { it.categoryId == selectedCategory.categoryId })
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
                            
                            val gap = if (chartShares.size > 1) 1.5f else 0f
                            val adjustedSweep = (sweep - gap).coerceAtLeast(0.5f)
                            
                            val maxCapAngle = adjustedSweep / 2f
                            val localCapAngle = capAngle.coerceAtMost(maxCapAngle)
                            
                            val drawStartAngle = currentAngle + gap / 2f + localCapAngle
                            val drawSweepAngle = (adjustedSweep - 2 * localCapAngle).coerceAtLeast(2f)
                            
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
                                text = cat.categoryName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray,
                                    fontSize = 9.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
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
                                "إجمالي المصاريف",
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
                    chartShares.forEach { share ->
                        val sweep = share.percentage * 360f
                        val isSelected = selectedCategory?.categoryName == share.categoryName ||
                                (share.categoryId == -99L && selectedCategory != null && smallShares.any { it.categoryId == selectedCategory.categoryId })
                        
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
                            
                            val parseColor = if (share.categoryId == -99L && selectedCategory != null) {
                                parsedShareColors[selectedCategory.categoryName] ?: Primary
                            } else {
                                parsedShareColors[share.categoryName] ?: Primary
                            }

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
                                            text = if (share.categoryId == -99L && selectedCategory != null) {
                                                "${selectedCategory.categoryName} \u2022 ${(selectedCategory.percentage * 100).toInt()}%"
                                            } else {
                                                "${share.categoryName} \u2022 ${(share.percentage * 100).toInt()}%"
                                            },
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
                    val totalBars = rootShares.size.coerceAtMost(6)
                    val barSpacing = 16.dp
                    
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        rootShares.take(6).forEach { share ->
                            val parseColor = parsedShareColors[share.categoryName] ?: Primary
                            val isSelected = selectedCategory?.categoryName == share.categoryName || selectedCategory?.categoryId == share.categoryId
                            val isActive = selectedCategory == null || isSelected

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectedCategoryChange(if (isSelected) null else share) }
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

            // Hierarchical Accordion Category Legend
            val legendHierarchicalShares = if (isLegendExpanded) hierarchicalShares else hierarchicalShares.take(5)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                legendHierarchicalShares.forEach { item ->
                    val share = item.rootShare
                    val rootCat = item.rootCategory
                    val parseColor = parsedShareColors[share.categoryName] ?: MaterialTheme.colorScheme.primary
                    val isRootSelected = selectedCategory?.categoryId == share.categoryId || selectedCategory?.categoryName == share.categoryName
                    val isChildSelected = item.subcategoryShares.any { it.categoryId == selectedCategory?.categoryId || it.categoryName == selectedCategory?.categoryName }
                    val isAnySelected = isRootSelected || isChildSelected
                    val isExpanded = item.isExpandable && expandedCategoryIds.contains(share.categoryId)

                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(durationMillis = 250),
                        label = "chevronRotation_${share.categoryId}"
                    )

                    val haptic = LocalHapticFeedback.current

                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isAnySelected) 1.5.dp else 0.dp,
                                color = if (isAnySelected) parseColor else Color.Transparent,
                                shape = ShapeTokens.Lg
                            ),
                        variant = CardVariant.SOLID,
                        shape = ShapeTokens.Lg,
                        backgroundColor = if (isAnySelected) parseColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        onClick = null
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Root Category Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(share, isExpanded, item.isExpandable, isRootSelected) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            try {
                                                withTimeout(2000L) {
                                                    val up = waitForUpOrCancellation()
                                                    if (up != null) {
                                                        if (item.isExpandable) {
                                                            expandedCategoryIds = if (isExpanded) {
                                                                expandedCategoryIds - share.categoryId
                                                            } else {
                                                                expandedCategoryIds + share.categoryId
                                                            }
                                                        }
                                                        onSelectedCategoryChange(if (isRootSelected) null else share)
                                                    }
                                                }
                                            } catch (e: TimeoutCancellationException) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onCategoryLongClick(share)
                                                try {
                                                    waitForUpOrCancellation()
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(parseColor.copy(alpha = 0.14f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rootCat != null) {
                                        CategoryIconView(
                                            iconStr = rootCat.icon,
                                            color = parseColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(parseColor, CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = share.categoryName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (item.isExpandable) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = parseColor.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = "${item.subcategoryShares.size} فرعية",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = parseColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = FormatterUtils.formatCurrency(share.amount),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { share.percentage.coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(3.5.dp)
                                                .clip(CircleShape),
                                            color = parseColor,
                                            trackColor = parseColor.copy(alpha = 0.12f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${(share.percentage * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isAnySelected) parseColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (item.isExpandable) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isExpanded) parseColor.copy(alpha = 0.12f) else Color.Transparent)
                                            .clickable {
                                                expandedCategoryIds = if (isExpanded) {
                                                    expandedCategoryIds - share.categoryId
                                                } else {
                                                    expandedCategoryIds + share.categoryId
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "طي الفئات الفرعية" else "عرض الفئات الفرعية",
                                            tint = if (isExpanded) parseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .rotate(chevronRotation)
                                        )
                                    }
                                }
                            }

                            // Subcategories list when expanded
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(200)),
                                exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(150))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(start = 18.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                        thickness = 0.8.dp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )

                                    // Subcategories
                                    item.subcategoryShares.forEach { subShare ->
                                        val subCatModel = categories.firstOrNull { it.id == subShare.categoryId } ?: categories.firstOrNull { it.name == subShare.categoryName }
                                        val subColor = parsedShareColors[subShare.categoryName] ?: parseColor
                                        val isSubSelected = selectedCategory?.categoryId == subShare.categoryId || selectedCategory?.categoryName == subShare.categoryName

                                        val parentPct = if (share.amount > 0) ((subShare.amount / share.amount) * 100).toInt() else 0

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    width = if (isSubSelected) 1.5.dp else 0.dp,
                                                    color = if (isSubSelected) subColor else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .pointerInput(subShare, isSubSelected) {
                                                    awaitEachGesture {
                                                        awaitFirstDown(requireUnconsumed = false)
                                                        try {
                                                            withTimeout(2000L) {
                                                                val up = waitForUpOrCancellation()
                                                                if (up != null) {
                                                                    onSelectedCategoryChange(if (isSubSelected) null else subShare)
                                                                }
                                                            }
                                                        } catch (e: TimeoutCancellationException) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            onCategoryLongClick(subShare)
                                                            try {
                                                                waitForUpOrCancellation()
                                                            } catch (_: Exception) {}
                                                        }
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSubSelected) subColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(subColor.copy(alpha = 0.12f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (subCatModel != null) {
                                                        CategoryIconView(
                                                            iconStr = subCatModel.icon,
                                                            color = subColor,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(subColor, CircleShape)
                                                        )
                                                    }
                                                }
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
                                                            text = subShare.categoryName,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = FormatterUtils.formatCurrency(subShare.amount),
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "$parentPct% من إجمالي الفئة",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium
                                                            ),
                                                            color = TextGray
                                                        )
                                                        Text(
                                                            text = "${(subShare.percentage * 100).toInt()}% من المصاريف",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = if (isSubSelected) subColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Direct parent spending item (if any)
                                    if (item.directShare != null) {
                                        val directShare = item.directShare
                                        val isDirectSelected = selectedCategory?.categoryId == directShare.categoryId && selectedCategory?.categoryName == directShare.categoryName
                                        val directParentPct = if (share.amount > 0) ((directShare.amount / share.amount) * 100).toInt() else 0

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    width = if (isDirectSelected) 1.5.dp else 0.dp,
                                                    color = if (isDirectSelected) parseColor else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .pointerInput(directShare, isDirectSelected) {
                                                    awaitEachGesture {
                                                        awaitFirstDown(requireUnconsumed = false)
                                                        try {
                                                            withTimeout(2000L) {
                                                                val up = waitForUpOrCancellation()
                                                                if (up != null) {
                                                                    onSelectedCategoryChange(if (isDirectSelected) null else directShare)
                                                                }
                                                            }
                                                        } catch (e: TimeoutCancellationException) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            onCategoryLongClick(directShare)
                                                            try {
                                                                waitForUpOrCancellation()
                                                            } catch (_: Exception) {}
                                                        }
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDirectSelected) parseColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(parseColor.copy(alpha = 0.12f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ReceiptLong,
                                                        contentDescription = null,
                                                        tint = parseColor,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
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
                                                            text = "${share.categoryName} (مباشر / أخرى)",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = FormatterUtils.formatCurrency(directShare.amount),
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "$directParentPct% من إجمالي الفئة",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium
                                                            ),
                                                            color = TextGray
                                                        )
                                                        Text(
                                                            text = "${(directShare.percentage * 100).toInt()}% من المصاريف",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = if (isDirectSelected) parseColor else MaterialTheme.colorScheme.onSurfaceVariant
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
            
            if (hierarchicalShares.size > 5) {
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    onClick = { isLegendExpanded = !isLegendExpanded },
                    variant = ButtonVariant.BORDERED,
                    intent = ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            imageVector = if (isLegendExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text(
                        text = if (isLegendExpanded) "عرض أقل" else "عرض المزيد (+${hierarchicalShares.size - 5} فئات رئيسية)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (showOtherBottomSheet && smallShares.isNotEmpty()) {
                AppBottomSheet(
                    onDismissRequest = { showOtherBottomSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "الفئات الصغيرة الأخرى",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "هذه الفئات تمثل أقل من 4% من إجمالي المصاريف وتم تجميعها لتسهيل قراءة المخطط الدائري.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            textAlign = TextAlign.Right
                        )
                        
                        smallShares.forEach { share ->
                            val parseColor = parsedShareColors[share.categoryName] ?: MaterialTheme.colorScheme.primary
                            AppCard(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                variant = CardVariant.SOLID,
                                shape = ShapeTokens.Lg,
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                onClick = {
                                    onSelectedCategoryChange(share)
                                    showOtherBottomSheet = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(parseColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = share.categoryName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = FormatterUtils.formatCurrency(share.amount),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${(share.percentage * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * A unified smart date navigator that replaces the old tab + dropdown selectors.
 * Shows period-mode pills (يوم / أسبوع / شهر / سنة) and left/right arrow
 * navigator to move between periods. RTL-aware.
 */
@Composable
fun SmartDateNavigator(
    uiState: AnalyticsUiState,
    onPeriodChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val arabicMonths = com.qdash.core.utils.FormatterUtils.getMonthNames()

    val arabicDays = arrayOf(
        "اليوم", "أمس", "قبل يومين", "قبل 3 أيام", "قبل 4 أيام", "قبل 5 أيام", "قبل 6 أيام"
    )
    val arabicWeeks = arrayOf(
        "الأسبوع الحالي", "الأسبوع الماضي", "قبل أسبوعين", "قبل 3 أسابيع"
    )

    val centerLabel = when (uiState.selectedPeriod) {
        "ALL"   -> "كل الأوقات"
        "DAY"   -> arabicDays.getOrElse(uiState.selectedDayOffset) { "اليوم" }
        "WEEK"  -> arabicWeeks.getOrElse(uiState.selectedWeekOffset) { "الأسبوع الحالي" }
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
        "ALL"   to "الكل",
        "DAY"   to "يوم",
        "WEEK"  to "أسبوع",
        "MONTH" to "شهر",
        "YEAR"  to "سنة"
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
                        contentDescription = "السابق",
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
                                "ALL"   -> "إحصائيات كل الأوقات"
                                "DAY"   -> "إحصائيات اليوم"
                                "WEEK"  -> "إحصائيات الأسبوع"
                                "MONTH" -> "إحصائيات الشهر"
                                "YEAR"  -> "إحصائيات السنة"
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
                        contentDescription = "التالي",
                        tint = if (canGoNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}



@Composable
fun SavingsChallengesSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
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
                        text = "تحديات الادخار والتحفيز المالي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "شارك في التحديات لتنمية مدخراتك بطريقة تفاعلية",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "تحديات نشطة",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Challenge 1: 52-Week Challenge
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xl,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                    text = "تحدي الـ 52 أسبوعاً",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ادخر مبلغاً متزايداً كل أسبوع",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }
                        
                        Text(
                            text = "الأسبوع 12 / 52",
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
                                text = "المبلغ الموفر: 6,000 دج",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Text(
                                text = "الهدف: 26,000 دج",
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

                    AppButton(
                        onClick = {
                            Toast.makeText(
                                context,
                                "تم إضافة 500 دج لتحدي الادخار الأسبوعي بنجاح! واصل التقدم 🎯",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.WARNING,
                        shape = ShapeTokens.Md
                    ) {
                        Text(
                            text = "+ ادخر 500 دج لهذا الأسبوع",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // Challenge 2: Spare Change Round-Up
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Xl,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                    text = "تجميع الفكة (Spare Change)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تقريب المعاملات لأقرب 50 أو 100 دج وادخار الفارق",
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
                                text = "الفكة المجمعة هذا الشهر",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            Text(
                                text = "1,450 دج",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = IncomeGreen
                            )
                        }

                        AppButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "تم تحويل 1,450 دج من الفكة المدخرة إلى حساب التوفير بنجاح! 💰",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            variant = ButtonVariant.SOLID,
                            intent = ButtonIntent.SUCCESS,
                            shape = ShapeTokens.Md
                        ) {
                            Text(
                                text = "تحويل للمدخرات",
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
        "DAY"   -> "لهذا اليوم"
        "WEEK"  -> "لهذا الأسبوع"
        "MONTH" -> "لهذا الشهر"
        "YEAR"  -> "لهذه السنة"
        else    -> "لهذه الفترة"
    }

    val periodIcon = when (selectedPeriod) {
        "YEAR"  -> Icons.Default.History
        else    -> Icons.Default.DateRange
    }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                    text = "المخطط البياني هادئ $periodLabel",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "لم تقم بتسجيل أي معاملة $periodLabel. أضف معاملاتك لتفعيل التحليلات والرسوم البيانية التفاعلية.",
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
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.SOLID,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "نصيحة: يمكنك التنقل بين الفترات الزمنية من الأعلى لرؤية إحصائيات الأيام أو الأسابيع الأخرى النشطة.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Call to Action
            AppButton(
                onClick = onAddTransactionClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY,
                shape = ShapeTokens.Lg
            ) {
                Text(
                    text = "+ تسجيل معاملة $periodLabel",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
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
        Icon(
            imageVector = Icons.Default.QuestionMark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun CardAiChatButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "الذكاء الاصطناعي",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun EmergencyFundCard(
    uiState: com.qdash.presentation.analytics.AnalyticsUiState,
    onAiChatClick: (CardAiContext) -> Unit,
    modifier: Modifier = Modifier,
    periodStart: Long = 0L,
    periodEnd: Long = Long.MAX_VALUE
) {
    if (uiState.isDatabaseEmpty) return

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val (statusLabel, statusColor) = when (uiState.emergencyFundStatus) {
                "CRITICAL" -> "حرِج (أقل من شهر)" to ExpenseRed
                "ACCEPTABLE" -> "مقبول (1-3 أشهر)" to SavingsAmber
                "SAFE" -> "آمن (3-6 أشهر)" to IncomeGreen
                else -> "ممتاز (+6 أشهر)" to MaterialTheme.colorScheme.primary
            }

            // Inner Header/Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مؤشر صندوق الطوارئ والأمان المالي",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "قدرتك على الصمود المالي في حال انقطاع الدخل",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                CardAiChatButton(
                    onClick = {
                        val chartDataString = org.json.JSONObject().apply {
                            put("emergencyFundStatus", uiState.emergencyFundStatus)
                            put("currentSavings", uiState.totalSavingsAmount)
                            put("averageExpenses", uiState.averageMonthlyExpense)
                            put("monthsFunded", uiState.emergencyFundRunwayMonths)
                        }.toString()
                        
                        val cardContext = CardAiContext(
                            cardId = "emergency_fund",
                            cardTitle = "مؤشر صندوق الطوارئ والأمان المالي",
                            cardType = CardAiContextType.EmergencyFund,
                            chartData = chartDataString,
                            periodStart = periodStart,
                            periodEnd = periodEnd,
                            tooltipContent = "يقيس هذا المؤشر عدد الأشهر الافتراضية التي يمكنك العيش فيها معتمداً بالكامل على مدخراتك الحالية لتغطية متوسط نفقاتك الشهرية إذا انقطع دخلك فجأة.\n\nالفائدة: يوفر مقياساً حقيقياً لمدى أمانك المالي وصمودك أمام الأزمات المفاجئة (مثل فقدان العمل أو الطوارئ الصحية) دون الحاجة للاقتراض أو الديون."
                        )
                        onAiChatClick(cardContext)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                        text = FormatterUtils.convertNumerals("${String.format(java.util.Locale.US, "%.1f", uiState.emergencyFundRunwayMonths)} أشهر"),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp
                        ),
                        color = statusColor
                    )
                    Text(
                        text = "مدة تغطية الطوارئ",
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
                        Text("إجمالي المدخرات:", style = MaterialTheme.typography.labelSmall, color = TextGray)
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
                        Text("معدل المصاريف الشهري:", style = MaterialTheme.typography.labelSmall, color = TextGray)
                        Text(
                            text = FormatterUtils.formatCurrency(uiState.averageMonthlyExpense),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            val explanation = when (uiState.emergencyFundStatus) {
                "CRITICAL" -> "صندوق طوارئك الحالي ضعيف جداً ولا يغطي شهر واحد من المصاريف. ننصحك بتحويل مبالغ إضافية للتوفير في أقرب وقت لتفادي الأزمات المالية المفاجئة."
                "ACCEPTABLE" -> "مدخراتك مقبولة وتغطي جزءاً من احتياجاتك المؤقتة. يُنصح بالعمل على زيادة المدخرات لتصل إلى تغطية 3 أشهر على الأقل لتحقيق أمان أكبر."
                "SAFE" -> "تهانينا! وضعك المالي آمن جداً. تغطي مدخراتك نفقاتك لعدة أشهر في حال الطوارئ، وهو ما يمنحك راحة بال ممتازة للتعامل مع أي طارئ."
                else -> "وضعك المالي استثنائي وممتاز! لديك وفرة مالية وصندوق طوارئ صلب للغاية يغطي أكثر من 6 أشهر من حياتك دون دخل. أنت تسير بخطى ذهبية."
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
fun SalaryCycleCard(
    uiState: com.qdash.presentation.analytics.AnalyticsUiState,
    onAiChatClick: (CardAiContext) -> Unit,
    modifier: Modifier = Modifier,
    periodStart: Long = 0L,
    periodEnd: Long = Long.MAX_VALUE
) {
    if (uiState.selectedPeriod != "MONTH" || uiState.spendingsByCategory.isEmpty()) return

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inner Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.hasSalarySource) "توقعات الإنفاق ودورة الراتب CCP" else "توقعات الإنفاق للشهر الحالي",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uiState.hasSalarySource) "دورة الراتب: ${uiState.salaryCycleStartLabel} ← ${uiState.salaryCycleEndLabel}" 
                               else "الفترة: 1 إلى نهاية الشهر",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                CardAiChatButton(
                    onClick = {
                        val chartDataString = org.json.JSONObject().apply {
                            put("hasSalarySource", uiState.hasSalarySource)
                            put("salaryCycleStartLabel", uiState.salaryCycleStartLabel)
                            put("salaryCycleEndLabel", uiState.salaryCycleEndLabel)
                            put("projectedSpend", uiState.projectedEndMonthSpending)
                            put("referenceSalary", uiState.salaryAmount)
                            put("referenceBudget", uiState.referenceBudget)
                            put("daysRemaining", uiState.daysRemainingInCycle)
                        }.toString()
                        
                        val cardContext = CardAiContext(
                            cardId = "salary_cycle",
                            cardTitle = "توقعات الإنفاق ودورة الراتب",
                            cardType = CardAiContextType.SalaryCycle,
                            chartData = chartDataString,
                            periodStart = periodStart,
                            periodEnd = periodEnd,
                            tooltipContent = "يقيس هذا المؤشر سرعة وتقدم معدل إنفاقك اليومي الفعلي ومقارنته بالميزانية المحددة أو الراتب المرجعي على مدار أيام دورة الراتب المالي المتبقية.\n\nالفائدة: يتنبأ بإجمالي نفقاتك بنهاية الشهر الجاري ويحذرك مبكراً إذا كنت متجهاً لتجاوز الميزانية لتتمكن من ترشيد نفقاتك وتعديل سلوك الاستهلاك قبل فوات الأوان."
                        )
                        onAiChatClick(cardContext)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (uiState.isProjectedToExceedBudget) ExpenseRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (uiState.isProjectedToExceedBudget) "تنبيه بالصرف" else "إنفاق مستقر",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.isProjectedToExceedBudget) ExpenseRed else IncomeGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                        text = "تقدم الدورة الزمنية (${((uiState.salaryCyclePercentageElapsed) * 100).toInt()}% من الشهر)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Text(
                        text = "متبقي ${uiState.daysRemainingInCycle} يوم",
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            // Projections grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الإنفاق المتوقع",
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
                        text = if (uiState.referenceBudget == uiState.salaryAmount) "ميزانية الراتب المرجعية" else "إجمالي الميزانيات المحددة",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.referenceBudget > 0) FormatterUtils.formatCurrency(uiState.referenceBudget) else "غير محددة",
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
                            text = "بناءً على سرعة إنفاقك، ستتجاوز الميزانية بـ ${FormatterUtils.formatCurrency(exceedAmount)} بنهاية الشهر. حاول ترشيد نفقاتك الكبيرة.",
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
fun WeekendWeekdayCard(
    uiState: com.qdash.presentation.analytics.AnalyticsUiState,
    onAiChatClick: (CardAiContext) -> Unit,
    modifier: Modifier = Modifier,
    periodStart: Long = 0L,
    periodEnd: Long = Long.MAX_VALUE
) {
    if (!uiState.hasWeekendData || uiState.spendingsByCategory.isEmpty()) return

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = CardVariant.SOLID,
        shape = ShapeTokens.Xxl,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val weekdayAvg = uiState.weekdayDailyAverage
            val weekendAvg = uiState.weekendDailyAverage
            val weekdayPercent = uiState.weekdayPercentage
            val weekendPercent = uiState.weekendPercentage

            // Inner Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تحليل الإنفاق: أيام العمل مقابل نهاية الأسبوع",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "توزيع النفقات بين أيام الأسبوع والجمعة/السبت بالجزائر",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                CardAiChatButton(
                    onClick = {
                        val chartDataString = org.json.JSONObject().apply {
                            put("weekdayDailyAverage", weekdayAvg)
                            put("weekendDailyAverage", weekendAvg)
                            put("weekdayPercentage", weekdayPercent)
                            put("weekendPercentage", weekendPercent)
                            put("totalWeekdaySpend", uiState.weekdayExpensesSum)
                            put("totalWeekendSpend", uiState.weekendExpensesSum)
                        }.toString()
                        
                        val cardContext = CardAiContext(
                            cardId = "weekend_weekday",
                            cardTitle = "تحليل الإنفاق: أيام العمل مقابل نهاية الأسبوع",
                            cardType = CardAiContextType.WeekendWeekday,
                            chartData = chartDataString,
                            periodStart = periodStart,
                            periodEnd = periodEnd,
                            tooltipContent = "يقيس هذا التحليل متوسط حجم إنفاقك المالي في أيام الأسبوع العادية (من الأحد إلى الخميس) مقارنة بمتوسط إنفاقك في عطلة نهاية الأسبوع (الجمعة والسبت بالجزائر).\n\nالفائدة: يوضح لك سلوكك الترفيهي أو الاستهلاكي خلال العطلات، مما يساعدك على كبح المصاريف غير الضرورية أو المبالغ فيها خلال عطلة نهاية الأسبوع."
                        )
                        onAiChatClick(cardContext)
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
                        text = "أيام العمل (${(weekdayPercent * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "عطلة نهاية الأسبوع (${(weekendPercent * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavingsAmber
                    )
                }

                // Dual progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(weekdayPercent.coerceAtLeast(0.01f))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(weekendPercent.coerceAtLeast(0.01f))
                                .background(SavingsAmber)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            // Info details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("متوسط يوم العمل (الأحد - الخميس)", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatterUtils.formatCurrency(weekdayAvg),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
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
                    Text("متوسط يوم العطلة (الجمعة والسبت)", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatterUtils.formatCurrency(weekendAvg),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Suggestion banner
            val ratioText = when {
                weekendAvg > weekdayAvg * 1.5 -> "إنفاقك في عطلة نهاية الأسبوع مرتفع جداً مقارنة بالأيام العادية. حاول رصد نفقات الترفيه والتسوق في عطلة نهاية الأسبوع لتقليل الهدر."
                weekendAvg > weekdayAvg -> "إنفاقك في العطلة أعلى بقليل من الأيام العادية. هذا طبيعي للترويح عن النفس، لكن احرص على البقاء ضمن حدود الميزانية."
                else -> "إنفاقك متوازن ومنضبط للغاية خلال العطلة مقارنة بأيام العمل. استمر في هذا الأداء المالي الرائع والمستقر."
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = ratioText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

enum class ChartViewMode {
    DONUT,
    BAR
}


