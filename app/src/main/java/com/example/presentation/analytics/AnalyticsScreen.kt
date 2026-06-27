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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
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
import java.util.Calendar

private data class AnalyticsTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

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

    // Compute Dashboard statistics reactively
    val dashboardTransactions = remember(uiState.transactions, uiState.dashboardPeriod, uiState.dashboardMonth, uiState.dashboardYear) {
        uiState.transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            if (uiState.dashboardPeriod == "MONTHLY") {
                cal.get(Calendar.MONTH) == uiState.dashboardMonth && cal.get(Calendar.YEAR) == uiState.dashboardYear
            } else {
                cal.get(Calendar.YEAR) == uiState.dashboardYear
            }
        }
    }

    val dashIncome = remember(dashboardTransactions) {
        dashboardTransactions.filter { it.type == com.example.domain.model.TransactionType.INCOME }.sumOf { it.amount }
    }
    
    val dashExpenses = remember(dashboardTransactions) {
        dashboardTransactions.filter { it.type == com.example.domain.model.TransactionType.EXPENSE }.sumOf { it.amount }
    }

    LaunchedEffect(uiState.spendingsByCategory) {
        selectedCategory = null
    }

    LaunchedEffect(uiState.dashboardTab) {
        selectedCategory = null
    }

    LaunchedEffect(uiState.dashboardTab) {
        selectedCategory = null
    }

    // Map period key â†’ Arabic display name
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
            // Cache expensive transaction filtering â€” only recomputed when transactions or category changes
            val categoryTxs by remember(category.categoryName, uiState.transactions, uiState.categories) {
                derivedStateOf {
                    val catId = uiState.categories.firstOrNull { it.name == category.categoryName }?.id
                    uiState.transactions
                        .filter { it.categoryId == catId }
                        .sortedByDescending { it.date }
                }
            }
            // Cache parsed selectedColor â€” avoids repeated parseColor on every recomposition
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
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Xl,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
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

                        // Preset colors â€” defined once, never recreated
                        val colorPresets = remember {
                            listOf(
                                "#6C63FF", "#22C55E", "#EF4444", "#3B82F6", "#F59E0B",
                                "#EC4899", "#8B5CF6", "#06B6D4", "#10B981", "#F97316"
                            )
                        }
                        // Parse preset colors once â€” never reparsed on recomposition
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

                                        // Custom Color Wheel Button â€” brush cached via remember
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
                                // Cache date formatter â€” SimpleDateFormat creation is expensive
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
                                            val dateStr = FormatterUtils.convertNumerals(dateFormatter.format(java.util.Date(tx.date)))
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

        // â”€â”€ Export Report Dialog Overlays â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

            // â”€â”€ Unified Screen Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                                visible = uiState.spendingsByCategory.isNotEmpty() && uiState.dashboardTab == 0,
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

            // ── Main Tab Switcher ───────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        AnalyticsTab("التقارير", Icons.Default.Assessment),
                        AnalyticsTab("المقارنة", Icons.Default.CompareArrows),
                        AnalyticsTab("التحليلات", Icons.Default.PieChart),
                        AnalyticsTab("الادخار", Icons.Default.Savings)
                    )
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = uiState.dashboardTab == index
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "tabBg_$index"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "tabContent_$index"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) Primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "tabBorder_$index"
                        )
                        val pillShape = RoundedCornerShape(12.dp)

                        Box(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(bgColor)
                                .border(
                                    width = 1.dp,
                                    color = borderColor,
                                    shape = pillShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    viewModel.setDashboardTab(index)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab.label,
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.dashboardTab == 0) {
                // ── GENERAL REPORTS TAB ───────────────────

                // â”€â”€ 2. Unified Smart Date Navigator â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                item {
                    SmartDateNavigator(
                        uiState = uiState,
                        onPeriodChange = { viewModel.setPeriod(it) },
                        onPrev = { viewModel.navigatePrev() },
                        onNext = { viewModel.navigateNext() }
                    )
                }

            // â”€â”€ 2.5 Salary Cycle & Spend Projection Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€


            // â”€â”€ 3. Donut Chart Card (top-5 legend + progress bars) â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                            longClickedCategory = category
                        },
                        categories = uiState.categories
                    )
                }
            }

            // â”€â”€ 4. Summary Cards â€” gradient accents â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                    // Largest Expense card â€” subtle red gradient
                    AppCard(
                        modifier = Modifier
                            .weight(1f),
                        variant = CardVariant.SOLID,
                        shape = ShapeTokens.Xl,
                        backgroundColor = Color.Transparent
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

                    // Savings Rate card â€” subtle green gradient
                    AppCard(
                        modifier = Modifier
                            .weight(1f),
                        variant = CardVariant.SOLID,
                        shape = ShapeTokens.Xl,
                        backgroundColor = Color.Transparent
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
                                val savingsRatePercent = (uiState.savingsRate * 100).toInt()
                                val rateColor = if (savingsRatePercent >= 0) IncomeGreen else ExpenseRed
                                val rateText = if (savingsRatePercent >= 0) "+$savingsRatePercent%" else "$savingsRatePercent%"

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(rateColor, CircleShape)
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
                                    text = rateText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = rateColor
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


            // â”€â”€ 5. Bar Chart â€” rounded bars, 200dp canvas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    variant = CardVariant.SOLID,
                    shape = ShapeTokens.Xl,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
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

                                // â”€â”€ 1. Draw Gridlines & Y-Axis Labels â”€â”€
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
                                    val labelText = FormatterUtils.convertNumerals("${decimalFormatter.format(amount)} دج")
                                    drawIntoCanvas { canvas ->
                                        canvas.nativeCanvas.drawText(
                                            labelText,
                                            leftPadding - 8.dp.toPx(),
                                            y + 4.dp.toPx(),
                                            textPaint
                                        )
                                    }
                                }

                                // â”€â”€ 2. Draw Bars & X-Axis Labels â”€â”€
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
                                    AppCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        variant = CardVariant.SOLID,
                                        shape = ShapeTokens.Lg,
                                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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

            // â”€â”€ 6. Empty state â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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



            } else if (uiState.dashboardTab == 1) {
                // ── INTERACTIVE DASHBOARD TAB ───────────────────

                // 1. Dashboard filter header & Period switcher
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Monthly vs Annually Segmented Pill Switcher
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val periods = listOf("MONTHLY" to "شهرياً", "ANNUALLY" to "سنوياً")
                                periods.forEach { (key, label) ->
                                    val isSelected = uiState.dashboardPeriod == key
                                    val bg by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        label = "periodBg_$key"
                                    )
                                    val txt by animateColorAsState(
                                        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        label = "periodTxt_$key"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(bg)
                                            .clickable { viewModel.setDashboardPeriod(key) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = txt
                                        )
                                    }
                                }
                            }
                        }

                        // Date Selector Button
                        var showDashboardDatePicker by remember { mutableStateOf(false) }
                        val arabicMonths = remember {
                            arrayOf(
                                "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
                                "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
                            )
                        }
                        val pickerLabel = if (uiState.dashboardPeriod == "MONTHLY") {
                            "${arabicMonths[uiState.dashboardMonth]} ${uiState.dashboardYear}"
                        } else {
                            "سنة ${uiState.dashboardYear}"
                        }

                        Button(
                            onClick = { showDashboardDatePicker = true },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = pickerLabel,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showDashboardDatePicker) {
                            if (uiState.dashboardPeriod == "MONTHLY") {
                                MonthYearPickerDialog(
                                    initialMonth = uiState.dashboardMonth,
                                    initialYear = uiState.dashboardYear,
                                    onDismiss = { showDashboardDatePicker = false },
                                    onConfirm = { m, y ->
                                        viewModel.setDashboardMonth(m)
                                        viewModel.setDashboardYear(y)
                                        showDashboardDatePicker = false
                                    }
                                )
                            } else {
                                YearPickerDialog(
                                    initialYear = uiState.dashboardYear,
                                    onDismiss = { showDashboardDatePicker = false },
                                    onConfirm = { y ->
                                        viewModel.setDashboardYear(y)
                                        showDashboardDatePicker = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (uiState.isDatabaseEmpty) {
                    item {
                        EmptyStateView(
                            title = "لا تتوفر بيانات للمقارنة",
                            description = "قم بتسجيل معاملاتك المالية أولاً لتتمكن من مقارنة الدخل والمصاريف بين الفترات المختلفة.",
                            icon = Icons.Default.CompareArrows,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    // 3. Render Dashboard overview card
                    item {
                        DashboardOverviewCard(
                            totalIncome = dashIncome,
                            totalExpenses = dashExpenses,
                            onHelpClick = { title, desc -> activeExplanationInfo = title to desc }
                        )
                    }

                    // 5. Render Month Comparison Card
                    item {
                        MonthComparisonCard(
                            transactions = uiState.transactions,
                            categories = uiState.categories,
                            compareMonthA = uiState.compareMonthA,
                            compareYearA = uiState.compareYearA,
                            compareMonthB = uiState.compareMonthB,
                            compareYearB = uiState.compareYearB,
                            onMonthAChange = { m, y -> viewModel.setCompareMonthA(m, y) },
                            onMonthBChange = { m, y -> viewModel.setCompareMonthB(m, y) },
                            onHelpClick = { title, desc -> activeExplanationInfo = title to desc }
                        )
                    }
                }
            } else if (uiState.dashboardTab == 2) {
                // ── SMART INSIGHTS TAB ───────────────────
                item { Spacer(modifier = Modifier.height(8.dp)) }
                if (uiState.isDatabaseEmpty) {
                    item {
                        EmptyStateView(
                            title = "التحليلات الذكية هادئة حالياً",
                            description = "أضف بعض المعاملات والمصاريف اليومية لتوليد تحليلات ذكية حول عادات الإنفاق وصندوق الطوارئ الخاص بك.",
                            icon = Icons.Default.PieChart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    item {
                        EmergencyFundCard(
                            uiState = uiState,
                            onHelpClick = { title, desc -> activeExplanationInfo = title to desc }
                        )
                    }
                    if (uiState.selectedPeriod == "MONTH" && uiState.spendingsByCategory.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(10.dp)) }
                        item {
                            SalaryCycleCard(
                                uiState = uiState,
                                onHelpClick = { title, desc -> activeExplanationInfo = title to desc }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(10.dp)) }
                    item {
                        WeekendWeekdayCard(
                            uiState = uiState,
                            onHelpClick = { title, desc -> activeExplanationInfo = title to desc }
                        )
                    }
                }
            } else if (uiState.dashboardTab == 3) {
                // ── SAVINGS TAB ───────────────────
                item { Spacer(modifier = Modifier.height(16.dp)) }
                if (uiState.isDatabaseEmpty) {
                    item {
                        EmptyStateView(
                            title = "لا تتوفر تحديات ادخار حالياً",
                            description = "سجل معاملاتك المعتادة لتفعيل تحديات الادخار المخصصة ومساعدتك على توفير المال.",
                            icon = Icons.Default.Savings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                } else if (!uiState.isLoading && uiState.spendingsByCategory.isNotEmpty()) {
                    item {
                        SavingsChallengesSection()
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        } // end LazyColumn
        } // end PullToRefreshBox
    } // end Scaffold
}
