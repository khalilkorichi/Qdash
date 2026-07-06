package com.qdash.presentation.settings.components

import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.TextGray
import kotlin.math.roundToInt

@Composable
fun DashboardCustomizationDialog(
    initialSectionsOrder: List<String>,
    initialSectionsVisibility: Map<String, Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (sectionsOrder: List<String>, visibleMap: Map<String, Boolean>) -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    var sectionsOrder by remember {
        mutableStateOf(initialSectionsOrder.toMutableList())
    }
    var visibleMap by remember {
        mutableStateOf(initialSectionsVisibility.toMutableMap())
    }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 70.dp.toPx() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "تخصيص أقسام الصفحة الرئيسية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "استخدم مقبض السحب (☰) لترتيب العناصر بالسحب والإفلات، أو الأسهم، والمفاتيح لإظهار/إخفاء أي قسم.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    AppButton(
                        onClick = {
                            val defaultOrder = "split_cards,context_templates,templates,quick_actions,accounts,chart,budget,subscriptions,recent_transactions".split(",")
                            sectionsOrder = defaultOrder.toMutableList()
                            val defaultVisibility = defaultOrder.associateWith { true }.toMutableMap()
                            visibleMap = defaultVisibility
                        },
                        variant = ButtonVariant.LIGHT,
                        intent = ButtonIntent.PRIMARY,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "استعادة الافتراضي",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("استعادة الافتراضي", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                sectionsOrder.forEachIndexed { index, section ->
                    val label = when (section) {
                        "split_cards" -> "بطاقة الدخل والمصاريف"
                        "context_templates" -> "اقتراحات ذكية (حسب الوقت)"
                        "templates" -> "القوالب المثبتة"
                        "quick_actions" -> "الوصول السريع"
                        "accounts" -> "حساباتي المالية"
                        "chart" -> "تحليل المصروفات"
                        "budget" -> "الميزانية الشهرية"
                        "subscriptions" -> "الاشتراكات القادمة"
                        "recent_transactions" -> "آخر العمليات"
                        else -> section
                    }

                    val isDragging = draggedIndex == index
                    val offsetY = if (isDragging) dragOffsetY else 0f

                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, offsetY.roundToInt()) }
                            .zIndex(if (isDragging) 10f else 1f),
                        variant = CardVariant.SOLID,
                        shape = ShapeTokens.Md,
                        backgroundColor = if (isDragging) 
                                MaterialTheme.colorScheme.surfaceVariant 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "سحب للترتيب",
                                    tint = if (isDragging) Primary else TextGray.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(24.dp)
                                        .pointerInput(index) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggedIndex = index
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    
                                                    val targetIndex = draggedIndex
                                                    if (targetIndex != null) {
                                                        val offsetIndexDiff = (dragOffsetY / itemHeightPx).roundToInt()
                                                        val newIndex = (targetIndex + offsetIndexDiff).coerceIn(0, sectionsOrder.size - 1)
                                                        if (newIndex != targetIndex) {
                                                            val newList = sectionsOrder.toMutableList()
                                                            val temp = newList[targetIndex]
                                                            newList[targetIndex] = newList[newIndex]
                                                            newList[newIndex] = temp
                                                            sectionsOrder = newList
                                                            draggedIndex = newIndex
                                                            dragOffsetY -= offsetIndexDiff * itemHeightPx
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        }
                                )

                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val newList = sectionsOrder.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index - 1]
                                            newList[index - 1] = temp
                                            sectionsOrder = newList
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "للأعلى",
                                        tint = if (index > 0) Primary else TextGray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (index < sectionsOrder.size - 1) {
                                            val newList = sectionsOrder.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index + 1]
                                            newList[index + 1] = temp
                                            sectionsOrder = newList
                                        }
                                    },
                                    enabled = index < sectionsOrder.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "للأسفل",
                                        tint = if (index < sectionsOrder.size - 1) Primary else TextGray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )

                            Switch(
                                checked = visibleMap[section] ?: true,
                                onCheckedChange = { isChecked ->
                                    val newMap = visibleMap.toMutableMap()
                                    newMap[section] = isChecked
                                    visibleMap = newMap
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    onConfirm(sectionsOrder, visibleMap)
                },
                variant = ButtonVariant.SOLID,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                variant = ButtonVariant.LIGHT,
                intent = ButtonIntent.PRIMARY
            ) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
