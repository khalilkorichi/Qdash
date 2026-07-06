package com.qdash.presentation.backup.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens

@Composable
fun BackupTabHeader(
    activeTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
    val primaryColor = MaterialTheme.colorScheme.primary
    val tabUnselectedBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color(0xFFF3F3F1)
    val tabUnselectedBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f) else ColorTokens.BorderLight
    val tabUnselectedContent = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else ColorTokens.TextSecondaryLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val tabs = listOf(
            "النسخ الاحتياطي اليدوي" to Icons.Default.Save,
            "النسخ السحابي" to Icons.Default.Cloud
        )
        tabs.forEachIndexed { index, (label, icon) ->
            val isSelected = activeTab == index

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) primaryColor.copy(alpha = 0.15f) else Color.White
                } else tabUnselectedBg,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "backupTabBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) primaryColor else tabUnselectedContent,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "backupTabContent"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) primaryColor.copy(alpha = 0.3f) else Color(0xFFB0B0AD)
                } else tabUnselectedBorder,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "backupTabBorder"
            )

            val pillShape = RoundedCornerShape(12.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
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
                        onTabSelect(index)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ManualBackupTabContent(
    backupFolderUri: String?,
    backupScheduleInterval: String,
    lastBackupUriForShare: Uri?,
    onSelectFolderClick: () -> Unit,
    onScheduleIntervalChange: (String) -> Unit,
    onImmediateBackupClick: () -> Unit,
    onImportZipClick: () -> Unit,
    onExportCsvTransactionsClick: () -> Unit,
    onExportCsvCategoriesClick: () -> Unit,
    onExportJsonClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
    val primaryColor = MaterialTheme.colorScheme.primary
    val textSecondaryColor = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight
    val cardBgColor = if (isDark) ColorTokens.CardDark else ColorTokens.CardLight
    val segmentedControlBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFEEEEEC)
    val selectedOptionBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val selectedOptionBorder = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFD0D0CD)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "خيارات الحفظ والاسترداد المحلي",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        AppCard(
            variant = CardVariant.SOLID,
            shape = ShapeTokens.Lg,
            backgroundColor = cardBgColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (backupFolderUri.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorTokens.Danger.copy(alpha = 0.08f), ShapeTokens.Md)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ColorTokens.Danger,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "يرجى تحديد مجلد لحفظ النسخ الاحتياطية لتنشيط الجدولة والنسخ الفوري.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.Danger,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(primaryColor.copy(alpha = 0.04f), ShapeTokens.Md)
                            .border(1.dp, primaryColor.copy(alpha = 0.08f), ShapeTokens.Md)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "مجلد الحفظ المعتمد حالياً:",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondaryColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = backupFolderUri,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                AppButton(
                    onClick = onSelectFolderClick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.BORDERED,
                    intent = ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = primaryColor
                        )
                    }
                ) {
                    Text(
                        text = if (backupFolderUri.isNullOrEmpty()) "تحديد مجلد الحفظ" else "تغيير مجلد الحفظ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.outlineVariant else ColorTokens.BorderLight)

                Text(
                    text = "جدولة النسخ الاحتياطي التلقائي الخلفي:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(segmentedControlBg, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    val options = listOf(
                        "none" to "أبداً",
                        "daily" to "يومياً",
                        "weekly" to "أسبوعياً",
                        "monthly" to "شهرياً"
                    )
                    options.forEach { (key, label) ->
                        val selected = backupScheduleInterval == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) selectedOptionBg else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = if (selected) 1.dp else 0.dp,
                                    color = if (selected) selectedOptionBorder else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onScheduleIntervalChange(key) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) primaryColor else textSecondaryColor
                            )
                        }
                    }
                }

                AppButton(
                    onClick = onImmediateBackupClick,
                    enabled = !backupFolderUri.isNullOrEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.SOLID,
                    intent = ButtonIntent.PRIMARY,
                    shape = ShapeTokens.Lg,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                ) {
                    Text(
                        text = "بدء نسخ فوري الآن",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (lastBackupUriForShare != null) {
                    AppButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, lastBackupUriForShare)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.FLAT,
                        intent = ButtonIntent.INFO,
                        shape = ShapeTokens.Lg,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = ColorTokens.Info
                            )
                        }
                    ) {
                        Text(
                            text = "مشاركة النسخة الاحتياطية الأخيرة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorTokens.Info
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(ShapeTokens.Lg)
                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(0.3f) else Color(0xFFF3F3F1))
                .border(
                    width = 1.dp,
                    color = if (isDark) MaterialTheme.colorScheme.outlineVariant else ColorTokens.BorderLight,
                    shape = ShapeTokens.Lg
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onImportZipClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDark) primaryColor else ColorTokens.TextPrimaryLight
                )
                Text(
                    text = "استيراد واستعادة البيانات من ملف (ZIP)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) primaryColor else ColorTokens.TextPrimaryLight
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "تصدير التقارير وجداول العمليات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        AppButton(
            onClick = onExportCsvTransactionsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            variant = ButtonVariant.BORDERED,
            intent = ButtonIntent.SUCCESS,
            shape = ShapeTokens.Lg,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = ColorTokens.Success
                )
            }
        ) {
            Text(
                text = "تصدير كشف المعاملات (CSV)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ColorTokens.Success
            )
        }

        AppButton(
            onClick = onExportCsvCategoriesClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            variant = ButtonVariant.BORDERED,
            intent = ButtonIntent.PRIMARY,
            shape = ShapeTokens.Lg,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = primaryColor
                )
            }
        ) {
            Text(
                text = "تصدير قوائم الأقسام (CSV)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(ShapeTokens.Lg)
                .background(
                    if (isDark) ColorTokens.Info.copy(alpha = 0.10f)
                    else ColorTokens.Info.copy(alpha = 0.08f)
                )
                .border(
                    width = 1.5.dp,
                    color = ColorTokens.Info.copy(alpha = if (isDark) 0.30f else 0.40f),
                    shape = ShapeTokens.Lg
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExportJsonClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDark) ColorTokens.InfoDark else ColorTokens.Info
                )
                Text(
                    text = "مشاركة وتبادل البيانات الفردية (JSON)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) ColorTokens.InfoDark else ColorTokens.Info
                )
            }
        }
    }
}

