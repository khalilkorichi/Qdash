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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val navController = LocalNavController.current
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()

    var selectedCategory by remember { mutableStateOf<CategoryShare?>(null) }
    var activeExplanationInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var longClickedCategory by remember { mutableStateOf<CategoryShare?>(null) }
    val scope = rememberCoroutineScope()
    var longClickJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(uiState.spendingsByCategory) {
        selectedCategory = null
    }

    // Map period key → Arabic display name
    val periodLabel = when (uiState.selectedPeriod) {
        "ALL"   -> "الكل"
        "DAY"   -> "اليوم"
        "WEEK"  -> "الأسبوع"
        "MONTH" -> "الشهر"
        "YEAR"  -> "السنة"
        else    -> uiState.selectedPeriod
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen")
    ) { innerPadding ->
        if (longClickedCategory != null) {
            val category = longClickedCategory!!
            var showColorPicker by remember { mutableStateOf(false) }
            var selectedColor by remember(category.categoryId) { mutableStateOf(category.color) }
            var showCustomColorDialog by remember { mutableStateOf(false) }
            // Cache expensive transaction filtering — only recomputed when transactions or category changes
            val categoryTxs by remember(category.categoryName, uiState.transactions, uiState.categories) {
                derivedStateOf {
                    val catId = uiState.categories.firstOrNull { it.name == category.categoryName }?.id
                    uiState.transactions
                        .filter { it.categoryId == catId }
                        .sortedByDescending { it.date }
                }
            }
            // Cache parsed selectedColor — avoids repeated parseColor on every recomposition
            val parsedSelectedColor = remember(selectedColor) {
                try { Color(android.graphics.Color.parseColor(selectedColor)) }
                catch (e: Exception) { null }
            }
            
            if (showCustomColorDialog) {
                var redVal by remember { mutableStateOf((try { android.graphics.Color.red(android.graphics.Color.parseColor(selectedColor)) } catch(e: Exception) { 108 }).toFloat() / 255f) }
                var greenVal by remember { mutableStateOf((try { android.graphics.Color.green(android.graphics.Color.parseColor(selectedColor)) } catch(e: Exception) { 99 }).toFloat() / 255f) }
                var blueVal by remember { mutableStateOf((try { android.graphics.Color.blue(android.graphics.Color.parseColor(selectedColor)) } catch(e: Exception) { 255 }).toFloat() / 255f) }
                
                val previewColor = Color(redVal, greenVal, blueVal)
                val hexString = String.format("#%02X%02X%02X", (redVal*255).toInt(), (greenVal*255).toInt(), (blueVal*255).toInt())
                
                AlertDialog(
                    onDismissRequest = { showCustomColorDialog = false },
                    title = { Text("اختر لوناً مخصصاً للفئة", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(previewColor)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            )
                            
                            Text(
                                text = hexString,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("الأحمر", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                        Text("${(redVal * 255).toInt()}", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                    }
                                    Slider(
                                        value = redVal,
                                        onValueChange = { redVal = it },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.Red,
                                            activeTrackColor = Color.Red.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("الأخضر", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                        Text("${(greenVal * 255).toInt()}", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                    }
                                    Slider(
                                        value = greenVal,
                                        onValueChange = { greenVal = it },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.Green,
                                            activeTrackColor = Color.Green.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("الأزرق", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                        Text("${(blueVal * 255).toInt()}", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                    }
                                    Slider(
                                        value = blueVal,
                                        onValueChange = { blueVal = it },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.Blue,
                                            activeTrackColor = Color.Blue.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            selectedColor = hexString
                            longClickedCategory = category.copy(color = hexString)
                            showCustomColorDialog = false
                        }) {
                            Text("حفظ")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomColorDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
            
            androidx.compose.ui.window.Dialog(onDismissRequest = {
                if (selectedColor != category.color) {
                    viewModel.updateCategoryColor(category.categoryId, selectedColor)
                }
                longClickedCategory = null
                longClickJob?.cancel()
            }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "سجل معاملات: ${category.categoryName}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            (parsedSelectedColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.2f)
                                        )
                                        .clickable {
                                            longClickJob?.cancel()
                                            showColorPicker = !showColorPicker
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "تعديل اللون",
                                        tint = parsedSelectedColor ?: MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Preset colors — defined once, never recreated
                        val colorPresets = remember {
                            listOf(
                                "#6C63FF", "#22C55E", "#EF4444", "#3B82F6", "#F59E0B",
                                "#EC4899", "#8B5CF6", "#06B6D4", "#10B981", "#F97316"
                            )
                        }
                        // Parse preset colors once — never reparsed on recomposition
                        val parsedPresetColors = remember {
                            colorPresets.map { hex ->
                                try { Color(android.graphics.Color.parseColor(hex)) }
                                catch (e: Exception) { Color.Gray }
                            }
                        }
                        // Sweep gradient brush cached once
                        val rainbowBrush = remember {
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color.Red, Color.Yellow, Color.Green,
                                    Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                            )
                        }

                        AnimatedVisibility(
                            visible = showColorPicker,
                            enter = fadeIn(animationSpec = tween(150)),
                            exit = fadeOut(animationSpec = tween(100))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "اختر لوناً للفئة:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextGray
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Row 1: First 5 preset colors
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        colorPresets.take(5).forEachIndexed { idx, hex ->
                                            val colorVal = parsedPresetColors[idx]
                                            val isCurrent = selectedColor.equals(hex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(colorVal)
                                                    .border(
                                                        width = if (isCurrent) 2.5.dp else 0.dp,
                                                        color = if (isCurrent) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        selectedColor = hex
                                                        longClickedCategory = category.copy(color = hex)
                                                    }
                                            )
                                        }
                                    }

                                    // Row 2: Next 5 preset colors + Custom color button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        colorPresets.drop(5).forEachIndexed { relIdx, hex ->
                                            val colorVal = parsedPresetColors[relIdx + 5]
                                            val isCurrent = selectedColor.equals(hex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(colorVal)
                                                    .border(
                                                        width = if (isCurrent) 2.5.dp else 0.dp,
                                                        color = if (isCurrent) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        selectedColor = hex
                                                        longClickedCategory = category.copy(color = hex)
                                                    }
                                            )
                                        }

                                        // Custom Color Wheel Button — brush cached via remember
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(rainbowBrush)
                                                .clickable { showCustomColorDialog = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = "لون مخصص",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        
                        if (categoryTxs.isEmpty()) {
                            Text(
                                text = "لا توجد معاملات لهذه الفئة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                // Cache date formatter — SimpleDateFormat creation is expensive
                                val dateFormatter = remember { java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US) }
                                categoryTxs.take(4).forEach { tx ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tx.note ?: "بدون ملاحظة",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val dateStr = dateFormatter.format(java.util.Date(tx.date))
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextGray
                                            )
                                        }
                                        Text(
                                            text = FormatterUtils.formatCurrency(tx.amount),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = if (tx.type == com.example.domain.model.TransactionType.INCOME) IncomeGreen else ExpenseRed
                                        )
                                    }
                                }
                                if (categoryTxs.size > 4) {
                                    Text(
                                        text = "+ وأكثر بـ ${categoryTxs.size - 4} عمليات",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeExplanationInfo != null) {
            AlertDialog(
                onDismissRequest = { activeExplanationInfo = null },
                confirmButton = {
                    TextButton(onClick = { activeExplanationInfo = null }) {
                        Text("حسناً", fontWeight = FontWeight.Bold)
                    }
                },
                title = { 
                    Text(
                        text = activeExplanationInfo?.first ?: "", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                text = {
                    Text(
                        text = activeExplanationInfo?.second ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                }
            )
        }

        // ── Export Report Dialog Overlays ──────────────────────────────────
        if (uiState.exportingProgressText != null) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("جاري تصدير التقرير", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.exportingProgressText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            )
        }

        if (uiState.exportResult != null) {
            val context = LocalContext.current
            val fileUri = uiState.exportResult?.fileUri
            AlertDialog(
                onDismissRequest = { viewModel.clearExportState() },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = {
                            fileUri?.let { com.example.core.utils.FileUtils.openPdfFile(context, it) }
                        }) {
                            Text("فتح التقرير")
                        }
                        TextButton(onClick = { viewModel.clearExportState() }) {
                            Text("حسناً")
                        }
                    }
                },
                title = { Text("تم التصدير بنجاح", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = {
                    Text("تم حفظ التقرير المالي الشامل بصيغة PDF بنجاح في المسار:\n\n$fileUri")
                }
            )
        }

        if (uiState.exportError != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearExportState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearExportState() }) {
                        Text("حسناً")
                    }
                },
                title = { Text("فشل تصدير التقرير", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = {
                    Text("حدث خطأ أثناء تصدير التقرير:\n\n${uiState.exportError}")
                }
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Unified Screen Header ─────────────────────────────────────
            item {
                UnifiedScreenHeader(
                    title = "التقارير والتحليلات",
                    subtitle = "حلل سلوكك الإنفاقي وحقق أهدافك الادخارية الذكية",
                    showBackButton = false,
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = uiState.spendingsByCategory.isNotEmpty(),
                                enter = androidx.compose.animation.fadeIn(animationSpec = tween(350)),
                                exit = androidx.compose.animation.fadeOut(animationSpec = tween(350))
                            ) {
                                Surface(
                                     shape = RoundedCornerShape(12.dp),
                                     color = Primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                         text = "عرض: $periodLabel",
                                         style = MaterialTheme.typography.labelSmall,
                                         color = Primary,
                                         modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                         fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                     .size(36.dp)
                                     .clip(CircleShape)
                                     .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                     .clickable { viewModel.exportPdfReport() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                     imageVector = Icons.Default.FileDownload,
                                     contentDescription = "تصدير تقرير PDF",
                                     tint = MaterialTheme.colorScheme.primary,
                                     modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }

            // ── 2. Unified Smart Date Navigator ───────────────────────────
            item {
                SmartDateNavigator(
                    uiState = uiState,
                    onPeriodChange = { viewModel.setPeriod(it) },
                    onPrev = { viewModel.navigatePrev() },
                    onNext = { viewModel.navigateNext() }
                )
            }

            // ── 2.5 Salary Cycle & Spend Projection Card ──────────────────
            if (!uiState.isLoading && uiState.selectedPeriod == "MONTH" && uiState.spendingsByCategory.isNotEmpty()) {
                item {
                    SalaryCycleProjectionCard(
                        uiState = uiState,
                        onHelpClick = { title, desc -> activeExplanationInfo = title to desc }
                    )
                }
            }

            // ── 3. Donut Chart Card (top-5 legend + progress bars) ─────────
            if (uiState.isLoading) {
                item {
                    DonutChartSkeleton()
                }
            } else if (uiState.spendingsByCategory.isNotEmpty()) {
                val shares = uiState.spendingsByCategory
                item {
                    InteractiveDonutCard(
                        shares = shares,
                        selectedCategory = selectedCategory,
                        onSelectedCategoryChange = { selectedCategory = it },
                        onHelpClick = { title, desc -> activeExplanationInfo = title to desc },
                        onCategoryLongClick = { category ->
                            longClickJob?.cancel()
                            longClickedCategory = category
                            longClickJob = scope.launch {
                                kotlinx.coroutines.delay(3000)
                                longClickedCategory = null
                            }
                        }
                    )
                }
            }

            // ── 4. Summary Cards — gradient accents ────────────────────────
            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCardSkeleton(modifier = Modifier.weight(1f))
                        SummaryCardSkeleton(modifier = Modifier.weight(1f))
                    }
                }
            } else if (uiState.spendingsByCategory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Largest Expense card — subtle red gradient
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ExpenseRed.copy(alpha = 0.08f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.02f)
                                        )
                                    )
                                )
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                        .copy(alpha = 0.95f)
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(ExpenseRed, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "أعلى إنفاق مفرد",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uiState.largestExpenseName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatterUtils.formatCurrency(uiState.largestExpense),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Savings Rate card — subtle green gradient
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                        .copy(alpha = 0.95f)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                IncomeGreen.copy(alpha = 0.08f),
                                                IncomeGreen.copy(alpha = 0.02f)
                                            )
                                        )
                                    )
                            )
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(IncomeGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "معدل الادخار",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${(uiState.savingsRate * 100).toInt()}% مدخر",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val (msg, icon, iconColor) = when {
                                        uiState.savingsRate >= 0.2f -> Triple("حالة مالية ممتازة ومستقرة", Icons.Default.TrendingUp, IncomeGreen)
                                        uiState.savingsRate >= 0.1f -> Triple("حالة جيدة", Icons.Default.TrendingFlat, SavingsAmber)
                                        else -> Triple("تحتاج لتقليل نفقاتك", Icons.Default.TrendingDown, ExpenseRed)
                                    }
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
            if (!uiState.isLoading && uiState.spendingsByCategory.isNotEmpty()) {
                item {
                    EmergencyFundRunwayCard(
                        uiState = uiState,
                        onHelpClick = { title, desc -> activeExplanationInfo = title to desc }
                    )
                }
            }

            // ── 4.5 Weekend vs Weekday Spending Card ───────────────────
            if (!uiState.isLoading && uiState.spendingsByCategory.isNotEmpty()) {
                item {
                    WeekendWeekdaySpendingCard(
                        uiState = uiState,
                        onHelpClick = { title, desc -> activeExplanationInfo = title to desc }
                    )
                }
            }

            // ── 5. Bar Chart — rounded bars, 200dp canvas ─────────────────
            if (uiState.isLoading) {
                item {
                    BarChartSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                }
            } else if (uiState.spendingsByCategory.isNotEmpty()) {
            item {
                val density = LocalDensity.current
                val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                val textPaint = remember(labelColor, density) {
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = with(density) { 9.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.RIGHT
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                    }
                }
                val xTextPaint = remember(labelColor, density) {
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = with(density) { 10.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                    }
                }
                val decimalFormatter = remember { java.text.DecimalFormat("#,##0") }

                var selectedTrendIndex by remember { mutableStateOf<Int?>(null) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "التدفق النقدي التاريخي (الدخل vs المصروف)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            HelpIconButton(
                                onClick = {
                                    activeExplanationInfo = "التدفق النقدي التاريخي" to "يقارن هذا المخطط تاريخياً بين التدفقات النقدية الداخلة (إجمالي الدخل المالي) والتدفقات الخارجة (إجمالي النفقات والمصاريف) على مدار الفترات الزمنية السابقة.\n\nالفائدة: يساعدك في رصد اتجاه نموك المالي؛ فبقاء عمود الدخل أعلى باستمرار من عمود المصاريف يضمن زيادة ثروتك وبناء ملاءة مالية متينة."
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        val gridLineColor = MaterialTheme.colorScheme.outlineVariant
                        val trendData = uiState.trendData
                        val primaryColor = MaterialTheme.colorScheme.primary

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .pointerInput(trendData) {
                                    detectTapGestures { offset ->
                                        val leftPadding = 56.dp.toPx()
                                        val rightPadding = 8.dp.toPx()
                                        val chartWidth = size.width - leftPadding - rightPadding
                                        if (trendData.isNotEmpty()) {
                                            val groupWidth = chartWidth / trendData.size
                                            val clickedIndex = ((offset.x - leftPadding) / groupWidth).toInt()
                                            if (clickedIndex in trendData.indices) {
                                                selectedTrendIndex = if (selectedTrendIndex == clickedIndex) null else clickedIndex
                                            }
                                        }
                                    }
                                }
                        ) {
                            val leftPadding = 56.dp.toPx()
                            val rightPadding = 8.dp.toPx()
                            val topPadding = 12.dp.toPx()
                            val bottomPadding = 24.dp.toPx()

                            val chartWidth = size.width - leftPadding - rightPadding
                            val chartHeight = size.height - topPadding - bottomPadding

                            if (trendData.isNotEmpty()) {
                                val maxAmount = trendData.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0 } ?: 10000.0
                                val yAxisMax = maxAmount * 1.15 // 15% padding at top

                                // ── 1. Draw Gridlines & Y-Axis Labels ──
                                val gridLines = 4
                                for (i in 0 until gridLines) {
                                    val fraction = i.toFloat() / (gridLines - 1)
                                    val amount = yAxisMax * fraction
                                    val y = size.height - bottomPadding - (fraction * chartHeight)

                                    // Gridline
                                    drawLine(
                                        color = if (i == 0) gridLineColor.copy(alpha = 0.8f)
                                                else gridLineColor.copy(alpha = 0.25f),
                                        start = androidx.compose.ui.geometry.Offset(leftPadding, y),
                                        end = androidx.compose.ui.geometry.Offset(size.width - rightPadding, y),
                                        strokeWidth = (if (i == 0) 1.5.dp else 1.dp).toPx(),
                                        pathEffect = if (i > 0) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                                    )

                                    // Y-Axis label
                                    val labelText = "${decimalFormatter.format(amount)} دج"
                                    drawIntoCanvas { canvas ->
                                        canvas.nativeCanvas.drawText(
                                            labelText,
                                            leftPadding - 8.dp.toPx(),
                                            y + 4.dp.toPx(),
                                            textPaint
                                        )
                                    }
                                }

                                // ── 2. Draw Bars & X-Axis Labels ──
                                val numGroups = trendData.size
                                val groupWidth = chartWidth / numGroups

                                val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                                trendData.forEachIndexed { i, trend ->
                                    val centerX = leftPadding + (i + 0.5f) * groupWidth
                                    
                                    val barWidth = (groupWidth * 0.25f).coerceIn(8.dp.toPx(), 16.dp.toPx())
                                    val barSpacing = (barWidth * 0.3f).coerceIn(2.dp.toPx(), 6.dp.toPx())

                                    // Calculate bar heights
                                    val incomeHeight = ((trend.income / yAxisMax) * chartHeight).toFloat().coerceAtLeast(0f)
                                    val expenseHeight = ((trend.expense / yAxisMax) * chartHeight).toFloat().coerceAtLeast(0f)

                                    // Draw selection background indicator
                                    if (selectedTrendIndex == i) {
                                        drawRoundRect(
                                            color = primaryColor.copy(alpha = 0.08f),
                                            topLeft = androidx.compose.ui.geometry.Offset(centerX - groupWidth / 2, topPadding),
                                            size = androidx.compose.ui.geometry.Size(groupWidth, chartHeight),
                                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                        )
                                    }

                                    // Draw Income Bar (Green Gradient)
                                    if (incomeHeight > 0f) {
                                        val incomeGradient = Brush.verticalGradient(
                                            colors = listOf(IncomeGreen, IncomeGreen.copy(alpha = 0.25f)),
                                            startY = size.height - bottomPadding - incomeHeight,
                                            endY = size.height - bottomPadding
                                        )
                                        drawRoundRect(
                                            brush = incomeGradient,
                                            topLeft = androidx.compose.ui.geometry.Offset(
                                                centerX - barWidth - barSpacing / 2,
                                                size.height - bottomPadding - incomeHeight
                                            ),
                                            size = androidx.compose.ui.geometry.Size(barWidth, incomeHeight),
                                            cornerRadius = cornerRadius
                                        )
                                    }

                                    // Draw Expense Bar (Red Gradient)
                                    if (expenseHeight > 0f) {
                                        val expenseGradient = Brush.verticalGradient(
                                            colors = listOf(ExpenseRed, ExpenseRed.copy(alpha = 0.25f)),
                                            startY = size.height - bottomPadding - expenseHeight,
                                            endY = size.height - bottomPadding
                                        )
                                        drawRoundRect(
                                            brush = expenseGradient,
                                            topLeft = androidx.compose.ui.geometry.Offset(
                                                centerX + barSpacing / 2,
                                                size.height - bottomPadding - expenseHeight
                                            ),
                                            size = androidx.compose.ui.geometry.Size(barWidth, expenseHeight),
                                            cornerRadius = cornerRadius
                                        )
                                    }

                                    // Draw X-Axis Period Label (centered below the group)
                                    drawIntoCanvas { canvas ->
                                        canvas.nativeCanvas.drawText(
                                            trend.periodLabel,
                                            centerX,
                                            size.height - bottomPadding + 18.dp.toPx(),
                                            xTextPaint
                                        )
                                    }
                                }
                            }
                        }

                        selectedTrendIndex?.let { index ->
                            val trend = trendData.getOrNull(index)
                            if (trend != null) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "تفاصيل التدفق: ${trend.periodLabel}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                IconButton(
                                                    onClick = { selectedTrendIndex = null },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "إغلاق التفاصيل",
                                                        tint = TextGray,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("الدخل", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                                    Text(FormatterUtils.formatCurrency(trend.income), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = IncomeGreen)
                                                }
                                                Column {
                                                    Text("المصاريف", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                                    Text(FormatterUtils.formatCurrency(trend.expense), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
                                                }
                                                val diff = trend.income - trend.expense
                                                Column {
                                                    Text("الصافي", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                                    Text(
                                                        text = (if (diff >= 0) "+" else "") + FormatterUtils.formatCurrency(diff),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (diff >= 0) IncomeGreen else ExpenseRed
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(IncomeGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "الدخل الكلي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(ExpenseRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "المصاريف",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }
            }

            
            // Explanatory Text removed and replaced by help icon button in card header

            // ── 6. Empty state ─────────────────────────────────────────────
            if (!uiState.isLoading && uiState.spendingsByCategory.isEmpty()) {
                item {
                    if (uiState.isDatabaseEmpty) {
                        AnalyticsEmptyState(
                            onAddTransactionClick = {
                                navController?.navigate(Screen.AddTransaction.createRoute("EXPENSE"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    } else {
                        SimplePeriodEmptyState(
                            selectedPeriod = uiState.selectedPeriod,
                            onAddTransactionClick = {
                                navController?.navigate(Screen.AddTransaction.createRoute("EXPENSE"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            // Deleted category ranking list to prevent duplication with the legend inside InteractiveDonutCard

            if (!uiState.isLoading && uiState.spendingsByCategory.isNotEmpty()) {
                item {
                    SavingsChallengesSection()
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        } // end LazyColumn
        } // end PullToRefreshBox
    } // end Scaffold
}

@Composable
private fun DonutChartSkeleton(modifier: Modifier = Modifier) {
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
private fun SummaryCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(96.dp)
            .shimmerEffect(ShapeTokens.Lg)
    )
}

@Composable
private fun BarChartSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .shimmerEffect(RoundedCornerShape(20.dp))
    )
}

@Composable
private fun CategoryRankSkeleton(modifier: Modifier = Modifier) {
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

            Button(
                onClick = onAddTransactionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = "سجل أول معاملة الآن",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}
@Composable
private fun InteractiveDonutCard(
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
    // Cache all share parsed colors once — avoids Color.parseColor on every animation frame
    val parsedShareColors = remember(shares) {
        shares.associate { share ->
            share.categoryName to (try { Color(android.graphics.Color.parseColor(share.color)) } catch (e: Exception) { null })
        }
    }
    // Cache bar chart gradients per share — avoids Brush creation on every recomposition
    val parsedBarBrushes = remember(shares) {
        shares.associate { share ->
            val c = try { Color(android.graphics.Color.parseColor(share.color)) } catch (e: Exception) { Color.Gray }
            share.categoryName to Brush.verticalGradient(colors = listOf(c, c.copy(alpha = 0.5f)))
        }
    }
    // Cache total amount — avoids sumOf traversal on every recomposition
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
                    HelpIconButton(
                        onClick = {
                            onHelpClick(
                                "توزيع المصاريف حسب الفئة",
                                "يقيس هذا المخطط النسبة المئوية لإجمالي نفقاتك الموزعة على مختلف الفئات المالية (كالأغذية، النقل، الفواتير، إلخ) خلال الفترة الزمنية المحددة.\n\nالفائدة: يساعدك على رصد الفئات الرئيسية الأكثر استهلاكاً لسيولتك النقدية لتتمكن من اتخاذ قرارات واعية بكبح الصرف في الجوانب الترفيهية وزيادة مدخراتك."
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
                // Donut Canvas — 240dp with premium visual layout and floating badge
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
                                            text = "${share.categoryName} • ${(share.percentage * 100).toInt()}%",
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
 * Shows period-mode pills (يوم / أسبوع / شهر / سنة) and left/right arrow
 * navigator to move between periods. RTL-aware.
 */
@Composable
private fun SmartDateNavigator(
    uiState: AnalyticsUiState,
    onPeriodChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val arabicMonths = arrayOf(
        "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
        "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
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
                            text = if (uiState.hasSalarySource) "توقعات الإنفاق ودورة الراتب CCP" else "توقعات الإنفاق للشهر الحالي",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.hasSalarySource) "دورة الراتب: ${uiState.salaryCycleStartLabel} ← ${uiState.salaryCycleEndLabel}" 
                                   else "الفترة: 1 إلى نهاية الشهر",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    HelpIconButton(
                        onClick = {
                            onHelpClick(
                                "توقعات الإنفاق ودورة الراتب",
                                "يقيس هذا المؤشر سرعة وتقدم معدل إنفاقك اليومي الفعلي ومقارنته بالميزانية المحددة أو الراتب المرجعي على مدار أيام دورة الراتب المالي المتبقية.\n\nالفائدة: يتنبأ بإجمالي نفقاتك بنهاية الشهر الجاري ويحذرك مبكراً إذا كنت متجهاً لتجاوز الميزانية لتتمكن من ترشيد نفقاتك وتعديل سلوك الاستهلاك قبل فوات الأوان."
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
                        text = if (uiState.isProjectedToExceedBudget) "تنبيه بالصرف" else "إنفاق مستقر",
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

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
                        text = "تحليل الإنفاق: أيام العمل مقابل نهاية الأسبوع",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "توزيع النفقات بين أيام الأسبوع والجمعة/السبت بالجزائر",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                HelpIconButton(
                    onClick = {
                        onHelpClick(
                            "تحليل أيام العمل مقابل عطلة نهاية الأسبوع",
                            "يقيس هذا التحليل متوسط حجم إنفاقك المالي في أيام الأسبوع العادية (من الأحد إلى الخميس) مقارنة بمتوسط إنفاقك في عطلة نهاية الأسبوع (الجمعة والسبت بالجزائر).\n\nالفائدة: يوضح لك سلوكك الترفيهي أو الاستهلاكي خلال العطلات، مما يساعدك على كبح المصاريف غير الضرورية أو المبالغ فيها خلال عطلة نهاية الأسبوع."
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
                        text = "أيام العمل (${(uiState.weekdayPercentage * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "نهاية الأسبوع (${(uiState.weekendPercentage * 100).toInt()}%)",
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
                        Text("معدل أيام العمل اليومي", style = MaterialTheme.typography.labelSmall, color = TextGray)
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
                        Text("معدل نهاية الأسبوع اليومي", style = MaterialTheme.typography.labelSmall, color = TextGray)
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
                "معدل إنفاقك اليومي في عطلة نهاية الأسبوع مرتفع جداً مقارنة بأيام العمل. احذر من التبذير خلال العطلة."
            } else {
                "معدل إنفاقك اليومي متزن بين أيام العمل وعطلة نهاية الأسبوع. استمر في هذا الانضباط المالي."
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
        "CRITICAL" -> "حرِج (أقل من شهر)" to ExpenseRed
        "ACCEPTABLE" -> "مقبول (1-3 أشهر)" to SavingsAmber
        "SAFE" -> "آمن (3-6 أشهر)" to IncomeGreen
        else -> "ممتاز (+6 أشهر)" to MaterialTheme.colorScheme.primary
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
                            text = "مؤشر صندوق الطوارئ والأمان المالي",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "قدرتك على الصمود المالي في حال انقطاع الدخل",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                    HelpIconButton(
                        onClick = {
                            onHelpClick(
                                "مؤشر صندوق الطوارئ والأمان المالي",
                                "يقيس هذا المؤشر عدد الأشهر الافتراضية التي يمكنك العيش فيها معتمداً بالكامل على مدخراتك الحالية لتغطية متوسط نفقاتك الشهرية إذا انقطع دخلك فجأة.\n\nالفائدة: يوفر مقياساً حقيقياً لمدى أمانك المالي وصمودك أمام الأزمات المفاجئة (مثل فقدان العمل أو الطوارئ الصحية) دون الحاجة للاقتراض أو الديون."
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
                        text = "${String.format(java.util.Locale.US, "%.1f", uiState.emergencyFundRunwayMonths)} أشهر",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

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

                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "تم إضافة 500 دج لتحدي الادخار الأسبوعي بنجاح! واصل التقدم 🎯",
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
                            text = "+ ادخر 500 دج لهذا الأسبوع",
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

                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "تم تحويل 1,450 دج من الفكة المدخرة إلى حساب التوفير بنجاح! 💰",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                            modifier = Modifier.height(38.dp)
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
                        text = "💡",
                        fontSize = 16.sp
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
            Button(
                onClick = onAddTransactionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = "+ تسجيل معاملة $periodLabel",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HelpIconButton(
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
            text = "؟",
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