@Composable
fun CloudBackupTabContent(
    isLinked: Boolean,
    email: String?,
    lastSyncTimestamp: Long,
    isLoading: Boolean,
    onTriggerDriveSync: () -> Unit,
    onTriggerDriveRestore: () -> Unit,
    onLaunchGoogleSignIn: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background != ColorTokens.BackgroundLight
    val textSecondaryColor = if (isDark) ColorTokens.TextSecondaryDark else ColorTokens.TextSecondaryLight
    val textMutedColor = if (isDark) ColorTokens.TextMutedDark else ColorTokens.TextSecondaryLight

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "المزامنة السحابية عبر Google Drive",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        AppCard(
            variant = CardVariant.OUTLINED,
            shape = ShapeTokens.Lg,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (isLinked) Color(0xFF22C55E) else textSecondaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "حالة الاتصال بالسحاب",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isLinked && !email.isNullOrEmpty()) {
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textMutedColor
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (isLinked) Color(0xFF22C55E).copy(alpha = 0.12f)
                                else textSecondaryColor.copy(alpha = 0.12f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isLinked) "متصل" else "غير متصل",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isLinked) Color(0xFF22C55E) else textSecondaryColor
                        )
                    }
                }

                if (!isLinked) {
                    Text(
                        text = "قم بربط حسابك في Google للتمكن من أخذ نسخة احتياطية سحابية واستعادتها في أي وقت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AppButton(
                        onClick = onLaunchGoogleSignIn,
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.SOLID,
                        intent = ButtonIntent.PRIMARY,
                        shape = ShapeTokens.Lg,
                        enabled = !isLoading
                    ) {
                        Text("ربط الحساب بجوجل للمزامنة السحابية", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isLinked) {
            AppCard(
                variant = CardVariant.OUTLINED,
                shape = ShapeTokens.Lg,
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "إجراءات المزامنة السحابية",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppButton(
                            onClick = onTriggerDriveSync,
                            modifier = Modifier.weight(1f),
                            variant = ButtonVariant.SOLID,
                            intent = ButtonIntent.PRIMARY,
                            enabled = !isLoading,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(
                                text = "مزامنة الآن",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                maxLines = 1
                            )
                        }

                        AppButton(
                            onClick = onTriggerDriveRestore,
                            modifier = Modifier.weight(1f),
                            variant = ButtonVariant.LIGHT,
                            intent = ButtonIntent.PRIMARY,
                            enabled = !isLoading,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(
                                text = "استعادة من السحاب",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                maxLines = 1
                            )
                        }
                    }

                    if (lastSyncTimestamp > 0) {
                        Text(
                            text = "آخر مزامنة سحابية ناجحة: ${formatRelativeSyncTime(lastSyncTimestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = textMutedColor,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun formatRelativeSyncTime(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val diffMin = diffMs / 60000
    return when {
        diffMin < 1 -> "الآن"
        diffMin < 60 -> "منذ $diffMin دقيقة"
        diffMin < 1440 -> {
            val hours = diffMin / 60
            if (hours == 1L) "منذ ساعة" else if (hours == 2L) "منذ ساعتين" else "منذ $hours ساعة"
        }
        else -> {
            val days = diffMin / 1440
            if (days == 1L) "منذ يوم" else if (days == 2L) "منذ يومين" else "منذ $days أيام"
        }
    }
}
